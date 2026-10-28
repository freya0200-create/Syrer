package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Mob.class)
public abstract class GhostMobMixin {
    @Inject(method="setTarget",at=@At("HEAD"),cancellable=true)
    private void acceleration$ignoreGhost(LivingEntity target,CallbackInfo ci){if(target!=null&&ProjectionPhysics.active(target))ci.cancel();}
}
