package dev.mikaaah.tnkprogression.client;

import dev.mikaaah.tnkprogression.CharacterClientState;
import dev.mikaaah.tnkprogression.OriginRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A two-step, one-time background choice. Closing leaves it unchosen and prompts on the next visit. */
public final class OriginScreen extends Screen {
    private final CharacterScreen character;
    private final Screen returnScreen;
    private String preview = "";
    private int ticks, pending;
    private double scale;
    private int left, top;
    private ParchmentButton confirm;

    public OriginScreen(CharacterScreen character, Screen returnScreen) {
        super(Component.literal("Choose Your Origin"));
        this.character = character;
        this.returnScreen = returnScreen;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(returnScreen); }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}

    @Override protected void init() { layout(); rebuild(); }

    private void layout() {
        scale = Math.min(1.35, Math.min((width - 28) / (double) CharacterScreen.W, (height - 40) / (double) CharacterScreen.H));
        left = (int) ((width - CharacterScreen.W * scale) / 2);
        top = (int) ((height - CharacterScreen.H * scale) / 2);
    }

    private ParchmentButton button(String label, int x, int y, int w, int h, net.minecraft.client.gui.components.Button.OnPress action) {
        return addRenderableWidget(new ParchmentButton(x, y, w, h, Component.literal(label), action));
    }

    private void rebuild() {
        clearWidgets();
        confirm = null;
        button("X", CharacterScreen.W - 26, -7, 21, 18, button -> onClose());
        if (preview.isEmpty()) {
            var origins = OriginRules.all();
            for (int i = 0; i < origins.size(); i++) {
                var origin = origins.get(i);
                button(origin.label(), 150 + i % 2 * 180, 96 + i / 2 * 40, 172, 34, button -> {
                    preview = origin.id();
                    rebuild();
                }).accent(0xFF000000 | origin.colour());
            }
        } else {
            button("Back", 150, 272, 102, 22, button -> { preview = ""; rebuild(); });
            confirm = button("Confirm Origin", 260, 272, 244, 22, button -> {
                pending = 30;
                CharacterClient.action("origin", preview);
                confirm.active = false;
            }).accent(CharacterTheme.PURPLE);
            confirm.active = ticks >= 7 && pending == 0;
        }
    }

    @Override public void tick() {
        ticks++;
        // Separate the open request from the existing Character refresh packet's two-tick limiter.
        if (ticks == 3) CharacterClient.action("origin_open", "");
        if (pending > 0) pending--;
        if (confirm != null) confirm.active = ticks >= 7 && pending == 0;
        var state = CharacterClientState.current;
        if (state != null && !state.origin().isEmpty()) minecraft.setScreen(character);
    }

    public static ItemStack icon(OriginRules.Origin origin) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(origin.icon())));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        layout();
        graphics.fill(0, 0, width, height, 0xA020293F);
        int vx = (int) ((mouseX - left) / scale), vy = (int) ((mouseY - top) / scale);
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0);
        graphics.pose().scale((float) scale, (float) scale, 1);
        CharacterTheme.background(graphics, "character", 0, 0, CharacterScreen.W, CharacterScreen.H);
        StationHeader.draw(graphics, font, "Character", 14, 114);
        CharacterTheme.heading(graphics, font, "Your Story", 20, 30, CharacterTheme.LIGHT);
        graphics.fillGradient(20, 52, 120, 206, 0xFF293349, 0xFF20293F);
        if (minecraft.player != null) {
            graphics.pose().popPose();
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,
                left + (int) (20 * scale), top + (int) (52 * scale), left + (int) (120 * scale), top + (int) (205 * scale),
                (int) (60 * scale), .0625f, mouseX, mouseY, minecraft.player);
            graphics.pose().pushPose();
            graphics.pose().translate(left, top, 0);
            graphics.pose().scale((float) scale, (float) scale, 1);
            CharacterTheme.heading(graphics, font, font.plainSubstrByWidth(minecraft.player.getName().getString(), 94), 20, 218, CharacterTheme.LIGHT);
        }
        graphics.drawWordWrap(font, Component.literal("Every adventurer begins somewhere."), 20, 240, 96, CharacterTheme.LIGHT);
        if (preview.isEmpty()) {
            CharacterTheme.section(graphics, font, "Choose Your Origin", 146, 50, 362, CharacterTheme.PURPLE);
            CharacterTheme.text(graphics, font, "A background for the journey ahead.", 150, 75, CharacterTheme.MUTED);
            super.render(graphics, vx, vy, delta);
            for (int i = 0; i < OriginRules.all().size(); i++) graphics.renderItem(icon(OriginRules.all().get(i)), 158 + i % 2 * 180, 105 + i / 2 * 40);
            graphics.drawWordWrap(font, Component.literal("Select a background to read its story and perks before confirming."), 150, 267, 354, CharacterTheme.MUTED);
        } else {
            var origin = OriginRules.find(preview).orElseThrow();
            CharacterTheme.section(graphics, font, "Your Beginning", 146, 50, 362, CharacterTheme.PURPLE);
            CharacterTheme.panel(graphics, 150, 76, 354, 37, CharacterTheme.CARD);
            graphics.renderItem(icon(origin), 160, 86);
            CharacterTheme.heading(graphics, font, origin.label(), 185, 87, CharacterTheme.INK);
            graphics.drawWordWrap(font, Component.literal(origin.story()), 160, 124, 334, CharacterTheme.INK);
            CharacterTheme.heading(graphics, font, "Origin Perks", 160, 168, CharacterTheme.PURPLE);
            int y = 185;
            for (String perk : origin.perks()) {
                graphics.drawWordWrap(font, Component.literal(perk), 160, y, 334, CharacterTheme.INK);
                y += Math.max(24, font.split(Component.literal(perk), 334).size() * 9 + 6);
            }
            super.render(graphics, vx, vy, delta);
        }
        graphics.pose().popPose();
    }

    @Override public boolean mouseClicked(double x, double y, int button) { return super.mouseClicked((x - left) / scale, (y - top) / scale, button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased((x - left) / scale, (y - top) / scale, button); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (CharacterClient.OPEN.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
