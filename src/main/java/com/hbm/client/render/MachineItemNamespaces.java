package com.hbm.client.render;

import com.hbm.Constants;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * The namespaces whose items reach HBM's item renderer (OBJ {@code hbm:machine_obj} models, the ported 1.7
 * {@code hbm:custom_item} renderers, guns) on the nodes below 26.1.
 *
 * <p>26.1 needs none of this: an item definition of any namespace names {@code hbm:machine_obj} and the special-model
 * system draws it. Below 26.1 each loader wants a per-item "render this yourself" callback (NeoForge 1.21.1's
 * {@code IClientItemExtensions}, Fabric's {@code BuiltinItemRendererRegistry}), and those are bound per item, so the
 * glue binds every item of the namespaces listed here: HBM's own, plus each addon that calls {@link #add} from its
 * client set-up. Binding a whole namespace is free for the same reason it is for HBM: vanilla only consults the
 * callback for items whose baked model parents onto {@code builtin/entity}, which the legacy asset build generates for
 * machine_obj/custom_item definitions only. Other mods' namespaces are never bound: their own builtin items keep their
 * renderers. Forge 1.20.1 needs no list, Golem's {@code CompatItem} hands every Golem-based item the renderer.
 *
 * <p>On NeoForge call {@link #add} from the mod constructor (the extensions event follows every constructor); on Fabric
 * at any point of client initialisation (a namespace added after HBM's glue ran is bound at once).
 */
public final class MachineItemNamespaces {

    private static final Set<String> NAMESPACES = new LinkedHashSet<>(List.of(Constants.MOD_ID));
    private static Consumer<String> binder;

    private MachineItemNamespaces() {}

    /** Lets {@code namespace}'s items use HBM's item renderer on the legacy nodes. */
    public static synchronized void add(String namespace) {
        if (NAMESPACES.add(namespace) && binder != null) binder.accept(namespace);
    }

    /** Whether {@code namespace}'s items are bound. */
    public static synchronized boolean contains(String namespace) {
        return NAMESPACES.contains(namespace);
    }

    /**
     * Loader glue: binds every listed namespace now with {@code binder}, and any namespace added later as it arrives.
     * {@code null} stops binding (a loader whose binding window has closed); a later {@link #add} is then only listed.
     */
    public static synchronized void bind(Consumer<String> binder) {
        MachineItemNamespaces.binder = binder;
        if (binder == null) return;
        for (String namespace : new ArrayList<>(NAMESPACES)) binder.accept(namespace);
    }
}
