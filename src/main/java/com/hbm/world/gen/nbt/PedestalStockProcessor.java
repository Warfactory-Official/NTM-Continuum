package com.hbm.world.gen.nbt;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.hbm.blocks.generic.BlockPedestal;
import com.hbm.world.gen.NTMWorldGenerator;
import dev.golem.core.registry.BlockEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Stocks every placed pedestal with the configured item (1.7's world-gen pedestal fills, e.g. Ike's divine shard). */
public final class PedestalStockProcessor extends StructureProcessor {
    private final Item item;

    public static final MapCodec<PedestalStockProcessor> MAP_CODEC =
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(processor -> processor.item)
            ).apply(instance, PedestalStockProcessor::new));

    public PedestalStockProcessor(Item item) {
        this.item = item;
    }

    @Override protected StructureProcessorType<?> getType() { return NTMWorldGenerator.PEDESTAL_STOCK.get(); }

    @Override
    public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
            List<StructureBlockInfo> original, List<StructureBlockInfo> processed, StructurePlaceSettings settings) {
        var result = new ArrayList<StructureBlockInfo>(processed.size());
        for (var info : processed) {
            if (!(info.state().getBlock() instanceof BlockPedestal)) {
                result.add(info);
                continue;
            }
            var entity = ((EntityBlock) info.state().getBlock()).newBlockEntity(info.pos(), info.state());
            if (entity instanceof BlockPedestal.ItemHolder holder) holder.setItem(new ItemStack(item));
            var data = BlockEntities.saveWithoutMetadata(entity, level.registryAccess());
            data.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString());
            result.add(new StructureBlockInfo(info.pos(), info.state(), data));
        }
        return result;
    }
}
