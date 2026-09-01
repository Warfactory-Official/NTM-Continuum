package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeaterHeatex;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Containers;
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

/** The 3x3 legacy heat-exchanger multiblock. */
public class HeaterHeatex extends BlockDummyable implements IFluidConnectorBlockMK2, ILookOverlay {

    public HeaterHeatex(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeaterHeatex(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level level, int x, int y, int z, ForgeDirection dir, int offset) {
        super.fillSpace(level, x, y, z, dir, offset);

        x += dir.offsetX * offset;
        z += dir.offsetZ * offset;
        makeExtra(level, x + dir.offsetX + dir.getRotation(ForgeDirection.UP).offsetX,
                y, z + dir.offsetZ + dir.getRotation(ForgeDirection.UP).offsetZ);
        makeExtra(level, x + dir.offsetX - dir.getRotation(ForgeDirection.UP).offsetX,
                y, z + dir.offsetZ - dir.getRotation(ForgeDirection.UP).offsetZ);
        makeExtra(level, x - dir.offsetX + dir.getRotation(ForgeDirection.UP).offsetX,
                y, z - dir.offsetZ + dir.getRotation(ForgeDirection.UP).offsetZ);
        makeExtra(level, x - dir.offsetX - dir.getRotation(ForgeDirection.UP).offsetX,
                y, z - dir.offsetZ - dir.getRotation(ForgeDirection.UP).offsetZ);
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        super.formStructure(level, core, dir);
        addExtraPorts(level, core, dir);
    }

    private void addExtraPorts(Level level, BlockPos core, ForgeDirection dir) {
        ForgeDirection lateral = dir.getRotation(ForgeDirection.UP);
        makeExtra(level, core.getX() + dir.offsetX + lateral.offsetX, core.getY(),
                core.getZ() + dir.offsetZ + lateral.offsetZ);
        makeExtra(level, core.getX() + dir.offsetX - lateral.offsetX, core.getY(),
                core.getZ() + dir.offsetZ - lateral.offsetZ);
        makeExtra(level, core.getX() - dir.offsetX + lateral.offsetX, core.getY(),
                core.getZ() - dir.offsetZ + lateral.offsetZ);
        makeExtra(level, core.getX() - dir.offsetX - lateral.offsetX, core.getY(),
                core.getZ() - dir.offsetZ - lateral.offsetZ);
    }

    /** Sneak + fluid identifier changes the input type; ordinary use opens the legacy menu. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            if (!(stack.getItem() instanceof com.hbm.items.machine.IItemFluidId identifier)) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (level.isClientSide()) return InteractionResult.SUCCESS;

            BlockEntity core = findCoreTE(level, pos);
            if (!(core instanceof TileEntityHeaterHeatex heater)) return InteractionResult.FAIL;
            FluidType type = identifier.getType(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            heater.tanks[0].setTankType(type);
            heater.setChanged();
            player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                    .append(type.getDisplayName()).append(Component.literal("!")));
            return InteractionResult.SUCCESS;
        }

        return openMenu(level, pos, player);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return openMenu(level, pos, player);
    }

    private InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        BlockPos core = findCore(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityHeaterHeatex heater)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        dev.golem.core.gui.Menus.openWithPos(serverPlayer, heater, core);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!BlockDummyable.safeRem && level.getBlockEntity(pos) instanceof TileEntityHeaterHeatex heater) {
            Containers.dropContents(level, pos, heater);
        }
        super.onBlockRemoved(state, level, pos, movedByPiston);
    }

    /** Fluid connections are accepted only at the four remote ports, as in the old proxy path. */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos cell = new BlockPos(x, y, z);
        return findCoreTE(world, cell) instanceof TileEntityHeaterHeatex heater
                && com.hbm.api.energymk2.IEnergyConnectorBlock.isPort(heater.getConPos(), cell, dir);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(findCoreTE(level, pos) instanceof TileEntityHeaterHeatex heater)) return null;
        List<String> text = new ArrayList<>();
        text.add(String.format("%,d", heater.heatEnergy) + " TU");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, ModBlockEntities.HEATER_HEATEX.get(), TileEntityHeaterHeatex::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
