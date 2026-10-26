package com.hbm.blocks.machine.rbmk;

import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.capability.ICapabilityBlock;
import com.hbm.capability.MachineCaps;
import net.minecraft.core.Direction;import com.hbm.tileentity.ModRBMKBlockEntities; import com.hbm.tileentity.machine.rbmk.*; import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.entity.BlockEntityType; import net.minecraft.world.level.block.state.BlockState;
public class RBMKControlAuto extends RBMKBase implements ICapabilityBlock { public RBMKControlAuto(Properties properties) { super(properties); } @Override public boolean hasOwnLid() { return true; } @Override public int coreMask() { return this == ModRBMKBlocks.rbmk_control_reasim_auto.get() ? MASK_DOWN : MASK_NONE; } @Override protected BlockEntityType<? extends BlockEntityRBMKBase> beType() { return ModRBMKBlockEntities.RBMK_CONTROL_AUTO.get(); } @Override protected BlockEntityRBMKBase createCore(BlockPos pos, BlockState state) { return new BlockEntityRBMKControlAuto(pos, state); }
    /** Live connection rules of the corresponding 1.7 tile, shared by native and foreign contacts. */
    private static final MachineCaps CAPS = MachineCaps.blockKeyed().powerIn()
            .faces(BlockEntityRBMKControl.class, (be, side) -> be.isPowered() && side == Direction.DOWN);

    @Override
    public MachineCaps caps() { return this == ModRBMKBlocks.rbmk_control_reasim_auto.get() ? CAPS : MachineCaps.NONE; }
}
