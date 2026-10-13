package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.pile.TileEntityPileBaseMK2;
import com.hbm.tileentity.machine.pile.TileEntityPileCore;
import com.hbm.tileentity.machine.pile.TileEntityPileCore.PileOrientation;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Loose graphite. A hand drill on any side face measures the solid block of bricks around that point and,
 * if it is at least {@value #MIN_H_SIZE} on every axis and the drilled cell is not on a face, turns the
 * whole box into a live pile with that cell as its core.
 *
 * <p>The measurement runs outward from the clicked block, so where the player drills decides where the
 * core sits, and with it which way the fuel channels run and how deep they are.
 */
public class BlockPileBrick extends CompatBlock implements IToolable {

    public static final int MIN_V_SIZE = 5;
    public static final int MIN_H_SIZE = 5;
    public static final int MAX_V_SIZE = 15;
    public static final int MAX_H_SIZE = 15;

    public BlockPileBrick(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit,
                           InteractionHand hand, ToolType tool) {
        if (tool != ToolType.HAND_DRILL) return false;
        if (side == Direction.DOWN || side == Direction.UP) return false;
        if (level.isClientSide()) return true;

        return assemble(level, player, pos, ForgeDirection.getOrientation(side).getOpposite());
    }

    /** Validates the graphite box around {@code pos} and converts it into a pile cored there. */
    public boolean assemble(Level level, Player player, BlockPos pos, ForgeDirection dir) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        ForgeDirection dirLeft = dir.getRotation(ForgeDirection.DOWN);

        int posHeight = 0;
        int negHeight = 0;
        int left = 0;
        int right = 0;
        int depth = 0;

        for (int i = 1; i <= MAX_V_SIZE - 1; i++) {
            if (!isBrick(level, x, y + i, z)) break;
            posHeight = i;
        }
        for (int i = 1; i <= MAX_V_SIZE - posHeight - 1; i++) {
            if (!isBrick(level, x, y - i, z)) break;
            negHeight = i;
        }
        for (int i = 1; i <= MAX_H_SIZE - 1; i++) {
            if (!isBrick(level, x + dirLeft.offsetX * i, y, z + dirLeft.offsetZ * i)) break;
            left = i;
        }
        for (int i = 1; i <= MAX_H_SIZE - left - 1; i++) {
            if (!isBrick(level, x - dirLeft.offsetX * i, y, z - dirLeft.offsetZ * i)) break;
            right = i;
        }
        for (int i = 1; i <= MAX_H_SIZE; i++) {
            if (!isBrick(level, x + dir.offsetX * i, y, z + dir.offsetZ * i)) break;
            depth = i;
        }

        if (posHeight + negHeight + 1 < MIN_V_SIZE) {
            BlockPile.sendError(level, new BlockPos(x, y + posHeight, z), "Height too low (<" + MIN_V_SIZE + ")", player);
            BlockPile.sendError(level, new BlockPos(x, y - negHeight, z), "Height too low (<" + MIN_V_SIZE + ")", player);
            return true;
        }
        if (left + right + 1 < MIN_H_SIZE) {
            BlockPile.sendError(level, new BlockPos(x + dirLeft.offsetX * left, y, z + dirLeft.offsetZ * left),
                    "Width too low (<" + MIN_H_SIZE + ")", player);
            BlockPile.sendError(level, new BlockPos(x - dirLeft.offsetX * right, y, z - dirLeft.offsetZ * right),
                    "Width too low (<" + MIN_H_SIZE + ")", player);
            return true;
        }
        if (depth + 1 < MIN_H_SIZE) {
            BlockPile.sendError(level, new BlockPos(x + dir.offsetX * depth, y, z + dir.offsetZ * depth),
                    "Depth too low (<" + MIN_H_SIZE + ")", player);
            return true;
        }
        if (posHeight == 0 || negHeight == 0 || left == 0 || right == 0) {
            BlockPile.sendError(level, pos, "Core cannot be on an edge", player);
            return true;
        }

        for (int h = -negHeight; h <= posHeight; h++) {
            for (int v = -left; v <= right; v++) {
                for (int d = 0; d <= depth; d++) {
                    BlockPos iPos = cell(x, y, z, dir, dirLeft, h, v, d);
                    if (!isBrick(level, iPos.getX(), iPos.getY(), iPos.getZ())) {
                        BlockPile.sendError(level, iPos, "Graphite block missing", player);
                        return true;
                    }
                }
            }
        }

        for (int h = -negHeight; h <= posHeight; h++) {
            for (int v = -left; v <= right; v++) {
                for (int d = 0; d <= depth; d++) {
                    BlockPos iPos = cell(x, y, z, dir, dirLeft, h, v, d);

                    if (iPos.getX() == x && iPos.getY() == y && iPos.getZ() == z) {
                        level.setBlock(iPos, ModPileBlocks.pile_block.get().defaultBlockState()
                                .setValue(BlockPile.META, BlockPile.META_CORE), 3);
                        if (level.getBlockEntity(iPos) instanceof TileEntityPileCore core) {
                            core.orientation = PileOrientation.getOrientation(dir);
                            core.setupSize(posHeight, negHeight, left, right, depth + 1);
                        }
                    } else {
                        int edgeCount = 0;
                        if (h == -negHeight || h == posHeight) edgeCount++;
                        if (v == -left || v == right) edgeCount++;
                        if (d == 0 || d == depth) edgeCount++;

                        level.setBlock(iPos, ModPileBlocks.pile_block.get().defaultBlockState()
                                .setValue(BlockPile.META, edgeCount > 1 ? BlockPile.META_EDGE : BlockPile.META_DUMMY), 3);
                        if (level.getBlockEntity(iPos) instanceof TileEntityPileBaseMK2 pile) {
                            pile.setCore(x, y, z);
                        }
                    }
                }
            }
        }

        return true;
    }

    private static BlockPos cell(int x, int y, int z, ForgeDirection dir, ForgeDirection dirLeft,
                                 int h, int v, int d) {
        return new BlockPos(x - dirLeft.offsetX * v + dir.offsetX * d, y + h,
                z - dirLeft.offsetZ * v + dir.offsetZ * d);
    }

    private boolean isBrick(Level level, int x, int y, int z) {
        return level.getBlockState(new BlockPos(x, y, z)).is(this);
    }
}
