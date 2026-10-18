package com.hbm.saveddata.satellites;

import com.hbm.inventory.recipes.SpaceAssemblerRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModSatelliteItems;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.machine.ItemSatellite.EnumSatType;
import com.hbm.util.BobMathUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code SatelliteScience}, the space laboratory: flight data for an empty disk every fifteen minutes, orbital
 * data once its sensor relays have measured for 100 hours between them, and 0G assemblers that finish orbital
 * assemblies sent up on its frequency, first in first out, into cargo a pad can call down.
 */
public class SatelliteScience extends Satellite {

    // 1.7 writes assemblerProgress but never reads it back, so a reload restarts the current task
    public static final MapCodec<SatelliteScience> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("lastScience", 0L).forGetter(sat -> sat.lastScience),
            Codec.INT.optionalFieldOf("sensorProgress", 0).forGetter(sat -> sat.sensorProgress),
            Codec.INT.optionalFieldOf("sensorCount", 0).forGetter(sat -> sat.sensorCount),
            Codec.INT.optionalFieldOf("assemblerCount", 0).forGetter(sat -> sat.assemblerCount),
            Codec.DOUBLE.optionalFieldOf("assemblerProgress", 0D).forGetter(sat -> sat.assemblerProgress),
            Codec.STRING.listOf().optionalFieldOf("tasks", List.of()).forGetter(SatelliteScience::taskNames)
    ).apply(i, (lastScience, sensorProgress, sensorCount, assemblerCount, assemblerProgress, tasks) -> {
        SatelliteScience sat = new SatelliteScience();
        sat.lastScience = lastScience;
        sat.sensorProgress = sensorProgress;
        sat.sensorCount = sensorCount;
        sat.assemblerCount = assemblerCount;
        for (String task : tasks) sat.assemblerTasks.add(new AssemblerTask(task));
        return sat;
    }));

    public static final int COOLDOWN = 15 * 60 * 20;
    public long lastScience;

    /** Sensor-relay ticks for one orbital data set; each relay adds one per tick. */
    public static final int SENSOR_DURATION = 100 * 60 * 60 * 20;
    public int sensorProgress;
    public int sensorCount;

    public int assemblerCount;
    public double assemblerProgress;

    /** First in, first out. */
    public final List<AssemblerTask> assemblerTasks = new ArrayList<>();

    @Override
    public SatelliteType type() {
        return SatelliteType.SCIENCE;
    }

    @Override public String getType() { return "SCIENCE_PROBE"; }

    private List<String> taskNames() {
        List<String> names = new ArrayList<>(assemblerTasks.size());
        for (AssemblerTask task : assemblerTasks) names.add(task.recipe);
        return names;
    }

    @Override
    public boolean hasData(Level level) {
        if (super.hasData(level)) return true;

        if (level.getGameTime() > this.lastScience + COOLDOWN) {
            this.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_FLIGHTDATA);
            this.lastScience = level.getGameTime();
            this.markDirty();
        }

        return super.hasData(level);
    }

    @Override
    public void onPartDelivered(ServerLevel level, ItemStack part) {
        EnumSatType chip = ModSatelliteItems.satellite.typeOf(part);

        if (chip != null) {
            if (chip == EnumSatType.SCIENCE_SENSOR) this.sensorCount++;
            if (chip == EnumSatType.SCIENCE_ASSEMBLER) this.assemblerCount++;
            this.markDirty();
            return;
        }

        GenericRecipe recipe = SpaceAssemblerRecipes.INSTANCE.getRecipe(part);

        if (recipe != null) {
            this.assemblerTasks.add(new AssemblerTask(recipe));
            this.markDirty();
        }
    }

    @Override
    public void onUpdateTick(ServerLevel level) {

        if (this.sensorProgress < SENSOR_DURATION) {
            if (this.sensorCount > 0) {
                this.sensorProgress += this.sensorCount;
                this.markDirty();
            }
        } else {
            this.sensorProgress = 0;
            this.produceData(EnumDriveType.DISK_EMPTY, EnumDriveType.DISK_ORBITDATA);
            this.markDirty();
        }

        if (this.assemblerCount > 0 && this.requestableSlots.length <= 0 && !this.assemblerTasks.isEmpty()) {

            AssemblerTask task = this.assemblerTasks.get(0);
            this.assemblerProgress += (double) this.assemblerCount / task.duration;

            if (this.assemblerProgress >= 1) {

                GenericRecipe recipe = SpaceAssemblerRecipes.INSTANCE.byName(task.recipe);

                if (recipe != null) {
                    this.requestableSlots = new ItemStack[recipe.outputItem.length];
                    for (int i = 0; i < recipe.outputItem.length; i++) {
                        this.requestableSlots[i] = recipe.outputItem[i].copy();
                    }
                }

                this.assemblerProgress = 0;
                this.assemblerTasks.remove(0);
                this.markDirty();
            }
        }
    }

    @Override
    public Component[] getInfo(Level level) {
        int cooldown = (int) ((lastScience + COOLDOWN) - level.getGameTime());
        int seconds = cooldown / 20;

        return new Component[] {
                nameOf(EnumSatType.SCIENCE),
                cooldown <= 0 ? Component.translatable("satellite.ready")
                        : Component.translatable("satellite.cooldown", (seconds / 60) + "m" + (seconds % 60) + "s"),

                this.sensorCount > 0 ? Component.translatable("satellite.sensors", this.sensorCount) : null,
                this.sensorCount > 0 ? Component.translatable("satellite.pending", BobMathUtil.format(SENSOR_DURATION - sensorProgress)) : null,
                this.driveOutput == EnumDriveType.DISK_ORBITDATA ? Component.translatable("satellite.data") : null,

                this.assemblerCount > 0 ? Component.translatable("satellite.assemblers", this.assemblerCount) : null,
                this.assemblerCount > 0 ? Component.translatable("satellite.progress", (int) Math.round(this.assemblerProgress * 100) + "%") : null,
                this.assemblerCount > 0 ? Component.translatable("satellite.queue", this.assemblerTasks.size()) : null,
        };
    }

    public static final class AssemblerTask {

        public final String recipe;
        public final int duration;
        public final ItemStack icon;

        public AssemblerTask(@Nullable GenericRecipe recipe) {
            if (recipe != null) {
                this.recipe = recipe.getInternalName();
                this.duration = recipe.duration;
                this.icon = recipe.getIcon();
            } else {
                this.recipe = "null";
                this.duration = 1;
                this.icon = ItemStack.EMPTY;
            }
        }

        public AssemblerTask(String name) {
            this(SpaceAssemblerRecipes.INSTANCE.byName(name));
        }
    }
}
