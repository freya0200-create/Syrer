package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(TargetingConditions.class)
public abstract class GhostTargetMixin {
    @Inject(method="test",at=@At("HEAD"),cancellable=true)
    private void acceleration$ignoreGhost(ServerLevel world,LivingEntity source,LivingEntity target,CallbackInfoReturnable<Boolean> ci){if(ProjectionPhysics.active(target))ci.setReturnValue(false);}
}
