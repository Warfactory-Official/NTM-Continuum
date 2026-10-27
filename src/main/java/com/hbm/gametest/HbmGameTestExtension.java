package com.hbm.gametest;

import dev.golem.core.gametest.GameTests;
import java.util.List;

/** Additional development suites, listed after the built-in ones. */
public interface HbmGameTestExtension {
    List<GameTests.Suite> suites();
}
