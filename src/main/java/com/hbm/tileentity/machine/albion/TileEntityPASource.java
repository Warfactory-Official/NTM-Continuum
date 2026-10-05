package com.hbm.tileentity.machine.albion;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.NTMItemHandler;
import com.hbm.inventory.container.ContainerPASource;
import com.hbm.lib.DirPos;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.lib.Library;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Head of the Albion particle accelerator: it fires the beam and owns the particle while it travels.
 * Two input particles are consumed to make a {@link Particle}, which is then walked one component at a
 * time by {@link #steppy()} — each {@link IParticleUser} it lands on decides whether the beam may enter,
 * modifies its momentum/defocus, and hands back the next cell. The run ends when the detector matches a
 * recipe ({@link PAState#SUCCESS}) or when the beam crashes, which is what every {@code CRASH_*} state
 * records for the GUI.
 *
 * <p>Ported from 1.12.2's {@code albion/TileEntityPASource}.
 *
 * <p>Deviations from CE: sided automation exposes the two input and two output slots to every face
 * rather than routing the red and yellow inputs to their own cells of the footprint — Continuum's
 * {@code WorldlyContainer} port asks per <i>face</i>, not per <i>cell</i>, so CE's positional
 * {@code getAccessibleSlotsFromSide(side, pos)} has nowhere to hook. Insert/extract are still gated to
 * the correct slots, so only which cell of the machine a hopper may sit on changes. The OpenComputers
 * component {@code ntm_pa_source} is served to CC: Tweaked by
 * {@code com.hbm.compat.computercraft.albion.PASourcePeripheral}.
 */
public class TileEntityPASource extends TileEntityCooledBase
        implements MenuProvider, IControlReceiver, WorldlyContainer, IRORValueProvider, IRORInteractive {

    public static final long usage = 100_000;

    /** The slots a hopper may touch: both particle inputs and both outputs. */
    private static final int[] IO_SLOTS = {1, 2, 3, 4};

    public @Nullable Particle particle;
    public PAState state = PAState.IDLE;

    public int lastSpeed;
    public int debugSpeed;

    public enum PAState {
        IDLE(0x8080ff),
        RUNNING(0xffff00),
        SUCCESS(0x00ff00),
        PAUSE_UNLOADED(0x808080),
        CRASH_DEFOCUS(0xff0000),
        CRASH_DERAIL(0xff0000),
        CRASH_CANNOT_ENTER(0xff0000),
        CRASH_NOCOOL(0xff0000),
        CRASH_NOPOWER(0xff0000),
        CRASH_NOCOIL(0xff0000),
        CRASH_OVERSPEED(0xff0000),
        CRASH_UNDERSPEED(0xff0000),
        CRASH_NORECIPE(0xff0000);

        public static final PAState[] VALUES = values();

        public final int color;

        PAState(int color) {
            this.color = color;
        }
    }

    public TileEntityPASource(BlockPos pos, BlockState state) {
        super(com.hbm.tileentity.ModBlockEntities.PA_SOURCE.get(), pos, state, 5);
    }

    /** One item per slot, like the 1.12.2 tile: the beam takes a single particle from each input. */
    @Override
    protected NTMItemHandler createInventory(int slotCount) {
        return new NTMItemHandler(slotCount, 1) {
            @Override
            protected void onContentsChanged(int slot) {
                TileEntityPASource.this.setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return TileEntityPASource.this.isItemValidForSlot(slot, stack);
            }
        };
    }

    @Override
    public String getDefaultName() {
        return "container.paSource";
    }

    @Override
    public long getMaxPower() {
        return 10_000_000;
    }

    public void updateState(PAState state) {
        this.state = state;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPASource te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.getMaxPower());

        int steps = 1;
        if (te.particle != null) steps = 1 + Mth.clamp(te.particle.momentum / 1_000, 0, 9);

        for (int i = 0; i < steps; i++) {
            if (te.particle != null) {
                te.state = PAState.RUNNING;
                te.steppy();
                te.debugSpeed = te.particle.momentum;
                if (te.particle.invalid) te.particle = null;
            } else if (te.power >= usage && !te.inventory.getStackInSlot(1).isEmpty()
                    && !te.inventory.getStackInSlot(2).isEmpty()) {
                te.tryRun();
                break;
            }
        }

        te.exchangeCoolant(level);
        te.updateCooling();
        te.networkPackNT();
    }

    /** Advances the beam by one component. */
    public void steppy() {
        if (particle == null || level == null) return;

        if (!level.hasChunkAt(particle.pos)) {
            this.state = PAState.PAUSE_UNLOADED;
            return;
        }

        BlockPos core = MultiblockSurface.coreOfAny(level, particle.pos);
        if (core == null) {
            particle.crash(PAState.CRASH_DERAIL);
            return;
        }

        BlockEntity tile = level.getBlockEntity(core);
        if (!(tile instanceof IParticleUser pa)) {
            particle.crash(PAState.CRASH_DERAIL);
            return;
        }

        if (!pa.canParticleEnter(particle, particle.dir, particle.pos)) {
            particle.crash(PAState.CRASH_CANNOT_ENTER);
            return;
        }

        pa.onEnter(particle, particle.dir);
        BlockPos exit = pa.getExitPos(particle);
        if (exit != null) particle.move(exit);
    }

    /** Consumes both inputs and injects a fresh particle five blocks along the beamline. */
    public void tryRun() {
        if (!isCool()) return;

        ItemStack slot1 = inventory.getStackInSlot(1);
        ItemStack slot2 = inventory.getStackInSlot(2);
        ItemStack slot3 = inventory.getStackInSlot(3);
        ItemStack slot4 = inventory.getStackInSlot(4);

        ItemStack remainder1 = Compat.craftingRemainder(slot1);
        ItemStack remainder2 = Compat.craftingRemainder(slot2);

        if (!remainder1.isEmpty() && !slot3.isEmpty()) return;
        if (!remainder2.isEmpty() && !slot4.isEmpty()) return;

        if (!remainder1.isEmpty()) inventory.setStackInSlot(3, remainder1.copy());
        if (!remainder2.isEmpty()) inventory.setStackInSlot(4, remainder2.copy());

        this.power -= usage;
        ForgeDirection rot = getBeamDir();
        this.particle = new Particle(this,
                worldPosition.offset(rot.offsetX * 5, 0, rot.offsetZ * 5), rot, slot1, slot2);
        inventory.setStackInSlot(1, ItemStack.EMPTY);
        inventory.setStackInSlot(2, ItemStack.EMPTY);
        this.markChanged();
    }

    /** The direction the beam leaves in — the machine's facing rotated a quarter turn, as in 1.12.2. */
    private ForgeDirection getBeamDir() {
        return MachineCore.facing(getBlockState())
                .getRotation(ForgeDirection.DOWN);
    }

    /**
     * Deviation from CE: the three ports under the machine face {@link Library#NEG_Y} rather than the
     * machine's own horizontal facing. They sit one block below the footprint, so down is the face a
     * cable there actually touches, and upstream's horizontal value makes the block-level connection
     * check disagree with the block entity's — the cable powers the machine but never snaps to it. Every
     * other cooled part of the accelerator already spells its vertical ports this way.
     */
    @Override
    public List<DirPos> getConPos() {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos p = worldPosition;
        List<DirPos> list = new ArrayList<>(10);
        list.add(new DirPos(p.offset(dir.offsetX * 2, 0, dir.offsetZ * 2), dir));
        list.add(new DirPos(p.offset(dir.offsetX * 2 + rot.offsetX * 2, 0, dir.offsetZ * 2 + rot.offsetZ * 2), dir));
        list.add(new DirPos(p.offset(dir.offsetX * 2 - rot.offsetX * 2, 0, dir.offsetZ * 2 - rot.offsetZ * 2), dir));
        list.add(new DirPos(p.offset(-dir.offsetX * 2, 0, -dir.offsetZ * 2), dir.getOpposite()));
        list.add(new DirPos(p.offset(-dir.offsetX * 2 + rot.offsetX * 2, 0, -dir.offsetZ * 2 + rot.offsetZ * 2), dir.getOpposite()));
        list.add(new DirPos(p.offset(-dir.offsetX * 2 - rot.offsetX * 2, 0, -dir.offsetZ * 2 - rot.offsetZ * 2), dir.getOpposite()));
        list.add(new DirPos(p.offset(rot.offsetX * 5, 0, rot.offsetZ * 5), rot));
        list.add(new DirPos(p.offset(0, -2, 0), Library.NEG_Y));
        list.add(new DirPos(p.offset(rot.offsetX * 2, -2, rot.offsetZ * 2), Library.NEG_Y));
        list.add(new DirPos(p.offset(-rot.offsetX * 2, -2, -rot.offsetZ * 2), Library.NEG_Y));
        return list;
    }

    // --- inventory ---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        return slot == 1 || slot == 2;
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1 || slot == 2; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 3 || slot == 4; }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return IO_SLOTS;
    }

    // --- sync + save ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(debugSpeed);
        buf.writeByte(this.state.ordinal());
        buf.writeInt(this.lastSpeed);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        debugSpeed = buf.readInt();
        state = PAState.VALUES[Mth.clamp(buf.readByte(), 0, PAState.VALUES.length - 1)];
        this.lastSpeed = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("hasParticle", particle != null);
        if (particle == null) return;

        DataOut child = output.child("particle");
        child.putInt("x", particle.pos.getX());
        child.putInt("y", particle.pos.getY());
        child.putInt("z", particle.pos.getZ());
        com.hbm.util.SavedIds.putEnum(child, "dir", particle.dir);
        child.putInt("momentum", particle.momentum);
        child.putInt("defocus", particle.defocus);
        child.putInt("dist", particle.distanceTraveled);
        child.store("input1", ItemStack.CODEC, particle.input1);
        child.store("input2", ItemStack.CODEC, particle.input2);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        if (!input.getBooleanOr("hasParticle", false)) return;

        DataIn child = input.childOrEmpty("particle");
        BlockPos pos = new BlockPos(child.getIntOr("x", 0), child.getIntOr("y", 0), child.getIntOr("z", 0));
        ForgeDirection dir = com.hbm.util.SavedIds.getEnum(child, "dir", ForgeDirection.DOWN);
        this.particle = new Particle(this, pos, dir,
                child.read("input1", ItemStack.CODEC).orElse(ItemStack.EMPTY),
                child.read("input2", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.particle.momentum = child.getIntOr("momentum", 0);
        this.particle.defocus = child.getIntOr("defocus", 0);
        this.particle.distanceTraveled = child.getIntOr("dist", 0);
    }

    // --- GUI ---

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerPASource(id, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.getBooleanOr(data, "cancel", false)) {
            this.particle = null;
            this.state = PAState.IDLE;
        }
    }

    // --- the travelling beam ---

    public static class Particle {

        private final TileEntityPASource source;
        public BlockPos pos;
        public ForgeDirection dir;
        public int momentum;
        public int defocus;
        public int distanceTraveled;
        public static final int maxDefocus = 1000;
        public boolean invalid = false;

        public ItemStack input1;
        public ItemStack input2;

        public Particle(TileEntityPASource source, BlockPos pos, ForgeDirection dir, ItemStack input1, ItemStack input2) {
            this.source = source;
            this.pos = pos;
            this.dir = dir;
            this.input1 = input1;
            this.input2 = input2;
        }

        public void crash(PAState state) {
            this.invalid = true;
            this.source.updateState(state);
        }

        public void move(BlockPos pos) {
            this.pos = pos;
            this.source.lastSpeed = this.momentum;
        }

        public void addDistance(int dist) {
            this.distanceTraveled += dist;
        }

        public void resetDistance() {
            this.distanceTraveled = 0;
        }

        public void defocus(int amount) {
            this.defocus += amount;
            if (this.defocus > maxDefocus) this.crash(PAState.CRASH_DEFOCUS);
        }

        public void focus(int amount) {
            this.defocus -= amount;
            if (this.defocus < 0) this.defocus = 0;
        }
    }

    // --- redstone over radio ---

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "status",
                PREFIX_VALUE + "momentum",
                PREFIX_VALUE + "defocus",
                PREFIX_VALUE + "temperature",
                PREFIX_VALUE + "pfmcold",
                PREFIX_VALUE + "pfm",
                PREFIX_FUNCTION + "cancel"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "status").equals(name)) return "" + this.state;
        if ((PREFIX_VALUE + "momentum").equals(name)) return "" + this.lastSpeed;
        if ((PREFIX_VALUE + "defocus").equals(name)) return this.particle != null ? "" + this.particle.defocus : "0";
        if ((PREFIX_VALUE + "temperature").equals(name)) return "" + (int) this.temperature;
        if ((PREFIX_VALUE + "pfmcold").equals(name)) return "" + coolantTanks[0].getFill();
        if ((PREFIX_VALUE + "pfm").equals(name)) return "" + coolantTanks[1].getFill();
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "cancel").equals(name)) {
            particle = null;
            state = PAState.IDLE;
        }
        return null;
    }
}
