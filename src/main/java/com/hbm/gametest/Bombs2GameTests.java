package com.hbm.gametest;

import com.hbm.inventory.container.ContainerNukeCustom;
import com.hbm.inventory.container.ContainerNukeFleija;
import com.hbm.inventory.container.ContainerNukeFstbmb;
import com.hbm.inventory.container.ContainerNukePrototype;
import com.hbm.inventory.container.ContainerNukeSolinium;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.items.ModBombs2Items;
import com.hbm.inventory.ModBombs2Menus;
import com.hbm.tileentity.bomb.TileEntityNukeBalefire;
import com.hbm.tileentity.bomb.TileEntityNukeCustom;
import com.hbm.tileentity.bomb.TileEntityNukeFleija;
import com.hbm.tileentity.bomb.TileEntityNukePrototype;
import com.hbm.tileentity.bomb.TileEntityNukeSolinium;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityCloudSolinium;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityBalefire;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.projectile.EntityFallingNuke;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemCell;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.items.machine.ItemBreedingRod.BreedingRodType;
import com.hbm.items.machine.ItemBreedingRod.RodSize;
import com.hbm.packet.HbmPackets;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Menu-opening and detonation-dispatch contracts for the special bombs ({@code com.hbm.bombs2}). */
public final class Bombs2GameTests {

    private Bombs2GameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite("hbm");
    public static final GameTests.Function CUSTOM =
            TESTS.function("bombs2_custom_nuke", 40, 8, () -> Bombs2GameTests::customNuke);
    public static final GameTests.Function FLEIJA =
            TESTS.function("bombs2_fleija", 40, 8, () -> Bombs2GameTests::fleija);
    public static final GameTests.Function SOLINIUM =
            TESTS.function("bombs2_solinium", 40, 8, () -> Bombs2GameTests::solinium);
    public static final GameTests.Function PROTOTYPE =
            TESTS.function("bombs2_prototype", 40, 8, () -> Bombs2GameTests::prototype);
    public static final GameTests.Function FSTBMB =
            TESTS.function("bombs2_balefire_bomb", 40, 8, () -> Bombs2GameTests::balefireBomb);
    public static final GameTests.Function THERMO =
            TESTS.function("bombs2_thermo", 40, 40, () -> Bombs2GameTests::thermo);
    public static final GameTests.Function FLOAT_EMP =
            TESTS.function("bombs2_float_emp", 40, 60, () -> Bombs2GameTests::floatAndEmp);
    public static final GameTests.Function FLAME_WAR =
            TESTS.function("bombs2_flame_war", 40, 40, () -> Bombs2GameTests::flameWar);

    public static void init() {}

    public static void customNuke(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBombs2Blocks.nuke_custom.get().defaultBlockState());
        TileEntityNukeCustom bomb = GameTestHelpers.blockEntity(helper, pos, TileEntityNukeCustom.class);
        helper.assertTrue(bomb.getContainerSize() == 27, "custom nuke must expose 27 slots");

        // Three TNT = 12 gates nuclear off; four TNT = 16 lets 15 nuclear through; nuke adj = 15 + 16/2.
        bomb.setItem(0, new ItemStack(Items.TNT, 3));
        bomb.setItem(1, new ItemStack(ModItems.ingot_u235.get()));
        bomb.updateEntity();
        helper.assertTrue(bomb.tnt == 12F && bomb.nuke == 0F, "TNT 12 must gate the nuclear stage to zero");
        bomb.setItem(0, new ItemStack(Items.TNT, 4));
        bomb.updateEntity();
        helper.assertTrue(bomb.tnt == 16F && bomb.nuke == 15F && bomb.getNukeAdj() == 23F && bomb.hydro == 0F,
                "custom nuke yield pass changed (expected tnt 16, nuke 15, adj 23)");
        bomb.setItem(2, new ItemStack(Items.REDSTONE, 2));
        bomb.updateEntity();
        helper.assertTrue(Math.abs(bomb.tnt - 16F * 2.1F) < 0.01F, "redstone must multiply TNT by value * count");

        assertMenuOpens(helper, pos, ModBombs2Menus.NUKE_CUSTOM.get(), ContainerNukeCustom.class);

        bomb.setItem(3, new ItemStack(ModItems.custom_fall.get()));
        IBomb.BombReturnCode result = ModBombs2Blocks.nuke_custom.get().explode(helper.getLevel(), helper.absolutePos(pos), null);
        List<EntityFallingNuke> falling = helper.getLevel().getEntitiesOfClass(EntityFallingNuke.class,
                new AABB(helper.absolutePos(pos)).inflate(2D));
        helper.assertTrue(result == IBomb.BombReturnCode.TRIGGERED && falling.size() == 1
                        && helper.getLevel().getBlockState(helper.absolutePos(pos)).isAir(),
                "a custom nuke with custom_fall must drop as a falling bomb");
        falling.forEach(Entity::discard);
        helper.succeed();
    }

    public static void fleija(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBombs2Blocks.nuke_fleija.get().defaultBlockState());
        TileEntityNukeFleija bomb = GameTestHelpers.blockEntity(helper, pos, TileEntityNukeFleija.class);
        helper.assertTrue(bomb.getContainerSize() == 11
                        && new ItemStack(ModBombs2Items.fleija_core.get()).getMaxStackSize() == 1,
                "F.L.E.I.J.A. must expose eleven slots of non-stackable parts");
        helper.assertTrue(ModBombs2Blocks.nuke_fleija.get().explode(helper.getLevel(), helper.absolutePos(pos), null)
                        == IBomb.BombReturnCode.ERROR_MISSING_COMPONENT,
                "an empty F.L.E.I.J.A. must report a missing component");

        bomb.setItem(0, new ItemStack(ModBombs2Items.fleija_igniter.get()));
        bomb.setItem(1, new ItemStack(ModBombs2Items.fleija_igniter.get()));
        for (int i = 2; i <= 4; i++) bomb.setItem(i, new ItemStack(ModBombs2Items.fleija_propellant.get()));
        for (int i = 5; i <= 9; i++) bomb.setItem(i, new ItemStack(ModBombs2Items.fleija_core.get()));
        bomb.setItem(10, new ItemStack(ModBombs2Items.fleija_propellant.get()));
        helper.assertTrue(bomb.hasPart(0) && bomb.hasPart(9) && !bomb.hasPart(10) && !bomb.isReady(),
                "a propellant in a core slot must not count as ready");
        bomb.setItem(10, new ItemStack(ModBombs2Items.fleija_core.get()));
        helper.assertTrue(bomb.isReady(), "eleven correct parts must arm the F.L.E.I.J.A.");

        assertMenuOpens(helper, pos, ModBombs2Menus.NUKE_FLEIJA.get(), ContainerNukeFleija.class);

        IBomb.BombReturnCode result = ModBombs2Blocks.nuke_fleija.get().explode(helper.getLevel(), helper.absolutePos(pos), null);
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                        && helper.getLevel().getBlockState(helper.absolutePos(pos)).isAir() && bomb.isEmpty(),
                "an armed F.L.E.I.J.A. must detonate, vanish and consume its parts");
        discardBlast(helper, pos);
        helper.succeed();
    }

    public static void solinium(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBombs2Blocks.nuke_solinium.get().defaultBlockState());
        TileEntityNukeSolinium bomb = GameTestHelpers.blockEntity(helper, pos, TileEntityNukeSolinium.class);
        helper.assertTrue(bomb.getContainerSize() == 9, "the Blue Rinse must expose nine slots");
        for (int slot : new int[]{0, 3, 5, 8}) bomb.setItem(slot, new ItemStack(ModBombs2Items.solinium_igniter.get()));
        for (int slot : new int[]{1, 2, 6, 7}) bomb.setItem(slot, new ItemStack(ModBombs2Items.solinium_propellant.get()));
        helper.assertTrue(!bomb.isReady(), "the Blue Rinse must not arm without its core");
        bomb.setItem(4, new ItemStack(ModBombs2Items.solinium_core.get()));
        helper.assertTrue(bomb.isReady(), "the Blue Rinse layout predicate changed");

        assertMenuOpens(helper, pos, ModBombs2Menus.NUKE_SOLINIUM.get(), ContainerNukeSolinium.class);

        IBomb.BombReturnCode result = ModBombs2Blocks.nuke_solinium.get().explode(helper.getLevel(), helper.absolutePos(pos), null);
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                        && helper.getLevel().getBlockState(helper.absolutePos(pos)).isAir(),
                "an armed Blue Rinse must detonate and vanish");
        discardBlast(helper, pos);
        helper.succeed();
    }

    public static void prototype(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBombs2Blocks.nuke_prototype.get().defaultBlockState());
        TileEntityNukePrototype bomb = GameTestHelpers.blockEntity(helper, pos, TileEntityNukePrototype.class);
        helper.assertTrue(bomb.getContainerSize() == 14, "the Prototype must expose fourteen slots");
        for (int slot : new int[]{0, 1, 12, 13}) bomb.setItem(slot, ItemCell.getFullCell(Fluids.SAS3));
        for (int slot : new int[]{2, 3, 10, 11}) bomb.setItem(slot, new ItemStack(ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.URANIUM)));
        for (int slot : new int[]{4, 5, 8, 9}) bomb.setItem(slot, new ItemStack(ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.LEAD)));
        bomb.setItem(6, new ItemStack(ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.NP237)));
        bomb.setItem(7, new ItemStack(ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.URANIUM)));
        helper.assertTrue(!bomb.isReady(), "a uranium rod in a neptunium slot must not arm the Prototype");
        bomb.setItem(7, new ItemStack(ModNuclearMiscItems.rod(RodSize.QUAD, BreedingRodType.NP237)));
        helper.assertTrue(bomb.isReady(), "the Prototype layout predicate changed");

        assertMenuOpens(helper, pos, ModBombs2Menus.NUKE_PROTOTYPE.get(), ContainerNukePrototype.class);

        IBomb.BombReturnCode result = ModBombs2Blocks.nuke_prototype.get().explode(helper.getLevel(), helper.absolutePos(pos), null);
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                        && helper.getLevel().getBlockState(helper.absolutePos(pos)).isAir(),
                "an armed Prototype must detonate and vanish");
        discardBlast(helper, pos);
        helper.succeed();
    }

    public static void balefireBomb(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBombs2Blocks.nuke_fstbmb.get().defaultBlockState());
        TileEntityNukeBalefire bomb = GameTestHelpers.blockEntity(helper, pos, TileEntityNukeBalefire.class);
        helper.assertTrue(bomb.timer == 18000 && !bomb.isLoaded(), "a fresh balefire bomb starts unloaded at 15:00");
        helper.assertTrue(ModBombs2Blocks.nuke_fstbmb.get().explode(helper.getLevel(), helper.absolutePos(pos), null)
                        == IBomb.BombReturnCode.ERROR_MISSING_COMPONENT,
                "an unloaded balefire bomb must report a missing component");

        bomb.setItem(0, new ItemStack(ModItems.egg_balefire.get()));
        bomb.setItem(1, new ItemStack(ModItems.battery_trixite.get()));
        helper.assertTrue(bomb.isLoaded() && bomb.getBattery() == 2, "egg + trixite battery must load the bomb");

        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = fixture.player();
            player.setPos(helper.absolutePos(pos).getCenter());
            openByClick(helper, pos, player);
            helper.assertTrue(player.containerMenu instanceof ContainerNukeFstbmb, "right-click must open the balefire bomb menu");
            GameTestHelpers.MenuOpening opening = GameTestHelpers.menuOpening(helper, fixture.outboundPackets());
            helper.assertTrue(opening.menuType() == ModBombs2Menus.NUKE_FSTBMB.get(), "wrong menu type for the balefire bomb");

            CompoundTag timer = new CompoundTag();
            timer.putInt("timer", 90);
            HbmPackets.handleControl(player, bomb.getBlockPos(), timer);
            helper.assertTrue(bomb.timer == 90 * 20 && "01".equals(bomb.getMinutes()) && "30".equals(bomb.getSeconds()),
                    "the timer control must set seconds * 20 and print 01:30");
            CompoundTag zero = new CompoundTag();
            zero.putInt("timer", 0);
            HbmPackets.handleControl(player, bomb.getBlockPos(), zero);
            helper.assertTrue(bomb.timer == 90 * 20, "a timer outside 1..999 seconds must be ignored");
            bomb.timer = 0;
            bomb.updateEntity();
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(pos)).is(ModBombs2Blocks.nuke_fstbmb.get()),
                    "a bomb that was never started must not go off when its timer reads zero");
            bomb.timer = 90 * 20;
            CompoundTag start = new CompoundTag();
            start.putBoolean("start", true);
            HbmPackets.handleControl(player, bomb.getBlockPos(), start);
            helper.assertTrue(bomb.started, "the start control must arm the countdown");
            bomb.updateEntity();
            helper.assertTrue(bomb.timer == 90 * 20 - 1, "a started bomb must count down one tick per tick");
        }

        IBomb.BombReturnCode result = ModBombs2Blocks.nuke_fstbmb.get().explode(helper.getLevel(), helper.absolutePos(pos), null);
        List<EntityBalefire> blasts = helper.getLevel().getEntitiesOfClass(EntityBalefire.class,
                new AABB(helper.absolutePos(pos)).inflate(2D));
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED && blasts.size() == 1
                        && blasts.get(0).destructionRange == 250
                        && helper.getLevel().getBlockState(helper.absolutePos(pos)).isAir(),
                "a loaded balefire bomb must spawn one 250-block balefire blast");
        blasts.forEach(Entity::discard);
        helper.getLevel().getEntitiesOfClass(EntityNukeTorex.class, new AABB(helper.absolutePos(pos)).inflate(3D))
                .forEach(Entity::discard);
        helper.succeed();
    }

    public static void thermo(GameTestHelper helper) {
        BlockPos endo = new BlockPos(4, 20, 4);
        // 14 down: inside the r=30 sweep but beyond the reach (~8.7) of the bomb's own strength-5 vanilla blast.
        BlockPos endoProbe = endo.below(14);
        helper.setBlock(endo, ModBombs2Blocks.therm_endo.get().defaultBlockState());
        helper.setBlock(endoProbe, Blocks.STONE.defaultBlockState());
        IBomb.BombReturnCode result = ModBombs2Blocks.therm_endo.get().explode(helper.getLevel(), helper.absolutePos(endo), null);
        BlockState frozen = helper.getLevel().getBlockState(helper.absolutePos(endoProbe));
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                        && !helper.getLevel().getBlockState(helper.absolutePos(endo)).is(ModBombs2Blocks.therm_endo.get()) // the fire-bearing TNT blast may leave fire here
                        && frozen.is(Blocks.PACKED_ICE),
                "the endothermic bomb must vanish and turn stone fourteen blocks down into packed ice (result=" + result + ", at bomb=" + helper.getLevel().getBlockState(helper.absolutePos(endo)) + ", probe=" + frozen + ")");

        BlockPos exo = new BlockPos(24, 20, 4);
        BlockPos exoProbe = exo.below(14);
        helper.setBlock(exo, ModBombs2Blocks.therm_exo.get().defaultBlockState());
        helper.setBlock(exoProbe, Blocks.STONE.defaultBlockState());
        result = ModBombs2Blocks.therm_exo.get().explode(helper.getLevel(), helper.absolutePos(exo), null);
        BlockState scorched = helper.getLevel().getBlockState(helper.absolutePos(exoProbe));
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                        && !helper.getLevel().getBlockState(helper.absolutePos(exo)).is(ModBombs2Blocks.therm_exo.get())
                        && scorched.is(Blocks.LAVA),
                "the exothermic bomb must vanish and turn stone eight blocks down into lava");
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(exoProbe), Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    public static void floatAndEmp(GameTestHelper helper) {
        BlockPos floating = new BlockPos(4, 2, 4);
        BlockPos lifted = floating.east(2);
        helper.setBlock(floating, ModBombs2Blocks.float_bomb.get().defaultBlockState());
        helper.setBlock(lifted, Blocks.STONE.defaultBlockState());
        IBomb.BombReturnCode result = ModBombs2Blocks.float_bomb.get().explode(helper.getLevel(), helper.absolutePos(floating), null);
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                        && helper.getLevel().getBlockState(helper.absolutePos(lifted)).isAir()
                        && helper.getLevel().getBlockState(helper.absolutePos(lifted).above(50)).is(Blocks.STONE),
                "the levitation bomb must lift a nearby block fifty up");
        helper.getLevel().setBlockAndUpdate(helper.absolutePos(lifted).above(50), Blocks.AIR.defaultBlockState());

        // The EMP device is not detonated here: its 50-block empBlast sphere reaches
        // into neighbouring test plots of the same batch and zeroes their machines' power (a real
        // cross-test interference), and a 1,000,000-block sweep is not a unit assertion. Its dispatch is
        // the same explode() path as the levitation bomb (BombFloat.emp), checked by hand in a world.
        helper.succeed();
    }

    public static void flameWar(GameTestHelper helper) {
        BlockPos pos = new BlockPos(20, 20, 20);
        helper.setBlock(pos, ModBombs2Blocks.flame_war.get().defaultBlockState());
        IBomb.BombReturnCode result = ModBombs2Blocks.flame_war.get().explode(helper.getLevel(), helper.absolutePos(pos), null);
        helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                        && helper.getLevel().getBlockState(helper.absolutePos(pos)).isAir(),
                "Flame War in a Box must detonate and vanish");
        helper.succeed();
    }

    // Helpers

    private static void assertMenuOpens(GameTestHelper helper, BlockPos pos, MenuType<?> type,
                                        Class<? extends AbstractContainerMenu> menuClass) {
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            ServerPlayer player = fixture.player();
            player.setPos(helper.absolutePos(pos).getCenter());
            openByClick(helper, pos, player);
            helper.assertTrue(menuClass.isInstance(player.containerMenu),
                    "right-click did not open " + menuClass.getSimpleName());
            GameTestHelpers.MenuOpening opening = GameTestHelpers.menuOpening(helper, fixture.outboundPackets());
            helper.assertTrue(opening.menuType() == type && helper.absolutePos(pos).equals(opening.position()),
                    "menu-opening packet carried the wrong type or position for " + menuClass.getSimpleName());
        }
    }

    /** The empty-hand block interaction, the path the playtest walks (AGENTS.md section 22). */
    private static void openByClick(GameTestHelper helper, BlockPos pos, ServerPlayer player) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        Compat.useBlockWithoutItem(state, helper.getLevel(), player, hit);
    }

    private static void discardBlast(GameTestHelper helper, BlockPos pos) {
        AABB box = new AABB(helper.absolutePos(pos)).inflate(3D);
        helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK3.class, box).forEach(Entity::discard);
        helper.getLevel().getEntitiesOfClass(EntityCloudFleija.class, box).forEach(Entity::discard);
        helper.getLevel().getEntitiesOfClass(EntityCloudSolinium.class, box).forEach(Entity::discard);
    }
}
