package com.hbm.items.machine;

import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** The reactor remote sensor: clicked on a research reactor it remembers the position for the control block. */
public class ItemReactorSensor extends CompatItem {

    /** The old {@code x/y/z} NBT triple, packed. */
    public static final GolemComponent<Long> TARGET = GolemComponent.longValue("hbm", "reactor_sensor_target");

    public ItemReactorSensor(Properties properties) {
        super(properties);
    }

    public static @Nullable BlockPos getTarget(ItemStack stack) {
        return TARGET.has(stack) ? BlockPos.of(TARGET.getOr(stack, 0L)) : null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (level.getBlockState(pos).getBlock() == ModNuclearMiscBlocks.REACTOR_RESEARCH.get()) {
            if (!level.isClientSide() && player != null) {
                player.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.DARK_AQUA)
                        .append(Component.translatable(getDescriptionId()).withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("] ").withStyle(ChatFormatting.DARK_AQUA))
                        .append(Component.literal("Position set!").withStyle(ChatFormatting.GREEN)));
            }
            TARGET.set(stack, pos.asLong());
            if (player != null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                player.swing(context.getHand());
            }
            return InteractionResult.SUCCESS;
        }
        InteractionResult other = USE.invoker().useOn(context);
        if (other != null) return other;
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    /** What the sensor does on a block that is not a research reactor; null means "not mine". */
    @FunctionalInterface
    public interface Use {
        @Nullable InteractionResult useOn(UseOnContext context);
    }

    /**
     * Addon hook: the sensor used on another block (NTM: Space's StarDar and its remote console). Asked after the
     * research-reactor check; the first non-null answer is the use's result. Plain HBM registers nothing.
     */
    public static final Hook<Use> USE = Hook.of(listeners -> context -> {
        for (Use listener : listeners) {
            InteractionResult result = listener.useOn(context);
            if (result != null) return result;
        }
        return null;
    });

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos target = getTarget(stack);
        if (target != null) {
            tooltip.add(Component.literal("x: " + target.getX()));
            tooltip.add(Component.literal("y: " + target.getY()));
            tooltip.add(Component.literal("z: " + target.getZ()));
        } else {
            tooltip.add(Component.literal("No reactor selected!"));
        }
    }
}
