package com.hbm.world.gen.nbt;

import com.hbm.world.gen.NTMWorldGenerator;
import com.hbm.world.gen.component.Component;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import it.unimi.dsi.fastutil.ints.IntArrays;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;

/** Applies a registered native piece's actual block table when placing its exported vanilla template. */
public final class LegacyBlockTableProcessor extends StructureProcessor {
    private static final MapCodec<LegacyBlockTableProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("spawn").forGetter(processor -> processor.spawnName),
            Codec.STRING.fieldOf("piece").forGetter(processor -> processor.pieceName)
    ).apply(instance, LegacyBlockTableProcessor::new));
    //? if >=1.21.1 {
    public static final MapCodec<LegacyBlockTableProcessor> MAP_CODEC = CODEC;
    //?} else {
    /*public static final Codec<LegacyBlockTableProcessor> MAP_CODEC = CODEC.codec();
    *///?}

    private final String spawnName;
    private final String pieceName;

    public LegacyBlockTableProcessor(String spawnName, String pieceName) {
        this.spawnName = spawnName;
        this.pieceName = pieceName;
    }

    @Override protected StructureProcessorType<?> getType() { return NTMWorldGenerator.LEGACY_BLOCK_TABLE.get(); }

    @Override
    public List<StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos position, BlockPos referencePos,
            List<StructureBlockInfo> original, List<StructureBlockInfo> processed, StructurePlaceSettings settings) {
        var piece = NBTStructure.getStructure(spawnName).findPiece(pieceName);
        if (piece.blockTable == null) throw new IllegalStateException(pieceName + " has no native block table");
        if (original.size() != processed.size()) {
            throw new IllegalStateException("A native block table must precede list-filtering finalizers");
        }
        var order = new int[original.size()];
        for (var index = 0; index < order.length; index++) order[index] = index;
        // Native NBTStructure.Placer draws in x/z/y order, before footprint assembly. Vanilla's storage
        // tiers use a different order, so sort indices while keeping the returned list order unchanged.
        IntArrays.quickSort(order, (left, right) -> {
            var a = original.get(left).pos();
            var b = original.get(right).pos();
            var compared = Integer.compare(a.getX(), b.getX());
            if (compared == 0) compared = Integer.compare(a.getZ(), b.getZ());
            return compared == 0 ? Integer.compare(a.getY(), b.getY()) : compared;
        });
        var result = new ArrayList<>(processed);
        for (var index : order) {
            var info = processed.get(index);
            var selected = Component.blockTablePick(piece.blockTable, info.state().getBlock(), level.getRandom());
            if (selected == null) {
                result.set(index, info);
            } else {
                var data = selected.getBlock() == info.state().getBlock() ? info.nbt() : null;
                result.set(index, new StructureBlockInfo(info.pos(), selected, data));
            }
        }
        return result;
    }
}
