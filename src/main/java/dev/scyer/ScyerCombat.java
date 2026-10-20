package dev.scyer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.*;

public final class ScyerCombat {
    private ScyerCombat() {}
    /** Normalize density smashes against a six-block Density V reference; retain real Breach effects. */
    public static float afterArmor(ScyerEntity target, DamageSource source, float actual) {
        if (!source.is(DamageTypes.MACE_SMASH) || !(source.getEntity() instanceof LivingEntity attacker) || attacker.fallDistance <= 1.5
                || !(attacker.getMainHandItem().getItem() instanceof MaceItem)) return actual;
        var density = target.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.DENSITY);
        if (EnchantmentHelper.getItemEnchantmentLevel(density, attacker.getMainHandItem()) == 0) return actual;
        // No weapon on the reference source: actual Breach can reduce protection without being canceled out.
        float reference = CombatRules.getDamageAfterAbsorb(target, ScyerBalance.maceDamage(5, 6),
            new DamageSource(source.typeHolder()), target.getArmorValue(),
            (float)target.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        return ScyerBalance.normalizedSmash(actual, reference, target.getMaxHealth());
    }
}
