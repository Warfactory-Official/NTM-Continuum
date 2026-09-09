package com.hbm.items.special;

import com.hbm.config.WeaponConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.items.ModItems;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.CompatItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A legacy dropped capsule whose ground trigger creates one of the real vortex entities. */
public final class ItemSingularity extends CompatItem {

    public enum Kind {
        SINGULARITY,
        COUNTER_RESONANT,
        SUPER_HEATED,
        BLACK_HOLE,
        SPARK
    }

    private final Kind kind;

    public ItemSingularity(Item.Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** Called from the loader's item-entity tick hook; mirrors ItemDrop.onEntityItemUpdate. */
    public static void tickDropped(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (!(stack.getItem() instanceof ItemSingularity singularity)
                || (!entity.onGround() && !entity.isOnFire())
                || entity.level().isClientSide()) return;

        if (WeaponConfig.dropSing && entity.level() instanceof net.minecraft.server.level.ServerLevel level) {
            EntityBlackHole vortex = singularity.create(level);
            vortex.setPos(entity.getX(), entity.getY(), entity.getZ());
            level.addFreshEntity(vortex);
        }
        // ItemDrop consumes every registered drop capsule once it is grounded or burning, even when the
        // config disables its effect. That oddity is part of the old contract.
        entity.discard();
    }

    private EntityBlackHole create(Level level) {
        return switch (kind) {
            case SINGULARITY -> new com.hbm.entity.effect.EntityVortex(ModEntities.VORTEX.get(), level, 1.5F);
            case COUNTER_RESONANT, SUPER_HEATED ->
                    new com.hbm.entity.effect.EntityVortex(ModEntities.VORTEX.get(), level, 2.5F);
            case BLACK_HOLE -> new EntityBlackHole(ModEntities.BLACK_HOLE.get(), level, 1.5F);
            case SPARK -> new com.hbm.entity.effect.EntityRagingVortex(ModEntities.RAGING_VORTEX.get(), level, 3.5F);
        };
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        switch (kind) {
            case SINGULARITY -> {
                tooltip.add(Component.literal("You may be asking:"));
                tooltip.add(Component.literal("\"But HBM, a manifold with an undefined"));
                tooltip.add(Component.literal("state of spacetime? How is this possible?\""));
                tooltip.add(Component.literal("Long answer short:"));
                tooltip.add(Component.literal("\"I have no idea!\""));
            }
            case COUNTER_RESONANT -> {
                tooltip.add(Component.literal("Nullifies resonance of objects in"));
                tooltip.add(Component.literal("non-euclidean space, creates variable"));
                tooltip.add(Component.literal("gravity well. Spontaneously spawns"));
                tooltip.add(Component.literal("tesseracts. If a tesseract happens to"));
                tooltip.add(Component.literal("appear near you, do not look directly"));
                tooltip.add(Component.literal("at it."));
            }
            case SUPER_HEATED -> {
                tooltip.add(Component.literal("Continuously heats up matter by"));
                tooltip.add(Component.literal("resonating every planck second."));
                tooltip.add(Component.literal("Tends to catch fire or to create"));
                tooltip.add(Component.literal("small plamsa arcs. Not edible."));
            }
            case BLACK_HOLE -> {
                tooltip.add(Component.literal("Contains a regular singularity"));
                tooltip.add(Component.literal("in the center. Large enough to"));
                tooltip.add(Component.literal("stay stable. It's not the end"));
                tooltip.add(Component.literal("of the world as we know it,"));
                tooltip.add(Component.literal("and I don't feel fine."));
            }
            case SPARK -> { }
        }
        tooltip.add(Component.literal("[").withStyle(ChatFormatting.RED)
                .append(Component.translatable("trait.drop").withStyle(ChatFormatting.RED))
                .append(Component.literal("]").withStyle(ChatFormatting.RED)));
    }

    /** Shared construction helper for the old {@code setContainerItem(nuclear_waste)} contract. */
    public static Item.Properties properties(Item.Properties properties) {
        return Compat.craftingRemainder(properties, ModItems.nuclear_waste.get()).stacksTo(1);
    }
}
