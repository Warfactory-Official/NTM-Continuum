package com.hbm.blocks.machine.storage;

import com.hbm.inventory.container.ContainerMassStorage;
import com.hbm.tileentity.machine.storage.MassStorageType;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import com.hbm.interfaces.ILookOverlay;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CarriedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * A parameterized mass-storage block. Registration supplies the matching BE type and menu type.
 *
 * <p>1.7 packs the facing into metadata ({@code meta / 4}: north, south, west, east) and draws the front
 * texture and {@code RenderMassStorage}'s panel on the side that faced the placer. A block saved
 * before this property existed loads with the default, north, which is 1.7's metadata-0 face.
 */
public class BlockMassStorage extends CarriedBlock implements TickingBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final MassStorageType storageType;
    private final Supplier<? extends BlockEntityType<TileEntityMassStorage>> blockEntityType;
    private final Supplier<MenuType<ContainerMassStorage>> menuType;

    public BlockMassStorage(Properties properties, MassStorageType storageType) {
        this(properties, storageType, storageType::blockEntityType, storageType::menuType);
    }

    public BlockMassStorage(Properties properties, MassStorageType storageType,
                            Supplier<? extends BlockEntityType<TileEntityMassStorage>> blockEntityType,
                            Supplier<MenuType<ContainerMassStorage>> menuType) {
        super(properties);
        this.storageType = storageType;
        this.blockEntityType = blockEntityType;
        this.menuType = menuType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** 1.7 {@code onBlockPlacedBy}: the front faces the placer. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMassStorage(storageType, blockEntityType, menuType, pos, state);
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
        if (TileEntityLockableBase.LOCK_TOOLS.test(player.getMainHandItem())) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && !player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileEntityMassStorage storage && storage.canAccess(player)) {
            Menus.openWithPos(serverPlayer, storage, pos);
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

}
