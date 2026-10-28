package com.hbm.gametest;

import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.BobbleFigure;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.client.render.util.RenderAccessoryUtility;
import com.hbm.handler.HazmatRegistry;
import com.hbm.handler.SpecialPlayers;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemDrive;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.potion.ModEffects;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyMachine;
import com.hbm.util.ShadyUtil;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Attributes;
import dev.golem.core.world.Effects;
import dev.golem.core.world.ItemStacks;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** The ShadyUtil players: the cape table and the server side of {@link SpecialPlayers}. */
public final class SpecialPlayerGameTests {

    private SpecialPlayerGameTests() {}

    private static final String[] CAPES = {"capehbm3", "capehbm2", "capedrillgon", "capedafnik", "capeshield",
            "capevertice_2", "capenostalgia2", "capesam", "capehoboy_mk3", "capemaster",
            "capemek", "capezippysqrl", "capetest", "capeschrabbyalt", "capesweatyswiggs", "capedoctor17",
            "capeblaze", "capeblaze2", "capewiki", "capeleftnugget", "caperightnugget", "capetankish",
            "capefrizzlefrazzle", "capevaer", "capeadam", "capejame", "capegolem"};

    public static void capes(GameTestHelper helper) {
        for (String cape : CAPES) {
            helper.assertTrue(GameTestHelpers.resourceExists(SpecialPlayerGameTests.class,
                    "assets/hbm/textures/models/capes/" + cape + ".png"), "missing cape texture " + cape);
        }
        for (String model : new String[] {"wings_pheo.obj", "tail_peep.obj", "murk.obj"}) {
            helper.assertTrue(GameTestHelpers.resourceExists(SpecialPlayerGameTests.class,
                    "assets/hbm/models/armor/" + model), "missing special-player model " + model);
        }
        for (String texture : new String[] {"wings_bob", "wings_black", "axepack", "tail_peep"}) {
            helper.assertTrue(GameTestHelpers.resourceExists(SpecialPlayerGameTests.class,
                    "assets/hbm/textures/armor/" + texture + ".png"), "missing special-player texture " + texture);
        }

        UUID stranger = UUID.fromString("00000000-0000-4000-8000-000000000001");
        Identifier bob = RenderAccessoryUtility.getCloakFromPlayer(UUID.fromString(ShadyUtil.HbMinecraft), "x");
        helper.assertTrue(bob != null && (bob.getPath().endsWith("capehbm3.png") || bob.getPath().endsWith("capehbm2.png")),
                "Bob does not wear CapeHbm3/CapeHbm2: " + bob);
        expect(helper, ShadyUtil.Dr_Nostalgia, "x", "capenostalgia2"); // 1.7 over 1.12.2's CapeNostalgia
        expect(helper, ShadyUtil.Hoboy03new, "x", "capehoboy_mk3"); // 1.7 over 1.12.2's CapeHoboy
        expect(helper, ShadyUtil.Golem, "x", "capegolem"); // 1.12.2 addition
        expect(helper, "06ab7c03-55ce-43f8-9d3c-2850e3c652de", "x", "capewiki");
        expect(helper, stranger.toString(), "Dev", "capetest");
        expect(helper, stranger.toString(), "Player512", "capetest");
        helper.assertTrue(RenderAccessoryUtility.getCloakFromPlayer(stranger, "Steve") == null, "a stranger wears a cape");
        helper.assertTrue(RenderAccessoryUtility.getCloakFromPlayer(
                UUID.fromString("0b399a4a-8545-45a1-be3d-ece70d7d48e9"), "Alcater") == null,
                "Alcater's cape is ported (user decision: skipped)");
        helper.assertTrue(RenderAccessoryUtility.getCloakFromPlayer(UUID.fromString(ShadyUtil.Barnaby99_x), "x") == null,
                "Pheo wears the cape 1.7 commented out");
        helper.succeed();
    }

    private static void expect(GameTestHelper helper, String uuid, String name, String cape) {
        Identifier found = RenderAccessoryUtility.getCloakFromPlayer(UUID.fromString(uuid), name);
        helper.assertTrue(found != null && found.getPath().equals("textures/models/capes/" + cape + ".png"),
                "expected " + cape + " for " + name + "/" + uuid + ", got " + found);
    }

    /** 1.7 onWingFlop: falling is capped at -0.4, then the C-toggle glide brakes it by 0.2 (bob, a fed the_NCR). */
    public static void wingFlop(GameTestHelper helper) {
        Player bob = airborne(helper, ShadyUtil.HbMinecraft);
        SpecialPlayers.tick(bob);
        helper.assertTrue(Math.abs(bob.getDeltaMovement().y + 0.2D) < 1e-9, "Bob's glide: " + bob.getDeltaMovement().y);
        helper.assertTrue(bob.fallDistance == 0, "Bob kept fall distance");

        Player hungry = airborne(helper, ShadyUtil.the_NCR);
        hungry.getFoodData().setFoodLevel(6);
        SpecialPlayers.tick(hungry);
        helper.assertTrue(Math.abs(hungry.getDeltaMovement().y + 0.4D) < 1e-9,
                "a hungry the_NCR only falls capped: " + hungry.getDeltaMovement().y);

        Player fed = airborne(helper, ShadyUtil.the_NCR);
        SpecialPlayers.tick(fed);
        helper.assertTrue(Math.abs(fed.getDeltaMovement().y + 0.2D) < 1e-9, "a fed the_NCR's glide: " + fed.getDeltaMovement().y);

        Player pheo = airborne(helper, ShadyUtil.Barnaby99_x);
        SpecialPlayers.tick(pheo);
        helper.assertTrue(Math.abs(pheo.getDeltaMovement().y + 0.925D) < 1e-9, "Pheo's glide: " + pheo.getDeltaMovement().y);

        Player plated = airborne(helper, ShadyUtil.HbMinecraft);
        plated.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        SpecialPlayers.tick(plated);
        helper.assertTrue(plated.getDeltaMovement().y == -1D && !SpecialPlayers.hasInnateFlight(plated),
                "Bob flies with a chestplate on");

        Player stranger = airborne(helper, UUID.randomUUID().toString());
        SpecialPlayers.tick(stranger);
        helper.assertTrue(stranger.getDeltaMovement().y == -1D, "a stranger flies");
        helper.succeed();
    }

    private static Player airborne(GameTestHelper helper, String uuid) {
        Player player = player(helper, uuid);
        player.setOnGround(false);
        player.setDeltaMovement(new Vec3(0, -1D, 0));
        player.fallDistance = 5;
        return player;
    }

    /** Pu_238: Radiation III for 15 s on everything else within 3 blocks, and 0.4 of his own resistance. */
    public static void puRadiation(GameTestHelper helper) {
        Player pu = player(helper, ShadyUtil.Pu_238);
        LivingEntity pig = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 1));
        SpecialPlayers.tick(pu);
        var effect = Effects.getRegistered(pig, ModEffects.RADIATION.get());
        helper.assertTrue(effect != null && effect.getAmplifier() == 2 && effect.getDuration() == 300,
                "Pu_238's aura: " + effect);
        helper.assertTrue(!Effects.hasRegistered(pu, ModEffects.RADIATION.get()), "Pu_238 irradiates himself");
        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(pu) - 0.4F) < 1e-6, "Pu_238's resistance");
        helper.assertTrue(HazmatRegistry.getResistance(player(helper, UUID.randomUUID().toString())) == 0F,
                "a stranger has Pu_238's resistance");
        helper.succeed();
    }

    /** Peep: +50% base movement speed while sprinting, gone when not. */
    public static void peepSprint(GameTestHelper helper) {
        Player peep = player(helper, ShadyUtil.LePeeperSauvage);
        var speed = peep.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        double base = speed.getValue();
        peep.setSprinting(true);
        SpecialPlayers.tick(peep);
        helper.assertTrue(Attributes.has(speed, "hbm:fop_speed"), "Peep's sprint modifier is missing");
        peep.setSprinting(false);
        SpecialPlayers.tick(peep);
        helper.assertTrue(!Attributes.has(speed, "hbm:fop_speed") && Math.abs(speed.getValue() - base) < 1e-9,
                "Peep's sprint modifier stays");
        helper.succeed();
    }

    /**
     * 1.12.2's Community Edition bobbleheads: the figure table ({@code BobbleFigure}, indexed by ordinal) still lines
     * up with the enum, the three skinned figures name their person's UUID, and Leafia's model is shipped.
     */
    public static void bobbleheads(GameTestHelper helper) {
        var types = BlockBobble.BobbleType.VALUES;
        helper.assertTrue(types.length == BobbleFigure.NAMES.length,
                "BobbleFigure.NAMES has " + BobbleFigure.NAMES.length + " entries for " + types.length + " types");
        for (var type : types) {
            int i = type.ordinal();
            helper.assertTrue(BobbleFigure.NAMES[i].equals(type.name()),
                    "figure " + i + " is " + BobbleFigure.NAMES[i] + ", type is " + type.name());
            BobbleFigure.suffix(i); // LAYERS covers every type
            boolean skinned = BobbleFigure.body(i) == BobbleFigure.Body.SKIN;
            helper.assertTrue(skinned == (type.skinUuid != null), type + " skin body/uuid mismatch");
        }
        helper.assertTrue(ShadyUtil.movblock.equals(BlockBobble.BobbleType.MOVBLOCK.skinUuid)
                && ShadyUtil.Hacker6329.equals(BlockBobble.BobbleType.HACKER6329.skinUuid)
                && ShadyUtil.EOS.equals(BlockBobble.BobbleType.EOS.skinUuid),
                "the skinned bobbleheads name the wrong people");
        helper.assertTrue(BlockBobble.BobbleType.LEAFIA.skinUuid == null
                && BobbleFigure.body(BlockBobble.BobbleType.LEAFIA.ordinal())
                        == BobbleFigure.Body.LEAFIA, "Leafia is not the modelled figure");
        helper.assertTrue(GameTestHelpers.resourceExists(SpecialPlayerGameTests.class, "assets/hbm/models/trinkets/leafia.obj")
                && GameTestHelpers.resourceExists(SpecialPlayerGameTests.class, "assets/hbm/textures/models/trinkets/leafia.png"),
                "Leafia's model or texture is missing");
        helper.assertTrue(BobbleFigure.body(BlockBobble.BobbleType.CLAWD.ordinal()) == BobbleFigure.Body.CLAWD
                && GameTestHelpers.resourceExists(SpecialPlayerGameTests.class, "assets/hbm/models/trinkets/clawd.obj")
                && GameTestHelpers.resourceExists(SpecialPlayerGameTests.class, "assets/hbm/textures/models/trinkets/clawd.png"),
                "Clawd's model or texture is missing");
        helper.succeed();
    }

    /** The datapack's Clawd row in a ticking assembly machine: Klaus, its parts and 1,000 mB of slop make one figure. */
    public static void clawdAssembly(GameTestHelper helper) {
        GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe("ass.bobbleclawd");
        helper.assertTrue(recipe != null && !recipe.isPooled(), "the Clawd assembler row did not load unpooled");
        BlockPos pos = new BlockPos(3, 1, 3);
        FoldedTestSupport.assemble(helper, ModMachineBlocks.machine_assembly_machine.get(), pos);
        TileEntityMachineAssemblyMachine assembler = GameTestHelpers.blockEntity(helper, pos, TileEntityMachineAssemblyMachine.class);
        assembler.assemblerModule.recipe = recipe.name;
        ItemStack[] inputs = {ItemDrive.stack(EnumDriveType.KLAUS, 1), new ItemStack(ModItems.ingot_polymer.get(), 4),
                ModIntermediateItems.circuit(EnumCircuitType.ADVANCED, 1), new ItemStack(Items.ORANGE_DYE, 2),
                new ItemStack(Items.BLACK_DYE)};
        for (int i = 0; i < inputs.length; i++) {
            assembler.inventory.setStackInSlot(TileEntityMachineAssemblyMachine.SLOT_INPUT_START + i, inputs[i]);
        }
        assembler.inputTank.setTankType(Fluids.SLOP);
        assembler.inputTank.setFill(1_000);
        assembler.power = 100_000;
        ItemStack clawd = ModDecoCrateBlocks.bobblehead_family.stack(BlockBobble.BobbleType.CLAWD);
        helper.succeedWhen(() -> {
            ItemStack out = assembler.inventory.getStackInSlot(TileEntityMachineAssemblyMachine.SLOT_OUTPUT);
            helper.assertTrue(ItemStacks.sameItemAndData(out, clawd), "no Clawd in the output yet: " + out);
            helper.assertTrue(assembler.inputTank.getFill() == 0, "the slop was not used: " + assembler.inputTank.getFill());
            for (int i = 0; i < inputs.length; i++) {
                helper.assertTrue(assembler.inventory.getStackInSlot(TileEntityMachineAssemblyMachine.SLOT_INPUT_START + i).isEmpty(),
                        "input " + i + " was not used");
            }
        });
    }

    private static Player player(GameTestHelper helper, String uuid) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setUUID(UUID.fromString(uuid));
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        return player;
    }
}
