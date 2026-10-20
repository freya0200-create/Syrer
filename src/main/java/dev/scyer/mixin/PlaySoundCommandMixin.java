package dev.scyer.mixin;

import java.util.Collection;
import dev.scyer.SoundSignals;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlaySoundCommand.class)
public abstract class PlaySoundCommandMixin {
    @Inject(method = "playSound", at = @At("RETURN"))
    private static void scyer$hearCommand(CommandSourceStack source, Collection<ServerPlayer> targets,
            Identifier sound, SoundSource category, Vec3 position, float volume, float pitch,
            float minVolume, CallbackInfoReturnable<Integer> ci) {
        if (volume > 0 && ci.getReturnValue() > 0) {
            SoundSignals.emit(source.getLevel(), position, source.getEntity());
        }
    }
}
