package dev.acceleration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.phys.Vec3;

public final class GhostWind {
    private GhostWind(){}
    public static Vec3 impulse(Entity source,Entity target,Vec3 nativeImpulse){
        if(target instanceof ServerPlayer owner&&ProjectionPhysics.active(owner)){
            if(source instanceof WindCharge charge&&charge.getOwner()==owner)
                return new Vec3(nativeImpulse.x,Math.max(0,nativeImpulse.y)+.5,nativeImpulse.z);
            return Vec3.ZERO; // Other explosions cannot touch a ghost; own wind boosts still work.
        }
        return nativeImpulse;
    }
}
