package com.hbm.items.weapon.sedna.mods;

/** Common priority and generic-cast helpers for the old modifier implementations. */
public abstract class WeaponModBase implements IWeaponMod {
    public static final int PRIORITY_SET = 1_000_000;
    public static final int PRIORITY_MULTIPLICATIVE = 1_000;
    public static final int PRIORITY_ADDITIVE = 500;
    public static final int PRIORITY_MULT_FINAL = -1;
    public String[] slots;
    public int priority;

    protected WeaponModBase(int id, String... slots) {
        this.slots = slots;
        this.priority = 0;
        XWeaponModManager.idToMod.put(id, this);
    }
    public WeaponModBase setPriority(int priority) { this.priority = priority; return this; }
    @Override public int getModPriority() { return priority; }
    @Override public String[] getSlots() { return slots; }
    @SuppressWarnings("unchecked") public <T> T cast(Object value, T ignored) { return (T) value; }
}
