package com.hbm.blocks.machine;

import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.machine.ItemMold;
import com.hbm.items.machine.ItemMold.Mold;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.TileEntityFoundryCastingBase;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Base for the casting blocks (1.12.2 {@code FoundryCastingBase}). Interactions: right-click takes the
 * finished item; right-click with a matching-size mold installs it (swapping out the old one);
 * <b>sneak-right-click with an empty hand extracts the mold</b> (replaces the old screwdriver);
 * right-click with a shovel empties the molten content to scraps. The look-overlay shows the installed
 * mold + fill state.
 */
public abstract class FoundryCastingBase extends dev.golem.core.world.CompatBlock implements EntityBlock, ICrucibleAcceptor, ILookOverlay {

    protected FoundryCastingBase(Properties properties) {
        super(properties);
    }

    protected abstract TileEntityFoundryCastingBase getTE(Level level, BlockPos pos);

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        TileEntityFoundryCastingBase cast = getTE(level, pos);
        if (cast == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        // take the finished item
        if (!cast.inventory.getStackInSlot(1).isEmpty()) {
            TileEntityFoundryCastingBase.giveOrDrop(level, pos, player, cast.inventory.getStackInSlot(1).copy());
            cast.inventory.setStackInSlot(1, ItemStack.EMPTY);
            cast.setChanged();
            return InteractionResult.SUCCESS;
        }

        // sneak + empty hand: extract the mold (replaces the old screwdriver)
        if (player.isShiftKeyDown() && !cast.inventory.getStackInSlot(0).isEmpty() && cast.amount == 0) {
            TileEntityFoundryCastingBase.giveOrDrop(level, pos, player, cast.inventory.getStackInSlot(0).copy());
            cast.inventory.setStackInSlot(0, ItemStack.EMPTY);
            cast.setChanged();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack heldItem, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        TileEntityFoundryCastingBase cast = getTE(level, pos);
        if (cast == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        // take the finished item first, regardless of what's held
        if (!cast.inventory.getStackInSlot(1).isEmpty()) {
            TileEntityFoundryCastingBase.giveOrDrop(level, pos, player, cast.inventory.getStackInSlot(1).copy());
            cast.inventory.setStackInSlot(1, ItemStack.EMPTY);
            cast.setChanged();
            return InteractionResult.SUCCESS;
        }

        if (heldItem.isEmpty() && player.isShiftKeyDown() && !cast.inventory.getStackInSlot(0).isEmpty() && cast.amount == 0) {
            TileEntityFoundryCastingBase.giveOrDrop(level, pos, player, cast.inventory.getStackInSlot(0).copy());
            cast.inventory.setStackInSlot(0, ItemStack.EMPTY);
            cast.setChanged();
            return InteractionResult.SUCCESS;
        }

        // insert a mold of the matching size
        Mold mold = ItemMold.getMold(heldItem);
        if (mold != null) {
            if (mold.size == cast.getMoldSize()) {
                if (!cast.inventory.getStackInSlot(0).isEmpty()) {
                    TileEntityFoundryCastingBase.giveOrDrop(level, pos, player, cast.inventory.getStackInSlot(0).copy());
                }

                ItemStack newMold = heldItem.copy();
                newMold.setCount(1);
                cast.inventory.setStackInSlot(0, newMold);
                if (!player.isCreative()) heldItem.shrink(1);

                level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.5F, 1.0F);
                cast.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        // shovel: scrap the molten content
        if (!heldItem.isEmpty() && heldItem.is(ItemTags.SHOVELS)) {
            if (cast.amount > 0 && cast.type != null) {
                ItemStack scrap = ItemScraps.create(new MaterialStack(cast.type, cast.amount));
                TileEntityFoundryCastingBase.giveOrDrop(level, pos, player, scrap);
                cast.amount = 0;
                cast.type = null;
                cast.setChanged();
            }
            return InteractionResult.SUCCESS;
        }

        return super.onUseItem(state, level, pos, player, hand, heldItem, hit);
    }

    // --- ICrucibleAcceptor: delegate to the TE ---

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        TileEntityFoundryCastingBase te = getTE(world, pos);
        return te != null && te.canAcceptPartialPour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, MaterialStack stack) {
        TileEntityFoundryCastingBase te = getTE(world, pos);
        return te == null ? stack : te.pour(world, pos, dX, dY, dZ, side, stack);
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, ForgeDirection side, MaterialStack stack) {
        TileEntityFoundryCastingBase te = getTE(world, pos);
        return te != null && te.canAcceptPartialFlow(world, pos, side, stack);
    }

    @Override
    public MaterialStack flow(Level world, BlockPos pos, ForgeDirection side, MaterialStack stack) {
        TileEntityFoundryCastingBase te = getTE(world, pos);
        return te == null ? stack : te.flow(world, pos, side, stack);
    }

    // --- look overlay: installed mold + fill state ---

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        TileEntityFoundryCastingBase cast = getTE(level, pos);
        if (cast == null) return null;

        List<String> text = new ArrayList<>();
        Mold mold = ItemMold.getMold(cast.syncMold);
        if (mold == null) {
            text.add("§cNo mold installed");
        } else {
            text.add("§e" + mold.getTitle());
            if (cast.type != null && cast.amount > 0) {
                text.add("&[" + cast.type.moltenColor + "&]" + cast.type.getDisplayName() + ": "
                        + cast.amount + " / " + mold.getCost());
            }
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xFF4000, 0x401000, text);
    }
}
