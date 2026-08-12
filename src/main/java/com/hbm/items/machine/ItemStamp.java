package com.hbm.items.machine;

import net.minecraft.world.item.Item;

/**
 * A press stamp/die: combined with an ingredient in a (burner/electric) press it produces a shaped
 * output. Each stamp carries a {@link StampType} that, together with the ingredient, keys a
 * {@link com.hbm.inventory.recipes.PressRecipes} entry. Ported from the 1.12.2 {@code ItemStamp}.
 *
 * <p>The original stored a {@code maxDamage} per stamp (stone 32 … obsidian 512; desh and casing
 * stamps unbreakable = 0). Here the durability is set on the {@link Item.Properties} at registration
 * (see {@link com.hbm.items.machine.ModStampItems}) and the press damages the stamp through the
 * vanilla {@code DataComponents.DAMAGE} component each time it presses.
 */
public class ItemStamp extends Item {

    private final StampType type;

    public ItemStamp(Properties properties, StampType type) {
        super(properties);
        this.type = type;
    }

    public StampType getStampType() {
        return type;
    }

    /** Stamp shape categories (the 1.12.2 enum, kept verbatim for recipe-key compatibility). */
    public enum StampType {
        FLAT,
        PLATE,
        WIRE,
        CIRCUIT,
        C357,
        C44,
        C50,
        C9,
        PRINTING1,
        PRINTING2,
        PRINTING3,
        PRINTING4,
        PRINTING5,
        PRINTING6,
        PRINTING7,
        PRINTING8
    }
}
