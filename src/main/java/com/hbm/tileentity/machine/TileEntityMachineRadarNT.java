package com.hbm.tileentity.machine;

import com.hbm.api.block.IRadarCommandReceiver;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.entity.IRadarDetectableNT;
import com.hbm.api.entity.IRadarDetectableNT.RadarScanParams;
import com.hbm.api.entity.RadarEntry;
import com.hbm.capability.HbmLivingProps;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachineRadarNT;
import com.hbm.items.ModRadarUtilityItems;
import com.hbm.items.tool.ItemCoordinateBase;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 1.7 {@code TileEntityMachineRadarNT}: "Now with SmЯt™ lag-free entity detection! (patent pending)".
 *
 * <p>Every server tick one global sweep collects the few entities any radar could care about into
 * {@link #matchingEntities}; each radar then filters that short list by range and altitude, turns hits into
 * {@link RadarEntry} blips through {@link #converters}, and derives its redstone from them. Slots 0-7 hold
 * link items (a radar linker aims launch commands, typed 1-8 over the map), slot 8 a linker to a screen, slot 9
 * a battery. The height map is sampled a hundred cells a tick into a 200x200 byte image.
 *
 * <p>Deliberate differences, each forced: the dimension of an entry is its level; the sweep runs at the end of
 * the server tick rather than the start, which is the same moment for the next tick's radars; the map only
 * reads loaded chunks, and for the rest the height map saved in the region file (1.7 {@code provideChunk}: off disk,
 * never generated) through Core's asynchronous {@code Chunks.readSavedHeights} - so an unloaded cell appears one sweep
 * (20 s) later than in 1.7; {@link #generateChunks} still generates as 1.7's did. There is no legacy {@code IRadarDetectable} converter
 * because no port entity implements the legacy interface.
 */
public class TileEntityMachineRadarNT extends TileEntityMachineBase implements IEnergyReceiverMK2, MenuProvider,
        IControlReceiver {

    public boolean scanMissiles = true;
    public boolean scanShells = true;
    public boolean scanPlayers = true;
    public boolean smartMode = true;
    public boolean redMode = true;
    public boolean showMap = false;

    public boolean jammed = false;

    public float prevRotation;
    public float rotation;

    public long power = 0;

    protected int pingTimer = 0;
    protected int lastPower;
    protected static final int maxTimer = 80;

    public static int maxPower = 100_000;
    public static int consumption = 500;
    public static int radarRange = 1_000;
    public static int radarBuffer = 30;
    public static int radarAltitude = 55;
    public static int chunkLoadCap = 10;
    public static boolean generateChunks = false;

    public byte[] map = new byte[40_000];
    public boolean clearFlag = false;

    public List<RadarEntry> entries = new ArrayList<>();

    public TileEntityMachineRadarNT(BlockPos pos, BlockState state) {
        this(ModRadarUtilityBlockEntities.RADAR.get(), pos, state);
    }

    protected TileEntityMachineRadarNT(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 10);
    }

    @Override
    public String getDefaultName() {
        return "container.radar";
    }

    public int getRange() {
        return radarRange;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRadarNT radar) {
        radar.updateServer((ServerLevel) level);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRadarNT radar) {
        if (radar.map == null || radar.map.length != 40_000) radar.map = new byte[40_000];
        radar.prevRotation = radar.rotation;
        if (radar.power > 0) radar.rotation += 5F;

        if (radar.rotation >= 360) {
            radar.rotation -= 360F;
            radar.prevRotation -= 360F;
        }
    }

    protected void updateServer(ServerLevel level) {
        if (this.map == null || this.map.length != 40_000) this.map = new byte[40_000];
        int x0 = worldPosition.getX(), y0 = worldPosition.getY(), z0 = worldPosition.getZ();

        // 1.7 charges from slot 9 and then again from slot 0 - slot 0 is the first link slot; the odd second
        // charge is kept, a battery in it works
        this.power = Library.chargeTEFromItems(inventory, 9, power, maxPower);
        this.power = Library.chargeTEFromItems(inventory, 0, power, maxPower);
        this.jammed = false;
        allocateTargets(level);

        if (this.lastPower != getRedPower()) {
            this.setChanged();
            updateRedstone(level);
        }
        lastPower = getRedPower();

        if (!this.muffled) {
            pingTimer++;
            if (power > 0 && pingTimer >= maxTimer) {
                level.playSound(null, x0, y0, z0, ModSounds.SONAR_PING.get(), SoundSource.BLOCKS, 5.0F, 1.0F);
                pingTimer = 0;
            }
        }

        if (this.showMap) {
            int chunkLoads = 0;
            for (int i = 0; i < 100; i++) {
                int index = (int) (level.getGameTime() % 400) * 100 + i;
                int iX = (index % 200) * getRange() * 2 / 200;
                int iZ = index / 200 * getRange() * 2 / 200;

                int x = x0 - getRange() + iX;
                int z = z0 - getRange() + iZ;

                if (level.hasChunk(x >> 4, z >> 4)) {
                    this.map[index] = (byte) Mth.clamp(height(level, x, z), 50, 128);
                } else if (this.map[index] == 0 && chunkLoads < chunkLoadCap) {
                    if (generateChunks) {
                        level.getChunk(x >> 4, z >> 4);
                        this.map[index] = (byte) Mth.clamp(height(level, x, z), 50, 128);
                        chunkLoads++;
                    } else {
                        // 1.7 WorldUtil.provideChunk: the chunk off disk if it was ever saved, never generated
                        int[] heights = savedHeights(level, x >> 4, z >> 4);
                        if (heights != null) {
                            this.map[index] = (byte) Mth.clamp(heights[(x & 15) + (z & 15) * 16], 50, 128);
                            chunkLoads++;
                        }
                    }
                }
            }
        }

        ItemStack screenLink = inventory.getStackInSlot(8);
        if (!screenLink.isEmpty() && screenLink.getItem() == ModRadarUtilityItems.radar_linker.get()) {
            BlockPos pos = ItemCoordinateBase.getPosition(screenLink);
            if (pos != null && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TileEntityMachineRadarScreen screen) {
                screen.entries.clear();
                screen.entries.addAll(this.entries);
                screen.refX = x0;
                screen.refY = y0;
                screen.refZ = z0;
                screen.range = this.getRange();
                screen.linked = true;
                screen.networkPackNT();
            }
        }

        this.networkPackNT();
        if (this.clearFlag) {
            this.map = new byte[40_000];
            this.clearFlag = false;
        }
    }

    /** 1.7 {@code getHeightValue}: the first light-blocking block from the top, water and leaves included. */
    /**
     * Saved height maps of unloaded chunks, read off the region files through Core {@code Chunks.readSavedHeights}
     * (no loading, no generation). The read is asynchronous: the first ask starts it and answers null; the height map
     * is used from the next sweep on. A chunk that was never saved stays unread (EMPTY), as 1.7's did.
     */
    private final java.util.Map<Long, java.util.concurrent.CompletableFuture<int[]>> savedChunks = new java.util.HashMap<>();
    private static final int[] EMPTY = new int[0];

    private int @org.jspecify.annotations.Nullable [] savedHeights(ServerLevel server, int cx, int cz) {
        long key = dev.golem.core.world.Chunks.key(cx, cz);
        java.util.concurrent.CompletableFuture<int[]> read = savedChunks.get(key);
        if (read == null) {
            if (savedChunks.size() > 16384) savedChunks.clear(); // a full 1000-block range is 15,625 chunks
            savedChunks.put(key, dev.golem.core.world.Chunks.readSavedHeights(server, cx, cz, Heightmap.Types.MOTION_BLOCKING)
                    .thenApply(h -> h == null ? EMPTY : h));
            return null;
        }
        if (!read.isDone()) return null;
        int[] heights = read.getNow(EMPTY);
        return heights.length == 256 ? heights : null;
    }

    private static int height(Level level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
    }

    /** 1.7 {@code updateRedstoneConnection} for every connection point: the neighbours, then through them. */
    protected void updateRedstone(Level level) {
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        for (DirPos pos : getConPos()) level.updateNeighborsAt(pos.getPos(), getBlockState().getBlock());
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z),
        };
    }

    /** 1.7 subscribed the four horizontal sides; a cable above or below does not connect. */
    @Override
    public boolean canConnect(ForgeDirection dir) {
        return dir == ForgeDirection.NORTH || dir == ForgeDirection.SOUTH || dir == ForgeDirection.EAST
                || dir == ForgeDirection.WEST;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(this.power);
        buf.writeBoolean(this.scanMissiles);
        buf.writeBoolean(this.scanShells);
        buf.writeBoolean(this.scanPlayers);
        buf.writeBoolean(this.smartMode);
        buf.writeBoolean(this.redMode);
        buf.writeBoolean(this.showMap);
        buf.writeBoolean(this.jammed);
        buf.writeInt(entries.size());
        for (RadarEntry entry : entries) entry.toBytes(buf);
        if (this.clearFlag) {
            buf.writeBoolean(true);
        } else {
            buf.writeBoolean(false);
            if (this.showMap && level != null) {
                buf.writeBoolean(true);
                short index = (short) (level.getGameTime() % 400);
                buf.writeShort(index);
                for (int i = index * 100; i < (index + 1) * 100; i++) {
                    buf.writeByte(this.map[i]);
                }
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.scanMissiles = buf.readBoolean();
        this.scanShells = buf.readBoolean();
        this.scanPlayers = buf.readBoolean();
        this.smartMode = buf.readBoolean();
        this.redMode = buf.readBoolean();
        this.showMap = buf.readBoolean();
        this.jammed = buf.readBoolean();
        int count = buf.readInt();
        this.entries.clear();
        for (int i = 0; i < count; i++) {
            RadarEntry entry = new RadarEntry();
            entry.fromBytes(buf);
            this.entries.add(entry);
        }
        if (buf.readBoolean()) { // clear flag
            this.map = new byte[40_000];
        } else if (buf.readBoolean()) { // map enabled
            int index = buf.readShort();
            for (int i = index * 100; i < (index + 1) * 100; i++) {
                this.map[i] = buf.readByte();
            }
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.power = input.getLongOr("power", 0L);
        this.scanMissiles = input.getBooleanOr("scanMissiles", false);
        this.scanShells = input.getBooleanOr("scanShells", false);
        this.scanPlayers = input.getBooleanOr("scanPlayers", false);
        this.smartMode = input.getBooleanOr("smartMode", false);
        this.redMode = input.getBooleanOr("redMode", false);
        this.showMap = input.getBooleanOr("showMap", false);
        input.getIntArray("map").ifPresent(packed -> this.map = unpack(packed));
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putBoolean("scanMissiles", scanMissiles);
        output.putBoolean("scanShells", scanShells);
        output.putBoolean("scanPlayers", scanPlayers);
        output.putBoolean("smartMode", smartMode);
        output.putBoolean("redMode", redMode);
        output.putBoolean("showMap", showMap);
        output.putIntArray("map", pack(map));
    }

    /** The data layer has no byte arrays, so the 1.7 {@code map} byte array travels four bytes per int. */
    static int[] pack(byte[] bytes) {
        int[] out = new int[(bytes.length + 3) / 4];
        for (int i = 0; i < bytes.length; i++) out[i >> 2] |= (bytes[i] & 0xFF) << ((i & 3) * 8);
        return out;
    }

    static byte[] unpack(int[] ints) {
        byte[] out = new byte[40_000];
        for (int i = 0; i < out.length && (i >> 2) < ints.length; i++) out[i] = (byte) (ints[i >> 2] >>> ((i & 3) * 8));
        return out;
    }

    protected void allocateTargets(ServerLevel level) {
        this.entries.clear();

        if (worldPosition.getY() < radarAltitude) return;
        if (this.power < consumption) {
            this.power = 0;
            return;
        }
        this.power -= consumption;

        int scan = this.getRange();
        double cx = worldPosition.getX() + 0.5, cz = worldPosition.getZ() + 0.5;

        RadarScanParams params = new RadarScanParams(this.scanMissiles, this.scanShells, this.scanPlayers, this.smartMode);

        for (Entity e : matchingEntities) {

            if (e.level() == level && Math.abs(e.getX() - cx) <= scan && Math.abs(e.getZ() - cz) <= scan
                    && e.getY() - worldPosition.getY() > radarBuffer) {

                if (e instanceof LivingEntity living && HbmLivingProps.getDigamma(living) > 0.001) {
                    this.jammed = true;
                    entries.clear();
                    return;
                }

                for (Function<Scan, RadarEntry> converter : converters) {
                    RadarEntry entry = converter.apply(new Scan(e, this, params));
                    if (entry != null) {
                        this.entries.add(entry);
                        break;
                    }
                }
            }
        }

        if (level.getGameTime() % 20 == 0) {
            SatelliteDetector.reportEvent(level, SatelliteDetector.DURATION_MEDIUM,
                    SatelliteDetector.BurstIntensity.MEDIUM, worldPosition.getX(), worldPosition.getZ());
            SatelliteRayScan.reportEvent(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    SatelliteRayScan.RayEvent.INFO_RADAR, 200);
        }
    }

    public int getRedPower() {

        if (!entries.isEmpty()) {

            /// PROXIMITY ///
            if (redMode) {

                double maxRange = this.getRange() * Math.sqrt(2D);
                int power = 0;

                for (RadarEntry e : entries) {
                    if (!e.redstone) continue;
                    double dist = Math.sqrt(Math.pow(e.posX - worldPosition.getX(), 2) + Math.pow(e.posZ - worldPosition.getZ(), 2));
                    int p = 15 - (int) Math.floor(dist / maxRange * 15);

                    if (p > power) power = p;
                }

                return power;

            /// TIER ///
            } else {

                int power = 0;

                for (RadarEntry e : entries) {
                    if (!e.redstone) continue;
                    if (e.blipLevel + 1 > power) {
                        power = e.blipLevel + 1;
                    }
                }

                return power;
            }
        }

        return 0;
    }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }

    /** 1.7 {@code isUseableByPlayer}: the map is a menu-less screen, and 1.7 only checked the tile was still there. */
    @Override
    public boolean hasPermission(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this;
    }

    @Override public void receiveControl(CompoundTag data) { }

    @Override
    public void receiveControl(Player player, CompoundTag data) {

        if (dev.golem.core.data.Tags.contains(data, "missiles")) this.scanMissiles = !this.scanMissiles;
        if (dev.golem.core.data.Tags.contains(data, "shells")) this.scanShells = !this.scanShells;
        if (dev.golem.core.data.Tags.contains(data, "players")) this.scanPlayers = !this.scanPlayers;
        if (dev.golem.core.data.Tags.contains(data, "smart")) this.smartMode = !this.smartMode;
        if (dev.golem.core.data.Tags.contains(data, "red")) this.redMode = !this.redMode;
        if (dev.golem.core.data.Tags.contains(data, "map")) this.showMap = !this.showMap;
        if (dev.golem.core.data.Tags.contains(data, "clear")) this.clearFlag = true;

        if (dev.golem.core.data.Tags.contains(data, "gui1") && player instanceof ServerPlayer serverPlayer) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, this, worldPosition);
        }

        if (dev.golem.core.data.Tags.contains(data, "link")) {
            int id = dev.golem.core.data.Tags.getIntOr(data, "link", -1);
            if (id < 0 || id > 7) return;
            ItemStack link = inventory.getStackInSlot(id);

            // 1.7: a sat_relay in the link slot hands a map click to the satellite on its frequency
            if (!link.isEmpty() && link.getItem() == com.hbm.items.ModSatelliteItems.sat_relay.get()
                    && level instanceof ServerLevel server) {
                com.hbm.saveddata.satellites.Satellite sat = com.hbm.saveddata.SatelliteSavedData.get(server)
                        .getSatFromFreq(com.hbm.items.ISatChip.getFreqS(link));

                if (dev.golem.core.data.Tags.contains(data, "launchPosX")) {
                    int x = dev.golem.core.data.Tags.getIntOr(data, "launchPosX", 0);
                    int z = dev.golem.core.data.Tags.getIntOr(data, "launchPosZ", 0);
                    int y = 60; // 1.7: "one day I will make radars transmit Y coordinate as well"
                    bleep(player);
                    // 1.7 dereferenced sat unguarded (a server crash on an empty frequency); guarded here
                    if (sat != null) {
                        sat.onCoordAction(server, player, x, y, z);
                        com.hbm.saveddata.SatelliteSavedData.get(server).setDirty();
                    }
                }
            }
            if (!link.isEmpty() && link.getItem() == ModRadarUtilityItems.radar_linker.get()) {
                BlockPos pos = ItemCoordinateBase.getPosition(link);

                if (pos != null && level != null && level.getBlockEntity(pos) instanceof IRadarCommandReceiver rec) {
                    if (dev.golem.core.data.Tags.contains(data, "launchEntity")) {
                        Entity entity = level.getEntity(dev.golem.core.data.Tags.getIntOr(data, "launchEntity", -1));
                        if (entity != null && rec.sendCommandEntity(entity)) bleep(player);
                    } else if (dev.golem.core.data.Tags.contains(data, "launchPosX")) {
                        int x = dev.golem.core.data.Tags.getIntOr(data, "launchPosX", 0);
                        int z = dev.golem.core.data.Tags.getIntOr(data, "launchPosZ", 0);
                        if (rec.sendCommandPosition(x, worldPosition.getY(), z)) bleep(player);
                    }
                }
            }
        }
    }

    private void bleep(Player player) {
        if (level != null) level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // --- MenuProvider: 1.7 GUI id 1, the link slots ---

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineRadarNT(containerId, playerInventory, this);
    }

    // --- the detection system ---

    /** What a converter is handed: 1.7's {@code Triplet<Entity, Object, RadarScanParams>}. */
    public record Scan(Entity entity, Object radar, RadarScanParams params) {}

    /**
     * Turn a detected entity into a {@link RadarEntry}, or null when it does not apply. The first converter
     * to answer wins, so the ones registered first have the highest priority.
     */
    public static final List<Function<Scan, RadarEntry>> converters = new ArrayList<>();
    public static final List<Class<?>> classes = new ArrayList<>();
    public static final List<Entity> matchingEntities = new ArrayList<>();

    /**
     * Collects every loaded entity whose class is registered in {@link #classes}. From this short list the
     * radars can cheaply pick their own, since the number of detectable entities is comparatively low.
     */
    public static void updateSystem(MinecraftServer server) {
        matchingEntities.clear();

        for (ServerLevel world : server.getAllLevels()) {
            for (Entity entity : world.getAllEntities()) {
                for (Class<?> clazz : classes) {
                    if (clazz.isInstance(entity)) {
                        matchingEntities.add(entity);
                        break;
                    }
                }
            }
        }
    }

    /** Registers a class that, if an entity inherits it, is picked up by the system. */
    public static void registerEntityClasses() {
        if (!classes.isEmpty()) return;
        classes.add(IRadarDetectableNT.class);
        classes.add(Player.class);
    }

    /** Registers the converters: detectables first, then players. */
    public static void registerConverters() {
        if (!converters.isEmpty()) return;
        // IRadarDetectableNT
        converters.add(x -> {
            if (x.entity() instanceof IRadarDetectableNT detectable) {
                if (detectable.canBeSeenBy(x.radar()) && detectable.paramsApplicable(x.params())) {
                    return new RadarEntry(detectable, x.entity(), detectable.suppliesRedstone(x.params()));
                }
            }
            return null;
        });
        // Players
        converters.add(x -> {
            if (x.entity() instanceof Player player && x.params().scanPlayers) return new RadarEntry(player);
            return null;
        });
    }
}
