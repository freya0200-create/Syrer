package dev.acceleration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
public final class AccelerationClient implements ClientModInitializer {
    public static boolean projecting;
    @SuppressWarnings({"unchecked","rawtypes"})
    @Override public void onInitializeClient(){
        PlayerBody.CLIENT_FACTORY=ClientPlayerBody::new;
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(AccelerationMod.BODY,
            context->(net.minecraft.client.renderer.entity.EntityRenderer)new net.minecraft.client.renderer.entity.player.AvatarRenderer<ClientPlayerBody>(context,false));
        ClientPlayNetworking.registerGlobalReceiver(SoulPackets.State.TYPE,(payload,context)->context.client().execute(()->{projecting=payload.active();if(context.client().player!=null)context.client().player.getEntityData().set(ProjectionPhysics.ACTIVE,payload.active());}));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->projecting=false);
    }
}
