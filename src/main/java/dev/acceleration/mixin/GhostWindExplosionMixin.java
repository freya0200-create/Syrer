package dev.acceleration.mixin;
import java.util.Map;
import dev.acceleration.GhostWind;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerExplosion.class)
public abstract class GhostWindExplosionMixin {
    @Shadow @Final private Entity source;
    @Redirect(method="hurtEntities",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/phys/Vec3;)V"))
    private void acceleration$stackSelfWind(Entity target,Vec3 impulse){target.push(GhostWind.impulse(source,target,impulse));}
    @Redirect(method="hurtEntities",at=@At(value="INVOKE",target="Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    @SuppressWarnings({"rawtypes","unchecked"})
    private Object acceleration$syncStackedImpulse(Map map,Object target,Object impulse){
        // Vanilla adds this vector on the client too. Send the same adjusted impulse.
        if(target instanceof Entity entity&&impulse instanceof Vec3 vector)return map.put(target,GhostWind.impulse(source,entity,vector));
        return map.put(target,impulse);
    }
}
