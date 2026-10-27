package dev.acceleration.mixin;
import java.util.*;
import dev.acceleration.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EnchantmentHelper.class)
public abstract class LungeMixin {
    @Unique private static final Map<UUID,Vec3> acceleration$origins=new HashMap<>();
    @Inject(method="doPostPiercingAttackEffects",at=@At("HEAD"))
    private static void acceleration$remember(ServerLevel world,LivingEntity actor,CallbackInfo ci){
        if(actor instanceof ServerPlayer player&&player.getMainHandItem().is(AccelerationMod.SPEAR)&&!player.isPassenger()
                &&!player.isFallFlying()&&!player.isInWater()&&(player.isCreative()||player.getFoodData().getFoodLevel()>=7))
            acceleration$origins.put(player.getUUID(),player.position());
    }
    @Inject(method="doPostPiercingAttackEffects",at=@At("RETURN"))
    private static void acceleration$project(ServerLevel world,LivingEntity actor,CallbackInfo ci){
        Vec3 origin=acceleration$origins.remove(actor.getUUID());
        if(origin!=null&&actor instanceof ServerPlayer player)SoulMode.begin(player,origin);
    }
}
