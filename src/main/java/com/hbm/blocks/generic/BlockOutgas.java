package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.ModOutgasBlocks;
import com.hbm.config.GeneralConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The old multi-source outgassing block: twelve registrations and four emission triggers. */
public class BlockOutgas extends dev.golem.core.world.CompatBlock {
    private final boolean randomTick;
    private final int rate;
    private final boolean onBreak;
    private final boolean onNeighbour;

    public BlockOutgas(Properties properties) {
        this(properties, false, 20, false, false);
    }

    public BlockOutgas(Properties properties, boolean randomTick, int rate, boolean onBreak) {
        this(properties, randomTick, rate, onBreak, false);
    }

    public BlockOutgas(Properties properties, boolean randomTick, int rate, boolean onBreak, boolean onNeighbour) {
        super(properties);
        this.randomTick = randomTick;
        this.rate = rate;
        this.onBreak = onBreak;
        this.onNeighbour = onNeighbour;
    }

    /** Retains the old tickRate value for diagnostics and GameTests. */
    public int outgasRate() {
        return rate;
    }

    public Block getGas() {
        if (GeneralConfig.enableRadon) {
            if (this == ModNuclearBlocks.ore_uranium.get() || this == ModNuclearBlocks.ore_uranium_scorched.get()
                    || this == ModNuclearBlocks.ore_gneiss_uranium.get()
                    || this == ModNuclearBlocks.ore_gneiss_uranium_scorched.get()
                    || this == ModNuclearBlocks.ore_nether_uranium.get()
                    || this == ModNuclearBlocks.ore_nether_uranium_scorched.get()) {
                return ModNuclearBlocks.gas_radon.get();
            }
            if (this == ModOutgasBlocks.block_corium_cobble.get()) return ModNuclearBlocks.gas_radon_dense.get();
            if (this == ModOutgasBlocks.ancient_scrap.get()) return ModNuclearBlocks.gas_radon_tomb.get();
        }

        if (GeneralConfig.enableCarbonMonoxide && this == ModOreBlocks.ore_nether_coal.get()) {
            return ModNuclearBlocks.gas_monoxide.get();
        }

        if (GeneralConfig.enableAsbestosDust) {
            if (this == ModBlocks.ore_asbestos.get() || this == ModBlocks.ore_gneiss_asbestos.get()
                    || this == ModOutgasBlocks.block_asbestos.get() || this == ModOutgasBlocks.deco_asbestos.get()
                    || this == ModOutgasBlocks.brick_asbestos.get() || this == ModBlocks.tile_lab.get()
                    || this == ModBlocks.tile_lab_cracked.get() || this == ModOutgasBlocks.tile_lab_broken.get()) {
                return ModNuclearBlocks.gas_asbestos.get();
            }
        }
        return Blocks.AIR;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!randomTick) return;
        Block gas = getGas();
        if (gas == Blocks.AIR) return;
        BlockPos target = pos.relative(Direction.getRandom(random));
        if (BlockGasBase.isAirLike(level.getBlockState(target))) level.setBlock(target, gas.defaultBlockState(), 3);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide() || !randomTick || getGas() != ModNuclearBlocks.gas_asbestos.get()) return;
        BlockPos above = pos.above();
        if (!BlockGasBase.isAirLike(level.getBlockState(above))) return;
        ServerLevel server = (ServerLevel) level;
        if (server.getRandom().nextInt(10) == 0) {
            server.setBlock(above, ModNuclearBlocks.gas_asbestos.get().defaultBlockState(), 3);
        }
        // TOWN_AURA was removed; MYCELIUM is the existing harmless dust-particle stand-in.
        for (int i = 0; i < 5; i++) {
            server.sendParticles(ParticleTypes.MYCELIUM,
                    pos.getX() + server.getRandom().nextDouble(), pos.getY() + 1.1D,
                    pos.getZ() + server.getRandom().nextDouble(), 1, 0, 0, 0, 0);
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        super.onBlockRemoved(state, level, pos, movedByPiston);
        if (level.isClientSide() || !onBreak) return;
        Block gas = getGas();
        if (gas != Blocks.AIR) level.setBlock(pos, gas.defaultBlockState(), 3);

        if (this == ModOutgasBlocks.ancient_scrap.get()) {
            // The source is gas by now, so schedule the callback on the gas block itself.
            if (gas instanceof BlockGasRadonTomb && level instanceof ServerLevel server)
                server.scheduleTick(pos, gas, 1);
        }
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                     boolean movedByPiston) {
        if (level.isClientSide() || !onNeighbour || level.getRandom().nextInt(3) != 0) return;
        // Air positions cannot own a BlockOutgas scheduled tick; tick the source and fan out there.
        if (level instanceof ServerLevel server) server.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Block gas = getGas();
        if (gas == Blocks.AIR) return;
        if (onNeighbour) {
            for (Direction direction : Direction.values()) {
                BlockPos target = pos.relative(direction);
                if (BlockGasBase.isAirLike(level.getBlockState(target)))
                    level.setBlock(target, gas.defaultBlockState(), 3);
            }
        } else if (BlockGasBase.isAirLike(level.getBlockState(pos))) {
            level.setBlock(pos, gas.defaultBlockState(), 3);
        }
    }
}
