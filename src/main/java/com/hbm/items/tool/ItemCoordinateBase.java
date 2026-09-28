package com.hbm.items.tool;

import com.hbm.items.ModRadarUtilityItems;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code ItemCoordinateBase}: an item that remembers one block position, taken by right-clicking a block
 * the subclass accepts. The position was three NBT integers ({@code posX/posY/posZ}) and is one data component
 * here; a subclass that does not save Y stores 0 for it, as the missing 1.7 key read back as 0.
 */
public abstract class ItemCoordinateBase extends CompatItem {

    protected ItemCoordinateBase(Properties properties) {
        super(properties);
    }

    /** The saved position, or null when the item was never linked (1.7: no tag compound). */
    public static @Nullable BlockPos getPosition(ItemStack stack) {
        return ModRadarUtilityItems.COORDINATE_POSITION.get(stack).orElse(null);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();

        if (!this.canGrabCoordinateHere(level, clicked)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide()) {
            BlockPos pos = this.getCoordinates(level, clicked);
            ModRadarUtilityItems.COORDINATE_POSITION.set(context.getItemInHand(),
                    new BlockPos(pos.getX(), includeY() ? pos.getY() : 0, pos.getZ()));
            if (context.getPlayer() != null) this.onTargetSet(level, pos, context.getPlayer());
        }

        return InteractionResult.SUCCESS;
    }

    /** Whether this position can be saved or if the position target is valid. */
    public abstract boolean canGrabCoordinateHere(Level level, BlockPos pos);

    /** Whether this linking item saves the Y coordinate. */
    public boolean includeY() {
        return true;
    }

    /** Modifies the saved coordinates, for example detecting the core for multiblocks. */
    public BlockPos getCoordinates(Level level, BlockPos pos) {
        return pos;
    }

    /** Extra on successful target set, eg. sounds. */
    public void onTargetSet(Level level, BlockPos pos, Player player) { }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        BlockPos pos = getPosition(stack);
        if (pos != null) {
            tooltip.add(Component.literal("X: " + pos.getX()));
            if (includeY()) tooltip.add(Component.literal("Y: " + pos.getY()));
            tooltip.add(Component.literal("Z: " + pos.getZ()));
        } else {
            tooltip.add(Component.literal("No position set!").withStyle(ChatFormatting.RED));
        }
    }
}
