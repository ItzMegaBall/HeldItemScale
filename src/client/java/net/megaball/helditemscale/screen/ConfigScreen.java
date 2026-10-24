package net.megaball.helditemscale.screen;

import net.megaball.helditemscale.config.ModConfig;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/**
 * Settings screen: two columns (Main Hand / Off-Hand), six sliders each, plus reset and Done buttons.
 * Changes apply live; they are written to disk when the screen is closed (not on every slider tick).
 * If the window is too small, the slider area scrolls with the mouse wheel.
 */
public final class ConfigScreen extends Screen {
	private static final String[] AXES = {"X", "Y", "Z"};
	private static final int ROW_PITCH = 24;
	private static final int WIDGET_HEIGHT = 20;
	private static final int COLUMN_WIDTH = 150;
	private static final int COLUMN_GAP = 10;
	private static final int TOP = 36;
	private static final int ROW_COUNT = 7; // 1 header row + 3 scale rows + 3 position rows
	private static final int FOOTER_HEIGHT = 58;

	private final Screen parent;
	private final List<ScrollEntry> scrollEntries = new ArrayList<>();
	private final List<ValueSlider> sliders = new ArrayList<>();
	private int scroll;
	private int contentBottom;
	private int maxScroll;

	public ConfigScreen(Screen parent) {
		super(Component.literal("Held Item Scale"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		scrollEntries.clear();
		sliders.clear();

		addRenderableWidget(new StringWidget(0, 14, this.width, 12, this.title, this.font).alignCenter());

		int leftX = this.width / 2 - COLUMN_WIDTH - COLUMN_GAP / 2;
		int rightX = this.width / 2 + COLUMN_GAP / 2;
		addColumn(leftX, "Main Hand", ModConfig.get().mainHand);
		addColumn(rightX, "Off-Hand", ModConfig.get().offHand);

		int row1 = this.height - 52;
		int row2 = this.height - 28;
		addRenderableWidget(Button.builder(Component.literal("Reset Main Hand"), button -> {
			ModConfig.get().mainHand.reset();
			syncSliders();
		}).bounds(leftX, row1, COLUMN_WIDTH, WIDGET_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("Reset Off-Hand"), button -> {
			ModConfig.get().offHand.reset();
			syncSliders();
		}).bounds(rightX, row1, COLUMN_WIDTH, WIDGET_HEIGHT).build());
		addRenderableWidget(Button.builder(Component.literal("Reset All"), button -> {
			ModConfig.get().resetAll();
			syncSliders();
		}).bounds(leftX, row2, COLUMN_WIDTH, WIDGET_HEIGHT).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
			.bounds(rightX, row2, COLUMN_WIDTH, WIDGET_HEIGHT).build());

		contentBottom = this.height - FOOTER_HEIGHT;
		int contentHeight = (ROW_COUNT - 1) * ROW_PITCH + WIDGET_HEIGHT;
		maxScroll = Math.max(0, contentHeight - (contentBottom - TOP));
		scroll = Math.max(0, Math.min(maxScroll, scroll));
		applyScroll();
	}

	private void addColumn(int x, String heading, ModConfig.HandSettings hand) {
		addScrollable(new StringWidget(x, 0, COLUMN_WIDTH, WIDGET_HEIGHT, Component.literal(heading), this.font).alignCenter(), 0);
		for (int axis = 0; axis < 3; axis++) {
			final int a = axis;
			addScrollable(new ValueSlider(x, COLUMN_WIDTH, AXES[axis] + " Scale",
				ModConfig.SCALE_MIN, ModConfig.SCALE_MAX,
				() -> hand.getScale(a), value -> hand.setScale(a, (float) value)), 1 + axis);
		}
		for (int axis = 0; axis < 3; axis++) {
			final int a = axis;
			addScrollable(new ValueSlider(x, COLUMN_WIDTH, AXES[axis] + " Position",
				ModConfig.POSITION_MIN, ModConfig.POSITION_MAX,
				() -> hand.getPosition(a), value -> hand.setPosition(a, (float) value)), 4 + axis);
		}
	}

	private void addScrollable(AbstractWidget widget, int row) {
		addRenderableWidget(widget);
		scrollEntries.add(new ScrollEntry(widget, row));
		if (widget instanceof ValueSlider slider) {
			sliders.add(slider);
		}
	}

	private void syncSliders() {
		for (ValueSlider slider : sliders) {
			slider.syncFromConfig();
		}
	}

	private void applyScroll() {
		for (ScrollEntry entry : scrollEntries) {
			int y = TOP + entry.row() * ROW_PITCH - scroll;
			entry.widget().setY(y);
			entry.widget().visible = y >= TOP - 2 && y + entry.widget().getHeight() <= contentBottom + 2;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (maxScroll > 0 && mouseY >= TOP && mouseY <= contentBottom) {
			scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.round(scrollY * 12)));
			applyScroll();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}

	/** Called whenever this screen is replaced or closed (Done, Escape, or opening another screen). */
	@Override
	public void removed() {
		ModConfig.save();
	}

	private record ScrollEntry(AbstractWidget widget, int row) {
	}

	/** Slider that shows "Label: 1.25", writes live into the config, and snaps to 0.01 steps. */
	private static final class ValueSlider extends AbstractSliderButton {
		private final String label;
		private final double min;
		private final double max;
		private final DoubleSupplier getter;
		private final DoubleConsumer setter;

		ValueSlider(int x, int width, String label, double min, double max, DoubleSupplier getter, DoubleConsumer setter) {
			super(x, 0, width, WIDGET_HEIGHT, Component.empty(), (getter.getAsDouble() - min) / (max - min));
			this.label = label;
			this.min = min;
			this.max = max;
			this.getter = getter;
			this.setter = setter;
			updateMessage();
		}

		void syncFromConfig() {
			this.value = normalize(getter.getAsDouble());
			updateMessage();
		}

		private double normalize(double actual) {
			return Math.max(0.0, Math.min(1.0, (actual - min) / (max - min)));
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(label + ": " + String.format(Locale.ROOT, "%.2f", getter.getAsDouble())));
		}

		@Override
		protected void applyValue() {
			setter.accept(min + this.value * (max - min));
			// The config rounds to 0.01 and clamps; move the handle to the stored value.
			this.value = normalize(getter.getAsDouble());
		}
	}
}
