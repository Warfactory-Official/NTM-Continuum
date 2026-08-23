package com.hbm.tileentity.machine.rbmk;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType; import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.state.BlockState;
public class BlockEntityRBMKReflector extends BlockEntityRBMKBase { public BlockEntityRBMKReflector(BlockPos p,BlockState s){super(com.hbm.tileentity.ModRBMKBlockEntities.RBMK_REFLECTOR.get(),p,s,0);}@Override public RBMKType getRBMKType(){return RBMKType.REFLECTOR;}@Override public RBMKColumnType getConsoleType(){return RBMKColumnType.REFLECTOR;} }
