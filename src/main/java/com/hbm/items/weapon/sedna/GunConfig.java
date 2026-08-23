package com.hbm.items.weapon.sedna;

import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import java.util.function.BiConsumer;
import net.minecraft.world.item.ItemStack;

/**
 * One whole weapon's worth of configuration: its {@link Receiver}s, how worn it can get before it jams,
 * what each button does, and the state machine that ties them together. A gun item holds an array of
 * these — akimbo pistols are one item with two configs.
 *
 * <p>Ported from 1.12.2 {@code com.hbm.items.weapon.sedna.GunConfig}. As with {@link Receiver}, the
 * {@code XWeaponModManager.eval} indirection on every getter is collapsed to a direct field read until
 * the weapon-mod system is ported; signatures and {@code *_DNA} names are unchanged so it can return.
 *
 * <p>Not ported in this slice, and simply never set: {@code smoke} (muzzle smoke nodes),
 * {@code orchestra} (the tick-indexed reload sound timeline), {@code anim} (the bus-animation lookup)
 * and {@code scopeTexture}/{@code thermalSights}. All of them are optional by design upstream — a null
 * animation lambda is already a no-op there — so a gun declared without them works, driven purely by
 * the server-side receiver timings.
 */
public class GunConfig {

    public int index;
    /** Receivers in use, primary first; index 1 is conventionally an underbarrel. */
    protected Receiver[] receivers_DNA;
    protected float durability_DNA;
    protected int drawDuration_DNA = 0;
    protected int inspectDuration_DNA = 0;
    protected boolean inspectCancel_DNA = true;
    protected Crosshair crosshair_DNA;
    protected boolean hideCrosshair_DNA = true;
    protected boolean reloadRequiresTypeChange_DNA = false;
    protected boolean reloadAnimationsSequential_DNA;
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

    public Receiver[] getReceivers(ItemStack stack) { return receivers_DNA; }
    public float getDurability(ItemStack stack) { return durability_DNA; }
    public int getDrawDuration(ItemStack stack) { return drawDuration_DNA; }
    public int getInspectDuration(ItemStack stack) { return inspectDuration_DNA; }
    public boolean getInspectCancel(ItemStack stack) { return inspectCancel_DNA; }
    public Crosshair getCrosshair(ItemStack stack) { return crosshair_DNA; }
    public boolean getHideCrosshair(ItemStack stack) { return hideCrosshair_DNA; }
    public boolean getReloadChangesType(ItemStack stack) { return reloadRequiresTypeChange_DNA; }
    public boolean getReloadAnimSequential(ItemStack stack) { return reloadAnimationsSequential_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressPrimary(ItemStack stack) { return onPressPrimary_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressSecondary(ItemStack stack) { return onPressSecondary_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressTertiary(ItemStack stack) { return onPressTertiary_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getPressReload(ItemStack stack) { return onPressReload_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleasePrimary(ItemStack stack) { return onReleasePrimary_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseSecondary(ItemStack stack) { return onReleaseSecondary_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseTertiary(ItemStack stack) { return onReleaseTertiary_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getReleaseReload(ItemStack stack) { return onReleaseReload_DNA; }
    public BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> getDecider(ItemStack stack) { return decider_DNA; }

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
    public GunConfig reloadChangeType(boolean flag) { this.reloadRequiresTypeChange_DNA = flag; return this; }
    public GunConfig reloadSequential(boolean flag) { this.reloadAnimationsSequential_DNA = flag; return this; }

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
