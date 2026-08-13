package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.client.sound.CrystallizerSoundHandler;
import com.hbm.interfaces.IClimbable;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerCrystallizer;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.tileentity.IUpgradeInfoProvider;
import net.minecraft.ChatFormatting;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;

/**
 * Ore Acidizer (1.12.2 {@code TileEntityMachineCrystallizer}) — dissolves one input item in the acid
 * held by its 8,000 mB tank (peroxide by default, retyped by a fluid identifier) at 1,000 HE/t. The
 * tank's fluid <i>selects</i> the recipe, so recipes sharing an input (e.g. peroxide vs sulfuric
 * refinement) are distinguished by what acid is loaded.
 *
 * <p>Slot roles: 0 input, 1 battery, 2 output (take-only), 3 fluid-container in → 4 emptied-container
 * out, 5–6 upgrades (SPEED/EFFECT/OVERDRIVE, 1.12.2 formulas: -25% duration and +1,000 HE/t per speed
 * level; ×(level+2) acid and 5%/level free-op chance for EFFECT; +2 process cycles/tick per OVERDRIVE
 * level, capped at 7), 7 fluid identifier (kept in-slot). Slim port (deferred): fluid-ID copying.
 */
public class TileEntityMachineCrystallizer extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2, WorldlyContainer, IClimbable, IUpgradeInfoProvider {

    public static final long MAX_POWER = 1_000_000;
    public static final int DEMAND = 1_000;
    private static final int[] IO_SLOTS = {0, 2, 4};

    public long power;
    public int progress;
    public int duration = 600;
    public boolean isOn;
    /** Synced OVERDRIVE cycle count (1..7); the client spinner speeds up with it like the 1.12.2 tile. */
    public int cycleCount = 1;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.PEROXIDE, 8_000);
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    // client-only spinner animation (advanced while isOn, like the 1.12.2 tile)
    public float angle;
    public float prevAngle;

    private boolean climbRegistered = false;
    private AABB ladderAABB;

    public TileEntityMachineCrystallizer(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CRYSTALLIZER.get(), pos, state, 8);
        CrystallizerRecipes.register(); // idempotent; registries are flushed by now
    }

    @Override
    public String getDefaultName() {
        return "container.crystallizer";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCrystallizer te) {
        te.ensureClimbRegistered();
        te.isOn = false;

        for (DirPos dp : te.getConPos()) {
            te.trySubscribe(level, dp);
            if (te.tank.getTankType() != Fluids.NONE) te.trySubscribe(te.tank.getTankType(), level, dp);
        }

        te.power = Library.chargeTEFromItems(te.inventory, 1, te.getPower(), te.getMaxPower());
        te.updateTankType();
        te.fillTankFromSlot();
        te.upgradeManager.checkSlots(te.inventory, 5, 6);
        te.cycleCount = te.getCycleCount();

        // Duration/power/acid all scale with the plugged upgrades (the 1.12.2 getDuration/getPowerRequired/
        // getRequiredAcid); OVERDRIVE runs the whole process block cycleCount times per tick.
        for (int cycle = 0; cycle < te.cycleCount; cycle++) {
            CrystallizerRecipe recipe = CrystallizerRecipes.getOutput(te.inventory.getStackInSlot(0), te.tank.getTankType());
            te.duration = te.getDuration(recipe);

            if (te.canProcess(recipe)) {
                te.progress++;
                te.power -= te.getPowerRequired();
                te.isOn = true;

                if (te.progress > te.duration) {
                    te.progress = 0;
                    te.processItem(recipe);
                    te.setChanged();
                }
            } else {
                te.progress = 0;
            }
        }

        te.networkPackNT();
    }

    /** Client ticker: advance the spinner (gated by the synced {@code isOn}) + the looping sound. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCrystallizer te) {
        te.ensureClimbRegistered();
        CrystallizerSoundHandler.tickLoop(te);

        te.prevAngle = te.angle;
        if (te.isOn) {
            te.angle += 5F * te.cycleCount;
            if (te.angle >= 360) {
                te.angle -= 360;
                te.prevAngle -= 360;
            }
        }
    }

    private boolean canProcess(CrystallizerRecipe recipe) {
        if (recipe == null) return false;
        if (inventory.getStackInSlot(0).getCount() < recipe.input.stacksize) return false;
        if (power < getPowerRequired()) return false;
        if (tank.getFill() < getRequiredAcid(recipe.acidAmount)) return false;

        ItemStack out = inventory.getStackInSlot(2);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(out, recipe.output)) return false;
        return out.getCount() + recipe.output.getCount() <= out.getMaxStackSize();
    }

    private void processItem(CrystallizerRecipe recipe) {
        ItemStack out = inventory.getStackInSlot(2);
        if (out.isEmpty()) {
            inventory.setStackInSlot(2, recipe.output.copy());
        } else {
            out.grow(recipe.output.getCount());
        }
        tank.setFill(tank.getFill() - getRequiredAcid(recipe.acidAmount));

        // EFFECT: 5%/level chance (max 15%) to run the op without consuming the input item.
        float freeChance = getFreeChance();
        if (freeChance == 0 || freeChance < level.getRandom().nextFloat()) {
            inventory.getStackInSlot(0).shrink(recipe.input.stacksize);
        }
    }

    // --- upgrade-scaled parameters (1.12.2 getRequiredAcid/getFreeChance/getDuration/getPowerRequired/getCycleCount) ---

    public int getRequiredAcid(int base) {
        int efficiency = Math.min(upgradeManager.getLevel(UpgradeType.EFFECT), 3);
        if (efficiency > 0) return base * (efficiency + 2);
        return base;
    }

    public float getFreeChance() {
        int efficiency = Math.min(upgradeManager.getLevel(UpgradeType.EFFECT), 3);
        if (efficiency > 0) return Math.min(efficiency * 0.05F, 0.15F);
        return 0;
    }

    public int getDuration(CrystallizerRecipe recipe) {
        int base = recipe != null ? recipe.duration : 600;
        int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        if (speed > 0) return (int) Math.ceil(base * Math.max(1F - 0.25F * speed, 0.25F));
        return base;
    }

    public int getPowerRequired() {
        int speed = Math.min(upgradeManager.getLevel(UpgradeType.SPEED), 3);
        return DEMAND + Math.min(speed * 1000, 3000);
    }

    public int getCycleCount() {
        int speed = upgradeManager.getLevel(UpgradeType.OVERDRIVE);
        return Math.min(1 + speed * 2, 7);
    }

    /** Slot 7 holds a fluid identifier; adopt its type (kept in-slot, matching the 1.12.2 {@code setType(7, ...)}). */
    private void updateTankType() {
        ItemStack id = inventory.getStackInSlot(7);
        if (id.getItem() instanceof IItemFluidId ident) {
            FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
            if (t != tank.getTankType()) {
                tank.setTankType(t);
                setChanged();
            }
        }
    }

    /** Drains a full container in slot 3 INTO the tank, emptied container → slot 4 (the 1.12.2
     * {@code loadTank(3, 4)}). Handles the universal tank/barrel ({@link IFillableItem}) and buckets. */
    private void fillTankFromSlot() {
        ItemStack in = inventory.getStackInSlot(3);
        if (in.isEmpty()) return;

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);
            if (held <= 0 || heldType == Fluids.NONE) return;
            int addable = tank.fill(heldType, held, false);
            if (addable <= 0) return; // tank full or wrong type
            ItemStack work = single(in);
            int taken = fillable.tryEmpty(heldType, addable, work);
            if (taken <= 0 || !fits(4, work)) return;
            tank.fill(heldType, taken, true);
            moveResult(3, 4, work);
        } else {
            FluidType fluid = bucketFluid(in);
            if (fluid == Fluids.NONE) return;
            if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!fits(4, empty)) return;
            tank.fill(fluid, ItemFluidBucket.VOLUME, true);
            moveResult(3, 4, empty);
        }
    }

    private static FluidType bucketFluid(ItemStack stack) {
        if (stack.getItem() instanceof ItemFluidBucket) return ItemFluidBucket.getFluid(stack);
        if (stack.is(Items.WATER_BUCKET)) return Fluids.WATER;
        if (stack.is(Items.LAVA_BUCKET)) return Fluids.LAVA;
        return Fluids.NONE;
    }

    /** Any fluid container the load slot accepts: a tank/barrel, an HBM fluid bucket, or a vanilla bucket. */
    private static boolean isFluidContainer(ItemStack stack) {
        return stack.getItem() instanceof IFillableItem
                || stack.getItem() instanceof ItemFluidBucket
                || stack.is(Items.BUCKET) || stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET);
    }

    private static ItemStack single(ItemStack stack) {
        ItemStack copy = stack.copy();
        copy.setCount(1);
        return copy;
    }

    private boolean fits(int outSlot, ItemStack stack) {
        return inventory.insertItem(outSlot, stack.copy(), true).isEmpty();
    }

    private void moveResult(int inSlot, int outSlot, ItemStack result) {
        inventory.extractItem(inSlot, 1, false);
        inventory.insertItem(outSlot, result, false);
        setChanged();
    }

    /** The 8 network-connection positions on the tower's four sides at ±2 (ported verbatim). */
    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z + 1, Library.POS_X), new DirPos(x + 2, y, z - 1, Library.POS_X),
                new DirPos(x - 2, y, z + 1, Library.NEG_X), new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x + 1, y, z + 2, Library.POS_Z), new DirPos(x - 1, y, z + 2, Library.POS_Z),
                new DirPos(x + 1, y, z - 2, Library.NEG_Z), new DirPos(x - 1, y, z - 2, Library.NEG_Z)};
    }

    /**
     * Slot roles: 0 input (any recipe input, regardless of the loaded acid — the player may load the
     * matching acid after), 1 battery, 3 fluid-container in, 7 identifier. Slot 4 must return true so
     * the machine's own insertItem can place the emptied container there (player insertion is blocked
     * by the take-only slot in {@link ContainerCrystallizer}); 2 (output, written via setStackInSlot)
     * and 5–6 (inert upgrade slots) reject everything.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> CrystallizerRecipes.isAnyRecipeInput(stack);
            case 1 -> Library.isBattery(stack);
            case 3 -> isFluidContainer(stack);
            case 4 -> true;
            case 5, 6 -> Library.isMachineUpgrade(stack); // upgrades (any type, like SlotUpgrade)
            case 7 -> stack.getItem() instanceof IItemFluidId;
            default -> false; // 2 output
        };
    }

    // --- WorldlyContainer: hoppers/pipes feed the input, pull the output + emptied containers ---

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }

    /** Automation only inserts inputs the CURRENT acid can process (the 1.12.2 {@code canInsertItem}). */
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && CrystallizerRecipes.getOutput(stack, tank.getTankType()) != null;
    }

    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 2 || slot == 4; }

    // --- IClimbable: the ladder column up the tower's side (ported from the 1.12.2 tile) ---

    private void ensureClimbRegistered() {
        if (!climbRegistered) {
            registerClimbable();
            climbRegistered = true;
        }
    }

    private AABB getLadderAABB() {
        if (ladderAABB == null) {
            ForgeDirection dir = ForgeDirection.getOrientation(getBlockState().getValue(BlockDummyable.META) - BlockDummyable.offset);
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            BlockPos p = getBlockPos();
            ladderAABB = new AABB(p.getX() + 0.25, p.getY() + 1, p.getZ() + 0.25, p.getX() + 0.75, p.getY() + 6, p.getZ() + 0.75)
                    .move(rot.offsetX * 1.5, 0, rot.offsetZ * 1.5);
        }
        return ladderAABB;
    }

    @Override
    public boolean isEntityInClimbAABB(LivingEntity entity) {
        return entity.getBoundingBox().intersects(getLadderAABB());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        unregisterClimbable();
    }

    // --- IUpgradeInfoProvider (text ported from the 1.12.2 provideInfo) ---

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{5, 6};
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.EFFECT || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, java.util.List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_crystallizer.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(KEY_EFFICIENCY, "x" + level).withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 200) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public java.util.HashMap<UpgradeType, Integer> getValidUpgrades() {
        java.util.HashMap<UpgradeType, Integer> upgrades = new java.util.HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.EFFECT, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    // --- IEnergyReceiverMK2 ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public long getReceiverSpeed() { return MAX_POWER; }

    // --- IFluidStandardReceiverMK2 ---

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    // --- MenuProvider ---

    @Override public Component getDisplayName() { return Component.translatable("block.hbm.machine_crystallizer"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCrystallizer(containerId, playerInventory, this);
    }

    // --- sync (BufPacket) + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(progress);
        buf.writeInt(duration);
        buf.writeLong(power);
        buf.writeBoolean(isOn);
        buf.writeInt(cycleCount);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        progress = buf.readInt();
        duration = buf.readInt();
        power = buf.readLong();
        isOn = buf.readBoolean();
        cycleCount = buf.readInt();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        tank.writeToNBT(output, "tankNew"); // the 1.12.2 key
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
        tank.readFromNBT(input, "tankNew");
    }
}
