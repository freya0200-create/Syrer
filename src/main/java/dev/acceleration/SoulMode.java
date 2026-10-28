package dev.acceleration;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

public final class SoulMode {
    public static final String BODY_TAG="acceleration_soul_body";
    public static final ResourceKey<DamageType> STRIKE=ResourceKey.create(Registries.DAMAGE_TYPE,AccelerationMod.id("soul_strike"));
    private SoulMode(){}
    public static boolean active(ServerPlayer player){return SoulState.get(player.level().getServer()).projection(player.getUUID())!=null;}
    private static void sync(ServerPlayer player,boolean active){if(ServerPlayNetworking.canSend(player,SoulPackets.State.TYPE))ServerPlayNetworking.send(player,new SoulPackets.State(active));}
    public static boolean begin(ServerPlayer player,Vec3 anchor){
        if(active(player))return player.isAlive()&&player.getMainHandItem().is(AccelerationMod.SPEAR)&&finish(player,anchor);
        if(!player.isAlive()||player.isSpectator()||active(player)||!player.getMainHandItem().is(AccelerationMod.SPEAR)
                ||player.getCooldowns().isOnCooldown(player.getMainHandItem())||player.isPassenger())return false;
        ServerLevel world=player.level();
        Mannequin body=EntityTypes.MANNEQUIN.create(world,EntitySpawnReason.EVENT);if(body==null)return false;
        body.setPos(anchor);body.setYRot(player.getYRot());body.setYHeadRot(player.getYHeadRot());body.setXRot(player.getXRot());
        body.setNoGravity(true);body.addTag(BODY_TAG);body.setCustomName(player.getName());body.setCustomNameVisible(false);
        body.setComponent(DataComponents.PROFILE,ResolvableProfile.createResolved(player.getGameProfile()));
        for(EquipmentSlot slot:EquipmentSlot.values())if(slot!=EquipmentSlot.BODY&&slot!=EquipmentSlot.SADDLE)
            body.setItemSlot(slot,player.getItemBySlot(slot).copy());
        var session=new SoulState.Projection(player.getUUID(),world.dimension(),anchor,player.getYRot(),player.getXRot(),
            player.gameMode(),player.getAbilities().pack(),body.getUUID(),Long.MAX_VALUE);
        var state=SoulState.get(player.level().getServer());state.put(session);
        if(!world.addFreshEntity(body)){state.remove(player.getUUID());return false;}
        player.stopUsingItem();player.setCamera(player);
        player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();
        player.setDeltaMovement(player.getLookAngle().scale(1.5));sync(player,true);
        world.playSound(null,anchor.x,anchor.y,anchor.z,SoundEvents.SOUL_ESCAPE,SoundSource.PLAYERS,1,1);
        return true;
    }
    /** Null destination returns to the original body; a strike commits the player at the target. */
    public static boolean finish(ServerPlayer player,Vec3 destination){
        var state=SoulState.get(player.level().getServer());var session=state.projection(player.getUUID());if(session==null)return false;
        state.remove(player.getUUID());
        for(ServerLevel world:player.level().getServer().getAllLevels()){
            var body=world.getEntity(session.body());if(body!=null)body.discard();
        }
        ServerLevel origin=player.level().getServer().getLevel(session.dimension());if(origin==null)origin=player.level().getServer().overworld();
        Vec3 returnTo=destination==null?session.anchor():destination;
        ServerLevel target=destination==null?origin:player.level();
        player.setCamera(player);player.setGameMode(session.mode());player.getAbilities().apply(session.abilities());player.onUpdateAbilities();
        player.teleportTo(target,returnTo.x,returnTo.y,returnTo.z,Set.of(),player.getYRot(),player.getXRot(),true);
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        sync(player,false);return true;
    }
    public static void tick(ServerPlayer player){
        var session=SoulState.get(player.level().getServer()).projection(player.getUUID());if(session==null)return;
        if(!player.isAlive()||!player.level().dimension().equals(session.dimension())
                ||!AccelerationMod.carrying(player)) {finish(player,null);return;}
        player.setCamera(player);
        if(player.tickCount%4==0)player.level().sendParticles(ParticleTypes.SOUL,player.getX(),player.getY()+.8,player.getZ(),3,.15,.3,.15,.02);
    }
    public static boolean damageBody(ServerLevel world,Entity body,DamageSource source,float amount){
        var session=SoulState.get(world.getServer()).body(body.getUUID());
        if(session==null){body.discard();return false;}
        var owner=world.getServer().getPlayerList().getPlayer(session.owner());
        if(owner==null){body.discard();return false;}
        finish(owner,null);owner.sendSystemMessage(Component.literal("Your body was hit. You returned to it."));
        return owner.hurtServer(owner.level(),source,amount);
    }
    private static boolean eligible(ServerPlayer player,LivingEntity victim){
        if(victim==player||!victim.isAlive()||victim.isInvulnerable()||victim.entityTags().contains(BODY_TAG)||victim.level()!=player.level())return false;
        return !(victim instanceof Player target)||(!target.isCreative()&&!target.isSpectator()&&player.level().getGameRules().get(net.minecraft.world.level.gamerules.GameRules.PVP)&&player.canHarmPlayer(target));
    }
    public static boolean strike(ServerPlayer player,LivingEntity victim){
        if(!active(player)||!player.getMainHandItem().is(AccelerationMod.SPEAR)||!eligible(player,victim)
                ||!player.isWithinEntityInteractionRange(victim,0))return false;
        ServerLevel world=player.level();Vec3 destination=landing(world,player,victim);
        if(destination==null){player.sendOverlayMessage(Component.literal("No clear landing space beside that target."));return false;}
        // End projection before applying one hit: no repeated packet can pop a second totem.
        finish(player,destination);
        DamageSource source=new DamageSource(world.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(STRIKE),player);
        victim.invulnerableTime=0;boolean hit=victim.hurtServer(world,source,1_000_000F);
        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,victim.getX(),victim.getY()+1,victim.getZ(),30,.4,.8,.4,.08);
        world.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.PLAYER_ATTACK_CRIT,SoundSource.PLAYERS,1,1);
        return hit;
    }
    private static Vec3 landing(ServerLevel world,ServerPlayer player,LivingEntity target){
        for(Vec3 offset:new Vec3[]{new Vec3(1.6,0,0),new Vec3(-1.6,0,0),new Vec3(0,0,1.6),new Vec3(0,0,-1.6),new Vec3(0,1,0)}){
            Vec3 position=target.position().add(offset);
            var box=player.getBoundingBox().move(position.subtract(player.position()));
            if(world.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(position))&&world.noCollision(player,box)
                    &&!world.containsAnyLiquid(box))return position;
        }
        return null;
    }
}
