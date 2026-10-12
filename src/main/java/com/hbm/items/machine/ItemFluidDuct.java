package com.hbm.items.machine;

import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import dev.golem.core.data.ModelData;
import dev.golem.core.world.CompatItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code ItemFluidDuct} ({@code ModItems.fluid_duct}): a pre-typed universal fluid duct. The fluid is 1.7's
 * metadata, here {@link ModDataComponents#FLUID}; the icon is 1.7's two passes, the {@code duct} sprite under the
 * {@code duct_overlay} tinted with the fluid colour. Using it places a {@code fluid_duct_neo} already set to that
 * fluid. It is made by the retyping recipes ({@code com.hbm.inventory.recipes.FluidDuctRetypeRecipe}) and turns
 * back into a plain duct shapelessly.
 */
public class ItemFluidDuct extends CompatItem {

    public ItemFluidDuct(Properties properties) {
        super(properties);
    }

    public static FluidType getFluid(ItemStack stack) {
        return ModDataComponents.fluid(stack);
    }

    public static ItemStack make(Item item, FluidType fluid, int count) {
        ItemStack stack = new ItemStack(item, count);
        ModDataComponents.fluid(stack, fluid);
        ModelData.setColor(stack, fluid.getColor() < 0 ? 0xFFFFFF : fluid.getColor());
        return stack;
    }

    /** 1.7 getSubItems: one duct per fluid that has an identifier, NONE excluded. */
    public static List<ItemStack> creativeStacks(Item item) {
        List<ItemStack> list = new ArrayList<>();
        FluidType[] all = Fluids.getInNiceOrder();
        for (int i = 1; i < all.length; ++i) {
            if (!all[i].hasNoID()) list.add(make(item, all[i], 1));
        }
        return list;
    }

    /** 1.7 getItemStackDisplayName: "Fluid Duct:" and the fluid's name. */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.hbm.fluid_duct", getFluid(stack).getDisplayName());
    }

    /** 1.7 onItemUse: place a duct on the clicked face (or into a replaceable block) and type it. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPlaceContext place = new BlockPlaceContext(context);
        Level level = context.getLevel();
        BlockPos pos = place.getClickedPos();
        Player player = context.getPlayer();
        if (!place.canPlace()) return InteractionResult.FAIL;
        if (player != null && !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockState state = ModFluidNetBlocks.fluid_duct_neo.get().getStateForPlacement(place);
        if (state == null || !level.setBlock(pos, state, 3)) return InteractionResult.FAIL;
        BlockFluidDuct.typePlaced(level, pos, getFluid(context.getItemInHand()));

        SoundType sound = state.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
