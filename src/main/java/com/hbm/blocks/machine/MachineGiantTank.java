package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityMachineGiantTank;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Common block-level connector, comparator, and interaction behavior for the two barrel variants. */
public abstract class MachineGiantTank extends BlockDummyable
        implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    protected MachineGiantTank(Properties properties) {
        super(properties);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !ModDataComponents.FLUID_AMOUNT.has(stack)) return;
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityMachineGiantTank tank)) return;

        tank.setTankType(Fluids.fromID(ModDataComponents.fluidId(stack)));
        tank.tankNew.withPressure(ModDataComponents.fluidPressure(stack));
        tank.tankNew.setFill(Math.max(0, Math.min(tank.tankNew.getMaxFill(), ModDataComponents.fluidAmount(stack))));
        tank.mode = (short) Math.max(0, Math.min(3, ModDataComponents.GIANT_TANK_MODE.getOr(stack, 0)));
        tank.setChanged();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        BlockPos origin = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockPos core = findCore(params.getLevel(), origin);
        if (core != null && params.getLevel().getBlockEntity(core) instanceof TileEntityMachineGiantTank tank
                && tank.tankNew.getFill() > 0) {
            ModDataComponents.fluidId(stack, tank.tankNew.getTankType().getID());
            ModDataComponents.fluidAmount(stack, tank.tankNew.getFill());
            if (tank.tankNew.getPressure() != 0) ModDataComponents.fluidPressure(stack, tank.tankNew.getPressure());
            ModDataComponents.GIANT_TANK_MODE.set(stack, (int) tank.mode);
        }
        return List.of(stack);
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof WorldlyContainer container ? container : null;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos cell = new BlockPos(x, y, z);
        return dir != ForgeDirection.UNKNOWN
                && findCoreTE(world, cell) instanceof TileEntityMachineGiantTank tank
                && IEnergyConnectorBlock.isPort(tank.getConPos(), cell, dir);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return findCoreTE(level, pos) instanceof TileEntityMachineGiantTank tank
                ? tank.tankNew.getRedstoneComparatorPower() : 0;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.SUCCESS;
        return super.onUseWithoutItem(state, level, pos, player, hit);
    }
}
