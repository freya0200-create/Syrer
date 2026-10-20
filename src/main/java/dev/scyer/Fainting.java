package dev.scyer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Authoritative 2.5-second movement/action lock; blindness supplies the fainting visual. */
public final class Fainting {
    private record Faint(ResourceKey<Level> level, Vec3 anchor, long until) {}
    private static final Map<UUID, Faint> ACTIVE = new HashMap<>();
    private Fainting() {}
    public static void initialize() {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
        AttackEntityCallback.EVENT.register((p,l,h,e,hit) -> result(p));
        AttackBlockCallback.EVENT.register((p,l,h,pos,face) -> result(p));
        UseEntityCallback.EVENT.register((p,l,h,e,hit) -> result(p));
        UseBlockCallback.EVENT.register((p,l,h,hit) -> result(p));
        UseItemCallback.EVENT.register((p,l,h) -> result(p));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isFainted(player)) holdStill(player);
            }
            ACTIVE.entrySet().removeIf(entry -> server.getPlayerList().getPlayer(entry.getKey()) == null);
        });
    }
    private static InteractionResult result(Player player) {
        return isFainted(player) ? InteractionResult.FAIL : InteractionResult.PASS;
    }
    public static void apply(ServerPlayer player) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) return;
        ACTIVE.put(player.getUUID(), new Faint(player.level().dimension(), player.position(), player.level().getGameTime() + 50));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 255, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 255, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 50, 255, false, false, true));
        player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.scyer.roar"));
        player.stopUsingItem();
        player.setDeltaMovement(Vec3.ZERO);
    }
    public static boolean isFainted(Player player) {
        Faint faint = ACTIVE.get(player.getUUID());
        if (faint == null) return false;
        if (!player.isAlive() || player.isCreative() || player.isSpectator()
                || !player.level().dimension().equals(faint.level) || player.level().getGameTime() >= faint.until) {
            ACTIVE.remove(player.getUUID());
            return false;
        }
        return true;
    }
    public static void holdStill(ServerPlayer player) {
        if (!isFainted(player)) return;
        Faint faint = ACTIVE.get(player.getUUID());
        player.setDeltaMovement(Vec3.ZERO);
        if (player.position().distanceToSqr(faint.anchor) > .0001) {
            player.teleportTo(faint.anchor.x, faint.anchor.y, faint.anchor.z);
        }
    }
}
