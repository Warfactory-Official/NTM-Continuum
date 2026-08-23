package com.hbm.tileentity.machine.rbmk;
import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.state.BlockState;
/** OTHER is intentionally transparent to neutron propagation. */
public class BlockEntityRBMKBlank extends BlockEntityRBMKBase { public BlockEntityRBMKBlank(BlockPos p,BlockState s){super(com.hbm.tileentity.ModRBMKBlockEntities.RBMK_BLANK.get(),p,s,0);}@Override public RBMKColumnType getConsoleType(){return RBMKColumnType.BLANK;} }
