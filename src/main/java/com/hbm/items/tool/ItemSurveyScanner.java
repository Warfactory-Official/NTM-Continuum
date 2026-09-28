package com.hbm.items.tool;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.tileentity.BedrockOreBlockEntity;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.Teleports;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * 1.7 {@code ItemSurveyScanner}: an 11x11 grid of columns five blocks apart, sampled every second block from
 * 15 above the player down to y 2, reports oil, bedrock oil, coltan, depth rock, schist and australium in chat.
 *
 * <p>The bedrock ore line samples every second column of the bedrock floor (1.7's y 0 is the level's minimum
 * height here, where the deposits are generated) and names the last deposit found. The 1.7 easter egg is: use it on a beryllium block while carrying an entanglement
 * kit and the player is sent to the End.
 */
public class ItemSurveyScanner extends CompatItem {

    public ItemSurveyScanner(Properties properties) {
        super(properties);
    }

    private static String id(Block block) {
        var key = BuiltInRegistries.BLOCK.getKey(block);
        return key.getNamespace().equals("hbm") ? key.getPath() : "";
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide()) {
            int x = (int) Math.floor(player.getX());
            int y = (int) Math.floor(player.getY());
            int z = (int) Math.floor(player.getZ());

            boolean hasOil = false;
            boolean hasColtan = false;
            boolean hasBedrockOil = false;
            boolean hasDepth = false;
            boolean hasSchist = false;
            boolean hasAussie = false;
            BedrockOreBlockEntity tile = null;

            for (int a = -5; a <= 5; a++) {
                for (int b = -5; b <= 5; b++) {
                    for (int i = y + 15; i > 1; i -= 2) {
                        String block = id(world.getBlockState(new BlockPos(x + a * 5, i, z + b * 5)).getBlock());

                        //wow, this sucks!
                        switch (block) {
                            case "ore_oil" -> hasOil = true;
                            case "ore_coltan" -> hasColtan = true;
                            case "ore_bedrock_oil" -> hasBedrockOil = true;
                            case "stone_depth", "stone_depth_nether" -> hasDepth = true;
                            case "stone_gneiss" -> hasSchist = true;
                            case "ore_australium" -> hasAussie = true;
                            default -> { }
                        }
                    }

                    BlockPos floor = new BlockPos(x + a * 2, world.getMinY(), z + b * 2);
                    if (world.getBlockState(floor).is(ModOreBlocks.ore_bedrock_block.get())
                            && world.getBlockEntity(floor) instanceof BedrockOreBlockEntity ore) {
                        tile = ore;
                    }
                }
            }

            if (hasOil) say(player, "Found OIL!", ChatFormatting.BLACK);
            if (hasBedrockOil) say(player, "Found BEDROCK OIL!", ChatFormatting.BLACK);
            if (hasColtan) say(player, "Found COLTAN!", ChatFormatting.GOLD);
            if (hasDepth) say(player, "Found DEPTH ROCK!", ChatFormatting.GRAY);
            if (hasSchist) say(player, "Found SCHIST!", ChatFormatting.DARK_AQUA);
            if (hasAussie) say(player, "Found AUSTRALIUM!", ChatFormatting.YELLOW);
            if (tile != null && !tile.getResource().isEmpty()) {
                player.sendSystemMessage(Component.literal("Found BEDROCK ORE for ")
                        .append(tile.getResource().getHoverName()).append("!").withStyle(ChatFormatting.RED));
            }
        }

        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    private static void say(Player player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        if (player != null && id(world.getBlockState(context.getClickedPos()).getBlock()).equals("block_beryllium")
                && hasEntanglementKit(player)) {
            if (world instanceof ServerLevel server) {
                ServerLevel end = server.getServer().getLevel(Level.END);
                // 1.7 travelToDimension(1): the End's obsidian spawn platform at (100, 49, 0).
                if (end != null) Teleports.toLevel(player, end, 100.5D, 50D, 0.5D);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static boolean hasEntanglementKit(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("entanglement_kit")) {
                return true;
            }
        }
        return false;
    }
}
