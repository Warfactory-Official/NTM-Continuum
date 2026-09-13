package com.hbm.tileentity.machine.struct;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.fusion.MachineFusionTorus;
import com.hbm.blocks.machine.struct.StructPattern;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Watches for a fusion torus to be built around it. The shape is {@link MachineFusionTorus#layout}
 * itself, so the pattern the ghost preview draws and the pattern the reactor occupies can never drift
 * apart — upstream re-walked the same mask in its own scan loop.
 */
public class TileEntityFusionTorusStruct extends TileEntityStructBase {

    private static final StructPattern PATTERN = StructPattern.builder()
            .mask(MachineFusionTorus.layout, MachineFusionTorus.HEIGHT, new StructPattern.PaletteEntry[]{
                    null, // index 0 is empty space
                    new StructPattern.PaletteEntry(ModMachineBlocks.fusion_component_bscco_welded, "fusion_component_bscco_welded"),
                    new StructPattern.PaletteEntry(ModMachineBlocks.fusion_component_blanket, "fusion_component_blanket"),
                    new StructPattern.PaletteEntry(ModMachineBlocks.fusion_component_motor, "fusion_component_motor"),
            })
            .build();

    public TileEntityFusionTorusStruct(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRUCT_TORUS_CORE.get(), pos, state);
    }

    @Override public StructPattern pattern() { return PATTERN; }

    @Override protected BlockMultiblockCore target() { return ModMachineBlocks.fusion_torus.get(); }
}
