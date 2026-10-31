package dev.acceleration.mixin;
import dev.acceleration.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class GhostBufferedAttackMixin {
    @Inject(method="attack",at=@At("HEAD"),cancellable=true)
    private void acceleration$quickRightThenLeft(Entity target,CallbackInfo ci){
        if((Object)this instanceof ServerPlayer player&&GhostSpearInput.recent(player)){
            SoulMode.strikeFromJoust(player,target);ci.cancel();
        }
    }
}
