package com.hbm.gametest;

import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.handler.JetpackHandler;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModJetpackDependencyItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.armor.ArmorBJJetpack;
import com.hbm.items.armor.JetpackFueledBase;
import com.hbm.items.armor.WingsMurk;
import com.hbm.items.gear.JetpackGlider;
import com.hbm.items.gear.JetpackRegular;
import com.hbm.items.special.ItemHot;
import com.hbm.items.special.ItemSteelDusted;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

/** Focused server-side contracts for the common jetpack and glider gameplay path. */
public final class JetpackGameTests {

    private JetpackGameTests() {}

    public static void registrationsAndFuelRates(GameTestHelper helper) {
        helper.assertTrue(BuiltInRegistries.ITEM.getKey(ModArmorItems.jetpack_fly.get())
                        .equals(Ids.of("hbm", "jetpack_fly")), "regular jetpack is not registered");
        helper.assertTrue(JetpackHandler.getMaxFuel(new ItemStack(ModArmorItems.jetpack_fly.get())) == 12_000,
                "regular jetpack capacity is not 12,000");
        helper.assertTrue(JetpackHandler.getMaxFuel(new ItemStack(ModArmorItems.jetpack_break.get())) == 12_000,
                "hover jetpack capacity is not 12,000");
        helper.assertTrue(JetpackHandler.getMaxFuel(new ItemStack(ModArmorItems.jetpack_vector.get())) == 16_000,
                "vector jetpack capacity is not 16,000");
        helper.assertTrue(JetpackHandler.getMaxFuel(new ItemStack(ModArmorItems.jetpack_boost.get())) == 32_000,
                "booster capacity is not 32,000");
        helper.assertTrue(JetpackHandler.getMaxFuel(new ItemStack(ModArmorItems.jetpack_glider.get())) == 20_000,
                "glider capacity is not 20,000");
        helper.assertTrue(JetpackHandler.getSpeed(com.hbm.inventory.fluid.Fluids.KEROSENE) == 0.3F
                        && JetpackHandler.getSpeed(com.hbm.inventory.fluid.Fluids.KEROSENE_REFORM) == 0.4F
                        && JetpackHandler.getSpeed(com.hbm.inventory.fluid.Fluids.NITAN) == 0.5F
                        && JetpackHandler.getSpeed(com.hbm.inventory.fluid.Fluids.BALEFIRE) == 1.5F,
                "glider fuel speeds changed");
        helper.succeed();
    }

    public static void gliderFuelAndDelayedState(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack gliderStack = new ItemStack(ModArmorItems.jetpack_glider.get());
        JetpackGlider glider = (JetpackGlider) gliderStack.getItem();
        player.setItemSlot(EquipmentSlot.CHEST, gliderStack);
        helper.assertFalse(JetpackHandler.hasGlider(player), "glider became directly wearable despite old module semantics");
        ItemStack chest = new ItemStack(ModArmorItems.t51_plate.get());
        player.setItemSlot(EquipmentSlot.CHEST, chest);

        helper.assertTrue(glider.tryFill(com.hbm.inventory.fluid.Fluids.KEROSENE, 1_000, gliderStack) == 0,
                "glider rejected kerosene");
        ArmorModHandler.applyMod(chest, gliderStack);
        helper.assertTrue(glider.getFill(gliderStack) == 1_000
                        && glider.getFirstFluidType(gliderStack) == com.hbm.inventory.fluid.Fluids.KEROSENE,
                "glider did not retain its fuel type and amount");
        helper.assertTrue(glider.tryFill(com.hbm.inventory.fluid.Fluids.NITAN, 1_000, gliderStack) == 1_000,
                "glider accepted a mixed fuel");

        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, true);
        JetpackHandler.JetpackInfo state = JetpackHandler.getState(player);
        helper.assertTrue(state.opening && !JetpackHandler.isJetpackActive(player),
                "glider activation did not honor the one-second opening delay");
        state.animTime = System.currentTimeMillis() - 1_001L;
        helper.assertTrue(JetpackHandler.isJetpackActive(player), "opened glider is not active");
        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK_HOVER, true);
        helper.assertTrue(JetpackHandler.isHovering(player), "glider hover toggle did not activate");
        helper.succeed();
    }

    public static void directAndModuleWearability(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack direct = new ItemStack(ModArmorItems.jetpack_fly.get());
        player.setItemSlot(EquipmentSlot.CHEST, direct);
        JetpackFueledBase.setFuel(direct, 500);
        helper.assertTrue(JetpackHandler.hasJetpack(player), "direct jetpack is not detected");

        ItemStack armor = new ItemStack(ModArmorItems.t51_plate.get());
        ItemStack module = new ItemStack(ModArmorItems.jetpack_vector.get());
        helper.assertTrue(ArmorModHandler.isApplicable(armor, module), "jetpack module rejected on chest armor");
        ArmorModHandler.applyMod(armor, module);
        player.setItemSlot(EquipmentSlot.CHEST, armor);
        ItemStack stored = ArmorModHandler.pryMod(armor, ArmorModHandler.plate_only);
        JetpackFueledBase.setFuel(stored, 777);
        ArmorModHandler.applyMod(armor, stored);
        helper.assertTrue(JetpackHandler.hasJetpack(player)
                        && JetpackHandler.getFuel(ArmorModHandler.pryMod(armor, ArmorModHandler.plate_only)) == 777,
                "jetpack module was not retained in the chestplate");
        var fillable = (com.hbm.api.fluidmk2.IFillableItem) armor.getItem();
        helper.assertTrue(fillable.tryFill(com.hbm.inventory.fluid.Fluids.KEROSENE, 223, armor) == 0
                        && JetpackHandler.getFuel(player) == 1_000,
                "machine-facing armor contract did not refill the attached ordinary jetpack");
        ArmorModHandler.applyMod(armor, new ItemStack(ModArmorItems.jetpack_glider.get()));
        helper.assertTrue(fillable.tryFill(com.hbm.inventory.fluid.Fluids.NITAN, 25_000, armor) == 5_000
                        && JetpackHandler.getFuel(player) == 20_000,
                "attached glider refill did not preserve capacity/remainder");
        ItemStack snapshot = armor.copy();
        helper.assertTrue(fillable.tryFill(com.hbm.inventory.fluid.Fluids.KEROSENE, 100, armor) == 100
                        && ItemStack.matches(snapshot, armor), "attached glider accepted mixed fuel");
        helper.assertTrue(fillable.tryEmpty(com.hbm.inventory.fluid.Fluids.NITAN, 1_000, armor) == 1_000
                        && JetpackHandler.getFuel(player) == 19_000,
                "attached glider drain did not write back its module");
        helper.succeed();
    }

    public static void reserveTankRefuelsRegularJetpack(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack jetpack = new ItemStack(ModArmorItems.jetpack_fly.get());
        player.setItemSlot(EquipmentSlot.CHEST, jetpack);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModMedicineItems.jetpack_tank.get()));
        ModMedicineItems.jetpack_tank.get().use(player.level(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(JetpackFueledBase.getFuel(jetpack) == 1_000,
                "reserve tank did not add 1,000mB of kerosene");
        helper.assertTrue(player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "reserve tank was not consumed after a successful refill");
        helper.succeed();
    }

    public static void dependencyItemsAndHotAnvil(GameTestHelper helper) {
        ItemStack left = ItemSteelDusted.stack(9);
        ItemStack right = ItemSteelDusted.stack(9);
        ItemHot.heatUp(left);
        ItemHot.heatUp(right);
        var chainsteel = AnvilRecipes.getSmithing().stream()
                .filter(recipe -> recipe.getSimpleOutput().is(ModJetpackDependencyItems.ingot_chainsteel.get()))
                .findFirst().orElse(null);
        helper.assertTrue(chainsteel != null && chainsteel.matches(left, right),
                "chainsteel hot anvil recipe is missing or rejected hot stage 9");
        ItemStack output = chainsteel == null ? ItemStack.EMPTY : chainsteel.getOutput(left, right);
        helper.assertTrue(output.is(ModJetpackDependencyItems.ingot_chainsteel.get())
                        && ItemHot.getHeat(output) == 1D,
                "chainsteel anvil output did not retain full averaged heat");

        com.hbm.items.weapon.ItemCustomMissilePart thruster = com.hbm.items.ModCustomMissileItems.mp_thruster_10_xenon.get();
        helper.assertTrue(thruster.fuelType() == com.hbm.items.weapon.ItemCustomMissilePart.FuelType.XENON
                        && thruster.consumption() == 1F && (Float) thruster.attributes[2] == 1.5F
                        && thruster.health == 5F && thruster.top == com.hbm.items.weapon.ItemCustomMissilePart.PartSize.SIZE_10,
                "xenon size-10 thruster attributes changed");
        helper.succeed();
    }

    public static void inputPayloadIsBounded(GameTestHelper helper) {
        com.hbm.packet.JetpackInputPayload input = new com.hbm.packet.JetpackInputPayload(
                Float.NaN, Float.POSITIVE_INFINITY, true, false, true);
        helper.assertTrue(input.forward() == 0F && input.strafe() == 0F,
                "non-finite flight input was not rejected");
        input = new com.hbm.packet.JetpackInputPayload(-4F, 4F, false, true, false);
        helper.assertTrue(input.forward() == -1F && input.strafe() == 1F,
                "flight input was not clamped to [-1,1]");
        helper.succeed();
    }

    public static void equivalentClientSnapshotKeepsSource(GameTestHelper helper) {
        ItemStack original = new ItemStack(ModArmorItems.jetpack_glider.get());
        JetpackGlider glider = (JetpackGlider) original.getItem();
        glider.tryFill(com.hbm.inventory.fluid.Fluids.KEROSENE, 123, original);
        ItemStack snapshot = original.copy();
        helper.assertTrue(JetpackHandler.isEquivalentClientSource(original, snapshot),
                "equivalent client stack snapshot was treated as a source swap");
        helper.assertFalse(JetpackHandler.isEquivalentClientSource(original,
                        new ItemStack(ModArmorItems.jetpack_fly.get())),
                "different jetpack kind was treated as an equivalent client source");
        ItemStack armor = new ItemStack(ModArmorItems.t51_plate.get());
        ArmorModHandler.applyMod(armor, original);
        var contents = ModDataComponents.ARMOR_MODS.get(armor).orElseThrow();
        var ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,
                helper.getLevel().registryAccess());
        var encoded = dev.golem.core.data.ItemContents.CODEC.encodeStart(ops, contents).result().orElseThrow();
        var decoded = dev.golem.core.data.ItemContents.CODEC.parse(ops, encoded).result().orElseThrow();
        ItemStack received = armor.copy();
        ModDataComponents.ARMOR_MODS.set(received, decoded);
        helper.assertTrue(ItemStack.matches(armor, received) && contents.hashCode() == decoded.hashCode(),
                "identical decoded armor modules look like an equipment change");
        var exposed = decoded.copies();
        glider.tryEmpty(com.hbm.inventory.fluid.Fluids.KEROSENE, 1, exposed.get(ArmorModHandler.plate_only));
        helper.assertTrue(ItemStack.matches(armor, received), "component exposed mutable nested stacks");
        ArmorModHandler.applyMod(received, exposed.get(ArmorModHandler.plate_only));
        helper.assertFalse(ItemStack.matches(armor, received), "actual module fuel change was hidden by equality");
        helper.succeed();
    }

    public static void ordinaryFuelCadenceAndRelease(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = new ItemStack(ModArmorItems.jetpack_fly.get());
        player.setItemSlot(EquipmentSlot.CHEST, stack);
        JetpackFueledBase.setFuel(stack, 10);
        JetpackRegular jetpack = (JetpackRegular) stack.getItem();
        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, true);

        player.tickCount = 1;
        jetpack.armorTick(player.level(), player, stack);
        helper.assertTrue(JetpackFueledBase.getFuel(stack) == 10,
                "regular jetpack drained before its five-tick cadence");
        player.tickCount = 5;
        player.setDeltaMovement(Vec3.ZERO);
        jetpack.armorTick(player.level(), player, stack);
        helper.assertTrue(JetpackFueledBase.getFuel(stack) == 9,
                "regular jetpack did not drain one unit on tick five");
        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, false);
        player.setDeltaMovement(Vec3.ZERO);
        player.tickCount = 10;
        jetpack.armorTick(player.level(), player, stack);
        helper.assertTrue(JetpackFueledBase.getFuel(stack) == 9
                        && player.getDeltaMovement().equals(Vec3.ZERO),
                "released regular jetpack continued flying or draining");
        helper.succeed();
    }

    public static void gliderLockoutAndFlyTimeRestart(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = new ItemStack(ModArmorItems.jetpack_glider.get());
        JetpackGlider glider = (JetpackGlider) stack.getItem();
        glider.tryFill(com.hbm.inventory.fluid.Fluids.KEROSENE, 1_000, stack);
        ItemStack chest = new ItemStack(ModArmorItems.t51_plate.get());
        ArmorModHandler.applyMod(chest, stack);
        player.setItemSlot(EquipmentSlot.CHEST, chest);

        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, true);
        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, false);
        JetpackHandler.JetpackInfo state = JetpackHandler.getState(player);
        helper.assertTrue(state.opening && state.jetpackFlyTime == 0,
                "opening a glider did not initialize fly time");

        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, true);
        helper.assertTrue(state.opening, "glider toggled during the one-second animation lockout");
        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, false);
        state.animTime = System.currentTimeMillis() - 1_001L;
        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, true);
        helper.assertFalse(state.opening, "glider did not close after the lockout elapsed");
        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, false);
        state.animTime = System.currentTimeMillis() - 1_001L;
        JetpackHandler.handleKeybind(player, EnumKeybind.JETPACK, true);
        helper.assertTrue(state.opening && state.jetpackFlyTime == 0,
                "glider fly time did not restart after reopening");
        player.setOnGround(false);
        JetpackHandler.tickGlider(player);
        helper.assertTrue(state.jetpackFlyTime == -1, "opening delay must not animate powered flight");
        state.animTime = System.currentTimeMillis() - 1_001L;
        JetpackHandler.tickGlider(player);
        helper.assertTrue(state.jetpackFlyTime == 1, "first glide stayed stuck after its opening delay");
        player.setOnGround(true);
        JetpackHandler.tickGlider(player);
        helper.assertTrue(state.jetpackFlyTime == -1, "grounded glider retained its flying pose");
        player.setOnGround(false);
        JetpackHandler.tickGlider(player);
        helper.assertTrue(state.jetpackFlyTime == 1, "taking off again required a hover toggle");
        JetpackHandler.toggleHoverState(player);
        JetpackHandler.tickGlider(player);
        helper.assertTrue(state.jetpackFlyTime == -1, "hover retained its glide timer");
        JetpackHandler.toggleHoverState(player);
        JetpackHandler.tickGlider(player);
        helper.assertTrue(state.jetpackFlyTime == 1, "hover-to-glide timer did not restart");
        helper.succeed();
    }

    public static void sourceSwapLoadsNewGliderState(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack first = new ItemStack(ModArmorItems.jetpack_glider.get());
        CompoundTag firstState = new CompoundTag();
        firstState.putBoolean("opening", true);
        ModDataComponents.JETPACK_STATE.set(first, firstState);
        ItemStack firstChest = new ItemStack(ModArmorItems.t51_plate.get());
        ArmorModHandler.applyMod(firstChest, first);
        player.setItemSlot(EquipmentSlot.CHEST, firstChest);
        helper.assertTrue(JetpackHandler.getState(player).opening, "first glider state was not loaded");

        ItemStack second = new ItemStack(ModArmorItems.jetpack_glider.get());
        CompoundTag secondState = new CompoundTag();
        secondState.putBoolean("opening", false);
        ModDataComponents.JETPACK_STATE.set(second, secondState);
        ItemStack secondChest = new ItemStack(ModArmorItems.t51_plate.get());
        ArmorModHandler.applyMod(secondChest, second);
        player.setItemSlot(EquipmentSlot.CHEST, secondChest);
        helper.assertFalse(JetpackHandler.getState(player).opening,
                "swapping gliders retained the old item's opening state");
        helper.succeed();
    }

    public static void gliderUsesReportedInputForMotion(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack gliderStack = new ItemStack(ModArmorItems.jetpack_glider.get());
        JetpackGlider glider = (JetpackGlider) gliderStack.getItem();
        glider.tryFill(com.hbm.inventory.fluid.Fluids.KEROSENE, 1_000, gliderStack);
        ItemStack chest = new ItemStack(ModArmorItems.t51_plate.get());
        ArmorModHandler.applyMod(chest, gliderStack);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        JetpackHandler.JetpackInfo state = JetpackHandler.getState(player);
        state.opening = true;
        state.animTime = System.currentTimeMillis() - 1_001L;
        player.setOnGround(false);
        player.setXRot(0F);
        player.setYRot(0F);
        player.setYHeadRot(0F);
        player.setDeltaMovement(Vec3.ZERO);

        JetpackHandler.setInput(player, 0F, 0F, true, false, false);
        JetpackHandler.tickGlider(player);
        Vec3 motion = player.getDeltaMovement();
        helper.assertTrue(Math.abs(motion.y) < 1.0E-6D
                        && Math.abs(Math.sqrt(motion.x * motion.x + motion.z * motion.z) - 0.15D) < 1.0E-6D,
                "glider ignored reported jump input and produced " + motion);
        helper.succeed();
    }

    public static void bjGliderMotionIncludesVerticalBrake(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModArmorItems.bj_helmet.get()));
        ItemStack chest = new ItemStack(ModArmorItems.bj_plate_jetpack.get());
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModArmorItems.bj_legs.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModArmorItems.bj_boots.get()));
        player.setOnGround(false);
        player.setShiftKeyDown(true);
        player.setXRot(0F);
        player.setYRot(0F);
        player.setYHeadRot(0F);
        player.setDeltaMovement(0D, -1D, 0D);

        ((ArmorBJJetpack) chest.getItem()).armorTick(player.level(), player, chest);
        Vec3 motion = player.getDeltaMovement();
        helper.assertTrue(Math.abs(motion.y + 0.6D) < 1.0E-6D && Math.abs(motion.z - 0.4D) < 1.0E-6D,
                "BJ glider brake produced " + motion + ", expected y=-0.6,z=0.4");
        helper.succeed();
    }

    public static void murkStrafeUsesLegacyPositiveRotation(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack wings = new ItemStack(ModArmorItems.wings_murk.get());
        player.setItemSlot(EquipmentSlot.CHEST, wings);
        player.setOnGround(false);
        player.setXRot(0F);
        player.setYRot(0F);
        player.setDeltaMovement(Vec3.ZERO);
        JetpackHandler.setInput(player, 0F, 1F, false, false, false);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 horizontal = new Vec3(look.x, 0, look.z).normalize();
        Vec3 expected = new Vec3(horizontal.z, 0, -horizontal.x).scale(0.15D);

        ((WingsMurk) wings.getItem()).armorTick(player.level(), player, wings);
        Vec3 motion = player.getDeltaMovement();
        helper.assertTrue(Math.abs(motion.x - expected.x) < 1.0E-6D
                        && Math.abs(motion.z - expected.z) < 1.0E-6D,
                "positive strafe rotated to " + motion + ", expected legacy +90-degree rotation " + expected);
        helper.succeed();
    }

    public static void stateScopeCleansOnLevelRemoval(GameTestHelper helper) {
        Player player = player(helper);
        JetpackHandler.JetpackInfo state = JetpackHandler.getState(player);
        helper.assertTrue(JetpackHandler.getState(player.level(), player.getUUID()) == state,
                "level-scoped state lookup did not resolve the player's state");
        JetpackHandler.clear(player.level());
        helper.assertTrue(JetpackHandler.getState(player.level(), player.getUUID()) == null,
                "level cleanup retained jetpack state");
        helper.succeed();
    }

    public static void smokeSessionRestoresOwnedState(GameTestHelper helper) {
        try (dev.golem.core.gametest.GameTestHelpers.MockServerPlayer fixture =
                     dev.golem.core.gametest.GameTestHelpers.mockServerPlayer(helper)) {
            Player player = fixture.player();
            Vec3 originalMotion = new Vec3(0.1D, 0.2D, 0.3D);
            player.setDeltaMovement(originalMotion);
            boolean originalOnGround = player.onGround();

            JetpackHandler.startSmoke(fixture.player());
            helper.assertTrue(JetpackHandler.hasGlider(player)
                            && player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof com.hbm.items.gear.ArmorFSB,
                    "smoke session did not install its owned glider module");
            helper.assertTrue(player.isNoGravity(), "smoke session did not apply its temporary no-gravity state");
            helper.assertTrue(JetpackHandler.stopSmoke(fixture.player()), "smoke session did not stop explicitly");
            helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                            && !player.isNoGravity() && player.onGround() == originalOnGround
                            && player.getDeltaMovement().equals(originalMotion)
                            && JetpackHandler.getState(player.level(), player.getUUID()) == null,
                    "smoke session did not restore the command-owned state");
        }
        helper.succeed();
    }

    private static Player player(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        var origin = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        return player;
    }
}
