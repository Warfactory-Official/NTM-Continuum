package com.hbm.items.tool;

import com.hbm.api.item.IDesignatorItem;
import com.hbm.blocks.bomb.LaunchPad;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Designator, 1.12.2's {@code ItemDesignator}. Right-click a block to record its X/Z as the target,
 * then drop the designator into a launch pad's second slot.
 *
 * <p>Clicking a {@link LaunchPad} is explicitly <em>not</em> a target: upstream passes the interaction
 * through so the click opens the pad instead of aiming the missile at it.
 *
 * <p>The coordinates are two data components, not NBT integers ({@code xCoord}/{@code zCoord}).
 * Their presence is what {@link #isReady} tests, exactly as {@code hasTagCompound} did: a designator
 * that has never been pointed at anything is not ready, and one pointed at (0, 0) is.
 */
public class ItemDesignator extends CompatItem implements IDesignatorItem {

    public ItemDesignator(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        appendTargetTooltip(stack, tooltip);
    }

    /** Shared by the target-recording designators: the recorded X/Z, or the 1.7 hint. */
    static void appendTargetTooltip(ItemStack stack, List<Component> tooltip) {
        if (ModDataComponents.DESIGNATOR_X.has(stack)) {
            tooltip.add(Component.literal("Target Coordinates:"));
            tooltip.add(Component.literal("X: " + ModDataComponents.DESIGNATOR_X.getOr(stack, 0)));
            tooltip.add(Component.literal("Z: " + ModDataComponents.DESIGNATOR_Z.getOr(stack, 0)));
        } else {
            tooltip.add(Component.literal("Please select a target."));
        }
    }

    /** Records a target and plays the confirmation bleep. */
    static void setTarget(Level level, Player player, ItemStack stack, int x, int z,
                          Component message) {
        ModDataComponents.DESIGNATOR_X.set(stack, x);
        ModDataComponents.DESIGNATOR_Z.set(stack, z);

        if (!level.isClientSide()) player.sendSystemMessage(message);

        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        if (level.getBlockState(pos).getBlock() instanceof LaunchPad) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (context.getPlayer() == null) return InteractionResult.TRY_WITH_EMPTY_HAND;

        setTarget(level, context.getPlayer(), context.getItemInHand(), pos.getX(), pos.getZ(),
                Component.literal("Position set!"));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isReady(Level world, ItemStack stack, BlockPos padPos) {
        return ModDataComponents.DESIGNATOR_X.has(stack);
    }

    @Override
    public Vec3 getCoords(Level world, ItemStack stack, BlockPos padPos) {
        return new Vec3(ModDataComponents.DESIGNATOR_X.getOr(stack, 0), 0, ModDataComponents.DESIGNATOR_Z.getOr(stack, 0));
    }
}
