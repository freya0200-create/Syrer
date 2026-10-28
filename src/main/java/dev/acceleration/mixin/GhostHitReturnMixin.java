package dev.acceleration.mixin;
import dev.acceleration.SoulMode;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class GhostHitReturnMixin {
    @Inject(method="hurtOrSimulate",at=@At("RETURN"))
    private void acceleration$reuniteAfterSuccessfulHit(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        SoulMode.afterMeleeHit((Entity)(Object)this,source,ci.getReturnValue());
    }
}
