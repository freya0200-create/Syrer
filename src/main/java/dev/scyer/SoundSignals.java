package dev.scyer;

import java.util.Comparator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Routes real server sounds and vibration events to nearby Scyers. */
public final class SoundSignals {
    private static final ThreadLocal<Boolean> MUTED = ThreadLocal.withInitial(() -> false);
    private SoundSignals() {}

    public static void withoutSelfEcho(Runnable sound) {
        boolean previous = MUTED.get();
        MUTED.set(true);
        try { sound.run(); } finally { MUTED.set(previous); }
    }

    public static void emit(ServerLevel level, Vec3 position, Entity source) {
        if (MUTED.get() || source instanceof ScyerEntity) return;
        double radius = 100;
        for (ScyerEntity scyer : level.getEntitiesOfClass(ScyerEntity.class,
                new AABB(position, position).inflate(radius), Entity::isAlive)) {
            scyer.hear(position, source);
        }
    }

    // Coordinate-based sound packets have no owner. Attribute them only to an actor
    // actually standing at that sound, never to an arbitrary player elsewhere.
    public static Entity actorAt(ServerLevel level, Vec3 position) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(position, position).inflate(2),
                actor -> actor.isAlive() && !(actor instanceof ScyerEntity)
                    && (!(actor instanceof Player p) || (!p.isCreative() && !p.isSpectator())))
            .stream().min(Comparator.<LivingEntity>comparingInt(actor -> actor instanceof Player ? 0 : 1)
                .thenComparingDouble(actor -> actor.position().distanceToSqr(position))).orElse(null);
    }
}
