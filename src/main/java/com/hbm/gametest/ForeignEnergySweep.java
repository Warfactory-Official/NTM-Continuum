package com.hbm.gametest;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.networkproviders.PowerNetProvider;
import dev.golem.core.gametest.GameTests;
import dev.golem.core.world.EnergyCompat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Builds energy machines one at a time and asks the foreign lookup on every face a cable can use: a ported machine's
 * port faces, else the faces HBM's cable connects to. Shared with addons' tests.
 */
public final class ForeignEnergySweep {

    /** How far from the core a footprint may reach (Manhattan, three times this); covers the largest multiblock. */
    public static final int SPAN = 20;
    private static final int TICKS = 5;

    public record Face(BlockPos cell, Direction side) {}

    public record Result(List<String> checked, List<String> missing, List<String> portOnly) {}

    private ForeignEnergySweep() {}

    public static List<Block> energyBlocks(String namespace) {
        List<Block> out = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!namespace.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())
                    || !(block instanceof EntityBlock entityBlock)) continue;
            if (entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState()) instanceof IEnergyHandlerMK2) out.add(block);
        }
        return out;
    }

    public static void run(GameTestHelper helper, List<Block> blocks, int padding,
                           Consumer<Result> done) {
        next(helper, blocks, 0, GameTests.paddedCentre(padding, 2),
                new Result(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()), done);
    }

    private static void next(GameTestHelper helper, List<Block> blocks, int index, BlockPos centre, Result result,
                             Consumer<Result> done) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(centre);
        if (index == blocks.size()) {
            done.accept(result);
            return;
        }
        Block block = blocks.get(index);
        if (block instanceof BlockMultiblockCore multiblock) MultiblockHandlerXR.placeAssembled(level, core, multiblock, Direction.NORTH);
        else level.setBlock(core, block.defaultBlockState(), 3);
        helper.runAfterDelay(TICKS, () -> {
            List<BlockPos> footprint = footprint(level, core);
            if (level.getBlockEntity(core) instanceof IEnergyHandlerMK2 machine && exposable(machine)) {
                String id = BuiltInRegistries.BLOCK.getKey(block).toString();
                result.checked().add(id);
                for (Face face : faces(level, footprint, machine, result.portOnly(), id)) {
                    if (EnergyCompat.find(level, face.cell(), face.side()) == null) {
                        result.missing().add(id + " " + helper.relativePos(face.cell()) + " " + face.side());
                    }
                }
            }
            for (BlockPos cell : footprint) level.setBlock(cell, Blocks.AIR.defaultBlockState(), 2 | 16);
            next(helper, blocks, index + 1, centre, result, done);
        });
    }

    /** A buffer, or a relay that takes (the charger); a bufferless source (creative generator) is never exposed. */
    private static boolean exposable(IEnergyHandlerMK2 machine) {
        return machine.ownsEnergyBuffer() || machine instanceof IEnergyReceiverMK2;
    }

    /** The core and every cell resolving to it, so the sweep reads and clears only the machine. */
    private static List<BlockPos> footprint(ServerLevel level, BlockPos core) {
        List<BlockPos> cells = new ArrayList<>();
        BlockEntity owner = level.getBlockEntity(core);
        if (level.getBlockState(core).isAir()) return cells;
        Set<BlockPos> seen = new HashSet<>(Set.of(core));
        Deque<BlockPos> open = new ArrayDeque<>(List.of(core));
        while (!open.isEmpty()) {
            BlockPos cell = open.poll();
            cells.add(cell);
            for (Direction side : Direction.values()) {
                BlockPos next = cell.relative(side);
                if (next.distManhattan(core) > 3 * SPAN || !seen.add(next) || level.getBlockState(next).isAir()) continue;
                if (owner != null && (level.getBlockEntity(next) == owner
                        || MachineCore.at(level, next, level.getBlockState(next)) == owner)) open.add(next);
            }
        }
        return cells;
    }

    private static List<Face> faces(ServerLevel level, List<BlockPos> footprint, IEnergyHandlerMK2 machine,
                                    List<String> portOnly, String id) {
        List<Face> out = new ArrayList<>();
        boolean ported = PortNodes.isPorted(level, machine, true);
        for (BlockPos cell : footprint) {
            for (Direction side : Direction.values()) {
                BlockPos outside = cell.relative(side);
                if (!level.getBlockState(outside).isAir()) continue;
                boolean cable = PowerNetwork.handlerAtFace(level, cell, side) == machine;
                if (ported) {
                    if (!port(level, outside, side, machine)) continue;
                    if (!cable) portOnly.add(id + " " + cell + " " + side);
                } else if (!cable) continue;
                out.add(new Face(cell, side));
            }
        }
        return out;
    }

    private static boolean port(ServerLevel level, BlockPos outside, Direction side, IEnergyHandlerMK2 machine) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        return machine instanceof IEnergyReceiverMK2
                && PortNodes.allows(level, outside.asLong(), dir, machine, PowerNetProvider.THE_PROVIDER, true)
                || machine instanceof IEnergyProviderMK2
                && PortNodes.allows(level, outside.asLong(), dir, machine, PowerNetProvider.THE_PROVIDER, false);
    }
}
