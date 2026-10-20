package dev.scyer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ScyerMod implements ModInitializer {
    private static EntityType<ScyerEntity> type(String id, int stage) {
        var name = Identifier.fromNamespaceAndPath("scyer", id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, name,
            EntityType.Builder.<ScyerEntity>of((type, world) -> new ScyerEntity(type, world, stage), MobCategory.MONSTER)
                .fireImmune().sized(1.2F, 4F).eyeHeight(3.55F).clientTrackingRange(10)
                .build(ResourceKey.create(Registries.ENTITY_TYPE, name)));
    }
    private static Item egg(String id, EntityType<ScyerEntity> type) {
        var name = Identifier.fromNamespaceAndPath("scyer", id);
        return Registry.register(BuiltInRegistries.ITEM, name,
            new ScyerSpawnEggItem(new Item.Properties().spawnEgg(type).setId(ResourceKey.create(Registries.ITEM, name))));
    }
    public static final EntityType<ScyerEntity> SCYER = type("scyer", 1);
    public static final EntityType<ScyerEntity> SCYER_2 = type("scyer_level_2", 2);
    public static final EntityType<ScyerEntity> SCYER_3 = type("scyer_level_3", 3);
    public static final EntityType<ScyerEntity> SCYER_4 = type("scyer_level_4", 4);
    public static final java.util.List<EntityType<ScyerEntity>> TYPES = java.util.List.of(SCYER, SCYER_2, SCYER_3, SCYER_4);
    public static final Item SPAWN_EGG = egg("scyer_spawn_egg", SCYER);
    public static final java.util.List<Item> EGGS = java.util.List.of(SPAWN_EGG,
        egg("scyer_level_2_spawn_egg", SCYER_2), egg("scyer_level_3_spawn_egg", SCYER_3), egg("scyer_level_4_spawn_egg", SCYER_4));

    @Override
    public void onInitialize() {
        ScyerGear.initialize();
        ScyerCommands.initialize();
        ScyerSpawning.initialize();
        Fainting.initialize();
        TYPES.forEach(type -> FabricDefaultAttributeRegistry.register(type, ScyerEntity.attributes()));
        SpawnPlacements.register(SCYER, SpawnPlacementTypes.ON_GROUND,
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ScyerSpawning::naturalRules);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(),
            MobCategory.MONSTER, SCYER, 8, 1, 1);
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
            .register(output -> EGGS.forEach(output::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
            .register(output -> ScyerGear.ITEMS.values().forEach(output::accept));
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(output -> output.accept(ScyerGear.URU_PICKAXE));
    }
}
