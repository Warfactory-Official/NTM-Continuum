package com.hbm.saveddata.satellites;

import com.hbm.saveddata.SatelliteSavedData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;

import java.util.Set;

/**
 * One satellite in orbit; 1.7 {@code com.hbm.saveddata.satellites.Satellite}. Instances live in
 * {@link SatelliteSavedData} on the server and are rebuilt from {@link SatelliteType} on the client for the
 * sat interface, so a subclass mutating state owes {@code SavedData.setDirty()} on the server side.
 */
public abstract class Satellite {

    public final Set<InterfaceActions> ifaceAcs;
    public final Set<CoordActions> coordAcs;
    public final Interfaces satIface;

    protected Satellite(Interfaces satIface, Set<InterfaceActions> ifaceAcs, Set<CoordActions> coordAcs) {
        this.satIface = satIface;
        this.ifaceAcs = ifaceAcs;
        this.coordAcs = coordAcs;
    }

    /**
     * 1.7 {@code XSatelliteRegistry.orbit}: a frequency already in orbit is NOT replaced — upstream hands
     * the payload to the satellite that is already there, and this port keeps the refusal.
     */
    public static void orbit(ServerLevel level, SatelliteType type, int freq, double x, double y, double z) {
        SatelliteSavedData data = SatelliteSavedData.get(level);
        if (data.isFreqTaken(freq)) return;
        Satellite sat = type.create();
        // 1.7 SatelliteBase.onOrbit: floor the launch site into the target, then announce the link on RTTY.
        sat.setTarget((int) Math.floor(x), (int) Math.floor(z));
        data.put(freq, sat);
        com.hbm.tileentity.network.RTTYSystem.broadcast(level, CHAN_SATLINK,
                "Established connection to " + sat.getType() + " at " + sat.targetX + " / " + sat.targetZ);
        sat.onOrbit(level, x, y, z);
        data.setDirty();
    }

    public static final String CHAN_SATLINK = "SAT_LINK";

    public abstract SatelliteType type();

    public void onOrbit(ServerLevel level, double x, double y, double z) {
    }

    public void onClick(ServerLevel level, int x, int z) {
    }

    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
    }

    public enum InterfaceActions {
        HAS_MAP,
        CAN_CLICK,
        SHOW_COORDS,
        HAS_RADAR,
        HAS_ORES
    }

    public enum CoordActions {
        HAS_Y
    }

    // --- the radio surface: what a sat link asks a satellite over RTTY ---

    /**
     * The satellite's reply register. A command writes here and the ground station reads it back on the
     * next {@code getvalue}, which is how 1.12.2's sat link returns anything at all.
     */
    public String tx = "";

    /** The ground coordinates a targeted satellite works over; 1.7 keeps them on the satellite itself. */
    public int targetX;
    public int targetZ;

    public void setTarget(int x, int z) {
        this.targetX = x;
        this.targetZ = z;
    }

    /** The model name a sat link prints for this satellite. */
    public String getType() {
        return type().getSerializedName().toUpperCase(java.util.Locale.US);
    }

    /** The lines a sat link shows for this satellite; empty where it has nothing to say. */
    public Component[] getInfo(Level level) {
        return new Component[0];
    }

    /** The colour a sat link draws this satellite's trace in. */
    public float[] getColor() {
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    public static final String CMD_SETTARGET = "settarget";
    public static final String CMD_GETTARGET = "gettarget";
    public static final String CMD_GETTARGETX = "gettargetx";
    public static final String CMD_GETTARGETZ = "gettargetz";

    /**
     * Run one radio command. The reply, if any, is left in {@link #tx}. As in 1.7, a command that writes
     * nothing leaves the previous answer in the register.
     */
    public final void onCommand(Level level, String... cmd) {
        onCommandTarget(cmd);
        onCommandImpl(level, cmd);
    }

    /** The four commands every satellite answers, whatever it is; 1.7 {@code onCommandTarget}. */
    private void onCommandTarget(String... cmd) {
        if (cmd.length == 0) return;
        switch (cmd[0]) {
            case CMD_SETTARGET -> {
                if (cmd.length == 3) {
                    targetX = parse(cmd[1]);
                    targetZ = parse(cmd[2]);
                } else if (cmd.length == 4) {
                    targetX = parse(cmd[1]);
                    targetZ = parse(cmd[3]);
                }
            }
            case CMD_GETTARGET -> tx = targetX + ";" + targetZ;
            case CMD_GETTARGETX -> tx = "" + targetX;
            case CMD_GETTARGETZ -> tx = "" + targetZ;
            default -> { }
        }
    }

    private static int parse(String value) {
        return com.hbm.api.redstoneoverradio.IRORInteractive.parseInt(
                value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    protected void onCommandImpl(Level level, String... cmd) {
    }

    /** Once a tick, for a satellite that watches something rather than answering questions. */
    public void onUpdateTick(ServerLevel level) {
    }

    public enum Interfaces {
        NONE,
        SAT_PANEL,
        SAT_COORD
    }
}
