package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class GhostExhaustionMixin {
    @Inject(method="causeFoodExhaustion",at=@At("HEAD"),cancellable=true)
    private void acceleration$noExhaustion(float amount,CallbackInfo ci){if(ProjectionPhysics.active((Player)(Object)this)||dev.acceleration.SpearHunger.active((Player)(Object)this))ci.cancel();}
}
