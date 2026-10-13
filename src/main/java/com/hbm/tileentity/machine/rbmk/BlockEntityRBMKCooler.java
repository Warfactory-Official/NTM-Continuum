package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.machine.rbmk.RBMKLoader;
import com.hbm.lib.DirPos;
import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;

/** Five-by-five RBMK cooler; each 50 mB cycle removes up to 200 C per column. */
public class BlockEntityRBMKCooler extends BlockEntityRBMKBase implements IFluidProviderMK2,IFluidReceiverMK2,IFluidStandardTransceiverMK2 {
    @Override public RBMKColumnType getConsoleType() { return RBMKColumnType.COOLER; }
    public final FluidTankNTM cold=new FluidTankNTM(Fluids.PERFLUOROMETHYL_COLD,4000),hot=new FluidTankNTM(Fluids.PERFLUOROMETHYL,4000); private final BlockEntityRBMKBase[] cache=new BlockEntityRBMKBase[25]; private int timer; public int lastCooled;
    public BlockEntityRBMKCooler(BlockPos p,BlockState s){super(ModRBMKBlockEntities.RBMK_COOLER.get(),p,s,0);}
    @Override public FluidTankNTM[] getAllTanks(){return new FluidTankNTM[]{cold,hot};}
    @Override public void tickServer(){if(timer<=0){timer=60;for(int i=0;i<25;i++)cache[i]=level.getBlockEntity(worldPosition.offset(-2+i/5,0,-2+i%5)) instanceof BlockEntityRBMKBase c?c:null;}else timer--;if(cold.getFill()>=50&&hot.getMaxFill()-hot.getFill()>=50){cold.setFill(cold.getFill()-50);hot.setFill(hot.getFill()+50);int cooled=0;for(BlockEntityRBMKBase c:cache)if(c!=null&&!c.isRemoved()){double old=c.heat;c.heat=Math.max(20,c.heat-200);int delta=(int)(old-c.heat);if(delta>0){cooled+=delta;c.markChanged();}}lastCooled=cooled;}else lastCooled=0;if(hot.getFill()>0)for(DirPos pos:RBMKLoader.outputPositions(level,worldPosition,false))tryProvide(hot,level,pos);trySubscribe(cold.getTankType(),level,worldPosition.below(),ForgeDirection.DOWN);/* 1.7: the coolant from below */super.tickServer();}
    @Override public RBMKColumn getConsoleData(){RBMKColumn.CoolerColumn data=(RBMKColumn.CoolerColumn)super.getConsoleData();data.cooled=lastCooled;data.cryo=cold.getFill();data.maxCryo=cold.getMaxFill();data.hot=hot.getFill();data.maxHot=hot.getMaxFill();data.coldType=RBMKColumn.fluidId(cold.getTankType());data.hotType=RBMKColumn.fluidId(hot.getTankType());return data;}
    @Override public long getFluidAvailable(FluidType t,int p){return t==hot.getTankType()&&p==hot.getPressure()?hot.getFill():0;} @Override public void useUpFluid(FluidType t,int p,long a){if(getFluidAvailable(t,p)>0){hot.setFill(hot.getFill()-(int)Math.min(a,hot.getFill()));setChanged();}} @Override public long getDemand(FluidType t,int p){return t==Fluids.PERFLUOROMETHYL_COLD&&p==cold.getPressure()?cold.getMaxFill()-cold.getFill():0;} @Override public long transferFluid(FluidType t,int p,long a){if(t!=Fluids.PERFLUOROMETHYL_COLD||p!=cold.getPressure())return a;int n=cold.fill(t,(int)Math.min(a,Integer.MAX_VALUE),true);if(n>0)setChanged();return a-n;}
    @Override public FluidTankNTM[] getReceivingTanks(){return new FluidTankNTM[]{cold};} @Override public FluidTankNTM[] getSendingTanks(){return new FluidTankNTM[]{hot};}
    @Override protected void saveData(DataOut o){super.saveData(o);cold.writeToNBT(o,"cold");hot.writeToNBT(o,"hot");o.putInt("timer",timer);} @Override protected void loadData(DataIn i){super.loadData(i);cold.readFromNBT(i,"cold");hot.readFromNBT(i,"hot");timer=i.getIntOr("timer",0);}
    @Override public void serialize(RegistryFriendlyByteBuf buffer){super.serialize(buffer);cold.serialize(buffer);hot.serialize(buffer);}
    @Override public void deserialize(RegistryFriendlyByteBuf buffer){super.deserialize(buffer);cold.deserialize(buffer);hot.deserialize(buffer);}
}
