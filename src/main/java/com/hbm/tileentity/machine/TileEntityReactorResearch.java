package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIngredientItems.PlateFuelType;
import com.hbm.items.machine.ItemPlateFuel;
import com.hbm.items.machine.ItemDepletedFuel;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.HashMap;
import java.util.Map;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.inventory.container.ContainerReactorResearch;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;

/**
 * The legacy research reactor: twelve plate-fuel slots in a hex layout, water cooling through the sides of
 * the column, a control-rod level with 0.04/tick travel, and flux that feeds adjacent breeders. 1:1 with the
 * 1.7 {@code TileEntityReactorResearch} (OpenComputers hooks aside).
 */
public final class TileEntityReactorResearch extends TileEntityMachineBase implements MenuProvider, IControlReceiver, WorldlyContainer {

    /** 1.7 {@code lastLevel} / {@code level}; renamed because {@code level} is the BlockEntity world field. */
    public double lastRodLevel;
    public double rodLevel;
    public double speed = 0.04;
    public double targetLevel;

    public int heat;
    public byte water;
    public final int maxHeat = 50000;
    public int[] slotFlux = new int[12];
    public int totalFlux = 0;

    private static final int[] slot_io = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    public TileEntityReactorResearch(BlockPos pos, BlockState state) {
        super(ModNuclearMiscBlockEntities.REACTOR_RESEARCH.get(), pos, state, 12);
    }

    private static Map<Item, ItemStack> fuelMap;

    /** 1.7 {@code fuelMap}: fresh plate -> hot spent plate (metadata 1). */
    private static Map<Item, ItemStack> fuelMap() {
        if (fuelMap == null) {
            Map<Item, ItemStack> map = new HashMap<>();
            map.put(ModIngredientItems.plateFuel(PlateFuelType.U233), ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_U233.get(), 1));
            map.put(ModIngredientItems.plateFuel(PlateFuelType.U235), ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_U235.get(), 1));
            map.put(ModIngredientItems.plateFuel(PlateFuelType.MOX), ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_MOX.get(), 1));
            map.put(ModIngredientItems.plateFuel(PlateFuelType.PU239), ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_PU239.get(), 1));
            map.put(ModIngredientItems.plateFuel(PlateFuelType.SA326), ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_SA326.get(), 1));
            map.put(ModIngredientItems.plateFuel(PlateFuelType.RA226BE), ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_RA226BE.get(), 1));
            map.put(ModIngredientItems.plateFuel(PlateFuelType.PU238BE), ItemDepletedFuel.makeHot(ModNuclearMiscItems.WASTE_PLATE_PU238BE.get(), 1));
            fuelMap = map;
        }
        return fuelMap;
    }

    @Override
    public String getDefaultName() {
        return "container.reactorResearch";
    }

    /** 1.7: {@code i < 12 && i <= 0} — only slot 0 ever accepts a plate through automation. */
    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 12 && slot <= 0) return stack.getItem().getClass() == ItemPlateFuel.class;
        return false;
    }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return slot_io; }
    @Override public int[] getSlotsForFace(Direction side) { return slot_io; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot < 12 && slot >= 0) {
            for (ItemStack waste : fuelMap().values()) {
                if (waste.getItem() == stack.getItem()) return true;
            }
        }
        return false;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityReactorResearch te) {
        te.lastRodLevel = te.rodLevel;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityReactorResearch te) {
        te.rodControl();

        te.totalFlux = 0;

        if (te.rodLevel > 0) te.reaction();

        if (te.heat > 0) {
            te.water = te.getWater();

            if (te.water > 0) {
                te.heat = (int) (te.heat - te.heat * (float) 0.07 * te.water / 12); // 1.7 compound int -= float: truncate the difference
            } else if (te.water == 0) {
                te.heat -= 1;
            }

            if (te.heat < 0) te.heat = 0;
        }

        if (te.heat > te.maxHeat) {
            te.explode();
            return;
        }

        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        if (te.rodLevel > 0 && te.heat > 0 && !(te.blocksRad(x + 1, y + 1, z) && te.blocksRad(x - 1, y + 1, z)
                && te.blocksRad(x, y + 1, z + 1) && te.blocksRad(x, y + 1, z - 1))) {
            float rad = (float) te.heat / (float) te.maxHeat * 50F;
            ChunkRadiationManager.proxy.incrementRad(level, pos, rad);
        }

        te.setChanged();
        te.networkPackNT();
    }

    private boolean isWater(BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER);
    }

    public byte getWater() {
        byte water = 0;
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        // 1.7: the block just above the column top / just below the base, then three high on each side
        if (isWater(new BlockPos(x, y + 3, z))) water++;
        if (isWater(new BlockPos(x, y - 1, z))) water++;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int i = 0; i < 3; i++) {
                if (isWater(new BlockPos(x + dir.getStepX(), y + i, z + dir.getStepZ()))) water++;
            }
        }
        return water;
    }

    public boolean isSubmerged() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return isWater(new BlockPos(x + 1, y + 1, z)) || isWater(new BlockPos(x, y + 1, z + 1))
                || isWater(new BlockPos(x - 1, y + 1, z)) || isWater(new BlockPos(x, y + 1, z - 1));
    }

    private boolean blocksRad(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        Block b = state.getBlock();
        FluidState fluid = level.getFluidState(pos);

        if (fluid.is(FluidTags.WATER) && fluid.isSource()) return true;

        if (b == ModHazardBlocks.block_lead.get() || b == ModBlocks.block_desh.get()
                || b == ModNuclearMiscBlocks.REACTOR_RESEARCH.get() || b == ModNuclearMiscBlocks.MACHINE_REACTOR_BREEDING.get()) return true;

        return b.getExplosionResistance() >= 100;
    }

    private int[] getNeighboringSlots(int id) {
        return switch (id) {
            case 0 -> new int[]{1, 5};
            case 1 -> new int[]{0, 6};
            case 2 -> new int[]{3, 7};
            case 3 -> new int[]{2, 4, 8};
            case 4 -> new int[]{3, 9};
            case 5 -> new int[]{0, 6, 0xA};
            case 6 -> new int[]{1, 5, 0xB};
            case 7 -> new int[]{2, 8};
            case 8 -> new int[]{3, 7, 9};
            case 9 -> new int[]{4, 8};
            case 10 -> new int[]{5, 0xB};
            case 11 -> new int[]{6, 0xA};
            default -> null;
        };
    }

    private void reaction() {
        for (int i = 0; i < 12; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                slotFlux[i] = 0;
                continue;
            }

            if (stack.getItem() instanceof ItemPlateFuel rod) {
                int outFlux = rod.react(level, stack, slotFlux[i]);
                this.heat += outFlux * 2;
                slotFlux[i] = 0;
                totalFlux += outFlux;

                int[] neighborSlots = getNeighboringSlots(i);

                if (ItemPlateFuel.getLifeTime(stack) > rod.lifeTime) {
                    ItemStack waste = fuelMap().get(stack.getItem());
                    inventory.setStackInSlot(i, waste == null ? ItemStack.EMPTY : waste.copy());
                }

                for (int neighbor : neighborSlots) {
                    slotFlux[neighbor] += (int) (outFlux * rodLevel);
                }
                continue;
            }

            // 1.7: a bred meteorite sword in any rod slot becomes the irradiated one (whenever the reaction runs)
            if (stack.is(com.hbm.items.ModAbilityToolItems.meteorite_sword_bred.get())) {
                inventory.setStackInSlot(i, new ItemStack(com.hbm.items.ModAbilityToolItems.meteorite_sword_irradiated.get()));
            }
            slotFlux[i] = 0;
        }
    }

    private void explode() {
        for (int i = 0; i < inventory.getSlots(); i++) inventory.setStackInSlot(i, ItemStack.EMPTY);

        Level world = this.level;
        BlockPos pos = worldPosition;
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();

        // 1.7 setBlockToAir on the core tears the dummies down with it
        world.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

        if (isWater(new BlockPos(x, y + 3, z))) world.setBlock(new BlockPos(x, y + 3, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        if (isWater(new BlockPos(x, y - 1, z))) world.setBlock(new BlockPos(x, y - 1, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int i = 0; i < 3; i++) {
                BlockPos side = new BlockPos(x + dir.getStepX(), y + i, z + dir.getStepZ());
                if (isWater(side)) world.setBlock(side, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }

        world.explode(null, x, y, z, 18.0F, true, Level.ExplosionInteraction.BLOCK);
        world.setBlock(pos, ModBlocks.deco_steel.get().defaultBlockState(), 3);
        world.setBlock(pos.above(), ModFluidNetBlocks.corium_block.get().defaultBlockState(), 3);
        world.setBlock(pos.above(2), ModBlocks.deco_steel.get().defaultBlockState(), 3);

        ChunkRadiationManager.proxy.incrementRad(world, pos, 50);
        // 1.7: every player within 100 blocks is marked for BossSpawnHandler's radiation beasts
        if (com.hbm.config.MobConfig.enableElementals) {
            for (net.minecraft.world.entity.player.Player player : world.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                    new net.minecraft.world.phys.AABB(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5).inflate(100.0D))) {
                com.hbm.extprop.HbmPlayerProps.getData(player).radMark = true;
            }
        }
    }

    //Control Rods
    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 20D * 20D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("level")) this.setTarget(Tags.getDoubleOr(data, "level", 0D));
        this.setChanged();
    }

    public void setTarget(double target) {
        this.targetLevel = target;
    }

    public void rodControl() {
        if (rodLevel < targetLevel) {
            rodLevel += speed;
            if (rodLevel >= targetLevel) rodLevel = targetLevel;
        }
        if (rodLevel > targetLevel) {
            rodLevel -= speed;
            if (rodLevel <= targetLevel) rodLevel = targetLevel;
        }
    }

    public int[] getDisplayData() {
        int[] data = new int[2];
        data[0] = this.totalFlux;
        data[1] = (int) Math.round((this.heat) * 0.00002 * 980 + 20);
        return data;
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerReactorResearch(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeByte(water);
        buf.writeDouble(rodLevel);
        buf.writeDouble(targetLevel);
        buf.writeVarIntArray(slotFlux);
        buf.writeInt(totalFlux);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        water = buf.readByte();
        rodLevel = buf.readDouble();
        targetLevel = buf.readDouble();
        slotFlux = buf.readVarIntArray();
        totalFlux = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("heat", heat);
        output.putByte("water", water);
        output.putDouble("level", rodLevel);
        output.putDouble("targetLevel", targetLevel);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        heat = input.getIntOr("heat", 0);
        water = input.getByteOr("water", (byte) 0);
        rodLevel = input.getDoubleOr("level", 0D);
        targetLevel = input.getDoubleOr("targetLevel", 0D);
    }
}
