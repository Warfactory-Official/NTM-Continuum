package com.hbm.items.armor;

/**
 * An armour item that is not an {@code ArmorFSB} but still draws a modelled body on its wearer - 1.7's
 * {@code getArmorModel} override on a plain {@code ItemArmor} ({@code ArmorModel}, {@code ArmorAshGlasses},
 * {@code ArmorHat}, {@code ArmorNo9}). The name selects the worn model on the client.
 */
public interface IWornModel {

    String wornModel();
}
