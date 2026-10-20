package dev.scyer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;

public final class ScyerCommands {
    private ScyerCommands() {}
    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> dispatcher.register(
            Commands.literal("scyer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("reset").executes(context -> {
                    var source = context.getSource();
                    ScyerPopulation.get(source.getServer()).reset(source.getServer());
                    source.sendSuccess(() -> Component.literal("Scyer slot cleared in every dimension, including unloaded creatures. No loot or level upgrade. Use an egg or /scyer spawn 1."), false);
                    return 1;
                }))
                .then(Commands.literal("spawn")
                    .executes(context -> spawn(context.getSource(), 1))
                    .then(Commands.argument("level", IntegerArgumentType.integer(1, 4))
                        .executes(context -> spawn(context.getSource(), IntegerArgumentType.getInteger(context, "level")))))));
    }
    private static int status(CommandSourceStack source) {
        var population = ScyerPopulation.get(source.getServer());
        source.sendSuccess(() -> Component.literal("Scyer slots: " + population.size() + "/1. Difficulty: " + source.getLevel().getDifficulty() + "."), false);
        for (var level : source.getServer().getAllLevels()) level.getAllEntities().forEach(entity -> {
            if (entity instanceof ScyerEntity scyer)
                source.sendSuccess(() -> Component.literal("Level " + scyer.stage() + " at " + scyer.blockPosition().toShortString()
                    + " in " + level.dimension().identifier() + "."), false);
        });
        return 1;
    }
    /** An explicit operator spawn replaces the old creature, while preserving the one-creature limit. */
    public static int spawn(CommandSourceStack source, int stage) {
        var level = source.getLevel();
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            source.sendFailure(Component.literal("Scyer cannot survive Peaceful. Run /difficulty normal, then /scyer spawn " + stage + ".")); return 0;
        }
        var scyer = ScyerMod.TYPES.get(stage - 1).create(level, EntitySpawnReason.COMMAND);
        if (scyer == null) return 0;
        BlockPos origin = BlockPos.containing(source.getPosition());
        for (int[] offset : new int[][]{{0,0,6},{6,0,0},{0,0,-6},{-6,0,0},{3,0,3},{-3,0,-3}}) {
            for (int dy : new int[]{0,1,-1,2,-2}) {
                BlockPos feet = origin.offset(offset[0],dy,offset[2]);
                if (!level.getWorldBorder().isWithinBounds(feet) || !level.getBlockState(feet.below()).isSolidRender()) continue;
                scyer.setPos(Vec3.atBottomCenterOf(feet));
                if (!level.noCollision(scyer) || level.containsAnyLiquid(scyer.getBoundingBox())) continue;
                ScyerPopulation.get(source.getServer()).reset(source.getServer());
                ScyerSpawning.prepareStalkingChunk(level, feet);
                if (level.addFreshEntity(scyer) && !scyer.isRemoved()) {
                    source.sendSuccess(() -> Component.literal("Spawned level " + stage + " Scyer at " + feet.toShortString() + ". The previous Scyer was replaced."), false); return 1;
                }
                source.sendFailure(Component.literal("Could not add Scyer. Try /scyer reset and retry on open ground.")); return 0;
            }
        }
        source.sendFailure(Component.literal("No safe space nearby. Stand on open, dry ground with at least four blocks of headroom."));
        return 0;
    }
}
