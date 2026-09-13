package com.hbm.tileentity.machine.oil;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.world.feature.OilSpot;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Fracking Tower — 1:1 port of the 1.12.2 {@code TileEntityMachineFrackingTower}. Extends the shared
 * {@link TileEntityOilDrillBase}: maxPower 5M, 5000 HE/tick, delay 20, drills to the world floor. A third
 * tank {@code [2] FRACKSOL 64k} (receiving) is consumed 10 mB per pump ({@link #canPump}); each suck also
 * blights the surface via {@link OilSpot} (range 75). Sucks both {@code ore_oil} (1000 oil + 100-500 gas,
 * 2% deplete) and {@code ore_bedrock_oil} (100 oil + 10-50 gas, never depletes). Reuses the oil-well GUI.
 *
 * <p>Deviations (§20): {@code getDrillDepth()} maps the old {@code 0} to the 26.1 world floor; IPersistentNBT
 * / IConfigurableMachine deferred (per the drill precedent).
 */
public class TileEntityMachineFrackingTower extends TileEntityOilDrillBase {

    protected static final int maxPower = 5_000_000;
    protected static final int consumption = 5000;
    protected static final int solutionRequired = 10;
    protected static final int delay = 20;
    protected static final int oilPerDeposit = 1000;
    protected static final int gasPerDepositMin = 100;
    protected static final int gasPerDepositMax = 500;
    protected static final double drainChance = 0.02D;
    protected static final int oilPerBedrockDeposit = 100;
    protected static final int gasPerBedrockDepositMin = 10;
    protected static final int gasPerBedrockDepositMax = 50;
    protected static final int destructionRange = 75;

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

    @Override
    public boolean canSuckBlock(Block b) {
        return super.canSuckBlock(b) || b == ModBlocks.ore_bedrock_oil.get();
    }

    @Override
    public void doSuck(BlockPos pos) {
        super.doSuck(pos);
        if (level.getBlockState(pos).getBlock() == ModBlocks.ore_bedrock_oil.get()) {
            onSuck(pos);
        }
    }

    @Override
    public void onSuck(BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();

        int oil = 0;
        int gas = 0;

        if (b == ModOilBlocks.ore_oil.get()) {
            tanks[0].setTankType(Fluids.OIL);
            oil = oilPerDeposit;
            gas = gasPerDepositMin + level.getRandom().nextInt(gasPerDepositMax - gasPerDepositMin + 1);
            if (level.getRandom().nextDouble() < drainChance) {
                level.setBlock(pos, ModBlocks.ore_oil_empty.get().defaultBlockState(), 3);
            }
        }

        if (b == ModBlocks.ore_bedrock_oil.get()) {
            oil = oilPerBedrockDeposit;
            gas = gasPerBedrockDepositMin + level.getRandom().nextInt(gasPerBedrockDepositMax - gasPerBedrockDepositMin + 1);
        }

        tanks[0].setFill(Math.min(tanks[0].getFill() + oil, tanks[0].getMaxFill()));
        tanks[1].setFill(Math.min(tanks[1].getFill() + gas, tanks[1].getMaxFill()));
        tanks[2].setFill(tanks[2].getFill() - solutionRequired);

        if (level instanceof ServerLevel server) {
            OilSpot.generateOilSpot(server, worldPosition.getX(), worldPosition.getZ(), destructionRange, 10, false);
        }
    }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[0], tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[2]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z)
        };
    }


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
