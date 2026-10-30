package dev.acceleration;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public final class SoulPackets {
    public static final int TOGGLE_ACCELERATION=2;
    public static final int TOGGLE_PHASING=3;
    public static final int CYCLE_PHASING=4;
    public static final int STEP_PHASING=5;
    public static boolean handleAction(net.minecraft.server.level.ServerPlayer player,int action){
        if(action==STEP_PHASING)return BlockPhasing.step(player);
        if(action==CYCLE_PHASING)return BlockPhasing.cycleMode(player);
        if(action==TOGGLE_PHASING)return BlockPhasing.toggle(player);
        if(action==TOGGLE_ACCELERATION)return AccelerationMod.toggleAcceleration(player);
        if(action==1)return SoulMode.finish(player,null);
        return false;
    }
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
    public record PhaseState(boolean active,int mode) implements CustomPacketPayload {
        public static final Type<PhaseState> TYPE=new Type<>(AccelerationMod.id("phase_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf,PhaseState> CODEC=StreamCodec.composite(ByteBufCodecs.BOOL,PhaseState::active,ByteBufCodecs.VAR_INT,PhaseState::mode,PhaseState::new);
        @Override public Type<PhaseState> type(){return TYPE;}
    }
    public static void initialize(){
        PayloadTypeRegistry.serverboundPlay().register(Action.TYPE,Action.CODEC);PayloadTypeRegistry.clientboundPlay().register(State.TYPE,State.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PhaseState.TYPE,PhaseState.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Action.TYPE,(payload,context)->{
            handleAction(context.player(),payload.action());
        });
    }
}
