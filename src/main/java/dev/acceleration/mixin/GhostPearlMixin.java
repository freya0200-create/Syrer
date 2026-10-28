package dev.acceleration.mixin;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.acceleration.SoulMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(ThrownEnderpearl.class)
public abstract class GhostPearlMixin {
    @ModifyExpressionValue(method="onHit",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer acceleration$materializeAfterLanding(ServerPlayer arrived){
        if(arrived!=null&&SoulMode.active(arrived))SoulMode.finish(arrived,arrived.position());
        return arrived;
    }
}
