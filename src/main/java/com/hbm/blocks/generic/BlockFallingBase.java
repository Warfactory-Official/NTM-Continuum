package com.hbm.blocks.generic;

//? if >=1.21.1 {
import com.mojang.serialization.MapCodec;
//?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A plain gravity block — the port of the 1.12.2 {@code BlockFallingBase}, which was a bare
 * {@code BlockFalling} with no extra behaviour.
 *
 * <p>Vanilla's {@link FallingBlock} is <b>abstract</b> in 26.1 (it requires a {@link MapCodec}), and it
 * has no plain concrete subclass, so this thin wrapper supplies one via {@link #simpleCodec}.
 */
public class BlockFallingBase extends FallingBlock {

    //? if >=1.21.1 {
    public static final MapCodec<BlockFallingBase> CODEC = simpleCodec(BlockFallingBase::new);
    //?}

    public BlockFallingBase(BlockBehaviour.Properties properties) {
        super(properties);
    }

    //? if >=1.21.1 {
    @Override
    public MapCodec<? extends FallingBlock> codec() {
        return CODEC;
    }
    //?}

    /**
     * The falling-dust particle tint. 26.1 made this abstract; 1.12.2's {@code BlockFalling} had a
     * concrete default of {@code -16777216} (opaque black) which {@code BlockFallingBase} never
     * overrode, so that value is the faithful port.
     */
    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return -16777216;
    }
}
