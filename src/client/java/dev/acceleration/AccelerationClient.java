package dev.acceleration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.*;
public final class AccelerationClient implements ClientModInitializer {
    public static boolean projecting;
    @Override public void onInitializeClient(){
        ClientPlayNetworking.registerGlobalReceiver(SoulPackets.State.TYPE,(payload,context)->context.client().execute(()->projecting=payload.active()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->projecting=false);
    }
}
