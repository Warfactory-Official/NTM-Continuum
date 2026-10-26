package com.hbm.world.gen.nbt;

import com.mojang.serialization.MapCodec;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.world.gen.NTMWorldGenerator;
import dev.golem.core.hook.Hook;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Rebuilds folded footprints around the core-only records stored in a vanilla structure template. */
public final class MultiblockAssemblyProcessor extends StructureProcessor {
    public static final MultiblockAssemblyProcessor INSTANCE = new MultiblockAssemblyProcessor();
    public static final MapCodec<MultiblockAssemblyProcessor> MAP_CODEC =
            MapCodec.unit(() -> INSTANCE);

    @FunctionalInterface
    public interface Transform {
        List<StructureBlockInfo> apply(List<StructureBlockInfo> source);
    }

    /** Transforms captured core state and data before assembly and vanilla's final block write. */
    public static final Hook<Transform> TRANSFORMS = Hook.of(listeners -> source -> {
        for (var transform : listeners) source = transform.apply(source);
        return source;
    });

    private MultiblockAssemblyProcessor() {}

    @Override protected StructureProcessorType<?> getType() { return NTMWorldGenerator.MULTIBLOCK_ASSEMBLY.get(); }

    @Override
    public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
            List<StructureBlockInfo> original, List<StructureBlockInfo> processed, StructurePlaceSettings settings) {
        var result = TRANSFORMS.invoker().apply(processed);
        var clip = settings.getBoundingBox();
        for (var info : result) {
            if (!(info.state().getBlock() instanceof BlockMultiblockCore block)) continue;
            var state = info.state().mirror(settings.getMirror()).rotate(settings.getRotation());
            if (clip == null || clip.isInside(info.pos())) level.setBlock(info.pos(), state, 3);
            block.fillSpace(level, info.pos(), state.getValue(BlockMultiblockCore.FACING), clip);
            block.assembled(level, info.pos(), state, clip);
            // Generation regions defer neighbour updates to vanilla; never load neighbouring chunks through ServerLevel here.
            if (level instanceof Level real && (clip == null || clip.isInside(info.pos()))) {
                MultiblockHandlerXR.refreshSpace(real, info.pos(), block.getDimensions(), state.getValue(BlockMultiblockCore.FACING));
            }
        }
        return result;
    }
}
