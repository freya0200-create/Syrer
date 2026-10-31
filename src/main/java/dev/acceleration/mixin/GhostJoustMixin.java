package dev.acceleration.mixin;
import dev.acceleration.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class GhostJoustMixin {
    @Inject(method="stabAttack",at=@At("HEAD"),cancellable=true)
    private void acceleration$ghostContactAlwaysStrikes(EquipmentSlot slot,Entity target,float damage,boolean hurts,boolean knocks,boolean dismounts,CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof ServerPlayer player&&slot==EquipmentSlot.MAINHAND&&SoulMode.active(player)
                &&player.getMainHandItem().is(AccelerationMod.SPEAR)
                &&(player.isUsingItem()&&player.getUseItem().is(AccelerationMod.SPEAR)||GhostSpearInput.recent(player)))
            // Kinetic contacts may be knockback-only; ghost contacts must still strike.
            ci.setReturnValue(SoulMode.strikeFromJoust(player,target));
    }
}
