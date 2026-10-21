package com.hbm.items.tool;

import dev.golem.core.world.Explosions;
import com.hbm.Constants;
import com.hbm.api.event.HbmEvents;
import com.hbm.config.GeneralConfig;
import com.hbm.config.WeaponConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import com.hbm.handler.ExplosivesEntityHooks;

/**
 * The two 1.7 {@code ItemDrop} detonators. The <b>dead man's detonator</b> binds a position with a
 * shift-click and fires the bomb there the moment its item entity exists in the world (drop it - or
 * die holding it); the <b>dead man's explosive</b> is a 15-strength blast on the same trigger. Both
 * are driven from the loaders' item-entity tick through {@link ExplosivesEntityHooks}.
 */
public class ItemDropDetonator extends CompatItem {

    public enum Kind { DEADMAN, DE }

    public final Kind kind;

    public ItemDropDetonator(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        if (kind == Kind.DEADMAN) {
            tooltip.add(Component.literal("Shift right-click to set position,"));
            tooltip.add(Component.literal("drop to detonate!"));
            BlockPos pos = ModDataComponents.DETONATOR_TARGET.get(stack).orElse(null);
            if (pos == null) {
                tooltip.add(Component.literal("No position set!"));
            } else {
                tooltip.add(Component.literal("Set pos to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            }
        } else {
            tooltip.add(Component.literal("Explodes when dropped!"));
        }
        tooltip.add(Component.literal("[").append(Component.translatable("trait.drop")).append("]")
                .withStyle(ChatFormatting.RED));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (kind != Kind.DEADMAN || player == null || !player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        ModDataComponents.DETONATOR_TARGET.set(context.getItemInHand(), context.getClickedPos());
        Level level = context.getLevel();
        if (level.isClientSide()) {
            player.sendSystemMessage(Component.literal("Position set!"));
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                SoundSource.PLAYERS, 2.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** 1.7 {@code onEntityItemUpdate}; called every tick for every item entity. */
    public static void tickDropped(ItemEntity entity) {
        Level level = entity.level();
        if (level.isClientSide() || !(entity.getItem().getItem() instanceof ItemDropDetonator item)) return;
        ItemStack stack = entity.getItem();

        if (item.kind == Kind.DEADMAN) {
            BlockPos pos = ModDataComponents.DETONATOR_TARGET.get(stack).orElse(null);
            if (pos != null && level.getBlockState(pos).getBlock() instanceof IBomb bomb) {
                HbmEvents.detonate(bomb, level, pos, entity);
                if (GeneralConfig.enableExtendedLogging) {
                    Constants.LOG.info("[DET] Tried to detonate block at {} / {} / {} by dead man's switch!",
                            pos.getX(), pos.getY(), pos.getZ());
                }
            }
            Explosions.explode(level, entity, entity.getX(), entity.getY(), entity.getZ(), 0.0F, Level.ExplosionInteraction.TNT);
            entity.discard();
        } else {
            if (WeaponConfig.dropDead) { // 1.7 10.05_dropDead
                Explosions.explode(level, entity, entity.getX(), entity.getY(), entity.getZ(), 15.0F, Level.ExplosionInteraction.TNT);
                if (GeneralConfig.enableExtendedLogging) {
                    Constants.LOG.info("[DET] Detonated dead man's explosive at {} / {} / {}!",
                            (int) entity.getX(), (int) entity.getY(), (int) entity.getZ());
                }
            }
            entity.discard();
        }
    }
}
