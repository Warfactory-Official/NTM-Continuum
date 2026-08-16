package com.hbm.blocks.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.material.Mats;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityCrucible;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Crucible (1.12.2 {@code machine_crucible}): a 3×2×3 dummyable basin that melts items into molten
 * material. Right-click opens the GUI; right-click with a <b>shovel</b> empties the molten content into
 * scraps instead. The block face is an {@link ICrucibleAcceptor} so channels/outlets can pour into the
 * basin from above (delegating to the core BlockEntity).
 */
public class MachineCrucible extends BlockDummyable implements ICrucibleAcceptor {

    public MachineCrucible(Properties properties) {
        super(properties);

        // The 1.12.2 basin hitbox: a 3×3 half-height base slab + the four rim walls (core-relative).
        this.bounding.add(new net.minecraft.world.phys.AABB(-1.5D, 0D, -1.5D, 1.5D, 0.5D, 1.5D));
        this.bounding.add(new net.minecraft.world.phys.AABB(-1.25D, 0.5D, -1.25D, 1.25D, 1.5D, -1D));
        this.bounding.add(new net.minecraft.world.phys.AABB(-1.25D, 0.5D, -1.25D, -1D, 1.5D, 1.25D));
        this.bounding.add(new net.minecraft.world.phys.AABB(-1.25D, 0.5D, 1D, 1.25D, 1.5D, 1.25D));
        this.bounding.add(new net.minecraft.world.phys.AABB(1D, 0.5D, -1.25D, 1.25D, 1.5D, 1.25D));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCrucible(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** Shovel right-click empties the molten content to scraps (the old {@code onBlockActivated}). */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!stack.isEmpty() && stack.is(ItemTags.SHOVELS)) {
            if (!level.isClientSide() && !player.isShiftKeyDown()) {
                if (findCoreTE(level, pos) instanceof TileEntityCrucible crucible) {
                    List<Mats.MaterialStack> stacks = new ArrayList<>();
                    stacks.addAll(crucible.recipeStack);
                    stacks.addAll(crucible.wasteStack);

                    for (Mats.MaterialStack matStack : stacks) {
                        ItemStack scrap = ItemScraps.create(new Mats.MaterialStack(matStack.material, matStack.amount));
                        if (!player.getInventory().add(scrap)) {
                            level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, scrap));
                        }
                    }

                    crucible.recipeStack.clear();
                    crucible.wasteStack.clear();
                    crucible.setChanged();
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUseItem(state, level, pos, player, hand, stack, hit);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_CRUCIBLE.get(), TileEntityCrucible::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    // --- ICrucibleAcceptor: delegate to the core (pour from the top only) ---

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, Mats.MaterialStack stack) {
        return findCoreTE(world, pos) instanceof TileEntityCrucible crucible
                && crucible.canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public Mats.MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, Mats.MaterialStack stack) {
        if (!(findCoreTE(world, pos) instanceof TileEntityCrucible crucible)) return stack;
        return crucible.pour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, ForgeDirection side, Mats.MaterialStack stack) { return false; }

    @Override
    public Mats.MaterialStack flow(Level world, BlockPos pos, ForgeDirection side, Mats.MaterialStack stack) { return null; }
}
