package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IInsertable;
import com.hbm.api.block.IToolable;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModFuelItems;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.special.ItemCell;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import com.hbm.tileentity.machine.pile.TileEntityPileBase;
import com.hbm.tileentity.machine.pile.TileEntityPileNeutronDetector;

/**
 * The legacy (deprecated upstream, still functional) drilled graphite pile block. 1.7 metadata was
 * {@code cfg (bits 0-1: 0 = Y, 1 = Z, 2 = X channel axis) | 4 (aluminium shroud) | 8 (per-block extra)};
 * here that is {@link #AXIS}, {@link #ALUMINUM} and {@link #OUT}.
 */
@Deprecated
public abstract class BlockGraphiteDrilledBase extends dev.golem.core.world.CompatBlock implements IToolable, IInsertable {

    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    /** The old meta bit 4: shrouded in aluminium (Windscale). */
    public static final BooleanProperty ALUMINUM = BooleanProperty.create("aluminum");
    /** The old meta bit 8: rod retracted / detector tripped / fuel bred rich. */
    public static final BooleanProperty OUT = BooleanProperty.create("out");

    protected BlockGraphiteDrilledBase(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y)
                .setValue(ALUMINUM, false).setValue(OUT, false));
    }

    /** A pillar: a quarter turn swaps the horizontal axes, a half turn or a mirror leaves it alone. */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> switch (state.getValue(AXIS)) {
                case X -> state.setValue(AXIS, Direction.Axis.Z);
                case Z -> state.setValue(AXIS, Direction.Axis.X);
                default -> state;
            };
            default -> state;
        };
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, ALUMINUM, OUT);
    }

    /** 1.7 {@code side == cfg * 2 || side == cfg * 2 + 1}. */
    public static boolean isChannelFace(BlockState state, Direction side) {
        return side.getAxis() == state.getValue(AXIS);
    }

    /** 1.7 {@code side / 2} turned into the channel axis. */
    public static Direction.Axis axisOf(Direction side) {
        return side.getAxis();
    }

    public static BlockState withChannel(Block block, BlockState from) {
        return block.defaultBlockState().setValue(AXIS, from.getValue(AXIS)).setValue(ALUMINUM, from.getValue(ALUMINUM));
    }

    protected static void ejectItem(Level level, BlockPos pos, Direction dir, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity dust = new ItemEntity(level, pos.getX() + 0.5D + dir.getStepX() * 0.75D,
                pos.getY() + 0.5D + dir.getStepY() * 0.75D, pos.getZ() + 0.5D + dir.getStepZ() * 0.75D, stack);
        dust.setDeltaMovement(dir.getStepX() * 0.25, dir.getStepY() * 0.25, dir.getStepZ() * 0.25);
        level.addFreshEntity(dust);
    }

    protected static ItemStack aluminumShell() {
        Item shell = ModAutogenItems.get(MaterialShapes.SHELL, Mats.MAT_ALUMINIUM);
        return shell == null ? ItemStack.EMPTY : new ItemStack(shell);
    }

    protected static boolean isAluminumShell(ItemStack stack) {
        Item shell = ModAutogenItems.get(MaterialShapes.SHELL, Mats.MAT_ALUMINIUM);
        return shell != null && stack.getItem() == shell;
    }

    /** The old drilled-block drop: it never dropped itself. */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel && !playerPreventsBlockDrops(player)) {
            for (ItemStack drop : getDrops(state)) Block.popResource(level, pos, drop);
        }
    }

    /** 1.7 {@code getDrops}: eight graphite ingots, the shroud if any, and whatever rod is inside. */
    public List<ItemStack> getDrops(BlockState state) {
        List<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(ModFuelItems.ingot_graphite.get(), 8));
        if (state.getValue(ALUMINUM)) drops.add(aluminumShell());
        ItemStack inserted = getInsertedItem(state);
        if (!inserted.isEmpty()) drops.add(inserted);
        return drops;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;

        if (!level.isClientSide()) {
            BlockState state = level.getBlockState(pos);
            if (isChannelFace(state, side)) {
                level.setBlock(pos, withChannel(ModNuclearMiscBlocks.BLOCK_GRAPHITE_DRILLED.get(), state), 3);
                ejectItem(level, pos, side, getInsertedItem(state));
            }
        }
        return true;
    }

    /** Thank god getDrops passes meta. */
    protected ItemStack getInsertedItem(BlockState state) {
        return getInsertedItem();
    }

    protected ItemStack getInsertedItem() {
        return ItemStack.EMPTY;
    }

    /** The block (with the OUT bit the rod wants) that inserting {@code stack} produces, or null. */
    protected @Nullable BlockState checkInteractions(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModNuclearMiscItems.PILE_ROD_URANIUM.get()) return ModNuclearMiscBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState();
        if (item == ModNuclearMiscItems.PILE_ROD_PU239.get()) return ModNuclearMiscBlocks.BLOCK_GRAPHITE_FUEL.get().defaultBlockState().setValue(OUT, true);
        if (item == ModNuclearMiscItems.PILE_ROD_PLUTONIUM.get()) return ModNuclearMiscBlocks.BLOCK_GRAPHITE_PLUTONIUM.get().defaultBlockState();
        if (item == ModNuclearMiscItems.PILE_ROD_SOURCE.get()) return ModNuclearMiscBlocks.BLOCK_GRAPHITE_SOURCE.get().defaultBlockState();
        if (item == ModNuclearMiscItems.PILE_ROD_BORON.get()) return ModNuclearMiscBlocks.BLOCK_GRAPHITE_ROD.get().defaultBlockState();
        if (item == ModNuclearMiscItems.PILE_ROD_LITHIUM.get()) return ModNuclearMiscBlocks.BLOCK_GRAPHITE_LITHIUM.get().defaultBlockState();
        if (ItemCell.isFullCell(stack, Fluids.TRITIUM)) return ModNuclearMiscBlocks.BLOCK_GRAPHITE_TRITIUM.get().defaultBlockState();
        if (item == ModNuclearMiscItems.PILE_ROD_DETECTOR.get()) return ModNuclearMiscBlocks.BLOCK_GRAPHITE_DETECTOR.get().defaultBlockState();
        return null;
    }

    /** One rod pushed in from the channel face shoves the chain along by one, up to three deep. */
    @Override
    public boolean insertItem(Level level, BlockPos pos, Direction dir, ItemStack stack) {
        if (stack.isEmpty()) return false;

        BlockState baseBlock = checkInteractions(stack);
        if (baseBlock == null) return false;

        BlockState here = level.getBlockState(pos);
        Direction.Axis pureAxis = here.getValue(AXIS); // in case it's shrouded in aluminum

        if (dir.getAxis() != pureAxis) return false;

        // first, make sure we can even push rods out
        for (int i = 0; i <= 3; i++) { // limited to 3 boyos
            BlockPos ipos = pos.relative(dir, i);
            BlockState istate = level.getBlockState(ipos);

            if (istate.getBlock() instanceof BlockGraphiteDrilledBase base) {
                if (istate.getValue(AXIS) != pureAxis) return false; // wrong orientation
                if (base.getInsertedItem(istate).isEmpty()) break; // if there's nothing to push
                else if (i >= 3) return false; // if there is stuff to push and we reach our limit
            } else {
                if (dev.golem.core.world.Compat.isSolidRender(istate, level, ipos)) return false; // obstructions
                else break; // empty space? no need to search
            }
        }

        BlockState oldState = baseBlock.setValue(AXIS, pureAxis);
        CompoundTag oldTag = new CompoundTag(); // In case of TEs

        // now actually make the change
        for (int i = 0; i <= 3; i++) { // yeah yeah we know it's safe but let's be *extra cautious* of infinite loops
            BlockPos ipos = pos.relative(dir, i);
            BlockState newState = level.getBlockState(ipos);

            if (newState.getBlock() instanceof BlockGraphiteDrilledBase newBase) {
                CompoundTag newTag = new CompoundTag();
                BlockEntity te = level.getBlockEntity(ipos);
                if (te instanceof TileEntityPileBase pile) pile.writeSnapshot(newTag);
                else if (te instanceof TileEntityPileNeutronDetector detector) detector.writeSnapshot(newTag);

                // the shroud stays with the position, the rod (and its OUT bit) travels
                BlockState placed = oldState.setValue(ALUMINUM, newState.getValue(ALUMINUM));
                level.setBlock(ipos, placed, 3);

                // 1.7 always read the carried tag back (initially just the coordinates), which resets a tile that
                // survived a same-block state change; skipping the empty tag would leave the freshly inserted rod
                // with the progress of the tile that was pushed along.
                BlockEntity fresh = level.getBlockEntity(ipos);
                if (fresh instanceof TileEntityPileBase pile) pile.readSnapshot(oldTag);
                else if (fresh instanceof TileEntityPileNeutronDetector detector) detector.readSnapshot(oldTag);

                oldState = newState;
                oldTag = newTag;

                if (newBase instanceof BlockGraphiteDrilled) break; // if there's no need to eject an item
            } else {
                ItemStack eject = ((BlockGraphiteDrilledBase) oldState.getBlock()).getInsertedItem(oldState);
                ejectItem(level, ipos.relative(dir, -1), dir, eject);
                level.playSound(null, ipos.getX() + 0.5, ipos.getY() + 0.5, ipos.getZ() + 0.5,
                        ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.25F, 1.0F);
                break;
            }
        }

        return true;
    }
}
