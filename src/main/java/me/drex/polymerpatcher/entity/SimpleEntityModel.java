package me.drex.polymerpatcher.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.factorytools.api.virtualentity.emuvanilla.poly.LeadAttachmentElement;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.data.DisplayEntityData;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.VirtualElement;
import me.drex.polymerpatcher.PolymerPatcher;
import me.drex.polymerpatcher.duck.IModelPart;
import me.drex.polymerpatcher.entity.render.ServerSubmitNodeCollector;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SimpleEntityModel<Entity extends net.minecraft.world.entity.Entity, RenderState extends EntityRenderState, Model extends EntityModel<? super RenderState>> extends ElementHolder {
    private static final CameraRenderState DEFAULT_CAMERA_STATE = new CameraRenderState();
    public final InteractionElement interaction;
    public final LeadAttachmentElement leadAttachment = new LeadAttachmentElement();
    private final List<RidingSeatElement> seats = new ArrayList<>();
    private List<net.minecraft.world.entity.Entity> riders = List.of();
    protected final Entity entity;
    private final EntityRenderer<Entity, RenderState> renderer;

    private boolean noTick = true;

    private final List<ItemDisplayElement> activeElements = new ArrayList<>();
    private final Set<ServerGamePacketListenerImpl> pendingPassengerUpdates = new HashSet<>();
    public int activeElementIndex = 0;

    public SimpleEntityModel(Entity entity, PolyModelInstance<Entity, RenderState, Model> model) {
        this.entity = entity;
        this.renderer = model.renderer();
        var interaction = VirtualElement.InteractionHandler.redirect(entity);
        this.interaction = new InteractionElement(interaction);
        this.interaction.setSendPositionUpdates(false);
        this.addElement(leadAttachment);
        if (!(entity instanceof AbstractBoat)) {
            this.addPassengerElement(this.interaction);
        }
    }

    @Override
    public boolean startWatching(ServerGamePacketListenerImpl player) {
        if (noTick) {
            onTick();
        }
        return super.startWatching(player);
    }

    @Override
    protected void startWatchingExtraPackets(ServerGamePacketListenerImpl player, Consumer<Packet<ClientGamePacketListener>> consumer) {
        super.startWatchingExtraPackets(player, consumer);
        sendPassengerPackets(consumer);
        pendingPassengerUpdates.add(player);
    }

    public void sendPassengerPackets(Consumer<? super Packet<ClientGamePacketListener>> consumer) {
        updateSeats();
        consumer.accept(VirtualEntityUtils.createClientboundSetPassengersPacket(entity.getId(), getAttachedPassengerEntityIds()));
        for (int i = 0; i < seats.size(); i++) {
            consumer.accept(VirtualEntityUtils.createClientboundSetPassengersPacket(
                seats.get(i).getEntityId(), new int[]{riders.get(i).getId()}));
        }
    }

    private boolean updateSeats() {
        var passengers = entity.getPassengers();
        boolean changed = !riders.equals(passengers);
        riders = passengers;
        while (seats.size() > passengers.size()) {
            removeElement(seats.removeLast());
        }
        for (int i = 0; i < passengers.size(); i++) {
            if (i == seats.size()) {
                seats.add(addElement(new RidingSeatElement(entity, passengers.get(i))));
            }
            seats.get(i).update(entity, passengers.get(i));
        }
        return changed;
    }

    @Override
    protected void onTick() {
        noTick = false;
        this.interaction.setCustomName(this.entity.getCustomName());
        this.interaction.setCustomNameVisible(this.entity.isCustomNameVisible());
        this.interaction.setSize(entity.getBbWidth(), entity.getBbHeight());
        this.leadAttachment.setOffset(new Vec3(0, entity.getBbHeight() / 2, 0));
        if (updateSeats()) sendPassengerPackets(this::sendPacket);

        this.interaction.setOnFire(this.entity.isOnFire());

        PoseStack poseStack = new PoseStack();

        // ensure the element doesn't clip into nearby blocks
        EntityDimensions dimensions = entity.getDimensions(entity.getPose());
        poseStack.translate(0.0F, -dimensions.height() / 2, 0.0F);

        RenderState renderState = renderer.createRenderState();
        try {
            activeElementIndex = 0;
            renderer.extractRenderState(entity, renderState, 0);
            ServerSubmitNodeCollector<Entity, RenderState, Model> nodeCollector = new ServerSubmitNodeCollector<>(this);
            ServerSubmitNodeCollector.ACTIVE_ENTITY.set(this);
            renderer.submit(renderState, poseStack, nodeCollector, DEFAULT_CAMERA_STATE);
        } catch (Throwable e) {
            PolymerPatcher.LOGGER.debug("Failed to render", e);
        } finally {
            ServerSubmitNodeCollector.ACTIVE_ENTITY.remove();
        }

        while (activeElements.size() > activeElementIndex) {
            removeElement(activeElements.removeLast());
        }

        for (ItemDisplayElement activeElement : activeElements) {
            activeElement.setGlowing(this.entity.isCurrentlyGlowing());
        }

        for (var player : pendingPassengerUpdates) {
            if (getWatchingPlayers().contains(player)) sendPassengerPackets(player::send);
        }
        pendingPassengerUpdates.clear();

        super.onTick();
    }

    public void updateModelPart(ModelPart part, Matrix4f matrix4f, int overlayCoords, Identifier texture, boolean hidden) {
        int id = ((IModelPart) (Object) part).polymer_patcher$getId();
        ModelLayerLocation modelLayer = ((IModelPart) (Object) part).polymer_patcher$getModelLayerLocation();
        if (id < 0 || modelLayer == null || hidden) {
            return;
        }

        boolean redOverlay = (overlayCoords >> 16) == OverlayTexture.RED_OVERLAY_V;

        updateItemDisplayElement(createItemStack(id, redOverlay, texture, modelLayer), matrix4f);
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
            element.getSyncedData().setDirty(DisplayEntityData.Item.ITEM, true);
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

    private ItemStack createItemStack(int id, boolean redOverlay, Identifier texture, ModelLayerLocation modelLayer) {
        Identifier modelPath = PolyModelInstance.partModelId(texture, modelLayer, id);
        ItemStack stack;
        // TODO use getModel?
        stack = ItemDisplayElementUtil.getModelCopy(modelPath);
        if (redOverlay) {
            stack.set(DataComponents.MAP_COLOR, new MapItemColor(0xff7e7e));
        } else {
            stack.remove(DataComponents.MAP_COLOR);
        }
        return stack;
    }
}
