package io.github.riveyder.shahed.client;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;

/** Accessible vanilla widgets; opening this menu sends brake input instead of continuing flight. */
public final class FpvSettingsScreen extends Screen {
    private final Screen parent;
    private final FpvSettings settings;

    public FpvSettingsScreen(Screen parent, FpvSettings settings) {
        super(Component.translatable("settings.shahed.title"));
        this.parent = parent;
        this.settings = settings;
    }

    @Override
    protected void init() {
        int x = width / 2 - 120;
        int y = Math.max(36, height / 2 - 86);
        addRenderableWidget(new SettingSlider(x, y, "settings.shahed.sensitivity",
                settings.sensitivity, 0.1, 3.0, value -> settings.sensitivity = value));
        addRenderableWidget(new SettingSlider(x, y + 28, "settings.shahed.smoothing",
                settings.smoothingSeconds, 0.0, 0.5, value -> settings.smoothingSeconds = value));
        addRenderableWidget(Button.builder(invertLabel(), button -> {
            settings.invertPitch = !settings.invertPitch;
            button.setMessage(invertLabel());
            settings.save();
        }).bounds(x, y + 56, 240, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("controls.keybinds"),
                button -> minecraft.gui.setScreen(new KeyBindsScreen(this, minecraft.options)))
                .bounds(x, y + 84, 240, 20).build());
        if (FpvController.isActive()) {
            addRenderableWidget(Button.builder(Component.translatable("key.shahed.exit"), button -> {
                FpvController.exit(true);
                onClose();
            }).bounds(x, y + 112, 117, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                    .bounds(x + 123, y + 112, 117, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                    .bounds(x, y + 112, 240, 20).build());
        }
    }

    private Component invertLabel() {
        return Component.translatable("settings.shahed.invert_pitch",
                Component.translatable(settings.invertPitch ? "options.on" : "options.off"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 18, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        settings.save();
        minecraft.gui.setScreen(parent);
    }

    @Override
    public void removed() {
        settings.save();
    }

    private static final class SettingSlider extends AbstractSliderButton {
        private final String label;
        private final double minimum;
        private final double maximum;
        private final DoubleConsumer setter;

        private SettingSlider(int x, int y, String label, double current, double minimum,
                              double maximum, DoubleConsumer setter) {
            super(x, y, 240, 20, Component.empty(), (current - minimum) / (maximum - minimum));
            this.label = label;
            this.minimum = minimum;
            this.maximum = maximum;
            this.setter = setter;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable(label,
                    String.format(Locale.ROOT, "%.2f", minimum + value * (maximum - minimum))));
        }

        @Override
        protected void applyValue() {
            setter.accept(minimum + value * (maximum - minimum));
        }
    }
}
