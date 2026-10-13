package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.ModCustomMissileBlocks;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.logic.BomberPayload;
import com.hbm.entity.logic.EntityBomber;
import com.hbm.handler.NTMToolHandler;
import com.hbm.handler.ability.IToolAreaAbility;
import com.hbm.handler.ability.IToolHarvestAbility;
import com.hbm.handler.ability.IWeaponAbility;
import com.hbm.interfaces.IEntityLookOverlay;
import com.hbm.inventory.RecipesCommon;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModWeaponModItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.packet.ParticlePayload;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import com.hbm.tileentity.machine.TileEntityMachineMissileAssembly;
import dev.golem.core.data.Tags;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.world.LightLevels;
import io.netty.buffer.Unpooled;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Map;

/**
 * The content registries opened to addons in M2 batch V2 (ADDON-API-INVENTORY.md V-081..V-162 and their E/X lines):
 * each registry or data table holds exactly 1.7's entries and values, and saves carry names, never numbers.
 */
public final class AddonContentGameTests {

    private AddonContentGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);

    public static final GameTests.Function BOMBERS =
            TESTS.function("addon_content_bomber_payloads", 20, () -> AddonContentGameTests::bomberPayloads);
    public static final GameTests.Function WEAPON_MODS =
            TESTS.function("addon_content_weapon_mod_names", 20, () -> AddonContentGameTests::weaponModNames);
    public static final GameTests.Function TOOL_CONVERSIONS =
            TESTS.function("addon_content_tool_conversions", 20, () -> AddonContentGameTests::toolConversions);
    public static final GameTests.Function LAUNCHABLES =
            TESTS.function("addon_content_launchables", 20, () -> AddonContentGameTests::launchables);
    public static final GameTests.Function BEDROCK_TYPES =
            TESTS.function("addon_content_bedrock_ore_types", 20, () -> AddonContentGameTests::bedrockTypes);
    public static final GameTests.Function ABILITIES =
            TESTS.function("addon_content_abilities", 20, () -> AddonContentGameTests::abilities);
    public static final GameTests.Function ENTITY_LOOK_OVERLAY =
            TESTS.function("addon_content_entity_look_overlay", 20, () -> AddonContentGameTests::entityLookOverlay);
    public static final GameTests.Function MISSILE_PART_PROPERTIES =
            TESTS.function("addon_content_missile_part_properties", 20, () -> AddonContentGameTests::missilePartProperties);

    public static final GameTests.Function EFFECTS =
            TESTS.function("addon_content_effects", 20, () -> AddonContentGameTests::effects);
    public static final GameTests.Function MULTIBLOCK_LIGHT =
            TESTS.function("multiblock_light_transparent", 20, () -> AddonContentGameTests::multiblockLight);

    public static void init() {}

    /**
     * E-046: HBM's 44 effects keep their 1.7 names in declaration order; an addon's namespaced effect is registered,
     * found by name, listed after HBM's, survives the particle payload, and a bare or taken name is refused.
     */
    public static void effects(GameTestHelper helper) {
        var all = HbmEffectNT.values();
        helper.assertTrue(all.length >= 44 && all[0] == HbmEffectNT.Flamethrower
                && all[43] == HbmEffectNT.Ufo
                && "ExplosionSmall".equals(HbmEffectNT.ExplosionSmall.name()),
                "HBM's effects must come first, in 1.7 order, under their 1.7 names");
        var probe = HbmEffectNT.byName("hbm_test:probe");
        if (probe == null) probe = HbmEffectNT.register("hbm_test:probe");
        helper.assertTrue(HbmEffectNT.byName("hbm_test:probe") == probe
                && Arrays.asList(HbmEffectNT.values()).indexOf(probe) >= 44,
                "an addon effect must resolve by name and follow HBM's");
        boolean bareRefused = false, takenRefused = false;
        try { HbmEffectNT.register("Probe"); } catch (IllegalArgumentException e) { bareRefused = true; }
        try { HbmEffectNT.register("hbm_test:probe"); } catch (IllegalArgumentException e) { takenRefused = true; }
        helper.assertTrue(bareRefused && takenRefused, "a bare or taken effect name must be refused");
        var buffer = GolemBuffers.wrap(Unpooled.buffer(), helper.getLevel());
        ParticlePayload.CODEC.encode(new ParticlePayload(probe.name(), 1, 2, 3, new CompoundTag()), buffer);
        helper.assertTrue(HbmEffectNT.byName(ParticlePayload.CODEC.decode(buffer).effect()) == probe,
                "an addon effect must survive the particle payload");
        probe.summonParticle(helper.getLevel(), 0, 0, 0, null); // no handler on a server: silent
        probe.send(helper.getLevel(), helper.absolutePos(BlockPos.ZERO), null);
        helper.succeed();
    }

    /**
     * X-24: every state of every folded multiblock block (core and cells, 1.7 {@code BlockDummyable}) has light
     * opacity 0 and lets sky light down at full strength, as 1.7's non-opaque blocks did; checked on vanilla's
     * per-state light cache, so the light engine and {@code canSeeSky} read the same.
     */
    public static void multiblockLight(GameTestHelper helper) {
        int blocks = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof BlockMultiblockCore)
                    && !(block instanceof BlockMultiblockCell)) continue;
            blocks++;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                if (!state.getFluidState().isEmpty()) continue;
                helper.assertTrue(LightLevels.opacity(state) == 0
                                && LightLevels.propagatesSkylightDown(state),
                        "multiblock state is not light-transparent: " + state);
            }
        }
        helper.assertTrue(blocks > 100, "expected the folded multiblocks, found " + blocks);
        var core = ModMachineBlocks.machine_assembly_machine.get().defaultBlockState();
        helper.assertTrue(LightLevels.opacity(core) == 0, "assembly machine must have opacity 0");
        helper.succeed();
    }

    /** C-20: an entity that is no {@code IEntityLookOverlay} has no readout (and plain HBM has no entity that is). */
    public static void entityLookOverlay(GameTestHelper helper) {
        var player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        helper.assertTrue(IEntityLookOverlay.of(player, player) == null
                && IEntityLookOverlay.of(null, player) == null, "no readout for a plain entity");
        helper.succeed();
    }

    /**
     * V-167: with no listener on {@code ModCustomMissileItems.PART_PROPERTIES} (plain HBM) every custom missile part
     * stacks to one, as 1.7, and the missile assembly holds one item per slot (hbm-space's limit).
     */
    public static void missilePartProperties(GameTestHelper helper) {
        int parts = 0;
        for (var item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ItemCustomMissilePart)) continue;
            var key = BuiltInRegistries.ITEM.getKey(item);
            if (!Constants.MOD_ID.equals(key.getNamespace())) continue;
            parts++;
            if (ModCustomMissileItems.PART_PROPERTIES.iterator().hasNext()) continue; // an addon is loaded
            helper.assertTrue(new ItemStack(item).getMaxStackSize() == 1, key + " must stack to 1 without an addon listener");
        }
        helper.assertTrue(parts == 122, "1.7 has 122 custom missile parts, found " + parts);
        var assembly = new TileEntityMachineMissileAssembly(BlockPos.ZERO,
                ModCustomMissileBlocks.machine_missile_assembly.get().defaultBlockState());
        helper.assertTrue(assembly.getMaxStackSize() == 1, "the missile assembly holds one item per slot");
        helper.succeed();
    }

    /** 1.7 {@code EntityBomber.statFac*}: (start, stop, rate) per {@code type} 0..7, and the airframe overrides. */
    public static void bomberPayloads(GameTestHelper helper) {
        String[] names = {"carpet", "napalm", "chlorine", "orange", "abomb", "stinger", "boxcar", "pc"};
        int[][] timing = {{50, 100, 2}, {50, 100, 5}, {50, 100, 4}, {75, 125, 1}, {60, 70, 65}, {50, 150, 10},
                {50, 150, 10}, {75, 125, 1}};
        List<BomberPayload> payloads = BomberPayload.REGISTRY.values();
        helper.assertTrue(payloads.size() >= names.length, "1.7 has eight bomber payloads");
        var level = helper.getLevel();
        for (int i = 0; i < names.length; i++) {
            BomberPayload payload = payloads.get(i);
            helper.assertTrue(names[i].equals(payload.name()) && BomberPayload.byName(names[i]) == payload,
                    "payload " + i + " must be " + names[i] + ", got " + payload.name());
            EntityBomber bomber = payload.create(level, 0, 64, 0);
            helper.assertTrue(Arrays.equals(timing[i], bomber.timing()) && names[i].equals(bomber.payload())
                            && bomber.timer == 200,
                    names[i] + " must time " + Arrays.toString(timing[i]) + " over 200 ticks, got "
                            + Arrays.toString(bomber.timing()) + " / " + bomber.timer);
            int style = bomber.getStyle();
            if (i == 5) helper.assertTrue(style == 4, "the stinger flies airframe 4");
            if (i == 6 || i == 7) helper.assertTrue(style == 6, names[i] + " flies airframe 6");
            if (i == 4) helper.assertTrue(style >= 5 && style <= 8, "the atomic bomber flies airframe 5..8");
            bomber.discard(); // never added; releases the chunk fac() force-loaded
        }
        helper.succeed();
    }

    /** Installed weapon modifiers are saved by name; 1.7's numeric ids stay runtime handles only. */
    public static void weaponModNames(GameTestHelper helper) {
        XWeaponModManager.init();
        Map<Integer, String> expected = Map.of(0, "hbm:test_firerate", 117, "hbm:bronze_dura",
                XWeaponModManager.ID_SILENCER, "hbm:silencer", XWeaponModManager.ID_SCOPE, "hbm:scope",
                XWeaponModManager.ID_CARBINE_BAYONET, "hbm:bayonet_carbine", 232, "hbm:canisters",
                371, "hbm:caliber_bmg50_minigun");
        for (var entry : expected.entrySet()) {
            var mod = XWeaponModManager.idToMod.get(entry.getKey());
            helper.assertTrue(mod != null && entry.getValue().equals(XWeaponModManager.nameOf(mod))
                            && XWeaponModManager.handle(entry.getValue()) == entry.getKey(),
                    "1.7 mod id " + entry.getKey() + " must be saved as " + entry.getValue());
        }
        // every 1.7 modifier has a name
        for (var entry : XWeaponModManager.idToMod.entrySet()) {
            if (entry.getKey() >= XWeaponModManager.FIRST_ADDON_HANDLE) continue;
            helper.assertTrue(XWeaponModManager.nameOf(entry.getValue()) != null, "1.7 mod id " + entry.getKey() + " has no name");
        }

        ItemStack pepperbox = new ItemStack(ModGunItems.gun_pepperbox.get());
        XWeaponModManager.install(helper.getLevel(), pepperbox, 0,
                ModWeaponModItems.weapon_mod_generic.stack(GunFactory.EnumModGeneric.IRON_DAMAGE));
        CompoundTag data = ItemGunBaseNT.getGunData(pepperbox);
        helper.assertTrue("hbm:iron_damage".equals(Tags.getStringOr(data, XWeaponModManager.KEY_MOD_NAMES + 0, ""))
                        && !Tags.contains(data, XWeaponModManager.KEY_MOD_LIST + 0)
                        && XWeaponModManager.hasUpgrade(pepperbox, 0, 100),
                "the iron damage mod must be saved as its name, got " + data);

        // a gun modded before names: the numeric list is not read (one WARN), the gun has no mods
        ItemStack old = new ItemStack(ModGunItems.gun_pepperbox.get());
        CompoundTag legacy = ItemGunBaseNT.getGunData(old);
        legacy.putIntArray(XWeaponModManager.KEY_MOD_LIST + 0, new int[] {100});
        ItemGunBaseNT.setGunData(old, legacy);
        helper.assertTrue(!XWeaponModManager.hasUpgrade(old, 0, 100), "a numeric mod id must not be read back");
        helper.succeed();
    }

    /** {@code data/hbm/recipe/tool_conversion} equals 1.7's {@code NTMToolHandler} table (four conversions). */
    public static void toolConversions(GameTestHelper helper) {
        helper.assertTrue(NTMToolHandler.TABLE.rows().size() == 4, "1.7 has four tool conversions, data has "
                + NTMToolHandler.TABLE.rows().size());
        conversion(helper, ToolType.BOLT, ModMachineBlocks.watz_casing.get(), ModMachineBlocks.watz_casing_tooled.get(),
                List.<Object[]>of(new Object[] {Mats.MAT_DURA, MaterialShapes.BOLT, 4}));
        conversion(helper, ToolType.TORCH, ModMachineBlocks.fusion_component.get(),
                ModMachineBlocks.fusion_component_bscco_welded.get(), List.<Object[]>of(new Object[] {Mats.MAT_STEEL, MaterialShapes.CASTPLATE, 1}));
        // 1.7 ANY_BISMOIDBRONZE.plateCast(): any bismoid bronze cast plate, the group's tag
        conversion(helper, ToolType.TORCH, ModIcfBlocks.icf_component_vessel.get(), ModIcfBlocks.icf_component_vessel_welded.get(),
                List.<Object[]>of(new Object[] {"c:plates_cast/any_bismoid_bronze", null, 1}));
        conversion(helper, ToolType.BOLT, ModIcfBlocks.icf_component_structure.get(), ModIcfBlocks.icf_component_structure_bolted.get(),
                List.<Object[]>of(new Object[] {Mats.MAT_STEEL, MaterialShapes.CASTPLATE, 1}, new Object[] {Mats.MAT_DURA, MaterialShapes.BOLT, 4}));
        helper.succeed();
    }

    private static void conversion(GameTestHelper helper, ToolType tool, Block from, Block to, List<Object[]> materials) {
        NTMToolHandler.Conversion conversion = NTMToolHandler.get(tool, from);
        helper.assertTrue(conversion != null && conversion.result().get() == to, tool + " on " + from + " must yield " + to);
        helper.assertTrue(conversion.materials().size() == materials.size(), from + " costs " + materials.size() + " stacks");
        for (int i = 0; i < materials.size(); i++) {
            Object[] want = materials.get(i);
            RecipesCommon.AStack have = conversion.materials().get(i);
            boolean same = want[0] instanceof String tag
                    ? have instanceof RecipesCommon.TagStack tagged && tagged.tag.location().toString().equals(tag)
                            && tagged.stacksize == (int) want[2]
                    : have instanceof RecipesCommon.MaterialStack stack
                            && stack.material == want[0] && stack.shape == want[1] && stack.stacksize == (int) want[2];
            helper.assertTrue(same, from + " material " + i + " differs from 1.7");
        }
    }

    /** The launch table holds 1.7's 27 launchable missiles, and building it twice adds nothing. */
    public static void launchables(GameTestHelper helper) {
        TileEntityLaunchPadBase.registerLaunchables();
        int size = TileEntityLaunchPadBase.missiles.size();
        TileEntityLaunchPadBase.registerLaunchables();
        helper.assertTrue(size >= 27 && TileEntityLaunchPadBase.missiles.size() == size,
                "1.7 has 27 launchable missiles (plus any addon's), got " + size);
        helper.succeed();
    }

    /** 1.7's six bedrock ore types lead the registry, by their saved names, with their exact outputs. */
    public static void bedrockTypes(GameTestHelper helper) {
        String[] names = {"light", "heavy", "rare", "actinide", "nonmetal", "crystal"};
        helper.assertTrue(BedrockOreType.VALUES.length == 6, "1.7 has six bedrock ore types");
        for (int i = 0; i < names.length; i++) {
            BedrockOreType type = BedrockOreType.VALUES[i];
            helper.assertTrue(names[i].equals(type.suffix) && BedrockOreType.byName(names[i]) == type
                            && BedrockOreType.values().get(i) == type && type.ordinal() == i,
                    "bedrock ore type " + i + " must be " + names[i]);
            helper.assertTrue(type.outputs().size() == 11, names[i] + " has 11 outputs");
        }
        helper.assertTrue(BedrockOreType.LIGHT_METAL.primary1.material() == Mats.MAT_IRON && BedrockOreType.LIGHT_METAL.primary1.amount() == 9
                        && BedrockOreType.ACTINIDE.byproductRad3.material() == Mats.MAT_U238 && BedrockOreType.ACTINIDE.byproductRad3.amount() == 1
                        && BedrockOreType.CRYSTALLINE.byproductSolvent3.material() == Mats.MAT_EMERALD,
                "bedrock ore outputs differ from 1.7");
        helper.succeed();
    }

    /** The ability registries hold 1.7's abilities in 1.7 order, found by the name tool presets save. */
    public static void abilities(GameTestHelper helper) {
        helper.assertTrue(IToolAreaAbility.REGISTRY.values().subList(0, 5).equals(List.of(IToolAreaAbility.NONE,
                IToolAreaAbility.RECURSION, IToolAreaAbility.HAMMER, IToolAreaAbility.HAMMER_FLAT, IToolAreaAbility.EXPLOSION)),
                "area abilities differ from 1.7");
        helper.assertTrue(IToolHarvestAbility.REGISTRY.values().subList(0, 8).equals(List.of(IToolHarvestAbility.NONE,
                IToolHarvestAbility.SILK, IToolHarvestAbility.LUCK, IToolHarvestAbility.SMELTER, IToolHarvestAbility.SHREDDER,
                IToolHarvestAbility.CENTRIFUGE, IToolHarvestAbility.CRYSTALLIZER, IToolHarvestAbility.MERCURY)),
                "harvest abilities differ from 1.7");
        helper.assertTrue(IWeaponAbility.REGISTRY.values().subList(0, 9).equals(List.of(IWeaponAbility.NONE,
                IWeaponAbility.RADIATION, IWeaponAbility.VAMPIRE, IWeaponAbility.STUN, IWeaponAbility.PHOSPHORUS,
                IWeaponAbility.FIRE, IWeaponAbility.CHAINSAW, IWeaponAbility.BEHEADER, IWeaponAbility.BOBBLE)),
                "weapon abilities differ from 1.7");
        helper.assertTrue(IToolAreaAbility.getByName("tool.ability.hammer") == IToolAreaAbility.HAMMER
                        && IToolHarvestAbility.getByName("tool.ability.mercury") == IToolHarvestAbility.MERCURY
                        && IToolAreaAbility.getByName("nonsense") == IToolAreaAbility.NONE,
                "a saved ability name must resolve as before");
        helper.succeed();
    }
}
