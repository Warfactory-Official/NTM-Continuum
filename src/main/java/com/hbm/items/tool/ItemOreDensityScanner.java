package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.packet.PlayerInformPayload;
import com.hbm.packet.PlayerInformPayload.Colored;
import com.hbm.packet.PlayerInformPayload.Translated;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ItemOreDensityScanner}: anywhere in the inventory, every 5 ticks each bedrock ore type's density at
 * the player's column goes to the info HUD (ids 777-782) with its grade in the grade's colour, followed by the
 * average's bore tier and bore fluid (id 783).
 */
public class ItemOreDensityScanner extends CompatItem {

    public ItemOreDensityScanner(Properties properties) {
        super(properties);
    }

    @Override
    protected void compatInventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot,
                                       boolean held) {
        if (!(entity instanceof ServerPlayer player) || world.getGameTime() % 5 != 0) return;

        double totalLevel = 0D;
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        // hbm-space: the types of the body the player stands on (ItemBedrockOreBase.SOURCE); plain HBM: its six
        ItemBedrockOreBase.OreField field = ItemBedrockOreBase.field(world, x, z);

        for (Map.Entry<BedrockOreType, Double> entry : field.levels().entrySet()) {
            BedrockOreType type = entry.getKey();
            double level = entry.getValue();
            GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC, PlayerInformPayload.of(
                    777 + type.ordinal(), 4000, ChatFormatting.RESET,
                    new Translated(type.translationKey()),
                    ": " + ((int) (level * 100) / 100D) + " (",
                    new Colored(new Translated(translateDensity(level)), getColor(level)),
                    ")"));
            totalLevel += level;
        }
        totalLevel /= field.levels().size();

        int tier = getTier(totalLevel);
        FluidStack boreFluid = getBoreFluid(totalLevel);
        // hbm-space: a body's own bedrock acid (WorldProviderCelestial.getBedrockAcid) replaces the tier's fluid
        if (field.boreFluid() != null) boreFluid = field.boreFluid();

        List<Object> parts = new ArrayList<>();
        parts.add("Tier " + tier);
        if (boreFluid != null) {
            parts.add(" - " + boreFluid.fill + "mB ");
            parts.add(new Translated(boreFluid.type.getTranslationKey()));
        }
        GolemNetwork.sendToPlayer(player, PlayerInformPayload.CODEC, PlayerInformPayload.of(
                777 + BedrockOreType.values().size(), 4000, ChatFormatting.YELLOW, parts.toArray()));
    }

    public static String translateDensity(double density) {
        if (density <= 0.1) return "item.ore_density_scanner.verypoor";
        if (density <= 0.35) return "item.ore_density_scanner.poor";
        if (density <= 0.75) return "item.ore_density_scanner.low";
        if (density >= 1.9) return "item.ore_density_scanner.excellent";
        if (density >= 1.65) return "item.ore_density_scanner.veryhigh";
        if (density >= 1.25) return "item.ore_density_scanner.high";
        return "item.ore_density_scanner.moderate";
    }

    public static ChatFormatting getColor(double density) {
        if (density <= 0.1) return ChatFormatting.DARK_RED;
        if (density <= 0.35) return ChatFormatting.RED;
        if (density <= 0.75) return ChatFormatting.GOLD;
        if (density > 2) return ChatFormatting.LIGHT_PURPLE; // only for BO items that got mined with fortune
        if (density >= 1.9) return ChatFormatting.AQUA;
        if (density >= 1.65) return ChatFormatting.BLUE;
        if (density >= 1.25) return ChatFormatting.GREEN;
        return ChatFormatting.YELLOW;
    }

    /** 1.7 {@code BedrockOre.getBoreFluid}: tier 1 needs none. */
    public static @Nullable FluidStack getBoreFluid(double density) {
        if (density > 1.5) return new FluidStack(2_000, Fluids.SOLVENT);
        if (density > 1) return new FluidStack(1_000, Fluids.SULFURIC_ACID);
        if (density > 0.75) return new FluidStack(1_000, Fluids.WATER);
        return null;
    }

    /** 1.7 {@code BedrockOre.getTier}. */
    public static int getTier(double density) {
        if (density > 1.5) return 4;
        if (density > 1) return 3;
        if (density > 0.75) return 2;
        return 1;
    }
}
