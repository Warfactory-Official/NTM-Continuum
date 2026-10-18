package com.hbm.saveddata.satellites;

import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.projectile.EntityTom;
import com.hbm.items.ModSatelliteItems;
import com.hbm.saveddata.SatelliteSavedData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Locale;

/**
 * 1.7 {@code SatelliteHorizons}, launched from {@code sat_gerald}: single use, it drops Tom the Moonstone
 * ({@link EntityTom}) from y=600 onto the target.
 */
public class SatelliteHorizons extends Satellite {

    public static final MapCodec<SatelliteHorizons> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("used", false).forGetter(sat -> sat.used)
    ).apply(i, SatelliteHorizons::new));

    public static final String CMD_FIRE = "fire";
    /**
     * 1.7 spells this constant "settarget", not "canfire": the readiness query shares its word with the base
     * target command, so a {@code settarget} both moves the target and answers whether Horizons is unused.
     */
    public static final String CMD_CANFIRE = "settarget";

    public boolean used;

    public SatelliteHorizons() {
        this(false);
    }

    private SatelliteHorizons(boolean used) {
        this.used = used;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.HORIZONS;
    }

    @Override public String getType() { return "PAYLOAD_UNKNOWN"; }

    @Override
    public Component[] getInfo(Level level) {
        return new Component[] {
                Component.translatable(ModSatelliteItems.sat_gerald.get().getDescriptionId()),
                used ? Component.translatable("satellite.spent") : Component.translatable("satellite.ready")
        };
    }

    /** 1.7 writeToNBT/readFromNBT store only {@code used}; the base target is not saved. */
    @Override
    public boolean savesBaseData() {
        return false;
    }

    @Override
    public void onOrbit(ServerLevel level, double x, double y, double z) {
        super.onOrbit(level, x, y, z);
        // 1.7 horizonsStart, awarded to every player in the world
        ItemStack chip = XSatelliteRegistry.stackFor(type());
        for (ServerPlayer player : level.players()) HbmCriteria.orbit(player, chip);
    }

    @Override
    public void onCommandImpl(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_FIRE)) {
            if (level instanceof ServerLevel server) theHorizons(server, targetX, targetZ);
            return;
        }

        if (cmd[0].equals(CMD_CANFIRE)) {
            this.tx = ("" + (!used)).toUpperCase(Locale.US);
        }
    }

    @Override
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        this.setTarget(x, z);
        this.theHorizons(level, x, z);
    }

    public void theHorizons(ServerLevel level, int x, int z) {
        if (used) return;

        used = true;
        SatelliteSavedData.get(level, new BlockPos(x, 0, z)).setDirty();

        EntityTom tom = new EntityTom(ModNtmEntities.TOM_THE_MOONSTONE.get(), level);
        tom.setPos(x + 0.5D, 600, z + 0.5D);
        // 1.7 provider.loadChunk(x >> 4, z >> 4)
        tom.anchorAndLoad(level, x >> 4, z >> 4);
        level.addFreshEntity(tom);

        // 1.7 horizonsEnd, awarded to every player in the world
        ItemStack chip = XSatelliteRegistry.stackFor(type());
        for (ServerPlayer p : level.players()) HbmCriteria.satelliteAction(p, chip);

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("Horizons has been activated.").withStyle(ChatFormatting.RED), false);
    }
}
