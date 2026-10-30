package dev.acceleration.mixin;
import dev.acceleration.BlockPhasing;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class BlockPhasingMixin {
    @Inject(method="collide",at=@At("RETURN"),cancellable=true)
    private void acceleration$phaseBlocks(Vec3 movement,CallbackInfoReturnable<Vec3> ci){
        if((Object)this instanceof Player player&&BlockPhasing.active(player)){
            Vec3 collision=ci.getReturnValue();
            int mode=BlockPhasing.mode(player);
            boolean walls=mode==0;
            boolean vertical=mode==1&&movement.y<0&&player.isShiftKeyDown()||mode==2&&movement.y>0;
            double y=vertical?movement.y:collision.y;
            double minimum=player.level().getMinY()+1;
            if(player.getY()+y<minimum){y=minimum-player.getY();player.fallDistance=0;}
            ci.setReturnValue(new Vec3(walls?movement.x:collision.x,y,walls?movement.z:collision.z));
        }
    }
    @Inject(method="isInWall",at=@At("HEAD"),cancellable=true)
    private void acceleration$noSuffocation(CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof Player player&&BlockPhasing.active(player))ci.setReturnValue(false);
    }
}
