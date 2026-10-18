package com.hbm.tileentity.turret;

import com.hbm.items.weapon.sedna.factory.XFactoryTurret;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.tileentity.ModBlockEntities;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The 5.56 mm gatling: the Chekhov in a smaller calibre, firing at less than half the rate. */
public class TileEntityTurretFriendly extends TileEntityTurretChekhov {

    private static @Nullable List<BulletConfig> configs;

    public TileEntityTurretFriendly(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_FRIENDLY.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        if (configs == null) {
            configs = List.of(XFactory556mm.r556_sp, XFactory556mm.r556_fmj, XFactory556mm.r556_jhp,
                    XFactory556mm.r556_ap);
        }
        return configs;
    }

    @Override
    public String getDefaultName() {
        return "container.turretFriendly";
    }

    @Override
    public Identifier guiTexture() {
        return turretGui("friendly");
    }

    @Override
    public int getDelay() {
        return 5;
    }

    @Override
    public float getBaseDamage() { return XFactoryTurret.TURRET_FRIENDLY_DAMAGE.get(); }
}
