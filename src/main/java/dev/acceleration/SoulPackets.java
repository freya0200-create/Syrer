package dev.acceleration;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public final class SoulPackets {
    public record Action(int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE=new Type<>(AccelerationMod.id("soul_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Action> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Action::action,Action::new);
        @Override public Type<Action> type(){return TYPE;}
    }
    public record State(boolean active) implements CustomPacketPayload {
        public static final Type<State> TYPE=new Type<>(AccelerationMod.id("soul_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,State> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,State::active,State::new);
        @Override public Type<State> type(){return TYPE;}
    }
    public static void initialize(){
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,(payload,context)->{
            if(payload.action()==1)SoulMode.finish(context.player(),null);
        });
    }
}
