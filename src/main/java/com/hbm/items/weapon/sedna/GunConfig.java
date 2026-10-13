package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.hud.IHUDComponent;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import dev.golem.core.render.anim.BusAnimation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/**
 * One whole weapon's worth of configuration: its {@link Receiver}s, how worn it can get before it jams,
 * what each button does, and the state machine that ties them together. A gun item holds an array of
 * these: akimbo pistols are one item with two configs.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.GunConfig}. As with {@link Receiver}, every
 * getter keeps the upstream {@code XWeaponModManager.eval} hook, so per-stack modifiers can change stats
 * without changing the gun declarations.
 *
 * <p>Smoke and orchestra callbacks are common hooks; their client sound/particle consumers are separate.
 * Animation tables stay in the client registration path so this class never names a client-only renderer
 * type.
 */
public class GunConfig {

    public static final String O_RECEIVERS = "O_RECEIVERS";
    public static final String F_DURABILITY = "F_DURABILITY";
    public static final String I_DRAWDURATION = "I_DRAWDURATION";
    public static final String I_INSPECTDURATION = "I_INSPECTDURATION";
    public static final String I_INSPECTCANCEL = "I_INSPECTCANCEL";
    public static final String O_CROSSHAIR = "O_CROSSHAIR";
    public static final String B_HIDECROSSHAIR = "B_HIDECROSSHAIR";
    public static final String B_THERMALSIGHTS = "B_THERMALSIGHTS";
    public static final String B_RELOADREQUIRESTYPECHANGE = "B_RELOADREQUIRESTYPECHANGE";
    public static final String B_RELOADANIMATIONSEQUENTIAL = "B_RELOADANIMATIONSEQUENTIAL";
    public static final String O_SCOPETEXTURE = "O_SCOPETEXTURE";
    public static final String CON_SMOKE = "CON_SMOKE";
    public static final String CON_ORCHESTRA = "CON_ORCHESTRA";
    public static final String CON_ONPRESSPRIMARY = "CON_ONPRESSPRIMARY";
    public static final String CON_ONPRESSSECONDARY = "CON_ONPRESSSECONDARY";
    public static final String CON_ONPRESSTERTIARY = "CON_ONPRESSTERTIARY";
    public static final String CON_ONPRESSRELOAD = "CON_ONPRESSRELOAD";
    public static final String CON_ONRELEASEPRIMARY = "CON_ONRELEASEPRIMARY";
    public static final String CON_ONRELEASESECONDARY = "CON_ONRELEASESECONDARY";
    public static final String CON_ONRELEASETERTIARY = "CON_ONRELEASETERTIARY";
    public static final String CON_ONRELEASERELOAD = "CON_ONRELEASERELOAD";
    public static final String CON_DECIDER = "CON_DECIDER";
    public static final String FUN_ANIMNATIONS = "FUN_ANIMNATIONS";
    public static final String O_HUDCOMPONENTS = "O_HUDCOMPONENTS";

    /** Mutable client smoke ribbons, isolated by owning hand while retaining config-local history. */
    private final List<List<ItemGunBaseNT.SmokeNode>> smokeNodesByHand = List.of(
            new ArrayList<>(), new ArrayList<>());

    public List<ItemGunBaseNT.SmokeNode> getSmokeNodes(InteractionHand hand) {
        return smokeNodesByHand.get(hand.ordinal());
    }

    public int index;
    /** Receivers in use, primary first; index 1 is conventionally an underbarrel. */
    protected Receiver[] receivers_DNA;
    protected float durability_DNA;
    protected int drawDuration_DNA = 0;
    protected int inspectDuration_DNA = 0;
    protected boolean inspectCancel_DNA = true;
    protected Crosshair crosshair_DNA;
    protected boolean hideCrosshair_DNA = true;
    protected boolean thermalSights_DNA = false;
    protected boolean reloadRequiresTypeChange_DNA = false;
    protected boolean reloadAnimationsSequential_DNA;
    protected Identifier scopeTexture_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> smokeHandler_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> orchestra_DNA;
    /** Button handlers, press side. */
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onPressPrimary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onPressSecondary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onPressTertiary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onPressReload_DNA;
    /** Button handlers, release side. */
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onReleasePrimary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onReleaseSecondary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onReleaseTertiary_DNA;
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> onReleaseReload_DNA;
    /** The state machine driving the gun's overall behaviour. */
    protected BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> decider_DNA;
    protected BiFunction<ItemStack, GunAnimation, BusAnimation> animation_DNA;
    protected IHUDComponent[] hudComponents_DNA;

    public Receiver[] getReceivers(ItemStack stack) { return XWeaponModManager.eval(receivers_DNA, stack, O_RECEIVERS, this, index); }
    public float getDurability(ItemStack stack) { return XWeaponModManager.eval(durability_DNA, stack, F_DURABILITY, this, index); }
    public int getDrawDuration(ItemStack stack) { return XWeaponModManager.eval(drawDuration_DNA, stack, I_DRAWDURATION, this, index); }
    public int getInspectDuration(ItemStack stack) { return XWeaponModManager.eval(inspectDuration_DNA, stack, I_INSPECTDURATION, this, index); }
    public boolean getInspectCancel(ItemStack stack) { return XWeaponModManager.eval(inspectCancel_DNA, stack, I_INSPECTCANCEL, this, index); }
    public Crosshair getCrosshair(ItemStack stack) { return XWeaponModManager.eval(crosshair_DNA, stack, O_CROSSHAIR, this, index); }
    public boolean getHideCrosshair(ItemStack stack) { return XWeaponModManager.eval(hideCrosshair_DNA, stack, B_HIDECROSSHAIR, this, index); }
    public boolean hasThermalSights(ItemStack stack) { return XWeaponModManager.eval(thermalSights_DNA, stack, B_THERMALSIGHTS, this, index); }
    public boolean getReloadChangesType(ItemStack stack) { return XWeaponModManager.eval(reloadRequiresTypeChange_DNA, stack, B_RELOADREQUIRESTYPECHANGE, this, index); }
    public boolean getReloadAnimSequential(ItemStack stack) { return XWeaponModManager.eval(reloadAnimationsSequential_DNA, stack, B_RELOADANIMATIONSEQUENTIAL, this, index); }
    public Identifier getScopeTexture(ItemStack stack) { return XWeaponModManager.eval(scopeTexture_DNA, stack, O_SCOPETEXTURE, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getSmokeHandler(ItemStack stack) { return XWeaponModManager.eval(smokeHandler_DNA, stack, CON_SMOKE, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getOrchestra(ItemStack stack) { return XWeaponModManager.eval(orchestra_DNA, stack, CON_ORCHESTRA, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressPrimary(ItemStack stack) { return XWeaponModManager.eval(onPressPrimary_DNA, stack, CON_ONPRESSPRIMARY, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressSecondary(ItemStack stack) { return XWeaponModManager.eval(onPressSecondary_DNA, stack, CON_ONPRESSSECONDARY, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressTertiary(ItemStack stack) { return XWeaponModManager.eval(onPressTertiary_DNA, stack, CON_ONPRESSTERTIARY, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressReload(ItemStack stack) { return XWeaponModManager.eval(onPressReload_DNA, stack, CON_ONPRESSRELOAD, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleasePrimary(ItemStack stack) { return XWeaponModManager.eval(onReleasePrimary_DNA, stack, CON_ONRELEASEPRIMARY, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseSecondary(ItemStack stack) { return XWeaponModManager.eval(onReleaseSecondary_DNA, stack, CON_ONRELEASESECONDARY, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseTertiary(ItemStack stack) { return XWeaponModManager.eval(onReleaseTertiary_DNA, stack, CON_ONRELEASETERTIARY, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseReload(ItemStack stack) { return XWeaponModManager.eval(onReleaseReload_DNA, stack, CON_ONRELEASERELOAD, this, index); }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getDecider(ItemStack stack) { return XWeaponModManager.eval(decider_DNA, stack, CON_DECIDER, this, index); }
    public BiFunction<ItemStack, GunAnimation, BusAnimation> getAnimation(ItemStack stack) {
        return XWeaponModManager.eval(animation_DNA, stack, FUN_ANIMNATIONS, this, index);
    }
    public IHUDComponent[] getHUDComponents(ItemStack stack) {
        return XWeaponModManager.eval(hudComponents_DNA, stack, O_HUDCOMPONENTS, this, index);
    }

    public GunConfig rec(Receiver... receivers) {
        this.receivers_DNA = receivers;
        for (Receiver r : receivers_DNA) r.parent = this;
        return this;
    }

    public GunConfig dura(float dura) { this.durability_DNA = dura; return this; }
    public GunConfig draw(int draw) { this.drawDuration_DNA = draw; return this; }
    public GunConfig inspect(int inspect) { this.inspectDuration_DNA = inspect; return this; }
    public GunConfig inspectCancel(boolean flag) { this.inspectCancel_DNA = flag; return this; }
    public GunConfig crosshair(Crosshair crosshair) { this.crosshair_DNA = crosshair; return this; }
    public GunConfig hideCrosshair(boolean flag) { this.hideCrosshair_DNA = flag; return this; }
    public GunConfig thermalSights(boolean flag) { this.thermalSights_DNA = flag; return this; }
    public GunConfig reloadChangeType(boolean flag) { this.reloadRequiresTypeChange_DNA = flag; return this; }
    public GunConfig reloadSequential(boolean flag) { this.reloadAnimationsSequential_DNA = flag; return this; }
    public GunConfig scopeTexture(Identifier texture) { this.scopeTexture_DNA = texture; return this; }
    public GunConfig smoke(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> smoke) { this.smokeHandler_DNA = smoke; return this; }
    public GunConfig orchestra(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> orchestra) { this.orchestra_DNA = orchestra; return this; }

    // press
    public GunConfig pp(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onPressPrimary_DNA = lambda; return this; }
    public GunConfig ps(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onPressSecondary_DNA = lambda; return this; }
    public GunConfig pt(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onPressTertiary_DNA = lambda; return this; }
    public GunConfig pr(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onPressReload_DNA = lambda; return this; }

    // release
    public GunConfig rp(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onReleasePrimary_DNA = lambda; return this; }
    public GunConfig rs(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onReleaseSecondary_DNA = lambda; return this; }
    public GunConfig rt(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onReleaseTertiary_DNA = lambda; return this; }
    public GunConfig rr(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.onReleaseReload_DNA = lambda; return this; }

    public GunConfig decider(BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> lambda) { this.decider_DNA = lambda; return this; }
    public GunConfig anim(BiFunction<ItemStack, GunAnimation, BusAnimation> lambda) { this.animation_DNA = lambda; return this; }
    public GunConfig hud(IHUDComponent... components) { this.hudComponents_DNA = components; return this; }

    /**
     * The standard button/decider package: fire on primary, reload on the reload key, aim toggle on
     * tertiary, and the standard state machine with jamming and auto-fire.
     */
    public GunConfig setupStandardConfiguration() {
        this.pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY);
        this.pr(Lego.LAMBDA_STANDARD_RELOAD);
        this.pt(Lego.LAMBDA_TOGGLE_AIM);
        this.decider(GunStateDecider.LAMBDA_STANDARD_DECIDER);
        return this;
    }
}
