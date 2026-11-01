package dev.acceleration;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.*;

/** Apply fire after the special hit, so a totem's activation cannot erase the ignition. */
public final class SpearFire {
    private SpearFire(){}
    public static void apply(ServerPlayer attacker,Entity target){
        if(target.fireImmune())return;
        var enchantments=attacker.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var weapon=attacker.getMainHandItem();
        int aspect=EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FIRE_ASPECT),weapon);
        int flame=EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FLAME),weapon);
        float seconds=Math.max(aspect*4F,flame>0?5F:0F);
        if(seconds>0)target.igniteForSeconds(seconds);
    }
}
