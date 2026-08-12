package com.hbm.tileentity.machine;

import com.hbm.blocks.generic.BlockDynamicSlag;
import com.hbm.blocks.generic.BlockDynamicSlag.TileEntitySlag;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * Foundry slagtap (1.12.2 {@code TileEntityFoundrySlagtap}) — an outlet variant that raytraces 15
 * blocks down and dumps the material into the world as {@link BlockDynamicSlag} piles instead of
 * pouring into an acceptor. The waste-disposal drain.
 */
public class TileEntityFoundrySlagtap extends TileEntityFoundryOutlet {

    public TileEntityFoundrySlagtap(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDRY_SLAGTAP.get(), pos, state);
    }

    @Override
    protected double getDropRange() {
        return 15;
    }

    private @Nullable BlockHitResult trace(Level world, BlockPos p) {
        Vec3 start = new Vec3(p.getX() + 0.5, p.getY() - 0.125, p.getZ() + 0.5);
        Vec3 end = new Vec3(p.getX() + 0.5, p.getY() + 0.125 - getDropRange(), p.getZ() + 0.5);
        BlockHitResult mop = world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return mop.getType() == HitResult.Type.BLOCK ? mop : null;
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        if (filter != null && (filter != stack.material ^ invertFilter)) return false;
        if (isClosed()) return false;
        if (side != getFacing().getOpposite()) return false;

        return trace(world, p) != null;
    }

    @Override
    public @Nullable MaterialStack flow(Level world, BlockPos p, ForgeDirection side, MaterialStack stack) {
        if (stack == null || stack.material == null || stack.amount <= 0) return null;

        BlockHitResult mop = trace(world, p);
        if (mop == null) return null;

        BlockPos hitPos = mop.getBlockPos();
        Block slag = ModMachineBlocks.slag.get();
        Block hit = world.getBlockState(hitPos).getBlock();

        boolean didFlow = false;

        if (hit == slag) {
            if (world.getBlockEntity(hitPos) instanceof TileEntitySlag tile && tile.mat == stack.material) {
                int transfer = Math.min(TileEntitySlag.maxAmount - tile.amount, stack.amount);
                tile.amount += transfer;
                stack.amount -= transfer;
                didFlow = transfer > 0;
                tile.setChanged();
                world.scheduleTick(hitPos, slag, 1);
            }
        } else if (world.getBlockState(hitPos).canBeReplaced()) {
            world.setBlockAndUpdate(hitPos, slag.defaultBlockState());
            if (world.getBlockEntity(hitPos) instanceof TileEntitySlag tile) {
                tile.mat = stack.material;
                int transfer = Math.min(TileEntitySlag.maxAmount, stack.amount);
                tile.amount += transfer;
                stack.amount -= transfer;
                didFlow = transfer > 0;
                tile.setChanged();
            }
            world.scheduleTick(hitPos, slag, 1);
        }

        BlockPos above = hitPos.above();
        if (stack.amount > 0 && world.getBlockState(above).canBeReplaced()) {
            world.setBlockAndUpdate(above, slag.defaultBlockState());
            if (world.getBlockEntity(above) instanceof TileEntitySlag tile) {
                tile.mat = stack.material;
                int transfer = Math.min(TileEntitySlag.maxAmount, stack.amount);
                tile.amount += transfer;
                stack.amount -= transfer;
                didFlow = didFlow || transfer > 0;
                tile.setChanged();
            }
            world.scheduleTick(above, slag, 1);
        }

        if (didFlow) {
            this.pourColor = stack.material.moltenColor;
            this.pourLen = Math.max(1F, p.getY() - (float) Math.ceil(hitPos.getY()));
            this.lastPourTime = world.getGameTime();
        }

        if (stack.amount <= 0) {
            stack = null;
        }

        return stack;
    }
}
