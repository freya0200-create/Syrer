package dev.acceleration.mixin;
import dev.acceleration.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.*;
import net.minecraft.core.registries.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import com.llamalad7.mixinextras.sugar.Local;
@Mixin(Player.class)
public abstract class SpearDensityMixin {
    @ModifyVariable(method="stabAttack",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float acceleration$densityJab(float damage,@Local(argsOnly=true) Entity target){
        if((Object)this instanceof ServerPlayer player&&player.getMainHandItem().is(AccelerationMod.SPEAR)){
            var source=new DamageSource(player.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(DamageTypes.SPEAR),player);
            return damage+SpearDensity.bonus(player,target,source);
        }
        return damage;
    }
}
