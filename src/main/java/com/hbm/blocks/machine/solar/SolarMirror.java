package com.hbm.blocks.machine.solar;

import com.hbm.tileentity.machine.solar.ModSolarBlockEntities;
import dev.golem.core.registry.TickingBlock;
import com.hbm.tileentity.machine.solar.TileEntitySolarMirror;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Non-opaque solar mirror whose base and aimed panel are drawn by its block-entity renderer. */
public class SolarMirror extends dev.golem.core.world.CompatBlock implements TickingBlock {

    public SolarMirror(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySolarMirror(pos, state);
    }

}
