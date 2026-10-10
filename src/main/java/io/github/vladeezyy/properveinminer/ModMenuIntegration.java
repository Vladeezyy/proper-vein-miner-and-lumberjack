package io.github.vladeezyy.properveinminer;

//? if fabric {
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.vladeezyy.properveinminer.client.PvmConfigScreen;

/** The settings button in Mod Menu's mod list. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return PvmConfigScreen::new;
    }
}
//?}
