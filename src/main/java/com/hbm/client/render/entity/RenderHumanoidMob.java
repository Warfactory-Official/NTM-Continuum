package com.hbm.client.render.entity;

import com.hbm.entity.mob.EntityDummy;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.main.ResourceManager;
import dev.golem.core.render.living.CompatHumanoidMobRenderer;
import dev.golem.core.render.living.HumanoidBody;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * 1.7 RenderGhost, RenderDummy and RenderFBI: all three are a bare {@code ModelBiped(0.0F)} plus a skin. The mesh is
 * baked at 64x32 because that is the sheet 1.7's ModelBiped was authored against, and all three NTM skins are that
 * size - the 64x64 player layout would sample them at half height.
 */
public class RenderHumanoidMob<T extends Mob> extends CompatHumanoidMobRenderer<T> {

    private static final LayerDefinition BIPED_64X32 =
            LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0F), 64, 32);

    private RenderHumanoidMob(EntityRendererProvider.Context context, Identifier texture, boolean translucent) {
        super(context, 0.5F, new Identifier[]{texture}, HumanoidBody.plain(BIPED_64X32, translucent));
    }

    // 1.7 RenderGhost wrapped the whole draw in SRC_ALPHA/ONE_MINUS_SRC_ALPHA with the alpha test off
    public static RenderHumanoidMob<EntityGhost> ghost(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(context, ResourceManager.ghost_tex, true);
    }

    public static RenderHumanoidMob<EntityDummy> dummy(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(context, ResourceManager.dummy_tex, false);
    }

    public static RenderHumanoidMob<EntityFBI> fbi(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(context, ResourceManager.fbi_tex, false);
    }
}
