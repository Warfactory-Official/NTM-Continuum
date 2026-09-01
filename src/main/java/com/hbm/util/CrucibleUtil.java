package com.hbm.util;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial.SmeltingBehavior;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The molten-metal pouring engine (1.12.2 {@code CrucibleUtil}): a hitscan straight down from the pour
 * point finds an {@link ICrucibleAcceptor} block and pours into it; with no acceptor the material spills
 * (lost unless {@code safe}). The {@code impactHolder} (single-cell array) receives the stream's impact
 * position, used to size the rendered pour-stream.
 */
public final class CrucibleUtil {

    private CrucibleUtil() {}

    /**
     * Pours from a single stack, up to {@code quanta} per call. Mutates {@code stack}; returns the
     * (possibly reduced) original stack, matching the old contract.
     */
    public static Mats.MaterialStack pourSingleStack(Level world, double x, double y, double z, double range,
                                                     boolean safe, Mats.MaterialStack stack, int quanta, Vec3 @Nullable [] impactHolder) {
        Vec3 start = new Vec3(x, y, z);
        Vec3 end = new Vec3(x, y - range, z);

        BlockHitResult[] mopHolder = new BlockHitResult[1];
        ICrucibleAcceptor acc = getPouringTarget(world, start, end, mopHolder);
        BlockHitResult mop = mopHolder[0];

        if (acc == null) {
            spill(mop, safe, stack, quanta, impactHolder);
            return stack;
        }

        Mats.MaterialStack ret = tryPourStack(world, acc, mop, stack, impactHolder);
        if (ret != null) return ret;

        spill(mop, safe, stack, quanta, impactHolder);
        return stack;
    }

    /**
     * Pours from a stack list (first pourable stack wins), up to {@code quanta} per call. Mutates the
     * list's stacks in place; returns the {@link Mats.MaterialStack} that was removed (or null).
     */
    public static Mats.@Nullable MaterialStack pourFullStack(Level world, double x, double y, double z, double range,
                                                             boolean safe, List<Mats.MaterialStack> stacks, int quanta, Vec3 @Nullable [] impactHolder) {
        if (stacks.isEmpty()) return null;

        Vec3 start = new Vec3(x, y, z);
        Vec3 end = new Vec3(x, y - range, z);

        BlockHitResult[] mopHolder = new BlockHitResult[1];
        ICrucibleAcceptor acc = getPouringTarget(world, start, end, mopHolder);
        BlockHitResult mop = mopHolder[0];

        if (acc == null) {
            return spill(mop, safe, stacks, quanta, impactHolder);
        }

        for (Mats.MaterialStack stack : stacks) {
            if (stack.material == null) continue;

            int amountToPour = Math.min(stack.amount, quanta);
            Mats.MaterialStack toPour = new Mats.MaterialStack(stack.material, amountToPour);
            Mats.MaterialStack left = tryPourStack(world, acc, mop, toPour, impactHolder);

            if (left != null) {
                stack.amount -= (amountToPour - left.amount);
                return new Mats.MaterialStack(stack.material, stack.amount - left.amount);
            }
        }

        return spill(mop, safe, stacks, quanta, impactHolder);
    }

    /**
     * Tries to pour the stack onto the acceptor. Only {@code SMELTABLE} materials pour. Returns the
     * leftover (never null on success — 0-amount stack when fully accepted) or null when rejected.
     */
    public static Mats.@Nullable MaterialStack tryPourStack(Level world, ICrucibleAcceptor acc, BlockHitResult mop,
                                                            Mats.MaterialStack stack, Vec3 @Nullable [] impactHolder) {
        Vec3 hit = mop.getLocation();

        if (stack.material.smeltable != SmeltingBehavior.SMELTABLE) {
            return null;
        }

        com.hbm.lib.ForgeDirection side = com.hbm.lib.ForgeDirection.getOrientation(mop.getDirection().get3DDataValue());
        if (acc.canAcceptPartialPour(world, mop.getBlockPos(), hit.x, hit.y, hit.z, side, stack)) {
            Mats.MaterialStack left = acc.pour(world, mop.getBlockPos(), hit.x, hit.y, hit.z, side, stack);
            if (left == null) {
                left = new Mats.MaterialStack(stack.material, 0);
            }
            if (impactHolder != null) impactHolder[0] = hit;
            return left;
        }

        return null;
    }

    /** Raytraces from {@code start} to {@code end}; returns the hit {@link ICrucibleAcceptor} block (or null). */
    public static @Nullable ICrucibleAcceptor getPouringTarget(Level world, Vec3 start, Vec3 end, BlockHitResult @Nullable [] mopHolder) {
        BlockHitResult mop = dev.golem.core.world.Compat.clip(world, start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY);

        if (mopHolder != null) {
            mopHolder[0] = mop;
        }

        if (mop == null || mop.getType() != HitResult.Type.BLOCK) {
            return null;
        }

        Block b = world.getBlockState(mop.getBlockPos()).getBlock();
        return b instanceof ICrucibleAcceptor acc ? acc : null;
    }

    /** Spill from a stack list — uses the first stack, then prunes emptied stacks. */
    public static Mats.@Nullable MaterialStack spill(@Nullable BlockHitResult mop, boolean safe, List<Mats.MaterialStack> stacks,
                                                     int quanta, Vec3 @Nullable [] impactHolder) {
        Mats.MaterialStack top = stacks.get(0);
        Mats.MaterialStack ret = spill(mop, safe, top, quanta, impactHolder);
        stacks.removeIf(o -> o.amount <= 0);
        return ret;
    }

    /**
     * No valid acceptor: NOP in safe mode, otherwise up to {@code quanta} of the stack is destroyed
     * (returned as the lost amount, so callers can still render the stream).
     */
    public static Mats.@Nullable MaterialStack spill(@Nullable BlockHitResult mop, boolean safe, Mats.MaterialStack stack,
                                                     int quanta, Vec3 @Nullable [] impactHolder) {
        if (safe) return null;

        Mats.MaterialStack toWaste = new Mats.MaterialStack(stack.material, Math.min(stack.amount, quanta));
        stack.amount -= toWaste.amount;

        if (impactHolder != null && mop != null) {
            impactHolder[0] = mop.getLocation();
        }

        return toWaste;
    }
}
