package com.hbm.tileentity.machine.oil;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.container.ContainerMachineOilWell;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
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

/**
 * Oil Derrick — 1:1 port of the 1.12.2 {@code TileEntityMachineOilWell} (config name "derrick"):
 * maxPower 100k, 100 HE per eligible tick, drill delay 50t, 500 mB oil + 100-500 mB gas per deposit,
 * 5% drain chance. Plays the generic-swim "glug" on each successful suck. Power/fluid ports are the
 * 4 horizontal neighbors of the core at base height.
 */
public class TileEntityMachineOilWell extends TileEntityOilDrillBase {

    protected static final int maxPower = 100_000;
    protected static final int consumption = 100;
    protected static final int delay = 50;
    protected static final int oilPerDeposit = 500;
    protected static final int gasPerDepositMin = 100;
    protected static final int gasPerDepositMax = 500;
    protected static final double drainChance = 0.05D;

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
    public void onSuck(BlockPos pos) {
        level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, 2.0F, 0.5F);
        Block block = level.getBlockState(pos).getBlock();

        if (block == ModOilBlocks.ore_oil.get()) {
            tanks[0].setTankType(Fluids.OIL);
            tanks[1].setTankType(Fluids.GAS);

            tanks[0].setFill(Math.min(tanks[0].getFill() + oilPerDeposit, tanks[0].getMaxFill()));
            tanks[1].setFill(Math.min(tanks[1].getFill()
                    + (gasPerDepositMin + level.getRandom().nextInt(gasPerDepositMax - gasPerDepositMin + 1)), tanks[1].getMaxFill()));

            if (level.getRandom().nextDouble() < drainChance) {
                level.setBlock(pos, ModBlocks.ore_oil_empty.get().defaultBlockState(), 3);
            }
        }
    }

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
