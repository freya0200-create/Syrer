package dev.scyer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public final class ScyerClient implements ClientModInitializer {
    private static final ModelLayerLocation LAYER = new ModelLayerLocation(
        Identifier.fromNamespaceAndPath("scyer", "scyer"), "main");

    @Override public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(LAYER, ScyerModel::layer);
        ScyerMod.TYPES.forEach(type -> EntityRendererRegistry.register(type, ScyerRenderer::new));
    }

    private static final class ScyerRenderer extends MobRenderer<ScyerEntity, ScyerRenderState, ScyerModel> {
        ScyerRenderer(EntityRendererProvider.Context context) {
            super(context, new ScyerModel(context.bakeLayer(LAYER)), .65F);
        }
        @Override public ScyerRenderState createRenderState() {
            return new ScyerRenderState();
        }
        @Override public void extractRenderState(ScyerEntity entity, ScyerRenderState state, float partialTick) {
            super.extractRenderState(entity, state, partialTick);
            state.stage = entity.stage();
            state.roaring = entity.isRoaring();
            state.clawSwing = entity.getAttackAnim(partialTick);
        }
        @Override public Identifier getTextureLocation(ScyerRenderState state) {
            return Identifier.fromNamespaceAndPath("scyer", "textures/entity/scyer_pale.png");
        }
        @Override protected void scale(ScyerRenderState state, PoseStack matrices) {
            matrices.scale(1.68F, 1.68F, 1.68F);
        }
    }
}
