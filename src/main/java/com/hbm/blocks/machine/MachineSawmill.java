package com.hbm.blocks.machine;

import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntitySawmill;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The 3x3 sawmill multiblock and its core interaction rules. */
public class MachineSawmill extends BlockMultiblockMachine implements WorldlyContainerHolder, ILookOverlay {

    public MachineSawmill(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, 1.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-1.25D, 1D, -0.5D, -0.625D, 1.875D, 0.5D));
        this.bounding.add(new AABB(-0.625D, 1D, -1D, 1.375D, 2D, 1D));
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySawmill(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntitySawmill sawmill ? sawmill : null;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntitySawmill sawmill)) {
            return super.onUseItem(state, level, pos, player, hand, stack, hit);
        }

        if (stack.is(ModItems.sawblade.get()) && !sawmill.hasBlade) {
            if (!level.isClientSide()) {
                stack.shrink(1);
                sawmill.hasBlade = true;
                sawmill.setChanged();
                level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.5F, 0.75F);
            }
            return InteractionResult.SUCCESS;
        }

        if (takeOutputs(level, pos, player, sawmill)) return InteractionResult.SUCCESS;

        if (sawmill.canAcceptInput(stack)) {
            if (!level.isClientSide()) {
                ItemStack input = stack.copyWithCount(1);
                sawmill.inventory.setStackInSlot(0, input);
                stack.shrink(1);
                sawmill.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                   BlockHitResult hit) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntitySawmill sawmill
                && takeOutputs(level, pos, player, sawmill)) {
            return InteractionResult.SUCCESS;
        }
        // The legacy sawmill is hand-operated; it never opened a container screen.
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || ModDataComponents.ITEM_VARIANT.getOr(stack, 0) != 1) return;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntitySawmill sawmill) {
            sawmill.hasBlade = false;
            sawmill.setChanged();
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = new ItemStack(this);
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockPos core = MultiblockSurface.coreOfAny(params.getLevel(), pos);
        if (core != null && params.getLevel().getBlockEntity(core) instanceof TileEntitySawmill sawmill
                && !sawmill.hasBlade) {
            ModDataComponents.ITEM_VARIANT.set(stack, 1);
        }
        return List.of(stack);
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return cloneWithBladeState(level, pos);
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return cloneWithBladeState(level, pos);
    }

    private ItemStack cloneWithBladeState(BlockGetter level, BlockPos pos) {
        ItemStack stack = new ItemStack(this);
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntitySawmill sawmill
                && !sawmill.hasBlade) {
            ModDataComponents.ITEM_VARIANT.set(stack, 1);
        }
        return stack;
    }

    private static boolean takeOutputs(Level level, BlockPos pos, Player player, TileEntitySawmill sawmill) {
        if (level.isClientSide()) {
            return !sawmill.inventory.getStackInSlot(1).isEmpty()
                    || !sawmill.inventory.getStackInSlot(2).isEmpty();
        }
        boolean took = false;
        for (int slot = 1; slot <= 2; slot++) {
            ItemStack output = sawmill.inventory.getStackInSlot(slot);
            if (output.isEmpty()) continue;
            ItemStack copy = output.copy();
            if (!player.getInventory().add(copy)) {
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5D, pos.getY() + 1D,
                        pos.getZ() + 0.5D, copy));
            }
            sawmill.inventory.setStackInSlot(slot, ItemStack.EMPTY);
            took = true;
        }
        if (took) sawmill.setChanged();
        return took;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntitySawmill sawmill)) return null;

        List<String> text = new java.util.ArrayList<>();
        text.add(sawmill.heat + "TU/t");

        double percent = (double) sawmill.heat / 300D;
        int color = ((int) (0xFF - 0xFF * percent)) << 16 | ((int) (0xFF * percent) << 8);
        if (percent > 1D) color = 0xff0000;
        text.add("&[" + color + "&]" + ((sawmill.heat * 1000 / 300) / 10D) + "%");

        int limiter = sawmill.progress * 26 / TileEntitySawmill.PROCESSING_TIME;
        StringBuilder bar = new StringBuilder("§a[ ");
        for (int i = 0; i < 25; i++) {
            if (i == limiter) bar.append("§r");
            bar.append("\u258f");
        }
        bar.append("§a ]");
        text.add(bar.toString());

        for (int i = 0; i < 3; i++) {
            ItemStack stack = sawmill.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                text.add((i == 0 ? "§a-> " : "§c<- ") + "§r" + stack.getHoverName().getString()
                        + (stack.getCount() > 1 ? " x" + stack.getCount() : ""));
            }
        }

        if (sawmill.heat > 300) {
            text.add("&[" + (System.currentTimeMillis() % 1000 < 500 ? 0xff0000 : 0xffff00)
                    + "&]! ! ! OVERSPEED ! ! !");
        }
        if (!sawmill.hasBlade) text.add("&[16711680&]Blade missing!");

        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!level.isClientSide() && state.getValue(META) >= offset + 2
                && level.getBlockEntity(pos) instanceof TileEntitySawmill sawmill) {
            Containers.dropContents(level, pos, sawmill);
        }
        super.onBlockRemoved(state, level, pos, movedByPiston);
    }

    /** No power or fluid surface at all in 1.7; the four waist cells take items and nothing else. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_ITEMS);
    }
}
