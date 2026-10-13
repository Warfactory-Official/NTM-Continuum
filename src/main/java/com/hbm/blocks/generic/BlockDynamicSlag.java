package com.hbm.blocks.generic;

import dev.golem.core.data.Tags;

import com.hbm.blocks.machine.ModMachineBlocks;
import dev.golem.core.registry.TickingBlock;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Dynamic slag (1.12.2 {@code BlockDynamicSlag}): a variable-height pile of molten waste deposited by
 * the foundry slagtap. On scheduled ticks it flows: down into replaceable blocks (the whole pile
 * moves), merges into same-material slag below, and spreads sideways once ≥ 1/5 full. Rendered by a
 * BER as a tinted box at {@code amount / maxAmount} height; drops its content as scraps.
 */
public class BlockDynamicSlag extends CompatBlock implements TickingBlock {

    public BlockDynamicSlag(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE; // the SlagRenderer draws the variable-height pile
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySlag(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (world.getBlockEntity(pos) instanceof TileEntitySlag tile && tile.amount > 0) {
            double h = Math.max(0.0625D, (double) tile.amount / TileEntitySlag.maxAmount);
            return Shapes.box(0, 0, 0, 1, Math.min(h, 1), 1);
        }
        return Shapes.box(0, 0, 0, 1, 0.0625D, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource rand) {
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        if (!(world.getBlockEntity(pos) instanceof TileEntitySlag self)) {
            world.removeBlock(pos, false);
            return;
        }

        self.networkPackNT();

        BlockPos below = pos.below();

        /* flow down: move the whole pile */
        if (world.getBlockState(below).canBeReplaced() && y > world.getMinY()) {
            world.setBlockAndUpdate(below, this.defaultBlockState());
            if (world.getBlockEntity(below) instanceof TileEntitySlag tile) {
                tile.mat = self.mat;
                tile.amount = self.amount;
                tile.setChanged();
            }
            world.scheduleTick(below, this, 1);
            world.removeBlock(pos, false);
            return;
        } else if (world.getBlockEntity(below) instanceof TileEntitySlag belowSlag) {
            if (belowSlag.mat == self.mat && belowSlag.amount < TileEntitySlag.maxAmount) {
                int transfer = Math.min(TileEntitySlag.maxAmount - belowSlag.amount, self.amount);
                belowSlag.amount += transfer;
                self.amount -= transfer;
                belowSlag.setChanged();

                if (self.amount <= 0) {
                    world.removeBlock(pos, false);
                } else {
                    world.scheduleTick(pos, this, 1);
                }
                world.scheduleTick(below, this, 1);
                return;
            }
        }

        /* flow sideways */
        ForgeDirection[] sides = {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST};
        int count = 0;
        for (ForgeDirection dir : sides) {
            if (world.getBlockState(pos.offset(dir.offsetX, 0, dir.offsetZ)).canBeReplaced()) count++;
        }

        if (self.amount >= TileEntitySlag.maxAmount / 5 && count > 0) {
            int toSpread = Math.max(self.amount / (count * 2), 1);

            for (ForgeDirection dir : sides) {
                BlockPos side = pos.offset(dir.offsetX, 0, dir.offsetZ);
                if (world.getBlockState(side).canBeReplaced()) {
                    world.setBlockAndUpdate(side, this.defaultBlockState());
                    if (world.getBlockEntity(side) instanceof TileEntitySlag tile) {
                        tile.mat = self.mat;
                        tile.amount = toSpread;
                        tile.setChanged();
                    }
                    world.scheduleTick(side, this, 1);
                    self.amount -= toSpread;
                    world.scheduleTick(pos, this, 1);
                }
            }
            self.setChanged();
        }
    }

    /** The variable-height slag pile. */
    public static class TileEntitySlag extends TileEntityLoadedBase {

        public @Nullable NTMMaterial mat;
        public int amount;
        public static final int maxAmount = MaterialShapes.BLOCK.quantity * 16;

        public TileEntitySlag(BlockPos pos, BlockState state) {
            super(ModBlockEntities.SLAG.get(), pos, state);
        }

        /** Drop the content as scraps on destruction. */
        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (level == null || level.isClientSide()) return;
            if (mat != null && amount > 0) {
                ItemScraps.spawnMats(level, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, new MaterialStack(mat, amount));
                amount = 0;
            }
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(mat == null ? -1 : mat.id);
            buf.writeInt(amount);
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            super.deserialize(buf);
            int id = buf.readInt();
            mat = id < 0 ? null : Mats.matById.get(id);
            amount = buf.readInt();
        }

        @Override
        protected void saveData(DataOut output) {
            super.saveData(output);
            Mats.write(output, "mat", mat); // by name, never the numeric id
            output.putInt("amount", amount);
        }

        @Override
        protected void loadData(DataIn input) {
            super.loadData(input);
            mat = Mats.read(input, "mat");
            amount = input.getIntOr("amount", 0);
        }
    }
}
