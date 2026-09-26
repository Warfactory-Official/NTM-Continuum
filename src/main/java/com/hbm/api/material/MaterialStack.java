package com.hbm.api.material;

/** A quantity of molten material, in quanta: nugget 8, ingot 72, block 648. */
public class MaterialStack {
    public final Material material;
    public int amount;

    public MaterialStack(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public MaterialStack copy() {
        return new MaterialStack(material, amount);
    }
}
