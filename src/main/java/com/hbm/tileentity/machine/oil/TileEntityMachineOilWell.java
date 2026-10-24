package com.hbm.tileentity.machine.oil;

import com.hbm.api.data.DepositDataMaps;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
 * Oil Derrick, 1:1 port of the 1.12.2 {@code TileEntityMachineOilWell} (config name "derrick"):
 * maxPower 100k, 100 HE per eligible tick, drill delay 50t, 500 mB oil + 100-500 mB gas per deposit,
 * 5% drain chance. Plays the generic-swim "glug" on each successful suck. Power/fluid ports are the
 * 4 horizontal neighbors of the core at base height.
 */
public class TileEntityMachineOilWell extends TileEntityOilDrillBase {

    public static int maxPower = 100_000;
    public static int consumption = 100;
    public static int delay = 50;
    public static int oilPerDeposit = 500;
    public static int gasPerDepositMin = 100;
    public static int gasPerDepositMax = 500;
    public static double drainChance = 0.05D;

    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "derrick"; }

        @Override public void readIfPresent(JsonObject obj) {
            maxPower = IConfigurableMachine.grab(obj, "I:powerCap", maxPower);
            consumption = IConfigurableMachine.grab(obj, "I:consumption", consumption);
            delay = IConfigurableMachine.grab(obj, "I:delay", delay);
            oilPerDeposit = IConfigurableMachine.grab(obj, "I:oilPerDeposit", oilPerDeposit);
            gasPerDepositMin = IConfigurableMachine.grab(obj, "I:gasPerDepositMin", gasPerDepositMin);
            gasPerDepositMax = IConfigurableMachine.grab(obj, "I:gasPerDepositMax", gasPerDepositMax);
            drainChance = IConfigurableMachine.grab(obj, "D:drainChance", drainChance);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:powerCap").value(maxPower);
            writer.name("I:consumption").value(consumption);
            writer.name("I:delay").value(delay);
            writer.name("I:oilPerDeposit").value(oilPerDeposit);
            writer.name("I:gasPerDepositMin").value(gasPerDepositMin);
            writer.name("I:gasPerDepositMax").value(gasPerDepositMax);
            writer.name("D:drainChance").value(drainChance);
        }
    };

    public TileEntityMachineOilWell(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_OIL_WELL.get(), pos, state);
    }

    @Override
    public String getDefaultName() {
        return "container.oilWell";
    }

    @Override public long getMaxPower() { return maxPower; }
    @Override public int getPowerReq() { return consumption; }
    @Override public int getDelay() { return delay; }

    @Override
    public void onDrill(int y) {
        Block gas = gasOfDrilledOre(y);
        if (gas == null) return;
        // 1.7 tests the cell at (j, j) but fills (k, k), so only the diagonal of the 3x3 gets gas
        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                if (level.getBlockState(worldPosition.offset(j, 10, j)).canBeReplaced()) {
                    level.setBlockAndUpdate(worldPosition.offset(k, 10, k), gas.defaultBlockState());
                }
            }
        }
    }

    @Override
    public void onSuck(BlockPos pos, DepositDataMaps.Deposit deposit) {
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 2.0F, 0.5F);
        suckDeposit(pos, deposit, oilPerDeposit, gasPerDepositMin, gasPerDepositMax, drainChance);
    }

    protected PortDef[] cachedPorts;
    @Override
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.horizontal(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.oilWell");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineOilWell(containerId, playerInventory, this);
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_well.get()));
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
