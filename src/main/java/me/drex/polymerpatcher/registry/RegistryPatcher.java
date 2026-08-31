package me.drex.polymerpatcher.registry;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import eu.pb4.polymer.core.api.item.PolymerItem;
import eu.pb4.polymer.core.api.other.*;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import eu.pb4.polymer.rsm.api.RegistrySyncUtils;
import me.drex.polymerpatcher.PolymerPatcher;
import me.drex.polymerpatcher.block.PolymerBlockHelper;
import me.drex.polymerpatcher.entity.AutomaticPolymerEntity;
import me.drex.polymerpatcher.item.PolyBaseItem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.*;
import java.util.function.BiConsumer;

public class RegistryPatcher {
    public static void patchRegistries() {
        PolymerPatcher.LOGGER.info("Patching registry entries...");

        patchRegistry(BuiltInRegistries.ENTITY_TYPE, RegistryPatcher::patchEntityType);
        patchRegistry(BuiltInRegistries.ITEM, (identifier, item) -> PolymerItem.registerOverlay(item, new PolyBaseItem(item)));
        patchRegistry(BuiltInRegistries.BLOCK,
            Comparator.comparing(PolymerBlockHelper::getPriority),
            PolymerBlockHelper::registerPolymerBlock
        );
        patchRegistry(BuiltInRegistries.PARTICLE_TYPE, (identifier, particleType) ->
            PolymerParticleType.setOverlay(particleType, (particleOptions, packetContext) -> ParticleTypes.ANGRY_VILLAGER));

        patchRegistry(BuiltInRegistries.DATA_COMPONENT_TYPE, (identifier, dataComponentType) -> PolymerComponent.registerDataComponent(dataComponentType));
        patchRegistry(BuiltInRegistries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, (identifier, dataComponentType) -> PolymerComponent.registerEnchantmentEffectComponent(dataComponentType));

        BuiltInRegistries.REGISTRY.listElements()
            .forEach(reference -> patchRegistry(reference.value()));

        PolymerPatcher.PATCHED_MODS.forEach(PolymerPatcher::setupModAssets);
    }


    private static <T> void patchRegistry(Registry<T> registry) {
        Optional<Holder.Reference<T>> defaultElement = registry.getAny();
        patchRegistry(registry, (identifier, t) -> {
            defaultElement.ifPresent(ref -> PolymerSyncedObject.setPlainSyncedObject(registry, t, (object, context) -> ref.value()));
            RegistrySyncUtils.setServerEntry(registry, t);
        });
    }

    private static <T> void patchRegistry(Registry<T> registry, BiConsumer<Identifier, T> consumer) {
        patchRegistry(registry, Comparator.comparing(ref -> 0), consumer);
    }

    private static <T> void patchRegistry(Registry<T> registry, Comparator<Holder.Reference<T>> comparator, BiConsumer<Identifier, T> consumer) {
        registry.listElements().sorted(comparator).forEachOrdered(ref -> {
            Identifier identifier = ref.key().identifier();

            if (isVanillaId(identifier) || RegistrySyncUtils.isServerEntry(registry, identifier)) return;
            PolymerPatcher.PATCHED_MODS.add(identifier.getNamespace());
            consumer.accept(identifier, ref.value());
        });
    }

    private static void patchEntityType(Identifier identifier, EntityType<?> entityType) {
        PolymerEntityUtils.registerOverlay(entityType, x -> {
            Entity entity = (Entity) x;
            return new AutomaticPolymerEntity<>(entity);
        });
    }

    public static boolean isVanillaId(Identifier id) {
        return id.getNamespace().equals(Identifier.DEFAULT_NAMESPACE) || id.getNamespace().equals("brigadier");
    }

}
