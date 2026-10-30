package dev.acceleration.mixin;
import dev.acceleration.BlockPhasing;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LocalPlayer.class)
public abstract class PhasingPushOutMixin {
    @Inject(method="moveTowardsClosestSpace",at=@At("HEAD"),cancellable=true)
    private void acceleration$stayInsideWall(double x,double z,CallbackInfo ci){if(BlockPhasing.active((LocalPlayer)(Object)this))ci.cancel();}
}
