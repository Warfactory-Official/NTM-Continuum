package com.hbm.blocks.bomb;

import com.hbm.items.ModItems;
import com.hbm.tileentity.bomb.TileEntityFireworks;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Dyes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockFireworks}: the firework battery's loading and readout interface.
 *
 * <p>Gunpowder loads three charges a piece and sulfur one, both consuming the <b>whole stack</b>; a
 * dye sets the burst colour and a name tag the message, one item each. Anything else - an empty hand
 * included - prints the current charge count, colour and message. Upstream returns from each loading
 * branch, so a stack that loads never also prints, which is why the readout sits in the empty-hand
 * hook rather than at the end of the item one.
 *
 * <p>The colour comes from {@code DyeColor.getFireworkColor()} rather than an index into 1.7's
 * {@code ItemDye.field_150922_c}. It is the same table in role - the colour a dye gives a firework -
 * but reading the old one by damage value would be wrong twice over on modern versions: dyes are
 * sixteen separate items with no damage value, and {@code DyeColor}'s ids run the opposite way.
 */
public class BlockFireworks extends CompatBlock implements TickingBlock {

    /** Charges a single piece of gunpowder is worth; sulfur is worth one. */
    private static final int GUNPOWDER_CHARGES = 3;

    public BlockFireworks(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFireworks(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFireworks fireworks)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (stack.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // A local rather than an `instanceof DyeColor dye` pattern: the facade already returns a
        // DyeColor, and a pattern whose type the expression always matches is an error on Java 17.
        DyeColor dye = Dyes.colorOf(stack);

        if (stack.is(Items.GUNPOWDER)) {
            fireworks.charges += stack.getCount() * GUNPOWDER_CHARGES;
            stack.setCount(0);
        } else if (stack.is(ModItems.sulfur.get())) {
            fireworks.charges += stack.getCount();
            stack.setCount(0);
        } else if (dye != null) {
            fireworks.color = dye.getFireworkColor();
            stack.shrink(1);
        } else if (stack.is(Items.NAME_TAG)) {
            fireworks.message = stack.getHoverName().getString();
            stack.shrink(1);
        } else {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        fireworks.setChanged();
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityFireworks fireworks)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        player.sendSystemMessage(Component.translatable("tile.fireworks.name")
                .withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.translatable("tile.fireworks.charges", fireworks.charges)
                .withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("tile.fireworks.color",
                Integer.toHexString(fireworks.color)).withStyle(ChatFormatting.YELLOW));
        player.sendSystemMessage(Component.translatable("tile.fireworks.message", fireworks.message)
                .withStyle(ChatFormatting.YELLOW));
        return InteractionResult.SUCCESS;
    }
}
