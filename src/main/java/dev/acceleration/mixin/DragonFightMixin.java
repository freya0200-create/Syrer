package dev.acceleration.mixin;
import java.util.UUID;
import dev.acceleration.DragonReward;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EnderDragonFight.class)
public abstract class DragonFightMixin {
    @Shadow private ServerLevel level;
    @Shadow private BlockPos exitPortalLocation;
    @Shadow private UUID dragonUUID;
    @Unique private UUID acceleration$reward;
    @Inject(method="setDragonKilled",at=@At("HEAD"))
    private void acceleration$capture(EnderDragon dragon,CallbackInfo ci){acceleration$reward=dragon.getUUID().equals(dragonUUID)?dragon.getUUID():null;}
    @Inject(method="setDragonKilled",at=@At("RETURN"))
    private void acceleration$reward(EnderDragon dragon,CallbackInfo ci){
        if(acceleration$reward!=null){DragonReward.drop(level,acceleration$reward,exitPortalLocation);acceleration$reward=null;}
    }
}
