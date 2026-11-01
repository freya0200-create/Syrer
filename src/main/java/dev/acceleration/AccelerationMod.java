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
    private static final Map<UUID,Long> SPEAR_GRACE=new HashMap<>();
    private static final Set<UUID> USING_SPEAR=new HashSet<>();
    private static final Set<UUID> ENABLED=new HashSet<>();
    public static boolean accelerationEnabled(ServerPlayer player){return ENABLED.contains(player.getUUID());}
    public static boolean toggleAcceleration(ServerPlayer player){
        if(ENABLED.remove(player.getUUID())){
            CHARGE.remove(player.getUUID());SPEAR_GRACE.remove(player.getUUID());USING_SPEAR.remove(player.getUUID());player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED);
            player.sendOverlayMessage(Component.literal("Acceleration OFF"));return true;
        }
        if(!player.isAlive()||player.isSpectator()||!carrying(player))return false;
        ENABLED.add(player.getUUID());player.sendOverlayMessage(Component.literal("Acceleration ON • +12% per second while sprinting"));return true;
    }
    public static void clearAcceleration(ServerPlayer player){ENABLED.remove(player.getUUID());CHARGE.remove(player.getUUID());SPEAR_GRACE.remove(player.getUUID());USING_SPEAR.remove(player.getUUID());player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SPEED);}
    public static void spearAction(net.minecraft.world.entity.player.Player player){
        if(player instanceof ServerPlayer actor&&accelerationEnabled(actor))SPEAR_GRACE.put(actor.getUUID(),actor.level().getGameTime()+10);
    }
    public static boolean carrying(ServerPlayer player){
        for(int slot=0;slot<player.getInventory().getContainerSize();slot++)if(player.getInventory().getItem(slot).is(SPEAR))return true;
        return false;
    }
    public static void accelerationTick(ServerPlayer player){
        boolean valid=carrying(player)&&player.isAlive()&&!player.isSpectator();
        if(!valid)ENABLED.remove(player.getUUID());
        boolean enabled=valid&&accelerationEnabled(player);
        boolean usingSpear=player.isUsingItem()&&player.getUseItem().is(net.minecraft.tags.ItemTags.SPEARS);
        if(!enabled){SPEAR_GRACE.remove(player.getUUID());USING_SPEAR.remove(player.getUUID());}
        else if(usingSpear)USING_SPEAR.add(player.getUUID());
        else if(USING_SPEAR.remove(player.getUUID()))spearAction(player);
        long until=SPEAR_GRACE.getOrDefault(player.getUUID(),-1L);
        boolean grace=until>=player.level().getGameTime();
        if(!grace)SPEAR_GRACE.remove(player.getUUID());
        long before=CHARGE.getOrDefault(player.getUUID(),0L);
        long after=AccelerationRules.charge(before,enabled,player.isSprinting(),usingSpear||grace);
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
        SpeedCommands.initialize();
        AscendedDragon.initialize();
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player,world,hand,target,hit)->
            ProjectionPhysics.active(target)?net.minecraft.world.InteractionResult.FAIL:net.minecraft.world.InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player,world,hand,entity,hit)->{
            if(player.getMainHandItem().is(SPEAR))spearAction(player);
            if(ProjectionPhysics.active(entity))return net.minecraft.world.InteractionResult.FAIL;
            if(!ProjectionPhysics.active(player))return net.minecraft.world.InteractionResult.PASS;
            if(hand!=net.minecraft.world.InteractionHand.MAIN_HAND||!player.getMainHandItem().is(SPEAR))return net.minecraft.world.InteractionResult.FAIL;
            if(world.isClientSide()){if(!player.isUsingItem())player.startUsingItem(hand);return net.minecraft.world.InteractionResult.PASS;}
            if(player instanceof ServerPlayer actor){
                if(!SoulMode.ownBody(actor,entity)&&!SpearCharge.ready(actor)){
                    if(!actor.isUsingItem())actor.startUsingItem(hand);
                    return net.minecraft.world.InteractionResult.SUCCESS;
                }
                return SoulMode.strike(actor,entity)?net.minecraft.world.InteractionResult.SUCCESS:net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.FAIL;
        });
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output->output.accept(SPEAR));
        ServerTickEvents.END_SERVER_TICK.register(server->{for(ServerPlayer player:server.getPlayerList().getPlayers()){accelerationTick(player);SoulMode.tick(player);BlockPhasing.tick(player);}});
        ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->SoulMode.finish(handler.player,null));
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{SoulMode.finish(handler.player,null);BlockPhasing.forget(handler.player);CHARGE.remove(handler.player.getUUID());ENABLED.remove(handler.player.getUUID());SPEAR_GRACE.remove(handler.player.getUUID());USING_SPEAR.remove(handler.player.getUUID());});
        ServerLifecycleEvents.SERVER_STOPPING.register(server->{for(ServerPlayer player:List.copyOf(server.getPlayerList().getPlayers())){SoulMode.finish(player,null);BlockPhasing.forget(player);}CHARGE.clear();ENABLED.clear();SPEAR_GRACE.clear();USING_SPEAR.clear();});
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
