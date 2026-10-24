package com.hbm.items.food;

import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityVortex;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import java.util.Locale;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * 1.7 {@code ItemConserve}: the canned-food {@code ItemEnumMulti}, flattened into one item per
 * {@link EnumFoodType} ({@code canned_conserve_<type>}). Every can hands back a winding key; the black hole can
 * opens a small vortex, recursion refills itself nine times in ten, and the fist hits the eater.
 *
 * <p>1.7 wrote the hunger and saturation itself instead of extending {@code ItemFood}; the food component
 * applies the same {@code FoodStats.addStats(level, saturation)} numbers.
 */
public class ItemConserve extends ItemModFood {

    public final EnumFoodType type;

    public ItemConserve(Properties properties, EnumFoodType type) {
        super(properties);
        this.type = type;
    }

    /** The key, the recursion can and the fist's punch are the treatment; a vortex is not data. */
    @Override
    protected void onFoodEaten(ItemStack stack, ServerLevel level, Player player) {
        if (type == EnumFoodType.BHOLE) {
            EntityVortex vortex = new EntityVortex(ModEntities.VORTEX.get(), level, 0.5F).setShrinkRate(0.01F);
            vortex.noBreak();
            vortex.setPos(player.getX(), player.getY(), player.getZ());
            level.addFreshEntity(vortex);
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        // 1.7 split one "$"-joined .desc value; the lang file carries the pieces as .desc.0, .desc.1, ...
        String base = getDescriptionId() + ".desc.";
        Language language = Language.getInstance();
        for (int i = 0; language.has(base + i); i++) tooltip.add(Component.translatable(base + i));
    }

    public String idSuffix() {
        return type.name().toLowerCase(Locale.US);
    }

    public enum EnumFoodType {
        BEEF(8, 0.75F),
        TUNA(4, 0.75F),
        MYSTERY(6, 0.5F),
        PASHTET(4, 0.5F),
        CHEESE(3, 1F),
        SLIME(15, 5F),
        MILK(5, 0.25F),
        ASS(6, 0.75F),
        PIZZA(8, 75F), // 1.7 wrote "075F", which Java reads as seventy-five
        TUBE(2, 0.25F),
        TOMATO(4, 0.5F),
        ASBESTOS(7, 1F),
        BHOLE(10, 1F),
        HOTDOGS(5, 0.75F),
        LEFTOVERS(1, 0.1F),
        YOGURT(3, 0.5F),
        STEW(5, 0.5F),
        CHINESE(6, 0.1F),
        OIL(3, 1F),
        FIST(6, 0.75F),
        SPAM(8, 1F),
        FRIED(10, 0.75F),
        NAPALM(6, 1F),
        DIESEL(6, 1F),
        KEROSENE(6, 1F),
        RECURSION(1, 1F),
        BARK(2, 1F);

        public final int foodLevel;
        public final float saturation;

        EnumFoodType(int level, float sat) {
            this.foodLevel = level;
            this.saturation = sat;
        }
    }
}
