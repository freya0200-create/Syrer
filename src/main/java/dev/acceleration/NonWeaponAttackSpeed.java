package dev.acceleration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;

public final class NonWeaponAttackSpeed {
    public static final AttributeModifier MODIFIER=new AttributeModifier(AccelerationMod.id("non_weapon_attack_speed"),6,AttributeModifier.Operation.ADD_VALUE);
    private NonWeaponAttackSpeed(){}
    public static boolean eligible(ItemStack stack){
        if(stack.isEmpty()||stack.has(DataComponents.WEAPON)||stack.has(DataComponents.KINETIC_WEAPON)
                ||stack.has(DataComponents.PIERCING_WEAPON)||stack.getItem() instanceof ProjectileWeaponItem)return false;
        var modifiers=stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if(modifiers!=null)for(var entry:modifiers.modifiers())
            if(entry.attribute().equals(Attributes.ATTACK_SPEED)||entry.attribute().equals(Attributes.ATTACK_DAMAGE))return false;
        return true;
    }
}
