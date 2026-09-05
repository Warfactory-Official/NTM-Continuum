package com.hbm.blocks.bomb;

public class BlockC4 extends BlockTNTBase {
    public BlockC4(Properties properties) { super(properties); }
    @Override public int kindId() { return 3; }
    @Override public String textureName() { return "c4"; }
    @Override public float blastStrength() { return 15F; }
}
