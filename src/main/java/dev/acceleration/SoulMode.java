package dev.acceleration;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

public final class SoulMode {
    public static final String BODY_TAG="acceleration_soul_body";
    public static final ResourceKey<DamageType> STRIKE=ResourceKey.create(Registries.DAMAGE_TYPE,AccelerationMod.id("soul_strike"));
    private record LungeReturn(Vec3 start,Vec3 last,int still,int ticks){}
    private static final Map<UUID,LungeReturn> LUNGE_RETURNS=new HashMap<>();
    private static final Map<UUID,Long> HIT_RETURNS=new HashMap<>();
    public static boolean hitReturnedThisTick(ServerPlayer player){return HIT_RETURNS.getOrDefault(player.getUUID(),-1L)==player.level().getGameTime();}
    private static boolean finishFromHit(ServerPlayer player,Vec3 destination){
        HIT_RETURNS.put(player.getUUID(),player.level().getGameTime());return finish(player,destination);
    }
    public static void afterMeleeHit(Entity victim,DamageSource source,boolean hit){
        if(hit&&source.getEntity() instanceof ServerPlayer actor&&source.getDirectEntity()==actor
                &&victim!=actor&&active(actor)&&!ownBody(actor,victim))finishFromHit(actor,actor.position());
    }
    private static final Set<UUID> BODY_DAMAGE=new HashSet<>();
    public static boolean forwardingBodyDamage(ServerPlayer player){return BODY_DAMAGE.contains(player.getUUID());}
    private SoulMode(){}
    public static boolean ownBody(Player player,Entity target){
        if(!(target instanceof PlayerBody body))return false;
        if(player instanceof ServerPlayer owner){
            var projection=SoulState.get(owner.level().getServer()).projection(owner.getUUID());
            return projection!=null&&projection.body().equals(body.getUUID());
        }
        return ProjectionPhysics.active(player)&&body.getProfile().partialProfile().id().equals(player.getUUID());
    }
    public static boolean active(ServerPlayer player){return SoulState.get(player.level().getServer()).projection(player.getUUID())!=null;}
    private static void sync(ServerPlayer player,boolean active){player.getEntityData().set(ProjectionPhysics.ACTIVE,active);if(ServerPlayNetworking.canSend(player,SoulPackets.State.TYPE))ServerPlayNetworking.send(player,new SoulPackets.State(active));}
    public static boolean begin(ServerPlayer player,Vec3 anchor){
        if(active(player)){
            if(!player.isAlive()||!player.getMainHandItem().is(AccelerationMod.SPEAR))return false;
            // Native enchantment has applied its impulse, but movement arrives in later ticks.
            LUNGE_RETURNS.put(player.getUUID(),new LungeReturn(player.position(),player.position(),0,0));return true;
        }
        if(!player.isAlive()||player.isSpectator()||active(player)||!player.getMainHandItem().is(AccelerationMod.SPEAR)
                ||player.getCooldowns().isOnCooldown(player.getMainHandItem())||player.isPassenger())return false;
        HIT_RETURNS.remove(player.getUUID());
        player.gameMode().updatePlayerAbilities(player.getAbilities());
        ServerLevel world=player.level();
        PlayerBody body=AccelerationMod.BODY.create(world,EntitySpawnReason.EVENT);if(body==null)return false;
        body.setPos(anchor);body.setYRot(player.getYRot());body.setYHeadRot(player.getYHeadRot());body.setXRot(player.getXRot());
        body.setNoGravity(true);body.addTag(BODY_TAG);body.setCustomName(player.getName());body.setCustomNameVisible(false);
        body.setProfile(ResolvableProfile.createResolved(player.getGameProfile()));
        for(EquipmentSlot slot:EquipmentSlot.values())if(slot!=EquipmentSlot.BODY&&slot!=EquipmentSlot.SADDLE)
            body.setItemSlot(slot,player.getItemBySlot(slot).copy());
        var session=new SoulState.Projection(player.getUUID(),world.dimension(),anchor,player.getYRot(),player.getXRot(),
            player.gameMode(),player.getAbilities().pack(),body.getUUID(),Long.MAX_VALUE);
        var state=SoulState.get(player.level().getServer());state.put(session);
        if(!world.addFreshEntity(body)){state.remove(player.getUUID());return false;}
        player.stopUsingItem();player.setCamera(player);
        player.onUpdateAbilities();
        sync(player,true); // Preserve the native lunge impulse instead of replacing or stopping it.
        for(Entity entity:world.getAllEntities())if(entity instanceof net.minecraft.world.entity.Mob mob){
            if(mob.getTarget()==player)mob.setTarget(null);
            var brain=mob.getBrain();
            var attack=net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET;
            if(brain.checkMemory(attack,net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)&&brain.getMemory(attack).orElse(null)==player)brain.eraseMemory(attack);
            var angry=net.minecraft.world.entity.ai.memory.MemoryModuleType.ANGRY_AT;
            if(brain.checkMemory(angry,net.minecraft.world.entity.ai.memory.MemoryStatus.REGISTERED)&&brain.getMemory(angry).filter(player.getUUID()::equals).isPresent())brain.eraseMemory(angry);
        }
        world.playSound(null,anchor.x,anchor.y,anchor.z,SoundEvents.SOUL_ESCAPE,SoundSource.PLAYERS,1,1);
        return true;
    }
    /** Null destination returns to the original body; a strike commits the player at the target. */
    public static boolean finish(ServerPlayer player,Vec3 destination){
        var state=SoulState.get(player.level().getServer());var session=state.projection(player.getUUID());if(session==null)return false;
        state.remove(player.getUUID());LUNGE_RETURNS.remove(player.getUUID());
        for(ServerLevel world:player.level().getServer().getAllLevels()){
            var body=world.getEntity(session.body());if(body!=null)body.discard();
        }
        ServerLevel origin=player.level().getServer().getLevel(session.dimension());if(origin==null)origin=player.level().getServer().overworld();
        Vec3 returnTo=destination==null?session.anchor():destination;
        ServerLevel target=destination==null?origin:player.level();
        player.setCamera(player);
        // Projection never changes game mode. Never restore a stale Creative mode over Survival.
        if(player.gameMode()==session.mode())player.getAbilities().apply(session.abilities());
        player.gameMode().updatePlayerAbilities(player.getAbilities());player.onUpdateAbilities();
        Vec3 momentum=player.getDeltaMovement();
        player.teleportTo(target,returnTo.x,returnTo.y,returnTo.z,Set.of(),player.getYRot(),player.getXRot(),true);
        player.setDeltaMovement(destination==null?Vec3.ZERO:momentum);player.fallDistance=0;
        sync(player,false);return true;
    }
    public static void tick(ServerPlayer player){
        if(!hitReturnedThisTick(player))HIT_RETURNS.remove(player.getUUID());
        var session=SoulState.get(player.level().getServer()).projection(player.getUUID());if(session==null)return;
        if(!player.isAlive()||!player.level().dimension().equals(session.dimension())
                ||!AccelerationMod.carrying(player)) {finish(player,null);return;}
        player.setCamera(player);
        var pending=LUNGE_RETURNS.get(player.getUUID());
        if(pending!=null){
            int ticks=pending.ticks()+1;
            int still=player.position().distanceToSqr(pending.last())<.0025?pending.still()+1:0;
            boolean travelled=player.position().distanceToSqr(pending.start())>.04;
            // Wait for forward movement to settle, then reunite at the actual position.
            // Continuous running/falling resolves after one second, retaining all momentum.
            if((ticks>=4&&travelled&&still>=3)||ticks>=20)finish(player,player.position());
            else LUNGE_RETURNS.put(player.getUUID(),new LungeReturn(pending.start(),player.position(),still,ticks));
        }
    }

    public static boolean damageBody(ServerLevel world,Entity body,DamageSource source,float amount){
        var session=SoulState.get(world.getServer()).body(body.getUUID());
        if(session==null){body.discard();return false;}
        var owner=world.getServer().getPlayerList().getPlayer(session.owner());
        if(owner==null){body.discard();return false;}
        if(source.getEntity() instanceof Player attacker&&attacker!=owner&&
                (!world.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP)||!attacker.canHarmPlayer(owner)))return false;
        // The real player's armor, effects, totem, invulnerability frames and health handle this hit.
        // External damage stays projected; only the owner's own direct hit recalls them.
        boolean hit;
        BODY_DAMAGE.add(owner.getUUID());
        try{hit=owner.hurtServer(owner.level(),source,amount);}finally{BODY_DAMAGE.remove(owner.getUUID());}
        if(body instanceof LivingEntity living){living.setHealth(owner.getHealth());if(hit)living.hurtTime=10;}
        if(!owner.isAlive())finish(owner,body.position());
        else if(hit&&source.getEntity()==owner&&source.getDirectEntity()==owner
                &&source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK))finishFromHit(owner,null);
        return hit;
    }
    private static boolean eligible(ServerPlayer player,LivingEntity victim){
        if(victim==player||!victim.isAlive()||victim.isInvulnerable()||victim.entityTags().contains(BODY_TAG)||victim.level()!=player.level())return false;
        return !(victim instanceof Player target)||(!target.isCreative()&&!target.isSpectator()&&player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP)&&player.canHarmPlayer(target));
    }
    public static boolean strike(ServerPlayer player,Entity clicked){
        if(active(player)&&ownBody(player,clicked)&&player.getMainHandItem().is(AccelerationMod.SPEAR)
                &&player.isWithinEntityInteractionRange(clicked,0))return finish(player,null);
        LivingEntity victim=clicked instanceof LivingEntity living?living:
            clicked instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part?part.parentMob:null;
        if(victim==null)return false;
        if(!active(player)||!player.getMainHandItem().is(AccelerationMod.SPEAR)||!eligible(player,victim)
                ||!player.isWithinEntityInteractionRange(clicked,0))return false;
        ServerLevel world=player.level();Vec3 destination=landing(world,player,clicked);
        if(destination==null){player.sendOverlayMessage(Component.literal("No clear landing space beside that target."));return false;}
        // End projection before applying one hit: no repeated packet can pop a second totem.
        finish(player,destination);
        DamageSource source=new DamageSource(world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(STRIKE),player);
        victim.invulnerableTime=0;
        float lethal=(float)Math.min(Float.MAX_VALUE,Math.max(1_000_000D,(double)victim.getHealth()+victim.getAbsorptionAmount()+1));
        boolean hit=victim instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon?
            dragon.hurt(world,dragon.head,source,lethal):victim.hurtServer(world,source,lethal);
        world.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.PLAYER_ATTACK_CRIT,SoundSource.PLAYERS,1,1);
        return hit;
    }
    private static Vec3 landing(ServerLevel world,ServerPlayer player,Entity target){
        for(Vec3 offset:new Vec3[]{new Vec3(1.6,0,0),new Vec3(-1.6,0,0),new Vec3(0,0,1.6),new Vec3(0,0,-1.6),new Vec3(0,1,0)}){
            Vec3 position=target.position().add(offset);
            var box=player.getBoundingBox().move(position.subtract(player.position()));
            if(world.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(position))&&world.noCollision(player,box)
                    &&!world.containsAnyLiquid(box))return position;
        }
        return null;
    }
}
