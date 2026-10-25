package com.hbm.client.render.armor;

import com.hbm.client.model.WavefrontObj;
import dev.golem.core.Ids;
import dev.golem.core.render.gem.Gem;
import dev.golem.core.render.gem.Surface;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * 1.7 {@code ModelMan}: the body drawn instead of an Astolfized player's skin. The players come from the server
 * ({@code ManlySyncPayload}, 1.7's {@code PermaSyncHandler.boykissers}), so others see it too.
 */
public final class ModelMan extends ArmorRenderer {

    private static final Identifier OBJ = Ids.of("hbm", "models/armor/player_fem.obj");
    private static final Surface SKIN = Surface.cutout("hbm:textures/entity/player_fem.png");

    private static final IntOpenHashSet PLAYERS = new IntOpenHashSet();
    /** The renderers' own models: 1.7 hid modelBipedMain, and from 26.1 the armour and cape models are PlayerModels. */
    private static final Set<HumanoidModel<?>> SKIN_MODELS = Collections.newSetFromMap(new WeakHashMap<>());

    /** The player whose third-person render is being posed; the first-person hand is never posed. */
    private static Player posed;

    public static void setPlayers(List<Integer> ids) {
        PLAYERS.clear();
        PLAYERS.addAll(ids);
    }

    public static void addSkinModel(HumanoidModel<?> model) {
        if (model != null) SKIN_MODELS.add(model);
    }

    static boolean isSkinModel(HumanoidModel<?> model) {
        return SKIN_MODELS.contains(model);
    }

    public static boolean isManly(Player player) {
        return player != null && PLAYERS.contains(player.getId());
    }

    public static void posing(Player player) {
        posed = player;
    }

    /** Whether {@code player}'s model is animated for a third-person render; 1.7 kept the first-person hand. */
    public static boolean takePosed(Player player) {
        boolean thirdPerson = player == posed;
        posed = null;
        return thirdPerson;
    }

    @Override
    protected void setup(Gem.Model<ArmorPose> declaration) {
        WavefrontObj model = WavefrontObj.load(OBJ);
        part(declaration, model, "Head", Pivot.HEAD);
        part(declaration, model, "Body", Pivot.BODY);
        part(declaration, model, "LeftArm", Pivot.LEFT_ARM);
        part(declaration, model, "RightArm", Pivot.RIGHT_ARM);
        part(declaration, model, "LeftLeg", Pivot.LEFT_LEG);
        part(declaration, model, "RightLeg", Pivot.RIGHT_LEG);
    }

    private static void part(Gem.Model<ArmorPose> declaration, WavefrontObj model, String group, Pivot pivot) {
        declaration.part(model.part(group), SKIN)
                .pose(state -> pivot.pose(state.wearer))
                .scale(1F / 16F);
    }
}
