package com.hbm.blocks.generic;

import com.hbm.api.block.IToolable;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.items.tool.ItemBlowtorch;
import com.hbm.tileentity.deco.TileEntityLanternBehemoth;
import com.hbm.util.InventoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code BlockLanternBehemoth}, the "Old Lantern": the lantern post's rusted twin, the same five-tall column
 * ({@code {4,0,0,0,0,0}}, offset 0) with a green-lit lamp that turns red and sags when broken. It drops nothing.
 *
 * <p>A broken one is repaired with the blowtorch, spending two steel plates and a basic circuit from the
 * inventory ({@code IRepairable.tryRepairMultiblock}); while a blowtorch is held the look overlay lists those
 * materials. Repaired, it calls for help: horns, then a Bobmazon drop of supplies (see
 * {@link TileEntityLanternBehemoth}). A repair raises the repairer's reputation ({@link HbmPlayerProps}, up to 25); a
 * lantern that is destroyed lowers everyone's within 50 blocks (down to -25).
 */
public class BlockLanternBehemoth extends BlockMultiblockMachine implements IToolable, ILookOverlay {

    public BlockLanternBehemoth(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLanternBehemoth(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{4, 0, 0, 0, 0, 0};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    private @Nullable TileEntityLanternBehemoth lanternAt(Level level, BlockPos pos) {
        BlockPos core = coreOf(level, pos, level.getBlockState(pos));
        return core != null && level.getBlockEntity(core) instanceof TileEntityLanternBehemoth lantern ? lantern : null;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand,
                           ToolType tool) {
        if (tool != ToolType.TORCH) return false;
        // 1.7 IRepairable.tryRepairMultiblock
        TileEntityLanternBehemoth lantern = lanternAt(level, pos);
        if (lantern == null || !lantern.isDamaged()) return false;
        List<AStack> list = lantern.getRepairMaterials();
        if (list.isEmpty() || InventoryUtil.doesPlayerHaveAStacks(player, list, true)) {
            if (!level.isClientSide()) {
                lantern.repair();
                // 1.7 onScrew: a repair earns reputation, up to 25
                if (HbmPlayerProps.getReputation(player) < HbmPlayerProps.REPUTATION_MAX) {
                    HbmPlayerProps.setReputation(player, HbmPlayerProps.getReputation(player) + 1);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 1.7 {@code TileEntityLanternBehemoth.invalidate}: a lantern that goes away costs every player within 50 blocks
     * one reputation, down to -25. 1.7 hooked the tile's invalidation, i.e. the block being broken; the port hooks the
     * core's removal (a chunk unload also removes block entities here and must not count).
     *
     * <p>The decision waits for the end of the server tick: hand placement removes the block it just placed and
     * re-sets it as the core ({@code setPlacedBy}), and in 1.7 that first block had no tile (only core metas &gt;= 12
     * made one), so placing a lantern cost nothing. A removal counts only if no lantern stands there at the end of the
     * tick (a lantern core or cell there means the multiblock rebuilt itself); a cell broken by hand still tears the
     * core down and counts.
     */
    @Override
    protected void removedAt(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!state.getValue(CELL)) {
            BlockPos at = pos.immutable();
            List<Player> near = new ArrayList<>(level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(
                    at.getX() - 50, at.getY() - 50, at.getZ() - 50, at.getX() + 51, at.getY() + 51, at.getZ() + 51)));
            synchronized (PENDING_REMOVALS) {
                PENDING_REMOVALS.add(new PendingRemoval(level, at, this, near));
            }
        }
        super.removedAt(state, level, pos, movedByPiston);
    }

    private record PendingRemoval(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockLanternBehemoth block,
                                  List<Player> near) {}

    private static final List<PendingRemoval> PENDING_REMOVALS = new ArrayList<>();
    private static boolean hooked;

    /** Registers the end-of-tick reputation settlement of removed lanterns (from {@code HbmPlayerProps.init}). */
    public static void initReputationHook() {
        if (hooked) return;
        hooked = true;
        dev.golem.core.platform.Lifecycle.onServerTick(server -> settleRemovals());
    }

    /** Charges the players near every lantern core removed this tick that did not come back. */
    public static void settleRemovals() {
        List<PendingRemoval> due;
        synchronized (PENDING_REMOVALS) {
            if (PENDING_REMOVALS.isEmpty()) return;
            due = new ArrayList<>(PENDING_REMOVALS);
            PENDING_REMOVALS.clear();
        }
        for (PendingRemoval removal : due) {
            if (removal.level().getBlockState(removal.pos()).is(removal.block())) continue;
            for (Player player : removal.near()) {
                if (HbmPlayerProps.getReputation(player) > HbmPlayerProps.REPUTATION_MIN) {
                    HbmPlayerProps.setReputation(player, HbmPlayerProps.getReputation(player) - 1);
                }
            }
        }
    }

    /** 1.7 {@code IRepairable.addGenericOverlay}: shown only with a blowtorch in hand and a broken lantern. */
    @Override
    public @Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof ItemBlowtorch)) return null;
        TileEntityLanternBehemoth lantern = lanternAt(level, pos);
        if (lantern == null || !lantern.isDamaged()) return null;
        List<String> text = new ArrayList<>();
        text.add(ChatFormatting.GOLD + "Repair with:");
        for (AStack stack : lantern.getRepairMaterials()) {
            try {
                ItemStack display = stack.getCyclingDisplayStack();
                text.add("- " + display.getHoverName().getString() + " x" + stack.stacksize);
            } catch (Exception ex) {
                text.add(ChatFormatting.RED + "- ERROR");
            }
        }
        return new Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
