package com.hbm.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hbm.Constants;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.machine.MachineFan;
import com.hbm.blocks.machine.MachineFan.TileEntityFan;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.entity.projectile.EntityShrapnel;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerMachineAssemblyFactory;
import com.hbm.inventory.container.ContainerMachinePrecAss;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModWatzItems;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.packet.HbmPackets;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import com.hbm.tileentity.machine.TileEntityMachinePrecAss;
import com.hbm.tileentity.machine.TileEntityWatz;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/** GitHub issues 122 to 125: 1.7's creative tabs, redstone a client can see, the Watz mud overflow, recipes picked through the real packet. */
public final class GhIssue122To125GameTests {

    private GhIssue122To125GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function FAN =
            TESTS.function("gh123_fan_reads_redstone_every_tick", 40, () -> GhIssue122To125GameTests::fanReadsRedstone);
    public static final GameTests.Function WATZ =
            TESTS.function("gh124_watz_mud_overflow_ruptures", 60, 10, () -> GhIssue122To125GameTests::watzMudOverflow);
    public static final GameTests.Function PREC =
            TESTS.function("gh125_precass_selector_packet", 40, 8, () -> GhIssue122To125GameTests::precassSelector);
    public static final GameTests.Function FACTORY =
            TESTS.function("gh125_factory_selector_packet", 40, 8, () -> GhIssue122To125GameTests::factorySelector);
    public static final GameTests.Function PERMISSION =
            TESTS.function("gh125_every_control_receiver_answers_permission", 20, () -> GhIssue122To125GameTests::everyReceiverAnswers);

    public static final GameTests.Function TABS =
            TESTS.function("gh122_items_1_7_keeps_out_of_its_tabs", 20, () -> GhIssue122To125GameTests::tabsFollow17);

    public static final GameTests.Function MODIFIER_TAGS =
            TESTS.function("biome_modifier_composites_share_no_tag", 20, () -> GhIssue122To125GameTests::compositesShareNoTag);

    public static void init() {}

    /**
     * NeoForge's composite holder sets add a listener to a tag's shared set while decoding, in a plain list, and 26.1 decodes
     * registry files in parallel: two files naming one tag inside a composite can crash a world load.
     */
    public static void compositesShareNoTag(GameTestHelper helper) {
        ResourceManager resources = helper.getLevel().getServer().getResourceManager();
        Map<String, String> owners = new HashMap<>();
        List<String> shared = new ArrayList<>();
        for (String folder : List.of("neoforge/biome_modifier", "forge/biome_modifier")) {
            resources.listResources(folder, id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
                try (Reader reader = resource.openAsReader()) {
                    JsonObject modifier = JsonParser.parseReader(reader).getAsJsonObject();
                    Set<String> tags = new HashSet<>();
                    compositeTags(modifier.get("biomes"), tags);
                    for (String tag : tags) {
                        String other = owners.putIfAbsent(tag, id.toString());
                        if (other != null) shared.add(tag + " in " + other + " and " + id);
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(id.toString(), e);
                }
            });
        }
        helper.assertTrue(shared.isEmpty(), "composites sharing a tag: " + shared);

        for (String name : List.of("bedrock_deposits_oil", "bedrock_deposits_ore")) {
            sameMembers(helper, "biome_modifier/" + name + "/biomes", Constants.MOD_ID + ":has_feature/bedrock_deposits");
            sameMembers(helper, "biome_modifier/" + name + "/excluded", "minecraft:is_overworld");
        }
        helper.succeed();
    }

    private static void compositeTags(JsonElement set, Set<String> tags) {
        if (set == null || !set.isJsonObject()) return;
        JsonObject composite = set.getAsJsonObject();
        for (String key : List.of("value", "values")) {
            JsonElement member = composite.get(key);
            if (member == null) continue;
            for (JsonElement entry : member.isJsonArray() ? member.getAsJsonArray().asList() : List.of(member)) {
                if (entry.isJsonPrimitive() && entry.getAsString().startsWith("#")) tags.add(entry.getAsString());
                else compositeTags(entry, tags);
            }
        }
    }

    private static void sameMembers(GameTestHelper helper, String ownPath, String source) {
        int colon = source.indexOf(':');
        TagKey<Biome> own = TagKey.create(Registries.BIOME, Ids.of(Constants.MOD_ID, ownPath));
        TagKey<Biome> original = TagKey.create(Registries.BIOME, Ids.of(source.substring(0, colon), source.substring(colon + 1)));
        helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).listElements().forEach(biome ->
                helper.assertTrue(biome.is(own) == biome.is(original), ownPath + " differs from " + source + " at " + biome.key()));
    }

    /** 1.7 passes these a null tab or none at all. They stay registered, because worlds, loot and recipes name them. */
    private static final List<String> NO_TAB = List.of(
            "coal_eternal", "circuit_star", "cbt_device", "plate_fuel_u233", "plate_fuel_u235", "plate_fuel_mox",
            "plate_fuel_pu239", "plate_fuel_sa326", "plate_fuel_ra226be", "plate_fuel_pu238be", "ams_core_thingy", "key_red",
            "key_red_cracked", "mech_key", "holotape_damaged", "burnt_bark", "structure_anchor", "machine_uf6_tank",
            "machine_puf6_tank", "fusion_heater", "fusion_hatch", "rail_narrow_straight", "rail_narrow_curve",
            "rail_large_straight", "rail_large_straight_short", "rail_large_curve", "rail_large_curve_7", "rail_large_curve_9",
            "rail_large_ramp", "rail_large_buffer", "rail_large_switch", "rail_large_switch_flipped", "crystal_hardened",
            "taint", "pink_planks", "bottle_empty", "bottle2_empty", "watch", "template_folder");

    /** 1.7 shows these: the capacitors here are its battery pack types, not its tabless capacitor blocks of the same name. */
    private static final List<String> STILL_TABBED = List.of("capacitor_gold", "capacitor_niobium", "rbmk_fuel_test");

    public static void tabsFollow17(GameTestHelper helper) {
        Set<String> listed = new HashSet<>();
        for (HbmTab tab : HbmTab.values()) {
            ModTabs.displayStacks(ModTabs.get(tab).getKey())
                    .forEach(stack -> listed.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()));
        }
        List<String> shown = NO_TAB.stream().filter(listed::contains).toList();
        helper.assertTrue(shown.isEmpty(), "1.7 keeps these out of its tabs: " + shown);
        List<String> gone = STILL_TABBED.stream().filter(id -> !listed.contains(id)).toList();
        helper.assertTrue(gone.isEmpty(), "1.7 shows these in a tab: " + gone);
        List<String> unregistered = NO_TAB.stream().filter(id -> !BuiltInRegistries.ITEM.containsKey(Ids.of(Constants.MOD_ID, id))).toList();
        helper.assertTrue(unregistered.isEmpty(), "out of the tabs is not out of the game: " + unregistered);
        helper.succeed();
    }

    /** Flag 2 sends no neighbour update, which is all a client ever learns of a redstone change. */
    public static void fanReadsRedstone(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        BlockPos signal = helper.absolutePos(pos.north());
        helper.setBlock(pos, ModMachineBlocks.fan.get().defaultBlockState().setValue(MachineFan.FACING, Direction.EAST));
        TileEntityFan fan = GameTestHelpers.blockEntity(helper, pos, TileEntityFan.class);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(fan.spin == 0F, "an unpowered fan stands still, spin " + fan.spin);
            helper.getLevel().setBlock(signal, Blocks.REDSTONE_BLOCK.defaultBlockState(), 2);
        });
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(fan.spin != fan.prevSpin, "the fan starts when a signal appears");
            helper.getLevel().setBlock(signal, Blocks.AIR.defaultBlockState(), 2);
        });
        helper.runAfterDelay(13, () -> {
            helper.assertTrue(fan.spin == fan.prevSpin, "the fan stops when the signal goes");
            helper.succeed();
        });
    }

    /** A full mud tank does nothing while the reactor is off; the first mud past capacity ruptures it, kit and fuel dropping. */
    public static void watzMudOverflow(GameTestHelper helper) {
        BlockPos core = new BlockPos(5, 1, 5);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.watz.get(), core);
        helper.setBlock(core.above(3), ModMachineBlocks.watz_pump.get().defaultBlockState());
        TileEntityWatz watz = GameTestHelpers.blockEntity(helper, core, TileEntityWatz.class);
        watz.inventory.setStackInSlot(0, ModWatzItems.fresh(EnumWatzType.SCHRABIDIUM).make());
        watz.tanks[2].setFill(watz.tanks[2].getMaxFill());
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(helper.getBlockState(core).is(ModMachineBlocks.watz.get()), "a Watz that is off holds its full mud tank");
            helper.setBlock(core.above(5), Blocks.REDSTONE_BLOCK.defaultBlockState());
        });
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(helper.getBlockState(core).is(ModHazardBlocks.mud_block.get()), "the first mud past capacity turns the core to mud");
            BlockPos absolute = helper.absolutePos(core);
            AABB around = new AABB(absolute).inflate(12D);
            helper.getLevel().getEntitiesOfClass(EntityShrapnel.class, around).forEach(EntityShrapnel::discard);
            List<Item> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                    .map(entity -> entity.getItem().getItem()).toList();
            helper.assertTrue(drops.contains(ModMachineBlocks.struct_watz_core.get().asItem()), "1.7 setBlock runs breakBlock: the kit drops, saw " + drops);
            helper.assertTrue(drops.contains(ModWatzItems.fresh(EnumWatzType.SCHRABIDIUM)), "1.7 BlockDummyable drops the fuel, saw " + drops);
            helper.succeed();
        });
    }

    public static void precassSelector(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 1, 2);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_precass.get(), rel);
        TileEntityMachinePrecAss te = GameTestHelpers.blockEntity(helper, rel, TileEntityMachinePrecAss.class);
        GenericRecipe recipe = firstOpen(PrecAssRecipes.INSTANCE);
        try (var fixture = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = stand(fixture.player(), helper, rel);
            control(helper, player, rel, 0, recipe.getInternalName());
            helper.assertTrue(te.assemblerModule.recipe.equals("null"), "no menu open, no selection");
            player.containerMenu = new ContainerMachinePrecAss(0, player.getInventory(), te);
            control(helper, player, rel, 0, recipe.getInternalName());
            helper.assertTrue(te.assemblerModule.recipe.equals(recipe.getInternalName()), "the open menu's selection reaches the module");
        }
        te.power = te.maxPower = recipe.power * 100L;
        for (int i = 0; i < recipe.inputItem.length; i++) te.setItem(4 + i, recipe.inputItem[i].getCyclingDisplayStack());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(te.assemblerModule.recipe.equals(recipe.getInternalName()) && te.assemblerModule.progress > 0D,
                    "the picked recipe runs, progress " + te.assemblerModule.progress);
            helper.succeed();
        });
    }

    public static void factorySelector(GameTestHelper helper) {
        BlockPos rel = new BlockPos(3, 1, 3);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_assembly_factory.get(), rel);
        TileEntityMachineAssemblyFactory te = GameTestHelpers.blockEntity(helper, rel, TileEntityMachineAssemblyFactory.class);
        GenericRecipe recipe = firstOpen(AssemblyMachineRecipes.INSTANCE);
        try (var fixture = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = stand(fixture.player(), helper, rel);
            control(helper, player, rel, 2, recipe.getInternalName());
            helper.assertTrue(te.assemblerModule[2].recipe.equals("null"), "no menu open, no selection");
            player.containerMenu = new ContainerMachineAssemblyFactory(0, player.getInventory(), te);
            control(helper, player, rel, 2, recipe.getInternalName());
            helper.assertTrue(te.assemblerModule[2].recipe.equals(recipe.getInternalName()), "the open menu's selection reaches lane 2");
        }
        te.power = te.maxPower = recipe.power * 100L;
        te.water.setFill(te.water.getMaxFill());
        int start = TileEntityMachineAssemblyFactory.inputStart(2);
        for (int i = 0; i < recipe.inputItem.length; i++) te.setItem(start + i, recipe.inputItem[i].getCyclingDisplayStack());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(te.assemblerModule[2].recipe.equals(recipe.getInternalName()) && te.assemblerModule[2].progress > 0D,
                    "the picked recipe runs, progress " + te.assemblerModule[2].progress);
            helper.succeed();
        });
    }

    /** The interface answers false for safety, so a receiver that forgets the override silently ignores its GUI. */
    public static void everyReceiverAnswers(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        List<String> silent = new ArrayList<>();
        Set<Class<?>> seen = new HashSet<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Constants.MOD_ID)) continue;
            if (!(block instanceof EntityBlock entityBlock)) continue;
            BlockEntity be;
            try {
                be = entityBlock.newBlockEntity(pos, block.defaultBlockState());
            } catch (RuntimeException | LinkageError e) {
                continue; // a block entity that needs a level to exist
            }
            if (!(be instanceof IControlReceiver) || !seen.add(be.getClass())) continue;
            try {
                if (be.getClass().getMethod("hasPermission", Player.class).getDeclaringClass() == IControlReceiver.class) {
                    silent.add(be.getClass().getSimpleName());
                }
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        helper.assertTrue(seen.size() >= 20, "expected at least 20 control receivers, saw " + seen.size());
        helper.assertTrue(silent.isEmpty(), "control receivers that refuse every player: " + silent);
        helper.succeed();
    }

    private static GenericRecipe firstOpen(GenericRecipes table) {
        table.registerRecipes();
        for (GenericRecipe recipe : table.recipeOrderedList) {
            if (!recipe.isPooled() && recipe.inputItem != null && recipe.inputFluid == null) return recipe;
        }
        throw new IllegalStateException("no recipe outside a blueprint pool");
    }

    private static ServerPlayer stand(ServerPlayer player, GameTestHelper helper, BlockPos rel) {
        BlockPos at = helper.absolutePos(rel.above(4));
        player.setPos(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
        return player;
    }

    private static void control(GameTestHelper helper, ServerPlayer player, BlockPos rel, int index, String selection) {
        CompoundTag data = new CompoundTag();
        data.putInt("index", index);
        data.putString("selection", selection);
        HbmPackets.handleControl(player, helper.absolutePos(rel), data);
    }
}
