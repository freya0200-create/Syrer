package dev.acceleration;
import java.util.*;
import net.minecraft.world.entity.player.Player;
public final class SpearHunger {
    private static final ThreadLocal<Set<UUID>> ACTIONS=ThreadLocal.withInitial(HashSet::new);
    public static boolean begin(Player player){return ACTIONS.get().add(player.getUUID());}
    public static boolean active(Player player){return ACTIONS.get().contains(player.getUUID());}
    public static void end(Player player,boolean added){if(added)ACTIONS.get().remove(player.getUUID());}
    private SpearHunger(){}
}
