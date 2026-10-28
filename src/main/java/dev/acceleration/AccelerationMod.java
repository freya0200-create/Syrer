package dev.acceleration;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;

public final class AccelerationMod implements ModInitializer {
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath("acceleration",path);}
    public static final Item SPEAR=Registry.register(BuiltInRegistries.ITEM,id("acceleration_spear"),new AccelerationSpear(AccelerationSpear.properties()));
    public static final net.minecraft.world.entity.EntityType<PlayerBody> BODY=Registry.register(BuiltInRegistries.ENTITY_TYPE,id("player_body"),
        net.minecraft.world.entity.EntityType.Builder.<PlayerBody>of(PlayerBody::create,net.minecraft.world.entity.MobCategory.MISC)
            .sized(.6F,1.8F).eyeHeight(1.62F).noSummon().noLootTable().clientTrackingRange(32)
            .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE,id("player_body"))));
    private static final Identifier SPEED=id("inventory_acceleration");
    private static final Map<UUID,Long> CHARGE=new HashMap<>();
    public static boolean carrying(ServerPlayer player){
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++)if(player.getInventory().getItem(slot).is(SPEAR))return true;
        return false;
    }
    public static void accelerationTick(ServerPlayer player){
        long before=CHARGE.getOrDefault(player.getUUID(),0L);
        long after=AccelerationRules.charge(before,carrying(player)&&player.isAlive()&&!player.isSpectator(),player.isSprinting());
        if(after==0)CHARGE.remove(player.getUUID());else CHARGE.put(player.getUUID(),after);
        var speed=player.getAttribute(Attributes.MOVEMENT_SPEED);speed.removeModifier(SPEED);
        if(after>0)speed.addTransientModifier(new AttributeModifier(SPEED,AccelerationRules.speedBonus(after),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    public static long charge(ServerPlayer player){return CHARGE.getOrDefault(player.getUUID(),0L);}
    @Override public void onInitialize(){
        ProjectionPhysics.initialize();
        ProjectionActions.initialize();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(BODY,net.minecraft.world.entity.LivingEntity.createLivingAttributes());
        SoulPackets.initialize();
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player,world,hand,target,hit)->
            ProjectionPhysics.active(target)?net.minecraft.world.InteractionResult.FAIL:net.minecraft.world.InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(ProjectionPhysics.active(entity))return net.minecraft.world.InteractionResult.FAIL;
            if(!ProjectionPhysics.active(player))return net.minecraft.world.InteractionResult.PASS;
            if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND||!player.getMainHandItem().is(SPEAR))return net.minecraft.world.InteractionResult.FAIL;
            if(world.isClientSide())return net.minecraft.world.InteractionResult.PASS; // Let vanilla send the entity-use packet.
            if(player instanceof ServerPlayer actor)
                return SoulMode.strike(actor,entity)?net.minecraft.world.InteractionResult.SUCCESS:net.minecraft.world.InteractionResult.FAIL;
            return net.minecraft.world.InteractionResult.FAIL;
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output->output.accept(SPEAR));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(ServerPlayer player:server.getPlayerList().getPlayers()){accelerationTick(player);SoulMode.tick(player);}});
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->SoulMode.finish(handler.player,null));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{SoulMode.finish(handler.player,null);CHARGE.remove(handler.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(ServerPlayer player:List.copyOf(server.getPlayerList().getPlayers()))SoulMode.finish(player,null);CHARGE.clear();});
        ServerEntityEvents.ENTITY_LOAD.register((entity,world)->{
            if(entity.entityTags().contains(SoulMode.BODY_TAG)){
                var session=SoulState.get(world.getServer()).body(entity.getUUID());
                if(session==null||world.getServer().getPlayerList().getPlayer(session.owner())==null)entity.discard();
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->dispatcher.register(Commands.literal("acceleration")
            .then(Commands.literal("return").executes(context->SoulMode.finish(context.getSource().getPlayerOrException(),null)?1:0))
            .then(Commands.literal("give").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context->{
                var player=context.getSource().getPlayerOrException();var stack=AccelerationSpear.create(player.level());
                if(!player.getInventory().add(stack))player.drop(stack,false);context.getSource().sendSuccess(()->Component.literal("Ghost Spear granted."),false);return 1;
            }))
            .then(Commands.literal("project").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(context->{
                var player=context.getSource().getPlayerOrException();return SoulMode.begin(player,player.position())?1:0;
            }))));
    }
}
