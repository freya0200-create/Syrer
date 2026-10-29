package dev.acceleration;
import net.minecraft.core.component.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
public final class AccelerationSpear extends Item {
    public AccelerationSpear(Properties properties){super(properties);}
    public static Properties properties(){
        return new Properties().spear(ToolMaterial.DIAMOND,.5F,1.075F,.5F,3F,10F,6.5F,5.1F,10F,4.6F)
            .setId(ResourceKey.create(Registries.ITEM,AccelerationMod.id("acceleration_spear")));
    }
    public static ItemStack create(ServerLevel world){ItemStack stack=new ItemStack(AccelerationMod.SPEAR);bless(world,stack);return stack;}
    public static void bless(ServerLevel world,ItemStack stack){
        var registry=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        registry.listElements().forEach(holder->{if(!holder.is(EnchantmentTags.CURSE)&&holder.value().isSupportedItem(stack))stack.enchant(holder,holder.value().getMaxLevel());});
        var piercing=registry.getOrThrow(Enchantments.PIERCING);stack.enchant(piercing,piercing.value().getMaxLevel());
    }
    @Override public void inventoryTick(ItemStack stack,ServerLevel world,Entity owner,EquipmentSlot slot){
        super.inventoryTick(stack,world,owner,slot);
        var lunge=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LUNGE);
        if(EnchantmentHelper.getItemEnchantmentLevel(lunge,stack)==0)bless(world,stack);
    }
}
