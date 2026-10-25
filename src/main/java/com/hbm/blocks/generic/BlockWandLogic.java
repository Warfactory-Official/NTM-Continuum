package com.hbm.blocks.generic;

import com.hbm.Constants;
import com.hbm.api.block.IToolable;
import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.config.ServerConfig;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.ICopiable;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.world.gen.nbt.ILegacyStructureTile;
import com.hbm.world.gen.nbt.INBTTileEntityTransformable;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import com.hbm.util.SavedIds;
import com.hbm.world.gen.util.LogicBlockActions;
import com.hbm.world.gen.util.LogicBlockConditions;
import com.hbm.world.gen.util.LogicBlockInteractions;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code BlockWandLogic} ({@code wand_logic}, "Structure Wand Block (Logic)"): marks a logic block (a scripted
 * trap / spawner) in an {@code .nbt} structure. Placed by a structure outside structure debug, or detonated, it turns
 * into {@code logic_block} (or {@code logic_block_invis} without a disguise) carrying its action, condition,
 * interaction, facing and disguise. Screwdriver / defuser / hand drill cycle the action / condition / interaction
 * lists (crouch steps back); a full-cube block in hand becomes the disguise.
 */
public class BlockWandLogic extends CompatBlock implements TickingBlock, ILookOverlay, IToolable, IBomb {

    public BlockWandLogic(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockWandLoot.ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockWandLoot.ROTATION);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWandLogic(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer == null) return;
        int i = Mth.floor(placer.getYRot() * 4.0F / 360.0F + 0.5D) & 3;
        level.setBlock(pos, state.setValue(BlockWandLoot.ROTATION, BlockWandLoot.quadrantMeta(placer)), 2);

        ForgeDirection dir = switch (i) {
            case 0 -> ForgeDirection.SOUTH;
            case 1 -> ForgeDirection.WEST;
            case 2 -> ForgeDirection.NORTH;
            default -> ForgeDirection.EAST;
        };
        if (level.getBlockEntity(pos) instanceof TileEntityWandLogic logic) logic.placedRotation = dir.ordinal();
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (stack.getItem() instanceof BlockItem item && !player.isShiftKeyDown()) {
            Block block = item.getBlock();

            // 1.7 renderAsNormalBlock: an opaque full cube
            if (Compat.isSolidRender(block.defaultBlockState()) && block != this) {
                if (level.getBlockEntity(pos) instanceof TileEntityWandLogic logic) {
                    if (!level.isClientSide()) {
                        logic.disguise = block.defaultBlockState();
                        logic.setChanged();
                    }
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandLogic logic)) return false;

        switch (tool) {
            case SCREWDRIVER -> logic.actionID = step(LogicBlockActions.getActionNames(), logic.actionID, player);
            case DEFUSER -> logic.conditionID = step(LogicBlockConditions.getConditionNames(), logic.conditionID, player);
            case HAND_DRILL -> logic.interactionID = step(LogicBlockInteractions.getInteractionNames(), logic.interactionID, player);
            default -> {
                return false;
            }
        }
        logic.setChanged();
        return true;
    }

    private static @Nullable String step(List<String> names, @Nullable String current, Player player) {
        if (names.isEmpty()) return current;
        int index = names.indexOf(current);
        index += player.isShiftKeyDown() ? -1 : 1;
        index = Mth.clamp(index, 0, names.size() - 1);
        return names.get(index);
    }

    @Override
    public @Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandLogic logic)) return null;

        List<String> text = new ArrayList<>();
        text.add("Action: " + logic.actionID);
        text.add("Condition: " + logic.conditionID);
        text.add("Interaction: " + (logic.interactionID != null ? logic.interactionID : "None"));

        String block;
        if (logic.disguise != null && !logic.disguise.isAir()) block = logic.disguise.getBlock().getName().getString();
        else block = "None";

        text.add("Disguise Block: " + block);

        return new Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandLogic logic)) return null;
        logic.triggerReplace = true;
        return BombReturnCode.TRIGGERED;
    }

    public static class TileEntityWandLogic extends TileEntityLoadedBase implements INBTTileEntityTransformable, ICopiable, ILegacyStructureTile {

        private boolean triggerReplace;

        public int placedRotation;

        @Nullable BlockState disguise;

        public String actionID = "FODDER_WAVE";
        public String conditionID = "PLAYER_CUBE_5";
        public @Nullable String interactionID;

        public TileEntityWandLogic(BlockPos pos, BlockState state) {
            super(ModStructureWandBlocks.WAND_LOGIC_ENTITY.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityWandLogic tile) {
            if (tile.triggerReplace) {
                // On the first tick of this TE, replace with intended block
                tile.replace();
            } else {
                tile.networkPackNT();
            }
        }

        private void replace() {
            Level world = level;
            BlockPos pos = worldPosition;
            if (world == null || world.isClientSide()) return;
            if (!(world.getBlockState(pos).getBlock() instanceof BlockWandLogic)) {
                Constants.LOG.warn("Somehow the block at: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " isn't a logic block but we're doing a TE update as if it is, cancelling!");
                return;
            }

            // copy before the block (and this block entity) goes away
            String action = actionID, condition = conditionID, interaction = interactionID;
            int rotation = placedRotation;
            BlockState disguiseState = disguise;

            world.setBlock(pos, (disguiseState == null ? ModDungeonBlocks.logic_block_invis : ModDungeonBlocks.logic_block).get().defaultBlockState(), 3);

            BlockEntity te = world.getBlockEntity(pos);

            if (te == null || te instanceof TileEntityWandLogic) {
                Constants.LOG.warn("TE for logic block set incorrectly at: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ". If you're using some sort of world generation mod, report it to the author!");
                return;
            }

            if (te instanceof LogicBlock.TileEntityLogicBlock logic) {
                logic.actionID = action;
                logic.conditionID = condition;
                logic.interactionID = interaction;
                logic.direction = ForgeDirection.getOrientation(rotation);
                logic.disguise = disguiseState;
                logic.setChanged();
            }
        }

        @Override
        public void transformTE(LevelAccessor level, int coordBaseMode) {
            triggerReplace = !ServerConfig.STRUCTURE_DEBUG.get();
        }

        @Override
        protected void saveData(DataOut out) {
            super.saveData(out);
            out.putString("actionID", actionID);
            out.putString("conditionID", conditionID);
            if (interactionID != null) out.putString("interactionID", interactionID);
            SavedIds.putEnum(out, "rotation", ForgeDirection.getOrientation(placedRotation));
            if (disguise != null) SavedIds.putState(out, "disguise", disguise);
            out.putBoolean("trigger", triggerReplace);
        }

        @Override
        protected void loadData(DataIn in) {
            super.loadData(in);
            actionID = in.getStringOr("actionID", "");
            conditionID = in.getStringOr("conditionID", "");
            String interaction = in.getStringOr("interactionID", "");
            interactionID = interaction.isEmpty() ? null : interaction;
            placedRotation = SavedIds.getEnum(in, "rotation", ForgeDirection.DOWN).ordinal();
            BlockState state = SavedIds.getState(in, "disguise", null);
            disguise = state == null || state.isAir() ? null : state;
            triggerReplace = in.getBooleanOr("trigger", false);
        }

        /** The 1.7 keys; {@code disguise} is a 1.7 registry name with {@code disguiseMeta} its metadata. */
        @Override
        public void readLegacyStructureNBT(CompoundTag nbt) {
            actionID = Tags.getStringOr(nbt, "actionID", "");
            conditionID = Tags.getStringOr(nbt, "conditionID", "");
            if (Tags.contains(nbt, "interactionID")) interactionID = Tags.getStringOr(nbt, "interactionID", "");
            placedRotation = Tags.getIntOr(nbt, "rotation", 0);
            if (Tags.contains(nbt, "disguise")) {
                BlockState state = LegacyStructurePalette.legacyState(Tags.getStringOr(nbt, "disguise", ""),
                        Tags.getIntOr(nbt, "disguiseMeta", 0));
                disguise = state.isAir() ? null : state;
            }
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeInt(placedRotation);
            buf.writeUtf(actionID);
            buf.writeUtf(conditionID);
            buf.writeUtf(interactionID == null ? "" : interactionID);
            buf.writeInt(disguise == null ? -1 : Block.getId(disguise));
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            placedRotation = buf.readInt();
            actionID = buf.readUtf();
            conditionID = buf.readUtf();
            String interaction = buf.readUtf();
            interactionID = interaction.isEmpty() ? null : interaction;
            int id = buf.readInt();
            disguise = id < 0 ? null : Block.stateById(id);
        }

        @Override
        public @Nullable CompoundTag getSettings(Level level, BlockPos pos) {
            CompoundTag nbt = new CompoundTag();
            nbt.putString("actionID", actionID);
            nbt.putString("conditionID", conditionID);
            if (interactionID != null) nbt.putString("interactionID", interactionID);
            SavedIds.putEnum(nbt, "rotation", ForgeDirection.getOrientation(placedRotation));
            if (disguise != null) SavedIds.putState(nbt, "disguise", disguise);
            return nbt;
        }

        @Override
        public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
            actionID = Tags.getStringOr(nbt, "actionID", "");
            conditionID = Tags.getStringOr(nbt, "conditionID", "");
            interactionID = Tags.getStringOr(nbt, "interactionID", "");
            // port: 1.7 pasted the rotation from the disguise's metadata; the disguise is a block state here
            placedRotation = SavedIds.getEnum(nbt, "rotation", ForgeDirection.DOWN).ordinal();
            if (Tags.contains(nbt, "disguise")) {
                BlockState state = SavedIds.getState(nbt, "disguise", null);
                disguise = state == null || state.isAir() ? null : state;
            }
            setChanged();
        }
    }
}
