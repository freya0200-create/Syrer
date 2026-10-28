package dev.acceleration.mixin;
import net.minecraft.world.level.dimension.end.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(EnderDragonFight.class)
public interface DragonFightAccess {
    @Accessor("dragonKilled") boolean acceleration$isKilled();
    @Accessor("dragonKilled") void acceleration$setKilled(boolean killed);
    @Accessor("respawnStage") DragonRespawnStage acceleration$respawnStage();
    @Invoker("createNewDragon") EnderDragon acceleration$create();
    @Invoker("spawnExitPortal") void acceleration$portal(boolean active);
}
