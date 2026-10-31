package dev.acceleration;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
public final class ProjectionActions {
    public static void initialize(){
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,blockEntity)->{
            if(player instanceof ServerPlayer actor&&SoulMode.active(actor))SoulMode.finish(actor,actor.position());
        });
        UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(player instanceof ServerPlayer actor&&hand==net.minecraft.world.InteractionHand.MAIN_HAND&&player.getItemInHand(hand).is(AccelerationMod.SPEAR))GhostSpearInput.arm(actor);
            if(player instanceof ServerPlayer actor&&SoulMode.active(actor)&&!player.getItemInHand(hand).is(AccelerationMod.SPEAR)&&!player.getItemInHand(hand).has(DataComponents.FOOD)&&!player.getItemInHand(hand).is(Items.ENDER_PEARL)&&!player.getItemInHand(hand).is(Items.WIND_CHARGE))SoulMode.finish(actor,actor.position());
            return InteractionResult.PASS;
        });
        UseItemCallback.EVENT.register((player,world,hand)->{
            var stack=player.getItemInHand(hand);
            if(player instanceof ServerPlayer actor&&hand==net.minecraft.world.InteractionHand.MAIN_HAND&&stack.is(AccelerationMod.SPEAR))GhostSpearInput.arm(actor);
            if(player instanceof ServerPlayer actor&&SoulMode.active(actor)&&!stack.isEmpty()&&!stack.is(AccelerationMod.SPEAR)&&!stack.has(DataComponents.FOOD)&&!stack.is(Items.ENDER_PEARL)&&!stack.is(Items.WIND_CHARGE))SoulMode.finish(actor,actor.position());
            return InteractionResult.PASS;
        });
    }
    private ProjectionActions(){}
}
