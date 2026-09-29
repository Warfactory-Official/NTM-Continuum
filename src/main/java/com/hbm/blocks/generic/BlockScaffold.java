package com.hbm.blocks.generic;

import net.minecraft.world.item.ItemStack;
import com.hbm.items.ModDataComponents;
import dev.golem.core.registry.TickingBlock;
import com.hbm.tileentity.deco.TileEntityDeco;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** The four-way steel scaffold, including the old metadata-backed colour variants. */
public class BlockScaffold extends CompatBlock implements TickingBlock {

    public static final EnumProperty<Orientation> ORIENT = EnumProperty.create("orient", Orientation.class);
    public static final EnumProperty<Variant> VARIANT = EnumProperty.create("variant", Variant.class);

    private static final VoxelShape HORIZONTAL_EW = Block.box(2, 0, 0, 14, 16, 16);
    private static final VoxelShape HORIZONTAL_NS = Block.box(0, 0, 2, 16, 16, 14);
    private static final VoxelShape VERTICAL = Block.box(0, 2, 0, 16, 14, 16);

    public BlockScaffold(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(ORIENT, Orientation.HORIZONTAL_NS)
                .setValue(VARIANT, Variant.STEEL));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(ORIENT)) {
            case HORIZONTAL_EW -> HORIZONTAL_EW;
            case HORIZONTAL_NS -> HORIZONTAL_NS;
            case VERTICAL_NS, VERTICAL_EW -> VERTICAL;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int variant = ModDataComponents.ITEM_VARIANT.getOr(context.getItemInHand(), 0) & 3;
        Orientation orientation;
        if (context.getClickedFace().getAxis().isVertical()) {
            float yaw = context.getPlayer() == null ? 0.0F : context.getPlayer().getYRot();
            int rotation = Mth.floor(yaw * 4.0F / 360.0F + 0.5D) & 3;
            orientation = rotation % 2 == 0 ? Orientation.HORIZONTAL_NS : Orientation.HORIZONTAL_EW;
        } else if (context.getClickedFace().getAxis() == net.minecraft.core.Direction.Axis.Z) {
            orientation = Orientation.VERTICAL_NS;
        } else {
            orientation = Orientation.VERTICAL_EW;
        }
        return defaultBlockState()
                .setValue(VARIANT, Variant.values()[variant])
                .setValue(ORIENT, orientation);
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(this);
        return withVariant(stack, state);
    }

    private static net.minecraft.world.item.ItemStack withVariant(net.minecraft.world.item.ItemStack stack,
                                                                   BlockState state) {
        ModDataComponents.ITEM_VARIANT.set(stack, state.getValue(VARIANT).ordinal());
        return stack;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDeco(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ORIENT, VARIANT);
    }

    public enum Orientation implements net.minecraft.util.StringRepresentable {
        HORIZONTAL_NS("horizontal_north_south"),
        HORIZONTAL_EW("horizontal_east_west"),
        VERTICAL_NS("vertical_north_south"),
        VERTICAL_EW("vertical_east_west");

        private final String name;

        Orientation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public enum Variant implements net.minecraft.util.StringRepresentable {
        STEEL("steel", "scaffold_steel"),
        RED("red", "scaffold_red"),
        WHITE("white", "scaffold_white"),
        YELLOW("yellow", "scaffold_yellow");

        private final String name;
        public final String texture;

        Variant(String name, String texture) {
            this.name = name;
            this.texture = texture;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
