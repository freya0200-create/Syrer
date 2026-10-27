package dev.acceleration.mixin;
import dev.acceleration.SoulMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class BodyDamageMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void acceleration$bodyHit(ServerLevel world,DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        LivingEntity entity=(LivingEntity)(Object)this;
        if(entity.entityTags().contains(SoulMode.BODY_TAG))ci.setReturnValue(SoulMode.damageBody(world,entity,source,amount));
    }
}
