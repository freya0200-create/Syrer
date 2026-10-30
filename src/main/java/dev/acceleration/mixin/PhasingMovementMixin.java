package dev.acceleration.mixin;
import dev.acceleration.BlockPhasing;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PhasingMovementMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="isEntityCollidingWithAnythingNew",at=@At("HEAD"),cancellable=true)
    private void acceleration$allowPhasing(LevelReader level,Entity entity,AABB box,double x,double y,double z,CallbackInfoReturnable<Boolean> ci){
        if(entity==player&&BlockPhasing.protectedInside(player))ci.setReturnValue(false);
    }
}
