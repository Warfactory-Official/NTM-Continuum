package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Strictly-typed column snapshot for the console/crane display; fluid types travel as registry ids.
 */
public abstract class RBMKColumn {

    public final RBMKColumnType type;
    public double heat;
    public double maxHeat;
    public boolean moderated;
    public int reasimWater;
    public int reasimSteam;
    public int indicator;

    protected RBMKColumn(RBMKColumnType type) {
        this.type = type;
    }

    protected static Component fluidLine(ChatFormatting color, int fluidId, int fill, int max) {
        return Component.literal(fluidName(fluidId) + " " + fill + "/" + max + "mB").withStyle(color);
    }

    protected static String fluidName(int fluidId) {
        if (fluidId < 0) return "";
        return Fluids.byIndex(fluidId).getDisplayName().getString();
    }

    /**
     * A tank's fluid type as its registry id for the DTO (-1 = empty).
     */
    public static int fluidId(FluidType fluid) {
        return fluid == null ? -1 : fluid.index();
    }

    public static RBMKColumn readFromBuf(ByteBuf buf) {
        byte ordinal = buf.readByte();
        if (ordinal == -1) return null;
        RBMKColumn column = createForType(RBMKColumnType.values()[ordinal]);
        column.deserialize(buf);
        return column;
    }

    public static void writeToBuf(ByteBuf buf, RBMKColumn column) {
        if (column == null) {
            buf.writeByte(-1);
        } else {
            buf.writeByte((byte) column.type.ordinal());
            column.serialize(buf);
        }
    }

    public static RBMKColumn createForType(RBMKColumnType type) {
        return switch (type) {
            case FUEL, FUEL_SIM, BREEDER -> new FuelColumn(type);
            case BOILER -> new BoilerColumn();
            case CONTROL, CONTROL_AUTO -> new ControlColumn(type);
            case COOLER -> new CoolerColumn();
            case OUTGASSER -> new OutgasserColumn();
            case HEATEX -> new HeaterColumn();
            default -> new StandardColumn(type);
        };
    }

    protected void serialize(ByteBuf buf) {
        buf.writeDouble(heat);
        buf.writeDouble(maxHeat);
        buf.writeBoolean(moderated);
        buf.writeInt(reasimWater);
        buf.writeInt(reasimSteam);
        buf.writeByte(indicator);
    }

    protected void deserialize(ByteBuf buf) {
        heat = buf.readDouble();
        maxHeat = buf.readDouble();
        moderated = buf.readBoolean();
        reasimWater = buf.readInt();
        reasimSteam = buf.readInt();
        indicator = buf.readByte();
    }

    /**
     * Hover tooltip lines for this column.
     */
    public List<Component> getFancyStats() {
        List<Component> stats = new ArrayList<>();
        stats.add(Component.translatable("rbmk.heat", ((int) (heat * 10D)) / 10D + "°C")
                           .withStyle(ChatFormatting.YELLOW));
        if (moderated) stats.add(Component.translatable("rbmk.moderated").withStyle(ChatFormatting.YELLOW));
        return stats;
    }

    public static class StandardColumn extends RBMKColumn {
        public StandardColumn(RBMKColumnType type) {
            super(type);
        }
    }

    public static class FuelColumn extends RBMKColumn {
        public double enrichment;
        public double xenon;
        public double c_coreHeat;
        public double c_heat;
        public double c_maxHeat;

        public FuelColumn(RBMKColumnType type) {
            super(type);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeDouble(enrichment);
            buf.writeDouble(xenon);
            buf.writeDouble(c_coreHeat);
            buf.writeDouble(c_heat);
            buf.writeDouble(c_maxHeat);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            enrichment = buf.readDouble();
            xenon = buf.readDouble();
            c_coreHeat = buf.readDouble();
            c_heat = buf.readDouble();
            c_maxHeat = buf.readDouble();
        }

        @Override
        public List<Component> getFancyStats() {
            List<Component> stats = super.getFancyStats();
            stats.add(Component.translatable("rbmk.rod.depletion", ((int) (((1D - enrichment) * 100000)) / 1000D) + "%")
                               .withStyle(ChatFormatting.GREEN));
            stats.add(Component.translatable("rbmk.rod.xenon", ((int) ((xenon * 1000D)) / 1000D) + "%")
                               .withStyle(ChatFormatting.DARK_PURPLE));
            stats.add(Component.translatable("rbmk.rod.coreTemp", ((int) (c_coreHeat * 10D)) / 10D + "°C")
                               .withStyle(ChatFormatting.DARK_RED));
            stats.add(Component.translatable("rbmk.rod.skinTemp", ((int) (c_heat * 10D)) / 10D + "°C",
                    ((int) (c_maxHeat * 10D)) / 10D + "°C").withStyle(ChatFormatting.RED));
            return stats;
        }
    }

    public static class BoilerColumn extends RBMKColumn {
        public int water;
        public int maxWater;
        public int steam;
        public int maxSteam;
        public int steamType = -1;

        public BoilerColumn() {
            super(RBMKColumnType.BOILER);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(water);
            buf.writeInt(maxWater);
            buf.writeInt(steam);
            buf.writeInt(maxSteam);
            buf.writeInt(steamType);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            water = buf.readInt();
            maxWater = buf.readInt();
            steam = buf.readInt();
            maxSteam = buf.readInt();
            steamType = buf.readInt();
        }

        @Override
        public List<Component> getFancyStats() {
            List<Component> stats = super.getFancyStats();
            stats.add(Component.translatable("rbmk.boiler.water", water, maxWater).withStyle(ChatFormatting.BLUE));
            stats.add(Component.translatable("rbmk.boiler.steam", steam, maxSteam).withStyle(ChatFormatting.WHITE));
            stats.add(
                    Component.translatable("rbmk.boiler.type", fluidName(steamType)).withStyle(ChatFormatting.YELLOW));
            return stats;
        }
    }

    public static class ControlColumn extends RBMKColumn {
        public double level;
        public short color = -1; // -1 = undyed

        public ControlColumn(RBMKColumnType type) {
            super(type);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeDouble(level);
            buf.writeShort(color);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            level = buf.readDouble();
            color = buf.readShort();
        }

        @Override
        public List<Component> getFancyStats() {
            List<Component> stats = super.getFancyStats();
            if (color >= 0 && color < RBMKColor.VALUES.length) {
                stats.add(
                        Component.translatable("rbmk.control." + RBMKColor.VALUES[color].name().toLowerCase(Locale.US))
                                 .withStyle(ChatFormatting.YELLOW));
            }
            stats.add(Component.translatable("rbmk.control.level", ((int) (level * 100D)) + "%")
                               .withStyle(ChatFormatting.YELLOW));
            return stats;
        }
    }

    public static class CoolerColumn extends RBMKColumn {
        public int cooled;
        public int cryo;
        public int maxCryo;
        public int hot;
        public int maxHot;
        public int coldType = -1;
        public int hotType = -1;

        public CoolerColumn() {
            super(RBMKColumnType.COOLER);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(cooled);
            buf.writeInt(cryo);
            buf.writeInt(maxCryo);
            buf.writeInt(hot);
            buf.writeInt(maxHot);
            buf.writeInt(coldType);
            buf.writeInt(hotType);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            cooled = buf.readInt();
            cryo = buf.readInt();
            maxCryo = buf.readInt();
            hot = buf.readInt();
            maxHot = buf.readInt();
            coldType = buf.readInt();
            hotType = buf.readInt();
        }

        @Override
        public List<Component> getFancyStats() {
            List<Component> stats = super.getFancyStats();
            stats.add(Component.translatable("rbmk.cooler.cooling", cooled * SharedConstants.TICKS_PER_SECOND)
                               .withStyle(ChatFormatting.AQUA));
            stats.add(fluidLine(ChatFormatting.BLUE, coldType, cryo, maxCryo));
            stats.add(fluidLine(ChatFormatting.RED, hotType, hot, maxHot));
            return stats;
        }
    }

    public static class OutgasserColumn extends RBMKColumn {
        public int gas;
        public int maxGas;
        public double progress;
        public double maxProgress;
        public double usedFlux;

        public OutgasserColumn() {
            super(RBMKColumnType.OUTGASSER);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(gas);
            buf.writeInt(maxGas);
            buf.writeDouble(progress);
            buf.writeDouble(maxProgress);
            buf.writeDouble(usedFlux);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            gas = buf.readInt();
            maxGas = buf.readInt();
            progress = buf.readDouble();
            maxProgress = buf.readDouble();
            usedFlux = buf.readDouble();
        }

        @Override
        public List<Component> getFancyStats() {
            List<Component> stats = super.getFancyStats();
            int pct = maxProgress > 0 ? (int) (progress / maxProgress * 100D) : 0;
            int etaSeconds = usedFlux > 0 ? (int) ((maxProgress - progress) / usedFlux / 20D) : 0;
            stats.add(Component.translatable("rbmk.outgasser.eta", etaSeconds).withStyle(ChatFormatting.GOLD));
            stats.add(Component.translatable("rbmk.outgasser.flux", (long) usedFlux).withStyle(ChatFormatting.AQUA));
            stats.add(Component.translatable("rbmk.outgasser.progress", (long) progress, (long) maxProgress, pct + "%")
                               .withStyle(ChatFormatting.DARK_AQUA));
            stats.add(Component.translatable("rbmk.outgasser.gas", gas, maxGas).withStyle(ChatFormatting.YELLOW));
            return stats;
        }
    }

    public static class HeaterColumn extends RBMKColumn {
        public int water;
        public int maxWater;
        public int steam;
        public int maxSteam;
        public int coldType = -1;
        public int hotType = -1;

        public HeaterColumn() {
            super(RBMKColumnType.HEATEX);
        }

        @Override
        protected void serialize(ByteBuf buf) {
            super.serialize(buf);
            buf.writeInt(water);
            buf.writeInt(maxWater);
            buf.writeInt(steam);
            buf.writeInt(maxSteam);
            buf.writeInt(coldType);
            buf.writeInt(hotType);
        }

        @Override
        protected void deserialize(ByteBuf buf) {
            super.deserialize(buf);
            water = buf.readInt();
            maxWater = buf.readInt();
            steam = buf.readInt();
            maxSteam = buf.readInt();
            coldType = buf.readInt();
            hotType = buf.readInt();
        }

        @Override
        public List<Component> getFancyStats() {
            List<Component> stats = super.getFancyStats();
            stats.add(fluidLine(ChatFormatting.BLUE, coldType, water, maxWater));
            stats.add(fluidLine(ChatFormatting.RED, hotType, steam, maxSteam));
            return stats;
        }
    }
}
