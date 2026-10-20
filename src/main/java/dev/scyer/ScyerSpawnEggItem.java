package dev.scyer;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/** Creative eggs replace the previous Scyer instead of silently failing against a hidden slot. */
public final class ScyerSpawnEggItem extends SpawnEggItem {
    public ScyerSpawnEggItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel world)) return super.useOn(context);
        var player = context.getPlayer();
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            if (player != null) player.sendSystemMessage(Component.literal("Scyer cannot survive Peaceful. Run /difficulty normal first."));
            return InteractionResult.FAIL;
        }
        var feet = context.getClickedPos();
        if (!world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()) feet = feet.relative(context.getClickedFace());
        var box = new AABB(feet.getX()-.1, feet.getY(), feet.getZ()-.1, feet.getX()+1.1, feet.getY()+4, feet.getZ()+1.1);
        if (!world.noCollision(box) || world.containsAnyLiquid(box)) {
            if (player != null) player.sendSystemMessage(Component.literal("Scyer needs an open, dry space four blocks tall and at least two blocks wide."));
            return InteractionResult.FAIL;
        }
        var population = ScyerPopulation.get(world.getServer());
        if (population.size() != 0) {
            if (player != null && player.isCreative()) {
                population.reset(world.getServer());
                player.sendSystemMessage(Component.literal("Replaced the previous Scyer. Only one can exist at a time."));
            } else {
                if (player != null) player.sendSystemMessage(Component.literal("A Scyer already exists somewhere in this world. An operator can use /scyer reset to clear it."));
                return InteractionResult.FAIL;
            }
        }
        var result = super.useOn(context);
        if (population.size() == 0 && player != null)
            player.sendSystemMessage(Component.literal("Scyer did not spawn here. Try open ground, or /scyer spawn 1 with cheats enabled."));
        return result;
    }
}
