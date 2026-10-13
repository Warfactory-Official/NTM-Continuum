package com.hbm.items.machine;

import com.hbm.items.special.ItemCustomLore;
import net.minecraft.world.item.Item;

/**
 * The shredder's cutting blades, a wear part, not a tool. Two go into the shredder (slots 27/28) and
 * take one point of damage per completed shredding cycle; the machine refuses to run once either pair
 * is missing or fully worn (see {@code TileEntityMachineShredder.getGearLeft/getGearRight}).
 *
 * <p>Faithful to the 1.12.2 {@code ItemBlades}: a lore item with a max stack size of 1 and a durability.
 * {@code blades_desh} is registered with <b>no</b> durability, which in 26.1 (as in 1.12.2's
 * {@code maxDamage == 0}) makes it unbreakable: the endgame blade.
 *
 * <p>Worn blades are <b>not</b> destroyed: they sit at full damage and can be pulled back out (the
 * container/hopper only permits extracting a blade once it is spent), matching the original.
 */
public class ItemBlades extends ItemCustomLore {

    public ItemBlades(Item.Properties properties) {
        super(properties);
    }
}
