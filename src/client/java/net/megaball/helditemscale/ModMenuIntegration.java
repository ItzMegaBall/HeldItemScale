package net.megaball.helditemscale;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.megaball.helditemscale.screen.ConfigScreen;

/** Registers the settings screen so it opens from the Mod Menu "Mods" list. */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ConfigScreen::new;
	}
}
