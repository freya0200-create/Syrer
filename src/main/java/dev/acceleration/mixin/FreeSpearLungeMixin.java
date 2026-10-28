package dev.acceleration.mixin;
import dev.acceleration.AccelerationMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.ApplyExhaustion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ApplyExhaustion.class)
public abstract class FreeSpearLungeMixin {
    @Inject(method="apply",at=@At("HEAD"),cancellable=true)
    private void acceleration$freeLunge(ServerLevel world,int level,EnchantedItemInUse item,Entity entity,Vec3 position,CallbackInfo ci){
        if(item.itemStack().is(AccelerationMod.SPEAR))ci.cancel();
    }
}
