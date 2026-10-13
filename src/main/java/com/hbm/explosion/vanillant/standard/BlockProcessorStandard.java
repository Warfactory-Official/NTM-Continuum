package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.IBlockExplodedVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm.explosion.vanillant.interfaces.IDropChanceMutator;
import com.hbm.explosion.vanillant.interfaces.IFortuneMutator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
    /** 1.7 {@code ExplosionNT} {@code ExAttrib.ERRODE}: block -> what it crumbles into, at 60% per block. */
    protected @Nullable Map<Block, Block> erosion;

    public BlockProcessorStandard() {}

    public BlockProcessorStandard withChance(IDropChanceMutator chance) {
        this.chance = chance;
        return this;
    }

    public BlockProcessorStandard withFortune(IFortuneMutator fortune) {
        this.fortune = fortune;
        return this;
    }

    public BlockProcessorStandard withErosion(Map<Block, Block> erosion) {
        this.erosion = erosion;
        return this;
    }

    public BlockProcessorStandard withBlockEffect(@Nullable IBlockMutator convert) {
        this.convert = convert;
        return this;
    }

    @Override
    public void process(ExplosionVNT explosion, Level world, double x, double y, double z, ObjectOpenHashSet<BlockPos> affectedBlocks) {

        float baseDropChance = 1.0F / explosion.size;

        for (Iterator<BlockPos> iterator = affectedBlocks.iterator(); iterator.hasNext(); ) {
            BlockPos pos = iterator.next();
            BlockState state = world.getBlockState(pos);

            if (state.isAir()) {
                // 1.7 iterator.remove(). Collecting these into a list for a later removeAll made the set test
                // every member against the list, quadratic in the blast: a volcano core left erupting in the
                // GameTest world pinned the server thread there for minutes.
                iterator.remove();
                continue;
            }

            Block block = state.getBlock();
            // 1.7 onBlockExploded: a block that handles its own blast (the fluid tank, the refinery) keeps its cell
            if (block instanceof IBlockExplodedVNT hooked
                    && world instanceof ServerLevel server
                    && hooked.onVNTExploded(server, pos, state, explosion)) {
                iterator.remove();
                continue;
            }
            // 1.7 ExplosionNT.doExplosionB: errosion has a 60% chance to occour; an eroded block drops nothing
            Block erodesInto = null;
            if (erosion != null && world.getRandom().nextFloat() < 0.6F) {
                erodesInto = erosion.get(block);
            }
            float dropChance = chance == null ? baseDropChance
                    : chance.mutateDropChance(explosion, block, pos.getX(), pos.getY(), pos.getZ(), baseDropChance);

            if (erodesInto == null && dropChance > 0F && world.getRandom().nextFloat() <= dropChance) {
                // dropResources is the modern spelling of dropBlockAsItemWithChance: the roll above is
                // the "chance" half, and the tool argument is where fortune would live.
                Block.dropResources(state, world, pos, world.getBlockEntity(pos), explosion.exploder, ItemStack.EMPTY);
            }

            world.removeBlock(pos, false);
            if (erodesInto != null) world.setBlock(pos, erodesInto.defaultBlockState(), Block.UPDATE_ALL);

            if (convert != null) convert.mutatePre(explosion, state, pos);
        }

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
     * nothing in the ported missile set asks for it, so the level is recorded and only read
     * back by {@link #withFortune} consumers.
     */
    public BlockProcessorStandard setFortune(int fortune) {
        this.fortune = (explosion, block, x, y, z) -> fortune;
        return this;
    }
}
