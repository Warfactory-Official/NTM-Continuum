package com.hbm.tileentity.machine.rbmk;
import com.hbm.entity.projectile.EntityRBMKDebris; import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType; import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.state.BlockState;
import com.hbm.tileentity.ModRBMKBlockEntities;
public class BlockEntityRBMKAbsorber extends BlockEntityRBMKBase { public BlockEntityRBMKAbsorber(BlockPos p,BlockState s){super(ModRBMKBlockEntities.RBMK_ABSORBER.get(),p,s,0);}@Override public void onMelt(int reduce) { spawnDebris(EntityRBMKDebris.DebrisType.BLANK, 1 + level.getRandom().nextInt(2)); super.onMelt(reduce); } @Override public RBMKType getRBMKType(){return RBMKType.ABSORBER;}@Override public RBMKColumnType getConsoleType(){return RBMKColumnType.ABSORBER;} }
