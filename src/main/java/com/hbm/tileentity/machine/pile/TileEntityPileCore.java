package com.hbm.tileentity.machine.pile;

import com.hbm.blocks.machine.pile.BlockPile;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The brain of an assembled Chicago Pile. It owns the channels drilled through the graphite — fuel,
 * ventilation and control — and runs the neutron economy between them once a tick.
 *
 * <p>Neutrons move by <b>segment</b>, not by ray: every channel is binned into the vertical slice of the
 * pile it enters through, each fuel slice pools the neutrons its rods emit, and the pool then walks
 * outward slice by slice, attenuated by every control slice it crosses. That is why an extended control
 * rod throttles the whole pile rather than just its neighbours, and why the simulation costs the same
 * whether the pile is minimal or fifteen blocks on a side.
 *
 * <p>Deferred: the burning debris thrown out on meltdown still flies and detonates, but does not trail
 * flame — the trail is a client-only particle and {@code BulletConfig}'s update hook runs on both sides.
 */
public class TileEntityPileCore extends TileEntityLoadedBase {

    public static final int MAX_HEAT = 800;
    /** Set while a meltdown is tearing the structure down, so the cells do not each try to clean up. */
    public static boolean meltingDown = false;

    public PileOrientation orientation = PileOrientation.NEITHER;

    public int height;
    public int width;
    public int depth;

    public int left;
    public int right;
    public int up;

    public final List<PileChannel> fuelChannels = new ArrayList<>();
    public final List<PileChannel> ventilationChannels = new ArrayList<>();
    public final List<PileChannel> controlChannels = new ArrayList<>();

    public PileSegment[] segments = new PileSegment[0];

    public double highestHeat;

    /** The fragments a meltdown throws. Registered by {@link #initDebris()} during mod setup. */
    public static BulletConfig pile_debris;

    public TileEntityPileCore(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PILE_CORE.get(), pos, state);
    }

    /**
     * Declares the meltdown debris round. Called from mod setup rather than a static initialiser because
     * {@link BulletConfigs} assigns ids in registration order and every config has to be in the registry
     * before the first save or packet touches one.
     */
    public static void initDebris() {
        if (pile_debris != null) return;
        pile_debris = BulletConfigs.register("pile_debris", new BulletConfig()
                .setLife(200).setVel(1F).setGrav(0.1F)
                .setOnUpdate(fragment -> {
                    if (!fragment.level().isClientSide()) return;
                    FlameCreator.composeEffectClient(fragment.level(), fragment.getX(),
                            fragment.getY() - 0.125D, fragment.getZ(), FlameCreator.META_FIRE);
                })
                .setOnImpact((bullet, hit) -> {
                    bullet.level().explode(bullet, bullet.getX(), bullet.getY(), bullet.getZ(), 5F,
                            Level.ExplosionInteraction.TNT);
                    bullet.discard();
                }));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPileCore core) {
        core.runSimulation();
        core.handleVentilation(level);
        core.handleMeltdown(level);
        core.networkPackNT();
    }

    // --- structure ---

    public TileEntityPileCore setupSize(int up, int down, int left, int right, int depth) {
        this.height = up + 1 + down;
        this.width = left + 1 + right;
        this.depth = depth;
        this.left = left;
        this.right = right;
        this.up = up;
        this.segments = new PileSegment[width];
        setChanged();
        return this;
    }

    public List<PileChannel> getChannelList(PileChannelType type) {
        if (type == PileChannelType.FUEL) return this.fuelChannels;
        if (type == PileChannelType.VENTILATION) return this.ventilationChannels;
        return this.controlChannels;
    }

    public @Nullable PileChannel getFuelChannel(int x, int y, int z) { return getChannel(x, y, z, fuelChannels); }
    public @Nullable PileChannel getVentilationChannel(int x, int y, int z) { return getChannel(x, y, z, ventilationChannels); }
    public @Nullable PileChannel getControlChannel(int x, int y, int z) { return getChannel(x, y, z, controlChannels); }

    private @Nullable PileChannel getChannel(int x, int y, int z, List<PileChannel> list) {
        for (PileChannel channel : list) {
            BlockPos p = channel.entry.getPos();
            if (p.getX() == x && p.getY() == y && p.getZ() == z) return channel;
        }
        return null;
    }

    public int getFuelChannelNum(PileChannel chan) { return fuelChannels.indexOf(chan); }
    public int getVentilationChannelNum(PileChannel chan) { return ventilationChannels.indexOf(chan); }
    public int getControlChannelNum(PileChannel chan) { return controlChannels.indexOf(chan); }

    private int getMeta(int x, int y, int z) {
        BlockState state = level.getBlockState(new BlockPos(x, y, z));
        return state.hasProperty(BlockPile.META) ? state.getValue(BlockPile.META) : -1;
    }

    private void setMeta(int x, int y, int z, int meta) {
        BlockPos target = new BlockPos(x, y, z);
        level.setBlock(target, level.getBlockState(target).setValue(BlockPile.META, meta), 3);
    }

    /**
     * Drills a channel from {@code (x, y, z)} along {@code dir}, or fills one back in when the hand drill
     * is used on a mouth that already has one. Reports every obstruction it finds rather than the first,
     * so one pass tells the player everything that is in the way.
     */
    public boolean drillChannel(int x, int y, int z, ForgeDirection dir, Player player) {
        int startMeta = getMeta(x, y, z);
        PileChannelType type = PileChannelType.getChannelType(dir, orientation);
        int size = channelLength(type);
        List<PileChannel> list = getChannelList(type);

        if (startMeta == BlockPile.META_FUEL_IN || startMeta == BlockPile.META_AIR_IN
                || startMeta == BlockPile.META_CONTROL) {
            for (int i = 0; i < list.size(); i++) {
                PileChannel chan = list.get(i);
                BlockPos p = chan.entry.getPos();
                if (p.getX() == x && p.getY() == y && p.getZ() == z && chan.entry.getDir() == dir) {
                    if (chan.type == PileChannelType.FUEL) chan.ejectAll();
                    list.remove(i);
                    for (int j = 0; j < size; j++) {
                        setMeta(x + dir.offsetX * j, y + dir.offsetY * j, z + dir.offsetZ * j, BlockPile.META_DUMMY);
                    }
                    level.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.ITEM_BREAK,
                            SoundSource.BLOCKS, 1F, 0.75F);
                    recalculateSegments();
                    setChanged();
                    return true;
                }
            }
        }

        boolean error = false;
        for (int i = 0; i < size; i++) {
            BlockPos iPos = new BlockPos(x + dir.offsetX * i, y + dir.offsetY * i, z + dir.offsetZ * i);
            if (!level.getBlockState(iPos).is(ModPileBlocks.pile_block.get())) {
                BlockPile.sendError(level, iPos, "Foreign block in reactor", player);
                error = true;
                continue;
            }
            int meta = getMeta(iPos.getX(), iPos.getY(), iPos.getZ());
            if (meta == BlockPile.META_EDGE) {
                BlockPile.sendError(level, iPos, "Cannot drill along edge", player);
                error = true;
            } else if (meta == BlockPile.META_CORE) {
                BlockPile.sendError(level, iPos, "Cannot intersect core", player);
                error = true;
            } else if (meta == BlockPile.META_CHANNEL) {
                BlockPile.sendError(level, iPos, "Cannot intersect channel", player);
                error = true;
            } else if (meta != BlockPile.META_DUMMY) {
                BlockPile.sendError(level, iPos, "Cannot intersect channel IO", player);
                error = true;
            }
        }

        if (error) return false;

        for (int i = 0; i < size; i++) {
            int iX = x + dir.offsetX * i;
            int iY = y + dir.offsetY * i;
            int iZ = z + dir.offsetZ * i;
            if (i == 0) {
                setMeta(iX, iY, iZ, switch (type) {
                    case FUEL -> BlockPile.META_FUEL_IN;
                    case VENTILATION -> BlockPile.META_AIR_IN;
                    case CONTROL -> BlockPile.META_CONTROL;
                });
            } else if (i == size - 1) {
                setMeta(iX, iY, iZ, switch (type) {
                    case FUEL -> BlockPile.META_FUEL_OUT;
                    case VENTILATION -> BlockPile.META_AIR_OUT;
                    case CONTROL -> BlockPile.META_CONTROL;
                });
            } else {
                setMeta(iX, iY, iZ, BlockPile.META_CHANNEL);
            }
        }

        list.add(new PileChannel(x, y, z, dir, size, type));
        level.playSound(null, x + 0.5, y + 0.5, z + 0.5, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1F, 1.25F);
        markChanged();
        recalculateSegments();
        return true;
    }

    private int channelLength(PileChannelType type) {
        return switch (type) {
            case CONTROL -> height;
            case FUEL -> depth;
            case VENTILATION -> width;
        };
    }

    /** Collapses the structure back to loose graphite by removing the core. */
    public void destroy() {
        level.setBlockAndUpdate(worldPosition, ModPileBlocks.pile_brick.get().defaultBlockState());
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            for (PileChannel chan : this.fuelChannels) chan.ejectAll();
        }
        super.setRemoved();
    }

    // --- simulation ---

    private void runSimulation() {
        for (PileChannel chan : this.fuelChannels) {
            if (chan.length <= 0) continue;
            double producedNeutrons = 0D;

            for (int i = 0; i < chan.rods.length; i++) {
                ItemStack stack = chan.rods[i];
                if (stack.isEmpty() || !(stack.getItem() instanceof ItemPileRodMK2)) continue;

                double neut = ItemPileRodMK2.getReactivity(stack, chan.incomingNeutrons / chan.length);
                producedNeutrons += neut;
                chan.heat += neut * ItemPileRodMK2.getHeatPerNeutron(stack);
                chan.rods[i] = ItemPileRodMK2.react(stack, neut);
            }
            chan.outgoingNeutrons = producedNeutrons;
            chan.incomingNeutrons = 0D;
        }

        for (PileSegment seg : this.segments) {
            if (seg == null || seg.segType != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel chan : seg.channels) outgoing += chan.outgoingNeutrons;
            for (PileChannel chan : seg.channels) chan.incomingNeutrons += outgoing;
        }

        for (int i = 1; i < this.segments.length - 1; i++) {
            PileSegment seg = this.segments[i];
            if (seg == null || seg.segType != PileChannelType.FUEL) continue;
            double outgoing = 0D;
            for (PileChannel chan : seg.channels) outgoing += chan.outgoingNeutrons;

            spread(i, -1, outgoing);
            spread(i, 1, outgoing);
        }
    }

    /** Walks outward from segment {@code from} in one direction, attenuating through control slices. */
    private void spread(int from, int step, double outgoing) {
        double mult = 1D;
        for (int j = from + step; j >= 1 && j < this.segments.length - 1; j += step) {
            PileSegment neighbor = this.segments[j];
            if (neighbor == null) continue;
            mult *= neighbor.getNeutronMult(this);
            if (neighbor.segType == PileChannelType.FUEL) {
                for (PileChannel chan : neighbor.channels) chan.incomingNeutrons += outgoing * mult;
            }
        }
    }

    private void handleVentilation(Level level) {
        for (PileChannel chan : this.ventilationChannels) {
            if (chan.air <= 0) continue;

            double airCap = (double) chan.air / (double) PileChannel.MAX_AIR;
            for (PileChannel fuel : this.fuelChannels) {
                if (Math.abs(fuel.entry.getPos().getY() - chan.entry.getPos().getY()) <= 1) {
                    fuel.heat *= (1D - airCap * 0.05D);
                }
            }

            chan.air -= (int) Math.ceil(airCap * 5D);

            if (level.getGameTime() % 3L != 0L) continue;

            RandomSource random = level.getRandom();
            CompoundTag data = new CompoundTag();
            data.putFloat("lift", 1F);
            data.putFloat("base", (0.125F + random.nextFloat() * 0.125F) * (float) airCap);
            data.putFloat("max", (float) airCap);
            data.putFloat("strafe", 0.0025F);
            data.putBoolean("noWind", true);
            data.putInt("life", 20 + random.nextInt(30));
            data.putInt("color", 0xa0a0a0);
            HbmEffectNT.Tower.send(level,
                    chan.entry.getPos().getX() + 0.5D + chan.entry.getDir().offsetX * (this.width - 0.375D),
                    chan.entry.getPos().getY() + 0.5D,
                    chan.entry.getPos().getZ() + 0.5D + chan.entry.getDir().offsetZ * (this.width - 0.375D),
                    data);
        }

        for (PileChannel chan : this.fuelChannels) {
            chan.heat *= 0.999D;
            if (chan.heat < 20D) chan.heat = 20D;
        }
    }

    private void handleMeltdown(Level level) {
        this.highestHeat = 0D;
        for (PileChannel chan : this.fuelChannels) {
            if (chan.heat > this.highestHeat) this.highestHeat = chan.heat;
        }

        if (this.highestHeat <= MAX_HEAT || this.fuelChannels.isEmpty()) return;

        destroy();

        double avgX = 0D;
        double avgZ = 0D;
        for (PileChannel chan : this.fuelChannels) {
            avgX += chan.entry.getPos().getX() + 0.5D + chan.entry.getDir().offsetX * (chan.length - 1) / 2D;
            avgZ += chan.entry.getPos().getZ() + 0.5D + chan.entry.getDir().offsetZ * (chan.length - 1) / 2D;
        }
        avgX /= this.fuelChannels.size();
        avgZ /= this.fuelChannels.size();

        meltingDown = true;
        level.explode(null, avgX, worldPosition.getY() + up, avgZ, 15F, true, Level.ExplosionInteraction.TNT);
        meltingDown = false;

        if (pile_debris == null) return;
        for (int i = 0; i < 15; i++) {
            EntityBulletBaseMK4 fragment = new EntityBulletBaseMK4(level, pile_debris, 100F, 0.35F, 0F,
                    (float) (Math.PI / 2D));
            fragment.setPos(avgX, worldPosition.getY() + up + 1, avgZ);
            fragment.setDeltaMovement(0D, level.getRandom().nextDouble() * 0.5D + 1D, 0D);
            level.addFreshEntity(fragment);
        }
    }

    private void recalculateSegments() {
        this.segments = new PileSegment[width];
        assignSegments(fuelChannels, PileChannelType.FUEL);
        assignSegments(controlChannels, PileChannelType.CONTROL);
    }

    private void assignSegments(List<PileChannel> channels, PileChannelType type) {
        for (PileChannel chan : channels) {
            int index = getChannelVerticalIndex(chan);
            if (index < 0 || index >= this.segments.length) continue;

            if (this.segments[index] == null) {
                this.segments[index] = new PileSegment(type).addChan(chan);
            } else if (this.segments[index].segType == type) {
                this.segments[index].addChan(chan);
            }
        }
    }

    private int getChannelVerticalIndex(PileChannel chan) {
        BlockPos entry = chan.entry.getPos();
        ForgeDirection right = chan.entry.getDir().getRotation(ForgeDirection.UP);
        int deltaX = (entry.getX() - worldPosition.getX()) * right.offsetX;
        int deltaZ = (entry.getZ() - worldPosition.getZ()) * right.offsetZ;
        return (deltaX == 0 ? deltaZ : deltaX) + this.left;
    }

    // --- sync + save ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(this.highestHeat);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.highestHeat = buf.readDouble();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("height", height);
        output.putInt("width", width);
        output.putInt("depth", depth);
        output.putInt("left", left);
        output.putInt("right", right);
        output.putInt("up", up);
        com.hbm.util.SavedIds.putEnum(output, "orientation", orientation); // by name, never an ordinal

        writeChannels(output, "fuelChannels", fuelChannels);
        writeChannels(output, "ventChannels", ventilationChannels);
        writeChannels(output, "controlChannels", controlChannels);
    }

    private static void writeChannels(DataOut output, String name, List<PileChannel> channels) {
        DataOut.ChildList list = output.childrenList(name);
        for (PileChannel chan : channels) chan.write(list.addChild());
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        height = input.getIntOr("height", 0);
        width = input.getIntOr("width", 0);
        depth = input.getIntOr("depth", 0);
        left = input.getIntOr("left", 0);
        right = input.getIntOr("right", 0);
        up = input.getIntOr("up", 0);
        orientation = com.hbm.util.SavedIds.getEnum(input, "orientation", PileOrientation.NEITHER);

        segments = new PileSegment[width];
        fuelChannels.clear();
        ventilationChannels.clear();
        controlChannels.clear();

        readChannels(input, "fuelChannels", fuelChannels);
        readChannels(input, "ventChannels", ventilationChannels);
        readChannels(input, "controlChannels", controlChannels);

        recalculateSegments();
    }

    private void readChannels(DataIn input, String name, List<PileChannel> channels) {
        for (DataIn child : input.childrenListOrEmpty(name)) {
            channels.add(readChannel(child));
        }
    }

    private PileChannel readChannel(DataIn input) {
        PileChannel chan = new PileChannel(
                input.getIntOr("x", 0), input.getIntOr("y", 0), input.getIntOr("z", 0),
                com.hbm.util.SavedIds.getEnum(input, "dir", ForgeDirection.DOWN));

        if (chan.type == PileChannelType.FUEL) {
            for (DataIn rod : input.childrenListOrEmpty("rods")) {
                int slot = rod.getIntOr("slot", -1);
                if (slot < 0 || slot >= chan.rods.length) continue;
                rod.read("item", ItemStack.CODEC).ifPresent(stack -> chan.rods[slot] = stack);
            }
            chan.heat = input.getDoubleOr("heat", 0D);
            chan.incomingNeutrons = input.getDoubleOr("neutrons", 0D);
        }
        if (chan.type == PileChannelType.VENTILATION) chan.air = input.getIntOr("air", 0);
        if (chan.type == PileChannelType.CONTROL) chan.control = input.getDoubleOr("control", 1D);

        return chan;
    }

    // --- channels ---

    /** One drilled bore through the graphite: what it holds, how hot it is, and where its mouth is. */
    public class PileChannel {

        public static final int MAX_AIR = 1_000;

        public final DirPos entry;
        public final int length;
        public final PileChannelType type;
        public final ItemStack[] rods;

        public double heat;
        public double outgoingNeutrons;
        public double incomingNeutrons;
        public int air;
        /** How far the control rod in this channel is withdrawn, 1 = fully out. */
        public double control = 1D;

        public PileChannel(int x, int y, int z, ForgeDirection dir) {
            this(x, y, z, dir, -1, null);
        }

        public PileChannel(int x, int y, int z, ForgeDirection dir, int length, PileChannelType type) {
            this.entry = new DirPos(x, y, z, dir);
            this.type = type != null ? type : PileChannelType.getChannelType(dir, orientation);
            this.length = length >= 0 ? length : channelLength(this.type);
            this.rods = new ItemStack[Math.max(0, this.length)];
            java.util.Arrays.fill(this.rods, ItemStack.EMPTY);
        }

        void write(DataOut output) {
            output.putInt("x", entry.getPos().getX());
            output.putInt("y", entry.getPos().getY());
            output.putInt("z", entry.getPos().getZ());
            com.hbm.util.SavedIds.putEnum(output, "dir", entry.getDir());

            if (type == PileChannelType.FUEL) {
                DataOut.ChildList list = output.childrenList("rods");
                for (int i = 0; i < rods.length; i++) {
                    if (rods[i].isEmpty()) continue;
                    DataOut child = list.addChild();
                    child.putInt("slot", i);
                    child.store("item", ItemStack.CODEC, rods[i]);
                }
                output.putDouble("heat", heat);
                output.putDouble("neutrons", incomingNeutrons);
            }
            if (type == PileChannelType.VENTILATION) output.putInt("air", air);
            if (type == PileChannelType.CONTROL) output.putDouble("control", control);
        }

        /** Pushes a rod in at the mouth, shoving whatever is already there one slot deeper. */
        public void loadItem(ItemStack stack) {
            if (stack.isEmpty()) return;
            if (rods.length <= 0) { dropItem(stack, -1); return; }

            for (int i = 0; i < rods.length; i++) {
                if (rods[i].isEmpty()) {
                    rods[i] = stack;
                    return;
                }
                ItemStack previous = rods[i];
                rods[i] = stack;
                stack = previous;
            }
            dropItem(stack, length);
        }

        public void ejectAll() {
            for (int i = 0; i < this.rods.length; i++) {
                dropItem(rods[i], length);
                rods[i] = ItemStack.EMPTY;
            }
        }

        /** Drops a rod out of the channel at {@code depth}, wiping the burn-up it accumulated inside. */
        public void dropItem(ItemStack stack, int depth) {
            if (stack.isEmpty() || level == null) return;

            ItemPileRodMK2.clearDepletion(stack);
            level.addFreshEntity(new ItemEntity(level,
                    entry.getPos().getX() + entry.getDir().offsetX * depth + 0.5D,
                    entry.getPos().getY() + 0.5D,
                    entry.getPos().getZ() + entry.getDir().offsetZ * depth + 0.5D,
                    stack));
        }
    }

    public enum PileOrientation {
        NORTH_SOUTH,
        EAST_WEST,
        NEITHER;

        public static final PileOrientation[] VALUES = values();

        public static PileOrientation getOrientation(ForgeDirection dir) {
            if (dir == ForgeDirection.NORTH || dir == ForgeDirection.SOUTH) return NORTH_SOUTH;
            if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) return EAST_WEST;
            return NEITHER;
        }
    }

    /**
     * What a bore drilled in a given direction becomes. Vertical is always a control channel; horizontal
     * is fuel when it runs along the pile's axis and ventilation when it cuts across it.
     */
    public enum PileChannelType {
        FUEL, VENTILATION, CONTROL;

        public static PileChannelType getChannelType(ForgeDirection channelDir, PileOrientation orientation) {
            if (channelDir == ForgeDirection.UP || channelDir == ForgeDirection.DOWN) return CONTROL;
            return PileOrientation.getOrientation(channelDir) == orientation ? FUEL : VENTILATION;
        }
    }

    /** One vertical slice of the pile, and every channel that enters through it. */
    public static class PileSegment {

        public final List<PileChannel> channels = new ArrayList<>();
        public final PileChannelType segType;

        public PileSegment(PileChannelType segType) {
            this.segType = segType;
        }

        public PileSegment addChan(PileChannel chan) {
            this.channels.add(chan);
            return this;
        }

        /** How much of a neutron flux survives crossing this slice: 1 unless its control rods are in. */
        public double getNeutronMult(TileEntityPileCore core) {
            if (this.segType != PileChannelType.CONTROL) return 1D;
            int size = core.depth - 1;
            if (size < 3) return 0D;

            double total = 0D;
            for (PileChannel chan : channels) total += chan.control;
            return Mth.clamp(total / size, 0D, 0.5D);
        }
    }
}
