package dev.mikaaah.tnkprogression.client;

import dev.mikaaah.tnkprogression.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Exercises the real first-open picker and server selection packet, only in a disposable test world. */
@EventBusSubscriber(modid = "tnkprogression", value = Dist.CLIENT)
public final class OriginUiSmoke {
    private static int total, ticks, stage;
    private static boolean seeded;

    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tnkprogression.originsSmoke")) return;
        var mc = Minecraft.getInstance();
        if (++total > 3000) { TnkProgression.LOG.error("TNK_ORIGINS_CLIENT_FAILED: timeout"); mc.stop(); return; }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        try {
            mc.getToasts().clear();
            if (!seeded) {
                seeded = true;
                mc.options.guiScale().set(2);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                mc.resizeDisplay();
                var id = mc.player.getUUID();
                mc.getSingleplayerServer().execute(() -> {
                    var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
                    player.setInvulnerable(true);
                    var profile = ProgressionAPI.profile(player);
                    for (String key : java.util.Set.copyOf(profile.getAllKeys())) profile.remove(key);
                    ProgressionAPI.profile(player);
                    profile.putInt("level", 42);
                    profile.putInt("ceiling", 50);
                    profile.putLong("xp", 321);
                    Titles.grant(player, "tnk:beta_tester");
                    Titles.grant(player, "tnk:owner");
                    Titles.select(player, "tnk:owner");
                    player.getInventory().setItem(0, new ItemStack(Items.IRON_PICKAXE));
                    CharacterState.sendFull(player);
                });
                return;
            }
            var state = CharacterClientState.current;
            if (state == null || state.level() != 42) return;
            ticks++;
            if (stage == 0 && ticks > 30) {
                mc.gui.getChat().clearMessages(true);
                check(state.origin().isEmpty(), "login did not select an origin");
                mc.setScreen(new CharacterScreen());
                next(1);
            } else if (stage == 1 && ticks > 25) {
                check(mc.screen instanceof OriginScreen, "first Character visit opens the origin picker");
                check(mc.screen.children().size() == 9, "eight origin choices and close button");
                shot(mc, "TNK-Origins-Choose.png");
                click(mc, 236, 113);
                next(2);
            } else if (stage == 2 && ticks > 15) {
                check(mc.screen instanceof OriginScreen && state.origin().isEmpty(), "preview has not committed a choice");
                shot(mc, "TNK-Origins-Miner-Preview.png");
                mc.screen.onClose();
                check(mc.screen == null, "unconfirmed selection can be closed safely");
                mc.setScreen(new CharacterScreen());
                next(3);
            } else if (stage == 3 && ticks > 25) {
                check(mc.screen instanceof OriginScreen && state.origin().isEmpty(), "unconfirmed player is prompted again");
                click(mc, 236, 113);
                next(4);
            } else if (stage == 4 && ticks > 15) {
                click(mc, 382, 283);
                next(5);
            } else if (stage == 5 && ticks > 25) {
                check(mc.screen instanceof CharacterScreen && state.origin().equals("tnk:miner"), "confirmation persists the chosen Miner origin on the server");
                check(state.xp() == 321 && state.selected().equals("tnk:owner") && state.passive().equals("tnk:beta_tester"), "origin selection preserves progression and both title slots");
                check(Math.abs(mc.player.getAttributeValue(Attributes.MOVEMENT_SPEED) - .103) < 1e-6, "existing Founder movement remains active");
                var speed = new PlayerEvent.BreakSpeed(mc.player, Blocks.STONE.defaultBlockState(), 10, mc.player.blockPosition());
                NeoForge.EVENT_BUS.post(speed);
                check(Math.abs(speed.getNewSpeed() - 11) < 1e-6, "Miner mining speed applies to client prediction");
                CharacterClient.action("origin", "tnk:librarian");
                next(6);
            } else if (stage == 6 && ticks > 20) {
                check(state.origin().equals("tnk:miner"), "server rejects a forged second origin choice");
                mc.gui.getChat().clearMessages(true);
                click(mc, 464, 25);
                next(7);
            } else if (stage == 7 && ticks > 15) {
                check(mc.screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                    .noneMatch(button -> button.getMessage().getString().contains("Confirm")), "origin tab is read-only");
                shot(mc, "TNK-Origins-Miner-Locked-Scale2.png");
                mc.options.guiScale().set(4);
                mc.resizeDisplay();
                next(8);
            } else if (stage == 8 && ticks > 15) {
                check(mc.getWindow().getGuiScale() == 4, "effective GUI scale is four");
                shot(mc, "TNK-Origins-Miner-Locked-Scale4.png");
                mc.screen.onClose();
                mc.setScreen(new CharacterScreen());
                next(9);
            } else if (stage == 9 && ticks > 20) {
                check(mc.screen instanceof CharacterScreen && state.origin().equals("tnk:miner"), "reopening Character does not offer another choice");
                TnkProgression.LOG.info("TNK_ORIGINS_CLIENT_OK: real first-open picker, eight choices, preview/cancel/reopen, permanent confirmation, rejected reselection, independent titles and scales 2/4");
                mc.stop();
            }
        } catch (Throwable failure) {
            TnkProgression.LOG.error("TNK_ORIGINS_CLIENT_FAILED", failure);
            mc.stop();
        }
    }

    private static void next(int value) { stage = value; ticks = 0; }
    private static void shot(Minecraft mc, String name) { Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {}); }
    private static void click(Minecraft mc, int x, int y) {
        var screen = mc.screen;
        double scale = Math.min(1.35, Math.min((screen.width - 28) / (double) CharacterScreen.W, (screen.height - 40) / (double) CharacterScreen.H));
        int left = (int) ((screen.width - CharacterScreen.W * scale) / 2), top = (int) ((screen.height - CharacterScreen.H * scale) / 2);
        screen.mouseClicked(left + x * scale, top + y * scale, 0);
        screen.mouseReleased(left + x * scale, top + y * scale, 0);
    }
}
