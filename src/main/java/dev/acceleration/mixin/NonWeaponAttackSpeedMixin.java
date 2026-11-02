package dev.acceleration.mixin;
import java.util.function.BiConsumer;
import dev.acceleration.NonWeaponAttackSpeed;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemStack.class)
public abstract class NonWeaponAttackSpeedMixin {
    @Inject(method="forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V",at=@At("TAIL"))
    private void acceleration$fastOrdinaryItems(EquipmentSlot slot,BiConsumer<Holder<Attribute>,AttributeModifier> consumer,CallbackInfo ci){
        if(slot==EquipmentSlot.MAINHAND&&NonWeaponAttackSpeed.eligible((ItemStack)(Object)this))
            consumer.accept(Attributes.ATTACK_SPEED,NonWeaponAttackSpeed.MODIFIER);
    }
}
