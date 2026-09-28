package com.hbm.items.special;

/**
 * The fluid cell's model/name variants, kept free of Minecraft classes because the build-time Dejson and
 * recipe declarations read them.
 *
 * <p>1.7 had one item per cell ({@code ModItems.cell_empty, cell_uf6, cell_puf6, cell_antimatter,
 * cell_deuterium, cell_tritium, cell_sas3, cell_anti_schrabidium}, in that declaration order). This port folds
 * them into one {@link ItemCell} whose fluid lives in components, so the model variant is the cell's own index
 * in that order — NOT the fluid id. Keying model and name on fluid ids broke silently once the port's fluid
 * list diverged from 1.12's numbering (SAS3 is id 31 here, the table said 30): a full SAS3 cell showed as
 * "Empty Cell" with the empty icon, and UF6/PUF6 cells showed the neighbouring cell's name and icon.
 */
public final class CellVariants {
    public static final int EMPTY = 0;
    public static final int UF6 = 1;
    public static final int PUF6 = 2;
    public static final int ANTIMATTER = 3;
    public static final int DEUTERIUM = 4;
    public static final int TRITIUM = 5;
    public static final int SAS3 = 6;
    public static final int ANTI_SCHRABIDIUM = 7;

    /** The 1.7 item-id suffix of each variant, in variant order ({@code item.hbm.cell_<name>}, {@code cell_<name>.png}). */
    public static final String[] NAMES = {
            "empty", "uf6", "puf6", "antimatter", "deuterium", "tritium", "sas3", "anti_schrabidium"};

    private CellVariants() {}
}
