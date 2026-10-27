package dev.acceleration.mixin;
import dev.acceleration.AccelerationClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public abstract class ClientActionMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void acceleration$soulAttack(CallbackInfoReturnable<Boolean> ci){
        if(AccelerationClient.soul){AccelerationClient.strike();ci.setReturnValue(false);}
    }
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void acceleration$soulUse(CallbackInfo ci){
        if(AccelerationClient.soul){AccelerationClient.strike();ci.cancel();}
    }
}
