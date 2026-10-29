package com.hbm.items.weapon;

import java.util.function.Consumer;

import dev.golem.core.hook.Hook;
import com.hbm.handler.MissileStruct;
import com.hbm.items.ModCustomMissileItems;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 1.7 {@code ItemCustomMissile}: the assembled missile the assembly station hands out. 1.7 wrote the five part
 * item ids as ints on the stack NBT ({@code chip}, {@code warhead}, {@code fuselage}, {@code stability},
 * {@code thruster}); numeric item ids are not stable on 26.x, so the same five travel as one component of item
 * names. The stability fin is optional, as it was.
 */
public class ItemCustomMissile extends CompatItem {

    @FunctionalInterface
    public interface Description {
        boolean describe(MissileStruct parts, @Nullable ItemCustomMissilePart chip,
                         Consumer<Component> output);
    }

    public static final Hook<Description> DESCRIBE =
            Hook.of(listeners -> (parts, chip, output) -> {
                for (var listener : listeners) if (listener.describe(parts, chip, output)) return true;
                return false;
            });

    /**
     * The five parts; {@code stability} is null for a finless missile.
     */
    public record Parts(Item chip, Item warhead, Item fuselage, @Nullable Item stability, Item thruster) {

        private static final Codec<Item> ITEM = BuiltInRegistries.ITEM.byNameCodec();

        public static final Codec<Parts> CODEC = RecordCodecBuilder.create(i -> i.group(
                ITEM.fieldOf("chip").forGetter(Parts::chip),
                ITEM.fieldOf("warhead").forGetter(Parts::warhead),
                ITEM.fieldOf("fuselage").forGetter(Parts::fuselage),
                ITEM.optionalFieldOf("stability").forGetter(p -> Optional.ofNullable(p.stability())),
                ITEM.fieldOf("thruster").forGetter(Parts::thruster)
        ).apply(i, (c, w, f, s, t) -> new Parts(c, w, f, s.orElse(null), t)));

    }

    public ItemCustomMissile(Properties properties) {
        super(properties);
    }

    public static ItemStack buildMissile(Item chip, Item warhead, Item fuselage, @Nullable Item stability,
                                         Item thruster) {
        ItemStack missile = new ItemStack(ModCustomMissileItems.missile_custom.get());
        ModCustomMissileItems.MISSILE_PARTS.set(missile, new Parts(chip, warhead, fuselage, stability, thruster));
        return missile;
    }

    /**
     * 1.7 {@code buildMissile(ItemStack...)}: the stacks only contribute their item.
     */
    public static ItemStack buildMissile(ItemStack chip, ItemStack warhead, ItemStack fuselage, ItemStack stability,
                                         ItemStack thruster) {
        return buildMissile(chip.getItem(), warhead.getItem(), fuselage.getItem(),
                stability == null || stability.isEmpty() ? null : stability.getItem(), thruster.getItem());
    }

    public static @Nullable Parts parts(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemCustomMissile)) return null;
        return ModCustomMissileItems.MISSILE_PARTS.get(stack).orElse(null);
    }

    public static @Nullable ItemCustomMissilePart chip(ItemStack stack) {
        Parts parts = parts(stack);
        return parts != null && parts.chip() instanceof ItemCustomMissilePart part ? part : null;
    }

    public static @Nullable ItemCustomMissilePart stability(ItemStack stack) {
        Parts parts = parts(stack);
        return parts != null && parts.stability() instanceof ItemCustomMissilePart part ? part : null;
    }

    /**
     * 1.7 {@code getStruct}: null for anything that is not a custom missile.
     */
    public static @Nullable MissileStruct getStruct(ItemStack stack) {
        Parts parts = parts(stack);
        if (parts == null) {
            // 1.7 read absent NBT keys as id 0 (air): a bare missile_custom has a struct of four nulls
            return stack != null && stack.getItem() instanceof ItemCustomMissile ? MissileStruct.EMPTY : null;
        }
        return new MissileStruct(cast(parts.warhead()), cast(parts.fuselage()), cast(parts.stability()),
                cast(parts.thruster()));
    }

    private static @Nullable ItemCustomMissilePart cast(@Nullable Item item) {
        return item instanceof ItemCustomMissilePart part ? part : null;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        Parts parts = parts(stack);
        if (parts == null) return;

        try {
            ItemCustomMissilePart chip = (ItemCustomMissilePart) parts.chip();
            ItemCustomMissilePart warhead = (ItemCustomMissilePart) parts.warhead();
            ItemCustomMissilePart fuselage = (ItemCustomMissilePart) parts.fuselage();
            ItemCustomMissilePart stability = parts.stability() == null ? null : (ItemCustomMissilePart) parts.stability();
            ItemCustomMissilePart thruster = (ItemCustomMissilePart) parts.thruster();
            var structure = new MissileStruct(warhead, fuselage, stability, thruster);
            if (!DESCRIBE.invoker().describe(structure, chip, tooltip::add)) describe(structure, chip, tooltip::add);
        } catch (RuntimeException ex) {
            tooltip.add(Component.translatable("error.generic").withStyle(ChatFormatting.RED));
        }
    }

    /**
     * The base missile description, also used when no addon replaces its presentation.
     */
    public static void describe(MissileStruct parts, ItemCustomMissilePart chip,
                                Consumer<Component> output) {
        var warhead = parts.warhead();
        var fuselage = parts.fuselage();
        var stability = parts.fins();
        output.accept(ItemCustomMissilePart.stat("item.missile.desc.warhead", warhead.warheadType().getDisplay()));
        output.accept(ItemCustomMissilePart.stat("item.missile.desc.strength",
                Component.literal(String.valueOf(warhead.warheadStrength()))));
        output.accept(ItemCustomMissilePart.stat("item.missile.desc.fuelType", fuselage.fuelType().getDisplay()));
        output.accept(ItemCustomMissilePart.stat("item.missile.desc.fuelAmount",
                Component.literal(fuselage.fuelAmount() + "l")));
        output.accept(ItemCustomMissilePart.stat("item.missile.desc.chipInaccuracy",
                Component.literal(chip.inaccuracy() * 100 + "%")));
        output.accept(ItemCustomMissilePart.stat("item.missile.desc.finInaccuracy",
                Component.literal(stability != null ? stability.inaccuracy() * 100 + "%" : "100%")));
        output.accept(ItemCustomMissilePart.stat("item.missile.desc.size",
                fuselage.top.getDisplay().copy().append("/").append(fuselage.bottom.getDisplay())));
        float health = warhead.health + fuselage.health + parts.thruster().health;
        if (stability != null) health += stability.health;
        output.accept(ItemCustomMissilePart.stat("item.missile.desc.health", Component.literal(health + "HP")));
    }

    /**
     * Whether a part of this type sits in the slot it claims to. Used by the part-type sanity checks.
     */
    static boolean is(@Nullable Item item, PartType type) {
        return item instanceof ItemCustomMissilePart part && part.type == type;
    }
}
