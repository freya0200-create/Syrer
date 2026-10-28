package dev.acceleration;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;

/** A linked player-shaped hitbox: no AI, dialogue, inventory or independent life. */
public class PlayerBody extends Avatar {
    public static EntityType.EntityFactory<PlayerBody> CLIENT_FACTORY=PlayerBody::new;
    private static final EntityDataAccessor<ResolvableProfile> PROFILE=SynchedEntityData.defineId(PlayerBody.class,EntityDataSerializers.RESOLVABLE_PROFILE);
    public PlayerBody(EntityType<? extends PlayerBody> type,Level level){
        super(type,level);entityData.set(DATA_PLAYER_MODE_CUSTOMISATION,(byte)127);setNoGravity(true);
    }
    public static PlayerBody create(EntityType<PlayerBody> type,Level level){return level.isClientSide()?CLIENT_FACTORY.create(type,level):new PlayerBody(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);builder.define(PROFILE,ResolvableProfile.createUnresolved("Player"));
    }
    @Override public ResolvableProfile getProfile(){return entityData.get(PROFILE);}
    public void setProfile(ResolvableProfile profile){entityData.set(PROFILE,profile);}
    @Override public boolean hurtServer(ServerLevel world,DamageSource source,float amount){return SoulMode.damageBody(world,this,source,amount);}
    @Override public void tick(){
        super.tick();setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        if(level() instanceof ServerLevel world){
            var session=SoulState.get(world.getServer()).body(getUUID());
            var owner=session==null?null:world.getServer().getPlayerList().getPlayer(session.owner());
            if(owner==null){discard();return;}
            setPos(session.anchor());setHealth(owner.getHealth());
            for(EquipmentSlot slot:EquipmentSlot.values())if(slot!=EquipmentSlot.BODY&&slot!=EquipmentSlot.SADDLE)
                setItemSlot(slot,owner.getItemBySlot(slot).copy());
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.store("profile",ResolvableProfile.CODEC,getProfile());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);in.read("profile",ResolvableProfile.CODEC).ifPresent(this::setProfile);}
}
