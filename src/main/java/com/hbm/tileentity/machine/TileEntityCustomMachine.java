package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.ModCustomMachineBlocks;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.config.CustomMachineConfigJSON;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration;
import com.hbm.config.CustomMachineConfigJSON.MachineConfiguration.ComponentDefinition;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.inventory.container.ContainerMachineCustom;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CustomMachineRecipes;
import com.hbm.inventory.recipes.CustomMachineRecipes.CustomMachineRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.tileentity.ModCustomMachineBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
 * 1.7 {@code TileEntityCustomMachine}: the controller of a player-configured machine. Everything it does comes
 * from its {@link MachineConfiguration}: tank counts and sizes, item slot counts, generator or consumer mode, the
 * recipe set, and the component blocks that have to stand around it, checked every 300 ticks.
 *
 * <p>The {@code TileEntityMachinePolluting} base is inlined as the three smoke tanks. The port's energy and fluid
 * graphs find the controller at the {@code cm_port} proxies ({@link TileEntityCMPort}); its 1.7 {@code trySubscribe}
 * and {@code tryProvide} calls at the connection positions say which role each port carries
 * ({@link com.hbm.uninos.graph.PortClaims}).
 */
public class TileEntityCustomMachine extends TileEntityMachineBase
        implements IFluidStandardTransceiverMK2, IEnergyProviderMK2, IEnergyReceiverMK2, MenuProvider, WorldlyContainer {

    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];

    public @Nullable String machineType;
    public @Nullable MachineConfiguration config;

    public long power;
    public int flux;
    public int heat;
    public int maxHeat;
    public int progress;
    public int maxProgress = 1;
    public FluidTankNTM[] inputTanks = NO_TANKS;
    public FluidTankNTM[] outputTanks = NO_TANKS;
    public ModulePatternMatcher matcher = new ModulePatternMatcher(0);
    public int structureCheckDelay;
    public boolean structureOK = false;
    public @Nullable CustomMachineRecipe cachedRecipe;

    /** 1.7 {@code TileEntityMachinePolluting(22, 100)}: the smoke buffers, resized by {@code maxPollutionCap}. */
    public final FluidTankNTM smoke = new FluidTankNTM(Fluids.SMOKE, 100);
    public final FluidTankNTM smoke_leaded = new FluidTankNTM(Fluids.SMOKE_LEADED, 100);
    public final FluidTankNTM smoke_poison = new FluidTankNTM(Fluids.SMOKE_POISON, 100);

    public List<DirPos> connectionPos = new ArrayList<>();
    public List<DirPos> fluxPos = new ArrayList<>();
    public List<DirPos> heatPos = new ArrayList<>();

    public TileEntityCustomMachine(BlockPos pos, BlockState state) {
        /*
         * 0: Battery
         * 1-3: Fluid IDs
         * 4-9: Inputs
         * 10-15: Template
         * 16-21: Output
         */
        super(ModCustomMachineBlockEntities.CUSTOM_MACHINE.get(), pos, state, 22);
    }

    public void init() {
        MachineConfiguration config = CustomMachineConfigJSON.customMachines.get(this.machineType);

        if (config != null) {
            this.config = config;

            inputTanks = new FluidTankNTM[config.fluidInCount];
            for (int i = 0; i < inputTanks.length; i++) inputTanks[i] = new FluidTankNTM(Fluids.NONE, config.fluidInCap);
            outputTanks = new FluidTankNTM[config.fluidOutCount];
            for (int i = 0; i < outputTanks.length; i++) outputTanks[i] = new FluidTankNTM(Fluids.NONE, config.fluidOutCap);
            maxHeat = config.maxHeat;
            matcher = new ModulePatternMatcher(config.itemInCount);
            smoke.changeTankSize(config.maxPollutionCap);
            smoke_leaded.changeTankSize(config.maxPollutionCap);
            smoke_poison.changeTankSize(config.maxPollutionCap);

        } else if (level != null && !level.isClientSide()) {
            // 1.7 func_147480_a(x, y, z, false): a controller naming no configured machine removes itself
            level.destroyBlock(worldPosition, false);
        }
    }

    @Override
    public String getName() {
        return config != null ? config.localizedName : "INVALID";
    }

    @Override
    public String getDefaultName() {
        return getName();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCustomMachine te) {
        te.updateServer(level);
    }

    private void updateServer(Level level) {

        if (config == null) {
            level.destroyBlock(worldPosition, false);
            return;
        }

        this.power = Library.chargeTEFromItems(inventory, 0, power, this.config.maxPower);

        if (this.inputTanks.length > 0) setType(this.inputTanks[0], 1);
        if (this.inputTanks.length > 1) setType(this.inputTanks[1], 2);
        if (this.inputTanks.length > 2) setType(this.inputTanks[2], 3);

        this.structureCheckDelay--;
        if (this.structureCheckDelay <= 0) this.checkStructure();

        if (level.getGameTime() % 20 == 0) {
            for (DirPos pos : this.connectionPos) {
                for (FluidTankNTM tank : this.inputTanks) this.trySubscribe(tank.getTankType(), level, pos);
                if (!config.generatorMode) this.trySubscribe(level, pos);
            }
            for (byte d = 2; d < 6; d++) {
                ForgeDirection dir = ForgeDirection.getOrientation(d);
                for (DirPos pos : this.fluxPos) {
                    BlockPos reactorCell = pos.getPos().offset(dir.offsetX, 0, dir.offsetZ);
                    if (level.getBlockState(reactorCell).getBlock() == ModNuclearMiscBlocks.REACTOR_RESEARCH.get()) {
                        BlockPos source = MultiblockSurface.coreOfAny(level, reactorCell);
                        if (source != null) {

                            BlockEntity tile = level.getBlockEntity(source);

                            if (tile instanceof TileEntityReactorResearch reactor) {
                                this.flux = reactor.totalFlux;
                            }
                        }
                    }
                }
                if (config.maxHeat > 0) {
                    for (DirPos pos : this.heatPos) {
                        // 1.7 pulls from one block down and one more step sideways of each heat-face neighbour
                        this.tryPullHeat(level, pos.getPos().getX() + dir.offsetX, pos.getPos().getY() - 1, pos.getPos().getZ() + dir.offsetZ);
                    }
                }
            }
        }

        for (DirPos pos : this.connectionPos) {
            if (config.generatorMode && power > 0)
                this.tryProvide(level, pos.getPos().getX(), pos.getPos().getY(), pos.getPos().getZ(), pos.getDir());
            for (FluidTankNTM tank : this.outputTanks)
                if (tank.getFill() > 0) this.tryProvide(tank, level, pos.getPos(), pos.getDir());
            this.sendSmoke(level, pos);
        }

        if (this.structureOK) {

            if (config.generatorMode) {
                if (this.cachedRecipe == null) {
                    CustomMachineRecipe recipe = this.getMatchingRecipe();
                    if (recipe != null && this.hasRequiredQuantities(recipe) && this.hasSpace(recipe)) {
                        this.cachedRecipe = recipe;
                        this.useUpInput(recipe);
                    }
                }

                if (this.cachedRecipe != null) {
                    this.maxProgress = (int) Math.max(cachedRecipe.duration / this.config.recipeSpeedMult, 1);
                    int powerReq = (int) Math.max(cachedRecipe.consumptionPerTick * this.config.recipeConsumptionMult, 1);

                    this.progress++;
                    this.power += powerReq;
                    this.heat -= cachedRecipe.heat;
                    if (power > config.maxPower) power = config.maxPower;
                    if (level.getGameTime() % 20 == 0) {
                        pollution(cachedRecipe);
                        radiation(cachedRecipe);
                    }
                    if (progress >= this.maxProgress) {
                        this.progress = 0;
                        this.processRecipe(cachedRecipe);
                        this.cachedRecipe = null;
                    }
                }

            } else {
                CustomMachineRecipe recipe = this.getMatchingRecipe();

                if (recipe != null) {
                    this.maxProgress = (int) Math.max(recipe.duration / this.config.recipeSpeedMult, 1);
                    int powerReq = (int) Math.max(recipe.consumptionPerTick * this.config.recipeConsumptionMult, 1);

                    if (this.power >= powerReq && this.hasRequiredQuantities(recipe) && this.hasSpace(recipe)) {
                        this.progress++;
                        this.power -= powerReq;
                        this.heat -= recipe.heat;
                        if (level.getGameTime() % 20 == 0) {
                            pollution(recipe);
                            radiation(recipe);
                        }
                        if (progress >= this.maxProgress) {
                            this.progress = 0;
                            this.useUpInput(recipe);
                            this.processRecipe(recipe);
                        }
                    }
                } else {
                    this.progress = 0;
                }
            }
        } else {
            this.progress = 0;
        }
        this.networkPackNT();
    }

    /** 1.7 {@code FluidTank.setType(slot, slots)}: an identifier in the slot retypes the tank and empties it. */
    private void setType(FluidTankNTM tank, int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemFluidId id)) return;
        FluidType newType = id.getType(null, 0, 0, 0, stack);
        if (newType != null && tank.getTankType() != newType) {
            tank.setTankType(newType);
            tank.setFill(0);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);

        buf.writeUtf(this.machineType == null ? "" : this.machineType);

        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeInt(flux);
        buf.writeInt(heat);
        buf.writeBoolean(structureOK);
        buf.writeInt(maxProgress);
        for (FluidTankNTM inputTank : inputTanks) inputTank.serialize(buf);
        for (FluidTankNTM outputTank : outputTanks) outputTank.serialize(buf);
        this.matcher.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);

        this.machineType = buf.readUtf();
        if (this.config == null) this.init();

        this.power = buf.readLong();
        this.progress = buf.readInt();
        this.flux = buf.readInt();
        this.heat = buf.readInt();
        this.structureOK = buf.readBoolean();
        this.maxProgress = buf.readInt();
        for (FluidTankNTM inputTank : inputTanks) inputTank.deserialize(buf);
        for (FluidTankNTM outputTank : outputTanks) outputTank.deserialize(buf);
        this.matcher.deserialize(buf);
    }

    /** Only accepts inputs in a fixed order, saves a ton of performance because there's no permutations to check for */
    public @Nullable CustomMachineRecipe getMatchingRecipe() {
        List<CustomMachineRecipe> recipes = CustomMachineRecipes.get(this.config.recipeKey);
        if (recipes == null || recipes.isEmpty()) return null;

        outer:
        for (CustomMachineRecipe recipe : recipes) {
            for (int i = 0; i < recipe.inputFluids.length; i++) {
                if (this.inputTanks[i].getTankType() != recipe.inputFluids[i].type || this.inputTanks[i].getPressure() != recipe.inputFluids[i].pressure) continue outer;
            }

            for (int i = 0; i < recipe.inputItems.length; i++) {
                ItemStack slot = inventory.getStackInSlot(i + 4);
                if (recipe.inputItems[i] != null && slot.isEmpty()) continue outer;
                if (!recipe.inputItems[i].matchesRecipe(slot, true)) continue outer;
            }

            return recipe;
        }

        return null;
    }

    public void pollution(CustomMachineRecipe recipe) {
        if (recipe.pollutionAmount > 0) {
            this.pollute(PollutionType.valueOf(recipe.pollutionType), recipe.pollutionAmount);
        } else if (recipe.pollutionAmount < 0 && PollutionHandler.getPollution(level, worldPosition, PollutionType.valueOf(recipe.pollutionType)) >= -recipe.pollutionAmount) {
            PollutionHandler.decrementPollution(level, worldPosition, PollutionType.valueOf(recipe.pollutionType), -recipe.pollutionAmount);
        }
    }

    public void radiation(CustomMachineRecipe recipe) {
        if (recipe.radiationAmount > 0) {
            ChunkRadiationManager.proxy.incrementRad(level, worldPosition, recipe.radiationAmount);
        } else if (recipe.radiationAmount < 0) {
            ChunkRadiationManager.proxy.decrementRad(level, worldPosition, -recipe.radiationAmount);
        }
    }

    /** 1.7 {@code TileEntityMachinePolluting.pollute}. */
    public void pollute(PollutionType type, float amount) {
        FluidTankNTM tank = type == PollutionType.SOOT ? smoke : type == PollutionType.HEAVYMETAL ? smoke_leaded : smoke_poison;

        int fluidAmount = (int) Math.ceil(amount * 100);
        tank.setFill(tank.getFill() + fluidAmount);

        if (tank.getFill() > tank.getMaxFill()) {
            int overflow = tank.getFill() - tank.getMaxFill();
            tank.setFill(tank.getMaxFill());
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);

            // 1.7 NTMSounds.VANILLA_HISS, "random.fizz"
            if (level.getRandom().nextInt(3) == 0) level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.1F, 1.5F);
        }
    }

    /** 1.7 {@code TileEntityMachinePolluting.sendSmoke}. */
    public void sendSmoke(Level level, DirPos pos) {
        if (this.smoke.getFill() > 0) this.tryProvide(smoke, level, pos.getPos(), pos.getDir());
        if (this.smoke_leaded.getFill() > 0) this.tryProvide(smoke_leaded, level, pos.getPos(), pos.getDir());
        if (this.smoke_poison.getFill() > 0) this.tryProvide(smoke_poison, level, pos.getPos(), pos.getDir());
    }

    public FluidTankNTM[] getSmokeTanks() {
        return new FluidTankNTM[]{smoke, smoke_leaded, smoke_poison};
    }

    protected void tryPullHeat(Level level, int x, int y, int z) {
        BlockEntity con = level.getBlockEntity(new BlockPos(x, y, z));

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;

            if (diff == 0) {
                return;
            }

            if (diff > 0) {
                source.useUpHeat(diff);
                this.heat += diff;
                if (this.heat > this.maxHeat)
                    this.heat = this.maxHeat;
            }
        }
    }

    public boolean hasRequiredQuantities(CustomMachineRecipe recipe) {

        for (int i = 0; i < recipe.inputFluids.length; i++) {
            if (this.inputTanks[i].getFill() < recipe.inputFluids[i].fill) return false;
        }

        for (int i = 0; i < recipe.inputItems.length; i++) {
            ItemStack slot = inventory.getStackInSlot(i + 4);
            if (!slot.isEmpty() && slot.getCount() < recipe.inputItems[i].stacksize) return false;
        }
        if (config.fluxMode ? this.flux < recipe.flux : false) return false;
        if (config.maxHeat > 0 && recipe.heat > 0 ? this.heat < recipe.heat : false) return false;
        return true;
    }

    public boolean hasSpace(CustomMachineRecipe recipe) {

        for (int i = 0; i < recipe.outputFluids.length; i++) {
            if (this.outputTanks[i].getTankType() == recipe.outputFluids[i].type && this.outputTanks[i].getFill() + recipe.outputFluids[i].fill > this.outputTanks[i].getMaxFill()) return false;
        }

        for (int i = 0; i < recipe.outputItems.length; i++) {
            ItemStack slot = inventory.getStackInSlot(i + 16);
            ItemStack out = recipe.outputItems[i].stack();
            if (!slot.isEmpty() && !ItemStacks.sameItemAndData(slot, out)) return false;
            if (!slot.isEmpty() && slot.getCount() + out.getCount() > slot.getMaxStackSize()) return false;
        }

        return true;
    }

    public void useUpInput(CustomMachineRecipe recipe) {

        for (int i = 0; i < recipe.inputFluids.length; i++) {
            this.inputTanks[i].setFill(this.inputTanks[i].getFill() - recipe.inputFluids[i].fill);
        }

        for (int i = 0; i < recipe.inputItems.length; i++) {
            inventory.extractItem(i + 4, recipe.inputItems[i].stacksize, false);
        }
    }

    public void processRecipe(CustomMachineRecipe recipe) {

        for (int i = 0; i < recipe.outputFluids.length; i++) {
            if (this.outputTanks[i].getTankType() != recipe.outputFluids[i].type) this.outputTanks[i].setTankType(recipe.outputFluids[i].type);
            this.outputTanks[i].setFill(this.outputTanks[i].getFill() + recipe.outputFluids[i].fill);
        }

        for (int i = 0; i < recipe.outputItems.length; i++) {

            if (level.getRandom().nextFloat() < recipe.outputItems[i].chance()) {
                ItemStack slot = inventory.getStackInSlot(i + 16);
                if (slot.isEmpty()) {
                    inventory.setStackInSlot(i + 16, recipe.outputItems[i].stack().copy());
                } else {
                    ItemStack grown = slot.copy();
                    grown.grow(recipe.outputItems[i].stack().getCount());
                    inventory.setStackInSlot(i + 16, grown);
                }
            }
        }
    }

    /**
     * 1.7's component offset: {@code x - dir * c.x + rot * c.x}, with the east/west case written separately
     * because the general form does not hold there. Shared by the check, the builder and the renderer.
     */
    public static BlockPos componentPos(BlockPos core, ForgeDirection dir, ComponentDefinition comp) {
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        /* vvv precisely the same method used for defining ports vvv */
        int x = core.getX() - dir.offsetX * comp.x + rot.offsetX * comp.x;
        int y = core.getY() + comp.y;
        int z = core.getZ() - dir.offsetZ * comp.z + rot.offsetZ * comp.z;
        /* but for EW directions it just stops working entirely */
        /* there is absolutely zero reason why this should be required */
        if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
            x = core.getX() + dir.offsetZ * comp.z - rot.offsetZ * comp.z;
            z = core.getZ() + dir.offsetX * comp.x - rot.offsetX * comp.x;
        }
        /* i wholeheartedly believe it is the computer who is wrong here */
        return new BlockPos(x, y, z);
    }

    public ForgeDirection facing() {
        BlockState state = getBlockState();
        return state.hasProperty(com.hbm.blocks.machine.BlockCustomMachine.FACING)
                ? ForgeDirection.getOrientation(state.getValue(com.hbm.blocks.machine.BlockCustomMachine.FACING))
                : ForgeDirection.NORTH;
    }

    public boolean checkStructure() {

        this.connectionPos.clear();
        // 1.7 never cleared these two, so they grew by six entries per receiver on every check; the
        // duplicates changed nothing (a repeated heat pull finds no difference left), so they are reset here.
        this.fluxPos.clear();
        this.heatPos.clear();
        this.structureCheckDelay = 300;
        this.structureOK = false;
        if (this.config == null || level == null) return false;

        ForgeDirection dir = facing();
        for (ComponentDefinition comp : config.components) {
            BlockPos pos = componentPos(worldPosition, dir, comp);

            Block b = level.getBlockState(pos).getBlock();
            if (!comp.accepts(b)) return false;

            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof TileEntityCMPort proxy) {
                proxy.setCore(worldPosition);

                for (ForgeDirection facing : ForgeDirection.VALID_DIRECTIONS) {
                    this.connectionPos.add(new DirPos(pos.getX() + facing.offsetX, pos.getY() + facing.offsetY, pos.getZ() + facing.offsetZ, facing));
                }
            }
            if (b == ModCustomMachineBlocks.cm_flux.get()) {
                for (ForgeDirection facing : ForgeDirection.VALID_DIRECTIONS) {
                    this.fluxPos.add(new DirPos(pos.getX() + facing.offsetX, pos.getY() + facing.offsetY, pos.getZ() + facing.offsetZ, facing));
                }
            } else if (b == ModCustomMachineBlocks.cm_heat.get()) {
                for (ForgeDirection facing : ForgeDirection.VALID_DIRECTIONS) {
                    this.heatPos.add(new DirPos(pos.getX() + facing.offsetX, pos.getY() + facing.offsetY, pos.getZ() + facing.offsetZ, facing));
                }
            }

        }
        for (ForgeDirection facing : ForgeDirection.VALID_DIRECTIONS) {
            this.connectionPos.add(new DirPos(worldPosition.getX() + facing.offsetX, worldPosition.getY() + facing.offsetY, worldPosition.getZ() + facing.offsetZ, facing));
        }

        this.structureOK = true;
        return true;
    }

    public void buildStructure() {

        if (this.config == null || level == null) return;

        ForgeDirection dir = facing();

        for (ComponentDefinition comp : config.components) {
            BlockPos pos = componentPos(worldPosition, dir, comp);
            level.setBlock(pos, comp.blocks().get(0).defaultBlockState(), 3);
        }
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        if (this.config == null) return new int[]{};
        if (this.config.itemInCount > 5) return new int[]{4, 5, 6, 7, 8, 9, 16, 17, 18, 19, 20, 21};
        if (this.config.itemInCount > 4) return new int[]{4, 5, 6, 7, 8, 16, 17, 18, 19, 20, 21};
        if (this.config.itemInCount > 3) return new int[]{4, 5, 6, 7, 16, 17, 18, 19, 20, 21};
        if (this.config.itemInCount > 2) return new int[]{4, 5, 6, 16, 17, 18, 19, 20, 21};
        if (this.config.itemInCount > 1) return new int[]{4, 5, 16, 17, 18, 19, 20, 21};
        if (this.config.itemInCount > 0) return new int[]{4, 16, 17, 18, 19, 20, 21};
        return new int[]{16, 17, 18, 19, 20, 21};
    }

    public boolean canExtractItem(int i, ItemStack stack, int j) {
        return i >= 16 && i <= 21;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 4 || slot > 9) return false;

        int index = slot - 4;
        int filterSlot = slot + 6;

        if (inventory.getStackInSlot(filterSlot).isEmpty()) return true;

        return matcher.isValidForFilter(inventory.getStackInSlot(filterSlot), index, stack);
    }

    @Override public int[] getSlotsForFace(Direction side) { return getAccessibleSlotsFromSide(side.ordinal()); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return canExtractItem(slot, stack, side.ordinal()); }

    /** 1.7 {@code BlockCustomMachine.breakBlock}: everything spills except the six pattern filters. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (int i = 0; i < getContainerSize(); i++) {
            if (i >= 10 && i <= 15) continue; // do NOT drop the filters
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    protected void loadData(DataIn nbt) {

        this.machineType = nbt.getStringOr("machineType", "");
        this.config = null;
        this.init();

        super.loadData(nbt);

        if (this.config != null) {

            for (int i = 0; i < inputTanks.length; i++) inputTanks[i].readFromNBT(nbt, "i" + i);
            for (int i = 0; i < outputTanks.length; i++) outputTanks[i].readFromNBT(nbt, "o" + i);

            this.matcher.load(nbt);

            int index = nbt.getIntOr("cachedIndex", -1);
            List<CustomMachineRecipe> recipes = index != -1 ? CustomMachineRecipes.get(this.config.recipeKey) : null;
            if (recipes != null && index >= 0 && index < recipes.size()) {
                this.cachedRecipe = recipes.get(index);
            }
        }
        smoke.readFromNBT(nbt, "smoke0");
        smoke_leaded.readFromNBT(nbt, "smoke1");
        smoke_poison.readFromNBT(nbt, "smoke2");
    }

    @Override
    protected void saveData(DataOut nbt) {

        if (machineType == null || this.config == null) {
            super.saveData(nbt);
            return;
        }

        nbt.putString("machineType", machineType);

        super.saveData(nbt);

        for (int i = 0; i < inputTanks.length; i++) inputTanks[i].writeToNBT(nbt, "i" + i);
        for (int i = 0; i < outputTanks.length; i++) outputTanks[i].writeToNBT(nbt, "o" + i);

        // 1.7 never wrote the power buffer: a reloaded controller starts empty. Kept.
        this.matcher.save(nbt);

        if (this.cachedRecipe != null) {
            int index = CustomMachineRecipes.get(this.config.recipeKey).indexOf(this.cachedRecipe);
            nbt.putInt("cachedIndex", index);
        } else {
            nbt.putInt("cachedIndex", -1);
        }
        smoke.writeToNBT(nbt, "smoke0");
        smoke_leaded.writeToNBT(nbt, "smoke1");
        smoke_poison.writeToNBT(nbt, "smoke2");
    }

    @Override
    public FluidTankNTM[] getAllTanks() {

        FluidTankNTM[] all = new FluidTankNTM[inputTanks.length + outputTanks.length];

        for (int i = 0; i < inputTanks.length; i++) all[i] = inputTanks[i];
        for (int i = 0; i < outputTanks.length; i++) all[inputTanks.length + i] = outputTanks[i];

        return all;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        FluidTankNTM[] smokes = this.getSmokeTanks();
        FluidTankNTM[] all = new FluidTankNTM[outputTanks.length + smokes.length];
        for (int i = 0; i < outputTanks.length; i++) all[i] = outputTanks[i];
        for (int i = 0; i < smokes.length; i++) all[outputTanks.length + i] = smokes[i];
        return all;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return inputTanks != null ? inputTanks : NO_TANKS;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal(config != null ? config.displayName() : "INVALID");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        if (this.config == null) return null;
        return new ContainerMachineCustom(containerId, playerInventory, this);
    }

    @Override
    public long getPower() {
        return this.power;
    }

    @Override
    public long getMaxPower() {
        return this.config != null ? this.config.maxPower : 1;
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long transferPower(long power, boolean simulate) {
        if (this.config != null && this.config.generatorMode) return power;
        return IEnergyReceiverMK2.super.transferPower(power, simulate);
    }

    @Override
    public long getReceiverSpeed() {
        if (this.config != null && !this.config.generatorMode) return this.getMaxPower();
        return 0;
    }

    @Override
    public long getProviderSpeed() {
        if (this.config != null && this.config.generatorMode) return this.getMaxPower();
        return 0;
    }
}
