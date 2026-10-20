package dev.scyer;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** The next natural/stalking Scyer gets stronger after an actual death, never on chunk unload. */
public final class ScyerEvolution extends SavedData {
    public static final Codec<ScyerEvolution> CODEC = Codec.INT.xmap(ScyerEvolution::new, ScyerEvolution::nextStage);
    private static final SavedDataType<ScyerEvolution> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("scyer", "evolution"), ScyerEvolution::new, CODEC, null);
    private int next;
    public ScyerEvolution() { this(1); }
    private ScyerEvolution(int next) { this.next = Math.clamp(next, 1, 4); }
    public static ScyerEvolution get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public int nextStage() { return next; }
    public void recordDeath(int stage) { next = Math.min(4, Math.max(next + 1, stage + 1)); setDirty(); }
}
