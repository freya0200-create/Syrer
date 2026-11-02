package dev.acceleration.mixin;
import dev.acceleration.BlockPhasing;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ScreenEffectRenderer.class)
public abstract class PhasingOverlayMixin {
    @Inject(method="submitBlockSprite",at=@At("HEAD"),cancellable=true)
    private static void acceleration$clearBlockOverlay(Identifier texture,float u0,float v0,float u1,float v1,PoseStack poses,SubmitNodeCollector collector,int color,CallbackInfo ci){
        var player=Minecraft.getInstance().player;if(player!=null&&BlockPhasing.active(player))ci.cancel();
    }
}
