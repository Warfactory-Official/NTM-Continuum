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

/** The old five-entry lightstone enum. Despite its name, the legacy block emits no light. */
public final class BlockLightstone extends CompatBlock {

    public static final EnumProperty<LightstoneType> TYPE =
            EnumProperty.create("type", LightstoneType.class);

    public BlockLightstone(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, LightstoneType.UNREFINED));
    }

    public static int variant(BlockState state) {
        return state.getValue(TYPE).ordinal();
    }

    public static LightstoneType type(int variant) {
        LightstoneType[] values = LightstoneType.values();
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
        return List.of(MaterialCompletionRegistrations.lightstoneItem(variant(state)));
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return MaterialCompletionRegistrations.lightstoneItem(variant(state));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    public enum LightstoneType implements StringRepresentable {
        UNREFINED("unrefined"),
        TILE("tile"),
        BRICKS("bricks"),
        BRICKS_CHISELED("bricks_chiseled"),
        CHISELED("chiseled");

        public static final LightstoneType[] VALUES = values();

        private final String name;

        LightstoneType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
