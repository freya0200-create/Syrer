package dev.acceleration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
public final class SpearCharge {
    private SpearCharge(){}
    public static boolean ready(Player player){
        if(!player.isUsingItem()||!player.getMainHandItem().is(AccelerationMod.SPEAR)||!player.getUseItem().is(AccelerationMod.SPEAR))return false;
        var kinetic=player.getUseItem().get(DataComponents.KINETIC_WEAPON);
        return kinetic!=null&&player.getTicksUsingItem()>=kinetic.delayTicks();
    }
}
