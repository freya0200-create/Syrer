package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.warden.Warden;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Warden.class)
public abstract class GhostWardenMixin {
    @Inject(method="canTargetEntity",at=@At("HEAD"),cancellable=true)
    private void acceleration$ignoreGhost(Entity entity,CallbackInfoReturnable<Boolean> ci){if(ProjectionPhysics.active(entity))ci.setReturnValue(false);}
}
