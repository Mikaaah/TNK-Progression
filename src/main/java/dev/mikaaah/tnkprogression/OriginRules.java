package dev.mikaaah.tnkprogression;

import java.util.List;
import java.util.Optional;

/** Stable background IDs and modest, level-independent origin rules. */
public final class OriginRules {
    public record Origin(String id, String label, String story, String icon, int colour, List<String> perks) {}
    public record Refund(int points, double remainder) {}

    public static final double WORK_SPEED = 0.10;
    public static final double BONUS_DROP_CHANCE = 0.10;
    private static final List<Origin> ALL = List.of(
        new Origin("tnk:miner", "Miner", "Before adventure called, you followed the sound of your pick through the deep. Stone still feels familiar in your hands.", "minecraft:iron_pickaxe", 0xB8B1A6,
            List.of("+10% pickaxe mining speed.", "10% chance for one extra natural ore drop. No Silk Touch or placed ores.")),
        new Origin("tnk:librarian", "Librarian", "You grew up among crowded shelves and well-worn spellbooks. Knowledge taught you how to make every lesson count.", "minecraft:book", 0xB896D6,
            List.of("20% of enchanting XP spent is refunded.", "10% of anvil XP spent is refunded. Required levels and enchantment strength stay unchanged.")),
        new Origin("tnk:forester", "Forester", "The woodland was your first workshop. You learned to read the grain and make a careful cut.", "minecraft:iron_axe", 0x8BAF83,
            List.of("+10% axe chopping speed.", "10% chance for one extra newly harvested natural log. Placed or moved logs do not qualify.")),
        new Origin("tnk:farmer", "Farmer", "Your days once followed the seasons. Patient work in the fields left you with a steady hand and a hardy constitution.", "minecraft:wheat", 0xD4B56C,
            List.of("10% chance for one extra mature crop drop when breaking crops.", "+1 maximum health (half a heart).")),
        new Origin("tnk:soldier", "Soldier", "You once kept watch over another home. The discipline of those long nights still steadies your stance.", "minecraft:iron_sword", 0xC78D92,
            List.of("+1 attack damage.", "+1 armour. Both are fixed bonuses, with no level scaling.")),
        new Origin("tnk:wanderer", "Wanderer", "Roads, ridgelines and unfamiliar horizons were your home. You learned to travel lightly and land on your feet.", "minecraft:leather_boots", 0x9FBBC1,
            List.of("+0.005 movement speed (+5% vanilla base).", "15% less fall damage.")),
        new Origin("tnk:fisher", "Fisher", "You learned patience beside quiet water. A little luck and a well-timed cast have carried you a long way.", "minecraft:fishing_rod", 0x87B8C6,
            List.of("+1 luck.", "10% chance for one extra fish on a successful rod catch. Treasure and junk are never copied.")),
        new Origin("tnk:blacksmith", "Blacksmith", "Heat, hammer and iron shaped your early days. You know the value of a sound repair and a little protection.", "minecraft:anvil", 0xBDA58D,
            List.of("20% of anvil XP spent is refunded. Normal level requirements still apply.", "+1 armour."))
    );

    private OriginRules() {}

    public static List<Origin> all() { return ALL; }

    public static Optional<Origin> find(String id) {
        return ALL.stream().filter(origin -> origin.id().equals(id)).findFirst();
    }

    public static boolean mayChoose(boolean prompted, boolean alreadyChosen, String id) {
        return prompted && !alreadyChosen && find(id).isPresent();
    }

    public static double enchantingRefund(String id) { return "tnk:librarian".equals(id) ? 0.20 : 0; }

    public static double anvilRefund(String id) {
        return switch (id) {
            case "tnk:librarian" -> 0.10;
            case "tnk:blacksmith" -> 0.20;
            default -> 0;
        };
    }

    /** Vanilla XP curve; never trust totalExperience, which is not decremented by every level-cost path. */
    public static long pointsAtLevel(int level) {
        double n = Math.max(0, level);
        double points = n <= 16 ? n * n + 6 * n : n <= 31 ? (5 * n * n - 81 * n + 720) / 2 : (9 * n * n - 325 * n + 4440) / 2;
        return (long) Math.min(Integer.MAX_VALUE, points);
    }

    public static int levelForPoints(long points) {
        long target = Math.clamp(points, 0, Integer.MAX_VALUE);
        int low = 0, high = 21863;
        while (low < high) {
            int middle = (low + high + 1) / 2;
            if (pointsAtLevel(middle) <= target) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    public static Refund refund(long spent, double fraction, double remainder) {
        if (spent <= 0 || !Double.isFinite(fraction) || fraction <= 0 || fraction > 0.25) return new Refund(0, 0);
        double carry = Double.isFinite(remainder) ? Math.clamp(remainder, 0, Math.nextDown(1.0)) : 0;
        double credit = Math.min(Integer.MAX_VALUE, spent) * fraction + carry;
        int points = (int) Math.min(Math.min(spent, Integer.MAX_VALUE), Math.floor(credit + 1e-9));
        return new Refund(points, Math.clamp(credit - points, 0, Math.nextDown(1.0)));
    }
}
