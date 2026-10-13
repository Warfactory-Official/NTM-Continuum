package com.hbm.commands;

import com.hbm.Constants;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import com.hbm.itempool.ItemPool;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.golem.core.command.Permissions;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * 1.7 {@code CommandReloadRecipes}, {@code /ntmreload}: re-reads the config-file-driven registries without a restart.
 *
 * <p>1.7 reloaded fluids, fluid containers, every {@code SerializableRecipe} file, the item pools, damage resistances
 * and the logic-block tables. In the port the machine recipes and fluids are built in (data-pack recipes reload
 * with vanilla {@code /reload}); what is still read from {@code config/} is the custom machine recipe file
 * ({@code hbmRecipes/hbmCustomMachines.json}, 1.7's {@code SerializableRecipe} for that handler) and the item pools,
 * and those two are what this reloads. The error report is 1.7's, line for line.
 */
public final class CommandReloadRecipes {

    private CommandReloadRecipes() {}

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("ntmreload").requires(Permissions.level(4)).executes(ctx -> {
            CommandSourceStack source = ctx.getSource();
            try {
                CustomMachineRecipes.reload();
                ItemPool.load(source.getServer().registryAccess());
                source.sendSuccess(() -> Component.translatable("commands.hbm.ntmreload.success")
                        .withStyle(ChatFormatting.YELLOW), false);
                return 1;
            } catch (Exception ex) {
                Constants.LOG.error("/ntmreload failed", ex);
                reportError(source, "commands.hbm.ntmreload.error", ex);
                throw ex;
            }
        });
    }

    /** 1.7's five-line error block: rule, message, exception, first frame, rule. */
    static void reportError(CommandSourceStack source, String key, Exception ex) {
        Component rule = Component.literal("----------------------------------").withStyle(ChatFormatting.GRAY);
        source.sendSystemMessage(rule);
        source.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
        source.sendSystemMessage(Component.literal(String.valueOf(ex.getLocalizedMessage())).withStyle(ChatFormatting.RED));
        if (ex.getStackTrace().length > 0) {
            source.sendSystemMessage(Component.literal(ex.getStackTrace()[0].toString()).withStyle(ChatFormatting.RED));
        }
        source.sendSystemMessage(rule);
    }
}
