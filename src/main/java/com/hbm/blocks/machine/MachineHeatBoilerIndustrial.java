package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeatBoilerIndustrial;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Industrial boiler: a five-port, four-block-tall fluid heater. */
public class MachineHeatBoilerIndustrial extends BlockDummyable implements IFluidConnectorBlockMK2, ILookOverlay {
    public MachineHeatBoilerIndustrial(Properties properties) { super(properties); }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeatBoilerIndustrial(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{4, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);
        BlockPos core = new BlockPos(x + dir.offsetX * offset, y, z + dir.offsetZ * offset);
        addExtraPorts(level, core);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addExtraPorts(level, core);
    }

    private void addExtraPorts(Level level, BlockPos core) {
        makeExtra(level, core.east().getX(), core.getY(), core.getZ());
        makeExtra(level, core.west().getX(), core.getY(), core.getZ());
        makeExtra(level, core.getX(), core.getY(), core.north().getZ());
        makeExtra(level, core.getX(), core.getY(), core.south().getZ());
        makeExtra(level, core.getX(), core.getY() + 4, core.getZ());
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (stack.getItem() instanceof IItemFluidId identifier) {
            if (!level.isClientSide()) {
                BlockPos core = findCore(level, pos);
                if (core == null || !(level.getBlockEntity(core) instanceof TileEntityHeatBoilerIndustrial boiler)) {
                    return InteractionResult.FAIL;
                }
                FluidType type = identifier.getType(level, core.getX(), core.getY(), core.getZ(), stack);
                if (com.hbm.inventory.recipes.HeatRecipes.hasBoilerRecipe(type)) {
                    boiler.tanks[0].setTankType(type);
                    boiler.setChanged();
                    player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                            .append(type.getDisplayName()).append(Component.literal("!")));
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityHeatBoilerIndustrial boiler
                && com.hbm.api.energymk2.IEnergyConnectorBlock.isPort(boiler.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_INDUSTRIAL_BOILER.get(), TileEntityHeatBoilerIndustrial::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityHeatBoilerIndustrial boiler)) return null;
        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", boiler.heat) + " TU");
        text.add("§a-> §r" + boiler.tanks[0].getTankType().getDisplayNameString() + ": "
                + String.format("%,d", boiler.tanks[0].getFill()) + "/" + String.format("%,d", boiler.tanks[0].getMaxFill()) + " mB");
        text.add("§c<- §r" + boiler.tanks[1].getTankType().getDisplayNameString() + ": "
                + String.format("%,d", boiler.tanks[1].getFill()) + "/" + String.format("%,d", boiler.tanks[1].getMaxFill()) + " mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
