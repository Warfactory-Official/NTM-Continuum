package com.hbm.blocks.machine.rbmk;

import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.Direction;import com.hbm.tileentity.ModRBMKBlockEntities; import com.hbm.tileentity.machine.rbmk.*; import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.entity.BlockEntityType; import net.minecraft.world.level.block.state.BlockState;
public class RBMKControl extends RBMKBase implements ICapabilityBlock { public final boolean moderated; public RBMKControl(Properties properties, boolean moderated) { super(properties); this.moderated = moderated; } @Override public boolean hasOwnLid() { return true; } @Override public int coreMask() { return this == ModRBMKBlocks.rbmk_control_reasim.get() ? MASK_DOWN : MASK_NONE; } @Override protected BlockEntityType<? extends BlockEntityRBMKBase> beType() { return ModRBMKBlockEntities.RBMK_CONTROL.get(); } @Override protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) { return new BlockEntityRBMKControlManual(pos, state); }
    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn()
            .faces(com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl.class, (be, side) -> be.isPowered() && side == Direction.DOWN);

    @Override
    public MachineCaps caps() { return this == ModRBMKBlocks.rbmk_control_reasim.get() ? CAPS : MachineCaps.NONE; }
}
