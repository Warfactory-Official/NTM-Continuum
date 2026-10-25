package com.hbm.gametest;

import com.hbm.api.block.IToolable.ToolType;
import com.hbm.blocks.bomb.BlockTNTBase;
import com.hbm.blocks.ModBombs2Blocks;
import com.hbm.blocks.ModExplosivesBlocks;
import com.hbm.blocks.bomb.ItemNukeBlock;
import com.hbm.explosion.BlastInfo;
import com.hbm.blocks.bomb.ModBombBlocks;
import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.explosive.EntityPrimedExplosive;
import com.hbm.entity.ModEntities;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.container.ContainerNukeMike;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.items.ModBombItems;
import com.hbm.tileentity.bomb.TileEntityNukeMike;
import com.mojang.serialization.Codec;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.registry.Entities;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Focused contracts for Ivy Mike and the shared conventional primed-explosive stack. */
public final class ExplosiveContentGameTests {

    private ExplosiveContentGameTests() {}

    /** Feature-local suite; loader integration forwards it without modifying the shared test catalogue. */
    public static final GameTests.Suite TESTS = GameTests.suite("hbm");
    public static final GameTests.Function MIKE_SLOTS_MENU =
            TESTS.function("explosive_mike_slots_menu", 40, 64,
                    () -> ExplosiveContentGameTests::mikeSlotsAndMenu);
    public static final GameTests.Function MIKE_RADII =
            TESTS.function("explosive_mike_radii", 40, 64,
                    () -> ExplosiveContentGameTests::mikeEmptyAndFilledRadii);
    public static final GameTests.Function MIKE_REDSTONE =
            TESTS.function("explosive_mike_redstone", 40, 64,
                    () -> ExplosiveContentGameTests::mikeRedstoneArming);
    public static final GameTests.Function PRIMED_PERSISTENCE =
            TESTS.function("explosive_primed_persistence", 40, 64,
                    () -> ExplosiveContentGameTests::primedFusePersistence);
    public static final GameTests.Function IGNITION =
            TESTS.function("explosive_redstone_tool_ignition", 40, 64,
                    () -> ExplosiveContentGameTests::redstoneAndToolIgnition);
    public static final GameTests.Function CHAIN_STRENGTHS =
            TESTS.function("explosive_chain_strengths", 40, 64,
                    () -> ExplosiveContentGameTests::chainAndStrengths);
    public static final GameTests.Function PRIMED_MOTION =
            TESTS.function("explosive_primed_vanilla_motion", 40,
                    () -> ExplosiveContentGameTests::primedMotionMatchesVanilla);
    public static final GameTests.Function NUCLEAR_TOOLTIP_FORMULAS =
            TESTS.function("explosive_nuclear_tooltip_formulas", 20,
                    () -> ExplosiveContentGameTests::nuclearTooltipFormulas);
    public static final GameTests.Function VNT_ORDER =
            TESTS.function("explosive_vnt_exposure_order", 20, () -> ExplosiveContentGameTests::vntExposureOrder);

    public static void init() {}

    public static void mikeSlotsAndMenu(GameTestHelper helper) {
        TileEntityNukeMike mike = placeMike(helper, new BlockPos(2, 2, 2));
        helper.assertTrue(mike.getContainerSize() == 8
                        && new ItemStack(ModBombItems.explosive_lenses.get()).getMaxStackSize() == 1,
                "Mike must expose eight slots whose component items are non-stackable");
        helper.assertTrue(mike.isItemValidForSlot(0, new ItemStack(ModBombItems.explosive_lenses.get()))
                        && !mike.isItemValidForSlot(0, new ItemStack(ModBombItems.man_core.get()))
                        && mike.isItemValidForSlot(4, new ItemStack(ModBombItems.man_core.get()))
                        && mike.isItemValidForSlot(5, new ItemStack(ModBombItems.mike_core.get()))
                        && mike.isItemValidForSlot(6, new ItemStack(ModBombItems.mike_deut.get()))
                        && mike.isItemValidForSlot(7, new ItemStack(ModBombItems.mike_cooling_unit.get())),
                "Mike's ordered slot predicates changed");

        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(ModBombItems.mike_cooling_unit.get()));
        ContainerNukeMike menu = new ContainerNukeMike(0, player.getInventory(), mike);
        menu.quickMoveStack(player, 35);
        helper.assertTrue(mike.getItem(7).is(ModBombItems.mike_cooling_unit.get()),
                "Mike shift-click did not route its cooling unit to slot 7");
        helper.succeed();
    }

    public static void mikeEmptyAndFilledRadii(GameTestHelper helper) {
        TileEntityNukeMike testDevice = placeMike(helper, new BlockPos(2, 2, 6));
        BlockPos testOrigin = testDevice.getBlockPos();
        BlockPos filledOrigin = helper.absolutePos(new BlockPos(8, 2, 6));
        try {
            fillPrimary(testDevice);
            UUID owner = UUID.randomUUID();
            testDevice.placerID = owner;
            IBomb.BombReturnCode result = ModBombBlocks.nuke_mike.get().explode(
                    helper.getLevel(), testOrigin, null, null);
            EntityNukeExplosionMK5 testBlast = onlyBlast(helper, testOrigin);
            helper.assertTrue(result == IBomb.BombReturnCode.DETONATED
                            && testBlast.getRadius() == BombConfig.manRadius
                            && owner.equals(testBlast.detonator),
                    "under-filled Mike did not use the old manRadius/placer path");

            TileEntityNukeMike filled = placeMike(helper, new BlockPos(8, 2, 6));
            fillPrimary(filled);
            filled.setItem(5, new ItemStack(ModBombItems.mike_core.get()));
            filled.setItem(6, new ItemStack(ModBombItems.mike_deut.get()));
            filled.setItem(7, new ItemStack(ModBombItems.mike_cooling_unit.get()));
            ModBombBlocks.nuke_mike.get().explode(helper.getLevel(), filledOrigin, null);
            EntityNukeExplosionMK5 fullBlast = onlyBlast(helper, filledOrigin);
            helper.assertTrue(fullBlast.getRadius() == BombConfig.mikeRadius && filled.isEmpty(),
                    "fully assembled Mike did not consume all parts at mikeRadius");
            helper.succeed();
        } finally {
            discardNuclear(helper, testOrigin);
            discardNuclear(helper, filledOrigin);
        }
    }

    public static void mikeRedstoneArming(GameTestHelper helper) {
        TileEntityNukeMike mike = placeMike(helper, new BlockPos(2, 2, 10));
        BlockPos origin = mike.getBlockPos();
        try {
            fillPrimary(mike);
            BlockPos power = origin.east();
            helper.getLevel().setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            helper.getLevel().updateNeighborsAt(power, Blocks.REDSTONE_BLOCK);
            helper.assertTrue(helper.getLevel().getBlockState(origin).isAir()
                            && onlyBlast(helper, origin).getRadius() == BombConfig.manRadius,
                    "redstone did not arm the under-filled Mike through its test-device path");
            helper.succeed();
        } finally {
            discardNuclear(helper, origin);
        }
    }

    public static void primedFusePersistence(GameTestHelper helper) {
        Player owner = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        PersistenceProbe source = new PersistenceProbe(helper, owner,
                ModBombBlocks.semtex.get().defaultBlockState().setValue(BlockTNTBase.ARMED, true));
        source.setFuse(37);
        source.detonateOnCollision = true;
        Map<String, Object> saved = new HashMap<>();
        source.write(new MapWriter(saved));
        PersistenceProbe copy = new PersistenceProbe(helper);
        copy.read(new MapReader(saved));
        // This verifies repairs to 1.12.2's mismatched fuse key, ineffective state load,
        // and omitted owner/collision fields rather than claiming persistence parity with those bugs.
        helper.assertTrue(copy.getFuse() == 37
                        && copy.getExplosiveState().is(ModBombBlocks.semtex.get())
                        && copy.getExplosiveState().getValue(BlockTNTBase.ARMED)
                        && copy.detonateOnCollision && owner.getUUID().equals(copy.getOwnerId()),
                "primed fuse, explosive kind, state, collision flag, or owner did not survive save/load");
        helper.succeed();
    }

    public static void redstoneAndToolIgnition(GameTestHelper helper) {
        BlockPos redstoneRelative = new BlockPos(2, 2, 18);
        helper.setBlock(redstoneRelative, ModBombBlocks.tnt.get().defaultBlockState());
        BlockPos redstonePos = helper.absolutePos(redstoneRelative);
        BlockPos power = redstonePos.east();
        helper.getLevel().setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        helper.getLevel().updateNeighborsAt(power, Blocks.REDSTONE_BLOCK);
        EntityPrimedExplosive redstone = onlyPrimed(helper, redstonePos);
        helper.assertTrue(redstone.getFuse() <= 80 && redstone.getExplosiveState().is(ModBombBlocks.tnt.get()),
                "redstone did not create the HBM primed TNT entity");
        redstone.discard();

        BlockPos toolRelative = new BlockPos(8, 2, 18);
        helper.setBlock(toolRelative, ModBombBlocks.c4.get().defaultBlockState());
        BlockPos toolPos = helper.absolutePos(toolRelative);
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        helper.assertTrue(ModBombBlocks.c4.get().onScrew(helper.getLevel(), player, toolPos, Direction.UP,
                        Vec3.atCenterOf(toolPos), InteractionHand.MAIN_HAND, ToolType.SCREWDRIVER),
                "screwdriver rejected C-4");
        helper.assertTrue(helper.getLevel().getBlockState(toolPos).getValue(BlockTNTBase.ARMED),
                "screwdriver did not enable ignite-on-break");
        ModBombBlocks.c4.get().playerWillDestroy(helper.getLevel(), toolPos,
                helper.getLevel().getBlockState(toolPos), player);
        helper.getLevel().removeBlock(toolPos, false);
        helper.assertTrue(onlyPrimed(helper, toolPos).getExplosiveState().is(ModBombBlocks.c4.get()),
                "armed tool path did not prime C-4 on break");
        discardPrimed(helper, toolPos);
        helper.succeed();
    }

    public static void chainAndStrengths(GameTestHelper helper) {
        helper.assertTrue(ModBombBlocks.dynamite.get().blastStrength() == 8F
                        && ModBombBlocks.tnt.get().blastStrength() == 10F
                        && ModBombBlocks.semtex.get().blastStrength() == 12F
                        && ModBombBlocks.c4.get().blastStrength() == 15F,
                "conventional explosive strengths no longer match 1.12.2");

        BlockPos relative = new BlockPos(5, 3, 22);
        BlockPos pos = helper.absolutePos(relative);
        try (GameTestHelpers.MockServerPlayer fixture = GameTestHelpers.mockServerPlayer(helper)) {
            fixture.player().setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            fixture.outboundPackets();
            for (BlockTNTBase explosive : List.of(ModBombBlocks.tnt.get(), ModBombBlocks.dynamite.get(),
                    ModBombBlocks.semtex.get(), ModBombBlocks.c4.get())) {
                Entity source = null;
                if (explosive == ModBombBlocks.tnt.get()) {
                    source = fixture.player();
                } else if (explosive == ModBombBlocks.dynamite.get()) {
                    source = new PrimedTnt(helper.getLevel(),
                            pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, fixture.player());
                } else if (explosive == ModBombBlocks.semtex.get()) {
                    source = new EntityPrimedExplosive(helper.getLevel(), pos.getX() + 0.5D,
                            pos.getY() + 0.5D, pos.getZ() + 0.5D, fixture.player(),
                            ModBombBlocks.tnt.get().defaultBlockState());
                } else {
                    Projectile projectile =
                            Entities.createTriggered(
                                    EntityType.SNOWBALL, helper.getLevel());
                    projectile.setOwner(fixture.player());
                    source = projectile;
                }
                helper.setBlock(relative, explosive.defaultBlockState());
                helper.getLevel().explode(source, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        2F, Level.ExplosionInteraction.TNT);
                EntityPrimedExplosive chained = onlyPrimed(helper, pos);
                helper.assertTrue(chained.getFuse() >= 10 && chained.getFuse() < 30
                                && chained.getExplosiveState().getBlock() == explosive,
                        "chain reaction lost the random [10,30) pop fuse or "
                                + explosive.textureName() + " kind");
                helper.assertTrue(fixture.player().getUUID().equals(chained.getOwnerId()),
                        "chain reaction lost direct, vanilla TNT, HBM primed, or projectile ownership for "
                                + explosive.textureName());
                helper.assertTrue(fixture.outboundPackets().stream()
                                .noneMatch(packet -> packet.getClass().getSimpleName().equals("ClientboundSoundPacket")),
                        "chain reaction played the TNT priming sound for " + explosive.textureName());
                chained.discard();
            }
        }

        AssemblyMachineRecipes.INSTANCE.registerRecipes();
        for (String id : List.of("ass.ivymike", "ass.mikecore", "ass.mikedeut", "ass.mikecooler")) {
            helper.assertTrue(AssemblyMachineRecipes.INSTANCE.getRecipe(id) != null,
                    "missing dependency-complete Mike recipe " + id);
        }
        helper.succeed();
    }

    public static void primedMotionMatchesVanilla(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos relative = new BlockPos(2, 20, 2);
        BlockPos pos = helper.absolutePos(relative);
        PrimedTnt vanilla = new PrimedTnt(level, pos.getX() + 0.5D, pos.getY() + 0.5D,
                pos.getZ() + 0.5D, null);
        EntityPrimedExplosive hbm = new EntityPrimedExplosive(level, pos.getX() + 0.5D,
                pos.getY() + 0.5D, pos.getZ() + 0.5D, null,
                ModBombBlocks.tnt.get().defaultBlockState());
        helper.assertTrue(vanilla.getDeltaMovement().y == hbm.getDeltaMovement().y,
                "primed explosives lost vanilla's upward spawn impulse");

        vanilla.setDeltaMovement(0.4D, 0.8D, -0.2D);
        hbm.setDeltaMovement(0.4D, 0.8D, -0.2D);
        vanilla.tick();
        hbm.tick();
        assertSameMotion(helper, vanilla, hbm, "normal gravity/drag motion");

        vanilla.setNoGravity(true);
        hbm.setNoGravity(true);
        vanilla.setDeltaMovement(0.1D, 0.8D, 0.2D);
        hbm.setDeltaMovement(0.1D, 0.8D, 0.2D);
        vanilla.tick();
        hbm.tick();
        assertSameMotion(helper, vanilla, hbm, "no-gravity motion");

        helper.setBlock(relative.offset(1, 0, 0), Blocks.STONE);
        vanilla.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        hbm.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        vanilla.setDeltaMovement(1D, 0D, 0D);
        hbm.setDeltaMovement(1D, 0D, 0D);
        vanilla.tick();
        hbm.tick();
        assertSameMotion(helper, vanilla, hbm, "horizontal collision");

        helper.setBlock(relative.offset(1, 0, 0), Blocks.AIR);
        helper.setBlock(relative, Blocks.AIR);
        helper.setBlock(relative.below(), Blocks.STONE);
        vanilla.setNoGravity(false);
        hbm.setNoGravity(false);
        vanilla.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        hbm.setPos(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        vanilla.setDeltaMovement(0.2D, 0D, 0D);
        hbm.setDeltaMovement(0.2D, 0D, 0D);
        vanilla.tick();
        hbm.tick();
        assertSameMotion(helper, vanilla, hbm, "ground drag and bounce");
        helper.succeed();
    }

    public static void nuclearTooltipFormulas(GameTestHelper helper) {
        helper.assertTrue(BlastInfo.FAT_MAN.radius() == BombConfig.manRadius
                        && BlastInfo.N2.radius() == BombConfig.n2Radius
                        && BlastInfo.FLEIJA.radius() == BombConfig.fleijaRadius
                        && BlastInfo.PROTOTYPE.radius() == BombConfig.prototypeRadius
                        && BlastInfo.SOLINIUM.radius() == BombConfig.soliniumRadius
                        && BlastInfo.BALEFIRE.radius() == 250
                        && BlastInfo.DUD_BALEFIRE.radius() == (int) (BombConfig.fatmanRadius * 1.25)
                        && BlastInfo.NUCLEAR_CHARGE.radius() == BombConfig.missileRadius
                        && BlastInfo.MISSILE_DOOMSDAY.radius() == BombConfig.missileRadius * 2,
                "a blast tooltip lost the radius its 1.7 explosion uses");
        helper.assertTrue(BlastInfo.FAT_MAN.falloutRadius()
                        == (int) (BombConfig.manRadius * 2.5) * BombConfig.falloutRange / 100
                        && BlastInfo.DUD_SALTED.falloutRadius() == (int) (25 * 2.5 + 25) * BombConfig.falloutRange / 100
                        && BlastInfo.n2ChargeRadius() == BombConfig.n2Radius / 12
                        && BlastInfo.shownRadius(437) == 435 && BlastInfo.shownRadius(43) == 45
                        && BlastInfo.shownRadius(88) == 90 && BlastInfo.shownRadius(175) == 175,
                "blast tooltip fallout or N2 charge arithmetic differs from 1.7 EntityNukeExplosionMK5");
        helper.assertTrue(!BlastInfo.N2.hasFallout() && !BlastInfo.FLEIJA.hasFallout()
                        && !BlastInfo.BALEFIRE.hasFallout() && !BlastInfo.FISSURE.hasFallout()
                        && BlastInfo.FAT_MAN.hasFallout() && BlastInfo.TSAR.hasFallout()
                        && BlastInfo.NUCLEAR_CHARGE.hasFallout() && BlastInfo.MISSILE_MICRO.hasFallout(),
                "blast tooltip fallout policy differs from which 1.7 explosions spawn fallout");
        for (Block block : List.of(ModBombs2Blocks.nuke_fleija.get(), ModBombs2Blocks.nuke_prototype.get(),
                ModBombs2Blocks.nuke_solinium.get(), ModBombs2Blocks.nuke_fstbmb.get(),
                ModExplosivesBlocks.crashed_bomb_nuke.get(), ModExplosivesBlocks.det_nuke.get(),
                ModExplosivesBlocks.fissure_bomb.get(), ModExplosivesBlocks.mine_fat.get())) {
            helper.assertTrue(block.asItem() instanceof ItemNukeBlock, "no blast tooltip on " + block);
        }
        helper.succeed();
    }

    public static void vntExposureOrder(GameTestHelper helper) {
        Level level = helper.getLevel();
        BlockPos occluder = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(occluder, Blocks.STONE.defaultBlockState(), 3);
        List<String> phases = new ArrayList<>();

        new ExplosionVNT(level, occluder.getX(), occluder.getY(), occluder.getZ(), 1F)
                .setBlockAllocator((explosion, world, x, y, z, size) -> {
                    phases.add("allocate");
                    ObjectOpenHashSet<BlockPos> blocks =
                            new ObjectOpenHashSet<>();
                    blocks.add(occluder);
                    return blocks;
                })
                .setEntityProcessor((explosion, world, x, y, z, size) -> {
                    phases.add("entities");
                    helper.assertTrue(!world.getBlockState(occluder).isAir(),
                            "Entity exposure saw the world after block destruction");
                    return new HashMap<>();
                })
                .setBlockProcessor((explosion, world, x, y, z, blocks) -> {
                    phases.add("blocks");
                    world.removeBlock(occluder, false);
                })
                .setPlayerProcessor((explosion, world, x, y, z, players) -> phases.add("players"))
                .explode();

        helper.assertTrue(phases.equals(List.of("allocate", "entities", "blocks", "players")),
                "VNT phases changed from allocate/expose/process blocks/process players: " + phases);
        helper.succeed();
    }

    private static void assertSameMotion(GameTestHelper helper, Entity expected, Entity actual, String phase) {
        helper.assertTrue(close(expected.getX(), actual.getX())
                        && close(expected.getY(), actual.getY())
                        && close(expected.getZ(), actual.getZ())
                        && close(expected.getDeltaMovement().x, actual.getDeltaMovement().x)
                        && close(expected.getDeltaMovement().y, actual.getDeltaMovement().y)
                        && close(expected.getDeltaMovement().z, actual.getDeltaMovement().z),
                "HBM primed explosive diverged from vanilla TNT during " + phase);
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) < 1.0E-9D;
    }

    private static TileEntityNukeMike placeMike(GameTestHelper helper, BlockPos relative) {
        helper.setBlock(relative, ModBombBlocks.nuke_mike.get().defaultBlockState());
        return GameTestHelpers.blockEntity(helper, relative, TileEntityNukeMike.class);
    }

    private static void fillPrimary(TileEntityNukeMike mike) {
        for (int slot = 0; slot < 4; slot++) {
            mike.setItem(slot, new ItemStack(ModBombItems.explosive_lenses.get()));
        }
        mike.setItem(4, new ItemStack(ModBombItems.man_core.get()));
    }

    private static EntityNukeExplosionMK5 onlyBlast(GameTestHelper helper, BlockPos around) {
        List<EntityNukeExplosionMK5> blasts = helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class,
                new AABB(around).inflate(2D));
        helper.assertTrue(blasts.size() == 1, "expected one MK5 blast, got " + blasts.size());
        return blasts.get(0);
    }

    private static EntityPrimedExplosive onlyPrimed(GameTestHelper helper, BlockPos around) {
        List<EntityPrimedExplosive> primed = helper.getLevel().getEntitiesOfClass(EntityPrimedExplosive.class,
                new AABB(around).inflate(3D));
        helper.assertTrue(primed.size() == 1, "expected one primed explosive, got " + primed.size());
        return primed.get(0);
    }

    private static void discardNuclear(GameTestHelper helper, BlockPos around) {
        AABB box = new AABB(around).inflate(3D);
        helper.getLevel().getEntitiesOfClass(EntityNukeExplosionMK5.class, box).forEach(e -> e.discard());
        helper.getLevel().getEntitiesOfClass(EntityNukeTorex.class, box).forEach(e -> e.discard());
    }

    private static void discardPrimed(GameTestHelper helper, BlockPos around) {
        AABB box = new AABB(around).inflate(3D);
        helper.getLevel().getEntitiesOfClass(EntityPrimedExplosive.class, box).forEach(e -> e.discard());
    }

    private static final class PersistenceProbe extends EntityPrimedExplosive {
        private PersistenceProbe(GameTestHelper helper) {
            super(ModEntities.PRIMED_EXPLOSIVE.get(), helper.getLevel());
        }
        private PersistenceProbe(GameTestHelper helper, Player owner, BlockState state) {
            super(helper.getLevel(), 2.5, 3, 14.5, owner, state);
        }
        private void write(DataOut output) { saveData(output); }
        private void read(DataIn input) { loadData(input); }
    }

    private record MapWriter(Map<String, Object> values) implements DataOut {
        @Override public void putBoolean(String name, boolean value) { values.put(name, value); }
        @Override public void putByte(String name, byte value) { values.put(name, value); }
        @Override public void putShort(String name, short value) { values.put(name, value); }
        @Override public void putInt(String name, int value) { values.put(name, value); }
        @Override public void putLong(String name, long value) { values.put(name, value); }
        @Override public void putFloat(String name, float value) { values.put(name, value); }
        @Override public void putDouble(String name, double value) { values.put(name, value); }
        @Override public void putString(String name, String value) { values.put(name, value); }
        @Override public void putIntArray(String name, int[] value) { values.put(name, value.clone()); }
        @Override public <T> void store(String name, Codec<T> codec, T value) {
            values.put(name, value);
        }
        @Override public <T> void storeNullable(String name, Codec<T> codec, T value) {
            if (value != null) values.put(name, value);
        }
        @Override public DataOut child(String name) { return new MapWriter(new HashMap<>()); }
        @Override public ChildList childrenList(String name) { return () -> new MapWriter(new HashMap<>()); }
        @Override public void discard(String name) { values.remove(name); }
    }

    private record MapReader(Map<String, Object> values) implements DataIn {
        @Override public boolean getBooleanOr(String name, boolean fallback) { return value(name, Boolean.class).orElse(fallback); }
        @Override public byte getByteOr(String name, byte fallback) { return number(name).map(Number::byteValue).orElse(fallback); }
        @Override public short getShortOr(String name, short fallback) { return number(name).map(Number::shortValue).orElse(fallback); }
        @Override public int getIntOr(String name, int fallback) { return number(name).map(Number::intValue).orElse(fallback); }
        @Override public long getLongOr(String name, long fallback) { return number(name).map(Number::longValue).orElse(fallback); }
        @Override public float getFloatOr(String name, float fallback) { return number(name).map(Number::floatValue).orElse(fallback); }
        @Override public double getDoubleOr(String name, double fallback) { return number(name).map(Number::doubleValue).orElse(fallback); }
        @Override public String getStringOr(String name, String fallback) { return value(name, String.class).orElse(fallback); }
        @Override public Optional<int[]> getIntArray(String name) { return value(name, int[].class).map(int[]::clone); }
        @Override @SuppressWarnings("unchecked") public <T> Optional<T> read(
                String name, Codec<T> codec) {
            return Optional.ofNullable((T) values.get(name));
        }
        @Override public Optional<DataIn> child(String name) { return Optional.empty(); }
        @Override public DataIn childOrEmpty(String name) { return new MapReader(Map.of()); }
        @Override public List<DataIn> childrenListOrEmpty(String name) { return List.of(); }
        private Optional<Number> number(String name) { return value(name, Number.class); }
        private <T> Optional<T> value(String name, Class<T> type) {
            Object value = values.get(name);
            return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
        }
    }
}
