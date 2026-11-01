package dev.acceleration;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
public final class SpearDensity {
    private SpearDensity(){}
    public static float bonus(LivingEntity attacker,Entity target,DamageSource source){
        if(!(attacker.level() instanceof ServerLevel world)||attacker.fallDistance<=1.5||attacker.isFallFlying()||!attacker.getMainHandItem().is(AccelerationMod.SPEAR))return 0;
        return EnchantmentHelper.modifyFallBasedDamage(world,attacker.getMainHandItem(),target,source,0)*(float)attacker.fallDistance;
    }
}
