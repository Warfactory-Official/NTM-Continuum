package com.hbm.blocks.generic;

import com.hbm.items.ModArmorSetBItems;
import com.hbm.particle.helper.HbmEffectNT;
import dev.golem.core.world.CompatBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/** Shared invisible-cell behaviour for the old {@code BlockGasBase} family. */
public abstract class BlockGasBase extends CompatBlock {

    /** 1.7 {@code BlockGasFlammable}'s {@code super(0.8F, 0.8F, 0.2F)}; the port's flammable gas is no BlockGasBase. */
    public static final float[] FLAMMABLE_COLOR = {0.8F, 0.8F, 0.2F};

    /**
     * The local player on a client (1.7 {@code MainRegistry.proxy.me()}), installed by the client init; null on a
     * server, where no display tick runs anyway.
     */
    public static Supplier<@Nullable Player> localPlayer = () -> null;

    /**
     * hbm-space {@code BlockGasBase.randomDisplayTick}: the ash glasses show a gas cloud only when
     * {@code rand.nextInt(9) == 1}. 1 (plain HBM, 1.7) shows one on every display tick and draws no random number;
     * N above 1 shows one when {@code rand.nextInt(N) == 1}. NTM: Space sets 9 (ADDON-API-INVENTORY V-187).
     */
    public static int ashGlassesParticleChance = 1;

    private float red = 1.0F, green = 1.0F, blue = 1.0F;

    protected BlockGasBase(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code canCollideCheck} false: the crosshair passes through a gas, so it has no outline. */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    /** 1.7 {@code BlockGasBase(r, g, b)}: the cloud colour the ash glasses reveal. Call from the constructor. */
    protected final void cloudColor(float r, float g, float b) {
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    /** The 1.7 ash-glasses cloud colour of a gas block, or null for a block that shows none. */
    public static float @Nullable [] cloudColor(Block block) {
        if (block instanceof BlockGasBase gas) return new float[] {gas.red, gas.green, gas.blue};
        if (block instanceof BlockGasFlammable) return FLAMMABLE_COLOR.clone();
        return null;
    }

    /**
     * 1.7 {@code BlockGasBase.randomDisplayTick}, the part every gas shares: subclasses that add their own particles call
     * {@code super.animateTick} first, as 1.7 called {@code super.randomDisplayTick}.
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        ashGlassesCloud(level, pos, random, red, green, blue);
    }

    /**
     * Whether a viewer sees a gas cloud this display tick: 1.7 {@code ArmorUtil.checkArmorPiece(p, ashglasses, 3)}, then
     * (hbm-space only) the {@link #ashGlassesParticleChance} roll, which is drawn only for a player wearing the glasses.
     */
    public static boolean revealsGas(@Nullable Player viewer, RandomSource rand) {
        if (viewer == null) return false;
        ItemStack head = viewer.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty() || !head.is(ModArmorSetBItems.ashglasses.get())) return false;
        return ashGlassesParticleChance <= 1 || rand.nextInt(ashGlassesParticleChance) == 1;
    }

    /**
     * 1.7: a local player in ash glasses sees a {@code vanillaExt} {@code cloud} of the gas colour in the cell's centre
     * ({@code effectNT}, client only; the client builds {@code ParticleGasCloud}).
     */
    public static void ashGlassesCloud(Level level, BlockPos pos, RandomSource random, float r, float g, float b) {
        if (!revealsGas(localPlayer.get(), random)) return;
        CompoundTag data = new CompoundTag();
        data.putString("type", "vanillaExt");
        data.putString("mode", "cloud");
        data.putFloat("r", r);
        data.putFloat("g", g);
        data.putFloat("b", b);
        HbmEffectNT.VanillaExt_Cloud.summonParticle(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, data);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    protected static Direction randomHorizontal(RandomSource random) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(random);
    }

    /** Old gas drift moved only into literal air, not into another gas cell. */
    protected static boolean tryMove(ServerLevel level, BlockState state, BlockPos pos, Direction direction) {
        BlockPos target = pos.relative(direction);
        if (!level.isLoaded(target) || !level.getBlockState(target).is(Blocks.AIR)) return false;
        level.removeBlock(pos, false);
        level.setBlock(target, state, 2);
        return true;
    }

    /** Vanilla does not expose a block-level isAir override, so the outgas meaning is kept locally. */
    protected static boolean isAirLike(BlockState state) {
        return state.isAir() || state.getBlock() instanceof BlockGasBase;
    }

    protected static void drift(ServerLevel level, BlockState state, BlockPos pos, RandomSource random,
                                Direction first, Direction second) {
        if (random.nextInt(2) == 0 && !tryMove(level, state, pos, first)) {
            tryMove(level, state, pos, second);
        }
    }
}
