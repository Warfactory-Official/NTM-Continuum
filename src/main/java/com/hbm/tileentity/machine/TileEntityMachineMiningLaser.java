package com.hbm.tileentity.machine;

import java.util.HashMap;
import java.util.List;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineMiningLaser;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Recipes;
import dev.golem.core.world.CompatEnchantments;
import net.minecraft.world.phys.AABB;

/**
 * Mining Laser (1.12.2 {@code TileEntityMachineMiningLaser}) — a ceiling-mounted quarry. While on, it
 * scans a square of the current {@code targetY} layer (widened +2/level by EFFECT upgrades), locks the
 * beam onto the first breakable block, accumulates {@code breakProgress} by hardness (driving the vanilla
 * crack overlay via {@code destroyBlockProgress}), breaks with FORTUNE-enchanted loot, vacuums the drops
 * into 21 buffer slots, and descends a layer when one is cleared. Mined {@code ore_oil} converts into
 * 500 mB of crude oil (sent out the side arms); liquids in the path are deleted and dammed with
 * {@code sandbags}. All formulas ported verbatim (old update() lines 116–212).
 *
 * <p>Slots: 0 battery, 1–8 upgrades, 9–29 output buffer. Deferred (documented): the CENTRIFUGE
 * drop-processing (CentrifugeRecipes unported — behaves as the old no-match fall-through), old
 * {@code IDrillInteraction} blocks and the gas-block check (neither system exists in the port).
 */
public class TileEntityMachineMiningLaser extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardSenderMK2, WorldlyContainer, IUpgradeInfoProvider, IControlReceiver {

    public static final long maxPower = 100_000_000;
    public static final int consumption = 10_000;

    /** Hoppers may extract the 21 output slots from any side (old getAccessibleSlotsFromSide). */
    private static final int[] OUTPUT_SLOTS;
    static {
        OUTPUT_SLOTS = new int[21];
        for (int i = 0; i < 21; i++) OUTPUT_SLOTS[i] = 9 + i;
    }

    public long power;
    public final FluidTankNTM tank = new FluidTankNTM(Fluids.OIL, 64_000);
    public boolean isOn;
    public int targetX;
    public int targetY;
    public int targetZ;
    public int lastTargetX;
    public int lastTargetY;
    public int lastTargetZ;
    public boolean beam;
    public double breakProgress;
    private double clientBreakProgress = 0;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineMiningLaser(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_MINING_LASER.get(), pos, state, 30);
        CrystallizerRecipes.register(); // idempotent (crystallizer-upgrade drop processing)
        ShredderRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.miningLaser";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineMiningLaser te) {
        // Power from the TOP arm; crude oil out the 4 side arms (old sendFluid coords kept verbatim,
        // including the odd y±2 Z-direction pair).
        te.trySubscribe(level, new DirPos(pos.getX(), pos.getY() + 2, pos.getZ(), ForgeDirection.UP));

        te.tryProvide(te.tank, level, pos.getX() + 2, pos.getY(), pos.getZ(), Library.POS_X);
        te.tryProvide(te.tank, level, pos.getX() - 2, pos.getY(), pos.getZ(), Library.NEG_X);
        te.tryProvide(te.tank, level, pos.getX(), pos.getY() + 2, pos.getZ(), Library.POS_Z);
        te.tryProvide(te.tank, level, pos.getX(), pos.getY() - 2, pos.getZ(), Library.NEG_Z);

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);

        // reset progress if the position changes
        if (te.lastTargetX != te.targetX || te.lastTargetY != te.targetY || te.lastTargetZ != te.targetZ)
            te.breakProgress = 0;

        // set last positions for interpolation and the like
        te.lastTargetX = te.targetX;
        te.lastTargetY = te.targetY;
        te.lastTargetZ = te.targetZ;

        if (te.isOn) {

            te.upgradeManager.checkSlots(te.inventory, 1, 8);
            int cycles = 1 + te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);
            int speed = 1 + Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 12);
            int range = 1 + Math.min(te.upgradeManager.getLevel(UpgradeType.EFFECT) * 2, 24);
            int fortune = Math.min(te.upgradeManager.getLevel(UpgradeType.FORTUNE), 3);
            int consumption = TileEntityMachineMiningLaser.consumption
                    - (TileEntityMachineMiningLaser.consumption * Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 12) / 16)
                    + (TileEntityMachineMiningLaser.consumption * Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 12) / 16);

            // Scream upgrade: ×4 cycles, ×4 speed, ×20 consumption (1.12.2 verbatim).
            boolean scream = te.hasUpgrade(ModUpgradeItems.upgrade_screm.get());
            if (scream) {
                cycles *= 4;
                speed *= 4;
                consumption *= 20;
            }

            for (int i = 0; i < cycles; i++) {

                if (te.power < consumption) {
                    te.beam = false;
                    break;
                }

                te.power -= consumption;

                // The old "targetY <= 0" world-bottom reset, retargeted to the 26.1 world floor.
                if (te.targetY <= level.getMinY())
                    te.targetY = pos.getY() - 2;

                te.scan(range);

                BlockPos targetPos = new BlockPos(te.targetX, te.targetY, te.targetZ);
                BlockState block = level.getBlockState(targetPos);

                if (block.liquid()) {
                    level.setBlockAndUpdate(targetPos, Blocks.AIR.defaultBlockState());
                    te.buildDam();
                    continue;
                }
                if (te.beam && te.canBreak(block, targetPos)) {

                    te.breakProgress += te.getBreakSpeed(speed);
                    te.clientBreakProgress = Math.min(te.breakProgress, 1);

                    if (te.breakProgress < 1) {
                        level.destroyBlockProgress(-1, targetPos, (int) Math.floor(te.breakProgress * 10));
                    } else {
                        te.breakBlock(fortune);
                        te.buildDam();
                    }
                }
            }

            // The scientists scream at the block being mined, once per beaming tick (old verbatim, volume 20).
            if (te.beam && scream) {
                level.playSound(null, te.targetX + 0.5, te.targetY + 0.5, te.targetZ + 0.5,
                        com.hbm.sound.ModSounds.SCREM.get(), net.minecraft.sounds.SoundSource.BLOCKS, 20.0F, 1.0F);
            }
        } else {
            te.targetY = pos.getY() - 2;
            te.beam = false;
        }

        te.tryFillContainer(pos.getX() + 2, pos.getY(), pos.getZ());
        te.tryFillContainer(pos.getX() - 2, pos.getY(), pos.getZ());
        te.tryFillContainer(pos.getX(), pos.getY(), pos.getZ() + 2);
        te.tryFillContainer(pos.getX(), pos.getY(), pos.getZ() - 2);

        te.networkPackNT();
    }

    /** Row-major layer scan (old scan()): lock onto the first breakable block, else descend a layer. */
    private void scan(int range) {

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {

                BlockPos p = new BlockPos(x + worldPosition.getX(), targetY, z + worldPosition.getZ());
                BlockState state = level.getBlockState(p);

                if (state.liquid()) continue;

                if (canBreak(state, p)) {
                    targetX = p.getX();
                    targetZ = p.getZ();
                    beam = true;
                    return;
                }
            }
        }

        beam = false;
        targetY--;
    }

    /** Old canBreak: no air/bedrock, no unbreakables (hardness <0) or ultra-hard (>3.5M), no liquids. */
    private boolean canBreak(BlockState state, BlockPos p) {
        if (state.isAir()) return false;
        if (state.is(Blocks.BEDROCK)) return false;
        float hardness = state.getDestroySpeed(level, p);
        if (hardness < 0 || hardness > 3_500_000) return false;
        return !state.liquid();
    }

    /** Old getBreakSpeed: progress per cycle = speed / (hardness * 15); instant for zero-hardness blocks. */
    private double getBreakSpeed(int speed) {
        BlockPos p = new BlockPos(targetX, targetY, targetZ);
        float hardness = level.getBlockState(p).getDestroySpeed(level, p) * 15 / speed;
        if (hardness == 0) return 1;
        return 1 / hardness;
    }

    /**
     * Breaks the target (old breakBlock): the drop-processing upgrades run in the old priority order
     * (crystallizer → centrifuge [unported: falls through like a recipe miss] → shredder → smelter);
     * a match spawns the processed output and suppresses the raw drop. Otherwise the block drops its
     * loot with the FORTUNE level applied via a synthetic enchanted tool.
     */
    private void breakBlock(int fortune) {
        BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
        BlockState state = level.getBlockState(targetPos);
        boolean normal = true;

        ItemStack stack = new ItemStack(state.getBlock().asItem());

        if (!stack.isEmpty()) {
            if (hasUpgrade(ModUpgradeItems.upgrade_crystallizer.get())) {

                CrystallizerRecipe result = CrystallizerRecipes.getOutput(stack, Fluids.PEROXIDE);
                if (result == null) result = CrystallizerRecipes.getOutput(stack, Fluids.SULFURIC_ACID);
                if (result != null) {
                    spawnDrop(result.output.copy());
                    normal = false;
                }

            } else if (hasUpgrade(ModUpgradeItems.upgrade_centrifuge.get())) {

                // CentrifugeRecipes is unported — the old code falls through to the normal drop when no
                // recipe matches, so the upgrade is faithfully a pass-through until the centrifuge lands.

            } else if (hasUpgrade(ModUpgradeItems.upgrade_shredder.get())) {

                ItemStack result = ShredderRecipes.getShredderResult(stack);
                if (!result.isEmpty() && result.getItem() != ModItems.scrap.get()) {
                    spawnDrop(result.copy());
                    normal = false;
                }

            } else if (hasUpgrade(ModUpgradeItems.upgrade_smelter.get())) {

                if (level instanceof ServerLevel serverLevel) {
                    ItemStack result = Recipes.smeltingResult(serverLevel, stack).orElse(ItemStack.EMPTY);
                    if (!result.isEmpty()) {
                        spawnDrop(result.copy());
                        normal = false;
                    }
                }
            }
        }

        if (normal && level instanceof ServerLevel serverLevel) {
            // The old dropBlockAsItem(world, pos, state, fortune): loot with the fortune level applied.
            ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
            CompatEnchantments.enchantFortune(serverLevel, tool, fortune);
            for (ItemStack drop : Block.getDrops(state, serverLevel, targetPos, level.getBlockEntity(targetPos), null, tool)) {
                spawnDrop(drop);
            }
        }

        level.destroyBlock(targetPos, false);

        suckDrops();

        breakProgress = 0;
    }

    private void spawnDrop(ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level, targetX + 0.5, targetY + 0.5, targetZ + 0.5, stack));
    }

    /** Vacuums drops around the target into the buffer (old suckDrops), and sets nearby mobs on fire. */
    private void suckDrops() {

        int rangeHor = 3;
        int rangeVer = 1;
        boolean nullifier = hasUpgrade(ModUpgradeItems.upgrade_nullifier.get());

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(
                targetX + 0.5 - rangeHor, targetY + 0.5 - rangeVer, targetZ + 0.5 - rangeHor,
                targetX + 0.5 + rangeHor, targetY + 0.5 + rangeVer, targetZ + 0.5 + rangeHor));

        for (ItemEntity item : items) {

            if (nullifier && ItemMachineUpgrade.isScrap(item.getItem())) {
                item.discard();
                continue;
            }

            // Mined oil deposits become 500 mB of crude in the output tank instead of an item.
            if (item.getItem().is(com.hbm.blocks.ModOilBlocks.ore_oil.get().asItem())) {

                tank.setTankType(Fluids.OIL); // just to be sure

                tank.setFill(Math.min(tank.getFill() + 500, tank.getMaxFill()));

                item.discard();
                continue;
            }

            ItemStack rest = InventoryUtil.tryAddItemToInventory(inventory, 9, 29, item.getItem().copy());

            if (rest.isEmpty()) item.discard();
            else item.setItem(rest.copy());
        }

        List<LivingEntity> mobs = level.getEntitiesOfClass(LivingEntity.class, new AABB(
                targetX + 0.5 - 1, targetY + 0.5 - 1, targetZ + 0.5 - 1,
                targetX + 0.5 + 1, targetY + 0.5 + 1, targetZ + 0.5 + 1));

        for (LivingEntity mob : mobs) {
            mob.setRemainingFireTicks(5 * 20); // old setFire(5) seconds
        }
    }

    /** Dams the target's horizontal neighbors with sandbags when they are liquid (old buildDam). */
    private void buildDam() {
        placeBags(new BlockPos(targetX + 1, targetY, targetZ));
        placeBags(new BlockPos(targetX - 1, targetY, targetZ));
        placeBags(new BlockPos(targetX, targetY, targetZ + 1));
        placeBags(new BlockPos(targetX, targetY, targetZ - 1));
    }

    private void placeBags(BlockPos wallPos) {
        BlockState state = level.getBlockState(wallPos);
        if (state.canBeReplaced() && state.liquid())
            level.setBlockAndUpdate(wallPos, ModMachineBlocks.sandbags.get().defaultBlockState());
    }

    /** Pushes buffered output into an adjacent inventory at the side arms (old tryFillContainer). */
    private void tryFillContainer(int x, int y, int z) {

        if (!(level.getBlockEntity(new BlockPos(x, y, z)) instanceof net.minecraft.world.Container container)) return;
        if (container == this) return;

        for (int i = 9; i <= 29; i++) {

            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int prev = stack.getCount();
                inventory.setStackInSlot(i, tryAddToContainer(container, stack));

                if (inventory.getStackInSlot(i).isEmpty() || inventory.getStackInSlot(i).getCount() < prev)
                    return;
            }
        }
    }

    /** Simple vanilla-Container insert (merge then empty slots, honoring canPlaceItem); returns the remainder. */
    private static ItemStack tryAddToContainer(net.minecraft.world.Container container, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = 0; i < container.getContainerSize() && !rest.isEmpty(); i++) {
            ItemStack existing = container.getItem(i);
            if (existing.isEmpty()) {
                if (!container.canPlaceItem(i, rest)) continue;
                container.setItem(i, rest);
                return ItemStack.EMPTY;
            }
            if (dev.golem.core.world.ItemStacks.sameItemAndData(existing, rest)) {
                int move = Math.min(rest.getCount(), Math.min(container.getMaxStackSize(), existing.getMaxStackSize()) - existing.getCount());
                if (move > 0) {
                    existing.grow(move);
                    rest.shrink(move);
                    container.setChanged();
                }
            }
        }
        return rest;
    }

    private boolean hasUpgrade(Item item) {
        for (int i = 1; i < 9; i++) {
            if (inventory.getStackInSlot(i).getItem() == item) return true;
        }
        return false;
    }

    /** GUI display: the mining square half-width from the installed EFFECT upgrades (old getRange). */
    public int getRange() {
        int range = 1;
        for (int i = 1; i < 9; i++) {
            Item item = inventory.getStackInSlot(i).getItem();
            if (item == ModUpgradeItems.upgrade_effect_1.get()) range += 2;
            else if (item == ModUpgradeItems.upgrade_effect_2.get()) range += 4;
            else if (item == ModUpgradeItems.upgrade_effect_3.get()) range += 6;
        }
        return Math.min(range, 25);
    }

    public int getWidth() {
        return 1 + getRange() * 2;
    }

    public long getPowerScaled(long i) {
        return power * i / maxPower;
    }

    public int getProgressScaled(int i) {
        return (int) (breakProgress * i);
    }

    // --- inventory gating ---

    /**
     * Slots: 0 battery, 1–8 upgrades. Outputs 9–29 must return true so the machine's own insertItem
     * (suckDrops) can fill them; player insertion is blocked by the take-only container slots.
     */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot >= 1 && slot <= 8) return Library.isMachineUpgrade(stack);
        return slot >= 9;
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{1, 8};
    }

    // --- WorldlyContainer: extract-only output buffer on every side (old getAccessibleSlotsFromSide) ---

    @Override public int[] getSlotsForFace(Direction side) { return OUTPUT_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 9 && slot <= 29; }

    // --- connections: power at the TOP arm, oil out the side arms (old getConPos, quirks preserved) ---

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x, y + 2, z, ForgeDirection.UP),
                new DirPos(x + 2, y, z, Library.POS_X),
                new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y + 2, z, Library.POS_Z),
                new DirPos(x, y - 2, z, Library.NEG_Z)
        };
    }

    // --- IEnergyReceiverMK2 ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }

    // --- IFluidStandardSenderMK2 ---

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    // --- IControlReceiver: the GUI on/off button (replaces the old AuxButtonPacket) ---

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) {
            isOn = !isOn;
            setChanged();
        }
    }

    // --- IUpgradeInfoProvider (text ported from the 1.12.2 provideInfo) ---

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE
                || type == UpgradeType.EFFECT || type == UpgradeType.FORTUNE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_mining_laser.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (100 * level / 16) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 * level / 16) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(KEY_RANGE, "+" + (2 * level) + "m").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.FORTUNE) {
            info.add(Component.translatable(KEY_FORTUNE, "+" + level).withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 12);
        upgrades.put(UpgradeType.POWER, 12);
        upgrades.put(UpgradeType.EFFECT, 12);
        upgrades.put(UpgradeType.FORTUNE, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 9);
        return upgrades;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_mining_laser");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineMiningLaser(containerId, playerInventory, this);
    }

    // --- sync (BufPacket) + persistence (old serialize/deserialize order preserved) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(lastTargetX);
        buf.writeInt(lastTargetY);
        buf.writeInt(lastTargetZ);
        buf.writeInt(targetX);
        buf.writeInt(targetY);
        buf.writeInt(targetZ);
        buf.writeBoolean(beam);
        buf.writeBoolean(isOn);
        buf.writeDouble(clientBreakProgress);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        lastTargetX = buf.readInt();
        lastTargetY = buf.readInt();
        lastTargetZ = buf.readInt();
        targetX = buf.readInt();
        targetY = buf.readInt();
        targetZ = buf.readInt();
        beam = buf.readBoolean();
        isOn = buf.readBoolean();
        breakProgress = buf.readDouble(); // the old client-side clamped copy
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        output.putInt("targetY", targetY);
        output.putDouble("breakProgress", breakProgress);
        tank.writeToNBT(output, "oil"); // the 1.12.2 key
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        isOn = input.getBooleanOr("isOn", false);
        targetY = input.getIntOr("targetY", 0);
        breakProgress = input.getDoubleOr("breakProgress", 0);
        tank.readFromNBT(input, "oil");
    }
}
