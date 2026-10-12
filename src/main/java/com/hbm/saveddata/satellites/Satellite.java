package com.hbm.saveddata.satellites;

import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.tileentity.network.RTTYSystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * One satellite in orbit; 1.7 {@code com.hbm.saveddata.satellites.SatelliteBase}. Instances live in
 * {@link SatelliteSavedData} on the server, keyed by the frequency of the chip that launched them. A subclass
 * mutating state owes {@code SatelliteSavedData.setDirty()}.
 *
 * <p>Everything a satellite does is reached one of two ways, exactly as in 1.7: a radio command relayed by a
 * ground station ({@link #onCommand}, which leaves its answer in {@link #tx}), or a coordinate handed over by the
 * satellite designator, the laser designator or a radar's relay slot ({@link #onCoordAction}).
 */
public abstract class Satellite {

    public static final String CHAN_SATLINK = "SAT_LINK";

    public static final String CMD_SETTARGET = "settarget";
    public static final String CMD_GETTARGET = "gettarget";
    public static final String CMD_GETTARGETX = "gettargetx";
    public static final String CMD_GETTARGETZ = "gettargetz";

    /** The ground coordinates a targeted satellite works over; 1.7 keeps them on the satellite itself. */
    public int targetX;
    public int targetZ;

    /**
     * The satellite's reply register. A command writes here and the ground station reads it back on the
     * next {@code VAL:rx}. As in 1.7, a command that writes nothing leaves the previous answer in place.
     */
    public String tx = "";

    /**
     * Data an addon keeps on the satellite, saved with it as {@code "extra"} (empty: not written). Keys are the addon's
     * own, namespaced: NTM: Space keeps hbm-space's orbit settings (inclination, altitude, colour, ...) under
     * {@code "ntmspace:orbit"}, set by an {@code XSatelliteRegistry.ORBITED} listener. Mutating it owes
     * {@code SatelliteSavedData.setDirty()}.
     */
    public net.minecraft.nbt.CompoundTag extra = new net.minecraft.nbt.CompoundTag();

    /** 1.7 {@code XSatelliteRegistry.orbit}. */
    public static void orbit(ServerLevel level, ItemStack stack, int freq, double x, double y, double z) {
        XSatelliteRegistry.orbit(level, stack, freq, x, y, z);
    }

    public abstract SatelliteType type();

    /** The model name a ground station and the launch broadcast print for this satellite. */
    public abstract String getType();

    /**
     * Whether 1.7's base {@code writeToNBT} runs for this class. {@code SatelliteMiner} and
     * {@code SatelliteHorizons} override {@code writeToNBT}/{@code readFromNBT} without calling super, so their
     * target and reply register are not saved and come back as 0 / 0 / "" after a reload. Ported as found.
     */
    public boolean savesBaseData() {
        return true;
    }

    /** When a satellite is created, i.e. this frequency is occupied for the first time. */
    public void onOrbit(ServerLevel level, double x, double y, double z) {
        setTarget((int) Math.floor(x), (int) Math.floor(z));
        RTTYSystem.broadcast(level, CHAN_SATLINK,
                "Established connection to " + getType() + " at " + targetX + " / " + targetZ);
    }

    /** For subsequent items sent under the same frequency as an existing satellite. */
    public void onPartDelivered(ServerLevel level, ItemStack part) {
    }

    /** Run one radio command. The reply, if any, is left in {@link #tx}. */
    public void onCommand(Level level, String... cmd) {
        onCommandTarget(level, cmd);
        onCommandImpl(level, cmd);
    }

    /** The four commands every satellite answers, whatever it is; 1.7 {@code onCommandTarget}. */
    public void onCommandTarget(Level level, String... cmd) {
        if (cmd.length <= 0) return;

        if (cmd[0].equals(CMD_SETTARGET)) {
            if (cmd.length == 3) {
                targetX = parse(cmd[1]);
                targetZ = parse(cmd[2]);
            }
            if (cmd.length == 4) {
                targetX = parse(cmd[1]);
                targetZ = parse(cmd[3]);
            }
            return;
        }

        if (cmd[0].equals(CMD_GETTARGET)) {
            this.tx = targetX + ";" + targetZ;
            return;
        }

        if (cmd[0].equals(CMD_GETTARGETX)) {
            this.tx = "" + targetX;
            return;
        }

        if (cmd[0].equals(CMD_GETTARGETZ)) {
            this.tx = "" + targetZ;
        }
    }

    /** 1.7 {@code IRORInteractive.parseInt(String)}: unbounded. */
    protected static int parse(String value) {
        return com.hbm.api.redstoneoverradio.IRORInteractive.parseInt(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public void setTarget(int x, int z) {
        this.targetX = x;
        this.targetZ = z;
    }

    public void onCommandImpl(Level level, String... cmd) {
    }

    public void onCoordAction(ServerLevel level, Player player, int x, int y, int z) {
    }
}
