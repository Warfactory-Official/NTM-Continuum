package com.hbm.tileentity.machine;

import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.MachineCore;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import dev.golem.core.data.Tags;
import com.hbm.api.block.ICrucibleAcceptor;
import com.hbm.api.tile.IHeatSource;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerCrucible;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes.CrucibleRecipe;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.hbm.util.CrucibleUtil;
import com.hbm.util.TickPhase;

import java.util.ArrayList;
import java.util.List;

/**
 * Crucible (1.12.2 {@code TileEntityCrucible}): melts items into molten material quanta using heat
 * pulled from an {@link IHeatSource} directly below (diffusion 0.25 of the delta), runs the selected
 * alloying {@link CrucibleRecipe} on its <b>recipe stack</b>, collects everything else in the <b>waste
 * stack</b>, and pours both out via a downward raytrace ({@link CrucibleUtil}): waste out the back,
 * recipe output out the front. Slots 1–9 are the 3×3 input grid (limit 1/slot; slot 0 is unused, as in
 * the original); thrown items landing in the basin are vacuumed in. Constants as written; the
 * {@code IConfigurableMachine} JSON config is deferred. Each non-empty pour stack adds 1.7's {@code SOOT_PER_SECOND / 20} soot every tick.
 */
public class TileEntityCrucible extends TileEntityMachineBase implements MenuProvider, ICrucibleAcceptor, IControlReceiver {

    public int heat;
    public int progress;

    public String recipe = "null";

    public List<Mats.MaterialStack> recipeStack = new ArrayList<>();
    public List<Mats.MaterialStack> wasteStack = new ArrayList<>();

    /* CONSTANTS (1.12.2 defaults) */
    public static final int recipeZCapacity = MaterialShapes.BLOCK.quantity * 16;
    public static final int wasteZCapacity = MaterialShapes.BLOCK.quantity * 16;
    public static final int processTime = 20_000;
    public static final double diffusion = 0.25D;
    public static final int maxHeat = 100_000;

    // Pour-stream visuals, synced to the client for the renderer (-1 colour = no stream). Each stream
    // has a server-only decay timer (the old ParticleFoundry lived 20 ticks): a successful pour refreshes
    // it, and the stream stays shown until it lapses, so the intermittent per-tick pours (the channel
    // only accepts once every few ticks) read as one continuous stream instead of flickering.
    public int wastePourColor = -1;
    public float wastePourLen;
    public int recipePourColor = -1;
    public float recipePourLen;
    private int wastePourTimer;
    private int recipePourTimer;
    /** How long a pour stream lingers after the last successful pour (ticks); the old particle used 20. */
    private static final int STREAM_LIFETIME = 15;

    public TileEntityCrucible(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CRUCIBLE.get(), pos, state, 10);
        CrucibleRecipes.register(); // idempotent; registries are flushed by now
    }

    @Override
    public String getDefaultName() {
        return "container.machineCrucible";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCrucible te) {
        te.tryPullHeat(level, pos);

        /* collect items */
        if (TickPhase.every(level, pos, 5)) {
            List<ItemEntity> list = level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(pos.getX() - 0.5, pos.getY() + 0.5, pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 1, pos.getZ() + 1.5));

            for (ItemEntity item : list) {
                if (!item.isAlive()) continue;
                ItemStack stack = item.getItem();
                if (te.isItemSmeltable(stack)) {
                    for (int i = 1; i < 10; i++) {
                        if (te.inventory.getStackInSlot(i).isEmpty()) {
                            if (stack.getCount() == 1) {
                                te.inventory.setStackInSlot(i, stack.copy());
                                item.discard();
                                break;
                            } else {
                                ItemStack single = stack.copy();
                                single.setCount(1);
                                te.inventory.setStackInSlot(i, single);
                                stack.shrink(1);
                            }
                            te.setChanged();
                        }
                    }
                }
            }
        }

        /* burn entities standing in the molten pool */
        int totalCap = recipeZCapacity + wasteZCapacity;
        int totalMass = 0;
        for (Mats.MaterialStack stack : te.recipeStack) totalMass += stack.amount;
        for (Mats.MaterialStack stack : te.wasteStack) totalMass += stack.amount;

        double poolLevel = ((double) totalMass / (double) totalCap) * 0.875D;
        if (poolLevel > 0) {
            List<LivingEntity> living = level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            pos.getX() + 0.5, pos.getY() + 0.5 + poolLevel, pos.getZ() + 0.5).inflate(1, 0, 1));
            for (LivingEntity entity : living) {
                entity.hurt(level.damageSources().lava(), 5F);
                Effects.ignite(entity, 5);
            }
        }

        /* smelt items from buffer */
        if (!te.trySmelt(level)) {
            te.progress = 0;
        }

        te.tryRecipe(level);

        // Decay the pour-stream timers; a stream only clears once its buffer lapses (see STREAM_LIFETIME).
        if (te.wastePourTimer > 0) te.wastePourTimer--; else te.wastePourColor = -1;
        if (te.recipePourTimer > 0) te.recipePourTimer--; else te.recipePourColor = -1;

        /* pour waste stack (out the back) */
        if (!te.wasteStack.isEmpty()) {
            ForgeDirection dir = MachineCore.facing(state).getOpposite();
            Vec3[] impact = new Vec3[1];
            double sx = pos.getX() + 0.5D + dir.offsetX * 1.875D;
            double sy = pos.getY() + 0.25D;
            double sz = pos.getZ() + 0.5D + dir.offsetZ * 1.875D;
            Mats.MaterialStack didPour = CrucibleUtil.pourFullStack(level, sx, sy, sz, 6, true, te.wasteStack, MaterialShapes.NUGGET.quantity * 3, impact);

            if (didPour != null && impact[0] != null) {
                te.wastePourColor = didPour.material.moltenColor;
                te.wastePourLen = Math.max(1F, pos.getY() - (float) (Math.ceil(impact[0].y) - 0.875));
                te.wastePourTimer = STREAM_LIFETIME;
            }

            PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 20F);
        }

        /* pour recipe stack (out the front) */
        if (!te.recipeStack.isEmpty()) {
            ForgeDirection dir = MachineCore.facing(state);
            List<Mats.MaterialStack> toCast = new ArrayList<>();

            CrucibleRecipe recipe = te.getLoadedRecipe();
            // if no recipe is loaded, everything from the recipe stack is drainable
            if (recipe == null) {
                toCast.addAll(te.recipeStack);
            } else {
                for (Mats.MaterialStack stack : te.recipeStack) {
                    for (Mats.MaterialStack output : recipe.output) {
                        if (stack.material == output.material) {
                            toCast.add(stack);
                            break;
                        }
                    }
                }
            }

            Vec3[] impact = new Vec3[1];
            double sx = pos.getX() + 0.5D + dir.offsetX * 1.875D;
            double sy = pos.getY() + 0.25D;
            double sz = pos.getZ() + 0.5D + dir.offsetZ * 1.875D;
            Mats.MaterialStack didPour = CrucibleUtil.pourFullStack(level, sx, sy, sz, 6, true, toCast, MaterialShapes.NUGGET.quantity * 3, impact);

            if (didPour != null && impact[0] != null) {
                te.recipePourColor = didPour.material.moltenColor;
                te.recipePourLen = Math.max(1F, pos.getY() - (float) (Math.ceil(impact[0].y) - 0.875));
                te.recipePourTimer = STREAM_LIFETIME;
            }

            PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND / 20F);
        }

        /* clean up stacks */
        te.recipeStack.removeIf(o -> o.amount <= 0);
        te.wasteStack.removeIf(x -> x.amount <= 0);

        te.networkPackNT();
    }

    protected void tryPullHeat(Level level, BlockPos pos) {
        if (this.heat >= maxHeat) return;

        BlockEntity con = level.getBlockEntity(pos.below());

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;

            if (diff == 0) return;

            diff = Math.min(diff, maxHeat - this.heat);

            if (diff > 0) {
                diff = (int) Math.ceil(diff * diffusion);
                source.useUpHeat(diff);
                this.heat += diff;
                if (this.heat > maxHeat) this.heat = maxHeat;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    protected boolean trySmelt(Level level) {
        if (this.heat < maxHeat / 2) return false;

        int slot = this.getFirstSmeltableSlot();
        if (slot == -1) return false;

        int delta = this.heat - (maxHeat / 2);
        delta = (int) (delta * 0.05);

        this.progress += delta;
        this.heat -= delta;

        if (this.progress >= processTime) {
            this.progress = 0;

            List<Mats.MaterialStack> materials = Mats.getSmeltingMaterialsFromItem(inventory.getStackInSlot(slot));
            CrucibleRecipe recipe = getLoadedRecipe();

            for (Mats.MaterialStack material : materials) {
                boolean recipeMaterial = recipe != null && (getQuantaFromType(recipe.input, material.material) > 0 || getQuantaFromType(recipe.output, material.material) > 0);

                if (recipeMaterial) {
                    this.addToStack(this.recipeStack, material);
                } else {
                    this.addToStack(this.wasteStack, material);
                }
            }

            inventory.getStackInSlot(slot).shrink(1);
            setChanged();
        }

        return true;
    }

    protected void tryRecipe(Level level) {
        CrucibleRecipe recipe = this.getLoadedRecipe();

        if (recipe == null) return;
        if (!TickPhase.every(level, worldPosition, recipe.frequency)) return;

        for (Mats.MaterialStack stack : recipe.input) {
            if (getQuantaFromType(this.recipeStack, stack.material) < stack.amount) return;
        }

        for (Mats.MaterialStack stack : this.recipeStack) {
            stack.amount -= getQuantaFromType(recipe.input, stack.material);
        }

        outer:
        for (Mats.MaterialStack out : recipe.output) {
            for (Mats.MaterialStack stack : this.recipeStack) {
                if (stack.material == out.material) {
                    stack.amount += out.amount;
                    continue outer;
                }
            }
            this.recipeStack.add(out.copy());
        }
    }

    protected int getFirstSmeltableSlot() {
        for (int i = 1; i < 10; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty() && isItemSmeltable(stack)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot >= 1 && isItemSmeltable(stack);
    }

    public boolean isItemSmeltable(ItemStack stack) {
        List<Mats.MaterialStack> materials = Mats.getSmeltingMaterialsFromItem(stack);

        // if there's no materials in there at all, don't smelt
        if (materials.isEmpty()) return false;
        CrucibleRecipe recipe = getLoadedRecipe();

        // needs to be true, will always be true if there's no recipe loaded
        boolean matchesRecipe = recipe == null;

        int recipeContent = recipe != null ? recipe.getInputAmount() : 0;
        int recipeAmount = getQuantaFromType(this.recipeStack, null);
        int wasteAmount = getQuantaFromType(this.wasteStack, null);

        for (Mats.MaterialStack mat : materials) {
            int recipeInputRequired = recipe != null ? getQuantaFromType(recipe.input, mat.material) : 0;

            // this allows pouring the output material back into the crucible
            if (recipe != null && getQuantaFromType(recipe.output, mat.material) > 0) {
                recipeAmount += mat.amount;
                matchesRecipe = true;
                continue;
            }

            if (recipeInputRequired == 0) {
                // if this type isn't required by the recipe (or no recipe is set), it lands in the waste stack
                wasteAmount += mat.amount;
            } else {
                // the maximum is the recipe's ratio scaled up to the recipe stack's capacity
                int matMaximum = recipeInputRequired * recipeZCapacity / recipeContent;
                int amountStored = getQuantaFromType(recipeStack, mat.material);

                matchesRecipe = true;
                recipeAmount += mat.amount;

                if (amountStored + mat.amount > matMaximum) return false;
            }
        }

        return recipeAmount <= recipeZCapacity && wasteAmount <= wasteZCapacity && matchesRecipe;
    }

    public void addToStack(List<Mats.MaterialStack> stack, Mats.MaterialStack matStack) {
        for (Mats.MaterialStack mat : stack) {
            if (mat.material == matStack.material) {
                mat.amount += matStack.amount;
                return;
            }
        }
        stack.add(matStack.copy());
    }

    public CrucibleRecipe getLoadedRecipe() {
        return CrucibleRecipes.INSTANCE.recipeNameMap.get(recipe) instanceof CrucibleRecipe rec ? rec : null;
    }

    public int getQuantaFromType(Mats.MaterialStack[] stacks, NTMMaterial mat) {
        for (Mats.MaterialStack stack : stacks) {
            if (mat == null || stack.material == mat) {
                return stack.amount;
            }
        }
        return 0;
    }

    public int getQuantaFromType(List<Mats.MaterialStack> stacks, NTMMaterial mat) {
        int sum = 0;
        for (Mats.MaterialStack stack : stacks) {
            if (stack.material == mat) {
                return stack.amount;
            }
            if (mat == null) {
                sum += stack.amount;
            }
        }
        return sum;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    }

    // --- ICrucibleAcceptor (the crucible itself can be poured into from the top) ---

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, Mats.MaterialStack stack) {
        CrucibleRecipe recipe = getLoadedRecipe();

        if (recipe == null) {
            return getQuantaFromType(this.wasteStack, null) < wasteZCapacity;
        }

        int recipeContent = recipe.getInputAmount();
        int recipeInputRequired = getQuantaFromType(recipe.input, stack.material);
        int matMaximum = recipeInputRequired * recipeZCapacity / recipeContent;
        int amountStored = getQuantaFromType(recipeStack, stack.material);

        return amountStored < matMaximum && getQuantaFromType(this.recipeStack, null) < recipeZCapacity;
    }

    @Override
    public Mats.MaterialStack pour(Level world, BlockPos pos, double dX, double dY, double dZ, ForgeDirection side, Mats.MaterialStack stack) {
        CrucibleRecipe recipe = getLoadedRecipe();

        if (recipe == null) {
            int amount = getQuantaFromType(this.wasteStack, null);

            if (amount + stack.amount <= wasteZCapacity) {
                this.addToStack(this.wasteStack, stack.copy());
                return null;
            } else {
                int toAdd = wasteZCapacity - amount;
                this.addToStack(this.wasteStack, new Mats.MaterialStack(stack.material, toAdd));
                return new Mats.MaterialStack(stack.material, stack.amount - toAdd);
            }
        }

        int recipeContent = recipe.getInputAmount();
        int recipeInputRequired = getQuantaFromType(recipe.input, stack.material);
        int matMaximum = recipeInputRequired * recipeZCapacity / recipeContent;
        int amount = getQuantaFromType(this.recipeStack, stack.material);
        int amountTotal = getQuantaFromType(this.recipeStack, null);

        int toAdd = Math.min(matMaximum - amount, recipeZCapacity - amountTotal);
        toAdd = Math.min(toAdd, stack.amount);
        if (toAdd <= 0) return stack;

        this.addToStack(this.recipeStack, new Mats.MaterialStack(stack.material, toAdd));
        return toAdd < stack.amount ? new Mats.MaterialStack(stack.material, stack.amount - toAdd) : null;
    }

    @Override
    public boolean canAcceptPartialFlow(Level world, BlockPos pos, ForgeDirection side, Mats.MaterialStack stack) { return false; }

    @Override
    public Mats.MaterialStack flow(Level world, BlockPos pos, ForgeDirection side, Mats.MaterialStack stack) { return null; }

    // --- IControlReceiver (recipe selector) ---

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerCrucible menu
                && menu.getCrucible() == this && menu.stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection")) {
            int index = Tags.getIntOr(data, "index", 0);
            String selection = Tags.getStringOr(data, "selection", "null");
            if (index == 0) {
                this.recipe = selection;
                this.setChanged();
            }
        }
    }

    /** On destruction, drop the slot items (super) plus the molten content as scraps (old breakBlock). */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null || level.isClientSide()) return;
        List<Mats.MaterialStack> stacks = new ArrayList<>();
        stacks.addAll(recipeStack);
        stacks.addAll(wasteStack);
        for (Mats.MaterialStack stack : stacks) {
            ItemScraps.spawnMats(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    new Mats.MaterialStack(stack.material, stack.amount));
        }
        recipeStack.clear();
        wasteStack.clear();
    }


    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_crucible");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCrucible(containerId, playerInventory, this);
    }

    // --- sync (BufPacket) + persistence (1.12.2 keys) ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(progress);
        buf.writeInt(heat);
        buf.writeUtf(recipe);

        buf.writeShort(recipeStack.size());
        for (Mats.MaterialStack sta : recipeStack) {
            buf.writeInt(sta.material == null ? -1 : sta.material.id);
            buf.writeInt(sta.amount);
        }

        buf.writeShort(wasteStack.size());
        for (Mats.MaterialStack sta : wasteStack) {
            buf.writeInt(sta.material == null ? -1 : sta.material.id);
            buf.writeInt(sta.amount);
        }

        buf.writeInt(wastePourColor);
        buf.writeFloat(wastePourLen);
        buf.writeInt(recipePourColor);
        buf.writeFloat(recipePourLen);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        progress = buf.readInt();
        heat = buf.readInt();
        recipe = buf.readUtf();

        int rLen = buf.readShort() & 0xFFFF;
        List<Mats.MaterialStack> newRecipe = new ArrayList<>(rLen);
        for (int i = 0; i < rLen; i++) {
            int id = buf.readInt();
            int amt = buf.readInt();
            if (id >= 0) newRecipe.add(new Mats.MaterialStack(Mats.matById.get(id), amt));
        }
        int wLen = buf.readShort() & 0xFFFF;
        List<Mats.MaterialStack> newWaste = new ArrayList<>(wLen);
        for (int i = 0; i < wLen; i++) {
            int id = buf.readInt();
            int amt = buf.readInt();
            if (id >= 0) newWaste.add(new Mats.MaterialStack(Mats.matById.get(id), amt));
        }
        this.recipeStack = newRecipe;
        this.wasteStack = newWaste;

        wastePourColor = buf.readInt();
        wastePourLen = buf.readFloat();
        recipePourColor = buf.readInt();
        recipePourLen = buf.readFloat();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putString("recipe", this.recipe);

        // 1.7 kept (material id, amount) int pairs; materials are saved by name here
        Mats.writeStacks(output, "rec", recipeStack);
        Mats.writeStacks(output, "was", wasteStack);
        output.putInt("progress", progress);
        output.putInt("heat", heat);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.recipe = input.getStringOr("recipe", "null");

        this.recipeStack.clear();
        this.wasteStack.clear();

        recipeStack.addAll(Mats.readStacks(input, "rec"));
        wasteStack.addAll(Mats.readStacks(input, "was"));

        this.progress = input.getIntOr("progress", 0);
        this.heat = input.getIntOr("heat", 0);
    }
}
