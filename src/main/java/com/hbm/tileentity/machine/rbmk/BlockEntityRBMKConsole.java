package com.hbm.tileentity.machine.rbmk;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.MenuRBMKConsole;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Scans the linked 15 by 15 reactor grid and controls its columns. */
public class BlockEntityRBMKConsole extends TileEntityMachineBase implements IControlReceiver, MenuProvider, IRBMKTicking {

    public static final int FLUX_BUFFER = 60;
    private static final int[] EMPTY_INT = new int[0];

    public final RBMKColumn[] columns = new RBMKColumn[15 * 15];
    public final RBMKScreen[] screens = new RBMKScreen[6];
    public final int[] fluxBuffer = new int[FLUX_BUFFER];
    private int targetX;
    private int targetY;
    private int targetZ;
    private byte rotation;
    private int cadence;

    public BlockEntityRBMKConsole(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_CONSOLE.get(), pos, state, 0);
        for (int i = 0; i < screens.length; i++) {
            screens[i] = new RBMKScreen();
        }
    }

    @Override
    public String getDefaultName() {
        return "container.rbmk_console";
    }

    @Override
    public void tickServer() {
        if (++cadence % 10 != 0) {
            return;
        }
        rescan();
        prepareScreenInfo();
        networkPackNT();
    }

    private void rescan() {
        double flux = 0;
        for (int index = 0; index < columns.length; index++) {
            BlockEntity blockEntity = level.getBlockEntity(columnPos(index));
            if (blockEntity instanceof BlockEntityRBMKBase column) {
                columns[index] = column.getConsoleData();
                if (blockEntity instanceof BlockEntityRBMKRod rod) {
                    flux += rod.lastFluxQuantity;
                }
            } else {
                columns[index] = null;
            }
        }
        System.arraycopy(fluxBuffer, 1, fluxBuffer, 0, fluxBuffer.length - 1);
        fluxBuffer[fluxBuffer.length - 1] = (int) flux;
    }

    private void prepareScreenInfo() {
        for (RBMKScreen screen : screens) {
            if (screen.type == ScreenType.NONE) {
                screen.display = null;
                continue;
            }

            double value = 0;
            int count = 0;
            for (Integer index : screen.columns) {
                if (index < 0 || index >= columns.length || columns[index] == null) {
                    continue;
                }
                RBMKColumn column = columns[index];
                boolean fuel = column.type == RBMKColumnType.FUEL || column.type == RBMKColumnType.FUEL_SIM
                        || column.type == RBMKColumnType.BREEDER;
                switch (screen.type) {
                    case COL_TEMP -> {
                        count++;
                        value += column.heat;
                    }
                    case FUEL_DEPLETION -> {
                        if (fuel && ((RBMKColumn.FuelColumn) column).c_maxHeat > 0) {
                            count++;
                            value += 100D - ((RBMKColumn.FuelColumn) column).enrichment * 100D;
                        }
                    }
                    case FUEL_POISON -> {
                        if (fuel && ((RBMKColumn.FuelColumn) column).c_maxHeat > 0) {
                            count++;
                            value += ((RBMKColumn.FuelColumn) column).xenon;
                        }
                    }
                    case FUEL_TEMP -> {
                        if (fuel && ((RBMKColumn.FuelColumn) column).c_maxHeat > 0) {
                            count++;
                            value += ((RBMKColumn.FuelColumn) column).c_heat;
                        }
                    }
                    case ROD_EXTRACTION -> {
                        if (column.type == RBMKColumnType.CONTROL || column.type == RBMKColumnType.CONTROL_AUTO) {
                            count++;
                            value += ((RBMKColumn.ControlColumn) column).level * 100D;
                        }
                    }
                    default -> {
                    }
                }
            }

            double result = value / count;
            String text = ((int) (result * 10D)) / 10D + "";
            screen.display = switch (screen.type) {
                case COL_TEMP -> "rbmk.screen.temp=" + text + "\u00b0C";
                case FUEL_DEPLETION -> "rbmk.screen.depletion=" + text + "%";
                case FUEL_POISON -> "rbmk.screen.xenon=" + text + "%";
                case FUEL_TEMP -> "rbmk.screen.core=" + text + "\u00b0C";
                case ROD_EXTRACTION -> "rbmk.screen.rod=" + text + "%";
                default -> text;
            };
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 20D * 20D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        switch (Tags.getStringOr(data, "action", "")) {
            case "level" -> {
                double target = Tags.getDoubleOr(data, "level", 0D);
                for (int index : Tags.getIntArrayOr(data, "cols", EMPTY_INT)) {
                    if (columnAt(index) instanceof BlockEntityRBMKControl rod) {
                        rod.setTarget(Mth.clamp(target, 0D, 1D));
                        rod.setChanged();
                    }
                }
            }
            case "toggle" -> {
                int slot = Tags.getIntOr(data, "slot", 0);
                if (slot >= 0 && slot < screens.length) {
                    screens[slot].type = ScreenType.VALUES[(screens[slot].type.ordinal() + 1) % ScreenType.VALUES.length];
                }
            }
            case "assign" -> {
                int slot = Tags.getIntOr(data, "slot", 0);
                if (slot >= 0 && slot < screens.length) {
                    screens[slot].columns = boxed(Tags.getIntArrayOr(data, "cols", EMPTY_INT));
                }
            }
            case "color" -> {
                int color = Tags.getIntOr(data, "color", -1);
                for (int index : Tags.getIntArrayOr(data, "cols", EMPTY_INT)) {
                    if (columnAt(index) instanceof BlockEntityRBMKControlManual rod) {
                        rod.color = color < 0 || color >= RBMKColor.VALUES.length ? null : RBMKColor.VALUES[color];
                        rod.setChanged();
                    }
                }
            }
            case "compressor" -> {
                for (int index : Tags.getIntArrayOr(data, "cols", EMPTY_INT)) {
                    if (columnAt(index) instanceof BlockEntityRBMKBoiler boiler) {
                        boiler.cycleCompressor();
                    }
                }
            }
            default -> {
            }
        }
    }

    private @Nullable BlockEntity columnAt(int index) {
        return index < 0 || index >= columns.length ? null : level.getBlockEntity(columnPos(index));
    }

    private BlockPos columnPos(int index) {
        return new BlockPos(targetX + getXFromIndex(index), targetY, targetZ + getZFromIndex(index));
    }

    private static Integer[] boxed(int[] values) {
        Integer[] result = new Integer[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }

    public void setTarget(int x, int y, int z) {
        targetX = x;
        targetY = y;
        targetZ = z;
        setChanged();
    }

    public void rotate() {
        rotation = (byte) ((rotation + 1) % 4);
        setChanged();
    }

    public int targetX() { return targetX; }
    public int targetY() { return targetY; }
    public int targetZ() { return targetZ; }
    public byte rotation() { return rotation; }

    public int getXFromIndex(int index) {
        int x = index % 15 - 7;
        int z = index / 15 - 7;
        return switch (rotation) {
            case 1 -> -z;
            case 2 -> -x;
            case 3 -> z;
            default -> x;
        };
    }

    public int getZFromIndex(int index) {
        int x = index % 15 - 7;
        int z = index / 15 - 7;
        return switch (rotation) {
            case 1 -> x;
            case 2 -> -z;
            case 3 -> -x;
            default -> z;
        };
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        targetX = input.getIntOr("tX", 0);
        targetY = input.getIntOr("tY", 0);
        targetZ = input.getIntOr("tZ", 0);
        rotation = (byte) input.getIntOr("rotation", 0);
        for (int i = 0; i < screens.length; i++) {
            screens[i].type = com.hbm.util.SavedIds.getEnum(input, "t" + i, ScreenType.VALUES[0]);
            screens[i].columns = boxed(input.getIntArrayOr("s" + i, EMPTY_INT));
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("tX", targetX);
        output.putInt("tY", targetY);
        output.putInt("tZ", targetZ);
        output.putInt("rotation", rotation);
        for (int i = 0; i < screens.length; i++) {
            com.hbm.util.SavedIds.putEnum(output, "t" + i, screens[i].type); // by name, never an ordinal
            int[] values = new int[screens[i].columns.length];
            for (int j = 0; j < values.length; j++) {
                values[j] = screens[i].columns[j];
            }
            output.putIntArray("s" + i, values);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        for (RBMKColumn column : columns) {
            RBMKColumn.writeToBuf(buffer, column);
        }
        for (int flux : fluxBuffer) {
            buffer.writeInt(flux);
        }
        for (RBMKScreen screen : screens) {
            buffer.writeByte(screen.type.ordinal());
        }
        int mask = 0;
        for (int i = 0; i < screens.length; i++) {
            if (screens[i].display != null && !screens[i].display.isEmpty()) {
                mask |= 1 << i;
            }
        }
        buffer.writeByte(mask);
        for (int i = 0; i < screens.length; i++) {
            if ((mask & (1 << i)) != 0) {
                buffer.writeUtf(screens[i].display);
            }
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        for (int i = 0; i < columns.length; i++) {
            columns[i] = RBMKColumn.readFromBuf(buffer);
        }
        for (int i = 0; i < fluxBuffer.length; i++) {
            fluxBuffer[i] = buffer.readInt();
        }
        for (RBMKScreen screen : screens) {
            screen.type = ScreenType.VALUES[buffer.readByte()];
        }
        int mask = buffer.readUnsignedByte();
        for (int i = 0; i < screens.length; i++) {
            screens[i].display = (mask & (1 << i)) != 0 ? buffer.readUtf() : null;
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.rbmk_console");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MenuRBMKConsole(id, inventory, this);
    }

    public enum ScreenType {
        NONE(0), COL_TEMP(18), ROD_EXTRACTION(36), FUEL_DEPLETION(54), FUEL_POISON(72), FUEL_TEMP(90);

        public static final ScreenType[] VALUES = values();
        public final int offset;

        ScreenType(int offset) {
            this.offset = offset;
        }
    }

    public static final class RBMKScreen {
        public ScreenType type = ScreenType.NONE;
        public Integer[] columns = new Integer[0];
        public @Nullable String display;
    }
}
