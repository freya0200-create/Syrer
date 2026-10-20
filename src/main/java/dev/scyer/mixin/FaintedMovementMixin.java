package dev.scyer.mixin;

import dev.scyer.Fainting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class FaintedMovementMixin {
    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER), cancellable = true)
    private void scyer$freezeMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        var player = ((ServerGamePacketListenerImpl)(Object)this).player;
        if (Fainting.isFainted(player)) {
            Fainting.holdStill(player);
            ci.cancel();
        }
    }
}
