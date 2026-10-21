package com.hbm.tileentity.machine.rbmk;

import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.inventory.container.MenuRBMKControl;
import com.hbm.inventory.container.MenuRBMKControlAuto;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.data.Tags;
 import com.hbm.api.energymk2.IEnergyReceiverMK2; import com.hbm.entity.projectile.EntityRBMKDebris; import com.hbm.api.redstoneoverradio.IRORValueProvider; import com.hbm.interfaces.IControlReceiver; import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType; import dev.golem.core.data.*; import net.minecraft.core.*; import net.minecraft.nbt.CompoundTag; import net.minecraft.network.RegistryFriendlyByteBuf; import net.minecraft.util.Mth; import net.minecraft.world.level.block.entity.BlockEntityType; import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import com.hbm.lib.Library;
import com.hbm.tileentity.TilePort.PortDef;
/** Control motion charges once per moving tick, not per distance travelled. */
 public abstract class BlockEntityRBMKControl extends BlockEntityRBMKBase implements IControlReceiver, IEnergyReceiverMK2, IRORValueProvider {
 public static final double speed=.00277D; public static final long consumption=5000,maxPower=consumption*10; public double lastLevel,level,targetLevel; public boolean hasPower; public long power;
 protected BlockEntityRBMKControl(BlockEntityType<?> t,BlockPos p,BlockState s){super(t,p,s,0);} public boolean isPowered(){return getBlockState().is(ModRBMKBlocks.rbmk_control_reasim.get())||getBlockState().is(ModRBMKBlocks.rbmk_control_reasim_auto.get());}
 @Override public long getPower(){return power;} @Override public void setPower(long p){power=p;} @Override public long getMaxPower(){return isPowered()?maxPower:0;} @Override public boolean canConnect(ForgeDirection d){return isPowered()&&d==ForgeDirection.DOWN;} @Override public ConnectionPriority getPriority(){return ConnectionPriority.LOW;} @Override public boolean isLidRemovable(){return false;} @Override public void tickClient(){lastLevel=level;}
 protected PortDef[] cachedPorts;
 public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = new PortDef[] { PortDef.make(worldPosition, Library.NEG_Y) }; return cachedPorts; }
 @Override public void tickServer(){if(isPowered()){setupPowerPorts(getPorts());updateAllPorts();receivePower();}hasPower=!isPowered()||power>=consumption;lastLevel=level;if(hasPower){if(level<targetLevel){level+=speed*RBMKDials.getControlSpeed(getLevel());if(level>targetLevel)level=targetLevel;}if(level>targetLevel){level-=speed*RBMKDials.getControlSpeed(getLevel());if(level<targetLevel)level=targetLevel;}if(isPowered()&&level!=lastLevel)power-=consumption;}super.tickServer();}
  public void setTarget(double target){targetLevel=Mth.clamp(target,0D,1D);} public double getMult(){return level;} @Override public int trackingRange(){return 100;} @Override public void onMelt(int reduce){if(isModerated())spawnDebris(EntityRBMKDebris.DebrisType.GRAPHITE,2+getLevel().getRandom().nextInt(2));spawnDebris(EntityRBMKDebris.DebrisType.ROD,2+getLevel().getRandom().nextInt(2));standardMelt(reduce);} @Override public RBMKType getRBMKType(){return RBMKType.CONTROL_ROD;} @Override public boolean hasPermission(Player p){if(p.containerMenu instanceof MenuRBMKControl manual)return manual.blockEntity()==this&&manual.stillValid(p);if(p.containerMenu instanceof MenuRBMKControlAuto automatic)return automatic.blockEntity()==this&&automatic.stillValid(p);return false;} @Override public void receiveControl(CompoundTag data){if(Tags.hasDouble(data,"level"))setTarget(Tags.getDoubleOr(data,"level",0D));}
 @Override public RBMKColumn getConsoleData(){RBMKColumn.ControlColumn c=(RBMKColumn.ControlColumn)super.getConsoleData();c.level=level;return c;} @Override protected void loadData(DataIn i){super.loadData(i);level=i.getDoubleOr("level",level);targetLevel=i.getDoubleOr("targetLevel",targetLevel);}@Override protected void saveData(DataOut o){super.saveData(o);o.putDouble("level",level);o.putDouble("targetLevel",targetLevel);}@Override public void serialize(RegistryFriendlyByteBuf b){super.serialize(b);b.writeDouble(level);b.writeDouble(targetLevel);b.writeLong(power);b.writeBoolean(hasPower);}@Override public void deserialize(RegistryFriendlyByteBuf b){super.deserialize(b);level=b.readDouble();targetLevel=b.readDouble();power=b.readLong();hasPower=b.readBoolean();}
  @Override public String[] getFunctionInfo(){return new String[]{PREFIX_VALUE+"extraction"};}
  @Override public String provideRORValue(String name){return (PREFIX_VALUE+"extraction").equals(name)?""+(int)(level*100):null;}
 }
