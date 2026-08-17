package com.hbm.items;

import com.hbm.Constants;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.factory.XFactory357;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.sound.ModSounds;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/**
 * The Sedna guns.
 *
 * <p>Both entries are ports of upstream's own declarations, stat for stat — {@code gun_debug} from
 * {@code GunFactory} and {@code gun_light_revolver} from {@code XFactory357}. They are the proving pair
 * for the gun engine: the debug gun is the smallest complete weapon in the mod (one receiver, one
 * round), and the revolver is the reference implementation of the standard fire path, so anything that
 * works for it works for the other sixty-odd guns as data.
 *
 * <p>The bullet configs a magazine names must exist first, so {@link GunFactory#init()} runs before the
 * suppliers here are resolved. Registry suppliers are only called during registration, which is well
 * after mod bootstrap calls it.
 *
 * <p>Deferred, and simply not set on these configs: {@code smoke}, {@code orchestra}, {@code anim} and
 * the per-gun {@code recoil} lambda — all client presentation, all optional upstream. The item icons
 * are placeholders borrowed from the legacy revolver sprites, because upstream's Sedna guns have no
 * item texture at all: they are drawn by a TEISR from an OBJ model, and the item JSON's {@code layer0}
 * points at a file that does not exist.
 */
public final class ModGunItems {

    private ModGunItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** The smallest complete gun in the mod: one receiver, one round, no frills. */
    public static final RegistryObject<ItemGunBaseNT> gun_debug = ITEMS.registerItem("gun_debug", props ->
            new ItemGunBaseNT(props, WeaponQuality.DEBUG, new GunConfig()
                    .dura(600F).draw(15).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .rec(new Receiver(0)
                            .dmg(10F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 12).addConfigs(GunFactory.ammo_debug))
                            .offset(0.75, -0.0625, -0.3125D)
                            .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE))
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .pt(Lego.LAMBDA_TOGGLE_AIM)
                    .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER)));

    /** The break-action .357 — upstream's reference weapon for the standard configuration. */
    public static final RegistryObject<ItemGunBaseNT> gun_light_revolver = ITEMS.registerItem("gun_light_revolver", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(7.5F).delay(16).reload(55).jam(45)
                            .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 6).addConfigs(XFactory357.all()))
                            .offset(0.75, -0.0625, -0.3125D)
                            .setupStandardFire())
                    .setupStandardConfiguration()));

    static {
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_debug);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_light_revolver);
    }

    public static void init() {}
}
