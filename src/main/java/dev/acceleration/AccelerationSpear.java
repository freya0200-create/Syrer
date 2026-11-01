package dev.acceleration;
import net.minecraft.core.component.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
public final class AccelerationSpear extends Item {
    public static final net.minecraft.world.item.component.AttackRange CLOSE_RANGE=new net.minecraft.world.item.component.AttackRange(0,4.5F,0,6.5F,.125F,.5F);
    public AccelerationSpear(Properties properties){super(properties);}
    private static Component redName(){return Component.translatable("item.acceleration.acceleration_spear").withStyle(ChatFormatting.RED);}
    private static net.minecraft.world.item.component.ItemAttributeModifiers combatAttributes(){
        return net.minecraft.world.item.component.ItemAttributeModifiers.builder()
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,ToolMaterial.DIAMOND.attackDamageBonus(),net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID,6,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
            .add(net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(AccelerationMod.id("spear_interaction_reach"),1.5,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build();
    }
    public static net.minecraft.world.item.component.KineticWeapon fastKineticWeapon(){
        return new net.minecraft.world.item.component.KineticWeapon(2,3,
            net.minecraft.world.item.component.KineticWeapon.Condition.ofAttackerSpeed(60,10F),
            net.minecraft.world.item.component.KineticWeapon.Condition.ofAttackerSpeed(130,5.1F),
            net.minecraft.world.item.component.KineticWeapon.Condition.ofRelativeSpeed(200,4.6F),
            .38F,1.075F,java.util.Optional.of(net.minecraft.sounds.SoundEvents.SPEAR_USE),java.util.Optional.of(net.minecraft.sounds.SoundEvents.SPEAR_HIT));
    }
    public static Properties properties(){
        return new Properties().spear(ToolMaterial.DIAMOND,.1F,1.075F,.15F,3F,10F,6.5F,5.1F,10F,4.6F)
            .setId(ResourceKey.create(Registries.ITEM,AccelerationMod.id("acceleration_spear")))
            .component(DataComponents.ITEM_NAME,redName())
            .component(DataComponents.KINETIC_WEAPON,fastKineticWeapon())
            .component(DataComponents.ATTACK_RANGE,CLOSE_RANGE)
            .component(DataComponents.ATTRIBUTE_MODIFIERS,combatAttributes());
    }
    public static ItemStack create(ServerLevel world){ItemStack stack=new ItemStack(AccelerationMod.SPEAR);bless(world,stack);return stack;}
    public static void bless(ServerLevel world,ItemStack stack){
        var registry=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        registry.listElements().forEach(holder->{if(!holder.is(EnchantmentTags.CURSE)&&holder.value().isSupportedItem(stack))stack.enchant(holder,holder.value().getMaxLevel());});
        var piercing=registry.getOrThrow(Enchantments.PIERCING);stack.enchant(piercing,piercing.value().getMaxLevel());
        addMaceEnchantments(world,stack);
    }
    private static void addMaceEnchantments(ServerLevel world,ItemStack stack){
        var registry=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for(var key:java.util.List.of(Enchantments.DENSITY,Enchantments.BREACH)){
            var enchantment=registry.getOrThrow(key);
            if(EnchantmentHelper.getItemEnchantmentLevel(enchantment,stack)<enchantment.value().getMaxLevel())stack.enchant(enchantment,enchantment.value().getMaxLevel());
        }
    }
    @Override public float getAttackDamageBonus(Entity target,float damage,net.minecraft.world.damagesource.DamageSource source){
        return source.getDirectEntity() instanceof net.minecraft.world.entity.LivingEntity attacker?SpearDensity.bonus(attacker,target,source):0;
    }
    @Override public void inventoryTick(ItemStack stack,ServerLevel world,Entity owner,EquipmentSlot slot){
        super.inventoryTick(stack,world,owner,slot);
        addMaceEnchantments(world,stack);
        var kinetic=fastKineticWeapon();
        if(!kinetic.equals(stack.get(DataComponents.KINETIC_WEAPON)))stack.set(DataComponents.KINETIC_WEAPON,kinetic);
        var swing=new net.minecraft.world.item.component.SwingAnimation(net.minecraft.world.item.SwingAnimationType.STAB,2);
        if(!swing.equals(stack.get(DataComponents.SWING_ANIMATION)))stack.set(DataComponents.SWING_ANIMATION,swing);
        if(!CLOSE_RANGE.equals(stack.get(DataComponents.ATTACK_RANGE)))stack.set(DataComponents.ATTACK_RANGE,CLOSE_RANGE);
        var attributes=combatAttributes();
        if(!attributes.equals(stack.get(DataComponents.ATTRIBUTE_MODIFIERS)))stack.set(DataComponents.ATTRIBUTE_MODIFIERS,attributes);
        var name=redName();
        if(!name.equals(stack.get(DataComponents.ITEM_NAME)))stack.set(DataComponents.ITEM_NAME,name);
        var lunge=world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LUNGE);
        if(EnchantmentHelper.getItemEnchantmentLevel(lunge,stack)==0)bless(world,stack);
    }
}
