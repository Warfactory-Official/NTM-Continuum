package com.hbm.items.tool;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
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
 * 15 above the player down to two above the world floor, reports oil, bedrock oil, coltan, depth rock, schist and australium in chat.
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
    public enum Deposit {
        OIL("Found OIL!", "chat.surveyScanner.foundOil", ChatFormatting.BLACK),
        BEDROCK_OIL("Found BEDROCK OIL!", "chat.surveyScanner.foundBedrockOil", ChatFormatting.BLACK),
        COLTAN("Found COLTAN!", "chat.surveyScanner.foundColtan", ChatFormatting.GOLD),
        DEPTH("Found DEPTH ROCK!", "chat.surveyScanner.foundDepthRock", ChatFormatting.GRAY),
        SCHIST("Found SCHIST!", "chat.surveyScanner.foundSchist", ChatFormatting.DARK_AQUA),
        AUSTRALIUM("Found AUSTRALIUM!", "chat.surveyScanner.foundAustralium", ChatFormatting.YELLOW);

        final String text;
        final String key;
        final ChatFormatting colour;
        Deposit(String text, String key, ChatFormatting colour) { this.text = text; this.key = key; this.colour = colour; }
        public Component message() { return Component.translatable(key).withStyle(colour); }
    }

    public record Finding(Predicate<Block> matches, String message, ChatFormatting colour,
                          @Nullable Deposit deposit) {
        public Finding(Predicate<Block> matches, String message, ChatFormatting colour) { this(matches, message, colour, null); }
        Component report() { return deposit == null ? Component.literal(message).withStyle(colour) : deposit.message(); }
    }

    private record Alias(Supplier<? extends Block> block, Deposit deposit) {}
    private record Line(Component message, List<Supplier<? extends Block>> blocks) {}
    private static final List<Alias> ALIASES = new CopyOnWriteArrayList<>();
    private static final List<Line> LINES = new CopyOnWriteArrayList<>();

    /** A real scan result, also used by the held item's chat and marker path. */
    public record Survey(Set<Deposit> deposits, ItemStack bedrockOre, List<Component> lines, List<Component> messages) {}

    public static void alias(Supplier<? extends Block> block, Deposit deposit) { ALIASES.add(new Alias(block, deposit)); }
    public static void addLine(String key, ChatFormatting colour, List<Supplier<? extends Block>> blocks) {
        LINES.add(new Line(Component.translatable(key).withStyle(colour), List.copyOf(blocks)));
    }

    /**
     * 1.7's checks in its message order (its "wow, this sucks!" if-chain: a block counts for the first check it matches).
     * An addon adds its own deposits with {@link #addFinding}; NTM: Space's natural gas and brine went in after OIL.
     */
    private static final List<Finding> FINDINGS = new CopyOnWriteArrayList<>(List.of(
            hbm(Deposit.OIL, "ore_oil"),
            hbm(Deposit.BEDROCK_OIL, "ore_bedrock_oil"),
            hbm(Deposit.COLTAN, "ore_coltan", "ore_coltan_deepslate"),
            hbm(Deposit.DEPTH, "stone_depth", "stone_depth_nether"),
            hbm(Deposit.SCHIST, "stone_gneiss"),
            hbm(Deposit.AUSTRALIUM, "ore_australium")));

    private static Finding hbm(Deposit deposit, String... paths) {
        List<String> ids = List.of(paths);
        return new Finding(block -> ids.contains(id(block)), deposit.text, deposit.colour, deposit);
    }

    /** Adds a finding at {@code index} of the report order (0 = first, {@link #findings()}{@code .size()} = last). */
    public static void addFinding(int index, Finding finding) {
        FINDINGS.add(index, finding);
    }

    /** The findings in report order (read-only). */
    public static List<Finding> findings() {
        return Collections.unmodifiableList(FINDINGS);
    }

    public static Survey scan(Level world, BlockPos origin) { return scan(world, origin, null); }

    private static Survey scan(Level world, BlockPos origin, @Nullable ServerPlayer marked) {
        var findings = List.copyOf(FINDINGS);
        var lines = List.copyOf(LINES);
        var aliases = new IdentityHashMap<Block, Deposit>();
        for (var alias : ALIASES) aliases.put(alias.block().get(), alias.deposit());
        var lineBlocks = new IdentityHashMap<Block, Integer>();
        for (var i = 0; i < lines.size(); i++) for (var block : lines.get(i).blocks()) lineBlocks.put(block.get(), i);
        var found = new boolean[findings.size()];
        var foundLines = new BitSet();
        var deposits = EnumSet.noneOf(Deposit.class);
        BedrockOreBlockEntity tile = null;
        var x = origin.getX(); var y = origin.getY(); var z = origin.getZ();
        for (var a = -5; a <= 5; a++) for (var b = -5; b <= 5; b++) {
            for (var i = y + 15; i > world.getMinY() + 1; i -= 2) {
                var at = new BlockPos(x + a * 5, i, z + b * 5);
                var block = world.getBlockState(at).getBlock();
                var alias = aliases.get(block);
                for (var f = 0; f < findings.size(); f++) {
                    var finding = findings.get(f);
                    if (finding.matches().test(block) || alias != null && finding.deposit() == alias) {
                        found[f] = true;
                        if (finding.deposit() != null) deposits.add(finding.deposit());
                        break;
                    }
                }
                var line = lineBlocks.get(block);
                if (line != null) foundLines.set(line);
                if (id(block).equals("ore_bedrock_oil") || alias == Deposit.BEDROCK_OIL) {
                    if (marked != null) OverheadMarkers.send(marked, at, 0x000000, "Bedrock Oil", 15_000, 300D);
                    break;
                }
            }
        }
        var cX = ((x >> 4) << 4) + 8;
        var cZ = ((z >> 4) << 4) + 8;
        for (var i = -2; i <= 2; i++) for (var j = -2; j <= 2; j++) {
            var floor = new BlockPos(cX + i, world.getMinY(), cZ + j);
            if (!world.getBlockState(floor).is(ModOreBlocks.ore_bedrock_block.get())) continue;
            if (marked != null) OverheadMarkers.send(marked, floor, 0xff0000, "Bedrock Ore", 15_000, 300D);
            if (world.getBlockEntity(floor) instanceof BedrockOreBlockEntity ore) tile = ore;
        }
        var extra = new ArrayList<Component>();
        for (var i = foundLines.nextSetBit(0); i >= 0; i = foundLines.nextSetBit(i + 1)) extra.add(lines.get(i).message());
        var messages = new ArrayList<Component>();
        for (var f = 0; f < findings.size(); f++) {
            if (found[f]) messages.add(findings.get(f).report());
            if (findings.get(f).deposit() == Deposit.OIL) messages.addAll(extra);
        }
        return new Survey(Set.copyOf(deposits), tile == null ? ItemStack.EMPTY : tile.getResource().copy(),
                List.copyOf(extra), List.copyOf(messages));
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide()) {
            var survey = scan(world, player.blockPosition(), player instanceof ServerPlayer server ? server : null);
            for (var message : survey.messages()) player.sendSystemMessage(message);
            if (!survey.bedrockOre().isEmpty()) player.sendSystemMessage(Component.translatable(
                    "chat.surveyScanner.foundBedrockOreFor", survey.bedrockOre().getHoverName()).withStyle(ChatFormatting.RED));
        }
        player.swing(hand);
        return InteractionResult.SUCCESS;
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
