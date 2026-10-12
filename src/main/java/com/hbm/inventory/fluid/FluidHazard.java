package com.hbm.inventory.fluid;

import com.google.gson.JsonObject;
import java.util.Locale;

/**
 * 1.7 {@code FluidType}'s NFPA 704 numbers and symbol ({@code poison}, {@code flammability}, {@code reactivity},
 * {@code EnumSymbol}), the four constructor arguments every 1.7 fluid took. 1.7 shows them only as the hazard diamond
 * ({@code DiamondPronter}) on the fluid tank, the barrels, the BAT-9000 and the big tank; a fluid with no diamond
 * values draws an all-zero diamond, never none. In {@code fluid_property} files it is
 * {@code "hazard": {"poison": 3, "flammability": 4, "reactivity": 0, "symbol": "cryogenic"}} (absent: all zero, no
 * symbol, which is also what 1.7 gave a fluid it met from another mod in {@code CompatFluidRegistry}).
 */
public record FluidHazard(int poison, int flammability, int reactivity, Symbol symbol) {

    public static final FluidHazard NONE = new FluidHazard(0, 0, 0, Symbol.NONE);

    /**
     * 1.7 {@code EnumSymbol} (where the symbol sits on {@code danger_diamond.png} is the renderer's business). 1.7 spells
     * the cryogenic one {@code CROYGENIC}; the file names it {@code cryogenic}.
     */
    public enum Symbol {
        NONE, RADIATION, NOWATER, ACID, ASPHYXIANT, CRYOGENIC, ANTIMATTER, OXIDIZER;

        public String fileName() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Symbol byFileName(String name) {
            return valueOf(name.toUpperCase(Locale.ROOT));
        }
    }

    static FluidHazard read(JsonObject json) {
        return new FluidHazard(
                json.has("poison") ? json.get("poison").getAsInt() : 0,
                json.has("flammability") ? json.get("flammability").getAsInt() : 0,
                json.has("reactivity") ? json.get("reactivity").getAsInt() : 0,
                json.has("symbol") ? Symbol.byFileName(json.get("symbol").getAsString()) : Symbol.NONE);
    }

    JsonObject write() {
        JsonObject json = new JsonObject();
        json.addProperty("poison", poison);
        json.addProperty("flammability", flammability);
        json.addProperty("reactivity", reactivity);
        json.addProperty("symbol", symbol.fileName());
        return json;
    }
}
