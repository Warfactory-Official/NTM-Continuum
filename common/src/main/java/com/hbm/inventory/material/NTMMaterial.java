package com.hbm.inventory.material;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.MaterialStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public final class NTMMaterial {

    public final String name;

    public final int id;

    public int solidColorLight = 0xFFFFFF;
    public int solidColorDark = 0x505050;
    public int moltenColor = 0xFFFFFF;

    public final Set<MaterialShapes> autogen = EnumSet.noneOf(MaterialShapes.class);

    public NTMMaterial(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public NTMMaterial colors(int solidLight, int solidDark, int molten) {
        this.solidColorLight = solidLight;
        this.solidColorDark = solidDark;
        this.moltenColor = molten;
        return this;
    }

    public NTMMaterial setAutogen(MaterialShapes... shapes) {
        for (MaterialShapes s : shapes) if (s != null) autogen.add(s);
        return this;
    }

    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();
        for (String part : name.toLowerCase(Locale.US).split("_")) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }

    public TagKey<Item> tag(MaterialShapes shape) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", shape.tagPlural + "/" + name));
    }

    public AStack nugget()  { return new TagStack(tag(MaterialShapes.NUGGET)); }
    public AStack billet()  { return new TagStack(tag(MaterialShapes.BILLET)); }
    public AStack ingot()   { return new TagStack(tag(MaterialShapes.INGOT)); }
    public AStack gem()     { return new TagStack(tag(MaterialShapes.GEM)); }
    public AStack crystal() { return new TagStack(tag(MaterialShapes.CRYSTAL)); }
    public AStack dust()    { return new TagStack(tag(MaterialShapes.DUST)); }
    public AStack plate()   { return new TagStack(tag(MaterialShapes.PLATE)); }
    public AStack block()   { return new TagStack(tag(MaterialShapes.BLOCK)); }
    public AStack ore()     { return new TagStack(tag(MaterialShapes.ORE)); }

    public AStack plateCast()   { return new MaterialStack(this, MaterialShapes.CASTPLATE); }
    public AStack plateWelded() { return new MaterialStack(this, MaterialShapes.WELDEDPLATE); }
    public AStack shell()       { return new MaterialStack(this, MaterialShapes.SHELL); }
    public AStack pipe()        { return new MaterialStack(this, MaterialShapes.PIPE); }
    public AStack wire()        { return new MaterialStack(this, MaterialShapes.WIRE); }
    public AStack wireDense()   { return new MaterialStack(this, MaterialShapes.DENSEWIRE); }
    public AStack bolt()        { return new MaterialStack(this, MaterialShapes.BOLT); }
    public AStack heavyComp()   { return new MaterialStack(this, MaterialShapes.HEAVY_COMPONENT); }

    @Override
    public String toString() {
        return "NTMMaterial[" + name + "]";
    }
}
