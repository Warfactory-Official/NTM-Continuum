package com.hbm.compat.computercraft.rbmk;

//? if computercraft {
import com.hbm.compat.computercraft.OpenComputers;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlAuto;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKCooler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKHeater;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKOutgasser;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKStorage;
import com.hbm.tileentity.machine.rbmk.RBMKColor;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKColumnType;
import dan200.computercraft.api.lua.LuaFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 1.7 {@code TileEntityRBMKConsole}'s component {@code rbmk_console}. Grid coordinates are the console's 0..14 cells;
 * like 1.7, a column is looked up unrotated at the target while {@code getColumnData} reads the scan copy at the
 * rotated index.
 */
public final class RBMKConsolePeripheral extends SnapshotPeripheral<BlockEntityRBMKConsole> {

    private static final int TARGET_X = 0;
    private static final int TARGET_Y = 1;
    private static final int TARGET_Z = 2;

    public RBMKConsolePeripheral(BlockEntityRBMKConsole machine) {
        super(machine, "rbmk_console", 3, 0);
    }

    @Override
    protected void capture(BlockEntityRBMKConsole machine) {
        put(TARGET_X, machine.targetX());
        put(TARGET_Y, machine.targetY());
        put(TARGET_Z, machine.targetZ());
    }

    /** 1.7 {@code Fluids.fromID(id).getUnlocalizedName()}. */
    private static String unlocalized(int legacyId) {
        return Fluids.byLegacyId(legacyId).getTranslationKey();
    }

    private static String itemName(ItemStack stack) {
        // the item's description id stands in for 1.7's getUnlocalizedName
        return stack.isEmpty() ? "" : stack.getItem().getDescriptionId();
    }

    /** 1.7 {@code Compat.getTileStandard}: the block entity at an offset from the target, never loading a chunk. */
    private @Nullable BlockEntity columnAt(int dx, int dz) {
        BlockEntityRBMKConsole console = machine();
        Level level = console.getLevel();
        BlockPos pos = new BlockPos(console.targetX() + dx, console.targetY(), console.targetZ() + dz);
        return level == null || !level.hasChunkAt(pos) ? null : level.getBlockEntity(pos);
    }

    @LuaFunction(mainThread = true)
    public final Object[] getColumnData(double gridX, double gridY) {
        BlockEntityRBMKConsole console = machine();
        int x = (int) gridX - 7;
        int y = -(int) gridY + 7;
        int i = x;
        int j = y;
        switch (console.rotation()) {
            case 1 -> {
                i = y;
                j = -x;
            }
            case 2 -> {
                i = -x;
                j = -y;
            }
            case 3 -> {
                i = -y;
                j = x;
            }
            default -> {
            }
        }
        int index = (j + 7) * 15 + (i + 7);

        if (!(columnAt(x, y) instanceof BlockEntityRBMKBase column)) return new Object[]{null};
        // 1.7 :602 reads the scan's copy: absent before the console's next 10-tick rescan, or outside the grid
        RBMKColumn cached = index >= 0 && index < console.columns.length ? console.columns[index] : null;
        if (cached == null) return OpenComputers.unknownError();

        // 1.7 read each value from the scan's NBT, where a key the column type does not write reads as 0
        RBMKColumn.ControlColumn control = cached instanceof RBMKColumn.ControlColumn c ? c : null;
        RBMKColumn.FuelColumn fuel = cached instanceof RBMKColumn.FuelColumn f ? f : null;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", column.getConsoleType().name());
        data.put("hullTemp", cached.heat);
        data.put("realSimWater", column.reasimWater);
        data.put("realSimSteam", column.reasimSteam);
        data.put("moderated", cached.moderated);
        data.put("level", control == null ? 0D : control.level);
        // the port's scan copy does not carry targetLevel; the live value stands in (at most ten ticks newer)
        data.put("targetLevel", control != null && column instanceof BlockEntityRBMKControl rod ? rod.targetLevel : 0D);
        // only the manual rod wrote "color" (-1 undyed)
        data.put("color", control != null && cached.type == RBMKColumnType.CONTROL ? control.color : (short) 0);
        data.put("enrichment", fuel == null ? 0D : fuel.enrichment);
        data.put("xenon", fuel == null ? 0D : fuel.xenon);
        data.put("coreSkinTemp", fuel == null ? 0D : fuel.c_heat);
        data.put("coreTemp", fuel == null ? 0D : fuel.c_coreHeat);
        data.put("coreMaxTemp", fuel == null ? 0D : fuel.c_maxHeat);

        if (column instanceof BlockEntityRBMKControlAuto auto) {
            data.put("function", auto.function.toString());
            data.put("heatUpper", auto.heatUpper);
            data.put("heatLower", auto.heatLower);
            data.put("levelUpper", auto.levelUpper);
            data.put("levelLower", auto.levelLower);
        }
        if (column instanceof BlockEntityRBMKRod rod) {
            data.put("fluxQuantity", rod.lastFluxQuantity);
            data.put("fluxRatio", rod.fluxFastRatio);
            data.put("rodName", itemName(rod.getItem(0)));
        }
        if (column instanceof BlockEntityRBMKBoiler boiler) {
            data.put("water", boiler.feed.getFill());
            data.put("steam", boiler.steam.getFill());
            data.put("steamType", unlocalized(cached instanceof RBMKColumn.BoilerColumn b ? b.steamType : 0));
        }
        if (column instanceof BlockEntityRBMKOutgasser outgasser) {
            data.put("fluxProgress", outgasser.progress);
            data.put("requiredFlux", outgasser.duration);
            ItemStack input = outgasser.getItem(BlockEntityRBMKOutgasser.SLOT_INPUT);
            data.put("craftingName", itemName(input));
            data.put("craftingNumber", input.isEmpty() ? 0 : input.getCount());
        }
        if (column instanceof BlockEntityRBMKHeater heater) {
            RBMKColumn.HeaterColumn h = cached instanceof RBMKColumn.HeaterColumn c ? c : null;
            data.put("coolant", heater.feed.getFill());
            data.put("hotcoolant", heater.steam.getFill());
            data.put("coldtype", unlocalized(h == null ? 0 : h.coldType));
            data.put("hottype", unlocalized(h == null ? 0 : h.hotType));
        }
        if (column instanceof BlockEntityRBMKCooler cooler) {
            data.put("cryogel", cooler.cold.getFill());
        }
        if (column instanceof BlockEntityRBMKStorage storage) {
            for (int k = 0; k < 12; k++) {
                ItemStack loaded = storage.getItem(k);
                if (loaded.getItem() instanceof ItemRBMKRod) {
                    data.put("slot" + k + "coreSkinTemp", ItemRBMKRod.getHullHeat(loaded));
                    data.put("slot" + k + "coreTemp", ItemRBMKRod.getCoreHeat(loaded));
                    data.put("slot" + k + "enrichment", ItemRBMKRod.getEnrichment(loaded));
                    data.put("slot" + k + "xenon", ItemRBMKRod.getPoisonLevel(loaded));
                    data.put("slot" + k + "rodName", loaded.getItem().getDescriptionId());
                }
            }
        }
        return new Object[]{data};
    }

    @LuaFunction
    public final Object[] getRBMKPos() {
        return read(s -> {
            if (s.intAt(TARGET_X) == 0 && s.intAt(TARGET_Y) == 0 && s.intAt(TARGET_Z) == 0) return new Object[]{null};
            Map<String, Integer> data = new LinkedHashMap<>();
            data.put("rbmkCenterX", s.intAt(TARGET_X));
            data.put("rbmkCenterY", s.intAt(TARGET_Y));
            data.put("rbmkCenterZ", s.intAt(TARGET_Z));
            return new Object[]{data};
        });
    }

    /** 1.7 sets {@code startingLevel = level} before {@code setTarget}; the manual rod's setTarget does both. */
    private static boolean retarget(@Nullable BlockEntity be, double level) {
        if (!(be instanceof BlockEntityRBMKControlManual rod)) return false;
        rod.setTarget(Math.min(1, Math.max(0, level)));
        rod.setChanged();
        return true;
    }

    @LuaFunction(mainThread = true)
    public final Object[] setLevel(double level) {
        boolean found = false;
        for (int i = -7; i <= 7; i++) {
            for (int j = -7; j <= 7; j++) {
                found |= retarget(columnAt(i, j), level);
            }
        }
        return found ? new Object[]{} : new Object[]{"No control rods found"};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setColumnLevel(double gridX, double gridY, double level) {
        int x = (int) gridX - 7;
        int y = -(int) gridY + 7;
        if (retarget(columnAt(x, y), level)) return new Object[]{};
        return new Object[]{"No control rod found at " + (x + 7) + "," + (7 - y)};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setColorLevel(double colorArg, double level) {
        int color = (int) colorArg;
        if (color < 0 || color > 4) return new Object[]{"Color " + color + " does not exist"};
        boolean found = false;
        for (int i = -7; i <= 7; i++) {
            for (int j = -7; j <= 7; j++) {
                if (columnAt(i, j) instanceof BlockEntityRBMKControlManual rod && rod.color == RBMKColor.VALUES[color]) {
                    found |= retarget(rod, level);
                }
            }
        }
        return found ? new Object[]{} : new Object[]{"No rods for color " + color + " found"};
    }

    @LuaFunction(mainThread = true)
    public final Object[] setColor(double gridX, double gridY, double colorArg) {
        int x = (int) gridX - 7;
        int y = -(int) gridY + 7;
        int color = (int) colorArg;
        if (color < 0 || color > 4) return new Object[]{"Color " + color + " does not exist"};
        if (!(columnAt(x, y) instanceof BlockEntityRBMKControlManual rod)) {
            return new Object[]{"No control rod found at " + (x + 7) + "," + (7 - y)};
        }
        rod.color = RBMKColor.VALUES[color];
        rod.setChanged();
        return new Object[]{};
    }

    @LuaFunction(mainThread = true)
    public final Object[] pressAZ5() {
        BlockEntityRBMKConsole console = machine();
        BlockPos pos = console.getBlockPos();
        Level level = console.getLevel();
        if (level != null) {
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.RBMK_SHUTDOWN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        boolean found = false;
        for (int i = -7; i <= 7; i++) {
            for (int j = -7; j <= 7; j++) {
                found |= retarget(columnAt(i, j), 0);
            }
        }
        return found ? new Object[]{} : new Object[]{"No control rods found"};
    }
}
//?}
