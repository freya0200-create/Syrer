package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Entity.class)
public abstract class EntityProjectionCollisionMixin {
    @Inject(method="push(Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"),cancellable=true)
    private void acceleration$noContactPush(Entity other,CallbackInfo ci){
        if(ProjectionPhysics.active((Entity)(Object)this)||ProjectionPhysics.active(other))ci.cancel();
    }
    @Inject(method="canCollideWith",at=@At("HEAD"),cancellable=true)
    private void acceleration$noEntityCollision(Entity other,CallbackInfoReturnable<Boolean> ci){
        if(ProjectionPhysics.active((Entity)(Object)this)||ProjectionPhysics.active(other))ci.setReturnValue(false);
    }
}
