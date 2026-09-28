package com.hbm.handler.ability;

import com.hbm.config.ToolConfig;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.items.tool.ItemToolAbility;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 1.7 {@code IToolAreaAbility}: which blocks come along with the one being mined. */
public interface IToolAreaAbility extends IBaseAbility {

    // Should call tool.breakExtraBlock on a bunch of blocks.
    // The initial block is implicitly broken, so don't call breakExtraBlock on it.
    // Returning true skips the reference block from being broken
    boolean onDig(int level, ServerLevel world, BlockPos pos, ServerPlayer player, ItemToolAbility tool);

    // Whether breakExtraBlock is called at all. Currently only false for explosion
    default boolean allowsHarvest(int level) {
        return true;
    }

    int SORT_ORDER_BASE = 0;

    IToolAreaAbility NONE = new IToolAreaAbility() {
        @Override public String getName() { return ""; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 0; }
        @Override public boolean onDig(int level, ServerLevel world, BlockPos pos, ServerPlayer player, ItemToolAbility tool) {
            return false;
        }
    };

    IToolAreaAbility RECURSION = new IToolAreaAbility() {
        @Override public String getName() { return "tool.ability.recursion"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityVein; }

        public final int[] radiusAtLevel = { 3, 4, 5, 6, 7, 9, 10 };

        @Override public int levels() { return radiusAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + radiusAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 1; }

        // 1.7 note, kept: this set is shared between every tool using the ability. It is cleared per dig
        // and the dig is single-threaded on the server, so the sharing is harmless.
        private final Set<BlockPos> pos = new HashSet<>();

        @Override
        public boolean onDig(int level, ServerLevel world, BlockPos ref, ServerPlayer player, ItemToolAbility tool) {
            Block b = world.getBlockState(ref).getBlock();

            if (b == Blocks.STONE && !ToolConfig.recursiveStone) return false;
            if (b == Blocks.NETHERRACK && !ToolConfig.recursiveNetherrack) return false;

            pos.clear();
            recurse(world, ref, ref, player, tool, 0, radiusAtLevel[level]);
            return false;
        }

        private final List<BlockPos> offsets = buildOffsets();

        private List<BlockPos> buildOffsets() {
            List<BlockPos> list = new ArrayList<>(3 * 3 * 3 - 1);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++)
                        if (dx != 0 || dy != 0 || dz != 0) list.add(new BlockPos(dx, dy, dz));
            return list;
        }

        private void recurse(ServerLevel world, BlockPos at, BlockPos ref, ServerPlayer player, ItemToolAbility tool,
                             int depth, int radius) {
            List<BlockPos> shuffled = new ArrayList<>(offsets);
            Collections.shuffle(shuffled);
            for (BlockPos offset : shuffled) {
                breakExtra(world, at.offset(offset), ref, player, tool, depth, radius);
            }
        }

        private void breakExtra(ServerLevel world, BlockPos at, BlockPos ref, ServerPlayer player, ItemToolAbility tool,
                                int depth, int radius) {
            if (pos.contains(at)) return;

            depth += 1;
            if (depth > ToolConfig.recursionDepth) return;

            pos.add(at);

            // don't lose the ref block just yet
            if (at.equals(ref)) return;

            double dx = at.getX() - ref.getX(), dy = at.getY() - ref.getY(), dz = at.getZ() - ref.getZ();
            if (Math.sqrt(dx * dx + dy * dy + dz * dz) > radius) return;

            BlockState b = world.getBlockState(at);
            BlockState refState = world.getBlockState(ref);

            // 1.7 compared block and metadata; lit and unlit redstone ore counted as one block. The modern
            // redstone ore is one block with a LIT property, so compare the block and every other property.
            if (b.getBlock() != refState.getBlock()) return;
            if (!sameMeta(b, refState)) return;

            if (player.getMainHandItem().isEmpty()) return;

            tool.breakExtraBlock(world, at, player, ref);

            recurse(world, at, ref, player, tool, depth, radius);
        }

        private boolean sameMeta(BlockState a, BlockState b) {
            if (a.getBlock() == Blocks.REDSTONE_ORE || a.getBlock() == Blocks.DEEPSLATE_REDSTONE_ORE) return true;
            return a.equals(b);
        }
    };

    IToolAreaAbility HAMMER = new IToolAreaAbility() {
        @Override public String getName() { return "tool.ability.hammer"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityHammer; }

        public final int[] rangeAtLevel = { 1, 2, 3, 4 };

        @Override public int levels() { return rangeAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + rangeAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 2; }

        @Override
        public boolean onDig(int level, ServerLevel world, BlockPos ref, ServerPlayer player, ItemToolAbility tool) {
            int range = rangeAtLevel[level];
            for (int a = ref.getX() - range; a <= ref.getX() + range; a++)
                for (int b = ref.getY() - range; b <= ref.getY() + range; b++)
                    for (int c = ref.getZ() - range; c <= ref.getZ() + range; c++) {
                        if (a == ref.getX() && b == ref.getY() && c == ref.getZ()) continue;
                        tool.breakExtraBlock(world, new BlockPos(a, b, c), player, ref);
                    }
            return false;
        }
    };

    IToolAreaAbility HAMMER_FLAT = new IToolAreaAbility() {
        @Override public String getName() { return "tool.ability.hammer_flat"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityHammer; }

        public final int[] rangeAtLevel = { 1, 2, 3, 4 };

        @Override public int levels() { return rangeAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + rangeAtLevel[level] + ")"; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 3; }

        @Override
        public boolean onDig(int level, ServerLevel world, BlockPos ref, ServerPlayer player, ItemToolAbility tool) {
            int range = rangeAtLevel[level];

            // 1.7 re-raytraced from the player at their block reach (4.5, 5 in creative) to learn the face.
            HitResult hit = player.pick(player.isCreative() ? 5.0D : 4.5D, 1.0F, false);
            if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return true;

            int xRange = range;
            int yRange = range;
            int zRange = 0;
            switch (blockHit.getDirection()) {
                case DOWN, UP -> { yRange = 0; zRange = range; }
                case NORTH, SOUTH -> { xRange = range; zRange = 0; }
                case WEST, EAST -> { xRange = 0; zRange = range; }
            }

            for (int a = ref.getX() - xRange; a <= ref.getX() + xRange; a++)
                for (int b = ref.getY() - yRange; b <= ref.getY() + yRange; b++)
                    for (int c = ref.getZ() - zRange; c <= ref.getZ() + zRange; c++) {
                        if (a == ref.getX() && b == ref.getY() && c == ref.getZ()) continue;
                        tool.breakExtraBlock(world, new BlockPos(a, b, c), player, ref);
                    }
            return false;
        }
    };

    IToolAreaAbility EXPLOSION = new IToolAreaAbility() {
        @Override public String getName() { return "tool.ability.explosion"; }
        @Override public boolean isAllowed() { return ToolConfig.abilityExplosion; }

        public final float[] strengthAtLevel = { 2.5F, 5F, 10F, 15F };

        @Override public int levels() { return strengthAtLevel.length; }
        @Override public String getExtension(int level) { return " (" + strengthAtLevel[level] + ")"; }
        @Override public boolean allowsHarvest(int level) { return false; }
        @Override public int sortOrder() { return SORT_ORDER_BASE + 4; }

        @Override
        public boolean onDig(int level, ServerLevel world, BlockPos ref, ServerPlayer player, ItemToolAbility tool) {
            float strength = strengthAtLevel[level];
            double x = ref.getX() + 0.5, y = ref.getY() + 0.5, z = ref.getZ() + 0.5;

            // 1.7 ExplosionNT with ALLDROP, NOHURT and NOPARTICLE; ExplosionNT was never ported, and this is
            // the same thing in the VNT kit: standard rays, every destroyed block drops, no entity processor,
            // no effects.
            new ExplosionVNT(world, x, y, z, strength, player)
                    .setBlockAllocator(new BlockAllocatorStandard())
                    .setBlockProcessor(new BlockProcessorStandard().setAllDrop())
                    .explode();

            world.explode(player, x, y, z, 0.1F, Level.ExplosionInteraction.NONE);
            return true;
        }
    };

    IToolAreaAbility[] abilities = { NONE, RECURSION, HAMMER, HAMMER_FLAT, EXPLOSION };

    static IToolAreaAbility getByName(String name) {
        for (IToolAreaAbility ability : abilities) {
            if (ability.getName().equals(name)) return ability;
        }
        return NONE;
    }
}
