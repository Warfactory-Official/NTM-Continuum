package com.hbm.saveddata.satellites;

import com.hbm.entity.ModNtmEntities;
import com.hbm.advancement.HbmCriteria;
import com.hbm.entity.projectile.EntityTom;
import com.hbm.saveddata.SatelliteSavedData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public class SatelliteHorizons extends Satellite {

    public static final MapCodec<SatelliteHorizons> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.optionalFieldOf("used", false).forGetter(sat -> sat.used)
    ).apply(i, SatelliteHorizons::new));

    public boolean used;

    public SatelliteHorizons() {
        this(false);
    }

    private SatelliteHorizons(boolean used) {
        super(Interfaces.SAT_COORD, Set.of(), Set.of());
        this.used = used;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.HORIZONS;
    }

    @Override
    public void onOrbit(ServerLevel level, double x, double y, double z) {
        ItemStack chip = new ItemStack(type().item());
        for (ServerPlayer player : level.players()) HbmCriteria.orbit(player, chip);
    }

    @Override
    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
        if (used) return;

        used = true;
        SatelliteSavedData.get(level).setDirty();

        EntityTom tom = new EntityTom(ModNtmEntities.TOM_THE_MOONSTONE.get(), level);
        tom.setPos(x + 0.5D, 600, z + 0.5D);
        tom.anchorAndLoad(level, x >> 4, z >> 4);
        level.addFreshEntity(tom);

        ItemStack chip = new ItemStack(type().item());
        for (ServerPlayer p : level.players()) HbmCriteria.satelliteAction(p, chip);

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("chat.satelliteHorizons.horizonsHasBeenActivated")
                        .withStyle(ChatFormatting.RED), false);
    }
}
