package com.hbm.items.tool;

import com.hbm.Constants;
import com.hbm.config.GeneralConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IBomb.BombReturnCode;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemMultiDetonator}: shift-click blocks to collect positions, right-click to fire every
 * {@link IBomb} among them, shift-click in the air to clear. The three parallel int arrays
 * ({@code xValues}/{@code yValues}/{@code zValues}) are one list-of-positions component here.
 */
public class ItemMultiDetonator extends CompatItem {

    public static final GolemComponent<List<BlockPos>> POSITIONS =
            GolemComponent.of(Constants.MOD_ID, "detonator_positions", BlockPos.CODEC.listOf());

    public ItemMultiDetonator(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Shift right-click block to add position,"));
        tooltip.add(Component.literal("right-click to detonate!"));
        tooltip.add(Component.literal("Shift right-click in the air to clear positions."));

        List<BlockPos> locations = getLocations(stack);
        if (locations.isEmpty()) {
            tooltip.add(Component.literal("No position set!").withStyle(ChatFormatting.RED));
        } else {
            for (BlockPos pos : locations) {
                tooltip.add(Component.literal(pos.getX() + " / " + pos.getY() + " / " + pos.getZ())
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Level level = context.getLevel();
        addLocation(context.getItemInHand(), context.getClickedPos());
        if (!level.isClientSide()) {
            player.sendSystemMessage(prefix().append(Component.literal("Position added!").withStyle(ChatFormatting.GREEN)));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                SoundSource.PLAYERS, 2.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        List<BlockPos> locations = getLocations(stack);

        if (locations.isEmpty()) {
            if (!level.isClientSide()) {
                player.sendSystemMessage(prefix().append(Component.literal("No position set!").withStyle(ChatFormatting.RED)));
            }
            return InteractionResult.SUCCESS;
        }

        if (!player.isShiftKeyDown()) {
            int succ = 0;
            if (!level.isClientSide()) {
                for (BlockPos pos : locations) {
                    if (level.getBlockState(pos).getBlock() instanceof IBomb bomb) {
                        BombReturnCode ret = com.hbm.api.event.HbmEvents.detonate(bomb, level, pos, player);
                        if (ret.wasSuccessful()) succ++;
                        if (GeneralConfig.enableExtendedLogging) {
                            Constants.LOG.info("[DET] Tried to detonate block at {} / {} / {} by {}!", pos.getX(), pos.getY(),
                                    pos.getZ(), player.getName().getString());
                        }
                    }
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            if (!level.isClientSide()) {
                player.sendSystemMessage(prefix().append(Component.literal("Triggered " + succ + "/" + locations.size() + "!")
                        .withStyle(ChatFormatting.YELLOW)));
            }
        } else {
            POSITIONS.remove(stack);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 2.0F, 1.0F);
            if (!level.isClientSide()) {
                player.sendSystemMessage(prefix().append(Component.literal("Locations cleared!").withStyle(ChatFormatting.RED)));
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                .append(Component.translatable("item.hbm.detonator_multi").withStyle(ChatFormatting.DARK_AQUA))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA));
    }

    private static void addLocation(ItemStack stack, BlockPos pos) {
        List<BlockPos> locations = new ArrayList<>(getLocations(stack));
        locations.add(pos);
        POSITIONS.set(stack, List.copyOf(locations));
    }

    public static List<BlockPos> getLocations(ItemStack stack) {
        return POSITIONS.getOr(stack, List.of());
    }
}
