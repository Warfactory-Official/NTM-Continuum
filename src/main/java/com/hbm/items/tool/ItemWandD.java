package com.hbm.items.tool;

import com.hbm.Constants;
import com.hbm.config.GeneralConfig;
import com.hbm.main.StructureManager;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/**
 * 1.7 {@code ItemWandD} ({@code wand_d}, "Debug Wand"): Bob's scratch item. Its body is a graveyard of commented-out
 * experiments (safes, VNT explosions, Torex, the entity-tracker reflection hack, dungeons); the one live line
 * ray-traces 500 blocks, takes the column's height value and calls {@code StructureManager.crane.build(...)}, which
 * pastes the {@code structures/crane_mod.nbt} NBTStructure centred on that column.
 *
 * <p>The commented-out experiments are not ported: they are not behaviour. In particular the
 * {@code ReflectionHelper}/{@code EntityTracker} snippet is dead code upstream, so nothing here needs reflection.
 *
 * <p>The paste goes through the ported {@code NBTStructure} with 1.7's legacy palette mapping, centred on the column
 * with no rotation. Server-side only, like 1.7 ({@code if(world.isRemote) return stack}).
 */
public class ItemWandD extends CompatItem {

    public ItemWandD(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        // 1.7 Library.rayTrace(player, 500, 1, false, true, false): blocks only, fluids passed through
        HitResult hit = player.pick(500.0D, 1.0F, false);

        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            // 1.7 world.getHeightValue: the first block above the column's highest light-blocking block
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());

            if (GeneralConfig.logDiagnostics()) {
                Constants.LOG.info("[wand_d] crane build requested at {} {} {}", pos.getX(), y, pos.getZ());
            }
            StructureManager.crane.build(level, pos.getX(), y, pos.getZ());
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Used for debugging purposes."));
    }
}
