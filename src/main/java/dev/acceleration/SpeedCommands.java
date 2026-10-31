package dev.acceleration;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.*;

public final class SpeedCommands {
    // Vanilla movement-speed attribute caps at 1024; normal player base is 0.1.
    public static final double MAX_PERCENT=1_023_900;
    private static final net.minecraft.resources.Identifier ID=AccelerationMod.id("command_speed");
    private SpeedCommands(){}
    public static double value(ServerPlayer player){var modifier=player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(ID);return modifier==null?0:modifier.amount()*100;}
    public static void set(ServerPlayer player,double percent){
        AccelerationMod.clearAcceleration(player);
        var attribute=player.getAttribute(Attributes.MOVEMENT_SPEED);attribute.removeModifier(ID);
        double amount=Math.max(0,Math.min(MAX_PERCENT,percent))/100;
        if(amount>0)attribute.addTransientModifier(new AttributeModifier(ID,amount,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
    private static int change(CommandSourceStack source,double amount,boolean add)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrException();set(player,add?value(player)+amount:amount);
        source.sendSuccess(()->Component.literal("Extra speed: "+value(player)+"% (0 = normal Survival speed)."),false);return 1;
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> speed(){
        return Commands.literal("speed").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("add").then(Commands.argument("percent",DoubleArgumentType.doubleArg(0,MAX_PERCENT)).executes(c->change(c.getSource(),DoubleArgumentType.getDouble(c,"percent"),true)))
                .then(Commands.literal("maximum").executes(c->change(c.getSource(),MAX_PERCENT,true))))
            .then(Commands.literal("set").then(Commands.argument("percent",DoubleArgumentType.doubleArg(0,MAX_PERCENT)).executes(c->change(c.getSource(),DoubleArgumentType.getDouble(c,"percent"),false)))
                .then(Commands.literal("maximum").executes(c->change(c.getSource(),MAX_PERCENT,false))))
            .then(Commands.literal("reset").executes(c->change(c.getSource(),0,false)));
    }
    public static void initialize(){CommandRegistrationCallback.EVENT.register((dispatcher,registry,environment)->{
        dispatcher.register(Commands.literal("accleration").then(speed()));
        dispatcher.register(Commands.literal("acceleration").then(speed()));
    });}
}
