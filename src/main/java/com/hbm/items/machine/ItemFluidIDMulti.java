package com.hbm.items.machine;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.IItemControlReceiver;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.items.tool.UtilityToolScreens;
import com.hbm.packet.PlayerInformPayload;
import com.hbm.packet.PlayerInformPayload.Translated;
import com.hbm.tileentity.IFluidCopiable;
import dev.golem.core.data.ModelData;
import dev.golem.core.data.Tags;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ItemFluidIDMulti}, the Multi Fluid Identifier: carries a primary and a secondary fluid
 * ({@code fluid1}/{@code fluid2}). Right-click swaps them and names the new primary on the info HUD;
 * sneak-right-click opens the fluid picker ({@code GUIScreenFluid}), whose left/right clicks set primary and
 * secondary through {@link IItemControlReceiver}. Machines and ducts read the primary.
 *
 * <p>The overlay is tinted with the primary fluid's colour ({@link ModelData}, read by the item definition's
 * custom-model-data tint), 1.7's second render pass. Not ported: 1.7's {@code getContainerItem} kept the
 * identifier in a crafting grid; vanilla has no stack-preserving remainder, and no ported recipe takes it.
 */
public class ItemFluidIDMulti extends CompatItem implements IItemFluidId, IItemControlReceiver {

    public ItemFluidIDMulti(Properties properties) {
        super(properties);
    }

    /** 1.7 {@code getSubItems}: one identifier per identifiable fluid, primary set. */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> list = new ArrayList<>();
        FluidType[] order = Fluids.getInNiceOrder();
        for (int i = 1; i < order.length; ++i) {
            if (!IFluidCopiable.ItemFluidIds.hasNoID(order[i])) {
                ItemStack id = new ItemStack(ModUtilityToolItems.fluid_identifier_multi.get());
                setType(id, order[i], true);
                list.add(id);
            }
        }
        return list;
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide() && !player.isShiftKeyDown()) {
            FluidType primary = getType(stack, true);
            FluidType secondary = getType(stack, false);
            setType(stack, secondary, true);
            setType(stack, primary, false);
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.25F, 1.25F);
            if (player instanceof ServerPlayer serverPlayer) {
                GolemNetwork.sendToPlayer(serverPlayer, PlayerInformPayload.CODEC, PlayerInformPayload.of(7, 3000,
                        ChatFormatting.RESET, new Translated(secondary.getTranslationKey())));
            }
        }

        if (world.isClientSide() && player.isShiftKeyDown()) {
            UtilityToolScreens.open(UtilityToolScreens.Kind.FLUID_MULTI, stack);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void receiveControl(Player player, ItemStack stack, CompoundTag data) {
        if (Tags.contains(data, "primary")) setType(stack, Fluids.read(data, "primary"), true);
        if (Tags.contains(data, "secondary")) setType(stack, Fluids.read(data, "secondary"), false);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.translatable("item.hbm.fluid_identifier_multi.info"));
        list.add(Component.literal("   ").append(getType(stack, true).getDisplayName()));
        list.add(Component.translatable("item.hbm.fluid_identifier_multi.info2"));
        list.add(Component.literal("   ").append(getType(stack, false).getDisplayName()));
    }

    @Override
    public FluidType getType(@Nullable Level world, int x, int y, int z, ItemStack stack) {
        return getType(stack, true);
    }

    public static void setType(ItemStack stack, FluidType type, boolean primary) {
        CompoundTag tag = ModUtilityToolItems.FLUID_MULTI.getOr(stack, new CompoundTag()).copy();
        Fluids.write(tag, "fluid" + (primary ? 1 : 2), type);
        ModUtilityToolItems.FLUID_MULTI.set(stack, tag);
        // 1.7 updateMeta + getColorFromItemStack: the overlay follows the primary fluid.
        if (primary) ModelData.setColor(stack, type.getColor() < 0 ? 0xFFFFFF : type.getColor());
    }

    public static FluidType getType(ItemStack stack, boolean primary) {
        CompoundTag tag = ModUtilityToolItems.FLUID_MULTI.getOr(stack, null);
        if (tag == null) return Fluids.NONE;
        return Fluids.read(tag, "fluid" + (primary ? 1 : 2));
    }
}
