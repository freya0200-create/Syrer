package dev.acceleration.mixin;
import dev.acceleration.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class GhostDamageMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void acceleration$ghostDamage(ServerLevel world,DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        LivingEntity victim=(LivingEntity)(Object)this;
        if(victim instanceof ServerPlayer player&&SoulMode.active(player)&&!SoulMode.forwardingBodyDamage(player)){ci.setReturnValue(false);return;}
        if(source.getEntity() instanceof ServerPlayer actor&&SoulMode.active(actor)
                &&!(victim instanceof ServerPlayer player&&SoulMode.forwardingBodyDamage(player)))ci.setReturnValue(false);
    }
    @Inject(method="canBeSeenAsEnemy",at=@At("HEAD"),cancellable=true)
    private void acceleration$notAnEnemy(CallbackInfoReturnable<Boolean> ci){if(ProjectionPhysics.active((LivingEntity)(Object)this))ci.setReturnValue(false);}
    @Inject(method="canBeSeenByAnyone",at=@At("HEAD"),cancellable=true)
    private void acceleration$notATarget(CallbackInfoReturnable<Boolean> ci){if(ProjectionPhysics.active((LivingEntity)(Object)this))ci.setReturnValue(false);}
}
