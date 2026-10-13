package com.hbm.blocks.bomb;

import com.hbm.api.block.IToolable;

import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockAllocatorWater;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.interfaces.IBomb;
import com.hbm.items.weapon.sedna.factory.XFactoryCatapult;
import com.hbm.main.MainRegistry;
import com.hbm.particle.helper.EntityEffects;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.sound.ModSounds;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import com.hbm.config.MineConfig;
import com.hbm.tileentity.bomb.TileEntityLandmine;

/**
 * 1.7 {@code Landmine}: five variants of one block class, each with its own trigger box and blast.
 * Rendered by a block-entity renderer ({@code getRenderType() == -1}); the block itself is invisible.
 * Drops nothing when broken (breaking it detonates it); only the defuser lifts it as an item.
 *
 * <p>The static {@code safeMode} flag is upstream's: it is raised while the mine removes itself so the
 * destroyed-by-player / lost-its-support paths do not detonate a mine that is already going off.
 */
public class Landmine extends CompatBlock implements TickingBlock, IBomb, IToolable {

    public enum Type { AP, HE, SHRAP, FAT, NAVAL }

    public static boolean safeMode = false;

    private static final VoxelShape SHAPE_AP = Block.box(5, 0, 5, 11, 1, 11);
    private static final VoxelShape SHAPE_HE = Block.box(4, 0, 4, 12, 2, 12);
    private static final VoxelShape SHAPE_FAT = Block.box(5, 0, 4, 11, 6, 12);

    public final Type type;
    public final double range;
    public final double height;

    public Landmine(Properties properties, Type type, double range, double height) {
        super(properties);
        this.type = type;
        this.range = range;
        this.height = height;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLandmine(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // 1.7 setBlockBoundsBasedOnState: the naval mine never sets bounds, so it keeps the full cube.
        return switch (type) {
            case AP, SHRAP -> SHAPE_AP;
            case HE -> SHAPE_HE;
            case FAT -> SHAPE_FAT;
            case NAVAL -> Shapes.block();
        };
    }

    private static boolean hasSupport(LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState state = level.getBlockState(below);
        return state.isFaceSturdy(level, below, Direction.UP) || state.getBlock() instanceof FenceBlock;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return hasSupport(level, pos);
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighbourBlock,
                                     boolean movedByPiston) {
        if (level.isClientSide()) return;
        if (level.hasNeighborSignal(pos)) {
            explode(level, pos, null);
        }
        if (level.getBlockState(pos).is(this) && !hasSupport(level, pos)) {
            if (!safeMode) {
                explode(level, pos, null);
            } else {
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !safeMode) {
            explode(level, pos, player);
        }
    }

    /** 1.7 onBlockActivated with the defuser in hand: lift the mine as an item without setting it off. */
    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.DEFUSER) return false;
        if (!level.isClientSide()) {
            safeMode = true;
            level.removeBlock(pos, false);
            safeMode = false;

            var rand = level.getRandom();
            float f = rand.nextFloat() * 0.6F + 0.2F;
            float f1 = rand.nextFloat() * 0.2F;
            float f2 = rand.nextFloat() * 0.6F + 0.2F;
            ItemEntity drop = new ItemEntity(level, pos.getX() + f, pos.getY() + f1 + 1, pos.getZ() + f2,
                    new ItemStack(this));
            float f3 = 0.05F;
            drop.setDeltaMovement(rand.nextGaussian() * f3, rand.nextGaussian() * f3 + 0.2F, rand.nextGaussian() * f3);
            level.addFreshEntity(drop);
        }
        return true;
    }

    public boolean isWaterAbove(Level level, BlockPos pos) {
        for (int xo = -1; xo <= 1; xo++) {
            for (int zo = -1; zo <= 1; zo++) {
                var fluid = level.getFluidState(pos.offset(xo, 1, zo));
                if (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER)) return true;
            }
        }
        return false;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.DETONATED;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        safeMode = true;
        level.destroyBlock(pos, false);
        safeMode = false;

        switch (type) {
            case AP -> {
                ExplosionVNT vnt = new ExplosionVNT(level, x + 0.5, y + 0.5, z + 0.5, 3F);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, MineConfig.MINE_AP_DAMAGE).setupPiercing(5F, 0.2F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(5, 1F, 0.5F));
                vnt.explode();
            }
            case HE -> {
                ExplosionVNT vnt = new ExplosionVNT(level, x + 0.5, y + 0.5, z + 0.5, 4F);
                vnt.setBlockAllocator(new BlockAllocatorStandard());
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, MineConfig.MINE_HE_DAMAGE).setupPiercing(15F, 0.2F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
                vnt.explode();
            }
            case SHRAP -> {
                ExplosionVNT vnt = new ExplosionVNT(level, x + 0.5, y + 0.5, z + 0.5, 3F);
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, MineConfig.MINE_SHRAP_DAMAGE));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(5, 1F, 0.5F));
                vnt.explode();

                ExplosionLarge.spawnShrapnelShower(level, x + 0.5, y + 0.5, z + 0.5, 0, 1D, 0, 45, 0.2D);
                ExplosionLarge.spawnShrapnels(level, x + 0.5, y + 0.5, z + 0.5, 5);
            }
            case NAVAL -> {
                // 1.7 centres this blast at x + 5 / y + 5 / z + 5 (not + 0.5); load-bearing oddity kept.
                ExplosionVNT vnt = new ExplosionVNT(level, x + 5, y + 5, z + 5, 25F);
                vnt.setBlockAllocator(new BlockAllocatorWater(32));
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, MineConfig.MINE_NAVAL_DAMAGE).setupPiercing(5F, 0.2F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectWeapon(10, 1F, 0.5F));
                vnt.explode();

                ExplosionLarge.spawnParticlesRadial(level, x + 0.5, y + 2, z + 0.5, 30);
                ExplosionLarge.spawnRubble(level, x + 0.5, y + 0.5, z + 0.5, 5);
                if (isWaterAbove(level, pos)) {
                    ExplosionLarge.spawnFoam(level, x + 0.5, y + 0.5, z + 0.5, 60);
                }
            }
            case FAT -> {
                ExplosionVNT vnt = new ExplosionVNT(level, x + 0.5, y + 0.5, z + 0.5, 10);
                vnt.setBlockAllocator(new BlockAllocatorStandard(64));
                vnt.setBlockProcessor(new BlockProcessorStandard());
                vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, MineConfig.MINE_NUKE_DAMAGE).withRangeMod(1.5F));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.explode();

                XFactoryCatapult.irradiate(level, x, y, z, 1.5F);
                EntityEffects.muke(level, x + 0.5, y + 0.5, z + 0.5, false,
                        MainRegistry.polaroidID == 11 || level.getRandom().nextInt(100) == 0, 250);
                SatelliteDetector.reportEvent(level, SatelliteDetector.DURATION_LOW,
                        SatelliteDetector.BurstIntensity.LOW, x + 0.5, z + 0.5);
                level.playSound(null, x + 0.5, y + 0.5, z + 0.5, ModSounds.MUKE_EXPLOSION.get(), SoundSource.BLOCKS,
                        25.0F, 0.9F); // upstream: "this has to be the single worst solution ever"
            }
        }
        return BombReturnCode.DETONATED;
    }
}
