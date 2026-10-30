package com.hbm.blocks.machine.rbmk;

import com.hbm.api.block.IToolable;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyConnectorMK2;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.handler.BossSpawnHandler;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKNeutronNode;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModRBMKItems;
import com.hbm.items.machine.ItemRBMKLid;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.IRBMKTicking;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import dev.golem.core.gui.Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Cells are this column's own block, preserving its lid and top-part state. */
public abstract class RBMKBase extends BlockMultiblockCore implements IFluidConnectorBlockMK2, IEnergyConnectorBlock, IToolable {
    public static final EnumProperty<Lid> LID = EnumProperty.create("lid", Lid.class);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final Direction DIR_NO_LID = Direction.NORTH;
    private static final int MAX_ABOVE = 15;
    public static boolean digamma;
    /** Cleared while a meltdown runs so the lids burn with the columns. */
    public static boolean dropLids = true;
    protected RBMKBase(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(LID, Lid.NONE).setValue(PART, Part.CORE)); }
    private static int above() { return RBMKDials.getColumnHeight(null); }
    public static Lid lidOf(BlockState state) { return state.hasProperty(LID) ? state.getValue(LID) : Lid.NONE; }
    private static @Nullable BlockPos coreOf(Level level, BlockPos pos, BlockState state) { return state.getValue(PART) == Part.CORE ? pos : MultiblockSurface.coreOfCell(level, pos); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(LID, PART); }
    @Override public int[] getDimensions() { return new int[]{above(), 0, 0, 0, 0, 0}; }
    @Override protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) { int above = above(); for (int y = 1; y <= above; y++) visitor.cell(core.above(y), y == above ? topMask() : MASK_NONE); }
    protected int topMask() { return MASK_NONE; }
    @Override protected void visitTeardownCells(ServerLevel level, BlockPos core, Direction facing, CellVisitor visitor) { for (int y = 1; y <= MAX_ABOVE; y++) visitor.cell(core.above(y), MASK_NONE); }
    @Override protected BlockState cellStateFor(int lx, int ly, int lz, Direction facing, int shapeId) { return defaultBlockState().setValue(PART, ly >= above() ? Part.TOP : Part.MIDDLE); }
    @Override public boolean isCellState(BlockState state) { return state.getValue(PART) != Part.CORE; }
    @Override public int getOffset() { return 0; }
    @Override public Direction getDirModified(Direction dir) { return DIR_NO_LID; }
    @Override public BlockState rotate(BlockState state, Rotation rotation) { return state; }
    @Override public BlockState mirror(BlockState state, Mirror mirror) { return state; }
    @Override public int coreMask() { return MASK_NONE; }
    public boolean hasOwnLid() { return false; }
    protected abstract BlockEntityType<? extends BlockEntityRBMKBase> beType();
    protected abstract BlockEntityRBMKBase createCore(BlockPos pos, BlockState state);
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(PART) == Part.CORE ? createCore(pos, state) : null; }
    @Override public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        if (!(world instanceof Level level) || dir == ForgeDirection.UNKNOWN) return false;
        BlockPos surface = new BlockPos(x, y, z), core = MultiblockSurface.coreOfAny(level, surface);
        if (core == null || !MultiblockSurface.isOpen(level, surface, level.getBlockState(surface), dir.toDirection())) return false;
        BlockEntity blockEntity = level.getBlockEntity(core);
        return blockEntity instanceof IFluidReceiverMK2 || blockEntity instanceof IFluidProviderMK2;
    }
    @Override public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        if (!(world instanceof Level level) || dir == ForgeDirection.UNKNOWN) return false;
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core == null || !MultiblockSurface.isOpen(level, pos, level.getBlockState(pos), dir.toDirection())) return false;
        return level.getBlockEntity(core) instanceof IEnergyConnectorMK2 energy && energy.canConnect(dir);
    }
    @Override protected void removedAt(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (state.getValue(PART) != Part.CORE) { BlockMultiblockCore.foldedCellRemoved(level, pos, movedByPiston); return; }
        if (dropLids && !movedByPiston && !hasOwnLid() && lidOf(state).present()) Block.popResource(level, pos.above(above()), lidItem(lidOf(state)));
        super.removedAt(state, level, pos, movedByPiston);
    }
    public @Nullable BlockPos rigidStructureCore(Level level, BlockPos member) { BlockState state = level.getBlockState(member); return state.getBlock() instanceof RBMKBase ? coreOf(level, member, state) : null; }
    @Override protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { BlockPos core = coreOf(level, pos, state); return core == null ? 0 : super.onAnalogOutputSignal(level.getBlockState(core), level, core); }
    @Override protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (this instanceof RBMKRod || this instanceof RBMKStorage) BossSpawnHandler.markFBI(player); // 1.7: rods and storage mark on any use
        BlockPos core = coreOf(level, pos, state); if (core == null) return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockState coreState = level.getBlockState(core);
        // 1.7 openInv: a lid in hand on a lidless column is the lid's click (ItemRBMKLid.onItemUse), not the GUI's.
        if (player.getMainHandItem().getItem() instanceof ItemRBMKLid && !hasOwnLid() && !lidOf(coreState).present()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown() && !hasOwnLid() && takeLid(level, core, coreState)) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(core) instanceof MenuProvider menu && player instanceof ServerPlayer serverPlayer) { Menus.openWithPos(serverPlayer, menu, core); return InteractionResult.SUCCESS; }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
    private static ItemStack lidItem(Lid lid) { return new ItemStack(lid == Lid.GLASS ? ModRBMKItems.rbmk_lid_glass.get() : ModRBMKItems.rbmk_lid.get()); }
    private boolean takeLid(Level level, BlockPos core, BlockState coreState) {
        Lid lid = lidOf(coreState);
        if (!lid.present()) return false;
        if (level instanceof ServerLevel server) writeLid(server, core, coreState, Lid.NONE);
        else level.setBlock(core, coreState.setValue(LID, Lid.NONE), 3);
        if (NeutronNodeWorld.getNode(level, core) instanceof RBMKNeutronNode node) node.removeLid();
        Block.popResource(level, core.above(above()), lidItem(lid));
        return true;
    }

    /**
     * Set the column's lid, core and cells alike. The column renderers draw it from the core's state.
     * Template assembly calls this through {@link #assembled}.
     */
    public void writeLid(ServerLevel level, BlockPos core, BlockState coreState, Lid lid) {
        level.setBlock(core, coreState.setValue(LID, lid), 3);
        visitTeardownCells(level, core, coreState.getValue(FACING), (pos, mask) -> {
            BlockState cell = level.getBlockState(pos);
            if (cell.getBlock() == this && isCellState(cell)) level.setBlock(pos.immutable(), cell.setValue(LID, lid), 3);
        });
    }

    // 1.7 RBMKBase.metaToLid: a template core's meta carries its lid, which the cells then answer.
    @Override
    public void assembled(LevelAccessor level, BlockPos core, BlockState coreState,
                          @Nullable BoundingBox clip) {
        Lid lid = lidOf(coreState);
        if (!lid.present()) return;
        visitPlacedCells(level, core, coreState.getValue(FACING), (pos, mask) -> {
            if (clip != null && !clip.isInside(pos)) return;
            BlockState cell = level.getBlockState(pos);
            if (cell.getBlock() == this && isCellState(cell)) level.setBlock(pos, cell.setValue(LID, lid), 3);
        });
    }
    /** 1.7 onScrew: the screwdriver lifts the lid off a column that carries one. */
    @Override public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER || hasOwnLid()) return false;
        BlockPos core = coreOf(level, pos, level.getBlockState(pos));
        if (core == null || !lidOf(level.getBlockState(core)).present()) return false;
        return level.isClientSide() || takeLid(level, core, level.getBlockState(core));
    }
    public enum Lid implements StringRepresentable { NONE("none"), CONCRETE("concrete"), GLASS("glass"); private final String name; Lid(String name) { this.name = name; } @Override public String getSerializedName() { return name; } public boolean present() { return this != NONE; } }
    public enum Part implements StringRepresentable { CORE("core"), MIDDLE("middle"), TOP("top"); private final String name; Part(String name) { this.name = name; } @Override public String getSerializedName() { return name; } }
}
