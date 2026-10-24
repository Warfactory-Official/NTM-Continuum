package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.machine.rbmk.RBMKLoader;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.container.MenuRBMKBoiler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.ModRBMKBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;

/** Water-to-steam RBMK heat column, including the original compression ratios. */
public class BlockEntityRBMKBoiler extends BlockEntityRBMKBase implements IFluidProviderMK2, IFluidReceiverMK2, IFluidStandardTransceiverMK2,
        IControlReceiver, MenuProvider, IRORValueProvider {
    @Override public RBMKColumnType getConsoleType() { return RBMKColumnType.BOILER; }
    @Override public void onMelt(int reduce) { spawnDebris(EntityRBMKDebris.DebrisType.BLANK, 1 + level.getRandom().nextInt(2)); super.onMelt(reduce); }
    public final FluidTankNTM feed = new FluidTankNTM(Fluids.WATER, 10_000), steam = new FluidTankNTM(Fluids.STEAM, 1_000_000);
    public int consumption, output;
    public int ventDelay;
    public BlockEntityRBMKBoiler(BlockPos pos, BlockState state) { super(ModRBMKBlockEntities.RBMK_BOILER.get(), pos, state, 0); ports = new ModulePortManFluidAdaptive(this).setInputTanks(feed).setOutputTanks(steam); }
    public static double heat(FluidType f) { return f == Fluids.STEAM ? 100 : f == Fluids.HOTSTEAM ? 300 : f == Fluids.SUPERHOTSTEAM ? 450 : f == Fluids.ULTRAHOTSTEAM ? 600 : 0; }
    public static double factor(FluidType f) { return f == Fluids.STEAM ? 1 : f == Fluids.HOTSTEAM ? 10 : f == Fluids.SUPERHOTSTEAM ? 100 : f == Fluids.ULTRAHOTSTEAM ? 1000 : 0; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{feed, steam}; }
    @Override public void tickServer() { ports.update(getLoaderPorts()); consumption = output = 0; if (ventDelay > 0) ventDelay--; double available = heat - heat(steam.getTankType()); if (available > 0) { double cost = RBMKDials.getBoilerHeatConsumption(level), factor = factor(steam.getTankType()); int used, made; if (steam.getTankType() == Fluids.ULTRAHOTSTEAM) { made = (int)Math.floor(available / cost * 100 / factor); used = (int)Math.floor(made / 100D * factor); if (used > feed.getFill()) { made = (int)Math.floor(feed.getFill() * 100D / factor); used = (int)Math.floor(made / 100D * factor); } } else { used = Math.min((int)Math.floor(available / cost), feed.getFill()); made = (int)Math.floor(used * 100D / factor); } consumption = used; output = made; feed.setFill(feed.getFill() - used); int total = steam.getFill() + made; steam.setFill(Math.min(steam.getMaxFill(), total)); if (total > steam.getMaxFill()) vent(); heat -= used * cost; } super.tickServer(); }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{feed}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{steam}; }
    /** A full steam tank blows off a jet above the column, at most every 20 to 30 ticks. */
    private void vent() {
        if (ventDelay > 0) return;
        RandomSource random = level.getRandom();
        double x = worldPosition.getX(), y = worldPosition.getY() + RBMKDials.getColumnHeight(level), z = worldPosition.getZ();
        HbmEffectNT.RBMKSteam.send(level, x + .25D + random.nextInt(2) * .5D, y, z + .25D + random.nextInt(2) * .5D, null, 100D);
        ventDelay = 20 + random.nextInt(10);
        level.playSound(null, x, y, z, ModSounds.STEAM_ENGINE_OPERATE.get(), SoundSource.BLOCKS, 2F, 1F + random.nextFloat() * .25F);
    }
    public void cycleCompressor() { if (heat > 50 && feed.getFill() > 0) return; FluidType f = steam.getTankType(); if (f == Fluids.STEAM) { steam.setTankType(Fluids.HOTSTEAM); steam.setFill(steam.getFill()/10); } else if (f == Fluids.HOTSTEAM) { steam.setTankType(Fluids.SUPERHOTSTEAM); steam.setFill(steam.getFill()/10); } else if (f == Fluids.SUPERHOTSTEAM) { steam.setTankType(Fluids.ULTRAHOTSTEAM); steam.setFill(steam.getFill()/10); } else if (f == Fluids.ULTRAHOTSTEAM) { steam.setTankType(Fluids.STEAM); steam.setFill(Math.min(steam.getFill()*1000, steam.getMaxFill())); } setChanged(); }
     @Override public boolean hasPermission(Player player) { return player.containerMenu instanceof MenuRBMKBoiler menu && menu.blockEntity() == this && menu.stillValid(player); }
     @Override public void receiveControl(CompoundTag data) { if (Tags.getBooleanOr(data, "compression", false)) cycleCompressor(); }
    @Override public RBMKColumn getConsoleData() { RBMKColumn.BoilerColumn data=(RBMKColumn.BoilerColumn)super.getConsoleData(); data.water=feed.getFill(); data.maxWater=feed.getMaxFill(); data.steam=steam.getFill(); data.maxSteam=steam.getMaxFill(); data.steamType=RBMKColumn.fluidId(steam.getTankType()); return data; }
    @Override public long getFluidAvailable(FluidType t, int p) { return steam.provides(t) && p == steam.getPressure() ? steam.getFill() : 0; }
    @Override public void useUpFluid(FluidType t, int p, long a) { if (getFluidAvailable(t,p)>0) { steam.setFill(steam.getFill()-(int)Math.min(a,steam.getFill())); setChanged(); } }
    @Override public long getDemand(FluidType t, int p) { return Fluids.kindOf(t) == Fluids.WATER && feed.accepts(t) && p == feed.getPressure() ? feed.getMaxFill()-feed.getFill() : 0; }
    @Override public long transferFluid(FluidType t,int p,long a) { if (Fluids.kindOf(t) != Fluids.WATER || !feed.accepts(t) || p != feed.getPressure()) return a; int n=feed.fill(t,(int)Math.min(a,Integer.MAX_VALUE),true); if(n>0)setChanged(); return a-n; }
    @Override protected void saveData(DataOut o) { super.saveData(o); feed.writeToNBT(o,"feed"); steam.writeToNBT(o,"steam"); }
    @Override protected void loadData(DataIn i) { super.loadData(i); feed.readFromNBT(i,"feed"); steam.readFromNBT(i,"steam"); }
    @Override public void serialize(RegistryFriendlyByteBuf buffer) { super.serialize(buffer); steam.serialize(buffer); feed.serialize(buffer); }
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { super.deserialize(buffer); steam.deserialize(buffer); feed.deserialize(buffer); }
    @Override public Component getDisplayName() { return Component.translatable("container.rbmkBoiler"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MenuRBMKBoiler(id, inventory, this); }
     @Override public String[] getFunctionInfo() { return new String[]{PREFIX_VALUE + "feed", PREFIX_VALUE + "steam", PREFIX_VALUE + "consumption"}; }
     @Override public String provideRORValue(String name) {
         if ((PREFIX_VALUE + "feed").equals(name)) return "" + feed.getFill();
         if ((PREFIX_VALUE + "steam").equals(name)) return "" + steam.getFill();
         if ((PREFIX_VALUE + "consumption").equals(name)) return "" + consumption;
         return null;
     }
}
