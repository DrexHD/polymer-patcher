package me.drex.polymerpatcher.entity;

import eu.pb4.factorytools.api.virtualentity.emuvanilla.poly.RideAttachmentElement;
import eu.pb4.factorytools.mixin.LivingEntityAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

final class RidingSeatElement extends RideAttachmentElement {
    private static final EntityAttachments HORSE_ATTACHMENTS = EntityType.HORSE.getDimensions()
        .scale((float) ((RangedAttribute) Attributes.SCALE.value()).getMinValue()).attachments();

    RidingSeatElement(Entity vehicle, Entity passenger) {
        update(vehicle, passenger);
    }

    void update(Entity vehicle, Entity passenger) {
        var horseSeat = HORSE_ATTACHMENTS.get(EntityAttachment.PASSENGER, 0, vehicle.getYRot());
        var riderAttachment = passenger.getAttachments().get(EntityAttachment.VEHICLE, 0, passenger.getYRot());
        var position = vehicle.getPassengerRidingPosition(passenger)
            .subtract(passenger.getVehicleAttachmentPoint(vehicle))
            .add(riderAttachment);
        setOffset(position.subtract(vehicle.position()).subtract(horseSeat));
        setYaw(vehicle.getYRot());
        if (vehicle instanceof LivingEntity living) {
            if (getHolder() != null) setMaxHealth(living.getMaxHealth());
            getSyncedData().set(LivingEntityAccessor.getDATA_HEALTH_ID(), living.getHealth());
        }
    }
}
