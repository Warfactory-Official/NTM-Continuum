package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.MenuRBMKHeater;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.tileentity.ModRBMKBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;

/** Heat-exchanger column: a fluid identifier selects the first FT_Heatable conversion step. */
public class BlockEntityRBMKHeater extends BlockEntityRBMKBase implements IFluidProviderMK2, IFluidReceiverMK2, MenuProvider, IRORValueProvider {
    public final FluidTankNTM feed = new FluidTankNTM(Fluids.COOLANT, 16_000);
    public final FluidTankNTM steam = new FluidTankNTM(Fluids.COOLANT_HOT, 16_000);

    public BlockEntityRBMKHeater(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_HEATER.get(), pos, state, 1);
        ports = new ModulePortManFluidAdaptive(this).setInputTanks(feed).setOutputTanks(steam);
    }

    @Override public void tickServer() {
        ports.update(getLoaderPorts());
        updateFeedType();
        FT_Heatable trait = feed.getTankType().getTrait(FT_Heatable.class);
        if (trait != null) {
            FT_Heatable.HeatingStep step = trait.getFirstStep();
            steam.setTankType(step.typeProduced);
            double tempRange = heat - steam.getTankType().temperature;
            double efficiency = trait.getEfficiency(FT_Heatable.HeatingType.HEATEXCHANGER);
            if (tempRange > 0D && efficiency > 0D) {
                double tuPerDegree = 2_000D * efficiency;
                int inputOps = feed.getFill() / step.amountReq;
                int outputOps = (steam.getMaxFill() - steam.getFill()) / step.amountProduced;
                int tempOps = (int) Math.floor(tempRange * tuPerDegree / step.heatReq);
                int ops = Math.min(inputOps, Math.min(outputOps, tempOps));
                if (ops > 0) {
                    feed.setFill(feed.getFill() - step.amountReq * ops);
                    steam.setFill(steam.getFill() + step.amountProduced * ops);
                    heat -= (step.heatReq * ops / tuPerDegree) * efficiency;
                    markChanged();
                }
            }
        }
        super.tickServer();
    }

    /** A hot column with coolant in its input keeps its type, so it stays configurable until the input fills. */
    public void updateFeedType() {
        if ((heat <= 50 || feed.getFill() <= 0) && feed.setType(inventory, 0, 0)) markChanged();
    }

    private boolean feedHeatable() {
        FT_Heatable trait = feed.getTankType().getTrait(FT_Heatable.class);
        return trait != null && trait.getEfficiency(FT_Heatable.HeatingType.HEATEXCHANGER) > 0D;
    }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{feed, steam}; }

    @Override public void onMelt(int reduce) { spawnDebris(EntityRBMKDebris.DebrisType.BLANK, 1 + level.getRandom().nextInt(2)); super.onMelt(reduce); }
    @Override public RBMKColumnType getConsoleType() { return RBMKColumnType.HEATEX; }
    @Override public RBMKColumn getConsoleData() {
        RBMKColumn.HeaterColumn data = (RBMKColumn.HeaterColumn) super.getConsoleData();
        data.water = feed.getFill(); data.maxWater = feed.getMaxFill();
        data.steam = steam.getFill(); data.maxSteam = steam.getMaxFill();
        data.coldType = RBMKColumn.fluidId(feed.getTankType()); data.hotType = RBMKColumn.fluidId(steam.getTankType());
        return data;
    }
    @Override public long getFluidAvailable(FluidType type, int pressure) { return pressure == steam.getPressure() && type == steam.getTankType() ? steam.getFill() : 0L; }
    @Override public void useUpFluid(FluidType type, int pressure, long amount) { if (type == steam.getTankType() && pressure == steam.getPressure()) { steam.setFill(steam.getFill() - (int) Math.min(amount, steam.getFill())); markChanged(); } }
    @Override public long getDemand(FluidType type, int pressure) { return type == feed.getTankType() && pressure == feed.getPressure() && feedHeatable() ? (long) feed.getMaxFill() - feed.getFill() : 0L; }
    @Override public long transferFluid(FluidType type, int pressure, long amount) {
        if (type != feed.getTankType() || pressure != feed.getPressure() || !feedHeatable()) return amount;
        int accepted = feed.fill(type, (int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) markChanged();
        return amount - accepted;
    }
    @Override protected void saveData(DataOut output) { super.saveData(output); feed.writeToNBT(output, "feed"); steam.writeToNBT(output, "steam"); }
    @Override protected void loadData(DataIn input) { super.loadData(input); feed.readFromNBT(input, "feed"); steam.readFromNBT(input, "steam"); }
    @Override public void serialize(RegistryFriendlyByteBuf buffer) { super.serialize(buffer); feed.serialize(buffer); steam.serialize(buffer); }
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { super.deserialize(buffer); feed.deserialize(buffer); steam.deserialize(buffer); }
    @Override public Component getDisplayName() { return Component.translatable("container.rbmkHeater"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MenuRBMKHeater(id, inventory, this); }
    @Override public String[] getFunctionInfo() { return new String[]{PREFIX_VALUE + "in", PREFIX_VALUE + "out"}; }
    @Override public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "in").equals(name)) return "" + feed.getFill();
        if ((PREFIX_VALUE + "out").equals(name)) return "" + steam.getFill();
        return null;
    }
}
