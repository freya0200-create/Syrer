package dev.scyer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public final class ScyerSpawning {
    private static final net.minecraft.server.level.TicketType STALKING_TICKET = net.minecraft.core.Registry.register(
        net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE,
        net.minecraft.resources.Identifier.fromNamespaceAndPath("scyer", "stalking"),
        new net.minecraft.server.level.TicketType(80, net.minecraft.server.level.TicketType.FLAG_LOADING
            | net.minecraft.server.level.TicketType.FLAG_SIMULATION | net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE));
    private ScyerSpawning() {}
    public static void keepAwake(ServerLevel level, BlockPos pos) {
        level.getChunkSource().addTicketWithRadius(STALKING_TICKET, new net.minecraft.world.level.ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), 2);
    }
    public static void prepareStalkingChunk(ServerLevel level, BlockPos pos) {
        var ready = level.getChunkSource().addTicketAndLoadWithRadius(STALKING_TICKET,
            new net.minecraft.world.level.ChunkPos(pos.getX() >> 4, pos.getZ() >> 4), 2);
        level.getServer().managedBlock(ready::isDone);
        ready.join();
    }
    public static void initialize() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ScyerEntity && !ScyerPopulation.get(level.getServer()).admit(entity.getUUID())) {
                entity.discard();
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            if (entity instanceof ScyerEntity && entity.getRemovalReason() != null
                    && entity.getRemovalReason().shouldDestroy()) {
                ScyerPopulation.get(level.getServer()).release(entity.getUUID());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 600 != 0 || ScyerPopulation.get(server).size() >= ScyerPopulation.LIMIT) return;
            ServerLevel level = server.overworld();
            if (!level.getGameRules().get(GameRules.SPAWN_MOBS) || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)
                    || level.getDifficulty() == Difficulty.PEACEFUL) return;
            for (Player player : level.players()) {
                if (player.isAlive() && !player.isCreative() && !player.isSpectator()
                        && trySpawnNear(level, player, level.getRandom()) != null) break;
            }
        });
    }

    public static boolean farEnoughFromPlayers(ServerLevel level, Vec3 position) {
        return level.players().stream().filter(Entity::isAlive)
            .noneMatch(player -> player.position().distanceToSqr(position) < 50 * 50);
    }

    public static boolean naturalRules(EntityType<ScyerEntity> type, ServerLevelAccessor accessor,
            EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        ServerLevel level = accessor.getLevel();
        return ScyerPopulation.get(level.getServer()).size() < ScyerPopulation.LIMIT
            && farEnoughFromPlayers(level, Vec3.atBottomCenterOf(pos))
            && level.players().stream().anyMatch(player -> !player.isCreative() && !player.isSpectator()
                && player.distanceToSqr(Vec3.atBottomCenterOf(pos)) <= 100 * 100)
            && Monster.checkMonsterSpawnRules(type, accessor, reason, pos, random);
    }

    /** Automatic hunters can appear in daylight, but only in loaded, dry, collision-free spaces. */
    public static ScyerEntity trySpawnNear(ServerLevel level, Player player, RandomSource random) {
        if (ScyerPopulation.get(level.getServer()).size() >= ScyerPopulation.LIMIT
                || !player.isAlive() || player.isCreative() || player.isSpectator()
                || level.getDifficulty() == Difficulty.PEACEFUL) return null;
        for (int attempt = 0; attempt < 40; attempt++) {
            BlockPos candidate = candidateNear(player, random);
            if (!level.hasChunkAt(candidate)) continue;
            for (int dy = 8; dy >= -8; dy--) {
                BlockPos feet = candidate.offset(0, dy, 0);
                Vec3 position = Vec3.atBottomCenterOf(feet);
                if (!farEnoughFromPlayers(level, position)
                        || !level.getBlockState(feet.below()).isSolidRender()
                        || !level.getWorldBorder().isWithinBounds(feet)) continue;
                ScyerEntity scyer = ScyerMod.SCYER.create(level, EntitySpawnReason.EVENT);
                if (scyer == null) return null;
                scyer.setStage(ScyerEvolution.get(level.getServer()).nextStage());
                scyer.setPos(position);
                if (!level.noCollision(scyer) || level.containsAnyLiquid(scyer.getBoundingBox())) continue;
                prepareStalkingChunk(level, feet);
                if (level.addFreshEntity(scyer) && scyer.isAlive()) {
                    // Hearing supplements the immediate 100-block tracker.
                    scyer.hear(player.position(), player);
                    return scyer;
                }
                return null;
            }
        }
        return null;
    }

    public static BlockPos candidateNear(Player player, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double radius = 52 + random.nextDouble() * 12;
        return BlockPos.containing(player.getX() + Math.cos(angle) * radius, player.getY(),
            player.getZ() + Math.sin(angle) * radius);
    }
}
