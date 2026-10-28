package dev.acceleration.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.acceleration.GhostWind;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerExplosion.class)
public abstract class GhostWindExplosionMixin {
    @Shadow @Final private Entity source;
    @ModifyExpressionValue(method="hurtEntities",at=@At(value="INVOKE",target="Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 acceleration$stackAndSyncSelfWind(Vec3 impulse,@Local Entity target){
        // Adjust once before vanilla uses the vector for both physics and its client packet.
        return GhostWind.impulse(source,target,impulse);
    }
}
