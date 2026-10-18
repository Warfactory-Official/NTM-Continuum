package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.data.MachinePartDataMaps;
import dev.golem.core.data.Tags;

import com.hbm.api.conveyor.IConveyorBelt;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.blocks.generic.BlockDepth;
import com.hbm.blocks.generic.BlockGasBase;
import com.hbm.util.ConveyorInventories;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.machine.DrillBitType;
import com.hbm.items.machine.ItemDrillbit;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.special.ItemBedrockOreBase;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.blocks.ModSecurityBlocks;
import com.hbm.tileentity.BedrockOreBlockEntity;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.CompatEnchantments;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.inventory.container.ContainerMachineExcavator;
import com.hbm.tileentity.ModProduction2BlockEntities;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * 1.7 {@code TileEntityMachineExcavator}: the drill-bit driven large mining drill. Slot layout: 0 battery,
 * 1 fluid identifier, 2-3 upgrades, 4 drill bit, 5-13 item buffer. The drill descends one ring level at a
 * time, breaking a ring of radius {@code 1 + 2 * effect level} around the shaft, optionally walling the
 * hole, vein-mining ores, silk-touching and crushing drops, and stops at depth rock or bedrock ore (which
 * it extracts every five minutes with the matching drilling fluid).
 */
public class TileEntityMachineExcavator extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IControlReceiver, IUpgradeInfoProvider,
        IRORValueProvider, IRORInteractive {

    public static final long maxPower = 1_000_000;
    public long power;
    public boolean operational = false;

    public boolean enableDrill = false;
    public boolean enableCrusher = false;
    public boolean enableWalling = false;
    public boolean enableVeinMiner = false;
    public boolean enableSilkTouch = false;

    protected int ticksWorked = 0;
    /** 0 is the first block below the null position. */
    protected int targetDepth = 0;
    protected boolean bedrockDrilling = false;

    public float drillRotation = 0F;
    public float prevDrillRotation = 0F;
    public float drillExtension = 0F;
    public float prevDrillExtension = 0F;
    public float crusherRotation = 0F;
    public float prevCrusherRotation = 0F;
    public int chuteTimer = 0;

    public double speed = 1.0D;
    public final long baseConsumption = 10_000L;
    public long consumption = baseConsumption;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.NONE, 16_000);

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineExcavator(BlockPos pos, BlockState state) {
        super(ModProduction2BlockEntities.EXCAVATOR.get(), pos, state, 14);
    }

    @Override
    public String getDefaultName() {
        return "container.machineExcavator";
    }

    public int getTargetDepth() { return targetDepth; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineExcavator te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        // needs to happen on client too for GUI rendering
        te.upgradeManager.checkSlots(te.inventory, 2, 3);
        int speedLevel = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = te.upgradeManager.getLevel(UpgradeType.POWER);

        te.consumption = te.baseConsumption * (1 + speedLevel);
        te.consumption /= (1 + powerLevel);

        te.tank.setType(te.inventory, 1, 1);

        if (level.getGameTime() % 20 == 0) {
            te.tryEjectBuffer();
        }

        if (te.chuteTimer > 0) te.chuteTimer--;

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.getPower(), te.getMaxPower());
        te.operational = false;
        int radiusLevel = te.upgradeManager.getLevel(UpgradeType.EFFECT);

        var type = te.getInstalledDrill();
        if (te.enableDrill && type != null && te.power >= te.getPowerConsumption()) {

            te.operational = true;
            te.power -= te.getPowerConsumption();

            te.speed = type.speed();
            te.speed *= (1 + speedLevel / 2D);

            // 1.7: yCoord - 4 with the world floor at 0; the floor is the level's minimum here.
            int maxDepth = pos.getY() - 4 - level.getMinY();

            if ((te.bedrockDrilling || te.targetDepth <= maxDepth) && te.tryDrill(1 + radiusLevel * 2)) {
                te.targetDepth++;

                if (te.targetDepth > maxDepth) {
                    te.enableDrill = false;
                }
            }
        } else {
            te.targetDepth = 0;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineExcavator te) {
        te.upgradeManager.checkSlots(te.inventory, 2, 3);
        int speedLevel = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = te.upgradeManager.getLevel(UpgradeType.POWER);
        te.consumption = te.baseConsumption * (1 + speedLevel);
        te.consumption /= (1 + powerLevel);

        te.prevDrillExtension = te.drillExtension;

        if (te.drillExtension != te.targetDepth) {
            float diff = Math.abs(te.drillExtension - te.targetDepth);
            float speed = Math.max(0.15F, diff / 10F);

            if (diff <= speed) {
                te.drillExtension = te.targetDepth;
            } else {
                float sig = Math.signum(te.drillExtension - te.targetDepth);
                te.drillExtension -= sig * speed;
            }
        }

        te.prevDrillRotation = te.drillRotation;
        te.prevCrusherRotation = te.crusherRotation;

        if (te.operational) {
            te.drillRotation += 15F;

            if (te.enableCrusher) {
                te.crusherRotation += 15F;
            }
        }

        if (te.drillRotation >= 360F) {
            te.drillRotation -= 360F;
            te.prevDrillRotation -= 360F;
        }

        if (te.crusherRotation >= 360F) {
            te.crusherRotation -= 360F;
            te.prevCrusherRotation -= 360F;
        }
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX * 3 + rot.offsetX, y + 1, z + dir.offsetZ * 3 + rot.offsetZ, dir),
                    PortDef.make(x + dir.offsetX * 3 - rot.offsetX, y + 1, z + dir.offsetZ * 3 - rot.offsetZ, dir),
                    PortDef.make(x + rot.offsetX * 3, y + 1, z + rot.offsetZ * 3, rot),
                    PortDef.make(x - rot.offsetX * 3, y + 1, z - rot.offsetZ * 3, rot.getOpposite()),
            };
        }
        return cachedPorts;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(enableDrill);
        buf.writeBoolean(enableCrusher);
        buf.writeBoolean(enableWalling);
        buf.writeBoolean(enableVeinMiner);
        buf.writeBoolean(enableSilkTouch);
        buf.writeBoolean(operational);
        buf.writeInt(targetDepth);
        buf.writeInt(chuteTimer);
        buf.writeLong(power);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        enableDrill = buf.readBoolean();
        enableCrusher = buf.readBoolean();
        enableWalling = buf.readBoolean();
        enableVeinMiner = buf.readBoolean();
        enableSilkTouch = buf.readBoolean();
        operational = buf.readBoolean();
        targetDepth = buf.readInt();
        chuteTimer = buf.readInt();
        power = buf.readLong();
        tank.deserialize(buf);
    }

    protected int getY() {
        return worldPosition.getY() - targetDepth - 4;
    }

    /** Works outwards and tries to break a ring, returns true if all rings are broken (or ignorable) and the drill should extend. */
    protected boolean tryDrill(int radius) {
        int y = getY();
        int xCoord = worldPosition.getX(), zCoord = worldPosition.getZ();

        if (targetDepth == 0 || y == level.getMinY()) {
            radius = 1;
        }

        for (int ring = 1; ring <= radius; ring++) {

            boolean ignoreAll = true;
            float combinedHardness = 0F;
            BlockPos bedrockOre = null;
            bedrockDrilling = false;

            for (int x = xCoord - ring; x <= xCoord + ring; x++) {
                for (int z = zCoord - ring; z <= zCoord + ring; z++) {

                    /* Process blocks either if we are in the inner ring (1 = 3x3) or if the target block is on the outer edge */
                    if (ring == 1 || (x == xCoord - ring || x == xCoord + ring || z == zCoord - ring || z == zCoord + ring)) {

                        BlockPos p = new BlockPos(x, y, z);
                        BlockState b = level.getBlockState(p);

                        if (b.is(ModOreBlocks.ore_bedrock_block.get())) {
                            combinedHardness = 5 * 60 * 20;
                            bedrockOre = p;
                            bedrockDrilling = true;
                            enableCrusher = false;
                            ignoreAll = false;
                            break; // 1.7: only the inner z loop ends; later x columns still add their hardness and may re-pick the ore
                        }

                        // if hitting depth rock, turn off the drill
                        if (b.getBlock() instanceof BlockDepth) {
                            this.enableDrill = false;
                        }

                        if (shouldIgnoreBlock(b, p)) continue;

                        ignoreAll = false;

                        combinedHardness += b.getDestroySpeed(level, p);
                    }
                }
            }

            if (!ignoreAll) {
                ticksWorked++;

                int ticksToWork = (int) Math.ceil(combinedHardness / this.speed);

                if (ticksWorked >= ticksToWork) {

                    if (bedrockOre == null) {
                        breakBlocks(ring);
                        buildWall(ring + 1, ring == radius && this.enableWalling);
                        if (ring == radius) mineOuterOres(ring + 1);
                        tryCollect(radius + 1);
                    } else {
                        collectBedrock(bedrockOre);
                    }
                    ticksWorked = 0;
                }

                return false;
            } else {
                tryCollect(radius + 1);
            }
        }

        buildWall(radius + 1, this.enableWalling);
        ticksWorked = 0;
        return true;
    }

    protected void collectBedrock(BlockPos pos) {
        BlockEntity oreTile = level.getBlockEntity(pos);

        if (oreTile instanceof BedrockOreBlockEntity ore) {

            ItemStack resource = ore.getResource();
            if (resource.isEmpty()) return;
            var drill = this.getInstalledDrill();
            if (drill == null || ore.getTier() > drill.tier()) return;
            FluidStack acid = ore.getAcidRequirement();
            if (acid != null) {

                if (acid.type != tank.getTankType() || acid.fill > tank.getFill()) return;

                tank.setFill(tank.getFill() - acid.fill);
            }

            ItemStack stack = resource.copy();
            List<ItemStack> stacks = new ArrayList<>();
            stacks.add(stack);

            if (stack.is(ModOreBlocks.bedrock_ore_base.get())) {
                // 1.7: 1 + fortune * 0.1 (the ported BedrockOreBlockEntity.extract uses 0.25; the original value is kept here)
                ItemBedrockOreBase.setOreAmount(level, stack, pos.getX(), pos.getZ(), 1D + drill.fortune() * 0.1D);
            }

            ForgeDirection dir = MachineCore.facing(getBlockState());
            BlockPos out = outputPos(dir);

            /* try to insert into a valid container */
            Container tile = ConveyorInventories.containerAt(level, out);
            if (tile != null) {
                supplyContainer(tile, stacks, dir.getOpposite());
            }

            if (stack.getCount() <= 0) return;

            /* try to place on conveyor belt */
            Block b = level.getBlockState(out).getBlock();
            if (b instanceof IConveyorBelt belt) {
                supplyConveyor(belt, stacks, out);
            }

            if (stack.getCount() <= 0) return;

            for (int i = 5; i < 14; i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                if (!slot.isEmpty() && slot.getCount() < slot.getMaxStackSize() && ItemStacks.sameItemAndData(stack, slot)) {
                    int toAdd = Math.min(slot.getMaxStackSize() - slot.getCount(), stack.getCount());
                    slot.grow(toAdd);
                    stack.shrink(toAdd);
                    inventory.setStackInSlot(i, slot);

                    chuteTimer = 40;

                    if (stack.getCount() <= 0) {
                        return;
                    }
                }
            }

            /* add leftovers to empty slots */
            for (int i = 5; i < 14; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    chuteTimer = 40;
                    inventory.setStackInSlot(i, stack.copy());
                    return;
                }
            }
        }
    }

    private BlockPos outputPos(ForgeDirection dir) {
        return new BlockPos(worldPosition.getX() + dir.offsetX * 4, worldPosition.getY() - 3, worldPosition.getZ() + dir.offsetZ * 4);
    }

    /** breaks and drops all blocks in the specified ring */
    protected void breakBlocks(int ring) {
        int y = getY();
        int xCoord = worldPosition.getX(), zCoord = worldPosition.getZ();

        for (int x = xCoord - ring; x <= xCoord + ring; x++) {
            for (int z = zCoord - ring; z <= zCoord + ring; z++) {

                if (ring == 1 || (x == xCoord - ring || x == xCoord + ring || z == zCoord - ring || z == zCoord + ring)) {

                    BlockPos p = new BlockPos(x, y, z);
                    BlockState b = level.getBlockState(p);

                    if (!this.shouldIgnoreBlock(b, p)) {
                        tryMineAtLocation(p);
                    }
                }
            }
        }
    }

    public void tryMineAtLocation(BlockPos p) {

        BlockState b = level.getBlockState(p);
        var drill = getInstalledDrill();

        if (this.enableVeinMiner && drill != null && drill.vein()) {

            if (isOre(b)) {
                minX = p.getX();
                minY = p.getY();
                minZ = p.getZ();
                maxX = p.getX();
                maxY = p.getY();
                maxZ = p.getZ();
                breakRecursively(p, 10);
                recursionBrake.clear();

                /* move all excavated items to the last drillable position which is also within collection range */
                List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1));
                for (ItemEntity item : items) item.setPos(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5);

                return;
            }
        }
        breakSingleBlock(b, p);
    }

    /** 1.7 checked the block item for any ore-dictionary name starting with "ore"; the tag equivalents are {@code c:ores*} and vanilla's {@code *_ores}. */
    protected boolean isOre(BlockState b) {
        if (b.getBlock().builtInRegistryHolder().tags().anyMatch(tag -> {
            String path = tag.location().getPath();
            return (tag.location().getNamespace().equals("c") && path.startsWith("ores")) || path.endsWith("_ores");
        })) return true;
        ItemStack blockItem = new ItemStack(b.getBlock().asItem());
        if (blockItem.isEmpty()) return false;
        return blockItem.getItem().builtInRegistryHolder().tags().anyMatch(tag -> tag.location().getNamespace().equals("c") && tag.location().getPath().startsWith("ores"));
    }

    private final Set<BlockPos> recursionBrake = new HashSet<>();
    private int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;

    protected void breakRecursively(BlockPos p, int depth) {

        if (depth < 0) return;
        if (recursionBrake.contains(p)) return;
        recursionBrake.add(p);

        BlockState b = level.getBlockState(p);

        for (Direction dir : Direction.values()) {
            BlockPos ip = p.relative(dir);

            if (level.getBlockState(ip).getBlock() == b.getBlock()) {
                breakRecursively(ip, depth - 1);
            }
        }

        breakSingleBlock(b, p);

        if (p.getX() < minX) minX = p.getX();
        if (p.getX() > maxX) maxX = p.getX();
        if (p.getY() < minY) minY = p.getY();
        if (p.getY() > maxY) maxY = p.getY();
        if (p.getZ() < minZ) minZ = p.getZ();
        if (p.getZ() > maxZ) maxZ = p.getZ();

        if (this.enableWalling) {
            level.setBlock(p, ModSecurityBlocks.barricade.get().defaultBlockState(), 3);
        }
    }

    protected void breakSingleBlock(BlockState b, BlockPos p) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        // 1.7: b.getDrops(world, x, y, z, meta, fortune), then createStackedBlock for silk touch.
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        if (this.canSilkTouch()) {
            CompatEnchantments.enchantSilkTouch(serverLevel, tool);
        } else {
            CompatEnchantments.enchantFortune(serverLevel, tool, this.getFortuneLevel());
        }
        List<ItemStack> items = new ArrayList<>(Block.getDrops(b, serverLevel, p, level.getBlockEntity(p), null, tool));

        if (this.enableCrusher) {

            List<ItemStack> list = new ArrayList<>();

            for (ItemStack stack : items) {
                ItemStack crushed = ShredderRecipes.getShredderResult(stack).copy();

                if (crushed.getItem() == ModItems.scrap.get() || crushed.getItem() == ModItems.dust.get()) {
                    list.add(stack);
                } else {
                    crushed.setCount(crushed.getCount() * stack.getCount());
                    list.add(crushed);
                }
            }

            items = list;
        }

        if (b.is(ModSecurityBlocks.barricade.get()))
            items.clear();

        for (ItemStack item : items) {
            level.addFreshEntity(new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, item));
        }

        level.destroyBlock(p, false);
    }

    /** builds a wall along the specified ring, replacing fluid blocks. if wallEverything is set, it will also wall off replacable blocks like air or grass */
    protected void buildWall(int ring, boolean wallEverything) {
        int y = getY();
        int xCoord = worldPosition.getX(), zCoord = worldPosition.getZ();

        for (int x = xCoord - ring; x <= xCoord + ring; x++) {
            for (int z = zCoord - ring; z <= zCoord + ring; z++) {

                BlockPos p = new BlockPos(x, y, z);
                BlockState b = level.getBlockState(p);

                if (x == xCoord - ring || x == xCoord + ring || z == zCoord - ring || z == zCoord + ring) {

                    if (b.canBeReplaced() && (wallEverything || b.liquid())) {
                        level.setBlock(p, ModSecurityBlocks.barricade.get().defaultBlockState(), 3);
                    }
                } else {

                    if (b.liquid()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    protected void mineOuterOres(int ring) {
        int y = getY();
        int xCoord = worldPosition.getX(), zCoord = worldPosition.getZ();

        for (int x = xCoord - ring; x <= xCoord + ring; x++) {
            for (int z = zCoord - ring; z <= zCoord + ring; z++) {

                if (ring == 1 || (x == xCoord - ring || x == xCoord + ring || z == zCoord - ring || z == zCoord + ring)) {

                    BlockPos p = new BlockPos(x, y, z);
                    BlockState b = level.getBlockState(p);

                    if (!this.shouldIgnoreBlock(b, p) && this.isOre(b)) {
                        tryMineAtLocation(p);
                    }
                }
            }
        }
    }

    protected void tryEjectBuffer() {

        ForgeDirection dir = MachineCore.facing(getBlockState());
        BlockPos out = outputPos(dir);

        List<ItemStack> items = new ArrayList<>();

        for (int i = 5; i < 14; i++) {
            ItemStack stack = inventory.getStackInSlot(i);

            if (!stack.isEmpty()) {
                items.add(stack.copy());
            }
        }

        Container tile = ConveyorInventories.containerAt(level, out);
        if (tile != null) {
            supplyContainer(tile, items, dir.getOpposite());
        }

        Block b = level.getBlockState(out).getBlock();
        if (b instanceof IConveyorBelt belt) {
            supplyConveyor(belt, items, out);
        }

        items.removeIf(i -> i == null || i.getCount() <= 0);

        for (int i = 5; i < 14; i++) {
            int index = i - 5;

            if (items.size() > index) {
                inventory.setStackInSlot(i, items.get(index).copy());
            } else {
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /** pulls up an AABB around the drillbit and tries to either conveyor output or buffer collected items */
    protected void tryCollect(int radius) {
        int yLevel = getY();
        int xCoord = worldPosition.getX(), zCoord = worldPosition.getZ();

        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(xCoord - radius, yLevel - 1, zCoord - radius, xCoord + radius + 1, yLevel + 2, zCoord + radius + 1));

        ForgeDirection dir = MachineCore.facing(getBlockState());
        BlockPos out = outputPos(dir);

        List<ItemStack> stacks = new ArrayList<>();
        items.forEach(i -> { if (i.isAlive()) stacks.add(i.getItem()); });

        /* try to insert into a valid container */
        Container tile = ConveyorInventories.containerAt(level, out);
        if (tile != null) {
            supplyContainer(tile, stacks, dir.getOpposite());
        }

        /* try to place on conveyor belt */
        Block b = level.getBlockState(out).getBlock();
        if (b instanceof IConveyorBelt belt) {
            supplyConveyor(belt, stacks, out);
        }

        items.removeIf(i -> !i.isAlive() || i.getItem().getCount() <= 0);

        /* collect remaining items in internal buffer */
        outer:
        for (ItemEntity item : items) {
            if (!item.isAlive()) continue;

            ItemStack stack = item.getItem();

            /* adding items to existing stacks */
            for (int i = 5; i < 14; i++) {
                ItemStack slot = inventory.getStackInSlot(i);

                if (!slot.isEmpty() && slot.getCount() < slot.getMaxStackSize() && ItemStacks.sameItemAndData(stack, slot)) {
                    int toAdd = Math.min(slot.getMaxStackSize() - slot.getCount(), stack.getCount());
                    slot.grow(toAdd);
                    stack.shrink(toAdd);
                    inventory.setStackInSlot(i, slot);

                    chuteTimer = 40;

                    if (stack.getCount() <= 0) {
                        item.discard();
                        item.setPickUpDelay(60); // seems stupid, but prevents frame-perfect dupe exploit
                        continue outer;
                    }
                }
            }

            /* add leftovers to empty slots */
            for (int i = 5; i < 14; i++) {

                if (inventory.getStackInSlot(i).isEmpty()) {

                    chuteTimer = 40;

                    inventory.setStackInSlot(i, stack.copy());
                    item.discard();
                    item.setPickUpDelay(60);
                    break;
                }
            }
        }
    }

    /** places all items into a connected container, if possible */
    protected void supplyContainer(Container inv, List<ItemStack> items, ForgeDirection dir) {

        Direction side = dir.toDirection();
        int[] access = ConveyorInventories.masquerade(inv, side);

        for (ItemStack item : items) {

            if (item.getCount() <= 0) continue;

            ItemStack remainder = ConveyorInventories.addToInventory(inv, access, item, side);
            item.setCount(remainder.getCount());
            chuteTimer = 40;
        }
    }

    /** moves all items onto a connected conveyor belt */
    protected void supplyConveyor(IConveyorBelt belt, List<ItemStack> items, BlockPos out) {

        var rand = level.getRandom();

        for (ItemStack item : items) {

            if (item.getCount() <= 0) continue;

            Vec3 base = new Vec3(out.getX() + rand.nextDouble(), out.getY() + 0.5, out.getZ() + rand.nextDouble());
            Vec3 vec = belt.getClosestSnappingPosition(level, out, base);

            EntityMovingItem moving = new EntityMovingItem(level);
            moving.setPos(base.x, vec.y, base.z);
            moving.setItemStack(item.copy());
            level.addFreshEntity(moving);
            item.setCount(0);

            chuteTimer = 40;
        }
    }

    public long getPowerConsumption() {
        return consumption;
    }

    public int getFortuneLevel() {
        var type = getInstalledDrill();

        if (type != null) return type.fortune();
        return 0;
    }

    public boolean shouldIgnoreBlock(BlockState block, BlockPos pos) {
        return block.isAir() || block.getBlock() instanceof BlockGasBase || block.getDestroySpeed(level, pos) < 0
                || block.liquid() || block.is(Blocks.BEDROCK);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("drill")) this.enableDrill = !this.enableDrill;
        if (data.contains("crusher")) this.enableCrusher = !this.enableCrusher;
        if (data.contains("walling")) this.enableWalling = !this.enableWalling;
        if (data.contains("veinminer")) this.enableVeinMiner = !this.enableVeinMiner;
        if (data.contains("silktouch")) this.enableSilkTouch = !this.enableSilkTouch;

        this.markChanged();
    }

    public MachinePartDataMaps.@Nullable DrillBit getInstalledDrill() {
        ItemStack stack = inventory.getStackInSlot(4);
        if (stack.getItem() instanceof ItemDrillbit drill) {
            return drill.stats();
        }

        return null;
    }

    public boolean canVeinMine() {
        var type = getInstalledDrill();
        return this.enableVeinMiner && type != null && type.vein();
    }

    public boolean canSilkTouch() {
        var type = getInstalledDrill();
        return this.enableSilkTouch && type != null && type.silk();
    }

    @Override
    protected void loadData(DataIn nbt) {
        super.loadData(nbt);

        this.enableDrill = nbt.getBooleanOr("d", false);
        this.enableCrusher = nbt.getBooleanOr("c", false);
        this.enableWalling = nbt.getBooleanOr("w", false);
        this.enableVeinMiner = nbt.getBooleanOr("v", false);
        this.enableSilkTouch = nbt.getBooleanOr("s", false);
        this.targetDepth = nbt.getIntOr("t", 0);
        this.power = nbt.getLongOr("p", 0L);
        this.tank.readFromNBT(nbt, "tank");
    }

    @Override
    protected void saveData(DataOut nbt) {
        super.saveData(nbt);

        nbt.putBoolean("d", enableDrill);
        nbt.putBoolean("c", enableCrusher);
        nbt.putBoolean("w", enableWalling);
        nbt.putBoolean("v", enableVeinMiner);
        nbt.putBoolean("s", enableSilkTouch);
        nbt.putInt("t", targetDepth);
        nbt.putLong("p", power);
        tank.writeToNBT(nbt, "tank");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 1) return stack.getItem() instanceof IItemFluidId;
        if (slot == 2 || slot == 3) return Library.isMachineUpgrade(stack);
        if (slot == 4) return stack.getItem() instanceof ItemDrillbit;
        return true;
    }

    @Override protected int[] getUpgradeSlotRange() { return new int[]{2, 3}; }

    @Override public boolean hasPermission(Player player) { return stillValid(player); }

    @Override public Component getDisplayName() { return Component.translatable("container.machineExcavator"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineExcavator(containerId, playerInventory, this);
    }

    @Override public long getPower() { return this.power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[] {tank}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[] {tank}; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModProduction2Blocks.MACHINE_EXCAVATOR.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (100 - 200 / (level + 2)) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.EFFECT, 3);
        return upgrades;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "power", PREFIX_VALUE + "fluid", PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "power").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fluid").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "state").equals(name)) return operational ? "1" : "0";
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length == 1) {
            enableDrill = params[0].equals("1");
            markChanged();
        }
        return null;
    }
}
