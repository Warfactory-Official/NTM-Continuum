package com.hbm.tileentity.machine.rbmk;
import com.hbm.entity.projectile.EntityRBMKDebris; import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType; import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModRBMKBlockEntities;
public class BlockEntityRBMKReflector extends BlockEntityRBMKBase { public BlockEntityRBMKReflector(BlockPos p,BlockState s){super(ModRBMKBlockEntities.RBMK_REFLECTOR.get(),p,s,0);}@Override public void onMelt(int reduce) { spawnDebris(EntityRBMKDebris.DebrisType.BLANK, 1 + level.getRandom().nextInt(2)); super.onMelt(reduce); } @Override public RBMKType getRBMKType(){return RBMKType.REFLECTOR;}@Override public RBMKColumnType getConsoleType(){return RBMKColumnType.REFLECTOR;} }
