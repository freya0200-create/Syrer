package dev.acceleration;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
public final class AccelerationClient implements ClientModInitializer {
    public static boolean soul;
    @Override public void onInitializeClient(){
        ClientPlayNetworking.registerGlobalReceiver(SoulPackets.State.TYPE,(payload,context)->context.client().execute(()->soul=payload.active()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->soul=false);
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(soul&&client.player!=null&&client.options.keyShift.isDown())ClientPlayNetworking.send(new SoulPackets.Action(1));
        });
    }
    public static void strike(){ClientPlayNetworking.send(new SoulPackets.Action(0));}
}
