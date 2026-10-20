package dev.scyer.mixin;

import dev.scyer.SoundSignals;
import dev.scyer.ScyerEntity;
import dev.scyer.ScyerPopulation;
import dev.scyer.ScyerSpawning;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerWorldMixin {
    @Inject(method = "gameEvent", at = @At("HEAD"))
    private void scyer$hearEvent(Holder<GameEvent> event, Vec3 pos, GameEvent.Context emitter, CallbackInfo ci) {
        SoundSignals.emit((ServerLevel)(Object)this, pos, emitter.sourceEntity());
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("HEAD"))
    private void scyer$hearSound(Entity excluded, double x, double y, double z, Holder<SoundEvent> sound,
                                SoundSource category, float volume, float pitch, long seed, CallbackInfo ci) {
        if (volume <= 0) return;
        ServerLevel level = (ServerLevel)(Object)this;
        Vec3 pos = new Vec3(x, y, z);
        SoundSignals.emit(level, pos, SoundSignals.actorAt(level, pos));
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("HEAD"))
    private void scyer$hearAttachedSound(Entity excluded, Entity actor, Holder<SoundEvent> sound,
                                        SoundSource category, float volume, float pitch, long seed, CallbackInfo ci) {
        if (volume > 0) SoundSignals.emit((ServerLevel)(Object)this, actor.position(), actor);
    }

    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    private void scyer$limitBeforeAddition(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (entity instanceof ScyerEntity) {
            var population = ScyerPopulation.get(((ServerLevel)(Object)this).getServer());
            if (population.retired(entity.getUUID()) || (!population.contains(entity.getUUID()) && population.size() >= ScyerPopulation.LIMIT)) {
                entity.discard();
                ci.setReturnValue(false);
            }
        }
    }
    @Inject(method = "addEntity", at = @At("RETURN"))
    private void scyer$reserveAddedEntity(Entity entity, CallbackInfoReturnable<Boolean> ci) {
        if (entity instanceof ScyerEntity && ci.getReturnValue()) {
            if (!ScyerPopulation.get(((ServerLevel)(Object)this).getServer()).admit(entity.getUUID())) entity.discard();
            else ScyerSpawning.keepAwake((ServerLevel)(Object)this, entity.blockPosition());
        }
    }
}
