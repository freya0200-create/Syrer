package dev.acceleration.mixin;
import dev.acceleration.ProjectionPhysics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(EntityRenderDispatcher.class)
public abstract class GhostVisibilityMixin {
    @Inject(method="shouldRender",at=@At("HEAD"),cancellable=true)
    private void acceleration$hideOtherGhosts(Entity entity,Frustum frustum,double x,double y,double z,CallbackInfoReturnable<Boolean> ci){
        if(ProjectionPhysics.hiddenFrom(entity,Minecraft.getInstance().player))ci.setReturnValue(false);
    }
}
