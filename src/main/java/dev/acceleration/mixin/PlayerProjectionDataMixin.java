package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class PlayerProjectionDataMixin {
    @Inject(method="defineSynchedData",at=@At("TAIL"))
    private void acceleration$projectionData(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(ProjectionPhysics.ACTIVE,false);}
}
