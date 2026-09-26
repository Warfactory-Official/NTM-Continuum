package com.hbm.blocks.network;

import com.hbm.tileentity.network.TileEntityPipeGauge;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.api.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The 1.7 {@code FluidDuctGauge}: the coated cube duct with a flow gauge on the face it was placed from
 * (1.7 {@code BlockPistonBase.determineOrientation} → the face toward the placer, any of six). The look
 * overlay adds the net's mB/t and mB/s from {@link TileEntityPipeGauge}.
 */
public class FluidDuctGauge extends FluidDuctPaintable {

    public static final EnumProperty<Direction> GAUGE = BlockStateProperties.FACING;

    public FluidDuctGauge(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(GAUGE, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GAUGE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) return null;
        // 1.7 determineOrientation: the block face pointing back at the placer (straight up/down when the
        // placer is close and looking steeply, else the horizontal opposite of their look direction).
        return state.setValue(GAUGE, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPipeGauge(pos, state);
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityPipeGauge duct)) return null;
        FluidType type = duct.getFluidType();
        List<String> text = new ArrayList<>();
        text.add("&[" + type.getColor() + "&]" + type.getDisplayNameString());
        text.add(String.format(Locale.US, "%,d", duct.deltaTick) + " mB/t");
        text.add(String.format(Locale.US, "%,d", duct.deltaLastSecond) + " mB/s");
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return super.rotate(state, rotation).setValue(GAUGE, rotation.rotate(state.getValue(GAUGE)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return super.mirror(state, mirror).setValue(GAUGE, mirror.mirror(state.getValue(GAUGE)));
    }
}
