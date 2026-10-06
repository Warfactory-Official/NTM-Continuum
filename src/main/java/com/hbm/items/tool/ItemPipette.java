package com.hbm.items.tool;

import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModUtilityToolItems;
import com.hbm.tileentity.machine.storage.TileEntityBarrel;
import dev.golem.core.data.ModelData;
import dev.golem.core.data.Tags;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ItemPipette}: a fluid container of adjustable capacity. Empty, right-click raises the capacity
 * (sneak lowers it) in 50 mB steps between 50 and 1,000 mB, or 1 mB steps between 1 and 50 mB for the laboratory
 * pipette. The plain glass pipette fizzles away when a corrosive fluid other than peroxide enters it; none
 * accept antimatter. State rides {@code hbm:pipette} ({@code type}, {@code fill}, {@code capacity}, 1.7's NBT
 * keys, stored as ints: the legacy tag reader takes ints only); the icon picks its filled overlay through {@code hbm:model_state} (from the fill) and tints it with the fluid's colour.
 */
public class ItemPipette extends CompatItem implements IFillableItem, com.hbm.items.ModelState {

    public ItemPipette(Properties properties) {
        super(properties);
    }

    private boolean isLaboratory() {
        return this == ModUtilityToolItems.pipette_laboratory.get();
    }

    public short getMaxFill() {
        if (isLaboratory()) return 50;
        else return 1_000;
    }

    private CompoundTag tag(ItemStack stack) {
        CompoundTag tag = ModUtilityToolItems.PIPETTE.getOr(stack, null);
        if (tag == null) {
            tag = new CompoundTag();
            Fluids.write(tag, "type", Fluids.NONE);
            tag.putInt("fill", 0);
            tag.putInt("capacity", getMaxFill());
        }
        return tag;
    }

    public FluidType getType(ItemStack stack) {
        return Fluids.read(tag(stack), "type");
    }

    public short getCapacity(ItemStack stack) {
        return (short) Tags.getIntOr(tag(stack), "capacity", getMaxFill());
    }

    public void setFill(ItemStack stack, FluidType type, short fill) {
        CompoundTag tag = tag(stack).copy();
        Fluids.write(tag, "type", type);
        tag.putInt("fill", fill);
        ModUtilityToolItems.PIPETTE.set(stack, tag);
        int color = type.getColor();
        ModelData.setColor(stack, color < 0 ? 0xFFFFFF : color);
    }

    private void setCapacity(ItemStack stack, short capacity) {
        CompoundTag tag = tag(stack).copy();
        tag.putInt("capacity", capacity);
        ModUtilityToolItems.PIPETTE.set(stack, tag);
    }

    /** 1.7 getIcon: pass 1 is the empty sprite at fill 0, else the tinted overlay ({@code hbm:model_state}, never saved). */
    @Override
    public int modelState(ItemStack stack) {
        return getFill(stack) > 0 ? 1 : 0;
    }

    @Override
    public int getFill(ItemStack stack) {
        return Tags.getIntOr(tag(stack), "fill", 0);
    }

    @Override
    protected InteractionResult compatUse(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide()) {
            if (this.getFill(stack) == 0) {
                int a;
                if (isLaboratory())
                    a = !player.isShiftKeyDown() ? Math.min(this.getCapacity(stack) + 1, 50) : Math.max(this.getCapacity(stack) - 1, 1);
                else
                    a = !player.isShiftKeyDown() ? Math.min(this.getCapacity(stack) + 50, 1_000) : Math.max(this.getCapacity(stack) - 50, 50);
                setCapacity(stack, (short) a);
                player.sendSystemMessage(Component.literal(a + "/" + this.getMaxFill() + "mB"));
            } else {
                player.sendSystemMessage(Component.translatable("desc.item.pipette.noEmpty"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        if (isLaboratory()) {
            list.add(Component.translatable("desc.item.pipette.corrosive"));
            list.add(Component.translatable("desc.item.pipette.laboratory"));
        }
        if (this == ModUtilityToolItems.pipette_boron.get())
            list.add(Component.translatable("desc.item.pipette.corrosive"));
        if (this == ModUtilityToolItems.pipette.get())
            list.add(Component.translatable("desc.item.pipette.noCorrosive"));
        list.add(Component.literal("Fluid: ").append(this.getType(stack).getDisplayName()));
        list.add(Component.literal("Amount: " + this.getFill(stack) + "/" + this.getCapacity(stack) + "mB (" + this.getMaxFill() + "mB)"));
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return (type == this.getType(stack) || this.getFill(stack) == 0) && !TileEntityBarrel.isAntimatter(type);
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack))
            return amount;

        if (this.getFill(stack) == 0)
            this.setFill(stack, type, (short) 0);

        int req = this.getCapacity(stack) - this.getFill(stack);
        int toFill = Math.min(req, amount);

        this.setFill(stack, type, (short) (this.getFill(stack) + toFill));

        // fizzling checks
        if (this.getFill(stack) > 0 && willFizzle(type)) {
            stack.setCount(0);
        }

        return amount - toFill;
    }

    public boolean willFizzle(FluidType type) {
        if (this != ModUtilityToolItems.pipette.get()) return false;
        return type.hasTrait(com.hbm.inventory.fluid.trait.FT_Corrosive.class) && type != Fluids.PEROXIDE;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return this.getType(stack) == type;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        if (providesFluid(type, stack)) {
            int toUnload = Math.min(amount, this.getFill(stack));
            this.setFill(stack, type, (short) (this.getFill(stack) - toUnload));
            if (this.getFill(stack) == 0)
                this.setFill(stack, Fluids.NONE, (short) 0);
            return toUnload;
        }
        return amount;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return this.getType(stack);
    }
}
