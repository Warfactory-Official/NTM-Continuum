package com.hbm.wiaj.cannery;

/** Tiles that exist only so a neighbouring cable or duct in the jar draws an arm towards them. */
public class Dummies {

    /**
     * 1.7 made this a tile entity implementing both connector interfaces, whose defaults accept every side
     * and every fluid. The jar asks nothing else of it, so here it is only the marker the jar's connection
     * pass looks for.
     */
    public static class JarDummyConnector { }
}
