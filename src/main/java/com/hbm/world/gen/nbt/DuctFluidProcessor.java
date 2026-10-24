package com.hbm.world.gen.nbt;

import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.world.gen.NTMWorldGenerator;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.BlockEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Converts a template duct's exact-fluid marker to the native block-entity payload before vanilla placement. */
public final class DuctFluidProcessor extends StructureProcessor {
    public static final String KEY = "hbm_duct_fluid";
    public static final DuctFluidProcessor INSTANCE = new DuctFluidProcessor();
    //? if >=1.21.1 {
    public static final com.mojang.serialization.MapCodec<DuctFluidProcessor> MAP_CODEC =
            com.mojang.serialization.MapCodec.unit(() -> INSTANCE);
    //?} else {
    /*public static final com.mojang.serialization.Codec<DuctFluidProcessor> MAP_CODEC =
            com.mojang.serialization.Codec.unit(() -> INSTANCE);
    *///?}

    private DuctFluidProcessor() {}

    @Override protected StructureProcessorType<?> getType() { return NTMWorldGenerator.DUCT_FLUID.get(); }

    @Override
    public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
            List<StructureBlockInfo> original, List<StructureBlockInfo> processed, StructurePlaceSettings settings) {
        var result = new ArrayList<StructureBlockInfo>(processed.size());
        for (var info : processed) {
            var marker = info.nbt();
            if (marker == null || !Tags.contains(marker, KEY)) {
                result.add(info);
                continue;
            }
            if (!(info.state().getBlock() instanceof BlockFluidDuct)) {
                throw new IllegalStateException(KEY + " on " + info.state());
            }
            if (!Tags.keys(marker).equals(Set.of(KEY))) throw new IllegalStateException("Untranslated duct marker data " + marker);
            var name = Tags.getStringOr(marker, KEY, "");
            var fluid = Fluids.byRegistryNameExact(name);
            if (!fluid.getRegistryName().equals(name)) throw new IllegalStateException("Unregistered template duct fluid " + name);
            var entity = ((EntityBlock) info.state().getBlock()).newBlockEntity(info.pos(), info.state());
            var data = BlockEntities.saveWithoutMetadata(entity, level.registryAccess());
            data.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()).toString());
            Fluids.write(data, "type", fluid);
            result.add(new StructureBlockInfo(info.pos(), info.state(), data));
        }
        return result;
    }
}
