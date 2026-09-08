package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.blocks.turret.ModTurretBlocks;
import com.hbm.inventory.container.ContainerTurretBase;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory50;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactoryAccelerator;
import com.hbm.items.weapon.sedna.factory.XFactoryRocket;
import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.turret.TileEntityTurretBaseNT;
import com.hbm.tileentity.turret.TileEntityTurretChekhov;
import com.hbm.tileentity.turret.TileEntityTurretFritz;
import com.hbm.tileentity.turret.TileEntityTurretHIMARS;
import com.hbm.tileentity.turret.TileEntityTurretHoward;
import com.hbm.tileentity.turret.TileEntityTurretHowardDamaged;
import com.hbm.tileentity.turret.TileEntityTurretJeremy;
import com.hbm.tileentity.turret.TileEntityTurretMaxwell;
import com.hbm.tileentity.turret.TileEntityTurretRichard;
import com.hbm.tileentity.turret.TileEntityTurretSentry;
import com.hbm.tileentity.turret.TileEntityTurretSentryDamaged;
import com.hbm.tileentity.turret.TileEntityTurretTauon;
import dev.golem.core.gametest.GameTests;
import java.io.InputStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Behavioural contracts for the turret pad, its menu and the target loop. The turret is the first
 * user of {@link com.hbm.blocks.multiblock.BlockMultiblockCell}, so the folded 2x2 pad is asserted
 * here rather than assumed.
 */
public final class TurretGameTests {

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function STRUCTURE =
            TESTS.function("turret_structure", 60, 8, () -> TurretGameTests::structure);
    public static final GameTests.Function POWER_CONNECTOR =
            TESTS.function("turret_power_connector", 60, 8, () -> TurretGameTests::powerConnector);
    public static final GameTests.Function MENU =
            TESTS.function("turret_menu", 60, 8, () -> TurretGameTests::menu);
    public static final GameTests.Function TARGETING =
            TESTS.function("turret_targeting", 100, 16, () -> TurretGameTests::targeting);
    public static final GameTests.Function AMMO =
            TESTS.function("turret_ammo", 60, 8, () -> TurretGameTests::ammo);
    public static final GameTests.Function SENTRY =
            TESTS.function("turret_sentry", 60, 8, () -> TurretGameTests::sentry);
    public static final GameTests.Function HOWARD =
            TESTS.function("turret_howard", 100, 16, () -> TurretGameTests::howard);
    public static final GameTests.Function TAUON =
            TESTS.function("turret_tauon", 100, 16, () -> TurretGameTests::tauon);
    public static final GameTests.Function MAXWELL =
            TESTS.function("turret_maxwell", 100, 16, () -> TurretGameTests::maxwell);
    public static final GameTests.Function JEREMY =
            TESTS.function("turret_jeremy", 100, 24, () -> TurretGameTests::jeremy);
    public static final GameTests.Function RICHARD =
            TESTS.function("turret_richard", 100, 24, () -> TurretGameTests::richard);
    public static final GameTests.Function FRITZ =
            TESTS.function("turret_fritz", 100, 16, () -> TurretGameTests::fritz);
    public static final GameTests.Function LINGERING_FIRE =
            TESTS.function("turret_lingering_fire", 100, 8, () -> TurretGameTests::lingeringFire);
    public static final GameTests.Function HIMARS_STRUCTURE =
            TESTS.function("turret_himars_structure", 60, 16, () -> TurretGameTests::himarsStructure);
    public static final GameTests.Function HIMARS_POWER =
            TESTS.function("turret_himars_power", 60, 16, () -> TurretGameTests::himarsPower);
    public static final GameTests.Function HIMARS_RELOAD =
            TESTS.function("turret_himars_reload", 60, 16, () -> TurretGameTests::himarsReload);
    public static final GameTests.Function HIMARS_FIRE =
            TESTS.function("turret_himars_fire", 400, 16, () -> TurretGameTests::himarsFire);
    public static final GameTests.Function ASSETS =
            TESTS.function("turret_assets", 20, () -> TurretGameTests::assets);

    private TurretGameTests() {}

    public static void init() {}

    /** Places a turret the way a player does, and hands back the core position. */
    private static BlockPos place(GameTestHelper helper, BlockPos relative, BlockMultiblockCore block) {
        BlockPos clicked = helper.absolutePos(relative);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setYRot(0F);
        player.setPos(clicked.getX() - 6D, clicked.getY(), clicked.getZ() - 6D);
        helper.getLevel().setBlock(clicked, block.defaultBlockState(), 3);
        block.setPlacedBy(helper.getLevel(), clicked, block.defaultBlockState(), player, new ItemStack(block));
        return clicked.above(block.getHeightOffset()).relative(Direction.NORTH, -block.getOffset());
    }

    public static void structure(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_chekhov.get());
        BlockState coreState = helper.getLevel().getBlockState(core);

        helper.assertTrue(coreState.getBlock() == ModTurretBlocks.turret_chekhov.get(),
                "The clicked cell must become the turret core");
        helper.assertTrue(coreState.getValue(BlockMultiblockCore.FACING) == Direction.NORTH,
                "A turret placed at yaw 0 must face north");
        helper.assertTrue(helper.getLevel().getBlockEntity(core) instanceof TileEntityTurretChekhov,
                "The core must carry the turret block entity");

        BlockPos[] cells = {core.east(), core.south(), core.east().south()};
        for (BlockPos cell : cells) {
            helper.assertTrue(helper.getLevel().getBlockState(cell).getBlock() == ModTurretBlocks.turret_pad.get(),
                    "Pad cell missing at " + cell);
            helper.assertTrue(helper.getLevel().getBlockEntity(cell) == null,
                    "A folded pad cell must not carry a block entity");
            helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), cell)),
                    "Pad cell at " + cell + " does not resolve to its core");
        }

        helper.getLevel().removeBlock(cells[0], false);
        helper.assertTrue(helper.getLevel().getBlockState(core).isAir(),
                "Breaking one pad cell must tear the whole turret down");
        for (BlockPos cell : cells) {
            helper.assertTrue(helper.getLevel().getBlockState(cell).isAir(),
                    "Pad cell survived teardown at " + cell);
        }
        helper.succeed();
    }

    public static void powerConnector(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_chekhov.get());
        TileEntityTurretBaseNT turret = (TileEntityTurretBaseNT) helper.getLevel().getBlockEntity(core);
        DirPos[] ports = turret.getConPos();

        helper.assertTrue(ports.length == 12,
                "1.12.2 subscribes eight pad-edge cells and four underneath; got " + ports.length);

        boolean throughFoldedCell = false;
        for (DirPos port : ports) {
            ForgeDirection outward = port.getDir();
            BlockPos cell = port.getPos().offset(-outward.offsetX, -outward.offsetY, -outward.offsetZ);
            helper.assertTrue(Library.canConnect(helper.getLevel(), cell, outward.getOpposite()),
                    "A cable at " + port.getPos() + " cannot reach the turret through " + cell);
            if (!cell.equals(core)) throughFoldedCell = true;
        }
        helper.assertTrue(throughFoldedCell,
                "Every port resolved to the core, so the folded pad cells route no power at all");

        helper.assertTrue(!Library.canConnect(helper.getLevel(), core.above(4), ForgeDirection.DOWN),
                "Empty air must not read as a power connection");
        helper.assertTrue(!Library.canConnect(helper.getLevel(), core.east().east(), ForgeDirection.EAST),
                "A cell outside the 2x2 pad must not answer for the turret");
        helper.succeed();
    }

    public static void menu(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_chekhov.get());
        TileEntityTurretChekhov turret =
                (TileEntityTurretChekhov) helper.getLevel().getBlockEntity(core);
        var player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.setPos(core.getX() + 0.5D, core.getY(), core.getZ() + 0.5D);

        ContainerTurretBase menu = new ContainerTurretBase(1, player.getInventory(), turret);
        helper.assertTrue(menu.slots.size() == TileEntityTurretBaseNT.SLOT_COUNT + 36,
                "The turret menu must expose eleven machine slots plus the player inventory");
        helper.assertTrue(menu.getSlot(TileEntityTurretBaseNT.SLOT_CHIP).x == 98
                        && menu.getSlot(TileEntityTurretBaseNT.SLOT_CHIP).y == 27,
                "The chip slot moved off 1.12.2's painted position");
        helper.assertTrue(menu.getSlot(TileEntityTurretBaseNT.SLOT_BATTERY).x == 152
                        && menu.getSlot(TileEntityTurretBaseNT.SLOT_BATTERY).y == 99,
                "The battery slot moved off 1.12.2's painted position");
        helper.assertTrue(menu.stillValid(player), "A player standing on the turret must keep its menu open");

        int playerSlot = TileEntityTurretBaseNT.SLOT_COUNT;
        menu.getSlot(playerSlot).set(new ItemStack(ModMachineItems.turret_chip.get()));
        menu.quickMoveStack(player, playerSlot);
        helper.assertTrue(turret.getItem(TileEntityTurretBaseNT.SLOT_CHIP)
                        .is(ModMachineItems.turret_chip.get()),
                "Shift-clicking a chip must route it to the chip slot");

        menu.getSlot(playerSlot).set(new ItemStack(ModAmmoItems.bmg50_sp.get(), 8));
        menu.quickMoveStack(player, playerSlot);
        helper.assertTrue(turret.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST)
                        .is(ModAmmoItems.bmg50_sp.get()),
                "Shift-clicking a round must route it to the ammo block");
        helper.succeed();
    }

    public static void ammo(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_chekhov.get());
        TileEntityTurretChekhov turret = (TileEntityTurretChekhov) helper.getLevel().getBlockEntity(core);

        helper.assertTrue(turret.getFirstConfigLoaded() == null, "An empty turret must report no loaded round");

        turret.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST, new ItemStack(ModAmmoItems.bmg50_ap.get(), 4));
        BulletConfig loaded = turret.getFirstConfigLoaded();
        helper.assertTrue(loaded == XFactory50.bmg50_ap, "The turret did not resolve .50 AP from its slot");

        turret.consumeAmmo(loaded);
        helper.assertTrue(turret.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() == 3,
                "Firing must consume exactly one round");

        turret.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST, new ItemStack(ModAmmoItems.r556_sp.get(), 4));
        helper.assertTrue(turret.getFirstConfigLoaded() == null,
                "A .50 turret must not fire 5.56, and XFactory556mm's rounds are a different item");
        helper.assertTrue(XFactory556mm.r556_sp != null && XFactory50.bmg50_sp != null,
                "Both ammo families must be registered before a turret asks for them");
        helper.succeed();
    }

    public static void targeting(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_chekhov.get());
        TileEntityTurretChekhov turret = (TileEntityTurretChekhov) helper.getLevel().getBlockEntity(core);
        turret.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST, new ItemStack(ModAmmoItems.bmg50_sp.get(), 64));

        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(10, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);

        BlockState state = helper.getLevel().getBlockState(core);
        turret.setPower(turret.getMaxPower());

        tick(helper, turret, state, core, 1);
        helper.assertTrue(turret.target == null, "A switched-off turret must not acquire a target");

        turret.isOn = true;
        turret.setPower(turret.getMaxPower());
        tick(helper, turret, state, core, 2);
        helper.assertTrue(turret.target instanceof net.minecraft.world.entity.monster.Enemy,
                "A powered turret did not acquire the hostile in front of it");

        double before = turret.rotationYaw;
        tick(helper, turret, state, core, 20);
        helper.assertTrue(turret.rotationYaw != before, "The turret never turned towards its target");

        turret.setPower(0L);
        tick(helper, turret, state, core, 1);
        helper.assertTrue(turret.target == null, "An unpowered turret must drop its target");
        helper.succeed();
    }

    private static void tick(GameTestHelper helper, TileEntityTurretChekhov turret, BlockState state,
                             BlockPos core, int times) {
        for (int i = 0; i < times; i++) {
            TileEntityTurretChekhov.serverTick(helper.getLevel(), core, state, turret);
        }
    }

    public static void sentry(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.getLevel().setBlock(pos, ModTurretBlocks.turret_sentry.get().defaultBlockState(), 3);

        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof TileEntityTurretSentry,
                "The sentry must carry its block entity without any multiblock step");
        TileEntityTurretSentry sentry = (TileEntityTurretSentry) helper.getLevel().getBlockEntity(pos);

        DirPos[] ports = sentry.getConPos();
        helper.assertTrue(ports.length == 1 && ports[0].getPos().equals(pos.below()),
                "1.12.2's sentry subscribes the block beneath it and nothing else");
        helper.assertTrue(sentry.canConnect(ForgeDirection.DOWN) && !sentry.canConnect(ForgeDirection.NORTH),
                "The sentry takes power from below only");
        helper.assertTrue(!Library.canConnect(helper.getLevel(), pos, ForgeDirection.NORTH),
                "A cable beside the sentry must not connect");
        helper.assertTrue(Library.canConnect(helper.getLevel(), pos, ForgeDirection.UP),
                "A cable under the sentry must connect");

        helper.assertTrue(helper.getLevel().getBlockState(pos.east()).isAir()
                        && helper.getLevel().getBlockState(pos.south()).isAir(),
                "The sentry must not claim a pad");
        helper.assertTrue(!sentry.isOn() && !sentry.hasPower(),
                "A freshly placed sentry is off and unpowered");

        BlockPos wreck = helper.absolutePos(new BlockPos(6, 1, 2));
        helper.getLevel().setBlock(wreck, ModTurretBlocks.turret_sentry_damaged.get().defaultBlockState(), 3);
        TileEntityTurretSentryDamaged damaged =
                (TileEntityTurretSentryDamaged) helper.getLevel().getBlockEntity(wreck);
        helper.assertTrue(damaged != null && damaged.isOn() && damaged.hasPower(),
                "The wrecked sentry is always live: it needs neither switch nor power");

        var cow = helper.spawn(EntityType.COW, new BlockPos(6, 2, 8));
        helper.assertTrue(damaged.entityAcceptableTarget(cow),
                "The wrecked sentry shoots anything alive, cows included");
        helper.assertTrue(!sentry.entityAcceptableTarget(cow),
                "A working sentry leaves animals alone until it is told not to");
        helper.succeed();
    }

    public static void howard(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_howard.get());
        TileEntityTurretHoward howard = (TileEntityTurretHoward) helper.getLevel().getBlockEntity(core);

        helper.assertTrue(howard.getFirstConfigLoaded() == null, "An empty CIWS must report no belt");
        howard.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST, new ItemStack(ModItems.ammo_dgk.get(), 2));
        helper.assertTrue(howard.getFirstConfigLoaded() == XFactoryTurret.dgk_normal,
                "The CIWS did not resolve its belt from ammo_dgk");

        BlockState state = helper.getLevel().getBlockState(core);
        howard.isOn = true;
        howard.setPower(howard.getMaxPower());
        TileEntityTurretHoward.serverTick(helper.getLevel(), core, state, howard);
        helper.assertTrue(howard.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() == 1,
                "Loading a belt must consume exactly one round");

        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(14, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);
        float before = zombie.getHealth();

        boolean everAligned = false;
        for (int i = 0; i < 200 && zombie.getHealth() >= before; i++) {
            howard.setPower(howard.getMaxPower());
            TileEntityTurretHoward.serverTick(helper.getLevel(), core, state, howard);
            everAligned |= howard.aligned;
        }
        helper.assertTrue(howard.target != null, "The CIWS never acquired a target");
        helper.assertTrue(everAligned, "The CIWS tracked its target but never came on aim");
        helper.assertTrue(zombie.getHealth() < before,
                "The CIWS fires no projectile, so its flat hit roll is the only thing that can hurt a"
                        + " target; aligned=" + everAligned + " power=" + howard.getPower());

        BlockPos wreck = place(helper, new BlockPos(2, 1, 10), ModTurretBlocks.turret_howard_damaged.get());
        TileEntityTurretHowardDamaged methusalem =
                (TileEntityTurretHowardDamaged) helper.getLevel().getBlockEntity(wreck);
        helper.assertTrue(methusalem != null && methusalem.isOn() && methusalem.hasPower(),
                "Methusalem runs without a switch or a power feed");
        helper.assertTrue(methusalem.getFirstConfigLoaded() == null
                        && methusalem.entityAcceptableTarget(zombie),
                "Methusalem tracks anything alive and never needs a belt");
        helper.succeed();
    }

    public static void tauon(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_tauon.get());
        TileEntityTurretTauon tauon = (TileEntityTurretTauon) helper.getLevel().getBlockEntity(core);
        tauon.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST,
                new ItemStack(ModAmmoItems.tau_uranium.get(), 64));
        helper.assertTrue(tauon.getFirstConfigLoaded() == XFactoryAccelerator.tau_uranium,
                "The Tauon did not resolve its uranium slug");

        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(14, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);
        float before = zombie.getHealth();
        int rounds = tauon.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount();

        BlockState state = helper.getLevel().getBlockState(core);
        tauon.isOn = true;
        for (int i = 0; i < 200 && zombie.getHealth() >= before; i++) {
            tauon.setPower(tauon.getMaxPower());
            TileEntityTurretTauon.serverTick(helper.getLevel(), core, state, tauon);
        }

        helper.assertTrue(tauon.target != null, "The Tauon never acquired a target");
        helper.assertTrue(zombie.getHealth() < before,
                "The Tauon spawns no projectile, so it must damage its target directly");
        helper.assertTrue(tauon.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() < rounds,
                "Firing the beam must still consume a slug");
        helper.succeed();
    }

    public static void maxwell(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_maxwell.get());
        TileEntityTurretMaxwell maxwell = (TileEntityTurretMaxwell) helper.getLevel().getBlockEntity(core);
        BlockState state = helper.getLevel().getBlockState(core);

        long baseRange = (long) maxwell.getDetectorRange();
        long baseDraw = maxwell.getConsumption();
        maxwell.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST,
                new ItemStack(ModUpgradeItems.upgrade_effect_3.get()));
        maxwell.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST + 1,
                new ItemStack(ModUpgradeItems.upgrade_power_3.get()));
        maxwell.upgradeManager.checkSlots(maxwell.inventory,
                TileEntityTurretBaseNT.SLOT_AMMO_FIRST, TileEntityTurretBaseNT.SLOT_AMMO_LAST);
        helper.assertTrue(maxwell.getDetectorRange() > baseRange,
                "An EFFECT upgrade must widen the MASER's range");
        helper.assertTrue(maxwell.getConsumption() < baseDraw,
                "A POWER upgrade must lower the MASER's draw");

        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(14, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);
        float before = zombie.getHealth();

        maxwell.isOn = true;
        for (int i = 0; i < 200 && zombie.getHealth() >= before; i++) {
            maxwell.setPower(maxwell.getMaxPower());
            TileEntityTurretMaxwell.serverTick(helper.getLevel(), core, state, maxwell);
        }

        helper.assertTrue(maxwell.target != null, "The MASER never acquired a target");
        helper.assertTrue(zombie.getHealth() < before,
                "The MASER takes no ammunition, so power alone must be enough to hurt a target");
        helper.assertTrue(maxwell.getAmmoTypesForDisplay().size() == 15,
                "The MASER's slot panel lists the five upgrade chains it reacts to, three tiers each");
        helper.succeed();
    }

    public static void jeremy(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_jeremy.get());
        TileEntityTurretJeremy jeremy = (TileEntityTurretJeremy) helper.getLevel().getBlockEntity(core);

        jeremy.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST,
                new ItemStack(ModAmmoItems.shell_explosive.get(), 8));
        helper.assertTrue(jeremy.getFirstConfigLoaded() == XFactoryTurret.shell_explosive,
                "The Jeremy did not resolve its HE shell");
        helper.assertTrue(jeremy.getAmmoTypesForDisplay().size() == 5,
                "All five 240 mm loads must show in the ammo panel");

        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(24, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);
        int rounds = jeremy.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount();

        BlockState state = helper.getLevel().getBlockState(core);
        jeremy.isOn = true;
        for (int i = 0; i < 200 && jeremy.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() >= rounds; i++) {
            jeremy.setPower(jeremy.getMaxPower());
            TileEntityTurretJeremy.serverTick(helper.getLevel(), core, state, jeremy);
        }

        helper.assertTrue(jeremy.target != null, "The Jeremy never acquired a target");
        helper.assertTrue(jeremy.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() < rounds,
                "The Jeremy tracked its target but never fired a shell");
        helper.succeed();
    }

    public static void richard(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_richard.get());
        TileEntityTurretRichard richard = (TileEntityTurretRichard) helper.getLevel().getBlockEntity(core);
        BlockState state = helper.getLevel().getBlockState(core);

        helper.assertTrue(XFactoryRocket.rocket_ml.length == 5,
                "The launcher belt carries all five 84 mm warheads");
        richard.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST,
                new ItemStack(ModAmmoItems.rocket_heat.get(), 32));
        helper.assertTrue(richard.getFirstConfigLoaded() == XFactoryRocket.rocket_ml[1],
                "The launcher did not resolve its HEAT rocket");

        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(20, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);

        helper.assertTrue(richard.loaded == 0, "A launcher starts with an empty rack");
        int rounds = richard.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount();
        richard.isOn = true;

        for (int i = 0; i < 400
                && richard.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() >= rounds; i++) {
            richard.setPower(richard.getMaxPower());
            TileEntityTurretRichard.serverTick(helper.getLevel(), core, state, richard);
        }

        helper.assertTrue(richard.target != null, "The launcher never acquired a target");
        helper.assertTrue(richard.loaded > 0 && richard.loaded < 17,
                "The rack must refill to seventeen and then empty as it fires; it holds " + richard.loaded);
        helper.assertTrue(richard.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() < rounds,
                "The launcher tracked its target but never fired");

        var rockets = helper.getLevel().getEntitiesOfClass(
                com.hbm.entity.projectile.EntityBulletBaseMK4.class,
                new net.minecraft.world.phys.AABB(core).inflate(48D));
        helper.assertTrue(!rockets.isEmpty() && rockets.get(0).lockonTarget != null,
                "A launched rocket must carry the turret's lock");
        helper.succeed();
    }

    public static void fritz(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(2, 1, 2), ModTurretBlocks.turret_fritz.get());
        TileEntityTurretFritz fritz = (TileEntityTurretFritz) helper.getLevel().getBlockEntity(core);
        BlockState state = helper.getLevel().getBlockState(core);

        helper.assertTrue(fritz.turretTank() == fritz.tank, "The Fritz must show its tank in the GUI");
        helper.assertTrue(fritz.getFirstConfigLoaded() == null && fritz.getAmmoTypesForDisplay().size() > 1,
                "The Fritz takes no bullet config, but its panel lists the canister and every fuel");

        fritz.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST,
                new ItemStack(ModAmmoItems.flame_diesel.get(), 4));
        fritz.isOn = true;
        fritz.setPower(fritz.getMaxPower());
        TileEntityTurretFritz.serverTick(helper.getLevel(), core, state, fritz);
        helper.assertTrue(fritz.tank.getFill() == 1000,
                "A canister must empty one bucket into the tank; tank holds " + fritz.tank.getFill());
        helper.assertTrue(fritz.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() == 3,
                "Emptying a canister must consume exactly one");

        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(14, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);

        fritz.tank.setFill(fritz.tank.getMaxFill());
        int fuel = fritz.tank.getFill();
        fritz.setPower(fritz.getMaxPower());

        // Run on real ticks: the turret's own ticker keeps it powered through the block's ticker, and a
        // synchronous loop never advances the level, which several of the turret's timers key off.
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(fritz.target != null, "The Fritz never acquired a target");
            helper.assertTrue(fritz.tank.getFill() < fuel,
                    "Spraying must burn fuel, not ammunition; tank holds " + fritz.tank.getFill());
            helper.succeed();
        });
    }

    public static void lingeringFire(GameTestHelper helper) {
        var zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        zombie.setNoGravity(true);
        zombie.setNoAi(true);

        var fire = new com.hbm.entity.effect.EntityFireLingering(helper.getLevel())
                .setArea(6, 2).setDuration(300)
                .setFireType(com.hbm.entity.effect.EntityFireLingering.TYPE_DIESEL);
        fire.setPos(zombie.getX(), zombie.getY(), zombie.getZ());
        helper.getLevel().addFreshEntity(fire);

        fire.tick();
        helper.assertTrue(com.hbm.capability.HbmLivingProps.getFire(zombie) == 60,
                "Standing in diesel fire must set a sixty-tick burn");

        float before = zombie.getHealth();
        // The burn damages on a forty-tick stride keyed to the entity id, so this has to run over real
        // ticks rather than a synchronous loop: tickCount never advances inside one.
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(zombie.getHealth() < before,
                    "The burn must damage what it is set on; health " + zombie.getHealth() + " of " + before);
            helper.succeed();
        });
    }

    /**
     * The artillery emplacement: a 4x4 footprint two courses tall, thirty-one of whose thirty-two
     * cells are folded. Unlike the gun turrets the core is not the clicked cell — the block's offset
     * moves it one back — so this asserts where the core actually lands as well as what fills the rest.
     */
    public static void himarsStructure(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(7, 1, 4), ModTurretBlocks.turret_himars.get());
        BlockState coreState = helper.getLevel().getBlockState(core);

        helper.assertTrue(coreState.getBlock() == ModTurretBlocks.turret_himars.get(),
                "The core must land one block back from the clicked cell; found " + coreState.getBlock());
        helper.assertTrue(coreState.getValue(BlockMultiblockCore.FACING) == Direction.NORTH,
                "A gun placed at yaw 0 must face north");
        helper.assertTrue(helper.getLevel().getBlockEntity(core) instanceof TileEntityTurretHIMARS,
                "The core must carry the HIMARS block entity");

        int cells = 0;
        BlockPos corner = null;
        for (int y = 0; y <= 1; y++) {
            for (int x = -1; x <= 2; x++) {
                for (int z = -1; z <= 2; z++) {
                    BlockPos at = core.offset(x, y, z);
                    if (at.equals(core)) continue;
                    helper.assertTrue(
                            helper.getLevel().getBlockState(at).getBlock()
                                    == ModTurretBlocks.turret_emplacement.get(),
                            "Emplacement cell missing at " + at);
                    helper.assertTrue(helper.getLevel().getBlockEntity(at) == null,
                            "A folded emplacement cell must not carry a block entity");
                    helper.assertTrue(core.equals(MultiblockSurface.coreOfCell(helper.getLevel(), at)),
                            "Emplacement cell at " + at + " does not resolve to its core");
                    cells++;
                    corner = at;
                }
            }
        }
        helper.assertTrue(cells == 31, "A 4x4x2 emplacement folds 31 cells; found " + cells);

        helper.getLevel().removeBlock(corner, false);
        helper.assertTrue(helper.getLevel().getBlockState(core).isAir(),
                "Breaking one emplacement cell must tear the whole gun down");
        helper.succeed();
    }

    /**
     * Power reaches the gun through the wall of its emplacement, on all four sides at every facing.
     * 1.12.2 asks each neighbour about a world-constant face, which lines up at one facing only; this
     * asserts the geometric behaviour that replaced it.
     */
    public static void himarsPower(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(7, 1, 4), ModTurretBlocks.turret_himars.get());
        TileEntityTurretBaseNT turret = (TileEntityTurretBaseNT) helper.getLevel().getBlockEntity(core);
        DirPos[] ports = turret.getConPos();

        helper.assertTrue(ports.length == 32,
                "Four cells a side, four sides, two courses; got " + ports.length);

        for (DirPos port : ports) {
            ForgeDirection outward = port.getDir();
            BlockPos cell = port.getPos().offset(-outward.offsetX, -outward.offsetY, -outward.offsetZ);
            helper.assertTrue(Library.canConnect(helper.getLevel(), cell, outward.getOpposite()),
                    "A cable at " + port.getPos() + " cannot reach the gun through " + cell);
        }

        helper.assertTrue(!Library.canConnect(helper.getLevel(), core.above(4), ForgeDirection.DOWN),
                "Empty air must not read as a power connection");
        helper.succeed();
    }

    /**
     * The reload cycle. The crane has to run all the way out before a pod is drawn, and all the way
     * back before the gun can fire — which is why this walks it rather than asserting a single tick.
     */
    public static void himarsReload(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(7, 1, 4), ModTurretBlocks.turret_himars.get());
        TileEntityTurretHIMARS himars = (TileEntityTurretHIMARS) helper.getLevel().getBlockEntity(core);
        BlockState state = helper.getLevel().getBlockState(core);

        helper.assertTrue(himars.getAmmoTypesForDisplay().size() == 8,
                "The panel must list all eight rocket pods; got " + himars.getAmmoTypesForDisplay().size());
        helper.assertTrue(!himars.hasAmmo(), "A fresh gun must be empty");

        ItemStack pod = ModAmmoItems.ammo_himars.get()
                .of(com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType.SMALL_HE, 2);
        himars.setItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST, pod);
        himars.isOn = true;

        helper.assertTrue(himars.getSpareRocket()
                        == com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType.SMALL_HE.ordinal(),
                "The gun must read the variant off the stack, not the item");

        boolean craneExtended = false;
        for (int i = 0; i < 400 && !himars.hasAmmo(); i++) {
            himars.setPower(himars.getMaxPower());
            TileEntityTurretHIMARS.serverTick(helper.getLevel(), core, state, himars);
            if (himars.crane >= 1F) craneExtended = true;
        }

        helper.assertTrue(craneExtended, "The crane must run all the way out before a pod is drawn");
        helper.assertTrue(himars.hasAmmo(), "The gun never loaded; crane at " + himars.crane);
        helper.assertTrue(himars.typeLoaded
                        == com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType.SMALL_HE.ordinal(),
                "The loaded type must be the one that was in the slot; got " + himars.typeLoaded);
        helper.assertTrue(himars.ammo == 6, "A small pod holds six rockets; got " + himars.ammo);
        helper.assertTrue(himars.getItem(TileEntityTurretBaseNT.SLOT_AMMO_FIRST).getCount() == 1,
                "Loading must consume exactly one pod");
        helper.succeed();
    }

    /**
     * A fire mission, from the radar command down to the rocket. Manual mode is the only path a test
     * can drive: the gun's 250-block detector grace means nothing inside a test plot is ever a target.
     */
    public static void himarsFire(GameTestHelper helper) {
        BlockPos core = place(helper, new BlockPos(7, 1, 4), ModTurretBlocks.turret_himars.get());
        TileEntityTurretHIMARS himars = (TileEntityTurretHIMARS) helper.getLevel().getBlockEntity(core);

        himars.isOn = true;
        himars.setPower(himars.getMaxPower());
        himars.mode = TileEntityTurretHIMARS.MODE_MANUAL;
        himars.typeLoaded = com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType.SMALL.ordinal();
        himars.ammo = 6;
        himars.crane = 0F;

        // Straight up the +z axis so the yaw is already on target and only the pitch has to travel.
        double aimX = core.getX() + 0.5D;
        double aimZ = core.getZ() + 60.5D;
        helper.assertTrue(himars.sendCommandPosition(core.getX(), core.getY(), core.getZ() + 60),
                "The gun must accept a fire mission inside its range");
        helper.assertTrue(himars.queuedTargets() == 1,
                "An accepted mission must be queued; got " + himars.queuedTargets());

        helper.runAfterDelay(300, () -> {
            var rockets = helper.getLevel().getEntitiesOfClass(
                    com.hbm.entity.projectile.EntityArtilleryRocket.class,
                    new net.minecraft.world.phys.AABB(core).inflate(128D));

            helper.assertTrue(!rockets.isEmpty(),
                    "The gun never fired; pitch " + himars.rotationPitch + ", ammo " + himars.ammo
                            + ", queued " + himars.queuedTargets());
            helper.assertTrue(himars.ammo < 6, "Firing must spend a rocket; ammo " + himars.ammo);

            var rocket = rockets.get(0);
            helper.assertTrue(rocket.getTypeIndex()
                            == com.hbm.items.weapon.ItemAmmoHIMARS.HIMARSRocketType.SMALL.ordinal(),
                    "The rocket must carry the loaded warhead; got " + rocket.getTypeIndex());
            var aim = rocket.getLastTarget();
            helper.assertTrue(aim != null && Math.abs(aim.x - aimX) < 1D && Math.abs(aim.z - aimZ) < 1D,
                    "The rocket must be aimed at the mission, not the gun; aim " + aim);
            helper.succeed();
        });
    }

    public static void assets(GameTestHelper helper) {
        String[] required = {
                "assets/hbm/models/turrets/turret_chekhov.obj",
                "assets/hbm/textures/models/turrets/base.png",
                "assets/hbm/textures/models/turrets/base_friendly.png",
                "assets/hbm/textures/models/turrets/carriage.png",
                "assets/hbm/textures/models/turrets/carriage_friendly.png",
                "assets/hbm/textures/models/turrets/chekhov.png",
                "assets/hbm/textures/models/turrets/chekhov_barrels.png",
                "assets/hbm/textures/models/turrets/connector.png",
                "assets/hbm/textures/gui/weapon/gui_turret_base.png",
                "assets/hbm/textures/gui/weapon/gui_turret_friendly.png",
                "assets/hbm/sounds/turret/chekhov_fire.ogg",
                "assets/hbm/blockstates/turret_chekhov.json",
                "assets/hbm/blockstates/turret_friendly.json",
                "assets/hbm/blockstates/turret_pad.json",
                "assets/hbm/blockstates/turret_howard.json",
                "assets/hbm/blockstates/turret_howard_damaged.json",
                "assets/hbm/models/turrets/turret_howard_damaged.obj",
                "assets/hbm/textures/models/turrets/rusted/howard.png",
                "assets/hbm/textures/models/turrets/rusted/carriage_ciws.png",
                "assets/hbm/blockstates/turret_sentry.json",
                "assets/hbm/blockstates/turret_tauon.json",
                "assets/hbm/blockstates/turret_maxwell.json",
                "assets/hbm/blockstates/turret_jeremy.json",
                "assets/hbm/blockstates/turret_richard.json",
                "assets/hbm/blockstates/turret_fritz.json",
                "assets/hbm/models/turrets/turret_fritz.obj",
                "assets/hbm/textures/models/turrets/fritz.png",
                "assets/hbm/textures/gui/weapon/gui_turret_fritz.png",
                "assets/hbm/blockstates/turret_himars.json",
                "assets/hbm/blockstates/turret_emplacement.json",
                "assets/hbm/models/turrets/turret_himars.obj",
                "assets/hbm/models/turrets/turret_arty.obj",
                "assets/hbm/textures/models/turrets/himars.png",
                "assets/hbm/textures/models/turrets/arty.png",
                "assets/hbm/textures/gui/weapon/gui_turret_himars.png",
                "assets/hbm/textures/models/projectiles/himars_standard.png",
                "assets/hbm/textures/models/projectiles/himars_single.png",
                "assets/hbm/textures/models/projectiles/himars_standard_he.png",
                "assets/hbm/textures/models/projectiles/himars_standard_wp.png",
                "assets/hbm/textures/models/projectiles/himars_standard_tb.png",
                "assets/hbm/textures/models/projectiles/himars_single_tb.png",
                "assets/hbm/textures/models/projectiles/himars_standard_mini_nuke.png",
                "assets/hbm/textures/models/projectiles/himars_standard_lava.png",
                "assets/hbm/blockstates/block_slag.json",
                "assets/hbm/textures/blocks/block_slag.png",
                "assets/hbm/textures/blocks/block_slag_broken.png",
                "assets/hbm/sounds/weapon/flamethrowershoot.ogg",
                "assets/hbm/textures/items/ammo_standard_flame_diesel.png",
                "assets/hbm/models/turrets/turret_richard.obj",
                "assets/hbm/textures/models/turrets/richard.png",
                "assets/hbm/textures/gui/weapon/gui_turret_richard.png",
                "assets/hbm/sounds/turret/richard_fire.ogg",
                "assets/hbm/textures/items/ammo_standard_rocket_he.png",
                "assets/hbm/models/turrets/turret_jeremy.obj",
                "assets/hbm/textures/models/turrets/jeremy.png",
                "assets/hbm/textures/gui/weapon/gui_turret_cannon.png",
                "assets/hbm/sounds/turret/jeremy_fire1.ogg",
                "assets/hbm/sounds/turret/jeremy_reload.ogg",
                "assets/hbm/sounds/weapon/explosion_tiny1.ogg",
                "assets/hbm/textures/items/ammo_shell_w9.png",
                "assets/hbm/models/turrets/turret_microwave.obj",
                "assets/hbm/textures/models/turrets/maxwell.png",
                "assets/hbm/textures/gui/weapon/gui_turret_maxwell.png",
                "data/hbm/damage_type/microwave.json",
                "assets/hbm/models/turrets/turret_tauon.obj",
                "assets/hbm/textures/models/turrets/tauon.png",
                "assets/hbm/textures/gui/weapon/gui_turret_tau.png",
                "assets/hbm/sounds/weapon/taushoot.ogg",
                "data/hbm/damage_type/electricity.json",
                "assets/hbm/models/turrets/turret_howard.obj",
                "assets/hbm/textures/models/turrets/howard.png",
                "assets/hbm/textures/models/turrets/howard_barrels.png",
                "assets/hbm/textures/models/turrets/carriage_ciws.png",
                "assets/hbm/textures/gui/weapon/gui_turret_howard.png",
                "assets/hbm/sounds/turret/howard_fire.ogg",
                "assets/hbm/sounds/turret/howard_reload.ogg",
                "data/hbm/damage_type/shrapnel.json",
                "assets/hbm/blockstates/turret_sentry_damaged.json",
                "assets/hbm/models/turrets/turret_sentry.obj",
                "assets/hbm/textures/models/turrets/sentry.png",
                "assets/hbm/textures/models/turrets/sentry_damaged.png",
                "assets/hbm/textures/gui/weapon/gui_turret_sentry.png",
                "assets/hbm/sounds/turret/sentry_fire.ogg",
                "assets/hbm/sounds/turret/sentry_lockon.ogg",
                "assets/hbm/items/turret_chekhov.json",
                "assets/hbm/items/turret_friendly.json",
                "assets/hbm/items/turret_chip.json",
                "data/hbm/loot_table/blocks/turret_chekhov.json",
                "data/hbm/loot_table/blocks/turret_friendly.json",
        };
        for (String path : required) {
            helper.assertTrue(resourceExists(path), "Missing turret asset: " + path);
        }
        helper.succeed();
    }

    private static boolean resourceExists(String path) {
        try (InputStream stream = TurretGameTests.class.getClassLoader().getResourceAsStream(path)) {
            return stream != null;
        } catch (Exception e) {
            return false;
        }
    }
}
