package com.hbm.blocks.generic;

import net.minecraft.util.valueproviders.IntProvider;
import dev.golem.core.world.CompatDropExperienceBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Sellafite: the vitrified terrain left under a nuclear blast, ported from 1.12.2's
 * {@code BlockSellafieldSlaked}/{@code BlockSellafieldOre}. One block carries a {@code SHADE 0..9}
 * state (0 = light outer ring, 9 = near-black ground zero); the grayscale darkening is a client
 * {@code BlockColor} tint ({@code brightness = 1 − shade/15}, see {@code HbmBlockColors}) over 4
 * position-random texture variants. The 1.12.2 block exposed shades 0..15 but every system here (the
 * fallout transform rings) only writes 0..9.
 *
 * <p>Extends {@link DropExperienceBlock} so the sellafield gem ores (diamond/emerald/radgem) grant
 * 3–7 XP; plain terrain passes a zero provider.
 */
public class BlockSellafieldSlaked extends CompatDropExperienceBlock {

    public static final IntegerProperty SHADE = IntegerProperty.create("shade", 0, 9);

    public BlockSellafieldSlaked(IntProvider xp, Properties props) {
        super(xp, props);
        registerDefaultState(getStateDefinition().any().setValue(SHADE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHADE);
    }
}
