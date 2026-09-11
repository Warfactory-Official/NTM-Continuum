package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.saveddata.satellites.Satellite;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.saveddata.satellites.SatelliteSavedData;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The dish that talks to orbit. It holds a frequency, and if it can see the sky it is connected to whatever
 * satellite answers on that frequency — which is the whole machine: a text channel between a radio torch on
 * the ground and something that is not in the world at all.
 *
 * <p>The fourteenth machine with a redstone-over-radio surface, and the only one whose function forwards to
 * something outside the world: {@code FUN:tx} splits its payload back into a command and hands it to the
 * satellite, whose reply is left in {@code VAL:rx}.
 */
public class TileEntityMachineSatLink extends TileEntityLoadedBase
        implements IRORValueProvider, IRORInteractive {

    /** How far the mast rises above the core cell, which is what the dish has to see past. */
    public static final int MAST_HEIGHT = 7;

    public static final float SPEED = 0.25F;
    public static final float ACTIVE_ROT = -15F;
    public static final float ACTIVE_LIFT = -45F;
    public static final float INACTIVE_ROT = 0F;
    public static final float INACTIVE_LIFT = -85F;

    public boolean connected;
    public int freq;

    public float rot = INACTIVE_ROT;
    public float prevRot = INACTIVE_ROT;
    public float lift = INACTIVE_LIFT;
    public float prevLift = INACTIVE_LIFT;

    public Component[] info = new Component[0];

    public TileEntityMachineSatLink(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SATLINK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSatLink link) {
        link.connected = false;

        if (level instanceof ServerLevel server && seesSky(server, pos)) {
            link.connected = SatelliteSavedData.get(server).isFreqTaken(link.freq);
        }

        link.updateInfo(level);
        link.networkPackNT();
    }

    /**
     * Whether anything is built over the dish. Upstream compares the core's own y against the world height,
     * which works there because 1.12's height map tracks light opacity and the machine's own parts are not
     * opaque; 26.1's height map tracks anything that blocks motion, so the mast would hide the dish from
     * itself. The comparison is made against the top of the mast instead, which asks the same question.
     */
    private static boolean seesSky(ServerLevel level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + MAST_HEIGHT;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineSatLink link) {
        link.prevRot = link.rot;
        link.prevLift = link.lift;

        link.rot = approach(link.rot, link.connected ? ACTIVE_ROT : INACTIVE_ROT);
        link.lift = approach(link.lift, link.connected ? ACTIVE_LIFT : INACTIVE_LIFT);
    }

    private static float approach(float value, float target) {
        if (Math.abs(value - target) <= SPEED) return target;
        return value < target ? value + SPEED : value - SPEED;
    }

    private void updateInfo(Level level) {
        if (!connected) {
            if (this.info.length > 0) this.info = new Component[0];
            return;
        }

        Satellite sat = satellite(level);
        if (sat != null) this.info = sat.getInfo(level);
    }

    private @Nullable Satellite satellite(Level level) {
        if (!(level instanceof ServerLevel server)) return null;
        return SatelliteSavedData.get(server).getSatFromFreq(freq);
    }

    // --- sync ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(connected);
        buf.writeInt(freq);
        buf.writeInt(info.length);
        for (Component component : info) {
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, component);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.connected = buf.readBoolean();
        this.freq = buf.readInt();
        this.info = new Component[buf.readInt()];
        for (int i = 0; i < info.length; i++) {
            this.info[i] = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("freq", freq);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        freq = input.getIntOr("freq", 0);
    }

    // --- redstone over radio ---

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "connected", PREFIX_VALUE + "freq", PREFIX_VALUE + "rx", PREFIX_VALUE + "type",
                PREFIX_FUNCTION + "setfreq" + NAME_SEPARATOR + "freq",
                PREFIX_FUNCTION + "tx" + NAME_SEPARATOR + "payload"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "connected").equals(name)) return this.connected ? "TRUE" : "FALSE";
        if ((PREFIX_VALUE + "freq").equals(name)) return "" + this.freq;

        if ((PREFIX_VALUE + "type").equals(name)) {
            Satellite sat = satellite(level);
            return sat != null ? sat.getType() : "";
        }

        if ((PREFIX_VALUE + "rx").equals(name)) {
            Satellite sat = satellite(level);
            return sat != null ? sat.tx : "";
        }

        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setfreq").equals(name) && params.length == 1) {
            this.freq = IRORInteractive.parseInt(params[0], 0, 100_000);
            setChanged();
        }

        if ((PREFIX_FUNCTION + "tx").equals(name)) {
            Satellite sat = satellite(level);
            String[] cmd = String.join(IRORInteractive.PARAM_SEPARATOR, params).split(" ");
            if (sat != null && level instanceof ServerLevel server) {
                sat.onCommand(level, cmd);
                SatelliteSavedData.get(server).setDirty();
            }
            BlockPos pos = getBlockPos();
            SatelliteRayScan.reportEvent(level, pos.getX(), pos.getY(), pos.getZ(),
                    SatelliteRayScan.RayEvent.INFO_RADIO, 300);
            setChanged();
        }

        return null;
    }
}
