package com.hbm.tileentity;

/** Explicit item-form data; the block entity's complete world save remains independent. */
public interface PersistentDrop {
    String[] persistentKeys();

    //? if >=1.21.1 {
    void writePersistent(net.minecraft.core.component.DataComponentMap.Builder components);
    //? if >=26.1 {
    void readPersistent(net.minecraft.core.component.DataComponentGetter components);
    //?} else {
    /*void readPersistent(net.minecraft.core.component.DataComponentMap components);
    *///?}
    //?}
}
