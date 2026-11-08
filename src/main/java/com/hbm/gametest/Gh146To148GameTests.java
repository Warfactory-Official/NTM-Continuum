package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.blocks.machine.pile.ModPileBlocks;
import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemPileRodMK2;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKRod;
import dev.golem.core.Ids;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.net.GolemBuffers;
import dev.golem.core.registry.BlockEntities;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** GitHub issues 146 (duct sound), 147 (RBMK DODD and rods by hand) and 148 (pile device animation, rod stacking). */
public final class Gh146To148GameTests {

    private Gh146To148GameTests() {}

    /** Every block 1.7 gives {@code ModSoundTypes.pipe}. */
    private static final List<String> PIPE_SOUND_IDS = pipeSoundIds();

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function PIPE_SOUND =
            TESTS.function("gh146_pipe_sound_type", 20, () -> Gh146To148GameTests::pipeSound);
    public static final GameTests.Function ROD_BY_HAND =
            TESTS.function("gh147_fuel_rod_by_hand", 20, () -> Gh146To148GameTests::rodByHand);
    public static final GameTests.Function DODD =
            TESTS.function("gh147_dodd_lines", 20, () -> Gh146To148GameTests::dodd);
    public static final GameTests.Function PILE_ROD_STACK =
            TESTS.function("gh148_pile_rods_stack", 20, () -> Gh146To148GameTests::pileRodStack);
    public static final GameTests.Function CLIENT_TICKERS =
            TESTS.function("gh148_client_tick_logic_has_a_ticker", 20, () -> Gh146To148GameTests::clientTickers);

    public static void init() {}

    private static List<String> pipeSoundIds() {
        List<String> ids = new ArrayList<>(List.of("fluid_duct_neo", "fluid_duct_neo_silver", "fluid_duct_neo_colored",
                "pipe_anchor", "pipe_anchor_exhaust", "pipe_anchor_pneumatic", "pneumatic_tube", "pneumatic_storage_access",
                "pneumatic_storage_clutter", "pneumatic_storage_mono", "pneumatic_storage_importer", "pneumatic_storage_exporter"));
        for (String material : new String[]{"silver", "copper", "white"}) {
            for (int size = 0; size < 5; size++) ids.add("fluid_duct_box_" + material + "_" + size);
        }
        for (int size = 0; size < 5; size++) ids.add("fluid_duct_exhaust_" + size);
        return ids;
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.getOptional(Ids.of(Constants.MOD_ID, path)).orElse(null);
    }

    private static String sound(SoundEvent event) {
        return String.valueOf(BuiltInRegistries.SOUND_EVENT.getKey(event));
    }

    /** 1.7 places and breaks at volume 0.85 with "hbm:block.pipePlaced", pitch 0.85..1.05, breaking 0.15 lower. */
    public static void pipeSound(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (String id : PIPE_SOUND_IDS) {
            Block block = block(id);
            if (block == null) {
                wrong.add(id + " (missing)");
                continue;
            }
            SoundType type = block.defaultBlockState().getSoundType();
            String place = sound(type.getPlaceSound());
            float placePitch = type.getPitch();
            String broken = sound(type.getBreakSound());
            float breakPitch = type.getPitch();
            if (!place.equals("hbm:block.pipe_placed") || !broken.equals("hbm:block.pipe_placed") || type.getVolume() != 0.85F
                    || placePitch < 0.85F || placePitch >= 1.05F || breakPitch < 0.70F || breakPitch >= 0.90F) {
                wrong.add(id + " (" + place + " " + placePitch + ", " + broken + " " + breakPitch + ", volume " + type.getVolume() + ")");
            }
        }
        helper.assertTrue(PIPE_SOUND_IDS.size() == 32, "1.7 gives the pipe sound to 32 block ids here, listed " + PIPE_SOUND_IDS.size());
        helper.assertTrue(wrong.isEmpty(), "blocks without 1.7's pipe sound: " + wrong);
        String pump = sound(block("fluid_pump").defaultBlockState().getSoundType().getPlaceSound());
        helper.assertFalse(pump.equals("hbm:block.pipe_placed"), "1.7's fluid pump has no pipe sound");
        helper.succeed();
    }

    private static BlockPos column(GameTestHelper helper, Block block, BlockPos core) {
        helper.setBlock(core, block.defaultBlockState());
        block.setPlacedBy(helper.getLevel(), helper.absolutePos(core), block.defaultBlockState(),
                GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL), new ItemStack(block));
        return helper.absolutePos(core);
    }

    /** A rod in hand fills an empty channel of each of the four fuel columns without a GUI (the full channel's GUI is a harness click). */
    public static void rodByHand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Block[] channels = {ModRBMKBlocks.rbmk_fuel_rod.get(), ModRBMKBlocks.rbmk_fuel_rod_moderated.get(),
                ModRBMKBlocks.rbmk_fuel_rod_reasim.get(), ModRBMKBlocks.rbmk_fuel_rod_reasim_moderated.get()};
        for (int i = 0; i < channels.length; i++) {
            BlockPos core = column(helper, channels[i], new BlockPos(1 + 2 * i, 1, 1));
            BlockPos top = core.above(3);
            ServerPlayer player = GameTestHelpers.mockServerPlayerInLevel(helper);
            ItemStack rods = new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, rods);
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(top).add(0, 0.5, 0), Direction.UP, top, false);
            player.gameMode.useItemOn(player, level, rods, InteractionHand.MAIN_HAND, hit);

            BlockEntityRBMKRod rod = (BlockEntityRBMKRod) level.getBlockEntity(core);
            String name = BuiltInRegistries.BLOCK.getKey(channels[i]).getPath();
            helper.assertTrue(rod != null && rod.inventory.getStackInSlot(0).is(ModRBMKItems.rbmk_fuel_ueu.get()),
                    name + ": the rod in hand must go into the empty channel");
            helper.assertTrue(player.getMainHandItem().isEmpty(), name + ": a survival player's rod must be used up");
            helper.assertTrue(player.containerMenu == player.inventoryMenu, name + ": inserting a rod must not open the GUI");

        }
        helper.succeed();
    }

    private static String line(String key, String value) {
        return Component.translatable("tile.rbmk.dodd." + key).getString() + ": " + value;
    }

    /** The DODD lines of a fresh UEU rod and of a boiler, read from client copies made through the sync packet. */
    public static void dodd(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = column(helper, ModRBMKBlocks.rbmk_fuel_rod.get(), new BlockPos(1, 1, 1));
        BlockEntityRBMKRod rod = (BlockEntityRBMKRod) level.getBlockEntity(core);
        rod.inventory.setStackInSlot(0, new ItemStack(ModRBMKItems.rbmk_fuel_ueu.get()));
        rod.hasRod = true;
        var buffer = GolemBuffers.wrap(Unpooled.buffer(), level);
        rod.serialize(buffer);
        BlockEntityRBMKRod copy = new BlockEntityRBMKRod(BlockPos.ZERO, rod.getBlockState());
        copy.deserialize(buffer);

        String name = ModRBMKBlocks.rbmk_fuel_rod.get().getName().getString();
        ILookOverlay.Data data = ((ILookOverlay) ModRBMKBlocks.rbmk_fuel_rod.get()).getLookOverlay(level, core.above(2), null);
        helper.assertTrue(data != null && data.diagnostic() && data.title().equals(name),
                "a fuel channel must show its DODD titled with the block name, got " + data);
        // 1.7: yield 1E8 of 1E8, no xenon, hull and core at 20 C, UEU melts at 2865 C; sorted by NBT key
        List<String> expected = List.of(
                line("f_heat", "\"20.000000 / 20.000000 / 2865.00\""),
                line("f_xenon", "\"0.0%\""),
                line("f_yield", "\"1.0E8 / 1.0E8 (100.0%)\""),
                line("fluxFast", "0.0"),
                line("fluxSlow", "0.0"),
                line("hasRod", "1"),
                line("heat", "20.0"),
                line("reasimSteam", "0"),
                line("reasimWater", "0"));
        List<String> lines = copy.diagnostic(name).lines();
        helper.assertTrue(lines.equals(expected), "fuel channel DODD: expected " + expected + ", got " + lines);

        BlockPos boilerCore = column(helper, ModRBMKBlocks.rbmk_boiler.get(), new BlockPos(4, 1, 1));
        BlockEntityRBMKBoiler boiler = (BlockEntityRBMKBoiler) level.getBlockEntity(boilerCore);
        List<String> boilerLines = boiler.diagnostic("boiler").lines();
        helper.assertTrue(boilerLines.contains(line("feed_type", String.valueOf(Fluids.legacyId(Fluids.WATER))))
                        && boilerLines.contains(line("feed_max", "10000")) && boilerLines.contains(line("steam_p", "0")),
                "boiler DODD must list its tanks as 1.7's FluidTank NBT, got " + boilerLines);
        helper.succeed();
    }

    /** 1.7's pile rods keep the default stack size of 64. */
    public static void pileRodStack(GameTestHelper helper) {
        List<String> wrong = new ArrayList<>();
        for (ItemStack stack : ModPileBlocks.pile_rod.stacks()) {
            if (!(stack.getItem() instanceof ItemPileRodMK2) || stack.getMaxStackSize() != 64) {
                wrong.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + "=" + stack.getMaxStackSize());
            }
        }
        helper.assertTrue(ModPileBlocks.pile_rod.stacks().size() == 9 && wrong.isEmpty(), "pile rods must stack to 64: " + wrong);
        helper.succeed();
    }

    /** A block entity with a static {@code clientTick} must tick on the client, or its animation never moves. */
    public static void clientTickers(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        Set<String> missing = new TreeSet<>();
        int checked = 0;
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Constants.MOD_ID)) continue;
            if (!(block instanceof EntityBlock entityBlock)) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                BlockEntity be;
                try {
                    be = entityBlock.newBlockEntity(pos, state);
                } catch (RuntimeException e) {
                    // states some block entities cannot be built for outside a level; the default-state test covers them
                    continue;
                }
                if (be == null || !hasClientTick(be.getClass())) continue;
                checked++;
                if (!BlockEntities.ticks(be.getType(), true)) missing.add(be.getClass().getSimpleName());
            }
        }
        helper.assertTrue(checked > 0, "no block entity with a clientTick was found");
        helper.assertTrue(missing.isEmpty(), "block entities with a clientTick but no client ticker: " + missing);
        helper.succeed();
    }

    private static boolean hasClientTick(Class<?> type) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals("clientTick") && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 4 && method.getParameterTypes()[0] == Level.class) return true;
        }
        return false;
    }
}
