package dev.acceleration;

import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/** Synced to every observer so server and client agree on contact physics. */
public final class ProjectionPhysics {
    public static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(Player.class,EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> PHASING=SynchedEntityData.defineId(Player.class,EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Integer> PHASE_MODE=SynchedEntityData.defineId(Player.class,EntityDataSerializers.INT);
    public static boolean active(Entity entity){return entity instanceof Player player&&player.getEntityData().get(ACTIVE);}
    public static boolean hiddenFrom(Entity entity,Player viewer){return active(entity)&&entity!=viewer;}
    public static void initialize(){}
    private ProjectionPhysics(){}
}
