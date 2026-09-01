package com.hbm.blocks.machine.storage;

import com.hbm.tileentity.machine.storage.MassStorageType;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import com.hbm.interfaces.ILookOverlay;
import dev.golem.core.world.CarriedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** A parameterized mass-storage block. Registration supplies the matching BE type and menu type. */
public class BlockMassStorage extends CarriedBlock implements ILookOverlay {

    private final MassStorageType storageType;
    private final Supplier<? extends BlockEntityType<TileEntityMassStorage>> blockEntityType;
    private final Supplier<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerMassStorage>> menuType;

    public BlockMassStorage(Properties properties, MassStorageType storageType) {
        this(properties, storageType, storageType::blockEntityType, storageType::menuType);
    }

    public BlockMassStorage(Properties properties, MassStorageType storageType,
                            Supplier<? extends BlockEntityType<TileEntityMassStorage>> blockEntityType,
                            Supplier<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerMassStorage>> menuType) {
        super(properties);
        this.storageType = storageType;
        this.blockEntityType = blockEntityType;
        this.menuType = menuType;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMassStorage(storageType, blockEntityType, menuType, pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, blockEntityType.get(), TileEntityMassStorage::serverTick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TileEntityMassStorage storage
                ? storage.getComparatorPower() : 0;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
        }
        return level.isClientSide() || !player.isShiftKeyDown()
                ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityMassStorage storage)) return null;

        ItemStack type = storage.getFilterType();
        boolean full = !type.isEmpty();
        List<String> lines = new ArrayList<>();
        if (full) {
            lines.add(String.format(Locale.US, "%,d / %,d", storage.getStockpile(), storage.getCapacity()));
            double fraction = (double) storage.getStockpile() / storage.getCapacity();
            int charge = (int) Math.floor(fraction * 10_000D);
            int color = ((int) (0xFF - 0xFF * fraction)) << 16 | ((int) (0xFF * fraction) << 8);
            lines.add("&[" + color + "&]" + (charge / 100D) + "%");
        }
        return new ILookOverlay.Data(full ? type.getHoverName().getString() : "Empty",
                full ? 0xffff00 : 0x00ffff, full ? 0x404000 : 0x004040, lines);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
