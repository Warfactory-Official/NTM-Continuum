package com.hbm.blocks.machine;

import dev.golem.core.data.Tags;

import dev.golem.core.data.CompatBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import com.hbm.tileentity.ModDecoBlockEntities;
import com.hbm.tileentity.machine.TileEntityFloodlight;

/**
 * 1.7 {@code FloodlightBeam}: the invisible full-light block a {@link TileEntityFloodlight} drops at
 * the end of each ray. Its block entity remembers the source and ray index and removes the beam every
 * fifth tick once the source is gone, dark, or shining somewhere else. See {@link SpotlightBeam} for
 * why this is {@code replaceable()} rather than {@code air()}.
 */
public class FloodlightBeam extends Block implements TickingBlock {

    public FloodlightBeam(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
    // Skylight passes: vanilla derives propagatesSkylightDown from the empty shape (its signature differs per era).
    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFloodlightBeam(pos, state);
    }

    /** 1.7 {@code FloodlightBeam.TileEntityFloodlightBeam}. */
    public static class TileEntityFloodlightBeam extends CompatBlockEntity {

        public TileEntityFloodlight cache;
        public BlockPos source = BlockPos.ZERO;
        public int index;

        public TileEntityFloodlightBeam(BlockPos pos, BlockState state) {
            super(ModDecoBlockEntities.FLOODLIGHT_BEAM.get(), pos, state);
        }

        public void serverTick() {
            Level level = getLevel();
            if (level == null || level.isClientSide() || level.getGameTime() % 5 != 0) return;
            if (cache == null) {
                if (level.hasChunkAt(source)) {
                    if (level.getBlockEntity(source) instanceof TileEntityFloodlight floodlight) {
                        cache = floodlight; // chunk is loaded, tile exists -> cache
                    } else {
                        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 2); // chunk is loaded, tile does not exist -> delete self
                        return;
                    }
                }
            }
            if ((cache != null && (cache.isRemoved() || !cache.isOn || !cache.holdsLight(index, worldPosition))) || source.getY() == 0) {
                level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 2);
            }
        }

        public void setSource(TileEntityFloodlight floodlight, BlockPos at, int i) {
            cache = floodlight;
            source = at;
            index = i;
        }

        @Override
        protected void loadData(DataIn in) {
            super.loadData(in);
            source = new BlockPos(in.getIntOr("sourceX", 0), in.getIntOr("sourceY", 0), in.getIntOr("sourceZ", 0));
            index = in.getIntOr("index", 0);
        }

        @Override
        protected void saveData(DataOut out) {
            super.saveData(out);
            out.putInt("sourceX", source.getX());
            out.putInt("sourceY", source.getY());
            out.putInt("sourceZ", source.getZ());
            out.putInt("index", index);
        }
    }
}
