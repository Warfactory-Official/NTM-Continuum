package com.hbm.blocks.generic;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.world.gen.nbt.ILegacyStructureTile;
import com.hbm.world.gen.nbt.JigsawPiece;
import com.hbm.world.gen.nbt.JigsawPool;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import com.hbm.world.gen.nbt.NBTStructure;
import com.hbm.world.gen.nbt.NBTStructure.JigsawConnection;
import com.hbm.world.gen.nbt.SpawnCondition;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.7 {@code BlockWandTandem} ({@code wand_tandem}): a delayed jigsaw connection. Built as part of a registered
 * structure it is armed with that structure; each tick it then draws a piece from its pool, builds it against its
 * front face and turns into its configured block ("make a hole with the first charge, then deliver the payload").
 * A paper copies its settings to the paper, or from a paper that already carries them; a block in hand sets the
 * replacement; otherwise a right-click opens {@code GUIWandTandem}.
 *
 * <p>1.7 armed every tandem a plain {@code build} placed with the structure named {@code null}, which has no
 * spawn condition, and then dereferenced it on the next tick; here an unknown structure simply never fires. No
 * structure is registered in this port (world generation is not ported), so tandems stay inert placeholders.
 */
public class BlockWandTandem extends CompatBlock implements TickingBlock, ILookOverlay {

    /** Client hook that opens {@code GUIWandTandem}; installed by the client registrations. */
    public static Consumer<TileEntityWandTandem> OPEN_SCREEN = tandem -> {};

    public BlockWandTandem(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BlockWandJigsaw.FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockWandJigsaw.FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWandTandem(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockWandJigsaw.FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockWandJigsaw.FACING, rotation.rotate(state.getValue(BlockWandJigsaw.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockWandJigsaw.FACING)));
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandTandem jigsaw)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (held.is(Items.PAPER)) {
            if (!level.isClientSide()) {
                var data = ModDataComponents.PERSISTENT_DATA.get(held);
                if (data.isEmpty()) {
                    CompoundTag tag = new CompoundTag();
                    jigsaw.writeCopy(tag);
                    ModDataComponents.PERSISTENT_DATA.set(held, tag);
                } else {
                    jigsaw.readCopy(data.get());
                    jigsaw.setChanged();
                }
            }
            return InteractionResult.SUCCESS;
        }

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
        if (!(level.getBlockEntity(pos) instanceof TileEntityWandTandem jigsaw)) return null;

        List<String> text = new ArrayList<>();

        text.add(ChatFormatting.GRAY + "Target pool: " + ChatFormatting.RESET + jigsaw.pool);
        text.add(ChatFormatting.GRAY + "Target name: " + ChatFormatting.RESET + jigsaw.target);
        text.add(ChatFormatting.GRAY + "Turns into: " + ChatFormatting.RESET + BuiltInRegistries.BLOCK.getKey(jigsaw.replaceState.getBlock()));
        text.add(ChatFormatting.GRAY + "   with meta: " + ChatFormatting.RESET + jigsaw.replaceMeta);
        text.add(ChatFormatting.GRAY + "Joint type: " + ChatFormatting.RESET + (jigsaw.isRollable ? "Rollable" : "Aligned"));

        return new Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    public static class TileEntityWandTandem extends TileEntityLoadedBase implements IControlReceiver, ILegacyStructureTile {

        public String pool = "default";
        public String target = "default";
        public BlockState replaceState = Blocks.AIR.defaultBlockState();
        public int replaceMeta = 0;
        public boolean isRollable = true; // sets joint type, rollable joints can be placed in any orientation for vertical jigsaw connections

        private boolean isArmed = false;
        private @Nullable SpawnCondition structure;
        /** The armed structure's name, kept even while no such structure is registered. */
        private String structureName = "";

        public TileEntityWandTandem(BlockPos pos, BlockState state) {
            super(ModStructureWandBlocks.WAND_TANDEM_ENTITY.get(), pos, state);
        }

        public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityWandTandem tile) {
            tile.tryGenerate();
            tile.networkPackNT();
        }

        private void tryGenerate() {
            if (!this.isArmed || structure == null || target == null || target.isEmpty() || pool == null || pool.isEmpty()) return;
            Level world = level;
            if (world == null) return;

            JigsawPool jigsawPool = structure.getPool(this.pool);
            if (jigsawPool == null) return;

            JigsawPiece nextPiece = jigsawPool.get(world.getRandom());
            if (nextPiece == null) return;

            ForgeDirection dir = ForgeDirection.getOrientation(getBlockState().getValue(BlockWandJigsaw.FACING).get3DDataValue());

            List<JigsawConnection> connectionPool = nextPiece.structure.getConnectionPool(dir, target);
            if (connectionPool == null || connectionPool.isEmpty()) return;

            JigsawConnection toConnection = connectionPool.get(world.getRandom().nextInt(connectionPool.size()));
            int nextCoordBase = directionOffsetToCoordBase(dir.getOpposite(), toConnection.dir);

            BlockPos pos = worldPosition.offset(dir.offsetX, dir.offsetY, dir.offsetZ);

            // offset the starting point to the connecting point
            int ox = nextPiece.structure.rotateX(toConnection.x, toConnection.z, nextCoordBase);
            int oy = toConnection.y;
            int oz = nextPiece.structure.rotateZ(toConnection.x, toConnection.z, nextCoordBase);

            nextPiece.structure.build(world, nextPiece, pos.getX() - ox, pos.getY() - oy, pos.getZ() - oz, nextCoordBase, structure.name);

            world.setBlock(worldPosition, replaceState, 2);
        }

        private int directionOffsetToCoordBase(ForgeDirection from, ForgeDirection to) {
            for (int i = 0; i < 4; i++) {
                if (from == to) return i % 4;
                from = from.getRotation(ForgeDirection.DOWN);
            }
            return 0;
        }

        @Override
        public void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeUtf(pool);
            buf.writeUtf(target);
            buf.writeInt(Block.getId(replaceState));
            buf.writeInt(replaceMeta);
            buf.writeBoolean(isRollable);
        }

        @Override
        public void deserialize(RegistryFriendlyByteBuf buf) {
            pool = buf.readUtf();
            target = buf.readUtf();
            replaceState = Block.stateById(buf.readInt());
            replaceMeta = buf.readInt();
            isRollable = buf.readBoolean();
        }

        /** 1.7 {@code copyMode}: only the settings a paper carries. */
        void writeCopy(CompoundTag nbt) {
            nbt.putString("pool", pool);
            nbt.putString("target", target);
            nbt.putString("block", LegacyStructurePalette.stateString(replaceState));
            nbt.putInt("meta", replaceMeta);
            nbt.putBoolean("roll", isRollable);
        }

        void readCopy(CompoundTag nbt) {
            pool = Tags.getStringOr(nbt, "pool", "");
            target = Tags.getStringOr(nbt, "target", "");
            replaceState = LegacyStructurePalette.parseState(Tags.getStringOr(nbt, "block", ""));
            replaceMeta = Tags.getIntOr(nbt, "meta", 0);
            isRollable = Tags.getBooleanOr(nbt, "roll", false);
        }

        @Override
        protected void saveData(DataOut out) {
            super.saveData(out);
            out.putInt("direction", getBlockState().getValue(BlockWandJigsaw.FACING).get3DDataValue());
            if (isArmed) {
                out.putBoolean("isArmed", true);
                out.putString("structure", structure != null ? structure.name : structureName);
            }
            out.putString("pool", pool);
            out.putString("target", target);
            out.putString("block", LegacyStructurePalette.stateString(replaceState));
            out.putInt("meta", replaceMeta);
            out.putBoolean("roll", isRollable);
        }

        @Override
        protected void loadData(DataIn in) {
            super.loadData(in);
            isArmed = in.getBooleanOr("isArmed", false);
            structureName = in.getStringOr("structure", "");
            structure = NBTStructure.getStructure(structureName);
            pool = in.getStringOr("pool", "");
            target = in.getStringOr("target", "");
            replaceState = LegacyStructurePalette.parseState(in.getStringOr("block", ""));
            replaceMeta = in.getIntOr("meta", 0);
            isRollable = in.getBooleanOr("roll", false);
        }

        @Override
        public void readLegacyStructureNBT(CompoundTag nbt) {
            isArmed = Tags.getBooleanOr(nbt, "isArmed", false);
            structureName = Tags.getStringOr(nbt, "structure", "");
            structure = NBTStructure.getStructure(structureName);
            pool = Tags.getStringOr(nbt, "pool", "");
            target = Tags.getStringOr(nbt, "target", "");
            replaceMeta = Tags.getIntOr(nbt, "meta", 0);
            replaceState = LegacyStructurePalette.legacyState(Tags.getStringOr(nbt, "block", ""), replaceMeta);
            isRollable = Tags.getBooleanOr(nbt, "roll", false);
        }

        @Override
        public boolean hasPermission(Player player) {
            return true;
        }

        @Override
        public void receiveControl(CompoundTag nbt) {
            pool = Tags.getStringOr(nbt, "pool", pool);
            target = Tags.getStringOr(nbt, "target", target);
            isRollable = Tags.getBooleanOr(nbt, "roll", isRollable);
            setChanged();
        }

        public void arm(@Nullable SpawnCondition structure) {
            isArmed = true;
            this.structure = structure;
            this.structureName = structure == null ? "" : structure.name;
        }
    }
}
