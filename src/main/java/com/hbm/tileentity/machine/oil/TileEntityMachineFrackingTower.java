package com.hbm.tileentity.machine.oil;

import com.hbm.api.data.DepositDataMaps;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.world.feature.OilSpot;
import dev.golem.core.hook.Hook;
import dev.golem.core.hook.Verdict;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import com.hbm.tileentity.IConfigurableMachine;

/**
 * Fracking Tower: 1:1 port of the 1.12.2 {@code TileEntityMachineFrackingTower}. Extends the shared
 * {@link TileEntityOilDrillBase}: maxPower 5M, 5000 HE/tick, delay 20, drills to the world floor. A third
 * tank {@code [2] FRACKSOL 64k} (receiving) is consumed 10 mB per pump ({@link #canPump}); each suck also
 * blights the surface via {@link OilSpot} (range 75). Sucks both {@code ore_oil} (1000 oil + 100-500 gas,
 * 2% deplete) and {@code ore_bedrock_oil} (100 oil + 10-50 gas, never depletes). Reuses the oil-well GUI.
 *
 * <p>Deviations: {@code getDrillDepth()} maps the old {@code 0} to the 26.1 world floor; IPersistentNBT deferred
 * (per the drill precedent).
 */
public class TileEntityMachineFrackingTower extends TileEntityOilDrillBase {

    public static int maxPower = 5_000_000;
    public static int consumption = 5000;
    public static int solutionRequired = 10;
    public static int delay = 20;
    /** The deposit's {@code hbm:fluid_deposit} yield scales these; NTM: Space assigns its own at start-up. */
    public static int oilPerDeposit = 1000;
    public static int gasPerDepositMin = 100;
    public static int gasPerDepositMax = 500;
    public static double drainChance = 0.02D;
    /** 1.7's bedrock amounts: they scale what a fracking-only deposit gives, so the defaults change nothing. */
    public static int oilPerBedrockDeposit = 100;
    public static int gasPerBedrockDepositMin = 10;
    public static int gasPerBedrockDepositMax = 50;
    public static int destructionRange = 75;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "frackingtower"; }

        @Override public void readIfPresent(JsonObject obj) {
            maxPower = IConfigurableMachine.grab(obj, "I:powerCap", maxPower);
            consumption = IConfigurableMachine.grab(obj, "I:consumption", consumption);
            solutionRequired = IConfigurableMachine.grab(obj, "I:solutionRequired", solutionRequired);
            delay = IConfigurableMachine.grab(obj, "I:delay", delay);
            oilPerDeposit = IConfigurableMachine.grab(obj, "I:oilPerDeposit", oilPerDeposit);
            gasPerDepositMin = IConfigurableMachine.grab(obj, "I:gasPerDepositMin", gasPerDepositMin);
            gasPerDepositMax = IConfigurableMachine.grab(obj, "I:gasPerDepositMax", gasPerDepositMax);
            drainChance = IConfigurableMachine.grab(obj, "D:drainChance", drainChance);
            oilPerBedrockDeposit = IConfigurableMachine.grab(obj, "I:oilPerBedrockDeposit", oilPerBedrockDeposit);
            gasPerBedrockDepositMin = IConfigurableMachine.grab(obj, "I:gasPerBedrockDepositMin", gasPerBedrockDepositMin);
            gasPerBedrockDepositMax = IConfigurableMachine.grab(obj, "I:gasPerBedrockDepositMax", gasPerBedrockDepositMax);
            destructionRange = IConfigurableMachine.grab(obj, "I:destructionRange", destructionRange);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:powerCap").value(maxPower);
            writer.name("I:consumption").value(consumption);
            writer.name("I:solutionRequired").value(solutionRequired);
            writer.name("I:delay").value(delay);
            writer.name("I:oilPerDeposit").value(oilPerDeposit);
            writer.name("I:gasPerDepositMin").value(gasPerDepositMin);
            writer.name("I:gasPerDepositMax").value(gasPerDepositMax);
            writer.name("D:drainChance").value(drainChance);
            writer.name("I:oilPerBedrockDeposit").value(oilPerBedrockDeposit);
            writer.name("I:gasPerBedrockDepositMin").value(gasPerBedrockDepositMin);
            writer.name("I:gasPerBedrockDepositMax").value(gasPerBedrockDepositMax);
            writer.name("I:destructionRange").value(destructionRange);
        }
    };

    public TileEntityMachineFrackingTower(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_FRACKING_TOWER.get(), pos, state);
        this.tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.OIL, 64_000),
                new FluidTankNTM(Fluids.GAS, 64_000),
                new FluidTankNTM(Fluids.FRACKSOL, 64_000)
        };
    }

    @Override public String getDefaultName() { return "container.frackingTower"; }

    @Override public long getMaxPower() { return maxPower; }
    @Override public int getPowerReq() { return consumption; }
    @Override public int getDelay() { return delay; }

    @Override
    public int getDrillDepth() {
        return level.getMinY(); // old: 0 (drill to bedrock)
    }

    @Override
    public boolean canPump() {
        boolean b = this.tanks[2].getFill() >= solutionRequired;
        if (!b) this.indicator = 3;
        return b;
    }

    /** 1.7's fracking tower also reaches {@code ore_bedrock_oil} (the fracking-only deposits). */
    @Override
    protected boolean canFrack() {
        return true;
    }

    /**
     * What sucking a deposit does to the surface around the tower. HBM's default (no listener deciding) is 1.7's oil spot
     * of radius 75; a listener that answers {@link Verdict#DENY} replaces it (NTM: Space cracks Tekto's ground instead).
     */
    @FunctionalInterface
    public interface Blight {
        Verdict blight(ServerLevel level, BlockPos tower, BlockPos deposit, DepositDataMaps.Deposit kind);
    }

    public static final Hook<Blight> BLIGHT = Hook.of(listeners -> (level, tower, deposit, kind) ->
            Verdict.first(listeners, listener -> listener.blight(level, tower, deposit, kind)));

    @Override
    public void onSuck(BlockPos pos, DepositDataMaps.Deposit deposit) {
        // 1.7 ore_oil: 1000 oil + 100-500 gas, 2% drain; ore_bedrock_oil: 100 + 10-50, never drains (yield 0.1, no empty)
        if (deposit.frackingOnly()) {
            suckAmounts(pos, deposit, scale(deposit.primaryAmount(oilPerDeposit), oilPerBedrockDeposit, 100),
                    scale(deposit.secondaryAmount(gasPerDepositMin), gasPerBedrockDepositMin, 10),
                    scale(deposit.secondaryAmount(gasPerDepositMax), gasPerBedrockDepositMax, 50), drainChance);
        } else {
            suckDeposit(pos, deposit, oilPerDeposit, gasPerDepositMin, gasPerDepositMax, drainChance);
        }
        tanks[2].setFill(tanks[2].getFill() - solutionRequired);

        if (level instanceof ServerLevel server
                && BLIGHT.invoker().blight(server, worldPosition, pos, deposit) != Verdict.DENY) {
            OilSpot.generateOilSpot(server, worldPosition.getX(), worldPosition.getZ(), destructionRange, 10, false);
        }
    }

    private static int scale(int amount, int bedrock, int bedrockDefault) {
        return (int) Math.round((double) amount * bedrock / bedrockDefault);
    }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[0], tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[2]}; }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    protected PortDef[] cachedPorts;
    @Override
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.horizontal(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frackingTower");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineOilWell(containerId, playerInventory, this);
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_fracking_tower.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 25) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + (level * 10) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.AFTERBURN) {
            info.add(Component.translatable(KEY_BURN, level * 10, level * 50).withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }
}
