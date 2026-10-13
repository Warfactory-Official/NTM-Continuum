package com.hbm.blocks.fluid;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.generic.BlockSellafieldSlaked;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 {@code RadBlock} ({@code rad_lava_block}): the sellafield counterpart of the volcanic lava, flowing the
 * same way (its own fluid, {@code hbm:rad_lava_fluid}).
 * Contact contaminates (5 rads); it slakes into {@code sellafield_slaked} (shade 5–7), rarely a
 * sellafield diamond/emerald ore, or the radgem ore where it is fully surrounded; diamond ore becomes
 * radgem ore and uranium ore is scorched (or, one time in five, schrabidium ore).
 */
public class BlockRadLava extends BlockVolcanicLava {

    public BlockRadLava(Properties properties) {
        super(properties, ModWorldFluids.RAD_LAVA);
    }

    @Override
    protected void onEntityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.onEntityInside(state, level, pos, entity); // still Material.lava in 1.7: burns as well
        if (!level.isClientSide() && entity instanceof LivingEntity living) {
            ContaminationUtil.contaminate(living, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
        }
    }

    private static BlockState shaded(Block block, int shade) {
        BlockState state = block.defaultBlockState();
        return state.hasProperty(BlockSellafieldSlaked.SHADE)
                ? state.setValue(BlockSellafieldSlaked.SHADE, Math.min(shade, 9)) : state;
    }

    @Override
    protected void onSolidify(ServerLevel level, BlockPos pos, int lavaCount, int basaltCount, RandomSource rand) {
        int r = rand.nextInt(400);
        BlockState above = level.getBlockState(pos.above(10));
        boolean canMakeGem = lavaCount + basaltCount == 6 && lavaCount < 3
                && (above.is(ModFalloutBlocks.sellafield_slaked.get()) || above.is(this));
        int meta = 5 + rand.nextInt(3);

        if (r < 2) level.setBlock(pos, shaded(ModFalloutBlocks.ore_sellafield_diamond.get(), meta), 3);
        else if (r == 2) level.setBlock(pos, shaded(ModFalloutBlocks.ore_sellafield_emerald.get(), meta), 3);
        else if (r < 20 && canMakeGem) level.setBlock(pos, shaded(ModFalloutBlocks.ore_sellafield_radgem.get(), meta), 3);
        else level.setBlock(pos, shaded(ModFalloutBlocks.sellafield_slaked.get(), meta), 3);
    }

    @Override
    protected Block getBasaltForCheck() {
        return ModFalloutBlocks.sellafield_slaked.get();
    }

    @Override
    protected @Nullable BlockState getReaction(Level level, BlockPos pos) {
        BlockState b = level.getBlockState(pos);
        if (b.liquid() && b.getFluidState().is(FluidTags.WATER)) return Blocks.STONE.defaultBlockState(); // 1.7 Material.water
        if (b.is(BlockTags.LOGS)) return ModFalloutBlocks.waste_log.get().defaultBlockState();
        if (b.is(BlockTags.PLANKS)) return ModBlocks.waste_planks.get().defaultBlockState();
        if (b.is(BlockTags.LEAVES)) return Blocks.FIRE.defaultBlockState();
        if (b.is(Blocks.DIAMOND_ORE)) return ModFalloutBlocks.ore_sellafield_radgem.get().defaultBlockState();
        if (b.is(ModNuclearBlocks.ore_uranium.get()) || b.is(ModNuclearBlocks.ore_gneiss_uranium.get())) {
            return level.getRandom().nextInt(5) == 0
                    ? ModFalloutBlocks.ore_sellafield_schrabidium.get().defaultBlockState()
                    : ModFalloutBlocks.ore_sellafield_uranium_scorched.get().defaultBlockState();
        }
        return null;
    }
}
