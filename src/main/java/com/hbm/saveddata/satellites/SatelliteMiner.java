package com.hbm.saveddata.satellites;

import com.hbm.itempool.ItemPool;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code SatelliteMiner}, the asteroid mining ship: every fifteen minutes of world time it fills its hold with
 * 10..15 draws from its item pool, which a cargo landing pad tuned to it calls down in a drop pod.
 */
public class SatelliteMiner extends Satellite {

    public static final MapCodec<SatelliteMiner> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.optionalFieldOf("progress", 0D).forGetter(sat -> sat.progress)
    ).apply(i, SatelliteMiner::new));

    /** Fraction of one load mined, 0..1. */
    public double progress;
    public static final double SPEED = 1D / (15 * 60 * 20);

    public SatelliteMiner() {
        this(0D);
    }

    protected SatelliteMiner(double progress) {
        this.progress = progress;
    }

    @Override
    public SatelliteType type() {
        return SatelliteType.MINER;
    }

    @Override public String getType() { return "ASTEROID_MINER"; }

    protected EnumSatType chip() {
        return EnumSatType.MINER_ASTRO;
    }

    @Override
    public Component[] getInfo(Level level) {
        int percent = (int) Math.round(this.progress * 100);
        return new Component[] {
                nameOf(chip()),
                Component.translatable("satellite.minerprogress", percent + "%")
        };
    }

    @Override
    public void onUpdateTick(ServerLevel level) {
        if (this.requestableSlots.length <= 0) {
            this.progress += SPEED;

            if (this.progress >= 1D) {
                this.progress = 0D;

                List<ItemPool.Entry> pool = ItemPool.getPool(getCargo());
                int itemAmount = 10 + level.getRandom().nextInt(6);
                this.requestableSlots = new ItemStack[itemAmount];

                for (int i = 0; i < itemAmount; i++) {
                    this.requestableSlots[i] = ItemPool.getStack(pool, level.getRandom());
                }

                this.markDirty();
            }

            // 1.7 forces a save every minute so the progress survives a crash
            if (level.getGameTime() % 1200 == 0) this.markDirty();
        }
    }

    /** 1.7 {@code SatelliteMiner.getCargoForItem}; null where the item launches no mining satellite. */
    public static @Nullable String getCargoForItem(ItemStack stack) {
        SatelliteType type = XSatelliteRegistry.typeFor(stack);
        return type == null ? null : type.cargo();
    }

    public @Nullable String getCargo() {
        return type().cargo();
    }
}
