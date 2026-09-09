package com.hbm.items.tool;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** The old shift-use turret mob-filter tool. */
public final class ItemTurretMobFilter extends CompatItem {
    private static volatile Consumer<TileEntityTurretBaseNT> clientScreenOpener;

    public ItemTurretMobFilter(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Installs the client-only screen bridge without putting client classes on the common path. */
    public static void setClientScreenOpener(Consumer<TileEntityTurretBaseNT> opener) {
        clientScreenOpener = opener;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Level level = context.getLevel();
        BlockPos core = MultiblockSurface.coreOfAny(level, context.getClickedPos());
        if (core == null || !(level.getBlockEntity(core) instanceof TileEntityTurretBaseNT turret)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (level.isClientSide()) {
            Consumer<TileEntityTurretBaseNT> opener = clientScreenOpener;
            if (opener != null) opener.accept(turret);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.turret_mob_filter.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
