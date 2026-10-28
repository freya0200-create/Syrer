package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class GhostPickMixin {
    @Inject(method="isPickable",at=@At("HEAD"),cancellable=true)
    private void acceleration$cannotSelectGhost(CallbackInfoReturnable<Boolean> ci){
        if(ProjectionPhysics.active((Player)(Object)this))ci.setReturnValue(false);
    }
}
