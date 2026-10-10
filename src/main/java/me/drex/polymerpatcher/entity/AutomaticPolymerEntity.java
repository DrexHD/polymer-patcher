package me.drex.polymerpatcher.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.UniqueIdentifiableAttachment;
import eu.pb4.polymer.virtualentity.api.data.DisplayEntityData;
import me.drex.polymerpatcher.PolymerPatcher;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public record AutomaticPolymerEntity<T extends Entity>(T entity) implements PolymerEntity {
    public static final Identifier MODEL = PolymerPatcher.id("model");

    public static boolean isServerControlled(Entity entity) {
        return !entity.level().isClientSide() && entity.getControllingPassenger() instanceof ServerPlayer
            && PolymerEntity.get(entity) instanceof AutomaticPolymerEntity;
    }

    public AutomaticPolymerEntity {
        Function<Entity, ? extends SimpleEntityModel<?, ?, ?>> factory = (Function<Entity, ? extends SimpleEntityModel<?, ?, ?>>) AnimatedEntities.ENTITY_FACTORIES.get(entity.getType());
        if (factory != null && entity.level() instanceof ServerLevel) {
            SimpleEntityModel<?, ?, ?> holder = factory.apply(entity);
            IdentifiedUniqueEntityAttachment.ofTicking(MODEL, holder, entity);
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        if (entity instanceof AbstractBoat) {
            return EntityType.ACACIA_BOAT;
        }
        return EntityType.BLOCK_DISPLAY;
    }

    @Override
    public void modifyRawTrackedData(List<SynchedEntityData.DataValue<?>> data, ServerPlayer player, boolean initial) {
        PolymerEntity.super.modifyRawTrackedData(data, player, initial);
        if (initial) {
            data.add(SynchedEntityData.DataValue.create(DisplayEntityData.TELEPORTATION_DURATION, 3));
        }
    }

    @Override
    public void onEntityPacketSent(Consumer<Packet<?>> consumer, Packet<?> packet) {
        if (packet instanceof ClientboundAnimatePacket) {
            return;
        }
        UniqueIdentifiableAttachment attachment = UniqueIdentifiableAttachment.get(entity, MODEL);
        if (attachment == null) {
            PolymerEntity.super.onEntityPacketSent(consumer, packet);
            return;
        }
        var model = (SimpleEntityModel<?, ?, ?>) attachment.holder();
        if (packet instanceof ClientboundSetPassengersPacket) {
            model.sendPassengerPackets(consumer);
            return;
        }

        if (packet instanceof ClientboundSetEntityLinkPacket packet1) {
            consumer.accept(VirtualEntityUtils.createClientboundSetEntityLinkPacket(model.leadAttachment.getEntityId(), packet1.getDestId()));
            return;
        }
        PolymerEntity.super.onEntityPacketSent(consumer, packet);
    }
}
