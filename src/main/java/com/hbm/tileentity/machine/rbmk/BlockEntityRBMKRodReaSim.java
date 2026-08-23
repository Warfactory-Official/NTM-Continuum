package com.hbm.tileentity.machine.rbmk;
import com.hbm.handler.neutron.*; import com.hbm.handler.neutron.NeutronNodeWorld.StreamWorld; import net.minecraft.core.BlockPos; import net.minecraft.world.level.block.state.BlockState; import net.minecraft.world.phys.Vec3;
/** ReaSim channels emit eight phase-offset streams at 75% strength. */
public class BlockEntityRBMKRodReaSim extends BlockEntityRBMKRod {
 public BlockEntityRBMKRodReaSim(BlockPos p, BlockState s){super(com.hbm.tileentity.ModRBMKBlockEntities.RBMK_ROD_REASIM.get(),p,s);}
 @Override public void spreadFlux(double flux,double ratio){if(flux==0D){NeutronNodeWorld.removeNode(level,worldPosition);return;} StreamWorld w=NeutronNodeWorld.getOrAddWorld(level); RBMKNeutronHandler.RBMKNeutronNode n=(RBMKNeutronHandler.RBMKNeutronNode)w.getNode(worldPosition);if(n==null){n=RBMKNeutronHandler.makeNode(w,this);w.addNode(n);} Vec3 v=new Vec3(1,0,0).yRot((float)Math.toRadians(level.getRandom().nextInt(4)*9D));for(int i=0;i<8;i++){new RBMKNeutronHandler.RBMKNeutronStream(n,v,flux*.75D,ratio);v=v.yRot((float)Math.toRadians(45D));}}
 @Override public RBMKColumnType getConsoleType(){return RBMKColumnType.FUEL_SIM;}
}
