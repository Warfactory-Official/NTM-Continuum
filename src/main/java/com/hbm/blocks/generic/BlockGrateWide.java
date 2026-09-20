package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The 1.7 {@code steel_grate_wide} ("Item Grate") flavour of {@link BlockGrate}: identical, but dropped
 * items and experience orbs fall straight through it (the old {@code addCollisionBoxesToList} skipped
 * {@code EntityItem}/{@code EntityXPOrb}).
 */
public class BlockGrateWide extends BlockGrate {

    public BlockGrateWide(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && (entityContext.getEntity() instanceof ItemEntity || entityContext.getEntity() instanceof ExperienceOrb)) {
            return Shapes.empty();
        }
        return super.getCollisionShape(state, level, pos, context);
    }
}
