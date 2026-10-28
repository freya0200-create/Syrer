package dev.acceleration.mixin;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(DragonStrafePlayerPhase.class)
public interface DragonTargetAccess {
    @Accessor("attackTarget") LivingEntity acceleration$target();
}
