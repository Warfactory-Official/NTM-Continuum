package com.hbm.gametest;

import com.hbm.blocks.ModConveyorBlocks;
import com.hbm.blocks.ModDoorBlocks;
import com.hbm.blocks.ModPneumaticBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.network.PneumoTube;
import com.hbm.tileentity.machine.TileEntityCargoElevator;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageExporter;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageImporter;
import com.hbm.tileentity.network.pneumatic.TileEntityPneumoStorageMono;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockCoreIndex;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModFluidNetItems;
import com.hbm.items.ModSecurityItems;
import com.hbm.items.tool.ItemKeyPin;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.tileentity.machine.storage.TileEntityMassStorage;
import net.minecraft.server.level.ServerPlayer;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.blocks.network.BlockConveyorBase;
import com.hbm.blocks.network.BlockCraneBase;
import com.hbm.blocks.network.CraneRouter;
import com.hbm.entity.item.EntityMovingItem;
import com.hbm.entity.item.EntityMovingPackage;
import com.hbm.tileentity.network.TileEntityCraneGrabber;
import com.hbm.tileentity.network.TileEntityCraneRouter;
import com.hbm.tileentity.network.TileEntityCraneSplitter;
import com.hbm.items.ModItems;
import com.hbm.tileentity.machine.storage.TileEntityCrate;
import com.hbm.tileentity.machine.storage.TileEntityCrateBase;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.ItemStacks;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import dev.golem.core.registry.BlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Item logistics audit: belts, the crane chain, and the lockable iron, steel and desh crates. */
public final class RcLogiGameTests {

    private RcLogiGameTests() {}

    private static BlockState crane(Block block, Direction input, Direction output) {
        return block.defaultBlockState().setValue(BlockCraneBase.INPUT, input).setValue(BlockCraneBase.OUTPUT, output);
    }

    private static BlockState belt(Block block, Direction travel) {
        return block.defaultBlockState().setValue(BlockConveyorBase.FACING, travel);
    }

    private static int count(Container container, ItemStack like) {
        int total = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (ItemStacks.sameItemAndData(container.getItem(i), like)) total += container.getItem(i).getCount();
        }
        return total;
    }

    /** The crane boxer survives a bare placement (the sweep saw it turn to air). */
    public static void boxerSurvives(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(pos, ModConveyorBlocks.CRANE_BOXER.get().defaultBlockState(), 3);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(ModConveyorBlocks.CRANE_BOXER.get()),
                    "crane boxer vanished: " + helper.getLevel().getBlockState(pos));
            helper.succeed();
        });
    }

    /** Regular belts move 0.0625 per tick, express three times that. */
    public static void beltSpeeds(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos start = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos startExpress = helper.absolutePos(new BlockPos(1, 1, 5));
        for (int i = 0; i < 12; i++) {
            level.setBlock(start.east(i), belt(ModConveyorBlocks.CONVEYOR.get(), Direction.EAST), 3);
            level.setBlock(startExpress.east(i), belt(ModConveyorBlocks.CONVEYOR_EXPRESS.get(), Direction.EAST), 3);
        }
        EntityMovingItem regular = new EntityMovingItem(level);
        regular.setItemStack(new ItemStack(Items.COBBLESTONE));
        Vec3 snap = ModConveyorBlocks.CONVEYOR.get().getClosestSnappingPosition(level, start, Vec3.atCenterOf(start));
        regular.snapTo(snap.x, snap.y, snap.z, 0F, 0F);
        level.addFreshEntity(regular);
        EntityMovingItem express = new EntityMovingItem(level);
        express.setItemStack(new ItemStack(Items.COBBLESTONE));
        Vec3 snapE = ModConveyorBlocks.CONVEYOR_EXPRESS.get().getClosestSnappingPosition(level, startExpress, Vec3.atCenterOf(startExpress));
        express.snapTo(snapE.x, snapE.y, snapE.z, 0F, 0F);
        level.addFreshEntity(express);

        double[] x0 = new double[2];
        helper.runAfterDelay(10, () -> {
            x0[0] = regular.getX();
            x0[1] = express.getX();
            helper.runAfterDelay(40, () -> {
                double dr = regular.getX() - x0[0];
                double de = express.getX() - x0[1];
                helper.assertTrue(Math.abs(dr - 40 * 0.0625D) < 0.02D, "regular belt moved " + dr + " per 40 ticks, expected " + 40 * 0.0625D);
                helper.assertTrue(Math.abs(de - 40 * 0.1875D) < 0.02D, "express belt moved " + de + " per 40 ticks, expected " + 40 * 0.1875D);
                helper.succeed();
            });
        });
    }

    /** Chest, ejector, four belts, inserter, chest: one item per 20 ticks, nothing lost, nothing duplicated. */
    public static void craneChain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos chestA = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos extractor = chestA.east();
        BlockPos chestB = extractor.east(6);
        BlockPos inserter = chestB.west();
        level.setBlock(chestA, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(extractor, crane(ModConveyorBlocks.CRANE_EXTRACTOR.get(), Direction.EAST, Direction.WEST), 3);
        for (int i = 1; i <= 4; i++) {
            level.setBlock(extractor.east(i), belt(ModConveyorBlocks.CONVEYOR.get(), Direction.EAST), 3);
        }
        level.setBlock(inserter, crane(ModConveyorBlocks.CRANE_INSERTER.get(), Direction.WEST, Direction.EAST), 3);
        level.setBlock(chestB, Blocks.CHEST.defaultBlockState(), 3);
        Container a = (Container) level.getBlockEntity(chestA);
        Container b = (Container) level.getBlockEntity(chestB);
        ItemStack probe = new ItemStack(Items.COBBLESTONE);
        a.setItem(0, new ItemStack(Items.COBBLESTONE, 64));

        helper.runAfterDelay(300, () -> {
            int inA = count(a, probe);
            int inB = count(b, probe);
            int riding = level.getEntitiesOfClass(EntityMovingItem.class, new AABB(chestA).inflate(12)).stream()
                    .mapToInt(e -> e.getItemStack().getCount()).sum();
            int loose = level.getEntitiesOfClass(ItemEntity.class, new AABB(chestA).inflate(12)).stream()
                    .mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(inA + inB + riding + loose == 64, "items lost: A " + inA + " B " + inB + " riding " + riding + " loose " + loose);
            int sent = 64 - inA;
            helper.assertTrue(sent >= 13 && sent <= 15, "ejector sent " + sent + " in 300 ticks, expected 15 (1 per 20 ticks)");
            helper.assertTrue(inB >= 10, "inserter delivered only " + inB);
            helper.succeed();
        });
    }

    /** Iron, steel and desh crates are pin-locked, keep their lock on the item, and report a comparator level. */
    public static void crateLockable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Block[] blocks = {ModStorageBlocks.crate_iron.get(), ModStorageBlocks.crate_steel.get(), ModStorageBlocks.crate_desh.get()};
        int[] sizes = {36, 54, 104};
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        for (int i = 0; i < blocks.length; i++) {
            Block block = blocks[i];
            BlockPos pos = helper.absolutePos(new BlockPos(1 + i * 3, 1, 2));
            level.setBlock(pos, block.defaultBlockState(), 3);
            helper.assertTrue(level.getBlockEntity(pos) instanceof TileEntityCrateBase, block + " is not lockable");
            TileEntityCrate crate = (TileEntityCrate) level.getBlockEntity(pos);
            helper.assertTrue(crate.getContainerSize() == sizes[i], block + " has " + crate.getContainerSize() + " slots");
            crate.setItem(0, new ItemStack(Items.DIAMOND, 3));
            crate.setPins(4242);
            crate.lock();
            helper.assertFalse(crate.canAccess(player), "locked " + block + " opened without a key");
            helper.assertFalse(crate.canTakeItemThroughFace(0, new ItemStack(Items.DIAMOND), Direction.UP), "hopper pulled from locked crate");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.key_red.get()));
            helper.assertTrue(crate.canAccess(player), "red key did not open " + block);
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);

            int analog = AbstractContainerMenu.getRedstoneSignalFromContainer(crate);
            helper.assertTrue(analog > 0 && level.getBlockState(pos).hasAnalogOutputSignal(), "comparator " + analog);

            level.destroyBlock(pos, true);
            List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            ItemStack carried = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(block.asItem())).findFirst().orElse(ItemStack.EMPTY);
            helper.assertFalse(carried.isEmpty(), "no crate item dropped for " + block);
            drops.forEach(ItemEntity::discard);

            BlockPos support = helper.absolutePos(new BlockPos(1 + i * 3, 1, 6));
            level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, carried);
            player.setShiftKeyDown(true);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
            carried.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, carried, hit));
            player.setShiftKeyDown(false);
            helper.assertTrue(level.getBlockEntity(support.above()) instanceof TileEntityCrate, block + " did not place");
            TileEntityCrate placed = (TileEntityCrate) level.getBlockEntity(support.above());
            helper.assertTrue(placed.isLocked() && placed.getPins() == 4242, block + " lost its lock through the item");
            helper.assertTrue(placed.getItem(0).is(Items.DIAMOND) && placed.getItem(0).getCount() == 3, block + " lost its contents");
        }
        helper.succeed();
    }

    /** A survival pickaxe break removes the crate before its drops, and the dropped crate still carries its contents. */
    public static void crateKeepsContentsOnPlayerBreak(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pos, ModStorageBlocks.crate_iron.get().defaultBlockState(), 3);
        TileEntityCrate crate = (TileEntityCrate) level.getBlockEntity(pos);
        crate.setItem(5, new ItemStack(Items.DIAMOND, 7));

        player.gameMode.destroyBlock(pos);
        ItemStack carried = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream().map(ItemEntity::getItem)
                .filter(s -> s.is(ModStorageBlocks.crate_iron.get().asItem())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertFalse(carried.isEmpty(), "no crate item dropped");
        ItemStack slot = ItemStacks.carriedItems(carried, "inventory", 36).get(5);
        helper.assertTrue(slot.is(Items.DIAMOND) && slot.getCount() == 7, "the crate item carries " + slot + " in slot 5, expected 7 diamonds");
        helper.succeed();
    }

    /** A padlock used on a generic door locks it instead of toggling it; only the matching key opens it. */
    public static void doorPadlockAndKey(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(6, 1, 6));
        MultiblockHandlerXR.placeAssembled(level, core, ModDoorBlocks.FIRE_DOOR.get(), Direction.NORTH);
        TileEntityDoorGeneric door = GameTestHelpers.blockEntity(helper, new BlockPos(6, 1, 6), TileEntityDoorGeneric.class);
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);

        ItemStack padlock = new ItemStack(ModSecurityItems.padlock.get());
        ItemKeyPin.setPins(padlock, 555);
        player.setItemInHand(InteractionHand.MAIN_HAND, padlock);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(core), Direction.UP, core, false);
        player.gameMode.useItemOn(player, level, padlock, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(door.isLocked() && door.getPins() == 555, "the padlock did not lock the door (locked " + door.isLocked() + ")");
        helper.assertTrue(door.state == TileEntityDoorGeneric.STATE_CLOSED, "using the padlock toggled the door");

        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        door.tryToggle(player);
        helper.assertTrue(door.state == TileEntityDoorGeneric.STATE_CLOSED, "a locked door opened without a key");

        ItemStack key = new ItemStack(ModItems.key.get());
        ItemKeyPin.setPins(key, 555);
        player.setItemInHand(InteractionHand.MAIN_HAND, key);
        door.tryToggle(player);
        helper.assertTrue(door.state == TileEntityDoorGeneric.STATE_OPENING, "the matching key did not open the door");
        helper.succeed();
    }

    /** A redstone block against a door cell opens the door; the 1.7 power counter is what drives it. */
    public static void doorRedstone(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(6, 1, 6));
        MultiblockHandlerXR.placeAssembled(level, core, ModDoorBlocks.FIRE_DOOR.get(), Direction.NORTH);
        TileEntityDoorGeneric door = GameTestHelpers.blockEntity(helper, new BlockPos(6, 1, 6), TileEntityDoorGeneric.class);
        level.setBlock(core.above(3), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(door.state == TileEntityDoorGeneric.STATE_OPENING || door.state == TileEntityDoorGeneric.STATE_OPEN,
                    "redstone did not open the door, state " + door.state + " power " + door.redstonePower);
            helper.succeed();
        });
    }

    /** Mass storage: an infinite barrel in the input slot fills the stockpile, and no filter empties it. */
    public static void massStorageInfinite(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlock(pos, ModStorageBlocks.mass_storage_iron.get().defaultBlockState(), 3);
        TileEntityMassStorage storage = GameTestHelpers.blockEntity(helper, new BlockPos(2, 1, 2), TileEntityMassStorage.class);
        storage.inventory.setStackInSlot(TileEntityMassStorage.FILTER_SLOT, new ItemStack(Items.COBBLESTONE));
        storage.inventory.setStackInSlot(TileEntityMassStorage.INPUT_SLOT, new ItemStack(ModFluidNetItems.fluid_barrel_infinite.get()));
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(storage.getStockpile() == storage.getCapacity() && storage.getCapacity() == 10_000,
                    "infinite barrel stockpile " + storage.getStockpile() + " of " + storage.getCapacity());
            storage.inventory.setStackInSlot(TileEntityMassStorage.INPUT_SLOT, ItemStack.EMPTY);
            storage.inventory.setStackInSlot(TileEntityMassStorage.FILTER_SLOT, ItemStack.EMPTY);
            helper.runAfterDelay(4, () -> {
                helper.assertTrue(storage.getStockpile() == 0, "stockpile kept " + storage.getStockpile() + " with no filter");
                helper.succeed();
            });
        });
    }

    private static boolean hasServerTickLogic(Class<?> type) {
        for (Class<?> c = type; c != null && c != BlockEntity.class && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                String name = m.getName();
                if (name.equals("serverTick") || name.equals("updateEntity")) return true;
                if (name.equals("tick") && m.getParameterCount() >= 4 && m.getParameterTypes()[0] == Level.class) return true;
            }
        }
        return false;
    }

    /** Every hbm block entity class with server tick logic is reachable from its block's ticker (a missing ticker is silent). */
    public static void everyTickingBlockEntityHasATicker(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        List<String> missing = new ArrayList<>();
        int checked = 0;
        int withTick = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals("hbm")) continue;
            if (!(block instanceof EntityBlock entityBlock)) continue;
            BlockState state = block.defaultBlockState();
            BlockEntity be;
            try {
                be = entityBlock.newBlockEntity(pos, state);
            } catch (RuntimeException e) {
                continue;
            }
            if (be == null) continue;
            checked++;
            if (!hasServerTickLogic(be.getClass())) continue;
            withTick++;
            if (entityBlock.getTicker(level, state, be.getType()) == null) {
                missing.add(BuiltInRegistries.BLOCK.getKey(block) + " (" + be.getClass().getSimpleName() + ")");
            }
        }
        System.out.println("[RcLogi] ticker check: " + checked + " block entities, " + withTick + " with server tick logic, missing " + missing);
        helper.assertTrue(withTick > 100, "ticker check looked at only " + withTick + " ticking classes");
        helper.assertTrue(missing.isEmpty(), "block entities with tick logic but no ticker: " + missing);
        helper.succeed();
    }

    /** The router sends a stack out of the side whose filter matches and the rest out of the wildcard side. */
    public static void routerFilters(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 1, 3));
        level.setBlock(pos, ModConveyorBlocks.CRANE_ROUTER.get().defaultBlockState(), 3);
        TileEntityCraneRouter router = GameTestHelpers.blockEntity(helper, new BlockPos(3, 1, 3), TileEntityCraneRouter.class);
        int east = Direction.EAST.get3DDataValue();
        int west = Direction.WEST.get3DDataValue();
        int south = Direction.SOUTH.get3DDataValue();
        router.modes[east] = TileEntityCraneRouter.MODE_WHITELIST;
        router.setItem(east * 5, new ItemStack(Items.COBBLESTONE));
        router.modes[south] = TileEntityCraneRouter.MODE_BLACKLIST;
        router.setItem(south * 5, new ItemStack(Items.COBBLESTONE));
        router.modes[west] = TileEntityCraneRouter.MODE_WILDCARD;
        List<List<ItemStack>> cobble = CraneRouter.sort(level, pos, List.of(new ItemStack(Items.COBBLESTONE, 5)));
        List<List<ItemStack>> dirt = CraneRouter.sort(level, pos, List.of(new ItemStack(Items.DIRT, 7)));
        helper.assertTrue(cobble.get(east).size() == 1 && cobble.get(east).get(0).getCount() == 5
                && cobble.get(south).isEmpty() && cobble.get(west).isEmpty(), "cobblestone must take the whitelist side only");
        helper.assertTrue(dirt.get(south).size() == 1 && dirt.get(east).isEmpty() && dirt.get(west).isEmpty(),
                "dirt must take the blacklist side, not the wildcard");
        router.modes[south] = TileEntityCraneRouter.MODE_NONE;
        List<List<ItemStack>> wild = CraneRouter.sort(level, pos, List.of(new ItemStack(Items.DIRT, 7)));
        helper.assertTrue(wild.get(west).size() == 1 && wild.get(west).get(0).getCount() == 7, "dirt must fall to the wildcard side");
        helper.succeed();
    }

    /** Splitter ratio 1:3 over eight items gives 2 left and 6 right, and the remainder carries to the next stack. */
    public static void splitterRatio(GameTestHelper helper) {
        TileEntityCraneSplitter splitter = new TileEntityCraneSplitter(helper.absolutePos(new BlockPos(1, 1, 1)),
                ModConveyorBlocks.CRANE_SPLITTER.get().defaultBlockState());
        splitter.leftRatio = 1;
        splitter.rightRatio = 3;
        List<ItemStack> first = splitter.splitStack(new ItemStack(Items.COBBLESTONE, 8));
        helper.assertTrue(first.get(0).getCount() == 2 && first.get(1).getCount() == 6, "1:3 over 8 gave " + first.get(0).getCount() + ":" + first.get(1).getCount());
        TileEntityCraneSplitter odd = new TileEntityCraneSplitter(helper.absolutePos(new BlockPos(2, 1, 1)),
                ModConveyorBlocks.CRANE_SPLITTER.get().defaultBlockState());
        odd.leftRatio = 2;
        odd.rightRatio = 1;
        int left = 0;
        int right = 0;
        for (int i = 0; i < 6; i++) {
            List<ItemStack> part = odd.splitStack(new ItemStack(Items.COBBLESTONE, 1));
            left += part.get(0).getCount();
            right += part.get(1).getCount();
        }
        helper.assertTrue(left == 4 && right == 2, "2:1 one item at a time gave " + left + ":" + right);
        helper.succeed();
    }

    /** Boxer, belt, unboxer, belt, inserter, chest: four full stacks (single swords) make one package and arrive whole. */
    public static void boxerUnboxerChain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos boxer = helper.absolutePos(new BlockPos(1, 1, 2));
        level.setBlock(boxer, crane(ModConveyorBlocks.CRANE_BOXER.get(), Direction.WEST, Direction.EAST), 3);
        for (int i = 1; i <= 3; i++) level.setBlock(boxer.east(i), belt(ModConveyorBlocks.CONVEYOR.get(), Direction.EAST), 3);
        BlockPos unboxer = boxer.east(4);
        level.setBlock(unboxer, crane(ModConveyorBlocks.CRANE_UNBOXER.get(), Direction.EAST, Direction.WEST), 3);
        for (int i = 1; i <= 3; i++) level.setBlock(unboxer.east(i), belt(ModConveyorBlocks.CONVEYOR.get(), Direction.EAST), 3);
        BlockPos inserter = unboxer.east(4);
        level.setBlock(inserter, crane(ModConveyorBlocks.CRANE_INSERTER.get(), Direction.WEST, Direction.EAST), 3);
        BlockPos chest = inserter.east();
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        Container boxerSlots = (Container) level.getBlockEntity(boxer);
        for (int i = 0; i < 4; i++) boxerSlots.setItem(i, new ItemStack(Items.WOODEN_SWORD));
        Container target = (Container) level.getBlockEntity(chest);
        ItemStack probe = new ItemStack(Items.WOODEN_SWORD);
        helper.runAfterDelay(500, () -> {
            int in = count(target, probe);
            int left = count(boxerSlots, probe);
            int riding = level.getEntitiesOfClass(EntityMovingItem.class, new AABB(boxer).inflate(14)).stream().mapToInt(e -> e.getItemStack().getCount()).sum();
            int packages = level.getEntitiesOfClass(EntityMovingPackage.class, new AABB(boxer).inflate(14)).size();
            helper.assertTrue(in == 4, "chest got " + in + " of 4 (boxer kept " + left + ", belts " + riding + ", packages " + packages + ")");
            helper.succeed();
        });
    }

    /** A grabber with a whitelist lifts only the filtered item off a passing belt into its chest. */
    public static void grabberWhitelist(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos source = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos extractor = source.east();
        level.setBlock(source, Blocks.CHEST.defaultBlockState(), 3);
        level.setBlock(extractor, crane(ModConveyorBlocks.CRANE_EXTRACTOR.get(), Direction.EAST, Direction.WEST), 3);
        for (int i = 1; i <= 8; i++) level.setBlock(extractor.east(i), belt(ModConveyorBlocks.CONVEYOR.get(), Direction.EAST), 3);
        BlockPos grabber = extractor.east(4).above(); // sits above the belt: input = belt below, output = chest above
        level.setBlock(grabber, crane(ModConveyorBlocks.CRANE_GRABBER.get(), Direction.DOWN, Direction.UP), 3);
        BlockPos chest = grabber.above();
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 3);
        TileEntityCraneGrabber tile = (TileEntityCraneGrabber) level.getBlockEntity(grabber);
        tile.isWhitelist = true;
        tile.setItem(0, new ItemStack(Items.DIRT));
        Container src = (Container) level.getBlockEntity(source);
        for (int i = 0; i < 8; i++) src.setItem(i, new ItemStack(i % 2 == 0 ? Items.DIRT : Items.COBBLESTONE, 4));
        Container target = (Container) level.getBlockEntity(chest);
        helper.runAfterDelay(400, () -> {
            helper.assertTrue(count(target, new ItemStack(Items.COBBLESTONE)) == 0, "the whitelist grabber took cobblestone");
            helper.assertTrue(count(target, new ItemStack(Items.DIRT)) > 0, "the whitelist grabber took nothing");
            helper.succeed();
        });
    }

    /** Mass storage takes a padlock, refuses automation while locked, and keeps lock and stockpile on its item. */
    public static void massStorageLockable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        Block block = ModStorageBlocks.mass_storage_iron.get();
        level.setBlock(pos, block.defaultBlockState(), 3);
        TileEntityMassStorage storage = GameTestHelpers.blockEntity(helper, new BlockPos(2, 1, 2), TileEntityMassStorage.class);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        storage.setPins(777);
        storage.lock();
        storage.setStockpile(1234);
        helper.assertFalse(storage.canAccess(player), "a locked mass storage opened without a key");
        helper.assertFalse(storage.canPlaceItemThroughFace(0, new ItemStack(Items.COBBLESTONE), Direction.UP), "automation inserted into a locked mass storage");
        helper.assertFalse(storage.canTakeItemThroughFace(2, new ItemStack(Items.COBBLESTONE), Direction.UP), "automation extracted from a locked mass storage");
        level.destroyBlock(pos, true);
        List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
        ItemStack carried = drops.stream().map(ItemEntity::getItem).filter(s -> s.is(block.asItem())).findFirst().orElse(ItemStack.EMPTY);
        helper.assertFalse(carried.isEmpty(), "no mass storage item dropped");
        BlockPos support = helper.absolutePos(new BlockPos(6, 1, 6));
        level.setBlock(support, Blocks.STONE.defaultBlockState(), 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, carried);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support), Direction.UP, support, false);
        carried.getItem().useOn(new UseOnContext(level, player, InteractionHand.MAIN_HAND, carried, hit));
        helper.assertTrue(level.getBlockEntity(support.above()) instanceof TileEntityMassStorage placed
                && placed.isLocked() && placed.getPins() == 777 && placed.getStockpile() == 1234,
                "the lock or the stockpile did not survive break and placement");
        helper.succeed();
    }

    /** A crate or mass storage saved before the crate base class took over still shows its anvil name. */
    public static void crateOldCustomName(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        int n = 0;
        for (Block block : List.of(ModStorageBlocks.crate_iron.get(), ModStorageBlocks.mass_storage_iron.get())) {
            BlockPos rel = new BlockPos(1 + n++ * 3, 1, 2);
            BlockState state = block.defaultBlockState();
            helper.setBlock(rel, state);
            BlockEntity be = level.getBlockEntity(helper.absolutePos(rel));
            CompoundTag saved = BlockEntities.save(be, level.registryAccess());
            saved.remove("name");
            saved.putString("customName", "Old Name");
            BlockEntity fresh = ((EntityBlock) block).newBlockEntity(helper.absolutePos(rel), state);
            fresh.setLevel(level);
            BlockEntities.loadWithComponents(fresh, saved, level.registryAccess());
            helper.assertTrue(fresh instanceof TileEntityCrateBase crate && "Old Name".equals(crate.customName),
                    BuiltInRegistries.BLOCK.getKey(block) + " lost its old customName key");
        }
        helper.succeed();
    }

    /** Importer, tubes, storage bank, tubes, exporter: items go in, are counted in the bank, and come out filtered. */
    public static void pneumaticImportExport(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos importer = helper.absolutePos(new BlockPos(1, 1, 2));
        BlockPos mono = importer.east(3);
        BlockPos exporter = importer.east(6);
        BlockState tube = ModPneumaticBlocks.PNEUMATIC_TUBE.get().defaultBlockState()
                .setValue(PneumoTube.WEST, true).setValue(PneumoTube.EAST, true);
        level.setBlock(importer, ModPneumaticBlocks.PNEUMATIC_STORAGE_IMPORTER.get().defaultBlockState(), 3);
        level.setBlock(mono, ModPneumaticBlocks.PNEUMATIC_STORAGE_MONO.get().defaultBlockState(), 3);
        level.setBlock(exporter, ModPneumaticBlocks.PNEUMATIC_STORAGE_EXPORTER.get().defaultBlockState(), 3);
        for (int x : new int[]{1, 2, 4, 5}) level.setBlock(importer.east(x), tube, 3);
        TileEntityPneumoStorageImporter in = (TileEntityPneumoStorageImporter) level.getBlockEntity(importer);
        TileEntityPneumoStorageMono bank = (TileEntityPneumoStorageMono) level.getBlockEntity(mono);
        TileEntityPneumoStorageExporter out = (TileEntityPneumoStorageExporter) level.getBlockEntity(exporter);
        bank.compair.setFill(4000);
        bank.inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE));
        in.inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 16));
        in.inventory.setStackInSlot(1, new ItemStack(Items.DIRT, 4));
        out.inventory.setStackInSlot(0, new ItemStack(Items.COBBLESTONE, 8));
        out.continuousRequest = true;
        helper.runAfterDelay(200, () -> {
            helper.assertTrue(in.inventory.getStackInSlot(0).isEmpty(), "the importer kept " + in.inventory.getStackInSlot(0));
            helper.assertTrue(in.inventory.getStackInSlot(1).getCount() == 4, "dirt has no cell and must stay in the importer");
            ItemStack got = out.inventory.getStackInSlot(9);
            helper.assertTrue(got.is(Items.COBBLESTONE) && got.getCount() == 16, "the exporter holds " + got + " (as much as possible = 16)");
            helper.assertTrue(bank.amounts[0] == 0, "the bank kept " + bank.amounts[0]);
            helper.succeed();
        });
    }

    /** A full round trip holds its rider at both endpoints; every section resolves and drops the whole lift. */
    public static void cargoElevatorCarries(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(4, 1, 4));
        // cores stacked on each other splice into one lift with the height of the stack
        for (int i = 0; i < 5; i++) {
            MultiblockHandlerXR.placeAssembled(level, core.above(i), ModMachineBlocks.cargo_elevator.get(), Direction.NORTH);
        }
        TileEntityCargoElevator lift = GameTestHelpers.blockEntity(helper, new BlockPos(4, 1, 4), TileEntityCargoElevator.class);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(lift.height == 4, "stacked lifts joined to height " + lift.height);
            var client = cargoClientView(level);
            for (int dy = 0; dy < 5; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        var cell = core.offset(dx, dy, dz);
                        helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(client, cell)),
                                "client lost the core of section " + cell);
                        if (!cell.equals(core)) helper.assertTrue(level.getBlockEntity(cell) == null,
                                "a spliced section kept its block entity at " + cell);
                    }
                }
            }
            helper.assertTrue(MultiblockSurface.rigidStructureBlocks(level, core).size() == 45,
                    "the live footprint must contain all five 3x3 courses");
            helper.assertTrue(lift.getBlockState().getCollisionShape(client, core).bounds().maxY == 5D,
                    "collision must read the live five-course geometry, not a cached cube");
            var eye = new Vec3(core.getX() + 0.5D, core.getY() + 2.62D, core.getZ() + 0.5D);
            var aim = new Vec3(eye.x, core.getY() + 0.99D, eye.z);
            var queried = core.above(2);
            var picked = level.getBlockState(queried).getShape(client, queried).clip(eye, aim, queried);
            helper.assertTrue(picked != null && picked.getBlockPos().equals(core.above()),
                    "looking down inside the shaft must report the platform's surface cell: " + picked);
            helper.assertTrue(picked.getLocation().distanceToSqr(Vec3.atCenterOf(picked.getBlockPos())) < 1D,
                    "the platform hit would be rejected by the server's cell-distance check");
            Mob pig = helper.spawn(EntityType.PIG, new BlockPos(4, 2, 4));
            pig.setNoAi(true);
            double y0 = pig.getY();
            lift.toggleElevator();
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(lift.extension == 4D, "platform reached " + lift.extension);
                helper.assertTrue(Math.abs(pig.getY() - y0 - 4D) < 0.01D,
                        "the mob did not stay on the fully extended platform: " + (pig.getY() - y0));
                helper.assertTrue(lift.getBlockState().getCollisionShape(client, core,
                                CollisionContext.empty()).bounds().maxY == 5D,
                        "the moving geometry lost the top of the shaft");
                lift.toggleElevator();
                helper.runAfterDelay(60, () -> {
                    helper.assertTrue(lift.extension == 0D, "platform did not retract: " + lift.extension);
                    helper.assertTrue(Math.abs(pig.getY() - y0) < 0.01D,
                            "the mob did not ride down and stay on the platform: " + (pig.getY() - y0));
                    helper.assertTrue(lift.height == 4, "spliced sections kept growing the shaft");
                    assertCargoDrops(helper, core, core.offset(1, 3, 0), 5,
                            GameTestHelpers.mockServerPlayerInLevel(helper));
                    helper.succeed();
                });
            });
        });
    }

    /** Adding one course is atomic when blocked or above the build limit, and a core break returns every course. */
    public static void cargoElevatorGrowth(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = ModMachineBlocks.cargo_elevator.get();
        var core = helper.absolutePos(new BlockPos(4, 1, 4));
        var ceiling = core.atY(level.getMinY() + level.getHeight() - 1);
        MultiblockHandlerXR.placeAssembled(level, core, block, Direction.NORTH);
        MultiblockHandlerXR.placeAssembled(level, ceiling, block, Direction.NORTH);
        var player = GameTestHelpers.mockServerPlayerInLevel(helper);
        var held = new ItemStack(block, 3);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        var topLift = (TileEntityCargoElevator) level.getBlockEntity(ceiling);
        player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(ceiling), Direction.UP, ceiling, false));
        helper.assertTrue(topLift.height == 0 && held.getCount() == 3,
                "adding above the build limit consumed an elevator or changed the shaft height");
        level.removeBlock(ceiling, false);

        var lift = (TileEntityCargoElevator) level.getBlockEntity(core);
        var obstacle = core.offset(1, 1, 0);
        level.setBlock(obstacle, Blocks.STONE.defaultBlockState(), 3);
        var hit = new BlockHitResult(Vec3.atCenterOf(core), Direction.UP, core, false);
        player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(lift.height == 0 && held.getCount() == 3, "a blocked course consumed an elevator");
        helper.assertTrue(level.getBlockState(core.above()).isAir(), "a blocked course was partly placed");
        level.removeBlock(obstacle, false);
        player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(lift.height == 1 && held.getCount() == 2, "adding a clear course must consume one item");
        var client = cargoClientView(level);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                var cell = core.offset(dx, 1, dz);
                helper.assertTrue(core.equals(MultiblockSurface.coreOfAny(level, cell))
                                && core.equals(MultiblockSurface.coreOfAny(client, cell)),
                        "the added course has no owner at " + cell);
            }
        }
        assertCargoDrops(helper, core, core, 2, player);
        helper.succeed();
    }

    private static BlockGetter cargoClientView(ServerLevel level) {
        return new BlockGetter() {
            @Override public BlockEntity getBlockEntity(BlockPos pos) { return level.getBlockEntity(pos); }
            @Override public BlockState getBlockState(BlockPos pos) { return level.getBlockState(pos); }
            @Override public FluidState getFluidState(BlockPos pos) { return level.getFluidState(pos); }
            @Override public int getHeight() { return level.getHeight(); }
            @Override public int getMinY() { return level.getMinY(); }
        };
    }

    private static void assertCargoDrops(GameTestHelper helper, BlockPos core, BlockPos broken, int courses,
                                         ServerPlayer player) {
        var level = helper.getLevel();
        var block = ModMachineBlocks.cargo_elevator.get();
        var footprint = MultiblockSurface.rigidStructureBlocks(level, core);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
        helper.assertTrue(player.gameMode.destroyBlock(broken), "survival could not break the elevator");
        int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(core).inflate(4)).stream()
                .map(ItemEntity::getItem).filter(stack -> stack.is(block.asItem())).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(dropped == courses, "breaking " + broken + " returned " + dropped + " of " + courses + " courses");
        for (var cell : footprint) {
            helper.assertFalse(level.getBlockState(cell).is(block), "teardown left elevator section " + cell);
            helper.assertTrue(MultiblockCoreIndex.lookup(level, cell) == Long.MIN_VALUE,
                    "teardown left ownership at " + cell);
        }
    }

    /** The key kit on a locked crate, mass storage and door makes a counterfeit key that opens that lock. */
    public static void keyKitCopiesLocks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
        BlockPos crate = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos mass = helper.absolutePos(new BlockPos(3, 1, 1));
        BlockPos doorCore = helper.absolutePos(new BlockPos(8, 1, 6));
        level.setBlock(crate, ModStorageBlocks.crate_steel.get().defaultBlockState(), 3);
        level.setBlock(mass, ModStorageBlocks.mass_storage_desh.get().defaultBlockState(), 3);
        MultiblockHandlerXR.placeAssembled(level, doorCore, ModDoorBlocks.FIRE_DOOR.get(), Direction.NORTH);
        TileEntityLockableBase[] locks = {
                (TileEntityLockableBase) level.getBlockEntity(crate), (TileEntityLockableBase) level.getBlockEntity(mass),
                (TileEntityLockableBase) level.getBlockEntity(doorCore)};
        BlockPos[] at = {crate, mass, doorCore};
        for (int i = 0; i < locks.length; i++) {
            locks[i].setPins(300 + i);
            locks[i].lock();
            player.getInventory().clearContent();
            ItemStack kit = new ItemStack(ModSecurityItems.key_kit.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, kit);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at[i]), Direction.UP, at[i], false);
            player.gameMode.useItemOn(player, level, kit, InteractionHand.MAIN_HAND, hit);
            ItemStack made = player.getMainHandItem();
            helper.assertTrue(made.is(ModItems.key_fake.get()) && ItemKeyPin.pins(made) == 300 + i,
                    "the key kit gave " + made + " for lock " + i);
            helper.assertTrue(locks[i].canAccess(player), "the counterfeit key did not open lock " + i);
        }
        helper.succeed();
    }
}
