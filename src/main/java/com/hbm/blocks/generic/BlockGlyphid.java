package com.hbm.blocks.generic;

import com.hbm.items.block.ItemBlockDecoVariant;
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
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 1.7 {@code BlockGlyphid} (glyphid_base, the hive wall): coral material, fleshy, hardness 0.5, and it drops
 * nothing. Metadata 0/1/2 is the normal, infested and radioactive hive ({@link #TYPE}); fallout turns a normal
 * hive radioactive. 1.7 picked one of two textures per position by hashing the coordinates - the blockstate's
 * two-model pool picks per position the same way.
 */
public class BlockGlyphid extends CompatBlock {

    public static final EnumProperty<HiveType> TYPE = EnumProperty.create("type", HiveType.class);

    public BlockGlyphid(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, HiveType.NORMAL));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(TYPE, HiveType.of(variantOf(context.getItemInHand())));
    }

    static int variantOf(ItemStack stack) {
        return stack.getItem() instanceof ItemBlockDecoVariant item ? item.variant(stack) : 0;
    }

    ItemStack stackFor(BlockState state) {
        return asItem() instanceof ItemBlockDecoVariant item ? item.of(state.getValue(TYPE).ordinal()) : new ItemStack(this);
    }

    /** 1.7 {@code getItemDropped} null. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of();
    }

    @Override
    protected ItemStack onPickBlock(BlockGetter level, BlockPos pos, BlockState state) {
        return stackFor(state);
    }

    /** 1.7 {@code EntityGlyphid.TYPE_NORMAL / TYPE_INFECTED / TYPE_RADIOACTIVE}, the block metadata. */
    public enum HiveType implements StringRepresentable {
        NORMAL("normal"), INFESTED("infested"), RAD("rad");

        public static final HiveType[] VALUES = values();
        private final String name;

        HiveType(String name) {
            this.name = name;
        }

        public static HiveType of(int ordinal) {
            return VALUES[Math.floorMod(ordinal, VALUES.length)];
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
