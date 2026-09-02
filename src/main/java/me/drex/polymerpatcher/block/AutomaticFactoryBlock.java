package me.drex.polymerpatcher.block;

import com.mojang.datafixers.util.Pair;
import eu.pb4.factorytools.api.block.FactoryBlock;
import eu.pb4.factorytools.api.block.model.SignModel;
import eu.pb4.factorytools.api.block.model.generic.BSMMParticleBlock;
import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.polymer.blocks.api.BlockModelType;
import eu.pb4.polymer.blocks.api.PolymerBlockModel;
import eu.pb4.polymer.blocks.api.PolymerBlockResourceUtils;
import eu.pb4.polymer.blocks.api.PolymerTexturedBlock;
import eu.pb4.polymer.common.api.PolymerCommonUtils;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.BlockStateAsset;
import eu.pb4.polymer.resourcepack.extras.api.format.blockstate.StateModelVariant;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import me.drex.polymerpatcher.PolymerPatcher;
import me.drex.polymerpatcher.dump.data.ClientDataRegistry;
import me.drex.polymerpatcher.resources.ResourceHelper;
import me.drex.polymerpatcher.resources.ResourcePackGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.*;
import java.util.function.Predicate;

public record AutomaticFactoryBlock(
    Map<BlockState, BlockState> mappedStates,
    Set<BlockState> requiresElementHolder
) implements FactoryBlock, PolymerTexturedBlock, BSMMParticleBlock {

    private static final Map<Class<? extends Block>, Set<BlockState>> BLOCK_STATE_BLOCK_CACHE = new HashMap<>();
    private static final Map<ShapeKey, Set<BlockState>> COLLISION_BLOCK_STATE_CACHE = new HashMap<>();
    private static final Map<ShapeKey, Set<BlockModelType>> COLLISION_BLOCK_MODEL_CACHE = new HashMap<>();

    // Special block types
    private static final Set<BlockModelType> BIOME_COLOR = Set.of(BlockModelType.BIOME_TRANSPARENT_BLOCK, BlockModelType.BIOME_TRANSPARENT_BLOCK_WATERLOGGED, BlockModelType.BIOME_PLANT_BLOCK);
    private static final Set<BlockModelType> TRANSPARENT = new HashSet<>() {{
        addAll(
            Set.of(
                BlockModelType.TRANSPARENT_BLOCK, BlockModelType.TRANSPARENT_BLOCK_WATERLOGGED,
                BlockModelType.BIOME_TRANSPARENT_BLOCK, BlockModelType.BIOME_TRANSPARENT_BLOCK_WATERLOGGED,
                BlockModelType.PLANT_BLOCK, BlockModelType.BIOME_PLANT_BLOCK,
                BlockModelType.VINES_BLOCK, BlockModelType.KELP_BLOCK,
                BlockModelType.CACTUS_BLOCK, BlockModelType.TRIPWIRE_BLOCK,
                BlockModelType.TRIPWIRE_BLOCK_FLAT
            )
        );
        for (BlockModelType modelType : BlockModelType.values()) {
            String name = modelType.name();
            if (name.contains("CHAIN_")) {
                add(modelType);
            } else if (name.contains("LANTERN")) {
                add(modelType);
            } else if (name.contains("_DOOR")) {
                add(modelType);
            } else if (name.contains("_TRAPDOOR")) {
                add(modelType);
            } else if (name.contains("CAMPFIRE")) {
                add(modelType);
            } else if (name.contains("SCULK_SENSOR_BLOCK")) {
                add(modelType);
            } else if (name.contains("_SCAFFOLDING")) {
                add(modelType);
            } else if (name.contains("BARS_")) {
                add(modelType);
            }
        }
    }};
    private static final Set<BlockModelType> CLIMBABLE = Set.of(BlockModelType.VINES_BLOCK);

    private static final Set<BlockModelType> WATERLOGGED = new HashSet<>() {{
        for (BlockModelType modelType : BlockModelType.values()) {
            if (modelType.name().contains("_WATERLOGGED")) {
                add(modelType);
            }
        }
        add(BlockModelType.KELP_BLOCK);
    }};

    static {
        var world = PolymerCommonUtils.getFakeWorld();

        for (Map.Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet().stream().sorted(Comparator.comparing(x -> x.getKey().identifier())).toList()) {
            Block block = entry.getValue();
            ResourceKey<Block> resourceKey = entry.getKey();
            if (!resourceKey.identifier().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                continue;
            }
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                try {
                    VoxelShape shape = state.getCollisionShape(world, BlockPos.ZERO);
                    ShapeKey key = ShapeKey.of(shape);

                    COLLISION_BLOCK_STATE_CACHE.computeIfAbsent(key, (x) -> new LinkedHashSet<>()).add(state);
                } catch (Exception ignored) {
                }
            }
            BLOCK_STATE_BLOCK_CACHE.computeIfAbsent(block.getClass(), (x) -> new LinkedHashSet<>()).add(block.defaultBlockState());
        }

        for (BlockModelType blockModel : BlockModelType.values()) {
            BlockState mappedStateCandidate = PolymerBlockResourceUtils.requestEmpty(blockModel);
            VoxelShape shape = mappedStateCandidate.getCollisionShape(PolymerCommonUtils.getFakeWorld(), BlockPos.ZERO);
            ShapeKey key = ShapeKey.of(shape);
            COLLISION_BLOCK_MODEL_CACHE.computeIfAbsent(key, (x) -> new LinkedHashSet<>()).add(blockModel);
        }
    }

    public static AutomaticFactoryBlock create(Identifier id, Block originalBlock) {
        // Some models have multiple block states that use the same state model variants
        // We don't want to waste blockstates for that, so we remember them
        // The boolean is used for waterlogged blockstates
        Map<Pair<List<StateModelVariant>, Boolean>, BlockState> modelVariantCache = new HashMap<>();

        List<Tuple<BlockStatePredicate, List<StateModelVariant>>> parsedVariants = new ArrayList<>();
        try {
            BlockStateAsset blockStateAsset = ResourceHelper.decodeBlockState(id);
            BlockStateModelManager.parseVariants(originalBlock, blockStateAsset.variants().orElseThrow(), (a, b) -> parsedVariants.add(new Tuple<>(a, b)));
        } catch (Exception ignored) {
        }

        Map<BlockState, BlockState> mappedStates = new HashMap<>();
        Set<BlockState> requiresElementHolder = new HashSet<>();
        originalBlock.getStateDefinition().getPossibleStates().forEach(state -> {
                BlockState mappedState = findMatchingState(id, state, parsedVariants, modelVariantCache, requiresElementHolder);
                mappedStates.put(state, mappedState);
            }
        );
        return new AutomaticFactoryBlock(mappedStates, requiresElementHolder);
    }

    private static BlockState findMatchingState(
        Identifier id, BlockState originalState,
        List<Tuple<BlockStatePredicate, List<StateModelVariant>>> parsedVariants,
        Map<Pair<List<StateModelVariant>, Boolean>, BlockState> modelVariantCache,
        Set<BlockState> requiresElementHolder
    ) {
        Boolean waterLogged = originalState.getValueOrElse(BlockStateProperties.WATERLOGGED, false);

        VoxelShape collisionShape = originalState.getCollisionShape(PolymerCommonUtils.getFakeWorld(), BlockPos.ZERO);

        ShapeKey collisionKey = ShapeKey.of(collisionShape);

        Block block = originalState.getBlock();
        BlockState result = switch (block) {
            case SignBlock ignored -> findMatchingBlockClass(id, originalState, requiresElementHolder);
            case ButtonBlock ignored -> findMatchingBlockClass(id, originalState, requiresElementHolder);
            case PressurePlateBlock ignored -> findMatchingBlockClass(id, originalState, requiresElementHolder);
            case FenceGateBlock ignored -> findMatchingBlockClass(id, originalState, requiresElementHolder);
            case FenceBlock ignored -> findMatchingBlockClass(id, originalState, requiresElementHolder);
            case WallBlock ignored -> findMatchingBlockClass(id, originalState, requiresElementHolder);
            default -> null;
        };
        if (result != null) {
            return result;
        }

        result = findMatchingBlockModelType(id, originalState, parsedVariants, modelVariantCache, requiresElementHolder, collisionKey, waterLogged);
        if (result != null) {
            return result;
        }

/*        Set<BlockState> mappedStateCandidates = COLLISION_BLOCK_STATE_CACHE.get(collisionKey);
        if (mappedStateCandidates != null) {
            ResourcePackGenerator.expandBlockModel(id);
            requiresElementHolder.add(originalState);
            return pickBlockStateCandidate(id, originalState, mappedStateCandidates).trySetValue(BlockStateProperties.WATERLOGGED, waterLogged);
        }*/
        requiresElementHolder.add(originalState);
        return null;
    }

    private static BlockState findMatchingBlockModelType(
        Identifier id, BlockState originalState,
        List<Tuple<BlockStatePredicate, List<StateModelVariant>>> parsedVariants,
        Map<Pair<List<StateModelVariant>, Boolean>, BlockState> modelVariantCache,
        Set<BlockState> requiresElementHolder, ShapeKey collisionKey,
        boolean waterLogged
    ) {
        Set<BlockModelType> blockModelTypeCandidates = COLLISION_BLOCK_MODEL_CACHE.get(collisionKey);
        if (blockModelTypeCandidates != null) {
            for (var parsedVariant : parsedVariants) {
                if (parsedVariant.getA().test(originalState)) {

                    Pair<List<StateModelVariant>, Boolean> key = new Pair<>(parsedVariant.getB(), waterLogged);
                    if (modelVariantCache.containsKey(key)) {
                        var state = modelVariantCache.get(key);
                        PolymerPatcher.LOGGER.debug("Mapping blockstate {} -> {} (cached)", originalState, state);
                        return state;
                    }

                    Set<BlockModelType> blockModelTypes = pickBlockModelTypeCandidate(id, originalState, blockModelTypeCandidates);
                    for (BlockModelType blockModelType : blockModelTypes) {
                        int blocksLeft = PolymerBlockResourceUtils.getBlocksLeft(blockModelType);
                        if (blocksLeft > 0) {
                            var state = modelVariantCache.computeIfAbsent(key, c -> PolymerBlockResourceUtils.requestBlock(
                                blockModelType,
                                c.getFirst().stream().map(x -> new PolymerBlockModel(x.model(), x.x(), x.y(), x.uvlock(), x.weigth()))
                                    .toArray(PolymerBlockModel[]::new)));
                            PolymerPatcher.LOGGER.debug("Mapping blockstate {} -> {} ({} {} blocks left)", originalState, state, blockModelType, blocksLeft);
                            return state;
                        }
                    }
                }
            }
            Set<BlockModelType> blockModelTypes = pickBlockModelTypeCandidate(id, originalState, blockModelTypeCandidates);
            for (BlockModelType blockModelType : blockModelTypes) {
                requiresElementHolder.add(originalState);
                var state = PolymerBlockResourceUtils.requestEmpty(blockModelType);
                PolymerPatcher.LOGGER.debug("Mapping blockstate {} -> {} ({} no variants left)", originalState, state, blockModelType);
                if (blockModelType == BlockModelType.FULL_BLOCK) {
                    return Blocks.BARRIER.defaultBlockState();
                }
                return state;
            }
        }
        return null;
    }

    private static BlockState findMatchingBlockClass(
        Identifier id, BlockState originalState,
        Set<BlockState> requiresElementHolder
    ) {
        Set<BlockState> mappedStateCandidates = BLOCK_STATE_BLOCK_CACHE.get(originalState.getBlock().getClass());
        if (mappedStateCandidates != null) {
            ResourcePackGenerator.expandBlockModel(id);
            requiresElementHolder.add(originalState);
            BlockState pickedBlockState = pickBlockStateCandidate(id, originalState, mappedStateCandidates);

            for (Map.Entry<Property<?>, Comparable<?>> entry : originalState.getValues().entrySet()) {
                pickedBlockState = copyProperty(pickedBlockState, (Property) entry.getKey(), entry.getValue());
            }
            PolymerPatcher.LOGGER.debug("Mapping blockstate {} -> {} (matching block class)", originalState, pickedBlockState);
            return pickedBlockState;
        }
        return null;
    }


    private static <T extends Comparable<T>> BlockState copyProperty(
        BlockState state,
        Property<T> property,
        Comparable<?> value
    ) {
        return state.setValue(property, property.getValueClass().cast(value));
    }

    private static Set<BlockModelType> pickBlockModelTypeCandidate(Identifier id, BlockState original, Set<BlockModelType> mappedBlockModelCandidates) {
        Block block = original.getBlock();
        ClientDataRegistry.BlockInfo blockInfo = PolymerPatcher.getRenderRegistry().blockInfoByBlock.get(block);
        boolean biomeColor = blockInfo != null && blockInfo.biomeColor() != null;
        // TODO check tag?
        boolean climbable = false;
        boolean transparent = blockInfo != null && blockInfo.transparent();
        boolean waterLogged = original.getValueOrElse(BlockStateProperties.WATERLOGGED, false);

        List<Predicate<BlockModelType>> predicatePriority = new LinkedList<>();

        predicatePriority.add(x -> climbable ^ !CLIMBABLE.contains(x));
        predicatePriority.add(x -> waterLogged ^ !WATERLOGGED.contains(x));
        predicatePriority.add(x -> biomeColor ^ !BIOME_COLOR.contains(x));
        predicatePriority.add(x -> transparent ^ !TRANSPARENT.contains(x));


        Set<BlockModelType> currentBlockModelCandidates = mappedBlockModelCandidates;
        for (Predicate<BlockModelType> blockModelTypePredicate : predicatePriority) {
            Set<BlockModelType> updatedBlockModelCandidates = new HashSet<>();

            for (BlockModelType currentBlockModelCandidate : currentBlockModelCandidates) {
                if (blockModelTypePredicate.test(currentBlockModelCandidate)) {
                    updatedBlockModelCandidates.add(currentBlockModelCandidate);
                }
            }

            if (updatedBlockModelCandidates.isEmpty()) {
                continue;
            }
            currentBlockModelCandidates = updatedBlockModelCandidates;
        }

        return currentBlockModelCandidates;
    }

    private static BlockState pickBlockStateCandidate(Identifier id, BlockState original, Set<BlockState> mappedStateCandidates) {
        if (original.getBlock() instanceof FlowerPotBlock) {
            return pickBlockStateCandidate0(blockState -> blockState.is(Blocks.FLOWER_POT), mappedStateCandidates);
        }
        return mappedStateCandidates.iterator().next();
    }

    private static BlockState pickBlockStateCandidate0(Predicate<BlockState> predicate, Set<BlockState> mappedStateCandidates) {
        for (BlockState mappedStateCandidate : mappedStateCandidates) {
            if (predicate.test(mappedStateCandidate)) {
                return mappedStateCandidate;
            }
        }
        return mappedStateCandidates.iterator().next();
    }


    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        BlockState mappedState = mappedStates.get(blockState);
        if (mappedState != null) return mappedState;
        return Blocks.BARRIER.defaultBlockState();
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        if (requiresElementHolder.contains(initialBlockState)) {
            if (initialBlockState.getBlock() instanceof SignBlock) {
                return new SignModel(initialBlockState, pos);
            }
            return BlockStateModel.midRange();
        }
        return null;
    }

    @Override
    public boolean isIgnoringBlockInteractionPlaySoundExceptedEntity(BlockState state, ServerPlayer player, InteractionHand hand, ItemStack stack, ServerLevel world, BlockHitResult blockHitResult) {
        return true;
    }

    @Override
    public boolean showCustomMiningStageMarker(BlockState state, BlockPos pos, ServerPlayer player) {
        return requiresElementHolder.contains(state);
    }
}
