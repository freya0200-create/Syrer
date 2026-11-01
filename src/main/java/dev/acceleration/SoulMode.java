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
    public static final float STRIKE_DAMAGE=Float.MAX_VALUE;
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
                &&victim!=actor&&active(actor)&&!STRIKING.contains(actor.getUUID())&&!ownBody(actor,victim)){
            if(actor.getMainHandItem().is(AccelerationMod.SPEAR))
                LUNGE_RETURNS.put(actor.getUUID(),new LungeReturn(actor.position(),actor.position(),0,0));
            else {
                // Mace smash and Wind Burst evaluate fall distance AFTER damage returns.
                // The reunion teleport must not erase that context before native item effects.
                double fall=actor.fallDistance;
                boolean mace=actor.getMainHandItem().is(net.minecraft.world.item.Items.MACE);
                finishFromHit(actor,actor.position());
                if(mace)actor.fallDistance=fall;
            }
        }
    }
    private static final Set<UUID> STRIKING=new HashSet<>();
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
    public static boolean activateMomentum(ServerPlayer player){
        return !active(player)&&begin(player,player.position(),true);
    }
    public static boolean begin(ServerPlayer player,Vec3 anchor){return begin(player,anchor,false);}
    private static boolean begin(ServerPlayer player,Vec3 anchor,boolean fromKey){
        if(active(player)){
            if(!player.isAlive()||!player.getMainHandItem().is(AccelerationMod.SPEAR))return false;
            // An empty lunge is movement, not a successful attack: keep the existing body.
            return true;
        }
        if(!player.isAlive()||player.isSpectator()||active(player)||player.isSleeping()
                ||(!fromKey&&!player.getMainHandItem().is(AccelerationMod.SPEAR))||!AccelerationMod.carrying(player)
                ||player.getCooldowns().isOnCooldown(new net.minecraft.world.item.ItemStack(AccelerationMod.SPEAR))||player.isPassenger())return false;
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
    /** Null destination returns to the anchor; a non-null position reunites the body there. */
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
        // In-place reunion needs no position-correction packet: avoid camera snapping on hits.
        if(destination==null||target!=player.level()||returnTo.distanceToSqr(player.position())>1e-10)
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
    private static boolean eligible(ServerPlayer player,Entity victim){
        if(victim==player||ProjectionPhysics.active(victim)||!victim.isAlive()||victim.isInvulnerable()||victim.entityTags().contains(BODY_TAG)||victim.level()!=player.level())return false;
        return !(victim instanceof Player target)||(!target.isCreative()&&!target.isSpectator()&&player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP)&&player.canHarmPlayer(target));
    }
    public static boolean ghostJoustHit(Entity target,DamageSource source){
        return source.getEntity() instanceof ServerPlayer actor&&source.getDirectEntity()==actor
            &&active(actor)&&actor.isUsingItem()&&actor.getMainHandItem().is(AccelerationMod.SPEAR)
            &&actor.getUseItem().is(AccelerationMod.SPEAR)&&!ownBody(actor,target);
    }
    public static boolean strikeFromJoust(ServerPlayer player,Entity clicked){return strike(player,clicked,true);}
    public static boolean strike(ServerPlayer player,Entity clicked){return strike(player,clicked,false);}
    private static boolean strike(ServerPlayer player,Entity clicked,boolean jousting){
        if(active(player)&&ownBody(player,clicked)&&player.getMainHandItem().is(AccelerationMod.SPEAR)
                &&player.isWithinAttackRange(player.getMainHandItem(),clicked.getBoundingBox(),0))return finish(player,null);
        Entity victim=clicked instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part?part.parentMob:clicked;
        if(!active(player)||STRIKING.contains(player.getUUID())||!player.getMainHandItem().is(AccelerationMod.SPEAR)||!eligible(player,victim)
                ||!(jousting?player.isWithinAttackRange(player.getMainHandItem(),clicked.getBoundingBox(),0):player.isWithinAttackRange(player.getMainHandItem(),clicked.getBoundingBox(),0)))return false;
        ServerLevel world=player.level();Vec3 destination=player.position();
        // Collect the narrow forward thrust before any damage, teleport or entity removal.
        Vec3 start=player.getEyePosition();
        var targetBox=clicked.getBoundingBox();
        // Keep a level thrust for tall targets rather than aiming down through their centre.
        double aimY=Math.max(targetBox.minY+.1,Math.min(start.y,targetBox.maxY-.1));
        Vec3 direction=new Vec3(targetBox.getCenter().x,aimY,targetBox.getCenter().z).subtract(start).normalize();
        Vec3 end=start.add(direction.scale(6));
        var obstruction=world.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player));
        if(obstruction.getType()!=HitResult.Type.MISS)end=obstruction.getLocation();
        final Vec3 rayEnd=end;
        Map<UUID,Entity> pierced=new LinkedHashMap<>();
        pierced.put(victim.getUUID(),victim);
        world.getEntities(player,new AABB(start,end).inflate(.3),entity->eligible(player,entity))
            .stream()
            .filter(entity->entity.getBoundingBox().inflate(.25).contains(start)
                ||entity.getBoundingBox().inflate(.25).clip(start,rayEnd).isPresent())
            .sorted(Comparator.comparingDouble(entity->entity.distanceToSqr(player)))
            .forEach(entity->{
                Entity resolved=entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part?part.parentMob:entity;
                if(eligible(player,resolved))pierced.putIfAbsent(resolved.getUUID(),resolved);
            });
        // Damage every distinct victim once while still projected. A totem saves each victim once.
        STRIKING.add(player.getUUID());
        try{
            DamageSource source=new DamageSource(world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(STRIKE),player);
            boolean hit=false;
            for(Entity target:pierced.values()){
                target.invulnerableTime=0;
                if(target instanceof net.minecraft.world.entity.boss.wither.WitherBoss wither)wither.setInvulnerableTicks(0);
                boolean damaged=target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon?
                    dragon.hurt(world,dragon.head,source,STRIKE_DAMAGE):target.hurtServer(world,source,STRIKE_DAMAGE);
                if(damaged)SpearFire.apply(player,target);
                hit|=damaged;
            }
            if(hit){
                finishFromHit(player,destination);
                world.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.PLAYER_ATTACK_CRIT,SoundSource.PLAYERS,1,1);
            }
            return hit;
        }finally{STRIKING.remove(player.getUUID());}
    }

}
