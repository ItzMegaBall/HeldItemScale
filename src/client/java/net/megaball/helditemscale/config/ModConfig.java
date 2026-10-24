package net.megaball.helditemscale.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.megaball.helditemscale.HeldItemScale;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Settings for both hands, stored in {@code <instance>/config/helditemscale.json}.
 *
 * <p>Units: scale is a multiplier (1.0 = vanilla). Position is in Minecraft render-space units
 * (1.0 = one block), applied in the held item's own coordinate space.
 */
public final class ModConfig {
	public static final float SCALE_MIN = 0.1f;
	public static final float SCALE_MAX = 3.0f;
	public static final float POSITION_MIN = -1.0f;
	public static final float POSITION_MAX = 1.0f;

	private static final String FILE_NAME = "helditemscale.json";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String[] AXIS_KEYS = {"X", "Y", "Z"};

	private static ModConfig instance = new ModConfig();

	public final HandSettings mainHand = new HandSettings();
	public final HandSettings offHand = new HandSettings();

	private ModConfig() {
	}

	public static ModConfig get() {
		return instance;
	}

	public void resetAll() {
		mainHand.reset();
		offHand.reset();
	}

	/** Settings for one hand. Axis index: 0 = X, 1 = Y, 2 = Z. Values are always kept inside their valid range. */
	public static final class HandSettings {
		private final float[] scale = {1.0f, 1.0f, 1.0f};
		private final float[] position = {0.0f, 0.0f, 0.0f};

		public float getScale(int axis) {
			return scale[axis];
		}

		public float getPosition(int axis) {
			return position[axis];
		}

		public void setScale(int axis, float value) {
			scale[axis] = sanitize(value, SCALE_MIN, SCALE_MAX, 1.0f);
		}

		public void setPosition(int axis, float value) {
			position[axis] = sanitize(value, POSITION_MIN, POSITION_MAX, 0.0f);
		}

		public void reset() {
			for (int i = 0; i < 3; i++) {
				scale[i] = 1.0f;
				position[i] = 0.0f;
			}
		}

		/** True when this hand renders exactly like vanilla, so the renderer can skip all work. */
		public boolean isDefault() {
			return scale[0] == 1.0f && scale[1] == 1.0f && scale[2] == 1.0f
				&& position[0] == 0.0f && position[1] == 0.0f && position[2] == 0.0f;
		}

		private JsonObject toJson() {
			JsonObject object = new JsonObject();
			for (int i = 0; i < 3; i++) {
				object.addProperty("scale" + AXIS_KEYS[i], scale[i]);
			}
			for (int i = 0; i < 3; i++) {
				object.addProperty("position" + AXIS_KEYS[i], position[i]);
			}
			return object;
		}

		/**
		 * Reads whatever valid numbers exist; missing or invalid fields keep their defaults.
		 *
		 * @return true if every field was present and valid
		 */
		private boolean readFrom(JsonElement element) {
			if (element == null || !element.isJsonObject()) {
				return false;
			}
			JsonObject object = element.getAsJsonObject();
			boolean clean = true;
			for (int i = 0; i < 3; i++) {
				Float s = readNumber(object, "scale" + AXIS_KEYS[i]);
				if (s == null) {
					clean = false;
				} else {
					setScale(i, s);
				}
				Float p = readNumber(object, "position" + AXIS_KEYS[i]);
				if (p == null) {
					clean = false;
				} else {
					setPosition(i, p);
				}
			}
			return clean;
		}

		private static Float readNumber(JsonObject object, String key) {
			JsonElement value = object.get(key);
			if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
				return null;
			}
			double d = value.getAsDouble();
			return Double.isFinite(d) ? (float) d : null;
		}

		private static float sanitize(float value, float min, float max, float fallback) {
			if (Float.isNaN(value) || Float.isInfinite(value)) {
				return fallback;
			}
			float clamped = Math.max(min, Math.min(max, value));
			return Math.round(clamped * 100.0f) / 100.0f;
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	/** Loads the config file (creating it if absent). Never throws; falls back to defaults on any problem. */
	public static void load() {
		Path path = configPath();
		ModConfig loaded = new ModConfig();

		if (!Files.exists(path)) {
			instance = loaded;
			save();
			HeldItemScale.LOGGER.info("Created default config at {}", path);
			return;
		}

		try (Reader reader = Files.newBufferedReader(path)) {
			JsonElement root = JsonParser.parseReader(reader);
			if (!root.isJsonObject()) {
				throw new IllegalStateException("Top level of the config is not a JSON object");
			}
			JsonObject object = root.getAsJsonObject();
			boolean clean = loaded.mainHand.readFrom(object.get("mainHand"));
			clean &= loaded.offHand.readFrom(object.get("offHand"));
			instance = loaded;
			if (!clean) {
				HeldItemScale.LOGGER.warn("Config {} had missing or invalid values; valid values were kept, the rest use defaults. Rewriting it.", path);
				save();
			}
		} catch (IOException e) {
			// Could not even read the file (permissions, disk). Do not touch it; just run with defaults.
			instance = loaded;
			HeldItemScale.LOGGER.error("Could not read config {}; using defaults and leaving the file untouched.", path, e);
		} catch (RuntimeException e) {
			// Malformed JSON: keep a copy so nothing is lost, then start from defaults.
			instance = loaded;
			Path backup = path.resolveSibling(FILE_NAME + ".broken-" + System.currentTimeMillis());
			try {
				Files.move(path, backup, StandardCopyOption.REPLACE_EXISTING);
				HeldItemScale.LOGGER.error("Config {} is corrupted ({}). Saved a copy as {} and created a fresh one.", path, e.getMessage(), backup);
			} catch (IOException moveError) {
				HeldItemScale.LOGGER.error("Config {} is corrupted ({}) and could not be backed up.", path, e.getMessage(), moveError);
			}
			save();
		}
	}

	/** Writes the current settings to disk (via a temp file so a crash cannot leave half a file). */
	public static void save() {
		Path path = configPath();
		Path temp = path.resolveSibling(FILE_NAME + ".tmp");
		JsonObject root = new JsonObject();
		root.add("mainHand", instance.mainHand.toJson());
		root.add("offHand", instance.offHand.toJson());

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(temp)) {
				GSON.toJson(root, writer);
			}
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			HeldItemScale.LOGGER.error("Could not save config to {}", path, e);
		}
	}
}
