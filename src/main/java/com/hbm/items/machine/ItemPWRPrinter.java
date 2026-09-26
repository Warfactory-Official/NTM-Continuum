package com.hbm.items.machine;

import com.hbm.lib.ForgeDirection;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.machine.BlockPWR;
import com.hbm.blocks.machine.MachinePWRController;
import com.hbm.tileentity.machine.TileEntityBlockPWR;
import com.hbm.tileentity.machine.TileEntityPWRController;

/**
 * The PWR printer (1.7 {@code ItemPWRPrinter}): used on an assembled controller, it measures the reactor's
 * bounding box, ships every cell's assembled-from block to the client on the controller's next sync packet,
 * and opens the slice printer for any client player holding the printer.
 *
 * <p>The client screen is reached through {@link #OPEN_SCREEN}, which the client registrations install, so
 * this common class never names a client class.
 */
public class ItemPWRPrinter extends CompatItem {

    /** Installed by the client registrations: opens the slice printer for the synced bounds. */
    public static @Nullable Opener OPEN_SCREEN;

    public interface Opener {
        void open(int x1, int y1, int z1, int x2, int y2, int z2, ForgeDirection dir, Set<Block> whitelist);
    }

    private static int x1, y1, z1;
    private static int x2, y2, z2;
    private static Block @Nullable [] blockSync;
    private static ForgeDirection dir = ForgeDirection.NORTH;

    private final Set<BlockPos> fill = new HashSet<>();

    public ItemPWRPrinter(Properties properties) {
        super(properties);
    }

    private static Set<Block> whitelist() {
        Set<Block> whitelist = new HashSet<>();
        whitelist.add(ModPwrBlocks.PWR_BLOCK.get());
        whitelist.add(ModPwrBlocks.PWR_CONTROLLER.get());
        return whitelist;
    }

    /** Piggybacking on the controller's BlockEntity sync. */
    public static void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeInt(x1);
        buf.writeInt(y1);
        buf.writeInt(z1);
        buf.writeInt(x2);
        buf.writeInt(y2);
        buf.writeInt(z2);
        buf.writeInt(dir.ordinal());

        Block[] blocks = blockSync == null ? new Block[0] : blockSync;
        buf.writeInt(blocks.length);
        for (Block block : blocks) {
            buf.writeInt(block == null ? -1 : BuiltInRegistries.BLOCK.getId(block));
        }
        blockSync = null;
    }

    /** Client: applies the snapshot to the cells and opens the printer for a player holding one. */
    public static void deserialize(Level level, RegistryFriendlyByteBuf buf) {
        x1 = buf.readInt();
        y1 = buf.readInt();
        z1 = buf.readInt();
        x2 = buf.readInt();
        y2 = buf.readInt();
        z2 = buf.readInt();
        dir = ForgeDirection.VALUES[Math.floorMod(buf.readInt(), ForgeDirection.VALUES.length)];
        int count = buf.readInt();

        int i = 0;
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    if (i >= count) return;
                    int id = buf.readInt();
                    i++;
                    Block block = id < 0 ? null : BuiltInRegistries.BLOCK.byId(id);
                    BlockEntity tile = level.getBlockEntity(new BlockPos(x, y, z));
                    if (!(tile instanceof TileEntityBlockPWR cell)) continue;
                    if (block != null) cell.block = block;
                }
            }
        }

        // Open the printer GUI on any client players holding the printer
        if (OPEN_SCREEN != null) OPEN_SCREEN.open(x1, y1, z1, x2, y2, z2, dir, whitelist());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity tile = level.getBlockEntity(context.getClickedPos());
        if (!(tile instanceof TileEntityPWRController pwr)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        syncAndScreenshot(level, pwr);
        return InteractionResult.CONSUME;
    }

    public void syncAndScreenshot(Level level, TileEntityPWRController pwr) {
        findBounds(level, pwr);

        int sizeX = x2 - x1 + 1;
        int sizeY = y2 - y1 + 1;
        int sizeZ = z2 - z1 + 1;

        blockSync = new Block[sizeX * sizeY * sizeZ];
        int i = 0;

        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    BlockEntity tile = level.getBlockEntity(new BlockPos(x, y, z));
                    if (tile instanceof TileEntityBlockPWR cell) blockSync[i] = cell.block;
                    i++;
                }
            }
        }

        pwr.isPrinting = true;
        pwr.syncState();
    }

    public void findBounds(Level level, TileEntityPWRController pwr) {
        BlockPos pos = pwr.getBlockPos();
        dir = ForgeDirection.getOrientation(pwr.getBlockState().getValue(MachinePWRController.FACING)).getOpposite();

        fill.clear();
        fill.add(pos);
        x1 = x2 = pos.getX();
        y1 = y2 = pos.getY();
        z1 = z2 = pos.getZ();
        floodFill(level, pos.offset(dir.offsetX, 0, dir.offsetZ));
    }

    private void floodFill(Level level, BlockPos pos) {
        if (fill.contains(pos)) return;

        if (level.getBlockState(pos).getBlock() instanceof BlockPWR) {
            fill.add(pos);

            x1 = Math.min(x1, pos.getX());
            y1 = Math.min(y1, pos.getY());
            z1 = Math.min(z1, pos.getZ());
            x2 = Math.max(x2, pos.getX());
            y2 = Math.max(y2, pos.getY());
            z2 = Math.max(z2, pos.getZ());

            floodFill(level, pos.offset(1, 0, 0));
            floodFill(level, pos.offset(-1, 0, 0));
            floodFill(level, pos.offset(0, 1, 0));
            floodFill(level, pos.offset(0, -1, 0));
            floodFill(level, pos.offset(0, 0, 1));
            floodFill(level, pos.offset(0, 0, -1));
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.literal("Use on a constructed PWR controller to generate construction diagrams"));
    }
}
