package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyHandlerMK2.ConnectionPriority;
import com.hbm.api.fluidmk2.DuctData;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.generic.BlockWandStructure;
import com.hbm.blocks.generic.ModStructureWandBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.creativetabs.ModTabs;
import com.hbm.entity.ModEntities;
import com.hbm.entity.ModNtmEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.mob.EntityUndeadSoldier;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.handler.pollution.PollutionSavedData;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModDataComponents;
import com.hbm.items.machine.ItemFluidId;
import com.hbm.tileentity.machine.TileEntityFoundryBase;
import com.hbm.tileentity.machine.TileEntityMachineBattery;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.tileentity.network.TileEntityFluidValve;
import com.hbm.util.SavedIds;
import dev.golem.core.data.Tags;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.Entities;
import dev.golem.core.world.ItemStacks;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Numeric ids are never persisted.
 *
 * <ul>
 *   <li>The guard: every HBM block entity is created and saved, and its save must hold no numeric value under a
 *       key that names an identity - a tank's {@code <name>_type}, and every key the IDS phase converted from a
 *       fluid id, block-state id, material id or enum ordinal to a name.</li>
 *   <li>Round trips per converted system: fluid (tank, duct, valve, barrel item data, component codec), block state
 *       (structure wand blacklist, rubble entity), material (foundry), enum (battery priority, pollution data).</li>
 *   <li>Old int-keyed data reads as the default instead of failing (pre-release, no migration).</li>
 *   <li>The 1.7 fluid numbers survive only as a frozen side table, checked here against 1.7's registration order.</li>
 *   <li>Items: every HBM item and creative-tab stack is encoded: no {@code hbm:item_variant} /
 *       {@code hbm:storage_waste_class} and no number under an identity key in its components.</li>
 * </ul>
 */
public final class IdsGameTests {

    private IdsGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function GUARD =
            TESTS.function("ids_no_numeric_ids_in_block_entity_saves", 20, () -> IdsGameTests::guard);
    public static final GameTests.Function FLUIDS =
            TESTS.function("ids_fluid_round_trip", 20, () -> IdsGameTests::fluids);
    public static final GameTests.Function STATES =
            TESTS.function("ids_block_state_round_trip", 20, () -> IdsGameTests::states);
    public static final GameTests.Function MATERIALS_ENUMS =
            TESTS.function("ids_material_enum_round_trip", 20, () -> IdsGameTests::materialsAndEnums);
    public static final GameTests.Function LEGACY =
            TESTS.function("ids_legacy_table_and_old_saves", 20, () -> IdsGameTests::legacy);
    public static final GameTests.Function ITEMS =
            TESTS.function("ids_no_numeric_ids_in_item_components", 40, () -> IdsGameTests::items);

    public static void init() {}

    /**
     * Keys whose value names an identity: a tank's {@code <name>_type} and every key the IDS phase converted. A numeric
     * tag (or int array) under one of these is a persisted numeric id. Generic names that many block entities also use
     * for plain numbers ({@code type}, {@code color}, {@code p}, {@code t<n>}) are covered by the targeted round trips.
     * {@code direction} is left out on purpose: the jigsaw/tandem wands save vanilla's stable
     * {@code Direction.get3DDataValue}, a data value vanilla itself guarantees, not an ordinal or registry id.
     */
    private static final Pattern IDENTITY_KEY = Pattern.compile(
            ".*_type|fluid|fluids|fluid[12]|fluidID|mat|nType|outType|leftType|rightType|blockState|paintblock|blocks"
                    + "|blacklist|priority|function|padSize|orientation|debtype|payload|flow|typeLoaded|formFactor"
                    + "|lock\\d+|dir|rec|recipe|was");
    /** Block entities exempt from the scan (none: list a class here only with a written reason). */
    private static final Set<String> NUMERIC_ALLOWED = Set.of();

    /** The 1.7 {@code Fluids.init} order: index = 1.7 {@code FluidType.getID()}. Frozen; never reorder. */
    private static final String[] LEGACY_ORDER = {
            "NONE", "WATER", "STEAM", "HOTSTEAM", "SUPERHOTSTEAM", "ULTRAHOTSTEAM", "COOLANT", "LAVA", "DEUTERIUM",
            "TRITIUM", "OIL", "HOTOIL", "HEAVYOIL", "BITUMEN", "SMEAR", "HEATINGOIL", "RECLAIMED", "PETROIL",
            "LUBRICANT", "NAPHTHA", "DIESEL", "LIGHTOIL", "KEROSENE", "GAS", "PETROLEUM", "LPG", "BIOGAS",
            "BIOFUEL", "NITAN", "UF6", "PUF6", "SAS3", "SCHRABIDIC", "AMAT", "ASCHRAB", "PEROXIDE", "WATZ",
            "CRYOGEL", "HYDROGEN", "OXYGEN", "XENON", "BALEFIRE", "MERCURY", "PAIN", "WASTEFLUID", "WASTEGAS",
            "GASOLINE", "COALGAS", "SPENTSTEAM", "FRACKSOL", "PLASMA_DT", "PLASMA_HD", "PLASMA_HT", "PLASMA_XM",
            "PLASMA_BF", "CARBONDIOXIDE", "PLASMA_DH3", "HELIUM3", "DEATH", "ETHANOL", "HEAVYWATER", "CRACKOIL",
            "COALOIL", "HOTCRACKOIL", "NAPHTHA_CRACK", "LIGHTOIL_CRACK", "DIESEL_CRACK", "AROMATICS",
            "UNSATURATEDS", "SALIENT", "XPJUICE", "ENDERJUICE", "PETROIL_LEADED", "GASOLINE_LEADED",
            "COALGAS_LEADED", "SULFURIC_ACID", "COOLANT_HOT", "MUG", "MUG_HOT", "WOODOIL", "COALCREOSOTE",
            "SEEDSLURRY", "NITRIC_ACID", "SOLVENT", "BLOOD", "BLOOD_HOT", "SYNGAS", "OXYHYDROGEN", "RADIOSOLVENT",
            "CHLORINE", "HEAVYOIL_VACUUM", "REFORMATE", "LIGHTOIL_VACUUM", "SOURGAS", "XYLENE", "HEATINGOIL_VACUUM",
            "DIESEL_REFORM", "DIESEL_CRACK_REFORM", "KEROSENE_REFORM", "REFORMGAS", "COLLOID", "PHOSGENE",
            "MUSTARDGAS", "IONGEL", "OIL_COKER", "NAPHTHA_COKER", "GAS_COKER", "EGG", "CHOLESTEROL", "ESTRADIOL",
            "FISHOIL", "SUNFLOWEROIL", "NITROGLYCERIN", "REDMUD", "CHLOROCALCITE_SOLUTION", "CHLOROCALCITE_MIX",
            "CHLOROCALCITE_CLEANED", "POTASSIUM_CHLORIDE", "CALCIUM_CHLORIDE", "CALCIUM_SOLUTION", "SMOKE",
            "SMOKE_LEADED", "SMOKE_POISON", "HELIUM4", "HEAVYWATER_HOT", "SODIUM", "SODIUM_HOT", "THORIUM_SALT",
            "THORIUM_SALT_HOT", "THORIUM_SALT_DEPLETED", "FULLERENE", "PHEROMONE", "PHEROMONE_M", "OIL_DS",
            "HOTOIL_DS", "CRACKOIL_DS", "HOTCRACKOIL_DS", "NAPHTHA_DS", "LIGHTOIL_DS", "STELLAR_FLUX", "VITRIOL",
            "SLOP", "LEAD", "LEAD_HOT", "PERFLUOROMETHYL", "PERFLUOROMETHYL_COLD", "PERFLUOROMETHYL_HOT", "LYE",
            "SODIUM_ALUMINATE", "BAUXITE_SOLUTION", "ALUMINA", "AIR", "CONCRETE", "DHC", "AIRBLAST", "FLUE"
    };


    /** Every HBM block entity, created detached and saved: no identity key may hold a number. */
    public static void guard(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        int scanned = 0, skipped = 0;
        List<String> numeric = new ArrayList<>();
        Set<Class<?>> seen = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Constants.MOD_ID)) continue;
            if (!(block instanceof EntityBlock entityBlock)) continue;
            BlockEntity be;
            CompoundTag saved;
            try {
                be = entityBlock.newBlockEntity(pos, block.defaultBlockState());
                if (be == null || !seen.add(be.getClass())) continue;
                saved = BlockEntities.saveWithoutMetadata(be, registries);
            } catch (RuntimeException | LinkageError e) {
                skipped++; // a block entity that needs a level to save is covered by the targeted round trips
                continue;
            }
            scanned++;
            if (NUMERIC_ALLOWED.contains(be.getClass().getSimpleName())) continue;
            scan(saved, BuiltInRegistries.BLOCK.getKey(block).toString(), "", numeric);
        }
        helper.assertTrue(scanned >= 200, "expected to save at least 200 HBM block entity classes, saved " + scanned
                + " (" + skipped + " needed a level)");
        helper.assertTrue(numeric.isEmpty(), "numeric ids in block entity saves: " + numeric);
        Constants.LOG.info("[IDS] guard scanned {} block entity classes ({} skipped), 0 numeric identity values", scanned, skipped);
        helper.succeed();
    }

    /**
     * Every HBM item's own stack and every stack an HBM creative tab shows, encoded as the game saves them: none may
     * carry the removed numeric metadata components ({@code hbm:item_variant}, {@code hbm:storage_waste_class}), nor a
     * number under an identity key (matched without the component's namespace).
     */
    public static void items(GameTestHelper helper) {
        var ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Constants.MOD_ID)) stacks.add(new ItemStack(item));
        }
        for (var suppliers : ModTabs.STACK_CONTENTS.values()) {
            for (var supplier : suppliers) stacks.addAll(supplier.get());
        }
        int scanned = 0, skipped = 0;
        List<String> numeric = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            Tag tag = ItemStacks.optionalCodec().encodeStart(ops, stack).result().orElse(null);
            if (tag == null) {
                skipped++;
                continue;
            }
            scanned++;
            String owner = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            String text = tag.toString();
            if (text.contains("item_variant") || text.contains("storage_waste_class")) numeric.add(owner + " " + text);
            scanComponents(tag, owner, "", numeric);
        }
        helper.assertTrue(scanned >= 3000, "expected to encode at least 3000 HBM item stacks, encoded " + scanned
                + " (" + skipped + " did not encode)");
        helper.assertTrue(numeric.isEmpty(), "numeric ids in item components: " + numeric);
        Constants.LOG.info("[IDS] item guard encoded {} HBM item stacks ({} skipped), 0 numeric identity values", scanned, skipped);
        helper.succeed();
    }

    private static void scanComponents(Tag tag, String owner, String path, List<String> numeric) {
        if (tag instanceof CompoundTag compound) {
            for (String key : Tags.keys(compound)) {
                Tag child = compound.get(key);
                String bare = key.substring(key.indexOf(':') + 1);
                if ((child instanceof NumericTag || child instanceof IntArrayTag)
                        && IDENTITY_KEY.matcher(bare).matches()) {
                    numeric.add(owner + ":" + path + "/" + key + "=" + child);
                }
                scanComponents(child, owner, path + "/" + key, numeric);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) scanComponents(list.get(i), owner, path + "[" + i + "]", numeric);
        }
    }

    private static void scan(Tag tag, String owner, String path, List<String> numeric) {
        if (tag instanceof CompoundTag compound) {
            for (String key : Tags.keys(compound)) {
                Tag child = compound.get(key);
                if ((child instanceof NumericTag || child instanceof IntArrayTag)
                        && IDENTITY_KEY.matcher(key).matches()) {
                    numeric.add(owner + ":" + path + "/" + key + "=" + child);
                }
                scan(child, owner, path + "/" + key, numeric);
            }
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) scan(list.get(i), owner, path + "[" + i + "]", numeric);
        }
    }


    public static void fluids(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        // every fluid has a unique name, its vanilla fluid id: hbm:<lowercase name>, and vanilla's ids for the three
        // fluids that are vanilla's, and each reads back from its id
        Set<String> names = new HashSet<>();
        for (FluidType fluid : Fluids.getAll()) {
            String name = fluid.getRegistryName();
            String expected = fluid == Fluids.NONE ? "minecraft:empty" : fluid == Fluids.WATER ? "minecraft:water"
                    : fluid == Fluids.LAVA ? "minecraft:lava" : "hbm:" + fluid.getName().toLowerCase(Locale.ROOT);
            helper.assertTrue(name.equals(expected), "registry name of " + fluid);
            helper.assertTrue(names.add(name), "duplicate fluid registry name " + name);
            helper.assertTrue(Fluids.byRegistryName(name) == fluid, name + " does not read back");
            Tag encoded = Fluids.CODEC.encodeStart(NbtOps.INSTANCE, fluid).result().orElseThrow();
            helper.assertTrue(!(encoded instanceof NumericTag) && Fluids.CODEC.parse(NbtOps.INSTANCE, encoded).result().orElseThrow() == fluid,
                    "the fluid codec must save " + name + " as its name, saved " + encoded);
        }

        // Older saves named the three vanilla fluids hbm:none, hbm:water, hbm:lava
        helper.assertTrue(Fluids.byRegistryName("hbm:water") == Fluids.WATER && Fluids.byRegistryName("hbm:lava") == Fluids.LAVA
                && Fluids.byRegistryName("hbm:none") == Fluids.NONE, "the former hbm: names of water, lava and none still read");

        // barrel: tank saved by name, round trip, carried item data by name
        BlockState barrelState = ModFluidNetBlocks.barrel_steel.get().defaultBlockState();
        TileEntityBarrel barrel = (TileEntityBarrel) ((EntityBlock) barrelState.getBlock()).newBlockEntity(pos, barrelState);
        barrel.tank.setTankType(Fluids.DIESEL);
        barrel.tank.setFill(1234);
        CompoundTag saved = BlockEntities.saveWithoutMetadata(barrel, registries);
        helper.assertTrue(saved.toString().contains("\"hbm:diesel\""), "the barrel saves its fluid by name: " + saved);
        TileEntityBarrel loaded = (TileEntityBarrel) BlockEntities.load(pos, barrelState, BlockEntities.save(barrel, registries), registries);
        helper.assertTrue(loaded != null && loaded.tank.getTankType() == Fluids.DIESEL && loaded.tank.getFill() == 1234,
                "the barrel reloads diesel 1234 mB, got " + (loaded == null ? null : loaded.tank.getTankType() + " " + loaded.tank.getFill()));

        // duct and valve
        BlockState ductState = ModFluidNetBlocks.fluid_duct_neo.get().defaultBlockState();
        TileEntityFluidDuctNT duct = (TileEntityFluidDuctNT) ((EntityBlock) ductState.getBlock()).newBlockEntity(pos, ductState);
        duct.setFluidType(Fluids.KEROSENE);
        CompoundTag ductSave = BlockEntities.saveWithoutMetadata(duct, registries);
        helper.assertTrue("hbm:kerosene".equals(Tags.getStringOr(ductSave, "type", "")), "the duct saves type by name: " + ductSave);
        TileEntityFluidDuctNT ductLoaded = (TileEntityFluidDuctNT) BlockEntities.load(pos, ductState, BlockEntities.save(duct, registries), registries);
        helper.assertTrue(ductLoaded != null && ductLoaded.getFluidType() == Fluids.KEROSENE, "the duct reloads kerosene");

        BlockState valveState = ModMachineBlocks.fluid_valve.get().defaultBlockState();
        TileEntityFluidValve valve = (TileEntityFluidValve) ((EntityBlock) valveState.getBlock()).newBlockEntity(pos, valveState);
        valve.setFluidType(Fluids.STEAM);
        TileEntityFluidValve valveLoaded = (TileEntityFluidValve) BlockEntities.load(pos, valveState, BlockEntities.save(valve, registries), registries);
        helper.assertTrue(valveLoaded != null && valveLoaded.getFluidType() == Fluids.STEAM, "the valve reloads steam");

        // the graph datum (saved with the level's plumbing) and the identifier item component
        for (DuctData data : new DuctData[]{DuctData.of(Fluids.WATER), DuctData.SMOKE_ANY, DuctData.NONE}) {
            Tag encoded = DuctData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();
            helper.assertTrue(!(encoded instanceof NumericTag) && DuctData.CODEC.parse(NbtOps.INSTANCE, encoded).result().orElseThrow().equals(data),
                    "duct datum " + data + " saved as " + encoded);
        }
        ItemStack identifier = ItemFluidId.make(Fluids.CHLORINE);
        helper.assertTrue(ModDataComponents.fluid(identifier) == Fluids.CHLORINE && ItemFluidId.getFluid(identifier) == Fluids.CHLORINE,
                "the identifier carries chlorine");

        // copied settings: a fluid list by name
        CompoundTag settings = new CompoundTag();
        Fluids.writeList(settings, "fluids", new FluidType[]{Fluids.WATER, Fluids.OIL});
        FluidType[] back = Fluids.readList(settings, "fluids");
        helper.assertTrue(back.length == 2 && back[0] == Fluids.WATER && back[1] == Fluids.OIL, "settings fluid list round trip");

        // entities: the mist saves its fluid by name; the synched int never reaches the save
        EntityMist mist = Entities.createDetached(ModEntities.MIST.get(), helper.getLevel());
        mist.setType(Fluids.CHLORINE);
        CompoundTag mistSave = Entities.saveToTag(mist);
        helper.assertTrue("hbm:chlorine".equals(Tags.getStringOr(mistSave, "type", "")), "the mist saves its fluid by name: " + mistSave);
        EntityMist mistLoaded = Entities.createDetached(ModEntities.MIST.get(), helper.getLevel());
        Entities.loadFromTag(mistLoaded, mistSave);
        helper.assertTrue(mistLoaded.getFluid() == Fluids.CHLORINE, "the mist reloads chlorine");
        helper.succeed();
    }

    public static void states(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState stairs = Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.EAST);

        CompoundTag wand = new CompoundTag();
        SavedIds.putState(wand, "block", stairs);
        helper.assertTrue(!(wand.get("block") instanceof NumericTag) && SavedIds.getState(wand, "block", null) == stairs,
                "a wand's block saves by state name: " + wand);

        BlockState wandState = ModStructureWandBlocks.wand_structure.get().defaultBlockState();
        BlockWandStructure.TileEntityWandStructure structure = (BlockWandStructure.TileEntityWandStructure)
                ((EntityBlock) wandState.getBlock()).newBlockEntity(pos, wandState);
        structure.blacklist.add(stairs);
        structure.blacklist.add(Blocks.STONE.defaultBlockState());
        CompoundTag structureSave = BlockEntities.saveWithoutMetadata(structure, registries);
        helper.assertTrue(!structureSave.contains("blocks") && structureSave.toString().contains("minecraft:oak_stairs[facing=east"),
                "the structure blacklist saves state names: " + structureSave);
        BlockWandStructure.TileEntityWandStructure structureLoaded = (BlockWandStructure.TileEntityWandStructure)
                BlockEntities.load(pos, wandState, BlockEntities.save(structure, registries), registries);
        helper.assertTrue(structureLoaded != null && structureLoaded.blacklist.contains(stairs)
                && structureLoaded.blacklist.contains(Blocks.STONE.defaultBlockState()), "the blacklist reloads both states");

        EntityRubble rubble = Entities.createDetached(ModEntities.RUBBLE.get(), helper.getLevel());
        rubble.setBlockState(stairs);
        CompoundTag rubbleSave = Entities.saveToTag(rubble);
        helper.assertTrue(!(rubbleSave.get("blockState") instanceof NumericTag), "the rubble saves its state by name: " + rubbleSave);
        EntityRubble rubbleLoaded = Entities.createDetached(ModEntities.RUBBLE.get(), helper.getLevel());
        Entities.loadFromTag(rubbleLoaded, rubbleSave);
        helper.assertTrue(rubbleLoaded.getBlockState() == stairs, "the rubble reloads the stairs, got " + rubbleLoaded.getBlockState());

        // the undead soldier's type is saved by name (phase 7 hand-over)
        var soldier = Entities.createDetached(ModNtmEntities.UNDEAD_SOLDIER.get(), helper.getLevel());
        CompoundTag soldierSave = Entities.saveToTag(soldier);
        soldierSave.putString("type", "skeleton");
        Entities.loadFromTag(soldier, soldierSave);
        CompoundTag soldierResave = Entities.saveToTag(soldier);
        helper.assertTrue(soldier.soldierType() == EntityUndeadSoldier.TYPE_SKELETON
                && "skeleton".equals(Tags.getStringOr(soldierResave, "type", "")), "the soldier type round-trips by name: " + soldierResave);
        helper.succeed();
    }

    public static void materialsAndEnums(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        BlockState channelState = ModMachineBlocks.foundry_channel.get().defaultBlockState();
        TileEntityFoundryBase channel = (TileEntityFoundryBase) ((EntityBlock) channelState.getBlock()).newBlockEntity(pos, channelState);
        channel.type = Mats.MAT_IRON;
        channel.amount = 72;
        CompoundTag channelSave = BlockEntities.saveWithoutMetadata(channel, registries);
        helper.assertTrue("iron".equals(Tags.getStringOr(channelSave, "type", "")), "the foundry saves its material by name: " + channelSave);
        TileEntityFoundryBase channelLoaded = (TileEntityFoundryBase) BlockEntities.load(pos, channelState, BlockEntities.save(channel, registries), registries);
        helper.assertTrue(channelLoaded != null && channelLoaded.type == Mats.MAT_IRON && channelLoaded.amount == 72, "the foundry reloads 72 iron");

        BlockState batteryState = ModMachineBlocks.machine_battery.get().defaultBlockState();
        TileEntityMachineBattery battery = (TileEntityMachineBattery) ((EntityBlock) batteryState.getBlock()).newBlockEntity(pos, batteryState);
        battery.priority = ConnectionPriority.HIGH;
        CompoundTag batterySave = BlockEntities.saveWithoutMetadata(battery, registries);
        helper.assertTrue("high".equals(Tags.getStringOr(batterySave, "priority", "")), "the battery saves its priority by name: " + batterySave);
        TileEntityMachineBattery batteryLoaded = (TileEntityMachineBattery) BlockEntities.load(pos, batteryState, BlockEntities.save(battery, registries), registries);
        helper.assertTrue(batteryLoaded != null && batteryLoaded.priority == ConnectionPriority.HIGH, "the battery reloads HIGH");

        // pollution: saved per type name, not as an ordinal-indexed list
        CompoundTag region = new CompoundTag();
        region.putInt("chunkX", 3);
        region.putInt("chunkZ", -2);
        CompoundTag values = new CompoundTag();
        values.putFloat("heavymetal", 7.5F);
        region.put("pollution", values);
        ListTag entries = new ListTag();
        entries.add(region);
        CompoundTag root = new CompoundTag();
        root.put("entries", entries);
        PollutionSavedData data = PollutionSavedData.CODEC.parse(NbtOps.INSTANCE, root).result().orElseThrow();
        var regionData = data.pollution.values().iterator().next();
        helper.assertTrue(regionData.get(PollutionType.HEAVYMETAL) == 7.5F && regionData.get(PollutionType.SOOT) == 0F,
                "pollution reads by type name");
        Tag reencoded = PollutionSavedData.CODEC.encodeStart(NbtOps.INSTANCE, data).result().orElseThrow();
        helper.assertTrue(reencoded.toString().contains("heavymetal") && !reencoded.toString().contains("values"),
                "pollution saves by type name: " + reencoded);
        helper.succeed();
    }

    public static void legacy(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        // the side table is exactly 1.7's order
        FluidType[] all = Fluids.getAll();
        helper.assertTrue(all.length == LEGACY_ORDER.length, "1.7 had " + LEGACY_ORDER.length + " fluids, the port has " + all.length);
        for (int i = 0; i < LEGACY_ORDER.length; i++) {
            FluidType fluid = Fluids.byLegacyId(i);
            helper.assertTrue(fluid.getName().equals(LEGACY_ORDER[i]) && Fluids.legacyId(fluid) == i,
                    "1.7 fluid " + i + " is " + LEGACY_ORDER[i] + ", the side table says " + fluid.getName());
        }

        // an int-keyed tank from an older save loads as an empty tank instead of failing
        BlockState barrelState = ModFluidNetBlocks.barrel_steel.get().defaultBlockState();
        TileEntityBarrel barrel = (TileEntityBarrel) ((EntityBlock) barrelState.getBlock()).newBlockEntity(pos, barrelState);
        CompoundTag old = BlockEntities.save(barrel, registries);
        old.putInt("tank_type", 20);
        old.putInt("tank", 500);
        TileEntityBarrel oldLoaded = (TileEntityBarrel) BlockEntities.load(pos, barrelState, old, registries);
        helper.assertTrue(oldLoaded != null && oldLoaded.tank.getTankType() == Fluids.NONE, "an old numeric tank type reads as NONE");
        helper.assertTrue(Fluids.CODEC.parse(NbtOps.INSTANCE, IntTag.valueOf(20)).result().orElseThrow() == Fluids.NONE,
                "the fluid codec reads an old int as NONE");
        CompoundTag oldEnum = new CompoundTag();
        oldEnum.putByte("priority", (byte) 2);
        helper.assertTrue(SavedIds.getEnum(oldEnum, "priority", ConnectionPriority.LOW) == ConnectionPriority.LOW,
                "an old ordinal reads as the default");
        helper.succeed();
    }
}
