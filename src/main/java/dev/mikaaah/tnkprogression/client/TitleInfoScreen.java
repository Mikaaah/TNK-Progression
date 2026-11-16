package dev.mikaaah.tnkprogression.client;

import dev.mikaaah.tnkprogression.CharacterClientState;
import dev.mikaaah.tnkprogression.CharacterState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.ArrayList;
import java.util.List;

/** Full server-owned acquisition requirements without clipping the title preview. */
public final class TitleInfoScreen extends Screen {
    private static final int LINES_PER_PAGE = 18;
    private final CharacterScreen parent;
    private final String titleId;
    private int page, left, top;
    private double scale;
    private ParchmentButton previous, next;

    public TitleInfoScreen(CharacterScreen parent, String titleId) {
        super(Component.literal("Title Information"));
        this.parent = parent;
        this.titleId = titleId;
        CharacterClient.action("refresh", "");
    }

    private CharacterState.Title state() {
        var snapshot = CharacterClientState.current;
        return snapshot == null ? null : snapshot.titles().stream().filter(value -> value.id().equals(titleId)).findFirst().orElse(null);
    }

    private void layout() {
        scale = Math.min(1.35, Math.min((width - 28) / (double) CharacterScreen.W, (height - 40) / (double) CharacterScreen.H));
        left = (int)((width - CharacterScreen.W * scale) / 2);
        top = (int)((height - CharacterScreen.H * scale) / 2);
    }

    @Override protected void init() {
        layout();
        addRenderableWidget(new ParchmentButton(20, 274, 104, 20, Component.literal("Back"), button -> onClose()));
        previous = addRenderableWidget(new ParchmentButton(350, 274, 32, 20, Component.literal("<"), button -> page--));
        next = addRenderableWidget(new ParchmentButton(472, 274, 32, 20, Component.literal(">"), button -> page++));
        addRenderableWidget(new ParchmentButton(CharacterScreen.W - 26, -7, 21, 18, Component.literal("X"), button -> onClose()));
    }

    private List<FormattedCharSequence> lines(CharacterState.Title value) {
        var result = new ArrayList<FormattedCharSequence>();
        if (value != null) for (String paragraph : value.info() == null ? List.of(value.description()) : value.info()) {
            result.addAll(font.split(Component.literal(paragraph), 338));
            result.add(FormattedCharSequence.EMPTY);
        }
        return result;
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        layout();
        graphics.fill(0, 0, width, height, 0xA020293F);
        var value = state();
        var lines = lines(value);
        int pages = Math.max(1, (lines.size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        page = Math.clamp(page, 0, pages - 1);
        previous.active = page > 0;
        next.active = page < pages - 1;
        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0);
        graphics.pose().scale((float)scale, (float)scale, 1);
        CharacterTheme.background(graphics, "character", 0, 0, CharacterScreen.W, CharacterScreen.H);
        StationHeader.draw(graphics, font, "Titles", 14, 114);
        graphics.drawWordWrap(font, Component.literal(value == null ? "Title" : value.label()), 24, 105, 96, CharacterTheme.LIGHT);
        graphics.drawWordWrap(font, Component.literal("How to unlock and use this title"), 24, 139, 96, CharacterTheme.LIGHT);
        CharacterTheme.heading(graphics, font, "How to unlock", 150, 20, CharacterTheme.INK);
        if (value != null) {
            CharacterTheme.text(graphics, font, value.legacy() ? "Legacy title" : value.earned() ? "Unlocked" : "Locked", 150, 44, CharacterTheme.MUTED);
            graphics.fill(150,56,504,58,CharacterTheme.PURPLE);
        }
        CharacterTheme.panel(graphics, 146, 64, 362, 199, CharacterTheme.CARD);
        int start = page * LINES_PER_PAGE;
        for (int i = start; i < Math.min(start + LINES_PER_PAGE, lines.size()); i++)
            graphics.drawString(font, lines.get(i), 156, 73 + (i - start) * 10, CharacterTheme.INK, false);
        CharacterTheme.text(graphics, font, (page + 1) + " / " + pages, 409, 280, CharacterTheme.INK);
        super.render(graphics, (int)((mouseX-left)/scale), (int)((mouseY-top)/scale), delta);
        graphics.pose().popPose();
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {}
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean mouseClicked(double x, double y, int button) { return super.mouseClicked((x-left)/scale, (y-top)/scale, button); }
    @Override public boolean mouseReleased(double x, double y, int button) { return super.mouseReleased((x-left)/scale, (y-top)/scale, button); }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int pages = Math.max(1, (lines(state()).size() + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
        page = Math.clamp(page + (dy < 0 ? 1 : -1), 0, pages - 1);
        return true;
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (CharacterClient.OPEN.matches(key, scan)) { onClose(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
}
