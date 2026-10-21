package com.hbm.items.tool;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.tileentity.BedrockOreBlockEntity;
import com.hbm.util.OverheadMarkers;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.Teleports;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 1.7 {@code ItemSurveyScanner}: an 11x11 grid of columns five blocks apart, sampled every second block from
 * 15 above the player down to y 2, reports oil, bedrock oil, coltan, depth rock, schist and australium in chat.
 *
 * <p>Bedrock oil and the 5x5 bedrock floor around the chunk centre (1.7's y 0 is the level's minimum height here)
 * also get a 15-second marker; the ore line names the last deposit found. The 1.7 easter egg: use it on a beryllium
 * block while carrying an entanglement kit and the player is sent to the End.
 */
public class ItemSurveyScanner extends CompatItem {

    public ItemSurveyScanner(Properties properties) {
        super(properties);
    }

    private static String id(Block block) {
        var key = BuiltInRegistries.BLOCK.getKey(block);
        return key.getNamespace().equals("hbm") ? key.getPath() : "";
    }

    /** One line the scanner can report: shown once if any sampled block matches. */
    public record Finding(Predicate<Block> matches, String message, ChatFormatting colour) {}

    /**
     * 1.7's checks in its message order (its "wow, this sucks!" if-chain: a block counts for the first check it matches).
     * An addon adds its own deposits with {@link #addFinding}; NTM: Space's natural gas and brine went in after OIL.
     */
    private static final List<Finding> FINDINGS = new CopyOnWriteArrayList<>(List.of(
            hbm("Found OIL!", ChatFormatting.BLACK, "ore_oil"),
            hbm("Found BEDROCK OIL!", ChatFormatting.BLACK, "ore_bedrock_oil"),
            hbm("Found COLTAN!", ChatFormatting.GOLD, "ore_coltan", "ore_coltan_deepslate"),
            hbm("Found DEPTH ROCK!", ChatFormatting.GRAY, "stone_depth", "stone_depth_nether"),
            hbm("Found SCHIST!", ChatFormatting.DARK_AQUA, "stone_gneiss"),
            hbm("Found AUSTRALIUM!", ChatFormatting.YELLOW, "ore_australium")));

    private static Finding hbm(String message, ChatFormatting colour, String... paths) {
        List<String> ids = List.of(paths);
        return new Finding(block -> ids.contains(id(block)), message, colour);
    }

    /** Adds a finding at {@code index} of the report order (0 = first, {@link #findings()}{@code .size()} = last). */
    public static void addFinding(int index, Finding finding) {
        FINDINGS.add(index, finding);
    }

    /** The findings in report order (read-only). */
    public static List<Finding> findings() {
        return Collections.unmodifiableList(FINDINGS);
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide()) {
            int x = (int) Math.floor(player.getX());
            int y = (int) Math.floor(player.getY());
            int z = (int) Math.floor(player.getZ());

            List<Finding> findings = List.copyOf(FINDINGS);
            boolean[] found = new boolean[findings.size()];
            BedrockOreBlockEntity tile = null;

            ServerPlayer marked = player instanceof ServerPlayer sp ? sp : null;
            for (int a = -5; a <= 5; a++) {
                for (int b = -5; b <= 5; b++) {
                    for (int i = y + 15; i > 1; i -= 2) {
                        BlockPos at = new BlockPos(x + a * 5, i, z + b * 5);
                        Block block = world.getBlockState(at).getBlock();
                        for (int f = 0; f < findings.size(); f++) {
                            if (findings.get(f).matches().test(block)) {
                                found[f] = true;
                                break;
                            }
                        }
                        if (id(block).equals("ore_bedrock_oil")) {
                            if (marked != null) OverheadMarkers.send(marked, at, 0x000000, "Bedrock Oil", 15_000, 300D);
                            break;
                        }
                    }
                }
            }

            int cX = ((x >> 4) << 4) + 8;
            int cZ = ((z >> 4) << 4) + 8;
            for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
                BlockPos floor = new BlockPos(cX + i, world.getMinY(), cZ + j);
                if (!world.getBlockState(floor).is(ModOreBlocks.ore_bedrock_block.get())) continue;
                if (marked != null) OverheadMarkers.send(marked, floor, 0xff0000, "Bedrock Ore", 15_000, 300D);
                if (world.getBlockEntity(floor) instanceof BedrockOreBlockEntity ore) tile = ore;
            }

            for (int f = 0; f < findings.size(); f++) {
                if (found[f]) say(player, findings.get(f).message(), findings.get(f).colour());
            }
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
