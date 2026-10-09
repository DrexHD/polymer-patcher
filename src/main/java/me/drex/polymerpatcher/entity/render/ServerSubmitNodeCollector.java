package me.drex.polymerpatcher.entity.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.drex.polymerpatcher.client.rendering.CubeConsumer;
import me.drex.polymerpatcher.client.rendering.NoOpVertexConsumer;
import me.drex.polymerpatcher.entity.SimpleEntityModel;
import me.drex.polymerpatcher.mixin.client.render.RenderSetupAccessor;
import me.drex.polymerpatcher.mixin.client.render.RenderTypeAccessor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ServerSubmitNodeCollector<Entity extends net.minecraft.world.entity.Entity, RenderState extends EntityRenderState, Model extends EntityModel<? super RenderState>> implements SubmitNodeCollector {
    private final SimpleEntityModel<Entity, RenderState, Model> entityModel;
    public static final ThreadLocal<SimpleEntityModel> ACTIVE_ENTITY = new ThreadLocal<>();

    public ServerSubmitNodeCollector(SimpleEntityModel<Entity, RenderState, Model> entityModel) {
        this.entityModel = entityModel;
    }

    @Override
    public OrderedSubmitNodeCollector order(int i) {
        return this;
    }

    @Override
    public void submitShadow(PoseStack poseStack, float f, List<EntityRenderState.ShadowPiece> list) {
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 vec3, int i, Component component, boolean bl, int j, double d, CameraRenderState cameraRenderState) {
    }

    @Override
    public void submitText(PoseStack poseStack, float f, float g, FormattedCharSequence formattedCharSequence, boolean bl, Font.DisplayMode displayMode, int i, int j, int k, int l) {
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState entityRenderState, Quaternionf quaternionf) {
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
    }

    @Override
    public <S> void submitModel(net.minecraft.client.model.Model<? super S> model, S object, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite textureAtlasSprite, int l, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        model.setupAnim(object);
        renderWithTexture(renderType, overlayCoords, () -> model.renderToBuffer(poseStack, NoOpVertexConsumer.INSTANCE, lightCoords, overlayCoords, tintedColor));
    }

    private void renderWithTexture(RenderType renderType, int overlayCoords, Runnable render) {
        RenderSetup renderSetup = ((RenderTypeAccessor) renderType).getState();
        Map<String, RenderSetup.TextureBinding> textures = ((RenderSetupAccessor) (Object) renderSetup).polymer_patcher$textures();
        for (Map.Entry<String, RenderSetup.TextureBinding> stringTextureBindingEntry : textures.entrySet()) {
            Identifier texture = stringTextureBindingEntry.getValue().location();
            String path = texture.getPath().replace("textures/", "").replace(".png", "");

            CubeConsumer.CONSUMER.set((part, matrix4f, hidden) -> entityModel.updateModelPart(part, matrix4f, overlayCoords, texture.withPath(path), hidden));
            try {
                render.run();
            } finally {
                CubeConsumer.CONSUMER.remove();
            }
            break;
        }
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, @NotNull RenderType renderType, int i, int j, @Nullable TextureAtlasSprite textureAtlasSprite, boolean bl, boolean bl2, int k, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int l) {
        renderWithTexture(renderType, j, () -> modelPart.render(poseStack, NoOpVertexConsumer.INSTANCE, i, j, k));
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {

    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        // TODO
        //entityModel.updateBlock(blockState, poseStack.last().pose());
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {

    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {

    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer customGeometryRenderer) {

    }

    @Override
    public void submitParticleGroup(ParticleGroupRenderer particleGroupRenderer) {

    }
}
