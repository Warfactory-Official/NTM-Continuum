package com.hbm.api.content;

import com.hbm.scripting.ScriptedContent;
import com.hbm.scripting.ScriptSpecs;
import com.hbm.scripting.NTMEvents;
import com.hbm.Constants;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import dev.golem.core.Ids;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * {@code NTMEvents.fluids}. Tanks save the id; references: {@code 'mithril_melt'} (kubejs), {@code 'ns:path'}, or a
 * built-in name.
 *
 * <pre>
 * e.create('mithril_melt', 0x8080FF)                  kubejs:mithril_melt; GUI texture custom_water, tinted
 * e.create('mymod:sludge', 0x405020, 'custom_toxin')  custom_water custom_oil custom_lava custom_toxin,
 *                                                      or a full id: 'kubejs:textures/gui/fluids/x.png'
 *   .displayName('Sludge').tint(0x405020).temperature(80)
 *   .corrosive(40).moderator(1.1)
 *   .flammable(200000).combustible('HIGH', 400000).gaseous()     LOW MEDIUM HIGH AERO GAS
 *   .heatable(h => h.eff('BOILER', 1).step(100, 1, 'STEAM', 10))  BOILER HEATEXCHANGER PWR ICF PA
 *   .coolable('WATER', 1, 1, 200, c => c.eff('TURBINE', 1))       TURBINE HEATEXCHANGER
 * e.get('DIESEL').corrosive(5)
 * </pre>
 */
public final class FluidsEvent {

    FluidsEvent() {}

    public FluidBuilder create(String id, int color) {
        return create(id, color, "custom_water");
    }

    /** {@code id} without a namespace = {@code kubejs:<id>}. */
    public FluidBuilder create(String id, int color, String texture) {
        var textureId = texture.indexOf(':') >= 0 ? Ids.parse(texture)
                : Ids.of(Constants.MOD_ID, "textures/gui/fluids/" + texture + ".png");
        var fluidId = id.indexOf(':') >= 0 ? Ids.parse(id.toLowerCase(java.util.Locale.US))
                : Ids.of("kubejs", id.toLowerCase(java.util.Locale.US));
        return new FluidBuilder(new FluidType(fluidId, color, textureId));
    }

    /** {@code texture} null = {@code <namespace>:textures/gui/fluids/<path>.png}. */
    public FluidBuilder register(Identifier id, int color, @Nullable Identifier texture) {
        return new FluidBuilder(new FluidType(id, color, texture));
    }

    public FluidBuilder register(Identifier id, int color) {
        return register(id, color, null);
    }

    public FluidBuilder get(String name) {
        return new FluidBuilder(ScriptSpecs.fluidType(name));
    }

    public static final class FluidBuilder {

        private final FluidType type;

        public FluidType type() {
            return type;
        }

        private FluidBuilder(FluidType type) {
            this.type = type;
        }

        public FluidBuilder displayName(String name) {
            ScriptedContent.lang(type.getTranslationKey(), name);
            return this;
        }

        public FluidBuilder tint(int tint) {
            type.setTint(tint);
            return this;
        }

        public FluidBuilder temperature(int celsius) {
            type.setTemp(celsius);
            return this;
        }

        public FluidBuilder corrosive(int rating) {
            type.addTraits(new FT_Corrosive(rating));
            return this;
        }

        public FluidBuilder moderator(double multiplier) {
            type.addTraits(new FT_PWRModerator(multiplier));
            return this;
        }

        public FluidBuilder flammable(long heatPerBucket) {
            FluidCombustion.setFlammable(type, heatPerBucket);
            return this;
        }

        public FluidBuilder combustible(String grade, long energyPerBucket) {
            FluidCombustion.setCombustible(type, ScriptSpecs.constant(FluidCombustion.FuelGrade.class, grade), energyPerBucket);
            return this;
        }

        public FluidBuilder gaseous() {
            FluidCombustion.setGaseous(type);
            return this;
        }

        public FluidBuilder heatable(Consumer<HeatableBuilder> spec) {
            FT_Heatable trait = new FT_Heatable();
            spec.accept(new HeatableBuilder(trait));
            type.addTraits(trait);
            return this;
        }

        public FluidBuilder coolable(String coolsTo, int amountReq, int amountProduced, int heatEnergy,
                Consumer<CoolableBuilder> spec) {
            FT_Coolable trait = new FT_Coolable(ScriptSpecs.fluidType(coolsTo), amountReq, amountProduced, heatEnergy);
            spec.accept(new CoolableBuilder(trait));
            type.addTraits(trait);
            return this;
        }
    }

    public static final class HeatableBuilder {

        private final FT_Heatable trait;

        private HeatableBuilder(FT_Heatable trait) {
            this.trait = trait;
        }

        public HeatableBuilder eff(String heating, double efficiency) {
            trait.setEff(ScriptSpecs.constant(FT_Heatable.HeatingType.class, heating), efficiency);
            return this;
        }

        public HeatableBuilder step(int heat, int amountReq, String produces, int amountProduced) {
            trait.addStep(heat, amountReq, ScriptSpecs.fluidType(produces), amountProduced);
            return this;
        }
    }

    public static final class CoolableBuilder {

        private final FT_Coolable trait;

        private CoolableBuilder(FT_Coolable trait) {
            this.trait = trait;
        }

        public CoolableBuilder eff(String cooling, double efficiency) {
            trait.setEff(ScriptSpecs.constant(FT_Coolable.CoolingType.class, cooling), efficiency);
            return this;
        }
    }
}
