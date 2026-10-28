package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(LivingEntity.class)
public abstract class LivingProjectionCollisionMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void acceleration$noContactPush(Entity other,CallbackInfo ci){
        if(ProjectionPhysics.active((Entity)(Object)this)||ProjectionPhysics.active(other))ci.cancel();
    }
    @Inject(method="isPushable",at=@At("HEAD"),cancellable=true)
    private void acceleration$notPushable(CallbackInfoReturnable<Boolean> ci){
        if(ProjectionPhysics.active((Entity)(Object)this))ci.setReturnValue(false);
    }
}
