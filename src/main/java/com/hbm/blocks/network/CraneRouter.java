package com.hbm.blocks.network;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.conveyor.IConveyorItem;
import com.hbm.api.conveyor.IConveyorPackage;
import com.hbm.api.conveyor.IEnterableBlock;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.modules.ModulePatternMatcher;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.items.tool.ItemConveyorWand;
import com.hbm.tileentity.network.TileEntityCraneRouter;

/**
 * 1.7 {@code blocks/network/CraneRouter} ("Conveyor Sorter"): sends whatever enters out of the side whose filter
 * matches. 1.7's six-pass render trick (one tint per side) is a model with six tint indices here.
 */
public class CraneRouter extends CompatBlock implements TickingBlock, IEnterableBlock {

    /** The 1.7 {@code colorMultiplier} per pass: down, up, north, south, west, east. */
    public static final int[] SIDE_COLORS = {0xff0000, 0xff8000, 0xffff00, 0x00ff00, 0x0080ff, 0x8000ff};

    public CraneRouter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCraneRouter(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                            InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (stack.getItem() instanceof ItemConveyorWand) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return onUseWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(level.getBlockEntity(pos) instanceof MenuProvider provider)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean canItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        return true;
    }

    @Override
    public boolean canPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        return true;
    }

    @Override
    public void onItemEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorItem entity) {
        List<List<ItemStack>> sort = sort(level, pos, List.of(entity.getItemStack()));

        for (int i = 0; i < 7; i++) {
            List<ItemStack> list = sort.get(i);
            if (i == 6) {
                for (ItemStack stack : list) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
            } else {
                Direction d = Direction.from3DDataValue(i);
                for (ItemStack stack : list) sendOnRoute(level, pos, stack, d);
            }
        }
    }

    protected void sendOnRoute(Level level, BlockPos pos, ItemStack item, Direction dir) {
        BlockPos target = pos.relative(dir);
        Block block = level.getBlockState(target).getBlock();
        double x = pos.getX() + 0.5 + dir.getStepX() * 0.55;
        double y = pos.getY() + 0.5 + dir.getStepY() * 0.55;
        double z = pos.getZ() + 0.5 + dir.getStepZ() * 0.55;

        if (block instanceof IConveyorBelt belt) {
            EntityMovingItem moving = new EntityMovingItem(level);
            Vec3 snap = belt.getClosestSnappingPosition(level, target, new Vec3(x, y, z));
            moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
            moving.setItemStack(item);
            level.addFreshEntity(moving);
        } else {
            level.addFreshEntity(new ItemEntity(level, x, y, z, item));
        }
    }

    @Override
    public void onPackageEnter(Level level, BlockPos pos, @Nullable Direction dir, IConveyorPackage entity) {
        List<List<ItemStack>> sort = sort(level, pos, entity.getItemStacks());

        for (int i = 0; i < 7; i++) {
            List<ItemStack> list = sort.get(i);
            if (list.isEmpty()) continue;

            if (i == 6) {
                for (ItemStack stack : list) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
            } else {
                Direction d = Direction.from3DDataValue(i);
                BlockPos target = pos.relative(d);
                Block block = level.getBlockState(target).getBlock();
                double x = pos.getX() + 0.5 + d.getStepX() * 0.55;
                double y = pos.getY() + 0.5 + d.getStepY() * 0.55;
                double z = pos.getZ() + 0.5 + d.getStepZ() * 0.55;

                if (block instanceof IConveyorBelt belt) {
                    EntityMovingPackage moving = new EntityMovingPackage(level);
                    Vec3 snap = belt.getClosestSnappingPosition(level, target, new Vec3(x, y, z));
                    moving.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
                    moving.setItemStacks(list);
                    level.addFreshEntity(moving);
                } else {
                    for (ItemStack stack : list) level.addFreshEntity(new ItemEntity(level, x, y, z, stack));
                }
            }
        }
    }

    /**
     * 1.7 {@code sort}: accepts an arbitrary amount of stacks (either solo or from boxes) and returns one list per
     * direction (index 6 = UNKNOWN, the unsortable items), each holding COPIES of the original stacks.
     */
    public static List<List<ItemStack>> sort(Level level, BlockPos pos, List<ItemStack> stacks) {
        List<List<ItemStack>> output = new ArrayList<>(7);
        for (int i = 0; i < 7; i++) output.add(new ArrayList<>());

        if (!(level.getBlockEntity(pos) instanceof TileEntityCraneRouter router)) {
            for (ItemStack stack : stacks) if (!stack.isEmpty()) output.get(6).add(stack.copy());
            return output;
        }

        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            int dir = getOutputDir(router, stack.copy());
            output.get(dir).add(stack.copy());
        }

        return output;
    }

    /** The 1.7 {@code getOutputDir}: a direction ordinal, or 6 for UNKNOWN. */
    public static int getOutputDir(TileEntityCraneRouter router, ItemStack stack) {
        List<Integer> validDirs = new ArrayList<>();

        // check filters for all sides
        for (int side = 0; side < 6; side++) {
            ModulePatternMatcher matcher = router.patterns[side];
            int mode = router.modes[side];

            // if the side is disabled or wildcard, skip
            if (mode == TileEntityCraneRouter.MODE_NONE || mode == TileEntityCraneRouter.MODE_WILDCARD) continue;

            boolean matchesFilter = false;

            for (int slot = 0; slot < 5; slot++) {
                ItemStack filter = router.getItem(side * 5 + slot);
                if (filter.isEmpty()) continue;

                // the filter kicks in so long as one entry matches
                if (matcher.isValidForFilter(filter, slot, stack)) {
                    matchesFilter = true;
                    break;
                }
            }

            // add dir if matches with whitelist on or doesn't match with blacklist on
            if ((mode == TileEntityCraneRouter.MODE_WHITELIST && matchesFilter) || (mode == TileEntityCraneRouter.MODE_BLACKLIST && !matchesFilter)) {
                validDirs.add(side);
            }
        }

        // if no valid dirs have yet been found, use wildcard
        if (validDirs.isEmpty()) {
            for (int side = 0; side < 6; side++) {
                if (router.modes[side] == TileEntityCraneRouter.MODE_WILDCARD) validDirs.add(side);
            }
        }

        if (validDirs.isEmpty()) return 6;

        Level level = router.getLevel();
        int i = level == null ? 0 : level.getRandom().nextInt(validDirs.size());
        return validDirs.get(i);
    }
}
