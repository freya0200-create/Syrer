package dev.scyer;

import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.equipment.*;
import net.minecraft.core.component.DataComponents;

public final class ScyerGear {
    public static final int ARMOR_DURABILITY = 5000;
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    private static final ArmorType[] TYPES = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    public static final Item[] ADAMANTIUM = armor("adamantium", 4, Map.of(ArmorType.HELMET, 4, ArmorType.CHESTPLATE, 9, ArmorType.LEGGINGS, 7, ArmorType.BOOTS, 4), .15F);
    public static final Item[] URU = armor("uru", 30, Map.of(ArmorType.HELMET, 5, ArmorType.CHESTPLATE, 12, ArmorType.LEGGINGS, 8, ArmorType.BOOTS, 5), .2F);
    private static final ToolMaterial URU_TOOLS = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4096, 18, 8, 30,
        ItemTags.REPAIRS_NETHERITE_ARMOR);
    public static final Item URU_PICKAXE = tool("uru_pickaxe", new Item.Properties().pickaxe(URU_TOOLS, 3, -2.8F));
    public static final Item URU_SWORD = tool("uru_sword", new Item.Properties().sword(URU_TOOLS, 7, -2.2F));
    public static final Item URU_AXE = register("uru_axe", new AxeItem(URU_TOOLS, 10, -3, new Item.Properties()
        .fireResistant().setId(key("uru_axe"))));
    public static final Item URU_MACE = register("uru_mace", new MaceItem(new Item.Properties().durability(4096).enchantable(30)
        .attributes(uruMaceAttributes()).component(DataComponents.TOOL, MaceItem.createToolProperties())
        .fireResistant().setId(key("uru_mace"))));
    private static net.minecraft.world.item.component.ItemAttributeModifiers uruMaceAttributes() {
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 19,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3.4,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),
                net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build();
    }
    private ScyerGear() {}
    public static void initialize() { /* Force item registration before registry freeze. */ }
    private static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("scyer", name));
    }
    private static Item register(String name, Item item) {
        Registry.register(BuiltInRegistries.ITEM, key(name).identifier(), item); ITEMS.put(name, item); return item;
    }
    private static Item tool(String name, Item.Properties properties) {
        return register(name, new Item(properties.fireResistant().setId(key(name))));
    }
    private static Item[] armor(String name, float toughness, Map<ArmorType, Integer> defense, float knockback) {
        var asset = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("scyer", name));
        var material = new ArmorMaterial(500, defense, 30, SoundEvents.ARMOR_EQUIP_NETHERITE,
            toughness, knockback, ItemTags.REPAIRS_NETHERITE_ARMOR, asset);
        Item[] result = new Item[4];
        String[] suffix = {"helmet", "chestplate", "leggings", "boots"};
        for (int i = 0; i < 4; i++) {
            String id = name + "_" + suffix[i];
            result[i] = register(id, new Item(new Item.Properties().humanoidArmor(material, TYPES[i]).durability(ARMOR_DURABILITY).fireResistant().setId(key(id))));
        }
        return result;
    }
    public static void equip(ScyerEntity entity) {
        for (int i = 0; i < SLOTS.length; i++) {
            entity.setItemSlot(SLOTS[i], entity.stage() == 1 ? ItemStack.EMPTY : new ItemStack((entity.stage() == 2 ? ADAMANTIUM : URU)[i]));
            entity.setDropChance(SLOTS[i], 0);
        }
        // Weapons are rendered as a compact cluster; their combat strength is in the entity attributes.
        entity.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }
    /** Separate rolls: multiple different weapons can drop on the same level-four death. */
    public static List<ItemStack> deathDrops(int stage, RandomSource random) {
        List<ItemStack> drops = new ArrayList<>();
        if (stage >= 2) for (Item item : stage == 2 ? ADAMANTIUM : URU) {
            ItemStack armor = new ItemStack(item);
            // Full-size armor, dropped badly damaged: repair can restore all 5,000 durability.
            armor.setDamageValue(ARMOR_DURABILITY - (20 + random.nextInt(31)));
            drops.add(armor);
        }
        if (stage == 4) {
            if (random.nextFloat() < .50F) drops.add(new ItemStack(URU_PICKAXE));
            if (random.nextFloat() < .10F) drops.add(new ItemStack(URU_SWORD));
            if (random.nextFloat() < .04F) drops.add(new ItemStack(URU_MACE));
            if (random.nextFloat() < .35F) drops.add(new ItemStack(URU_AXE));
        }
        return drops;
    }
}
