package dev.acceleration.mixin;
import dev.acceleration.BlockPhasing;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ScreenEffectRenderer.class)
public abstract class PhasingOverlayMixin {
    @Inject(method="getViewBlockingState",at=@At("HEAD"),cancellable=true)
    private static void acceleration$clearBlockOverlay(Player player,CallbackInfoReturnable<BlockState> ci){if(BlockPhasing.protectedInside(player))ci.setReturnValue(null);}
}
