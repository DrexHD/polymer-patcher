package me.drex.polymerpatcher.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.IdentifiedUniqueEntityAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.UniqueIdentifiableAttachment;
import eu.pb4.polymer.virtualentity.api.data.DisplayEntityData;
import it.unimi.dsi.fastutil.ints.IntList;
import me.drex.polymerpatcher.PolymerPatcher;
import net.fabricmc.fabric.api.networking.v1.context.PacketContext;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public record AutomaticPolymerEntity<T extends Entity>(T entity) implements PolymerEntity {
    public static final Identifier MODEL = PolymerPatcher.id("model");

    public AutomaticPolymerEntity {
        Function<Entity, ? extends SimpleEntityModel<?, ?, ?>> factory = (Function<Entity, ? extends SimpleEntityModel<?, ?, ?>>) AnimatedEntities.ENTITY_FACTORIES.get(entity.getType());
        if (factory != null) {
            SimpleEntityModel<?, ?, ?> holder = factory.apply(entity);
            IdentifiedUniqueEntityAttachment.ofTicking(MODEL, holder, entity);
        }
    }

    @Override
    public EntityType<?> getPolymerEntityType(PacketContext packetContext) {
        if (entity instanceof AbstractBoat) {
            // TODO Ensure acacia boat gets custom model
            return EntityType.ACACIA_BOAT;
        }
        return EntityType.ITEM_DISPLAY;
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
        if (packet instanceof ClientboundSetPassengersPacket packet1 && packet1.getPassengers().length != 0) {
            var model = (SimpleEntityModel) UniqueIdentifiableAttachment.get(entity, MODEL).holder();
            consumer.accept(VirtualEntityUtils.createClientboundSetPassengersPacket(entity.getId(), IntList.of(model.interaction.getEntityId())));
            consumer.accept(VirtualEntityUtils.createClientboundSetPassengersPacket(model.interaction.getEntityId(), packet1.getPassengers()));
            return;
        }

        if (packet instanceof ClientboundSetEntityLinkPacket packet1) {
            var model = (SimpleEntityModel) UniqueIdentifiableAttachment.get(entity, MODEL).holder();
            consumer.accept(VirtualEntityUtils.createClientboundSetEntityLinkPacket(model.leadAttachment.getEntityId(), packet1.getDestId()));
            return;
        }
        if (packet instanceof ClientboundSetEntityDataPacket packet1) {
            return;
        }
        PolymerEntity.super.onEntityPacketSent(consumer, packet);
    }
}

