package com.hbm.blocks.generic;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.world.gen.nbt.ILegacyStructureTile;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.7 {@code BlockWandJigsaw} ({@code wand_jigsaw}, "Structure Wand Block (Jigsaw)"): a jigsaw connection point in
 * an {@code .nbt} structure piece. Its front faces the way a connecting piece extends; it names the pool to draw the
 * next piece from, its own connection name, the connection name it targets, the selection and placement
 * priorities and the joint type. A block in hand sets what it turns into once the structure is built; otherwise a
 * right-click opens {@code GUIWandJigsaw}. The structure wand passes through, so a box can be saved with it.
 */
public class BlockWandJigsaw extends CompatBlock implements TickingBlock, ILookOverlay {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    /** Client hook that opens {@code GUIWandJigsaw}; installed by the client registrations. */
    public static Consumer<TileEntityWandJigsaw> OPEN_SCREEN = jigsaw -> {};

    public BlockWandJigsaw(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWandJigsaw(pos, state);
    }

    /** 1.7 {@code BlockPistonBase.determineOrientation}: the face pointing back at the placer. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandJigsaw jigsaw)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!player.isShiftKeyDown()) {
            Block block = ModStructureWandBlocks.getBlockFromStack(held);
            if (block == ModStructureWandBlocks.wand_air.get()) block = Blocks.AIR;

            if (block != null && !ModStructureWandBlocks.isStructureBlock(block, false)) {
                if (!level.isClientSide()) {
                    jigsaw.replaceState = block.defaultBlockState();
                    jigsaw.replaceMeta = com.hbm.items.LegacyMeta.of(held);
                    jigsaw.setChanged();
                }
                return InteractionResult.SUCCESS;
            }

            if (held.is(com.hbm.items.ModWandItems.wand_s.get())) return InteractionResult.TRY_WITH_EMPTY_HAND;

            if (level.isClientSide()) OPEN_SCREEN.accept(jigsaw);

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public @Nullable Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandJigsaw jigsaw)) return null;

        List<String> text = new ArrayList<>();

        text.add(ChatFormatting.GRAY + "Target pool: " + ChatFormatting.RESET + jigsaw.pool);
        text.add(ChatFormatting.GRAY + "Name: " + ChatFormatting.RESET + jigsaw.name);
        text.add(ChatFormatting.GRAY + "Target name: " + ChatFormatting.RESET + jigsaw.target);
        text.add(ChatFormatting.GRAY + "Turns into: " + ChatFormatting.RESET + BuiltInRegistries.BLOCK.getKey(jigsaw.replaceState.getBlock()));
        text.add(ChatFormatting.GRAY + "   with meta: " + ChatFormatting.RESET + jigsaw.replaceMeta);
        text.add(ChatFormatting.GRAY + "Selection/Placement priority: " + ChatFormatting.RESET + jigsaw.selectionPriority + "/" + jigsaw.placementPriority);
        text.add(ChatFormatting.GRAY + "Joint type: " + ChatFormatting.RESET + (jigsaw.isRollable ? "Rollable" : "Aligned"));

        return new Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    public static class TileEntityWandJigsaw extends TileEntityLoadedBase implements IControlReceiver, ILegacyStructureTile {

        public int selectionPriority = 0; // higher priority = this jigsaw block is selected first for generation
        public int placementPriority = 0; // higher priority = children of this jigsaw block are checked for jigsaw blocks of their own and selected first
        public String pool = "default";
        public String name = "default";
        public String target = "default";
        public BlockState replaceState = Blocks.AIR.defaultBlockState();
        public int replaceMeta = 0;
        public boolean isRollable = true; // sets joint type, rollable joints can be placed in any orientation for vertical jigsaw connections

        public TileEntityWandJigsaw(BlockPos pos, BlockState state) {
            super(ModStructureWandBlocks.WAND_JIGSAW_ENTITY.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityWandJigsaw tile) {
            tile.networkPackNT();
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeInt(selectionPriority);
            buf.writeInt(placementPriority);
            buf.writeUtf(pool);
            buf.writeUtf(name);
            buf.writeUtf(target);
            buf.writeInt(Block.getId(replaceState));
            buf.writeInt(replaceMeta);
            buf.writeBoolean(isRollable);
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            selectionPriority = buf.readInt();
            placementPriority = buf.readInt();
            pool = buf.readUtf();
            name = buf.readUtf();
            target = buf.readUtf();
            replaceState = Block.stateById(buf.readInt());
            replaceMeta = buf.readInt();
            isRollable = buf.readBoolean();
        }

        /**
         * 1.7's keys; {@code direction} is the ForgeDirection the jigsaw faces and {@code block}/{@code meta} what
         * it becomes. A port-saved structure also stores {@code stateString}, since a flattened block has no metadata.
         */
        @Override
        protected void saveData(DataOut out) {
            super.saveData(out);
            out.putInt("direction", getBlockState().getValue(FACING).get3DDataValue());
            out.putInt("selection", selectionPriority);
            out.putInt("placement", placementPriority);
            out.putString("pool", pool);
            out.putString("name", name);
            out.putString("target", target);
            out.putString("block", String.valueOf(BuiltInRegistries.BLOCK.getKey(replaceState.getBlock())));
            out.putInt("meta", replaceMeta);
            out.putBoolean("roll", isRollable);
            out.putString("stateString", LegacyStructurePalette.stateString(replaceState));
        }

        @Override
        protected void loadData(DataIn in) {
            super.loadData(in);
            selectionPriority = in.getIntOr("selection", 0);
            placementPriority = in.getIntOr("placement", 0);
            pool = in.getStringOr("pool", "");
            name = in.getStringOr("name", "");
            target = in.getStringOr("target", "");
            replaceState = LegacyStructurePalette.parseState(in.getStringOr("stateString", in.getStringOr("block", "")));
            replaceMeta = in.getIntOr("meta", 0);
            isRollable = in.getBooleanOr("roll", false);
        }

        @Override
        public void readLegacyStructureNBT(CompoundTag nbt) {
            selectionPriority = Tags.getIntOr(nbt, "selection", 0);
            placementPriority = Tags.getIntOr(nbt, "placement", 0);
            pool = Tags.getStringOr(nbt, "pool", "");
            name = Tags.getStringOr(nbt, "name", "");
            target = Tags.getStringOr(nbt, "target", "");
            replaceMeta = Tags.getIntOr(nbt, "meta", 0);
            replaceState = LegacyStructurePalette.legacyState(Tags.getStringOr(nbt, "block", ""), replaceMeta);
            isRollable = Tags.getBooleanOr(nbt, "roll", false);
        }

        @Override
        public boolean hasPermission(Player player) {
            return true;
        }

        /** The GUI's fields (1.7 sent the whole tile NBT with its edits; only these can change there). */
        @Override
        public void receiveControl(CompoundTag nbt) {
            if (Tags.contains(nbt, "selection")) selectionPriority = Tags.getIntOr(nbt, "selection", 0);
            if (Tags.contains(nbt, "placement")) placementPriority = Tags.getIntOr(nbt, "placement", 0);
            pool = Tags.getStringOr(nbt, "pool", pool);
            name = Tags.getStringOr(nbt, "name", name);
            target = Tags.getStringOr(nbt, "target", target);
            isRollable = Tags.getBooleanOr(nbt, "roll", isRollable);
            setChanged();
        }
    }
}
