package dev.mikaaah.tnkprogression;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Descriptions are built from the server's live rules, including its recipe allowlists. */
public final class ActivityHelp {
    private ActivityHelp() {}

    public static List<String> describe(ServerPlayer player, String activity) {
        var c = ProgressionConfig.current;
        var lines = new ArrayList<String>();
        switch (activity) {
            case "combat" -> {
                lines.add("Deal damage to hostile enemies, then survive nearby until they die. Melee, projectiles and supported player-owned summons count.");
                lines.add("Work depends on enemy strength and your share of its damage. A kill is not always 1 Work; small contributions accumulate.");
                lines.add("Your last hit must be within " + seconds(c.contributionTicks) + " seconds, and you must be alive, in the same dimension and within " + number(c.contributionRange) + " blocks when it dies.");
                lines.add("Repeated kills of the same enemy type in one 64-block area give less Work during a " + seconds(c.combatWindowTicks) + "-second window.");
                lines.add("Owned pets and summoned enemies give no Work. Excluded spawn sources: " + c.excludedSpawns.stream().sorted().map(ActivityHelp::spawnName).collect(java.util.stream.Collectors.joining(", ")) + ".");
            }
            case "mining" -> {
                lines.add("Personally mine natural base stone or ore and produce a block drop: 1 Work per eligible block.");
                lines.add("Examples: stone, deepslate, netherrack, iron ore and diamond ore. Compatible modded base stones and ores also count.");
                lines.add("Placed, fluid-generated, piston-moved and previously mined positions do not count. Machine mining and blocks without drops do not count.");
            }
            case "fishing" -> {
                lines.add("Successfully reel in a catch with loot: 1 Work per catch, regardless of stack size or fishing luck.");
                lines.add("The fishing hook must have existed for at least 4 seconds. Catches can count at most once per second.");
                lines.add(c.requireOpenWater ? "Open-water fishing is required. Enclosed fishing setups that fail Minecraft's open-water check do not count." : "This server allows catches without Minecraft's open-water requirement.");
                lines.add("Empty reels, collecting dropped fish and automated fishing do not count.");
            }
            case "exploration" -> {
                lines.add("Enter a chunk your character has not visited before: 1 Work, at most once every 5 seconds. Each dimension is tracked separately.");
                lines.add("Your location is checked once per second. Chunks first seen during the cooldown are recorded as visited but give no Work later.");
                lines.add("Returning to known chunks does not count. This activity tracks chunks, rather than distance walked, structures or newly discovered biome types.");
            }
            case "farming" -> {
                lines.add("Personally harvest a fully grown crop: 1 Work. Breaking mature crops, including nether wart and cocoa, qualifies.");
                lines.add("Right-click harvesting also counts when a ripe crop becomes unripe, including ripe sweet berries and glow berries.");
                lines.add("Each crop position has a shared " + seconds(c.harvestCooldownTicks) + "-second cooldown. Immature crops, placing crops and automated harvests do not count.");
                lines.add("Compatible modded crops also qualify; decorative plants and unsupported custom harvest systems do not.");
            }
            case "crafting", "engineering" -> {
                boolean engineering = activity.equals("engineering");
                lines.add("Complete a valid crafting-grid recipe listed below. Only the recipes listed here award Work; merely obtaining an item does not count.");
                lines.add("Work is awarded per validated crafting action, not per item in the output stack. Shift-clicking does not multiply Work by the output count.");
                lines.add(engineering ? "Machine processing does not automatically award Engineering Work. Configured quest commissions count only when the server explicitly awards them, once per character." : "Engineering recipes belong to Engineering instead. Unlisted recipes, machine outputs and reversible packing/unpacking loops do not count.");
                lines.add("Eligible crafting recipes on this server:");
                int active = 0;
                var rules = engineering ? c.engineeringRecipes : c.craftingRecipes;
                for (var entry : rules.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
                    if (active >= 128) { lines.add("Additional recipes are configured by this server."); break; }
                    if (!engineering && c.engineeringRecipes.containsKey(entry.getKey())) continue;
                    var recipe = player.serverLevel().getRecipeManager().byKey(ResourceLocation.parse(entry.getKey()));
                    if (recipe.isEmpty() || recipe.get().value().getType() != net.minecraft.world.item.crafting.RecipeType.CRAFTING) continue;
                    var output = recipe.get().value().getResultItem(player.registryAccess());
                    String label = output.isEmpty() ? name(entry.getKey()) : output.getHoverName().getString();
                    lines.add(label + " — " + number(entry.getValue()) + " Work per craft");
                    active++;
                }
                if (active == 0) lines.add("No eligible crafting recipes are currently loaded.");
                if (engineering) {
                    lines.add("Server-awarded commissions (once each):");
                    c.engineeringObjectives.entrySet().stream().sorted(java.util.Comparator.comparingInt((Map.Entry<String,Double> entry) -> commissionOrder(entry.getKey())).thenComparing(Map.Entry::getKey)).limit(128).forEach(entry ->
                        lines.add(name(entry.getKey()) + " — " + number(entry.getValue()) + " Work"));
                }
            }
            default -> lines.add("No automatic activity rules are configured.");
        }
        lines.add("Work fills this activity's milestone bar. Completing a milestone awards the shown character XP; later milestones need more Work and give more XP.");
        lines.add("Activities require Survival or Adventure mode. Creative, Spectator and fake players do not count. Work and XP pause at your current campaign ceiling.");
        return List.copyOf(lines);
    }

    private static int commissionOrder(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        if (path.equals("first_workshop")) return 0;
        if (path.equals("first_processing_line")) return 1;
        if (path.startsWith("chapter_") && path.endsWith("_complete")) {
            try { return Math.addExact(2, Integer.parseInt(path.substring(8, path.length() - 9))); }
            catch (NumberFormatException | ArithmeticException ignored) {}
        }
        return Integer.MAX_VALUE;
    }

    private static String spawnName(String source) {
        return switch (source) {
            case "SPAWNER" -> "mob spawners";
            case "TRIAL_SPAWNER" -> "trial spawners";
            case "SPAWN_EGG" -> "spawn eggs";
            case "DISPENSER" -> "dispensers";
            case "BREEDING" -> "breeding";
            case "COMMAND" -> "commands";
            case "MOB_SUMMONED" -> "summoned mobs";
            case "TRIGGERED" -> "triggered summons";
            case "SPLIT" -> "split mobs";
            default -> name(source);
        };
    }

    private static String seconds(long ticks) { return number(ticks / 20.0); }
    private static String number(double value) { return value == (long)value ? Long.toString((long)value) : String.format(Locale.ROOT, "%.2f", value); }
    private static String name(String id) {
        String value = id.substring(id.indexOf(':') + 1).replace('_', ' ').replace('/', ' ').toLowerCase(Locale.ROOT);
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
