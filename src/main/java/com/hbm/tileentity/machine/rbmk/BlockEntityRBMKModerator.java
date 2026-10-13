package com.hbm.tileentity.machine.rbmk;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType; import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModRBMKBlockEntities;
public class BlockEntityRBMKModerator extends BlockEntityRBMKBase { public BlockEntityRBMKModerator(BlockPos p,BlockState s){super(ModRBMKBlockEntities.RBMK_MODERATOR.get(),p,s,0);}@Override public RBMKType getRBMKType(){return RBMKType.MODERATOR;}@Override public RBMKColumnType getConsoleType(){return RBMKColumnType.MODERATOR;} }
