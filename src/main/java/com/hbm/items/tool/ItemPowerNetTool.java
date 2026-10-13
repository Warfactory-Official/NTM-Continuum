package com.hbm.items.tool;

import com.hbm.api.energymk2.CableData;
import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.PowerGraph;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.uninos.graph.NodeNetwork;
import com.hbm.util.NetworkDiagnostics;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1.7 {@code ItemPowerNetTool}, the Cable Network Analysis Tool: right-click a conductor and the diagnostic of
 * its power net goes to chat - the net's id (its identity hash), link, provider and receiver counts. Counts
 * come from {@link NetworkDiagnostics}, the graph's equivalent of 1.7's subscriber lists.
 *
 * <p>Every link of the net (each node and every cell of every segment, 1.7's {@code link.positions}) also gets the
 * yellow floating net id 1.5 blocks above it ({@code debug} text particle, scale 0.5), sent to players within 20 blocks
 * of that link.
 */
public class ItemPowerNetTool extends CompatItem {

    private static final int radius = 20;

    public ItemPowerNetTool(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockPos core = MultiblockSurface.coreOfAny(world, pos);
        if (core != null) pos = core;

        BlockEntity te = world.getBlockEntity(pos);
        if (world.isClientSide()) return InteractionResult.SUCCESS;
        if (!(te instanceof IEnergyConductorMK2) || player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        ServerLevel server = (ServerLevel) world;
        NodeNetwork<CableData> net = PowerGraph.get(server).networkAt(pos.asLong());
        if (net != null) {
            String id = Integer.toHexString(net.hashCode());
            say(player, "Start of diagnostic for network " + id, ChatFormatting.GOLD);
            say(player, "Links: " + net.size(), ChatFormatting.YELLOW);
            say(player, "Providers: " + NetworkDiagnostics.countEndpoints(server, net,
                    be -> be instanceof IEnergyProviderMK2), ChatFormatting.YELLOW);
            say(player, "Receivers: " + NetworkDiagnostics.countEndpoints(server, net,
                    be -> be instanceof IEnergyReceiverMK2), ChatFormatting.YELLOW);
            say(player, "End of diagnostic for network " + id, ChatFormatting.GOLD);
            Set<Long> links = new LinkedHashSet<>();
            for (var node : net.nodes) links.add(node.posKey);
            for (var segment : net.segments) for (int i = 0; i < segment.cellCount(); i++) links.add(segment.cellAt(i));
            for (long key : links) {
                BlockPos link = BlockPos.of(key);
                CompoundTag data = new CompoundTag();
                data.putInt("color", 0xffff00);
                data.putFloat("scale", 0.5F);
                data.putString("text", id);
                HbmEffectNT.DebugText.send(server, link.getX() + 0.5, link.getY() + 1.5,
                        link.getZ() + 0.5, data, radius);
            }
        } else {
            say(player, "Error: No network found!", ChatFormatting.RED);
        }
        return InteractionResult.SUCCESS;
    }

    private static void say(Player player, String text, ChatFormatting color) {
        player.sendSystemMessage(Component.literal(text).withStyle(color));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Right-click cable to analyze the power net.").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Links (cables, poles, etc.) are YELLOW").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Subscribers (any receiver) are BLUE").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Links with mismatching network info (BUGGED!) are RED").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Displays stats such as link and subscriber count").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Proxies are connection points for multiblock links (e.g. 4 for substations)").withStyle(ChatFormatting.RED));
        list.add(Component.literal("Particles only spawn in a " + radius + " block radius!").withStyle(ChatFormatting.RED));
    }
}
