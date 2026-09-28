package com.hbm.items.tool;

import com.hbm.items.machine.ItemSatChip;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.Satellite;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1.7 {@code ItemSatDesignator}, the "Satellite Laser Designator": right-click traces 300 blocks and hands the
 * cell in front of the face that was hit to the satellite on the chip's frequency as a coordinate action.
 */
public class ItemSatDesignator extends ItemSatChip {

    public ItemSatDesignator(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level instanceof ServerLevel server) {
            Satellite sat = SatelliteSavedData.get(server).getSatFromFreq(this.getFreq(stack));

            if (sat != null) {
                // 1.7 Library.rayTrace(player, 300, 1): blocks only, and a MOP even on a miss (the last cell
                // the ray reached), so a shot into the sky still designates the far end of the trace
                HitResult hit = player.pick(300.0D, 1.0F, false);
                if (hit instanceof BlockHitResult blockHit) {
                    BlockPos pos = blockHit.getBlockPos().relative(blockHit.getDirection());
                    sat.onCoordAction(server, player, pos.getX(), pos.getY(), pos.getZ());
                    SatelliteSavedData.get(server).setDirty();
                }
            }
        }

        return InteractionResult.SUCCESS;
    }
}
