package com.hbm.util;

import com.hbm.particle.helper.HbmEffectNT;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * The server half of 1.7's {@code marker} aux particle: tells one player's client to outline a block
 * ({@code RenderOverhead.queuedMarkers}), optionally labelled, for {@code expires} ms (0 = until out of range) while the
 * player stays within {@code dist} blocks.
 */
public final class OverheadMarkers {

    private OverheadMarkers() {}

    public static void send(ServerPlayer player, BlockPos pos, int color, @Nullable String label, int expires, double dist) {
        CompoundTag data = new CompoundTag();
        data.putInt("color", color);
        data.putInt("expires", expires);
        data.putDouble("dist", dist);
        if (label != null) data.putString("label", label);
        HbmEffectNT.Marker.sendTo(player, pos.getX(), pos.getY(), pos.getZ(), data);
    }
}
