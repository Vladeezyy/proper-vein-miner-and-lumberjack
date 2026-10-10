package io.github.vladeezyy.properveinminer;

//? if fabric {
import net.fabricmc.api.ModInitializer;

public class ProperVeinMinerFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ProperVeinMiner.init();
        if (System.getProperty("pvm.selftest") != null) {
            SelfTestHooks.fabric();
        }
    }
}
//?}
