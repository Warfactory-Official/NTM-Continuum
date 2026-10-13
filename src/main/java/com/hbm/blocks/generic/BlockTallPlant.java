package com.hbm.blocks.generic;

import com.hbm.items.ModItems;
import dev.golem.core.world.BlockTickScheduler;
import dev.golem.core.world.CompatBonemealableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.items.ModTerrainItems;

/**
 * 1.7 {@code BlockTallPlant}: the two-block hemp / mustard-willow stages. The old meta packed the
 * {@code EnumTallFlower} in the low bits and {@code +8} for the upper half; here the four types are
 * flattened to {@code plant_tall_<type>} carrying vanilla's {@code half} property.
 *
 * <p>Ported oddities, all upstream's: both halves drop a <b>flower</b> item (hemp -> hemp flower,
 * every willow stage -> {@code CD0}); harvesting the lower half also drops the upper's loot unless in
 * creative; a lower half whose top is cut off reverts to the flower block ({@code detectCut}); the
 * mature willow's upper half adds 3-6 mustard willow leaves; hemp on dead/oily dirt withers into a
 * big dead flower on its random tick; and {@code CD3 -> CD4} turns the ground under it into plain dirt.
 */
public class BlockTallPlant extends BlockTerrainPlant implements CompatBonemealableBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public enum Type {
        WEED(false), CD2(true), CD3(true), CD4(true);

        public final boolean needsOil;
        Type(boolean needsOil) { this.needsOil = needsOil; }

        public String id() { return "plant_tall_" + name().toLowerCase(Locale.ROOT); }
        public String texture(String half) { return "hbm:blocks/plant_tall." + name().toLowerCase(Locale.ROOT) + "." + half; }
        /** 1.7 {@code getRenderColor}: metas 0 and 8 (hemp, both halves) take the biome grass colour. */
        public boolean tinted() { return this == WEED; }
    }

    public final Type type;

    public BlockTallPlant(Properties properties, Type type) {
        super(properties);
        this.type = type;
        registerDefaultState(getStateDefinition().any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    public static boolean isLower(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    /** Places both halves of a tall plant of {@code type} with {@code pos} as the lower block (flags 3). */
    public static void place(Level level, BlockPos pos, Type type) {
        BlockTallPlant block = ModTerrainBlocks.plant_tall(type);
        level.setBlock(pos, block.defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER), 3);
        level.setBlock(pos.above(), block.defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    // Support

    @Override
    protected boolean canStay(BlockState state, LevelReader level, BlockPos pos) {
        if (!isLower(state)) {
            BlockState below = level.getBlockState(pos.below());
            return below.getBlock() instanceof BlockTallPlant && isLower(below);
        }
        return isFlowerGround(level.getBlockState(pos.below()));
    }

    /** 1.7 {@code checkAndDropBlock}: an upper half (meta > 7) that loses its base leaves nothing behind. */
    @Override
    protected boolean dropsWhenUnsupported(BlockState state) {
        return isLower(state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        // 1.7 canPlaceBlockAt: ground below and air above
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).isAir()
                && isFlowerGround(level.getBlockState(pos.below()))) {
            return defaultBlockState();
        }
        return null;
    }

    /** 1.7 {@code onBlockPlacedBy}: the item places the lower half; the upper half is set here (flags 2). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), defaultBlockState().setValue(HALF, DoubleBlockHalf.UPPER), 2);
    }

    @Override
    protected BlockState onUpdateShape(BlockState state, LevelReader level, BlockTickScheduler ticks, BlockPos pos,
                                       Direction direction, BlockPos neighbourPos, BlockState neighbourState) {
        BlockState result = super.onUpdateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState);
        if (result != state) return result;

        // 1.7 detectCut: a lower half whose upper half is gone reverts to the flower it grew from.
        if (isLower(state) && direction == Direction.UP
                && !(neighbourState.getBlock() instanceof BlockTallPlant && !isLower(neighbourState))
                && isFlowerGround(level.getBlockState(pos.below()))) {
            return ModTerrainBlocks.plant_flower(type == Type.WEED
                    ? BlockNTMFlower.Type.WEED : BlockNTMFlower.Type.CD0).defaultBlockState();
        }
        return state;
    }

    // Drops

    /** 1.7 {@code getItemDropped/damageDropped/getDrops}: one flower item per half, plus willow leaves. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(2);
        drops.add(new ItemStack(ModTerrainBlocks.plant_flower(type == Type.WEED
                ? BlockNTMFlower.Type.WEED : BlockNTMFlower.Type.CD0)));
        if (type == Type.CD4 && !isLower(state)) {
            drops.add(new ItemStack(ModTerrainItems.plant_item_mustardwillow.get(),
                    3 + params.getLevel().getRandom().nextInt(4)));
        }
        return drops;
    }

    /** 1.7 {@code onBlockHarvested}: breaking the base also yields the top's loot (unless creative). */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!isLower(state)) return;
        BlockPos above = pos.above();
        BlockState top = level.getBlockState(above);
        if (!(top.getBlock() instanceof BlockTallPlant) || isLower(top)) return;
        if (playerPreventsBlockDrops(player)) {
            level.setBlock(above, Blocks.AIR.defaultBlockState(), 2);
        } else if (level instanceof ServerLevel server) {
            dropResources(top, server, above);
        }
    }

    // Growth

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!isLower(state)) return; // 1.7: meta > 7 returns

        BlockState onTop = level.getBlockState(pos.below());
        if (!type.needsOil && isDeadDirt(onTop)) {
            level.setBlock(pos, ModTerrainBlocks.plant_dead(BlockDeadPlant.Type.BIGFLOWER).defaultBlockState(), 3);
            return;
        }
        if (canBonemeal(level, pos, state) && isBonemealSuccess(level, random, pos, state) && random.nextInt(3) == 0) {
            performBonemeal(level, random, pos, state);
        }
    }

    /** The lower half's position, whichever half was asked. */
    private static BlockPos base(BlockPos pos, BlockState state) {
        return isLower(state) ? pos : pos.below();
    }

    @Override
    public boolean canBonemeal(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos y0 = base(pos, state);
        if (type == Type.CD2 || type == Type.CD3) {
            if (!BlockNTMFlower.hasWaterBeside(level, y0.below())) return false;
        }
        if (type == Type.CD3) {
            return isDeadDirt(level.getBlockState(y0.below()));
        }
        return type != Type.CD4 && type != Type.WEED;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        if (type == Type.CD3) return true;
        return random.nextFloat() < 0.33F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (type != Type.CD2 && type != Type.CD3) return;
        BlockPos y0 = base(pos, state);
        Type next = type == Type.CD2 ? Type.CD3 : Type.CD4;
        // 1.7 sets the upper half first, then the lower (both metas +1); both halves are rewritten here.
        place(level, y0, next);
        if (type == Type.CD3) {
            level.setBlock(y0.below(), Blocks.DIRT.defaultBlockState(), 3);
        }
    }
}
