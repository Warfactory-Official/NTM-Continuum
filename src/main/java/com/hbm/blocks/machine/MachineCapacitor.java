package com.hbm.blocks.machine;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityCapacitor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
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
import org.jspecify.annotations.Nullable;

/**
 * A standalone power buffer, one block of it. Ported from 1.12.2's {@code MachineCapacitor}.
 *
 * <p>Five of these exist, differing only in capacity, so the capacity lives on the block and every
 * variant shares one block entity type. Input is the face the block points at; output runs away from it
 * along {@link MachineCapacitorBus} blocks.
 */
public class MachineCapacitor extends dev.golem.core.world.CompatBlock implements dev.golem.core.registry.TickingBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    private final long capacity;

    public MachineCapacitor(long capacity, Properties properties) {
        super(properties);
        this.capacity = capacity;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    public long getCapacity() {
        return capacity;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCapacitor(pos, state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityCapacitor capacitor)) return null;
        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(capacitor.getPower()) + " / "
                + Library.getShortNumber(capacitor.getMaxPower()) + " HE");
        text.add("§a+" + Library.getShortNumber(capacitor.lastPowerReceived) + "HE/t");
        text.add("§c-" + Library.getShortNumber(capacitor.lastPowerSent) + "HE/t");
        return new ILookOverlay.Data(getName().getString(), 0xFFFF00, 0x404000, text);
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

}
