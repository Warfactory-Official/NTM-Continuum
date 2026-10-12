package com.hbm.items.weapon.sedna.mods;

/** Common priority and generic-cast helpers for the old modifier implementations. */
public abstract class WeaponModBase implements IWeaponMod {
    public static final int PRIORITY_SET = 1_000_000;
    public static final int PRIORITY_MULTIPLICATIVE = 1_000;
    public static final int PRIORITY_ADDITIVE = 500;
    public static final int PRIORITY_MULT_FINAL = -1;
    public String[] slots;
    public int priority;

    /** HBM's modifiers: {@code id} is the 1.7 numeric id, kept only as the runtime handle (the save uses the name). */
    protected WeaponModBase(int id, String... slots) {
        this.slots = slots;
        this.priority = 0;
        XWeaponModManager.idToMod.put(id, this);
    }

    /** An addon's modifier, registered under {@code name} ({@code "<namespace>:<path>"}); it gets a runtime handle. */
    protected WeaponModBase(String name, String[] slots) {
        this.slots = slots;
        this.priority = 0;
        XWeaponModManager.register(name, this);
    }
    public WeaponModBase setPriority(int priority) { this.priority = priority; return this; }
    @Override public int getModPriority() { return priority; }
    @Override public String[] getSlots() { return slots; }
    @SuppressWarnings("unchecked") public <T> T cast(Object value, T ignored) { return (T) value; }
}
