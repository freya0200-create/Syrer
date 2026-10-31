package dev.acceleration;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;

/** Preserve a brief right-click intent when release arrives before the following attack packet. */
public final class GhostSpearInput {
    private static final Map<UUID,Long> RIGHT_CLICK=new HashMap<>();
    private GhostSpearInput(){}
    public static void arm(ServerPlayer player){
        if(SoulMode.active(player)&&player.getMainHandItem().is(AccelerationMod.SPEAR))RIGHT_CLICK.put(player.getUUID(),player.level().getGameTime());
    }
    public static boolean recent(ServerPlayer player){
        Long tick=RIGHT_CLICK.get(player.getUUID());
        if(tick==null)return false;
        long elapsed=player.level().getGameTime()-tick;
        if(elapsed<0||elapsed>6||!SoulMode.active(player)||!player.getMainHandItem().is(AccelerationMod.SPEAR)){forget(player);return false;}
        return true;
    }
    public static void forget(ServerPlayer player){RIGHT_CLICK.remove(player.getUUID());}
}
