package com.hbm.blocks.material;

import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** The old eight-entry extended concrete enum, including the machine-stripe face contract. */
public final class BlockConcreteColoredExt extends CompatBlock {

    public static final EnumProperty<EnumConcreteType> TYPE =
            EnumProperty.create("type", EnumConcreteType.class);

    public BlockConcreteColoredExt(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, EnumConcreteType.MACHINE));
    }

    public static int variant(BlockState state) {
        return state.getValue(TYPE).ordinal();
    }

    public static EnumConcreteType type(int variant) {
        EnumConcreteType[] values = EnumConcreteType.values();
        return values[Math.floorMod(variant, values.length)];
    }

    public BlockState stateForVariant(int variant) {
        return defaultBlockState().setValue(TYPE, type(variant));
    }

    public BlockState stateForItem(ItemStack stack) {
        return stateForVariant(ModDataComponents.ITEM_VARIANT.getOr(stack, 0));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return stateForItem(context.getItemInHand());
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(MaterialCompletionRegistrations.concreteColoredExtItem(variant(state)));
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return MaterialCompletionRegistrations.concreteColoredExtItem(variant(state));
    }

    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return MaterialCompletionRegistrations.concreteColoredExtItem(variant(state));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    public enum EnumConcreteType implements StringRepresentable {
        MACHINE("machine"),
        MACHINE_STRIPE("machine_stripe"),
        INDIGO("indigo"),
        PURPLE("purple"),
        PINK("pink"),
        HAZARD("hazard"),
        SAND("sand"),
        BRONZE("bronze");

        public static final EnumConcreteType[] VALUES = values();

        private final String name;

        EnumConcreteType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
