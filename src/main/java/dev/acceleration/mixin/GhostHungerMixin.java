package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FoodData.class)
public abstract class GhostHungerMixin {
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void acceleration$freezeHunger(ServerPlayer player,CallbackInfo ci){if(ProjectionPhysics.active(player))ci.cancel();}
}
