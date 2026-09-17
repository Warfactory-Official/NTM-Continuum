package com.hbm.items.weapon.grenade;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * The four 1.7 ItemGrenadeUniversal NBT selectors, carried as one typed item component.
 */
public record GrenadeData(ItemGrenadeShell.EnumGrenadeShell shell,
                          ItemGrenadeFilling.EnumGrenadeFilling filling,
                          ItemGrenadeFuze.EnumGrenadeFuze fuze,
                          ItemGrenadeExtra.@Nullable EnumGrenadeExtra extra) {

    public static final GrenadeData DEFAULT = new GrenadeData(
            ItemGrenadeShell.EnumGrenadeShell.FRAG,
            ItemGrenadeFilling.EnumGrenadeFilling.HE,
            ItemGrenadeFuze.EnumGrenadeFuze.S3,
            null);
    private static final Codec<ItemGrenadeShell.EnumGrenadeShell> SHELL = Codec.STRING.xmap(
            value -> ItemGrenadeShell.EnumGrenadeShell.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));
    private static final Codec<ItemGrenadeFilling.EnumGrenadeFilling> FILLING = Codec.STRING.xmap(
            value -> ItemGrenadeFilling.EnumGrenadeFilling.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));
    private static final Codec<ItemGrenadeFuze.EnumGrenadeFuze> FUZE = Codec.STRING.xmap(
            value -> ItemGrenadeFuze.EnumGrenadeFuze.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));
    private static final Codec<ItemGrenadeExtra.EnumGrenadeExtra> EXTRA = Codec.STRING.xmap(
            value -> ItemGrenadeExtra.EnumGrenadeExtra.valueOf(value.toUpperCase(Locale.ROOT)),
            value -> value.name().toLowerCase(Locale.ROOT));

    public static final Codec<GrenadeData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SHELL.fieldOf("shell").forGetter(GrenadeData::shell),
            FILLING.fieldOf("filling").forGetter(GrenadeData::filling),
            FUZE.fieldOf("fuze").forGetter(GrenadeData::fuze),
            EXTRA.optionalFieldOf("extra").forGetter(data -> Optional.ofNullable(data.extra()))
    ).apply(instance, (shell, filling, fuze, extra) -> new GrenadeData(shell, filling, fuze, extra.orElse(null))));

    private static <T> T enumAt(T[] values, int ordinal, @Nullable T fallback) {
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : fallback;
    }
}
