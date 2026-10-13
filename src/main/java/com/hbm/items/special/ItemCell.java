package com.hbm.items.special;

import com.hbm.config.BombConfig;
import com.hbm.config.WeaponConfig;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModSingularityItems;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.items.ModelState;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Component-backed replacement for the old metadata fluid cell used by the antimatter recipe. */
public final class ItemCell extends CompatItem implements IFillableItem, ModelState {
    public static final int CAPACITY = 1_000;

    public ItemCell(Item.Properties properties) {
        super(properties);
    }

    public static boolean isEmptyCell(ItemStack stack) {
        return stack.getItem() instanceof ItemCell && getFluidType(stack) == null
                && ModDataComponents.fluidAmount(stack) == 0;
    }

    public static boolean isFullCell(ItemStack stack, FluidType fluid) {
        return storedAmount(stack) == CAPACITY && getFluidType(stack) == fluid;
    }

    public static FluidType getFluidType(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemCell)) return null;
        FluidType fluid = ModDataComponents.fluid(stack);
        return fluid == Fluids.NONE ? null : fluid;
    }

    public static ItemStack getFullCell(FluidType fluid) {
        return getFullCell(fluid, 1);
    }

    /** The second argument is the old stack count, not a fluid amount. */
    public static ItemStack getFullCell(FluidType fluid, int count) {
        if (!isCellFluid(fluid) || count <= 0) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(ModSingularityItems.cell.get(), count);
        setFluid(stack, fluid, CAPACITY);
        return stack;
    }

    private static void setFluid(ItemStack stack, FluidType fluid, int amount) {
        if (!isCellFluid(fluid) || amount <= 0) {
            ModDataComponents.removeFluid(stack);
            ModDataComponents.removeFluidAmount(stack);
            return;
        }
        ModDataComponents.fluid(stack, fluid);
        ModDataComponents.fluidAmount(stack, Math.min(CAPACITY, amount));
    }

    /**
     * {@code hbm:model_state}: the 1.7 cell item ({@link CellVariants}) whose sprite this cell draws, from its fluid
     * (saved by name). Nothing about the sprite is stored on the stack.
     */
    @Override
    public int modelState(ItemStack stack) {
        return variantOf(getFluidType(stack));
    }

    /** The cell variant (1.7 item) that holds this fluid, or {@link CellVariants#EMPTY} if no cell holds it. */
    public static int variantOf(FluidType fluid) {
        if (fluid == null) return CellVariants.EMPTY;
        if (fluid == Fluids.UF6) return CellVariants.UF6;
        if (fluid == Fluids.PUF6) return CellVariants.PUF6;
        if (fluid == Fluids.AMAT) return CellVariants.ANTIMATTER;
        if (fluid == Fluids.DEUTERIUM) return CellVariants.DEUTERIUM;
        if (fluid == Fluids.TRITIUM) return CellVariants.TRITIUM;
        if (fluid == Fluids.SAS3) return CellVariants.SAS3;
        if (fluid == Fluids.ASCHRAB) return CellVariants.ANTI_SCHRABIDIUM;
        return CellVariants.EMPTY;
    }

    private static boolean isCellFluid(FluidType fluid) {
        return variantOf(fluid) != CellVariants.EMPTY;
    }

    /** Full cells leave one empty cell, while an empty cell is not a container item. */
    public static ItemStack remainder(ItemStack stack) {
        return isEmptyCell(stack) ? ItemStack.EMPTY
                : isFullCell(stack, getFluidType(stack)) ? new ItemStack(ModSingularityItems.cell.get())
                : ItemStack.EMPTY;
    }

    @Override
    public int getFill(ItemStack stack) {
        return storedAmount(stack);
    }

    private static int storedAmount(ItemStack stack) {
        return Math.max(0, Math.min(CAPACITY, ModDataComponents.fluidAmount(stack)));
    }

    @Override
    public boolean acceptsFluid(FluidType fluid, ItemStack stack) {
        return isCellFluid(fluid) && isEmptyCell(stack);
    }

    @Override
    public int tryFill(FluidType fluid, int amount, ItemStack stack) {
        if (amount < CAPACITY || !acceptsFluid(fluid, stack)) return amount;
        setFluid(stack, fluid, CAPACITY);
        return amount - CAPACITY;
    }

    @Override
    public boolean providesFluid(FluidType fluid, ItemStack stack) {
        return isFullCell(stack, fluid);
    }

    @Override
    public int tryEmpty(FluidType fluid, int amount, ItemStack stack) {
        if (amount < CAPACITY || !providesFluid(fluid, stack)) return 0;
        setFluid(stack, fluid, 0);
        return CAPACITY;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        FluidType fluid = getFluidType(stack);
        return isFullCell(stack, fluid) ? fluid : Fluids.NONE;
    }

    @Override
    public Component getName(ItemStack stack) {
        int variant = variantOf(getFluidType(stack));
        MutableComponent name = Component.translatable("item.hbm.cell_" + CellVariants.NAMES[variant]);
        // 1.7 cell_sas3 was ItemCustomLore.setRarity(EnumRarity.rare); per-stack rarity is spelled differently
        // on every target, so the rare (aqua) colour is carried by the name itself, which wins over the
        // outer rarity style in every version's tooltip and held-item label.
        return variant == CellVariants.SAS3 ? name.withStyle(ChatFormatting.AQUA) : name;
    }

    /**
     * 1.7 {@code ItemDrop.onEntityItemUpdate} for {@code cell_antimatter} and {@code cell_anti_schrabidium}, the
     * only two cells that were {@code ItemDrop}s. On the ground (1.7 checked {@code onGround} only - a burning cell
     * in the air does nothing) the entity is always removed, and with {@code dropCell} on:
     * <ul>
     *   <li>antimatter: {@code new ExplosionVNT(..., 3F).makeAmat().explode()};</li>
     *   <li>anti-schrabidium: an {@code aSchrabRadius} FLEIJA field ({@code EntityNukeExplosionMK3.statFacFleija})
     *       plus its cloud and a volume-100 explosion sound, unless the factory refused the blast.</li>
     * </ul>
     * Odd but 1.7: with {@code dropCell} off the dropped cell still vanishes, because {@code setDead} sat outside
     * the config check.
     */
    /** The dropped antimatter cell's blast strength (1.7: 3; ADDON-API-INVENTORY V-172). NTM: Space sets hbm-space's 5. */
    public static float antimatterDropStrength = 3F;

    public static void tickDropped(ItemEntity entity) {
        if (!(entity.getItem().getItem() instanceof ItemCell) || entity.level().isClientSide()) return;
        if (!entity.onGround() || !(entity.level() instanceof ServerLevel level)) return;
        FluidType fluid = getFluidType(entity.getItem());
        if (!isFullCell(entity.getItem(), fluid) || (fluid != Fluids.AMAT && fluid != Fluids.ASCHRAB)) return;

        double x = entity.getX(), y = entity.getY(), z = entity.getZ();
        if (WeaponConfig.dropCell) {
            if (fluid == Fluids.AMAT) {
                new ExplosionVNT(level, x, y, z, antimatterDropStrength).makeAmat().explode();
            } else {
                int radius = BombConfig.aSchrabRadius;
                EntityNukeExplosionMK3 ex =
                        EntityNukeExplosionMK3.statFacFleija(level, x, y, z, radius);
                if (!ex.isRemoved()) {
                    level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(),
                            SoundSource.BLOCKS, 100.0F, level.getRandom().nextFloat() * 0.1F + 0.9F);
                    level.addFreshEntity(ex);
                    level.addFreshEntity(EntityCloudFleija.statFac(level, radius, x, y, z));
                }
            }
        }
        entity.discard();
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        // 1.7 ItemDrop.addInformation: plain grey lines, no drop badge.
        FluidType fluid = getFluidType(stack);
        if (fluid == Fluids.AMAT) {
            tooltip.add(Component.literal("Warning: Exposure to matter will"));
            tooltip.add(Component.literal("lead to violent annihilation!"));
        } else if (fluid == Fluids.ASCHRAB) {
            tooltip.add(Component.literal("Warning: Exposure to matter will"));
            tooltip.add(Component.literal("create a fólkvangr field!"));
        }
    }
}
