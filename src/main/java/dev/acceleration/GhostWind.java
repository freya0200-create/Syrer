package dev.acceleration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.phys.Vec3;

public final class GhostWind {
    private GhostWind(){}
    public static Vec3 impulse(Entity source,Entity target,Vec3 nativeImpulse){
        if(source instanceof WindCharge charge&&target instanceof ServerPlayer owner
                &&charge.getOwner()==owner&&ProjectionPhysics.active(owner))
            return new Vec3(nativeImpulse.x,Math.max(0,nativeImpulse.y)+.5,nativeImpulse.z);
        return nativeImpulse;
    }
}
