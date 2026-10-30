package dev.acceleration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
public final class AccelerationClient implements ClientModInitializer {
    public static boolean projecting;
    private static net.minecraft.client.KeyMapping momentumKey,phaseKey,phaseModeKey;
    @SuppressWarnings({"unchecked","rawtypes"})
    @Override public void onInitializeClient(){
        var category=net.minecraft.client.KeyMapping.Category.register(AccelerationMod.id("controls"));
        momentumKey=net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping(
            "key.acceleration.activate_momentum",com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_I,category));
        phaseKey=net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping(
            "key.acceleration.phase",com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_6,category));
        phaseModeKey=net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper.registerKeyMapping(new net.minecraft.client.KeyMapping(
            "key.acceleration.phase_mode",com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,org.lwjgl.glfw.GLFW.GLFW_KEY_7,category));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client->{
            while(phaseModeKey.consumeClick()){
                if(client.player!=null&&client.level!=null&&client.gui.screen()==null&&client.gui.overlay()==null&&ClientPlayNetworking.canSend(SoulPackets.Action.TYPE))
                    ClientPlayNetworking.send(new SoulPackets.Action(SoulPackets.CYCLE_PHASING));
            }
            while(phaseKey.consumeClick()){
                if(client.player!=null&&client.level!=null&&client.gui.screen()==null&&client.gui.overlay()==null&&ClientPlayNetworking.canSend(SoulPackets.Action.TYPE))
                    ClientPlayNetworking.send(new SoulPackets.Action(SoulPackets.TOGGLE_PHASING));
            }
            while(momentumKey.consumeClick()){
                if(client.player!=null&&client.level!=null&&client.gui.screen()==null&&client.gui.overlay()==null
                        &&ClientPlayNetworking.canSend(SoulPackets.Action.TYPE))
                    ClientPlayNetworking.send(new SoulPackets.Action(SoulPackets.TOGGLE_ACCELERATION));
            }
        });
        PlayerBody.CLIENT_FACTORY=ClientPlayerBody::new;
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(AccelerationMod.BODY,
            context->(net.minecraft.client.renderer.entity.EntityRenderer)new net.minecraft.client.renderer.entity.player.AvatarRenderer<ClientPlayerBody>(context,false));
        ClientPlayNetworking.registerGlobalReceiver(SoulPackets.State.TYPE,(payload,context)->context.client().execute(()->{projecting=payload.active();if(context.client().player!=null)context.client().player.getEntityData().set(ProjectionPhysics.ACTIVE,payload.active());}));
        ClientPlayNetworking.registerGlobalReceiver(SoulPackets.PhaseState.TYPE,(payload,context)->context.client().execute(()->{if(context.client().player!=null){context.client().player.getEntityData().set(ProjectionPhysics.PHASING,payload.active());context.client().player.getEntityData().set(ProjectionPhysics.PHASE_MODE,payload.mode());}}));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->projecting=false);
    }
}
