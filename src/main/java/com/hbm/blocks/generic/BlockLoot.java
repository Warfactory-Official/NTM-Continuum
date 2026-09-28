package com.hbm.blocks.generic;

import com.hbm.tileentity.ModDungeonBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code BlockLoot}, the "Loot Pile": a one-pixel sheet on the floor carrying a few stacks laid out where the
 * structure that placed it put them, drawn by {@code RenderLoot} (mini nukes, the lever-action shotgun and power
 * armor pieces as their models, everything else as a flat icon). Right-clicking without sneaking picks the whole
 * pile up - the block goes and every stack drops at its foot; breaking it does the same. The block itself drops
 * nothing.
 */
public class BlockLoot extends CompatBlock implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public BlockLoot(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLoot(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        level.removeBlock(pos, false);
        return InteractionResult.SUCCESS;
    }

    /** One stack of the pile and where on the block it lies (1.7's {@code Quartet<ItemStack, x, y, z>}). */
    public record LootItem(ItemStack stack, double x, double y, double z) {}

    public static class TileEntityLoot extends TileEntityLoadedBase {

        public final List<LootItem> items = new ArrayList<>();

        public TileEntityLoot(BlockPos pos, BlockState state) {
            super(ModDungeonBlockEntities.LOOT.get(), pos, state);
        }

        public TileEntityLoot addItem(ItemStack stack, double x, double y, double z) {
            items.add(new LootItem(stack, x, y, z));
            setChanged();
            return this;
        }

        /** Pushes the pile to watching clients, 1.7's description packet after a structure filled it. */
        public void sync() {
            if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            super.preRemoveSideEffects(pos, state);
            if (level == null || level.isClientSide()) return;
            for (LootItem item : items) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, item.stack().copy()));
            }
            items.clear();
        }

        @Override
        protected void loadData(DataIn input) {
            super.loadData(input);
            items.clear();
            for (DataIn entry : input.childrenListOrEmpty("items")) {
                ItemStack stack = entry.read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
                if (stack.isEmpty()) continue;
                items.add(new LootItem(stack, entry.getDoubleOr("x", 0), entry.getDoubleOr("y", 0), entry.getDoubleOr("z", 0)));
            }
        }

        @Override
        protected void saveData(DataOut output) {
            super.saveData(output);
            DataOut.ChildList list = output.childrenList("items");
            for (LootItem item : items) {
                if (item.stack().isEmpty()) continue;
                DataOut entry = list.addChild();
                entry.store("item", ItemStack.CODEC, item.stack());
                entry.putDouble("x", item.x());
                entry.putDouble("y", item.y());
                entry.putDouble("z", item.z());
            }
        }
    }
}
