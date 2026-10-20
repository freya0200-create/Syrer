package dev.scyer;

import java.util.*;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.saveddata.*;

/** A server-wide slot, with persistent invalidation of unloaded creatures replaced by an operator. */
public final class ScyerPopulation extends SavedData {
    public static final int LIMIT = 1;
    private static final Codec<ScyerPopulation> MODERN = RecordCodecBuilder.create(instance -> instance.group(
        UUIDUtil.STRING_CODEC.listOf().fieldOf("active").forGetter(data -> new ArrayList<>(data.ids)),
        UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("retired", List.of()).forGetter(data -> new ArrayList<>(data.retired))
    ).apply(instance, ScyerPopulation::new));
    // Read old 1.6.0 saves as well as the new reset-safe format.
    public static final Codec<ScyerPopulation> CODEC = Codec.either(MODERN, UUIDUtil.STRING_CODEC.listOf())
        .xmap(value -> value.map(data -> data, ids -> new ScyerPopulation(ids, List.of())), Either::left);
    private static final SavedDataType<ScyerPopulation> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath("scyer", "population"), ScyerPopulation::new, CODEC, null);
    private final Set<UUID> ids, retired;
    public ScyerPopulation() { this(List.of(), List.of()); }
    private ScyerPopulation(List<UUID> ids, List<UUID> retired) {
        this.ids = new HashSet<>(ids); this.retired = new HashSet<>(retired); this.ids.removeAll(this.retired);
    }
    public static ScyerPopulation get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(TYPE); }
    public boolean contains(UUID id) { return ids.contains(id); }
    public boolean retired(UUID id) { return retired.contains(id); }
    public int size() { return ids.size(); }
    public boolean admit(UUID id) {
        if (retired.contains(id)) return false;
        if (ids.contains(id)) return true;
        if (ids.size() >= LIMIT) return false;
        ids.add(id); setDirty(); return true;
    }
    public void release(UUID id) { if (ids.remove(id)) setDirty(); }
    /** Discard without loot or evolution; unloaded old UUIDs can never reclaim a reset slot. */
    public void reset(MinecraftServer server) {
        retired.addAll(ids); ids.clear();
        for (var level : server.getAllLevels()) {
            List<ScyerEntity> loaded = new ArrayList<>();
            level.getAllEntities().forEach(entity -> { if (entity instanceof ScyerEntity scyer) loaded.add(scyer); });
            for (var scyer : loaded) { retired.add(scyer.getUUID()); scyer.remove(Entity.RemovalReason.DISCARDED); }
        }
        setDirty();
    }
}
