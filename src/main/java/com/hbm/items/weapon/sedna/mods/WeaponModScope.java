package com.hbm.items.weapon.sedna.mods;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.ModGunItems;
import com.hbm.Constants;
import dev.golem.core.Ids;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
/** Scope stat hook; the client renderer supplies the actual overlay texture when one is available. */
public class WeaponModScope extends WeaponModBase {
    private final Identifier overrideTexture;
    public WeaponModScope(int id) { this(id, null); }
    public WeaponModScope(int id, Identifier texture) { super(id, "SCOPE"); overrideTexture = texture; }
    @Override public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (GunConfig.O_SCOPETEXTURE.equals(key)) {
            if (overrideTexture != null) return cast(overrideTexture, base);
            if (gun.is(ModGunItems.gun_heavy_revolver.get())) return cast(Ids.of(Constants.MOD_ID, "textures/misc/scope_44.png"), base);
            if (((ItemGunBaseNT) gun.getItem()).quality == WeaponQuality.UTILITY) return cast(Ids.of(Constants.MOD_ID, "textures/misc/scope_tool.png"), base);
            return cast(Ids.of(Constants.MOD_ID, "textures/misc/scope_bolt.png"), base);
        }
        return GunConfig.B_HIDECROSSHAIR.equals(key) ? cast(true, base) : base;
    }
}
