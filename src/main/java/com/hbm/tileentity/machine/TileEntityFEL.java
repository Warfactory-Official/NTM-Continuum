package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.inventory.container.ContainerFEL;
import com.hbm.items.machine.ItemFELCrystal;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.main.MainRegistry;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.ContaminationUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Free Electron Laser — a straight port of 1.7.10's {@code TileEntityFEL} (original-hbm), the machine that
 * powers the SILEX.
 *
 * <p>Every tick, while switched on and holding a crystal, it traces a straight line up to <b>24 blocks</b>
 * along its facing at {@code y+1}:
 * <ol>
 *   <li><b>A non-opaque block</b> — the beam passes through and keeps going.</li>
 *   <li><b>A SILEX</b> — if that chamber is aligned on the same axis, sits at least 5 blocks away, and
 *       is the first one since the last gap, its {@code mode} is set to this laser's wavelength.
 *       Otherwise the chamber is <b>destroyed</b> and dropped as an item.</li>
 *   <li><b>Anything else opaque</b> (or TNT) — the beam stops there. Liquids are removed; any block with
 *       explosion resistance below 75 catches fire 1-in-5 per tick, whatever the wavelength (DRX leaves
 *       {@code fire_digamma}, or {@code digamma_matter} on the polaroid-11 world, with digamma ash below).</li>
 * </ol>
 *
 * <p><b>Power is {@code 1250 * 3^ordinal}</b> HE/t (3,750 on infrared up to 303,750 on digamma) out of a
 * 20 MHE buffer. If the buffer cannot cover the draw it is dumped to zero (the mode itself stays set,
 * as in 1.7). The 1.12.2 values (1000 * 4^ordinal, 2 GHE, a per-wavelength burn table) are not the spec.
 *
 * <p>Upstream quirks preserved, flagged at their sites: the client audio, GUI and renderer gates use
 * {@code pow(2)}, the VISIBLE entity case falls through into the IR/UV ignite case, and
 * {@code missingValidSilex} is only ever cleared, never set back.
 */
public class TileEntityFEL extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, WorldlyContainer {

    public static final long maxPower = 20_000_000L;
    /** Base draw; the real cost is {@code powerReq * 3^mode.ordinal()} (0 for NULL). */
    public static final int powerReq = 1250;
    /** Hard cap on beam length, in blocks. */
    public static final int RANGE = 24;

    public long power;
    public EnumWavelengths mode = EnumWavelengths.NULL;
    public boolean isOn;
    /**
     * True until a valid SILEX has ever been hit. <b>Upstream never resets it to true</b>, so once the
     * laser has found a chamber the GUI's "ERR." indicator stays off for good. Kept verbatim.
     */
    public boolean missingValidSilex = true;
    /** How far the beam reaches this tick — drives the renderer and the client audio gate. */
    public int distance;

    // client-only
    private int prevDistance;
    public int audioDuration;

    public TileEntityFEL(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_FEL.get(), pos, state, 2);
    }

    @Override
    public String getDefaultName() {
        return "container.machineFEL";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFEL te) {
        ForgeDirection dir = te.facing();
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);

        ItemStack crystal = te.inventory.getStackInSlot(1);
        te.mode = (te.isOn && crystal.getItem() instanceof ItemFELCrystal c) ? c.wavelength : EnumWavelengths.NULL;

        int req = requiredPower(te.mode);

        if (te.isOn && te.mode != EnumWavelengths.NULL && te.power < req) {
            te.power = 0;
        }

        if (te.isOn && te.power >= req && te.mode != EnumWavelengths.NULL) {
            te.hurtEntitiesInBeam(level, dir);
            te.power -= req;
            te.trace(level, dir);
        }

        te.networkPackNT();
    }

    /** 1.7: {@code (int) (powerReq * (ordinal == 0 ? 0 : 3^ordinal))}. */
    public static int requiredPower(EnumWavelengths mode) {
        return (int) (powerReq * ((mode.ordinal() == 0) ? 0 : Math.pow(3, mode.ordinal())));
    }

    /** Everything living between the emitter and the beam's end takes the wavelength's effect. */
    private void hurtEntitiesInBeam(Level level, ForgeDirection dir) {
        double x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        int reach = distance - 1;

        double blx = Math.min(x, x + dir.offsetX * reach) + 0.2;
        double bux = Math.max(x, x + dir.offsetX * reach) + 0.8;
        double bly = Math.min(y, 1 + y + dir.offsetY * reach) + 0.2;
        double buy = Math.max(y, 1 + y + dir.offsetY * reach) + 0.8;
        double blz = Math.min(z, z + dir.offsetZ * reach) + 0.2;
        double buz = Math.max(z, z + dir.offsetZ * reach) + 0.8;

        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(blx, bly, blz, bux, buy, buz));
        for (LivingEntity entity : list) {
            switch (mode) {
                // 1.7 switch fall-through, kept: VISIBLE blinds AND falls into the IR/UV ignite case.
                case VISIBLE -> {
                    dev.golem.core.world.Effects.add(entity, MobEffects.BLINDNESS, 60 * 60 * 65536, 0);
                    dev.golem.core.world.Effects.ignite(entity, 10);
                }
                case IR, UV -> dev.golem.core.world.Effects.ignite(entity, 10);
                case GAMMA -> ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION,
                        ContaminationUtil.ContaminationType.CREATIVE, 25);
                case DRX -> ContaminationUtil.applyDigammaData(entity, 0.1F);
                default -> { }
            }
        }
    }

    /** Walks the beam from 3 blocks out to {@link #RANGE}, driving SILEX chambers and burning blocks. */
    private void trace(Level level, ForgeDirection dir) {
        boolean silexSpacing = false;
        int felMeta = MachineCore.facingOrdinal(getBlockState(), 2);

        for (int i = 3; i < RANGE; i++) {
            BlockPos beam = new BlockPos(worldPosition.getX() + dir.offsetX * i,
                    worldPosition.getY() + 1,
                    worldPosition.getZ() + dir.offsetZ * i);
            BlockState b = level.getBlockState(beam);

            // The SILEX check comes FIRST. Upstream relied on the chamber being Material.IRON (opaque)
            // to reach this branch, but the port renders every multiblock cell as INVISIBLE with
            // noOcclusion(), so an opacity test would let the beam pass straight through it. Testing
            // for the block explicitly is equivalent for the original and correct for the port.
            if (b.is(ModMachineBlocks.machine_silex.get())) {
                // Upstream looks up the BE one block FURTHER along the beam and at the FEL's own y —
                // which is exactly where a SILEX core sits, since the beam grazes its y+1 dummy.
                BlockPos silexPos = new BlockPos(beam.getX() + dir.offsetX, worldPosition.getY(),
                        beam.getZ() + dir.offsetZ);
                if (level.getBlockEntity(silexPos) instanceof TileEntitySILEX silex) {
                    int silexMeta = MachineCore.facingOrdinal(silex.getBlockState(), 2);
                    if (rotationIsValid(silexMeta, felMeta) && i >= 5 && !silexSpacing) {
                        if (silex.mode != this.mode) {
                            silex.mode = this.mode;
                            this.missingValidSilex = false;
                            silexSpacing = true;
                        }
                    } else {
                        // Wrong rotation, too close, or a second chamber with no gap: destroy it.
                        level.removeBlock(silexPos, false);
                        net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                                level, beam.getX() + 0.5, beam.getY() + 0.5, beam.getZ() + 0.5,
                                new ItemStack(ModMachineBlocks.machine_silex.get()));
                        level.addFreshEntity(drop);
                    }
                }
                continue;
            }

            // Empty space (or anything non-opaque): the beam reaches its full range and the "one SILEX
            // per run" latch clears, which is what allows a LINE of chambers with gaps between them.
            if (!isOpaque(b, level, beam) && !b.is(Blocks.TNT)) {
                this.distance = RANGE;
                silexSpacing = false;
                continue;
            }

            // 1.7: the first opaque block (or TNT) stops the beam.
            this.distance = i;
            burn(level, beam, b);
            break;
        }
    }

    /**
     * The 26.1 stand-in for 1.7's {@code Material.isOpaque()}, which was "blocks movement AND is not
     * translucent": a full-cube collision shape that also occludes. Stone qualifies; glass, torches and
     * air do not — so, exactly as upstream, the beam passes straight through glass and liquids (which
     * makes 1.7's liquid branch in {@link #burn} unreachable; kept for fidelity).
     */
    private static boolean isOpaque(BlockState state, Level level, BlockPos pos) {
        return state.canOcclude() && state.isCollisionShapeFullBlock(level, pos);
    }

    /**
     * 1.7's single burn rule at the block that stopped the beam: a liquid is removed; otherwise a block
     * with explosion resistance below 75 is set alight 1-in-5 (vanilla fire, or for DRX digamma fire /
     * digamma matter on the polaroid-11 world plus digamma ash below). "Hardness" is the block's
     * <b>explosion resistance</b>, matching the old {@code getExplosionResistance(null)}.
     */
    private void burn(Level level, BlockPos pos, BlockState b) {
        if (!b.getFluidState().isEmpty() && b.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            lavaHiss(level, pos);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            return;
        }

        float hardness = b.getBlock().getExplosionResistance();
        if (hardness < 75 && level.getRandom().nextInt(5) == 0) {
            lavaHiss(level, pos);
            BlockState fire = mode != EnumWavelengths.DRX ? Blocks.FIRE.defaultBlockState()
                    : (MainRegistry.polaroidID == 11 ? ModBombBlocks.digamma_matter.get() : ModBombBlocks.fire_digamma.get()).defaultBlockState();
            level.setBlockAndUpdate(pos, fire);
            if (mode == EnumWavelengths.DRX) {
                level.setBlockAndUpdate(pos.below(), ModFallingBlocks.ash_digamma.get().defaultBlockState());
            }
        }
    }

    private static void lavaHiss(Level level, BlockPos pos) {
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /**
     * A SILEX only accepts the beam if it lies on the SAME axis — facing either into it or away.
     * Public so {@code FelGameTests} can pin the rule directly.
     */
    public static boolean rotationIsValid(int silexMeta, int felMeta) {
        ForgeDirection silexDir = ForgeDirection.getOrientation(silexMeta);
        ForgeDirection felDir = ForgeDirection.getOrientation(felMeta);
        return silexDir == felDir || silexDir == felDir.getOpposite();
    }

    private ForgeDirection facing() {
        return MachineCore.facing(getBlockState());
    }

    /**
     * The single power port: 5 blocks <b>behind</b> the core at {@code y+1}, one cell past the machine's
     * rearmost cell (the "extra" dummy {@link com.hbm.blocks.machine.MachineFEL#fillSpace} places at
     * {@code core + facing·−4}).
     *
     * <p><b>Deliberate deviation.</b> Upstream passed {@code dir} (the facing) as this port's direction,
     * but a {@code DirPos}'s direction means "the machine's side that faces the connecting cable" —
     * that is what the graph inverts when it probes a conductor's face, and what
     * {@code IEnergyConnectorBlock.isPort} matches against. Since the port sits behind the
     * machine, that direction is {@code facing.getOpposite()}, not {@code facing}; every other ported
     * machine (assembly, crystallizer, …) already uses the outward convention. With upstream's value a
     * cable placed at the port <b>fails to connect</b>: {@code isPort} rejects the direction mismatch,
     * so the cable neither renders as attached nor reports a connection. (In 1.12 the functional half
     * happened to survive because a plain cable's {@code canConnect} returns true for every side.)
     */
    public DirPos[] getConPos() {
        ForgeDirection dir = facing();
        return new DirPos[]{new DirPos(worldPosition.getX() + dir.offsetX * -5, worldPosition.getY() + 1,
                worldPosition.getZ() + dir.offsetZ * -5, dir.getOpposite())};
    }

    /**
     * The audio ramp: it climbs by 2 per tick while firing and falls by 3 when not, clamped 0–60, and
     * the loop only plays above 10 — so the laser winds up and down instead of clicking on and off.
     * Pitch tracks the ramp ({@code (n − 10)/100 + 0.5}).
     */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFEL te) {
        if (te.prevDistance != te.distance) {
            te.prevDistance = te.distance;
            level.setBlocksDirty(pos, state, state); // the beam length changed -> re-render
        }

        // NOTE: pow(2), not the server's 3^ordinal — 1.7's inconsistency, preserved.
        boolean firing = te.power > powerReq * Math.pow(2, te.mode.ordinal())
                && te.isOn && te.mode != EnumWavelengths.NULL && te.distance - 3 > 0;
        te.audioDuration = net.minecraft.util.Mth.clamp(te.audioDuration + (firing ? 2 : -3), 0, 60);
        com.hbm.client.sound.FELSoundHandler.tickLoop(te);
    }

    public boolean isAudioPlaying() { return audioDuration > 10; }
    public float audioPitch() { return (audioDuration - 10) / 100F + 0.5F; }

    /** Slot 0 is the battery bay, slot 1 the laser crystal. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> Library.isBattery(stack);
            case 1 -> stack.getItem() instanceof ItemFELCrystal;
            default -> false;
        };
    }

    private static final int[] IO_SLOTS = {0, 1};

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isItemValidForSlot(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return true; }

    /** The GUI's power switch. Upstream keys it on {@code meta == 2}; the menu sends that id. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        if (meta == 2) {
            this.isOn = !this.isOn;
            setChanged();
        }
    }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm.machine_fel"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerFEL(containerId, playerInventory, worldPosition);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    /**
     * NOTE: upstream's {@code serialize} deliberately does <b>not</b> call {@code super.serialize}
     * (unlike the SILEX's), so the FEL never syncs its inventory over the BufPacket. Preserved — the
     * menu handles the slots.
     */
    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeLong(power);
        buf.writeUtf(mode.name());
        buf.writeBoolean(isOn);
        buf.writeBoolean(missingValidSilex);
        buf.writeInt(distance);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        power = buf.readLong();
        mode = EnumWavelengths.byName(buf.readUtf());
        isOn = buf.readBoolean();
        missingValidSilex = buf.readBoolean();
        distance = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putString("mode", mode.name());
        output.putBoolean("isOn", isOn);
        output.putBoolean("valid", missingValidSilex); // the 1.12.2 key
        output.putInt("distance", distance);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        mode = EnumWavelengths.byName(input.getStringOr("mode", EnumWavelengths.NULL.name()));
        isOn = input.getBooleanOr("isOn", false);
        missingValidSilex = input.getBooleanOr("valid", true);
        distance = input.getIntOr("distance", 0);
    }
}
