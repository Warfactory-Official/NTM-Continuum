package com.hbm.handler;

import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemCustomMissilePart.PartType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The four load-bearing parts of an assembled custom missile - what the launch pad hands the entity and what a
 * missile item carries around.
 *
 * 1.7 shipped this as a mutable holder with hand-rolled ByteBuf read/write over numeric item ids. 26.2 carries it as
 * an item component instead, so it is a record with a codec pair; the chip rides on the item next to it rather than
 * inside the struct, exactly as in 1.7.
 */
public record MissileStruct(@Nullable ItemCustomMissilePart warhead,
                            @Nullable ItemCustomMissilePart fuselage,
                            @Nullable ItemCustomMissilePart fins,
                            @Nullable ItemCustomMissilePart thruster) {

    private static final Codec<ItemCustomMissilePart> PART = BuiltInRegistries.ITEM.byNameCodec().comapFlatMap(
            item -> item instanceof ItemCustomMissilePart part
                    ? DataResult.success(part)
                    : DataResult.error(() -> "not a missile part: " + item),
            part -> part);

    public static final Codec<MissileStruct> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PART.optionalFieldOf("warhead").forGetter(s -> Optional.ofNullable(s.warhead())),
            PART.optionalFieldOf("fuselage").forGetter(s -> Optional.ofNullable(s.fuselage())),
            PART.optionalFieldOf("fins").forGetter(s -> Optional.ofNullable(s.fins())),
            PART.optionalFieldOf("thruster").forGetter(s -> Optional.ofNullable(s.thruster()))
    ).apply(instance, (w, f, s, t) -> new MissileStruct(w.orElse(null), f.orElse(null), s.orElse(null),
            t.orElse(null))));


    /** 1.7 sends this rather than nothing when a launcher holds no missile. */
    public static final MissileStruct EMPTY =
            new MissileStruct((ItemCustomMissilePart) null, null, null, null);

    public MissileStruct(ItemStack w, ItemStack f, ItemStack s, ItemStack t) {
        this(ItemCustomMissilePart.part(w), ItemCustomMissilePart.part(f), ItemCustomMissilePart.part(s),
                ItemCustomMissilePart.part(t));
    }

    private static @Nullable ItemCustomMissilePart cast(Optional<? extends Item> item) {
        return item.orElse(null) instanceof ItemCustomMissilePart part ? part : null;
    }

    /** 1.7 writeToByteBuffer dropped any part sitting in the wrong slot; keep that guard on the read side. */
    public MissileStruct sanitised() {
        return new MissileStruct(
                slot(this.warhead, PartType.WARHEAD),
                slot(this.fuselage, PartType.FUSELAGE),
                slot(this.fins, PartType.FINS),
                slot(this.thruster, PartType.THRUSTER));
    }

    private static @Nullable ItemCustomMissilePart slot(@Nullable ItemCustomMissilePart part, PartType expected) {
        return part != null && part.type == expected ? part : null;
    }

    /**
     * 1.7 MissileMultipart.getHeight: how tall the assembled missile stands. The fins ride the fuselage rather than
     * stacking on it, so they add nothing.
     */
    public float height() {
        float height = 0F;
        if(this.warhead != null && this.warhead.type == PartType.WARHEAD) height += this.warhead.height;
        if(this.fuselage != null && this.fuselage.type == PartType.FUSELAGE) height += this.fuselage.height;
        if(this.thruster != null && this.thruster.type == PartType.THRUSTER) height += this.thruster.height;
        return height;
    }

    public boolean isComplete() {
        return this.warhead != null && this.fuselage != null && this.thruster != null;
    }
}
