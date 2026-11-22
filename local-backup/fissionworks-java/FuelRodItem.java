package fissionworks;

import fissionworks.sim.Zirnox;
import net.minecraft.world.item.Item;

/** A ZIRNOX fuel rod. Its durability bar is the burnup: 0 damage is fresh, 1000 would be spent. */
public class FuelRodItem extends Item {
    public static final int LIFE = 1000;
    private final Zirnox.Fuel fuel;

    public FuelRodItem(Properties properties, Zirnox.Fuel fuel) {
        super(properties);
        this.fuel = fuel;
    }

    public Zirnox.Fuel fuel() {
        return fuel;
    }
}
