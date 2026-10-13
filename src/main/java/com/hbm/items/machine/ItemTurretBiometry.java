package com.hbm.items.machine;

import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Biometric scanner, the turret whitelist card. Right-clicking writes the holder's name onto the
 * stack; a turret carrying the card will not fire on any name it lists. Ported from 1.12.2's
 * {@code ItemTurretBiometry}, whose NBT string list becomes
 * {@link ModDataComponents#TURRET_WHITELIST}.
 */
public class ItemTurretBiometry extends CompatItem {

    public ItemTurretBiometry(Properties properties) {
        super(properties);
    }

    /** The roster, or null when empty. 1.12.2 reads an empty roster as no roster. */
    public static String @Nullable [] getNames(ItemStack stack) {
        List<String> names = ModDataComponents.TURRET_WHITELIST.getOr(stack, List.of());
        return names.isEmpty() ? null : names.toArray(new String[0]);
    }

    public static void addName(ItemStack stack, String name) {
        List<String> names = new ArrayList<>(ModDataComponents.TURRET_WHITELIST.getOr(stack, List.of()));
        if (names.contains(name)) return;
        names.add(name);
        ModDataComponents.TURRET_WHITELIST.set(stack, List.copyOf(names));
    }

    public static void clearNames(ItemStack stack) {
        ModDataComponents.TURRET_WHITELIST.set(stack, List.of());
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        addName(stack, player.getScoreboardName());

        if (level.isClientSide()) player.sendSystemMessage(Component.translatable("chat.turretBiometryAdded"));

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
        player.swing(hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        String[] names = getNames(stack);
        if (names == null) return;
        for (String name : names) tooltip.add(Component.literal(name));
    }
}
