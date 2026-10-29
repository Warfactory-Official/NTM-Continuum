package com.hbm.commands;

import dev.golem.core.Ids;
import com.hbm.items.ISatChip;
import com.hbm.items.ModSatelliteItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.api.environment.Environment;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import dev.golem.core.command.Permissions;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * 1.7 {@code CommandSatellites}, {@code /ntmsatellites}: {@code orbit} launches the held satellite chip straight
 * into orbit on its frequency, {@code descend <frequency>} deletes a satellite, {@code list} prints every
 * satellite as "frequency - class". Player only; permission level 4 (1.7 {@code CommandBase}'s default).
 */
public final class CommandSatellites {

    private CommandSatellites() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmsatellites")
                .requires(Permissions.level(4))
                .then(Commands.literal("orbit").executes(ctx -> orbit(ctx.getSource())))
                .then(Commands.literal("descend")
                        .then(Commands.argument("frequency", IntegerArgumentType.integer())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(frequencies(ctx.getSource()), builder))
                                .executes(ctx -> descend(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "frequency")))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource()))));
    }

    private static Iterable<String> frequencies(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer)) return List.of();
        return Environment.satelliteLevels(source.getLevel(), BlockPos.containing(source.getPosition())).stream()
                          .flatMap(level -> SatelliteSavedData.get(level).sats.keySet().stream())
                          .map(String::valueOf).distinct().toList();
    }

    private static boolean notPlayer(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer) return false;
        source.sendFailure(Component.translatable("commands.satellite.should_be_run_as_player").withStyle(ChatFormatting.RED));
        return true;
    }

    private static int orbit(CommandSourceStack source) {
        if (notPlayer(source)) return 0;
        ServerPlayer player = (ServerPlayer) source.getEntity();
        ItemStack held = player.getMainHandItem();

        // 1.7: any ISatChip but the blank ID chip. A chip whose item launches nothing is still consumed and still
        // reported as launched, because XSatelliteRegistry.orbit quietly does nothing for it.
        if (held.getItem() instanceof ISatChip && held.getItem() != ModSatelliteItems.sat_chip.get()) {
            Satellite.orbit(player.level() instanceof ServerLevel level ? level : source.getLevel(), held,
                    ISatChip.getFreqS(held), player.getX(), player.getY(), player.getZ());
            held.shrink(1);
            source.sendSuccess(() -> Component.translatable("commands.satellite.satellite_orbited")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }

        source.sendFailure(Component.translatable("commands.satellite.not_a_satellite").withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int descend(CommandSourceStack source, int freq) {
        if (notPlayer(source)) return 0;
        SatelliteSavedData data = SatelliteSavedData.get(source.getLevel(), BlockPos.containing(source.getPosition()),
                freq);
        if (data.descend(freq)) {
            source.sendSuccess(() -> Component.translatable("commands.satellite.satellite_descended")
                    .withStyle(ChatFormatting.GREEN), false);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.satellite.no_satellite").withStyle(ChatFormatting.RED));
        return 0;
    }

    private static int list(CommandSourceStack source) {
        if (notPlayer(source)) return 0;
        var visible = Environment.satelliteLevels(source.getLevel(), BlockPos.containing(source.getPosition()));
        int count = 0;
        for (var level : visible) {
            var data = SatelliteSavedData.get(level);
            String namespace = visible.size() == 1 ? "" : "[" + Ids.keyId(level.dimension()) + "] ";
            for (Map.Entry<Integer, Satellite> entry : data.sats.entrySet()) {
                String messageText = namespace + entry.getKey() + " - " + entry.getValue().getClass().getSimpleName();
                source.sendSuccess(() -> Component.literal(messageText).withStyle(ChatFormatting.GREEN), false);
                count++;
            }
        }
        if (count == 0) {
            source.sendFailure(Component.translatable("commands.satellite.no_active_satellites").withStyle(ChatFormatting.RED));
            return 0;
        }
        return count;
    }
}
