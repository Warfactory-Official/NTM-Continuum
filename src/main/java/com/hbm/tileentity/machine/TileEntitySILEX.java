package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.ContainerSILEX;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SILEXRecipes;
import com.hbm.inventory.recipes.SILEXRecipes.SILEXRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;

/**
 * SILEX — Separation of Isotopes by Laser EXcitation (1.12.2 {@code TileEntitySILEX}).
 *
 * <p>The chamber dissolves an isotope-bearing item into a pool of "solution" using peroxide, then a
 * {@code machine_fel} firing through it sorts the isotopes out. It consumes <b>no power</b>; all the
 * energy comes from the laser.
 *
 * <p><b>The FEL link is not a connection — it is a push, every tick.</b> {@link #mode} is reset to
 * {@link EnumWavelengths#NULL} at the end of every server tick, and any FEL whose beam passes through
 * re-asserts it on its own tick. That is why an unpowered or misaimed laser stops the chamber
 * instantly, and why the two block entities' tick order is load-bearing. Preserved verbatim.
 *
 * <p>Slots: <b>0</b> input · <b>1</b> fluid identifier · <b>2–3</b> fluid container bays ·
 * <b>4</b> output · <b>5–10</b> output queue.
 */
public class TileEntitySILEX extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardReceiverMK2, WorldlyContainer {

    /** Maximum solution held, in mB. */
    public static final int maxFill = 16_000;
    /** Ticks of progress needed per output (1.7.10: 100; at an exactly-matched wavelength 100 ticks). */
    public final int processTime = 100;
    /** 1.7.10 deterministic output cycling: the index advances by {@link #PRIME} per output. */
    public static final int PRIME = 137;
    public int recipeIndex = 0;

    /** Sided access: the input plus the whole queue. Note slot 4 (the staging output) is NOT exposed. */
    private static final int[] IO_SLOTS = {0, 5, 6, 7, 8, 9, 10};

    /** Pushed in by the FEL each tick, cleared at the end of each tick. Never persists on its own. */
    public EnumWavelengths mode = EnumWavelengths.NULL;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.PEROXIDE, maxFill);
    /** What the current solution pool is made of, as a one-item stack. Null when the pool is empty. */
    public @Nullable ItemStack current;
    public int currentFill;
    public int progress;

    /** Wraps at 20; the item→solution dissolve step only fires on the tick it hits 0. */
    private int loadDelay;

    public TileEntitySILEX(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SILEX.get(), pos, state, 11);
        SILEXRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.machineSILEX";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntitySILEX te) {
        te.updateTankType();
        te.fillTankFromSlots();


        te.loadFluid();

        if (!te.process()) te.progress = 0;

        te.dequeue();

        if (te.currentFill <= 0) te.current = null;

        te.networkPackNT();

        // The laser link is re-asserted from the FEL's own tick; without one, the chamber goes dark.
        te.mode = EnumWavelengths.NULL;
    }

    // ---------------------------------------------------------------- solution loading

    /**
     * Two ways solution enters the pool:
     * <ol>
     *   <li><b>Directly from the tank</b>, at up to 50 mB/tick, when the tank holds a fluid that is
     *       itself a SILEX input (UF6, PUF6, DEATH, VITRIOL, REDMUD). The pool is then "made of" that
     *       fluid, represented as a {@code fluid_icon} stack.</li>
     *   <li><b>By dissolving an item</b> from slot 0 — but only when the tank holds <b>peroxide</b>,
     *       and only on the tick {@code loadDelay} wraps to 0 (so once every 20 ticks). One item
     *       becomes {@code recipe.fluidProduced} mB of solution and costs the same amount of peroxide.</li>
     * </ol>
     * A pool can only ever hold one substance: a second input is refused until the pool drains.
     */
    private void loadFluid() {
        FluidType tankType = tank.getTankType();

        // path 1 — the tank's own fluid is a recipe input
        if (tankType != Fluids.NONE && tankType != Fluids.PEROXIDE) {
            ItemStack direct = ItemFluidIcon.make(tankType);
            if (SILEXRecipes.getOutput(direct) != null) {
                if (currentFill == 0) current = direct.copy();
                if (current != null && sameSubstance(current, direct)) {
                    int toFill = Math.min(50, Math.min(maxFill - currentFill, tank.getFill()));
                    currentFill += toFill;
                    tank.setFill(tank.getFill() - toFill);
                }
            }
        }

        loadDelay++;
        if (loadDelay > 20) loadDelay = 0;
        if (loadDelay != 0) return;

        // path 2 — dissolve one item, peroxide only
        ItemStack in = inventory.getStackInSlot(0);
        if (in.isEmpty() || tankType != Fluids.PEROXIDE) return;
        ItemStack single = dev.golem.core.world.ItemStacks.copyWithCount(in, 1);
        if (current != null && !sameSubstance(current, single)) return;

        SILEXRecipe recipe = SILEXRecipes.getOutput(in);
        if (recipe == null) return;

        int load = recipe.fluidProduced;
        if (currentFill + load > maxFill) return;
        if (tank.getFill() < load) return;

        if (currentFill == 0) current = single;
        currentFill += load;
        tank.setFill(tank.getFill() - load);
        in.shrink(1);
        setChanged();
    }

    // ---------------------------------------------------------------- processing

    /**
     * One tick of laser separation. Returns whether progress was made — a {@code false} resets
     * {@code progress} to 0 in {@link #serverTick}, so interrupting the beam loses the partial roll.
     *
     * <p>The speed formula is upstream's, including its integer truncation:
     * <pre>progress += Math.pow(2, mode.strength() - recipe.laserStrength.strength() + 1) / 2</pre>
     * At an exact wavelength match that is {@code 2^1 / 2 = 1} per tick; one step stronger gives 2,
     * two steps 4, and so on. Because it is a {@code double} added into an {@code int}, the value is
     * truncated — which is invisible here only because every result is already a whole number.
     */
    private boolean process() {
        if (current == null || currentFill <= 0) return false;

        SILEXRecipe recipe = SILEXRecipes.getOutput(current);
        if (recipe == null) return false;
        // the loaded wavelength must be at least as strong as the recipe demands
        if (recipe.laserStrength.strength() > mode.strength()) return false;
        if (currentFill < recipe.fluidConsumed) return false;
        if (!inventory.getStackInSlot(4).isEmpty()) return false;

        progress += (int) (Math.pow(2, mode.strength() - recipe.laserStrength.strength() + 1) / 2);

        if (progress >= processTime) {
            currentFill -= recipe.fluidConsumed;
            // 1.7.10 does not roll randomly: it walks the weighted table with recipeIndex (+137 per output).
            int totalWeight = recipe.totalWeight();
            recipeIndex %= Math.max(totalWeight, 1);
            int weight = 0;
            for (SILEXRecipes.WeightedOutput weighted : recipe.outputs) {
                weight += weighted.weight();
                if (recipeIndex < weight) {
                    inventory.setStackInSlot(4, weighted.stack().copy());
                    break;
                }
            }
            progress = 0;
            setChanged();
            recipeIndex += PRIME;
        }
        return true;
    }

    /**
     * Moves the finished item out of the staging slot 4 into the 5–10 queue: into a matching partial
     * stack first, else the first empty slot. If the queue is full the item stays in slot 4, which
     * stalls {@link #process} — the machine's "output full" state.
     */
    private void dequeue() {
        ItemStack out = inventory.getStackInSlot(4);
        if (out.isEmpty()) return;

        for (int i = 5; i <= 10; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot.isEmpty()) continue;
            if (!dev.golem.core.world.ItemStacks.sameItemAndData(slot, out)) continue;
            int space = slot.getMaxStackSize() - slot.getCount();
            if (space <= 0) continue;
            int moved = Math.min(space, out.getCount());
            slot.grow(moved);
            out.shrink(moved);
            if (out.isEmpty()) {
                inventory.setStackInSlot(4, ItemStack.EMPTY);
                setChanged();
                return;
            }
        }
        for (int i = 5; i <= 10; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) continue;
            inventory.setStackInSlot(i, out.copy());
            inventory.setStackInSlot(4, ItemStack.EMPTY);
            setChanged();
            return;
        }
    }

    /** Whether two one-item stacks represent the same dissolvable substance (item + fluid identity). */
    private static boolean sameSubstance(ItemStack a, ItemStack b) {
        if (a.getItem() != b.getItem()) return false;
        if (a.getItem() instanceof ItemFluidIcon) {
            return ItemFluidIcon.getFluid(a) == ItemFluidIcon.getFluid(b);
        }
        return true;
    }

    // ---------------------------------------------------------------- fluid plumbing

    /** Slot 1 holds a fluid identifier; the tank adopts its type (the 1.12.2 {@code setType(1, 1)}). */
    private void updateTankType() {
        ItemStack id = inventory.getStackInSlot(1);
        if (id.getItem() instanceof IItemFluidId ident) {
            FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
            if (t != tank.getTankType()) {
                tank.setTankType(t);
                setChanged();
            }
        }
    }

    /** Drains a full container in slot 2 into the tank; the emptied container goes to slot 3. */
    private void fillTankFromSlots() {
        ItemStack in = inventory.getStackInSlot(2);
        if (in.isEmpty()) return;

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);
            if (held <= 0 || heldType == Fluids.NONE) return;
            int addable = tank.fill(heldType, held, false);
            if (addable <= 0) return;
            ItemStack work = dev.golem.core.world.ItemStacks.copyWithCount(in, 1);
            int taken = fillable.tryEmpty(heldType, addable, work);
            if (taken <= 0 || !fits(3, work)) return;
            tank.fill(heldType, taken, true);
            moveResult(work);
        } else if (in.getItem() instanceof ItemFluidBucket) {
            FluidType fluid = ItemFluidBucket.getFluid(in);
            if (fluid == Fluids.NONE) return;
            if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!fits(3, empty)) return;
            tank.fill(fluid, ItemFluidBucket.VOLUME, true);
            moveResult(empty);
        }
    }

    private boolean fits(int slot, ItemStack stack) {
        ItemStack there = inventory.getStackInSlot(slot);
        if (there.isEmpty()) return true;
        return dev.golem.core.world.ItemStacks.sameItemAndData(there, stack)
                && there.getCount() + stack.getCount() <= there.getMaxStackSize();
    }

    private void moveResult(ItemStack result) {
        ItemStack there = inventory.getStackInSlot(3);
        if (there.isEmpty()) inventory.setStackInSlot(3, result);
        else there.grow(result.getCount());
        inventory.getStackInSlot(2).shrink(1);
        setChanged();
    }

    /**
     * The two fluid ports: {@code ±2} blocks out along the axis PERPENDICULAR to the machine's facing,
     * at {@code y+1} — the two cells {@code MachineSILEX.visitCells} opens.
     */
    public DirPos[] getConPos() {
        ForgeDirection dir = ForgeDirection.getOrientation(
                getBlockState().getValue(BlockMultiblockCore.FACING)).getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + dir.offsetX * 2, y + 1, z + dir.offsetZ * 2, dir),
                new DirPos(x - dir.offsetX * 2, y + 1, z - dir.offsetZ * 2, dir.getOpposite())};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tank};
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{tank};
    }

    // ---------------------------------------------------------------- inventory rules

    /**
     * Slot 0 accepts anything a recipe can dissolve, 1 a fluid identifier, 2 a fluid container, 3 the
     * emptied container (so the machine's own transfer can place it — the player is blocked by the
     * take-only slot in {@link ContainerSILEX}). Slot 4 is the staging output and 5–10 are the queue:
     * both are written by the machine only, never inserted into.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> SILEXRecipes.getOutput(stack) != null;
            case 1 -> stack.getItem() instanceof IItemFluidId;
            case 2 -> stack.getItem() instanceof IFillableItem || stack.getItem() instanceof ItemFluidBucket;
            case 3 -> true;
            default -> false;
        };
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }

    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && isItemValidForSlot(slot, stack);
    }

    /** Automation may only pull finished goods out of the queue — never the raw input. */
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 5; }

    // ---------------------------------------------------------------- GUI scaling

    public int getProgressScaled(int i) { return (progress * i) / processTime; }
    public int getFluidScaled(int i) { return tank.getMaxFill() == 0 ? 0 : (tank.getFill() * i) / tank.getMaxFill(); }
    public int getFillScaled(int i) { return (currentFill * i) / maxFill; }

    /** The GUI's "void contents" button — dumps the solution pool. Upstream ignores value and meta. */
    @Override
    public void handleButtonPacket(int value, int meta) {
        this.currentFill = 0;
        this.current = null;
        setChanged();
    }

    // ---------------------------------------------------------------- menu

    @Override public Component getDisplayName() { return Component.translatable("block.hbm.machine_silex"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerSILEX(containerId, playerInventory, worldPosition);
    }

    // ---------------------------------------------------------------- sync + persistence

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(currentFill);
        buf.writeInt(progress);
        buf.writeUtf(mode.name());
        tank.serialize(buf);
        // Upstream wrote the pool's item only when `current != null` but read it back when
        // `currentFill > 0` — an asymmetry that desyncs the buffer whenever one holds without the
        // other. Both sides key on the SAME condition here; the observable state is identical because
        // serverTick nulls `current` whenever the fill hits 0.
        boolean hasCurrent = current != null && !current.isEmpty();
        buf.writeBoolean(hasCurrent);
        if (hasCurrent) {
            buf.writeUtf(BuiltInRegistries.ITEM.getKey(current.getItem()).toString());
            buf.writeInt(current.getItem() instanceof ItemFluidIcon ? Fluids.networkId(ItemFluidIcon.getFluid(current)) : -1);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        currentFill = buf.readInt();
        progress = buf.readInt();
        mode = EnumWavelengths.byName(buf.readUtf());
        tank.deserialize(buf);
        current = buf.readBoolean() ? readSubstance(buf.readUtf(), buf.readInt()) : null;
    }

    private static @Nullable ItemStack readSubstance(String itemId, int fluidId) {
        Item item = BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.parse(itemId)).orElse(null);
        if (item == null) return null;
        return fluidId >= 0 ? ItemFluidIcon.make(Fluids.byNetworkId(fluidId)) : new ItemStack(item);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("fill", currentFill);
        output.putInt("recipeIndex", recipeIndex);
        output.putString("mode", mode.name());
        tank.writeToNBT(output, "tankNew"); // the 1.12.2 key
        if (current != null && !current.isEmpty()) {
            output.putString("item", BuiltInRegistries.ITEM.getKey(current.getItem()).toString());
            output.putInt("meta", current.getItem() instanceof ItemFluidIcon
                    ? Fluids.networkId(ItemFluidIcon.getFluid(current)) : -1);
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        currentFill = input.getIntOr("fill", 0);
        recipeIndex = input.getIntOr("recipeIndex", 0);
        mode = EnumWavelengths.byName(input.getStringOr("mode", EnumWavelengths.NULL.name()));
        tank.readFromNBT(input, "tankNew");
        String item = input.getStringOr("item", "");
        current = item.isEmpty() ? null : readSubstance(item, input.getIntOr("meta", -1));
    }
}
