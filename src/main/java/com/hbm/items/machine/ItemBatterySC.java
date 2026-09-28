package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.lib.Library;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 1.7 {@code ItemBatterySC}, the radiovoltaic self-charging battery: an enum-multi of ten ({@link EnumBatterySC}),
 * each variant registered as its own {@code battery_sc_<variant>} item as the port does for every 1.7 metadata
 * enum. It never charges, never drains, and always reports its whole rating as both charge and discharge rate:
 * a battery socket or a charging slot sees a bottomless source of {@code power} HE/t.
 *
 * <p>Not the same item as the older fixed batteries {@code battery_sc_uranium} ... {@code battery_sc_americium}
 * ({@link ItemSelfcharger}), which the port keeps for powered armour's auxiliary drain.
 */
public class ItemBatterySC extends CompatItem implements IBatteryItem {

    public final EnumBatterySC pack;

    public ItemBatterySC(Item.Properties properties, EnumBatterySC pack) {
        super(properties);
        this.pack = pack;
    }

    public enum EnumBatterySC {
        EMPTY(0),
        WASTE(150),
        RA226(200),
        TC99(500),
        CO60(750),
        PU238(1_000),
        PO210(1_250),
        AU198(1_500),
        PB209(2_000),
        AM241(2_500);

        public final long power;
        public static final EnumBatterySC[] VALUES = values();

        EnumBatterySC(long power) {
            this.power = power;
        }

        /** 1.7 {@code ItemEnumMulti} texture/lang suffix: the lowercase enum name. */
        public String id() {
            return "battery_sc_" + name().toLowerCase(java.util.Locale.US);
        }
    }

    /** 1.7 {@code ResourceManager.battery_sc_tex}: the socket's battery body texture for every variant. */
    public static final Identifier SOCKET_TEXTURE = dev.golem.core.Ids.of("hbm", "textures/models/machines/battery_sc.png");

    @Override public void chargeBattery(ItemStack stack, long i) {}
    @Override public void setCharge(ItemStack stack, long i) {}
    @Override public void dischargeBattery(ItemStack stack, long i) {}
    @Override public long getChargeRate(ItemStack stack) { return 0; }
    @Override public long getCharge(ItemStack stack) { return getMaxCharge(stack); }
    @Override public long getDischargeRate(ItemStack stack) { return getMaxCharge(stack); }
    @Override public long getMaxCharge(ItemStack stack) { return pack.power; }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        if (pack.power > 0) {
            tooltip.add(Component.literal("Discharge rate: " + Library.getShortNumber(pack.power) + "HE/t")
                    .withStyle(ChatFormatting.YELLOW));
        }
        // 1.7 I18nUtil.resolveKeyArray("item.battery_sc.desc"), one red line per '$'-separated part
        for (String line : Component.translatable("item.hbm.battery_sc.desc").getString().split("[$]")) {
            tooltip.add(Component.literal(line).withStyle(ChatFormatting.RED));
        }
    }
}
