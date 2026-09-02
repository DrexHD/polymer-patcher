package me.drex.polymerpatcher.block;

import eu.pb4.factorytools.api.block.model.generic.BlockStateModelManager;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.ItemDisplayElementUtil;
import eu.pb4.polymer.virtualentity.api.attachment.BlockAwareAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import me.drex.polymerpatcher.PolymerPatcher;
import me.drex.polymerpatcher.dump.data.ClientDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class BlockStateModel extends BlockModel {
    private final List<ItemDisplayElement> modelElements = new ArrayList<>();
    private final float viewRange;

    public BlockStateModel(float viewRange) {
        this.viewRange = viewRange;
    }

    public static BlockStateModel midRange() {
        return new BlockStateModel(3.0F);
    }

    public void notifyUpdate(HolderAttachment.UpdateType updateType) {
        super.notifyUpdate(updateType);
        if (updateType == BlockAwareAttachment.BLOCK_STATE_UPDATE) {
            this.applyModel(BlockStateModelManager.get(this.blockState()), this.blockPos());
        }
    }

    @Override
    protected void onAttachmentSet(HolderAttachment attachment, @Nullable HolderAttachment oldAttachment) {
        super.onAttachmentSet(attachment, oldAttachment);
        this.applyModel(BlockStateModelManager.get(this.blockState()), this.blockPos());
    }

    private void applyModel(List<BlockStateModelManager.ModelGetter> models, BlockPos pos) {
        RandomSource random = RandomSource.create(this.blockState().getSeed(pos));
        int i = 0;

        while (models.size() < this.modelElements.size()) {
            this.removeElement(this.modelElements.removeLast());
        }

        for (; i < models.size(); ++i) {
            boolean newModel = false;
            ItemDisplayElement element;
            if (this.modelElements.size() <= i) {
                element = ItemDisplayElementUtil.createSimple();
                element.setViewRange(this.viewRange);
                element.setTeleportDuration(0);
                element.setItemDisplayContext(ItemDisplayContext.NONE);
                element.setYaw(180.0F);
                newModel = true;
                this.modelElements.add(element);
            } else {
                element = this.modelElements.get(i);
            }

            BlockStateModelManager.ModelData model = models.get(i).getModel(random);
            element.setItem(model.stack());
            element.setLeftRotation(model.quaternionfc());
            this.setupElement(element, i, pos);
            if (newModel) {
                this.addElement(element);
            } else {
                element.tick();
            }
        }
    }

    protected void setupElement(ItemDisplayElement element, int i, BlockPos pos) {
        if (!(getAttachment() instanceof BlockBoundAttachment blockBoundAttachment)) {
            return;
        }
        ClientDataRegistry.BlockInfo blockInfo = PolymerPatcher.getRenderRegistry().blockInfoByBlock.get(blockBoundAttachment.getBlockState().getBlock());
        if (blockInfo != null && blockInfo.biomeColor() != null) {
            int color = getAverageColor(blockBoundAttachment.getChunk(), pos, blockInfo.biomeColor());
            if (color != -1) {
                ItemStack item = element.getItem().copy();
                item.set(DataComponents.MAP_COLOR, new MapItemColor(color));
                element.setItem(item);
            }
        }
    }

    private static int getAverageColor(LevelChunk chunk, BlockPos blockPos, ColorResolver colorResolver) {
        Holder<Biome> biome = chunk.getNoiseBiome(QuartPos.fromBlock(blockPos.getX()), QuartPos.fromBlock(blockPos.getY()), QuartPos.fromBlock(blockPos.getZ()));
        return colorResolver.getColor(biome.value(), blockPos.getX(), blockPos.getZ());
    }
}
