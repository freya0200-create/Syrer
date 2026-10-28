package dev.acceleration.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.acceleration.SpearHunger;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(Player.class)
public abstract class FreeSpearAttackMixin {
    @WrapMethod(method="attack")
    private void acceleration$freeSpearAttack(Entity target,Operation<Void> original){
        Player player=(Player)(Object)this;
        if(!player.getMainHandItem().is(ItemTags.SPEARS)){original.call(target);return;}
        boolean added=SpearHunger.begin(player);try{original.call(target);}finally{SpearHunger.end(player,added);}
    }
    @WrapMethod(method="stabAttack")
    private boolean acceleration$freeStab(EquipmentSlot slot,Entity target,float damage,boolean hurts,boolean knocks,boolean dismounts,Operation<Boolean> original){
        Player player=(Player)(Object)this;
        if(!player.getItemBySlot(slot).is(ItemTags.SPEARS))return original.call(slot,target,damage,hurts,knocks,dismounts);
        boolean added=SpearHunger.begin(player);try{return original.call(slot,target,damage,hurts,knocks,dismounts);}finally{SpearHunger.end(player,added);}
    }
}
