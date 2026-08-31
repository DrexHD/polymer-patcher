package me.drex.polymerpatcher.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.emuvanilla2.poly.RideAttachmentElement;
import eu.pb4.factorytools.api.virtualentity.emuvanilla2.poly.LeadAttachmentElement;
import eu.pb4.factorytools.mixin.LivingEntityAccessor;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import eu.pb4.polymer.virtualentity.api.tracker.DisplayTrackedData;
import me.drex.polymerpatcher.PolymerPatcher;
import me.drex.polymerpatcher.duck.IModelPart;
import me.drex.polymerpatcher.entity.render.ServerSubmitNodeCollector;
import me.drex.polymerpatcher.mixin.client.AbstractBoatRendererAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.*;

public class SimpleEntityModel<Entity extends net.minecraft.world.entity.Entity, RenderState extends EntityRenderState, Model extends EntityModel<? super RenderState>> extends ElementHolder {
    public final InteractionElement interaction;
    public final LeadAttachmentElement leadAttachment = new LeadAttachmentElement();
    public final RideAttachmentElement rideAttachment = new RideAttachmentElement();
    protected final Entity entity;
    private final EntityRenderer<Entity, RenderState> renderer;

    private boolean noTick = true;
    private float height = -1;

    private final List<ItemDisplayElement> activeElements = new ArrayList<>();
    public int activeElementIndex = 0;

    public SimpleEntityModel(Entity entity, PolyModelInstance<Entity, RenderState, Model> model) {
        this.entity = entity;
        this.renderer = model.renderer();
        var interaction = VirtualElement.InteractionHandler.redirect(entity);
        this.interaction = new InteractionElement(interaction);
        this.interaction.setSendPositionUpdates(false);
        this.addElement(leadAttachment);
        this.addElement(rideAttachment);
        this.addPassengerElement(this.interaction);
    }

    @Override
    public boolean startWatching(ServerGamePacketListenerImpl player) {
        if (noTick) {
            onTick();
        }
        return super.startWatching(player);
    }

    @Override
    protected void onTick() {
        noTick = false;
        if (this.entity instanceof LivingEntity livingEntity) {
            this.rideAttachment.setMaxHealth(livingEntity.getMaxHealth());
            this.rideAttachment.getDataTracker().set(LivingEntityAccessor.getDATA_HEALTH_ID(), livingEntity.getHealth());
        }
        this.interaction.setCustomName(this.entity.getCustomName());
        this.interaction.setCustomNameVisible(this.entity.isCustomNameVisible());
        this.rideAttachment.setYaw(entity.getYRot());
        if (entity.getBbHeight() != this.height) {
            this.height = entity.getBbHeight();
            this.interaction.setSize(entity.getBbWidth(), this.height);
            this.leadAttachment.setOffset(new Vec3(0, this.height / 2, 0));
            this.rideAttachment.setOffset(new Vec3(0, this.height, 0));
        }

        PoseStack poseStack = new PoseStack();

        // ensure the element doesn't clip into nearby blocks
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        poseStack.translate(0.0F, -dimensions.height() / 2, 0.0F);

        RenderState renderState = renderer.createRenderState();
        try {
            renderer.extractRenderState(entity, renderState, 0);
            if (renderer instanceof LivingEntityRenderer livingEntityRenderer) {
                livingEntityRenderer.getModel().setupAnim(renderState);
            } else if (renderer instanceof AbstractBoatRendererAccessor abstractBoatRenderer) {
                abstractBoatRenderer.invokeModel().setupAnim((BoatRenderState) renderState);
            }

            ServerSubmitNodeCollector<Entity, RenderState, Model> nodeCollector = new ServerSubmitNodeCollector<>(this);
            ServerSubmitNodeCollector.ACTIVE_ENTITY.set(this);
            activeElementIndex = 0;
            renderer.submit(renderState, poseStack, nodeCollector, null);
        } catch (Throwable e) {
            PolymerPatcher.LOGGER.debug("Failed to render", e);
        } finally {
            ServerSubmitNodeCollector.ACTIVE_ENTITY.remove();
        }

        while (activeElements.size() > activeElementIndex) {
            removeElement(activeElements.removeLast());
        }

        super.onTick();
    }

    public void updateModelPart(ModelPart part, Matrix4f matrix4f, int overlayCoords, Identifier texture, boolean translucent, boolean hidden) {
        int id = ((IModelPart) (Object) part).polymer_patcher$getId();
        ModelLayerLocation modelLayer = ((IModelPart) (Object) part).polymer_patcher$getModelLayerLocation();
        if (id < 0 || modelLayer == null || hidden) {
            return;
        }

        boolean redOverlay = (overlayCoords >> 16) == OverlayTexture.RED_OVERLAY_V;

        updateItemDisplayElement(createItemStack(id, redOverlay, texture, modelLayer, translucent), matrix4f);
    }

    public void updateBlock(BlockState blockState, Matrix4f matrix4f) {
        var randomSource = RandomSource.create(0);
        List<BlockStateModelManager.ModelGetter> modelGetters = BlockStateModelManager.get(blockState);

        for (BlockStateModelManager.ModelGetter modelGetter : modelGetters) {

            BlockStateModelManager.ModelData model = modelGetter.getModel(randomSource);

            Matrix4f transformation = new  Matrix4f(matrix4f);
            transformation.rotate(model.quaternionfc());
            updateItemDisplayElement(model.stack(), transformation);
        }
    }

    public void updateItem(ItemStack itemStack, ItemDisplayContext context, Matrix4f matrix4f) {
        ItemDisplayElement element = updateItemDisplayElement(itemStack, matrix4f);
        element.setItemDisplayContext(context);
    }

    private ItemDisplayElement updateItemDisplayElement(ItemStack itemStack, Matrix4f matrix4f) {
        ItemDisplayElement element;
        if (activeElementIndex >= activeElements.size()) {
            element = null;
        } else {
            element = this.activeElements.get(activeElementIndex);
        }

        activeElementIndex++;

        if (element == null) {
            // Show element
            element = this.createItemDisplayElement(itemStack);
            this.addElement(element);
            this.activeElements.add(element);
            element.setTeleportDuration(0);
        } else {
            // Update element
            element.setTeleportDuration(3);
            element.setItem(itemStack);
            element.getDataTracker().setDirty(DisplayTrackedData.Item.ITEM, true);
        }

        element.setTransformation(matrix4f);
        element.startInterpolationIfDirty();
        return element;
    }

    private ItemDisplayElement createItemDisplayElement(ItemStack itemStack) {
        var element = ItemDisplayElementUtil.createSimple(itemStack);
        element.setInterpolationDuration(1);
        element.setTeleportDuration(3);
        element.setViewRange(2);
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        element.setOffset(new Vec3(0, dimensions.height() / 2, 0));
        return element;
    }

    private ItemStack createItemStack(int id, boolean redOverlay, Identifier texture, ModelLayerLocation modelLayer, boolean translucent) {
        Identifier modelPath = texture.withSuffix("/" + modelLayer.layer() + "/part_" + id);
        ItemStack stack;
        if (translucent) {
           stack = ItemDisplayElementUtil.getTransparentModel(modelPath);
        } else {
            stack = ItemDisplayElementUtil.getSolidModel(modelPath);
        }
        if (redOverlay) {
            stack.set(DataComponents.MAP_COLOR, new MapItemColor(0xff7e7e));
        } else {
            stack.remove(DataComponents.MAP_COLOR);
        }
        return stack;
    }
}
