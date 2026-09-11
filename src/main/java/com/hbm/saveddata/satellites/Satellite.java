package com.hbm.saveddata.satellites;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.tileentity.network.RTTYSystem;
import dev.golem.core.data.Tags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One thing in orbit. A satellite is addressed by the frequency it was launched on rather than by any
 * position, lives in {@link SatelliteSavedData} rather than in the world, and answers a small text command
 * surface — which is what makes it reachable from a sat link, and through that from a radio torch.
 *
 * <p>The id a satellite persists under is its slot in {@link #REGISTRY}, so that order must never change.
 * Slots whose satellite is not ported yet are left empty rather than closed up, so the ids stay stable for
 * whoever ports them.
 *
 * <p>Deviations from CE: the tape-drive data pair ({@code driveInput}/{@code driveOutput}) is left out
 * with the tape drive itself, and satellites are constructed from an explicit factory per slot rather than
 * by reflecting on a class list.
 */
public abstract class Satellite {

    public static final String CHAN_SATLINK = "SAT_LINK";

    public static final String CMD_SETTARGET = "settarget";
    public static final String CMD_GETTARGET = "gettarget";
    public static final String CMD_GETTARGETX = "gettargetx";
    public static final String CMD_GETTARGETZ = "gettargetz";

    /** What a satellite lets an interface do with it. */
    public enum InterfaceActions { HAS_MAP, CAN_CLICK, SHOW_COORDS, HAS_RADAR, HAS_ORES }

    public enum CoordActions { HAS_Y }

    public enum Interfaces { NONE, SAT_PANEL, SAT_COORD }

    public final List<InterfaceActions> ifaceAcs = new ArrayList<>();
    public final List<CoordActions> coordAcs = new ArrayList<>();
    public Interfaces satIface = Interfaces.NONE;

    public int targetX;
    public int targetZ;
    /** The last value a {@code get} command left for the reader to pick up. */
    public String tx = "";

    public boolean isDirty;

    public int getID() {
        return SatelliteRegistry.idOf(getClass());
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    // --- persistence ---

    public void save(CompoundTag tag) {
        tag.putInt("targetX", targetX);
        tag.putInt("targetZ", targetZ);
        tag.putString("tx", tx);
    }

    public void load(CompoundTag tag) {
        this.targetX = Tags.getIntOr(tag, "targetX", 0);
        this.targetZ = Tags.getIntOr(tag, "targetZ", 0);
        this.tx = Tags.getStringOr(tag, "tx", "");
    }

    // --- commands ---

    public void onCommand(Level level, String... cmd) {
        onCommandTarget(level, cmd);
        onCommandImpl(level, cmd);
    }

    /** The target commands every satellite answers, whatever else it does. */
    public void onCommandTarget(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (CMD_SETTARGET.equals(cmd[0])) {
            if (cmd.length == 3) {
                targetX = IRORInteractive.parseInt(cmd[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
                targetZ = IRORInteractive.parseInt(cmd[2], Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
            if (cmd.length == 4) {
                targetX = IRORInteractive.parseInt(cmd[1], Integer.MIN_VALUE, Integer.MAX_VALUE);
                targetZ = IRORInteractive.parseInt(cmd[3], Integer.MIN_VALUE, Integer.MAX_VALUE);
            }
            return;
        }

        if (CMD_GETTARGET.equals(cmd[0])) {
            this.tx = targetX + ";" + targetZ;
            return;
        }

        if (CMD_GETTARGETX.equals(cmd[0])) {
            this.tx = "" + targetX;
            return;
        }

        if (CMD_GETTARGETZ.equals(cmd[0])) {
            this.tx = "" + targetZ;
        }
    }

    public void onCommandImpl(Level level, String... cmd) { }

    public void setTarget(int x, int z) {
        this.targetX = x;
        this.targetZ = z;
    }

    public void markDirty() {
        this.isDirty = true;
    }

    // --- hooks ---

    public void onUpdateTick(Level level) { }

    /** A second item arriving on a frequency that already has a satellite. */
    public void onPartDelivered(Level level, ItemStack part) { }

    /** Called once the launch reaches orbit. */
    public void onOrbit(Level level, double x, double y, double z) { }

    public void onClick(Level level, ServerPlayer player, int x, int z) { }

    public void onCoordAction(Level level, ServerPlayer player, int x, int y, int z) { }

    public Component[] getInfo(Level level) {
        return new Component[0];
    }

    public abstract float[] getColor();

    // --- orbit insertion ---

    public static void orbit(Level level, int id, int freq, double x, double y, double z) {
        orbit(level, id, ItemStack.EMPTY, freq, x, y, z);
    }

    /**
     * Puts a satellite on a frequency. A frequency that already has one takes the arriving item as a part
     * instead, which is how the relay is fed its extra stages.
     */
    public static void orbit(Level level, int id, ItemStack part, int freq, double x, double y, double z) {
        if (!(level instanceof ServerLevel server)) return;

        SatelliteSavedData data = SatelliteSavedData.get(server);
        Satellite existing = data.getSatFromFreq(freq);

        if (existing != null) {
            existing.onPartDelivered(level, part);
            data.setDirty();
            return;
        }

        Satellite sat = SatelliteRegistry.create(id);
        if (sat == null) return;

        data.put(freq, sat);
        sat.setTarget((int) Math.floor(x), (int) Math.floor(z));
        RTTYSystem.broadcast(level, CHAN_SATLINK,
                "Established connection to " + sat.getType() + " at " + sat.targetX + " / " + sat.targetZ);
        sat.onOrbit(level, x, y, z);
        data.setDirty();
    }

    public static @Nullable Satellite create(int id) {
        return SatelliteRegistry.create(id);
    }
}
