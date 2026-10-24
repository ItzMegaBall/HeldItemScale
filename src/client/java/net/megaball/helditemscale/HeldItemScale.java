package net.megaball.helditemscale;

import net.fabricmc.api.ClientModInitializer;
import net.megaball.helditemscale.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint. Only loads the configuration; all rendering work happens in the mixin. */
public final class HeldItemScale implements ClientModInitializer {
	public static final String MOD_ID = "helditemscale";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		ModConfig.load();
	}
}
