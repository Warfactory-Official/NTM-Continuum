import dev.golem.harness.api.Client;
import dev.golem.harness.api.Expect;
import dev.golem.harness.api.Fixtures;
import dev.golem.harness.api.Scenario;

/**
 * GitHub issue 87: HBM's search bars match through Just Enough Characters when it is installed. Run with JECh in the
 * client's mods folder and {@code lang:zh_cn}: the multi identifier's picker, searched for the pinyin "shui", must list
 * water (水) and its relatives. The screenshots are the check; without JECh the list stays empty.
 */
public final class Gh87PinyinSearch implements Scenario {

    @Override
    public void run(Client c) throws Exception {
        c.waitInWorld(180_000L);
        c.cmd("gamemode creative");
        c.waitTicks(20);
        Expect.that(!c.log(200_000, ".*jecharacters.*").isEmpty(), "Just Enough Characters is loaded");
        Fixtures.giveOnly(c, "hbm:fluid_identifier_multi", 1);
        c.keyDown("key.sneak");
        c.waitTicks(5);
        c.use(2);
        c.waitTicks(5);
        c.keyUp("key.sneak");
        c.waitTicks(10);
        Expect.that(c.screenClass().contains("GUIScreenFluid"), "the picker opened: " + c.screenClass());
        c.type("shui");
        c.waitTicks(10);
        c.screenshot("01-pinyin-shui");
        for (int i = 0; i < 4; i++) c.key("backspace");
        c.type("rong");
        c.waitTicks(10);
        c.screenshot("02-pinyin-rong");
        c.key("escape");
    }
}
