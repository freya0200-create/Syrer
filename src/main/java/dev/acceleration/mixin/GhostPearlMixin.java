package dev.acceleration.mixin;
import dev.acceleration.SoulMode;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(ThrownEnderpearl.class)
public abstract class GhostPearlMixin {
    @Redirect(method="onHit",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerPlayer;teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer acceleration$materializeAfterLanding(ServerPlayer owner,TeleportTransition transition){
        ServerPlayer arrived=owner.teleport(transition);
        if(arrived!=null&&SoulMode.active(arrived))SoulMode.finish(arrived,arrived.position());
        return arrived;
    }
}
