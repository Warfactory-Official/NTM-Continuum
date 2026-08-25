package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm.explosion.vanillant.interfaces.IDropChanceMutator;
import com.hbm.explosion.vanillant.interfaces.IFortuneMutator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Breaks every allocated block, dropping it with probability {@code 1 / size} unless a
 * {@link IDropChanceMutator} says otherwise, then hands the cleared positions to the optional
 * {@link IBlockMutator} (which is how the incendiary missiles leave fire behind).
 *
 * <p>Positions that were <em>already</em> air are dropped from the set before the post pass, exactly as
 * upstream: the mutator must only see space the blast itself made.
 *
 * <p>Vanilla's {@code onBlockExploded}/{@code canDropFromExplosion} pair has no portable stand-in
 * across the era range (see {@link ExplosionVNT}); the drop roll plus {@code Block.dropResources} do
 * the same two jobs directly.
 */
public class BlockProcessorStandard implements IBlockProcessor {

    protected @Nullable IDropChanceMutator chance;
    protected @Nullable IFortuneMutator fortune;
    protected @Nullable IBlockMutator convert;

    public BlockProcessorStandard() {}

    public BlockProcessorStandard withChance(IDropChanceMutator chance) {
        this.chance = chance;
        return this;
    }

    public BlockProcessorStandard withFortune(IFortuneMutator fortune) {
        this.fortune = fortune;
        return this;
    }

    public BlockProcessorStandard withBlockEffect(@Nullable IBlockMutator convert) {
        this.convert = convert;
        return this;
    }

    @Override
    public void process(ExplosionVNT explosion, Level world, double x, double y, double z, ObjectOpenHashSet<BlockPos> affectedBlocks) {

        float baseDropChance = 1.0F / explosion.size;
        List<BlockPos> blocksToRemove = new ArrayList<>();

        for (Iterator<BlockPos> iterator = affectedBlocks.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            BlockState state = world.getBlockState(pos);

            if (state.isAir()) {
                blocksToRemove.add(pos);
                continue;
            }

            Block block = state.getBlock();
            float dropChance = chance == null ? baseDropChance
                    : chance.mutateDropChance(explosion, block, pos.getX(), pos.getY(), pos.getZ(), baseDropChance);

            if (dropChance > 0F && world.getRandom().nextFloat() <= dropChance) {
                // dropResources is the modern spelling of dropBlockAsItemWithChance: the roll above is
                // the "chance" half, and the tool argument is where fortune would live.
                Block.dropResources(state, world, pos, world.getBlockEntity(pos), explosion.exploder, ItemStack.EMPTY);
            }

            world.removeBlock(pos, false);

            if (convert != null) convert.mutatePre(explosion, state, pos);
        }

        affectedBlocks.removeAll(blocksToRemove);

        if (convert != null) {
            for (BlockPos pos : affectedBlocks) {
                if (world.getBlockState(pos).isAir()) convert.mutatePost(explosion, pos);
            }
        }
    }

    public BlockProcessorStandard setNoDrop() {
        this.chance = new DropChanceMutatorStandard(0F);
        return this;
    }

    public BlockProcessorStandard setAllDrop() {
        this.chance = new DropChanceMutatorStandard(1F);
        return this;
    }

    /**
     * Fortune is carried by an enchanted tool in the modern loot context rather than by an int, and
     * nothing in the ported missile set asks for it — so the level is recorded and, for now, only read
     * back by {@link #withFortune} consumers.
     */
    public BlockProcessorStandard setFortune(int fortune) {
        this.fortune = (explosion, block, x, y, z) -> fortune;
        return this;
    }
}
