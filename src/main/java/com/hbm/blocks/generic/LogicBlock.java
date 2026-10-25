package com.hbm.blocks.generic;

import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModDungeonBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.SavedIds;
import com.hbm.world.gen.util.LogicBlockActions;
import com.hbm.world.gen.util.LogicBlockConditions;
import com.hbm.world.gen.util.LogicBlockInteractions;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 1.7 {@code LogicBlock}, the "Dungeon Action Block": a structure scripting block. Its block entity names an action,
 * a condition and optionally an interaction ({@link LogicBlockActions}, {@link LogicBlockConditions},
 * {@link LogicBlockInteractions}); every server tick the action runs, then the condition decides whether the
 * phase advances (the timer counts ticks since the last advance), and a right-click runs the interaction. An
 * unknown action or condition removes the block. The default script is {@code FODDER_WAVE} on
 * {@code PLAYER_CUBE_5}: a ring of armed zombies once a player comes near.
 *
 * <p>It may wear another block as a disguise, which its renderer draws in place of its own face; the invisible
 * variant ({@code logic_block_invis}, 1.7 {@code LogicBlockInvis}) draws nothing at all. 1.7 read the disguise's
 * metadata too; the port stores the block and draws its default state.
 */
public class LogicBlock extends CompatBlock implements TickingBlock {

    private final boolean invisible;

    public LogicBlock(Properties properties, boolean invisible) {
        super(properties);
        this.invisible = invisible;
    }

    public boolean isInvisible() {
        return invisible;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // both are drawn by their renderer (the visible one wears its disguise there); the invisible one by nothing
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLogicBlock(pos, state);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.getBlockEntity(pos) instanceof TileEntityLogicBlock logic && logic.interaction != null) {
            logic.interaction.accept(new Object[]{level, logic, pos.getX(), pos.getY(), pos.getZ(), player,
                    hit.getDirection().ordinal(), (float) (hit.getLocation().x - pos.getX()),
                    (float) (hit.getLocation().y - pos.getY()), (float) (hit.getLocation().z - pos.getZ())});
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    public static class TileEntityLogicBlock extends TileEntityLoadedBase {

        // phase is incremented per condition check, timer counts since last condition check by default
        public int phase = 0;
        public int timer = 0;

        public @Nullable BlockState disguise;

        /** Actions always get called before conditions, use the phase and timer variables to control behaviour. */
        public String conditionID = "PLAYER_CUBE_5";
        public String actionID = "FODDER_WAVE";
        /** Interactions are called on right click, and pass on the parameters of the right click to the consumer. */
        public @Nullable String interactionID;

        public @Nullable Function<TileEntityLogicBlock, Boolean> condition;
        public @Nullable Consumer<TileEntityLogicBlock> action;
        /** World, block entity, x, y, z, player, side, hit x, hit y, hit z - in that order. */
        public @Nullable Consumer<Object[]> interaction;

        public @Nullable Player player;

        public ForgeDirection direction = ForgeDirection.UNKNOWN;

        boolean disguised = false;

        public TileEntityLogicBlock(BlockPos pos, BlockState state) {
            super(ModDungeonBlockEntities.LOGIC_BLOCK.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityLogicBlock tile) {
            if (tile.action == null) tile.action = LogicBlockActions.actions.get(tile.actionID);
            if (tile.condition == null) tile.condition = LogicBlockConditions.conditions.get(tile.conditionID);
            if (tile.interaction == null && tile.interactionID != null)
                tile.interaction = LogicBlockInteractions.interactions.get(tile.interactionID);

            if (tile.action == null || tile.condition == null) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                return;
            }
            tile.action.accept(tile);
            // the action may have replaced this block
            if (tile.isRemoved() || level.getBlockEntity(pos) != tile) return;
            if (tile.condition.apply(tile)) {
                tile.phase++;
                tile.timer = 0;
            } else {
                tile.timer++;
            }

            if (!tile.disguised) {
                tile.setChanged();
                level.sendBlockUpdated(pos, state, state, 3);
                tile.disguised = true;
            }
        }

        @Override
        protected void saveData(DataOut nbt) {
            super.saveData(nbt);
            nbt.putInt("phase", phase);
            nbt.putString("actionID", actionID);
            nbt.putString("conditionID", conditionID);
            if (interactionID != null) nbt.putString("interactionID", interactionID);
            SavedIds.putEnum(nbt, "direction", direction);
            if (disguise != null) SavedIds.putState(nbt, "disguise", disguise);
        }

        @Override
        protected void loadData(DataIn nbt) {
            super.loadData(nbt);
            this.phase = nbt.getIntOr("phase", 0);
            this.actionID = nbt.getStringOr("actionID", "FODDER_WAVE");
            this.conditionID = nbt.getStringOr("conditionID", "PLAYER_CUBE_5");
            String interactionId = nbt.getStringOr("interactionID", "");
            this.interactionID = interactionId.isEmpty() ? null : interactionId;
            this.direction = SavedIds.getEnum(nbt, "direction", ForgeDirection.UNKNOWN);
            BlockState state = SavedIds.getState(nbt, "disguise", null);
            this.disguise = state == null || state.isAir() ? null : state;
            // a changed script takes effect on the next tick
            this.action = null;
            this.condition = null;
            this.interaction = null;
        }
    }
}
