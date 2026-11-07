package dev.mikaaah.tnkprogression;

import java.util.List;
import java.util.Optional;

/** Stable background IDs and modest, level-independent origin rules. */
public final class OriginRules {
    public enum Perk { ORE_SPEED, RAW_ORE_CHANCE, LOG_SPEED, LOG_CHANCE, CROP_YIELD, EXHAUSTION_REDUCTION, BREEDING_REDUCTION, MOUNT_SPEED, FISH_WAIT_REDUCTION, FISH_XP, RANGED_DAMAGE, HUNT_YIELD, ANVIL_REPAIR_REFUND, WEAPON_DURABILITY_REDUCTION, STONE_SPEED, PLACEMENT_REACH, ENGINEERING_XP, WRENCH_REACH, ENCHANT_REFUND, GRINDSTONE_XP, POTION_DURATION, POTION_HEAL, NATURAL_HEAL, HERB_CHANCE, TRADE_DISCOUNT, TRADE_XP, MOVE_SPEED, FALL_REDUCTION, SWIM_SPEED, AIR_TICKS, ARMOUR, KNOCKBACK_RESISTANCE, SNEAK_SPEED, SNEAK_VISIBILITY_REDUCTION, UNDEAD_REDUCTION, UNDEAD_XP, HEALTH, HARMFUL_DURATION_REDUCTION, MANA, MANA_REGEN, PICKAXE_SPEED, AXE_SPEED, ORE_CHANCE, FISH_CHANCE, ANVIL_REFUND, ATTACK, FLAT_MOVE, LUCK }
    public record Origin(String id, String label, String story, String icon, int colour, List<String> perks, java.util.Map<Perk,Double> bonuses) {
        public Origin(String id, String label, String story, String icon, int colour, List<String> perks) {
            this(id, label, story, icon, colour, List.copyOf(perks), java.util.Map.of());
        }
    }
    public record Refund(int points, double remainder) {}

    public static final double WORK_SPEED = 0.10;
    public static final double BONUS_DROP_CHANCE = 0.10;
    private static final List<Origin> LEGACY = List.of(
        new Origin("tnk:miner", "Miner", "Before adventure called, you followed the sound of your pick through the deep. Stone still feels familiar in your hands.", "tnkprogression:textures/gui/origins/ancient_pickaxe.png", 0xB8B1A6,
            List.of("+10% pickaxe mining speed.", "10% chance for one extra natural ore drop. No Silk Touch or placed ores.")),
        new Origin("tnk:librarian", "Librarian", "You grew up among crowded shelves and well-worn spellbooks. Knowledge taught you how to make every lesson count.", "tnkprogression:textures/gui/origins/golden_book.png", 0xB896D6,
            List.of("20% of enchanting XP spent is refunded.", "10% of anvil XP spent is refunded. Required levels and enchantment strength stay unchanged.")),
        new Origin("tnk:forester", "Forester", "The woodland was your first workshop. You learned to read the grain and make a careful cut.", "tnkprogression:textures/gui/origins/mossy_axe.png", 0x8BAF83,
            List.of("+10% axe chopping speed.", "10% chance for one extra newly harvested natural log. Placed or moved logs do not qualify.")),
        new Origin("tnk:farmer", "Farmer", "Your days once followed the seasons. Patient work in the fields left you with a steady hand and a hardy constitution.", "tnkprogression:textures/gui/origins/sun_carrot.png", 0xD4B56C,
            List.of("10% chance for one extra mature crop drop when breaking crops.", "+1 maximum health (half a heart).")),
        new Origin("tnk:soldier", "Soldier", "You once kept watch over another home. The discipline of those long nights still steadies your stance.", "tnkprogression:textures/gui/origins/spear.png", 0xC78D92,
            List.of("+1 attack damage.", "+1 armour. Both are fixed bonuses, with no level scaling.")),
        new Origin("tnk:wanderer", "Wanderer", "Roads, ridgelines and unfamiliar horizons were your home. You learned to travel lightly and land on your feet.", "tnkprogression:textures/gui/origins/portal_scroll.png", 0x9FBBC1,
            List.of("+0.005 movement speed (+5% vanilla base).", "15% less fall damage.")),
        new Origin("tnk:fisher", "Fisher", "You learned patience beside quiet water. A little luck and a well-timed cast have carried you a long way.", "tnkprogression:textures/gui/origins/golden_hook.png", 0x87B8C6,
            List.of("+1 luck.", "10% chance for one extra fish on a successful rod catch. Treasure and junk are never copied.")),
        new Origin("tnk:blacksmith", "Blacksmith", "Heat, hammer and iron shaped your early days. You know the value of a sound repair and a little protection.", "tnkprogression:textures/gui/origins/hammer.png", 0xBDA58D,
            List.of("20% of anvil XP spent is refunded. Normal level requirements still apply.", "+1 armour."))
    );

    private static final List<Origin> ALL = List.of(
        new Origin("tnk:deepdelver", "Deepdelver", "You spent your early years working beneath the mountains.", "tnkprogression:textures/gui/origins/ancient_pickaxe.png", 0xa9b4cc, List.of("+10% ore mining speed.", "5% chance for an extra raw ore from natural deposits."), java.util.Map.of(Perk.ORE_SPEED, 0.1, Perk.RAW_ORE_CHANCE, 0.05)),
        new Origin("tnk:woodcutter", "Woodcutter", "Forest camps and falling timber were your everyday life.", "tnkprogression:textures/gui/origins/mossy_axe.png", 0x8baf83, List.of("+15% log chopping speed.", "5% chance for an extra natural log."), java.util.Map.of(Perk.LOG_SPEED, 0.15, Perk.LOG_CHANCE, 0.05)),
        new Origin("tnk:homesteader", "Homesteader", "You inherited a small farm and learned to make every harvest count.", "tnkprogression:textures/gui/origins/sun_carrot.png", 0xd4b56c, List.of("+10% mature crop yield when harvested by hand.", "10% less hunger from exhaustion."), java.util.Map.of(Perk.CROP_YIELD, 0.1, Perk.EXHAUSTION_REDUCTION, 0.1)),
        new Origin("tnk:stablehand", "Stablehand", "You raised livestock and cared for travelling merchants' horses.", "tnkprogression:textures/gui/origins/horseshoe.png", 0xbda58d, List.of("10% shorter animal breeding cooldowns.", "+15% movement speed for living mounts you control."), java.util.Map.of(Perk.BREEDING_REDUCTION, 0.1, Perk.MOUNT_SPEED, 0.15)),
        new Origin("tnk:riverfolk", "Riverfolk", "You grew up along the river, living from whatever your line caught.", "tnkprogression:textures/gui/origins/golden_hook.png", 0x87b8c6, List.of("10% shorter fishing wait.", "+15% vanilla fishing XP."), java.util.Map.of(Perk.FISH_WAIT_REDUCTION, 0.1, Perk.FISH_XP, 0.15)),
        new Origin("tnk:huntsman", "Huntsman", "You supplied a remote settlement with meat and hides.", "tnkprogression:textures/gui/origins/bone_bow.png", 0xacb680, List.of("+5% bow and crossbow damage.", "+10% meat and leather drops from your kills."), java.util.Map.of(Perk.RANGED_DAMAGE, 0.05, Perk.HUNT_YIELD, 0.1)),
        new Origin("tnk:forgehand", "Forgehand", "Years at the anvil taught you how to keep equipment working.", "tnkprogression:textures/gui/origins/hammer.png", 0xc4997c, List.of("10% of anvil repair XP refunded; normal requirements apply.", "10% less weapon durability loss."), java.util.Map.of(Perk.ANVIL_REPAIR_REFUND, 0.1, Perk.WEAPON_DURABILITY_REDUCTION, 0.1)),
        new Origin("tnk:stonemason", "Stonemason", "You helped build walls, bridges and castles that would outlive you.", "tnkprogression:textures/gui/origins/bricks.png", 0xb8b1a6, List.of("+15% stone breaking speed.", "+0.5 block reach while holding a building block."), java.util.Map.of(Perk.STONE_SPEED, 0.15, Perk.PLACEMENT_REACH, 0.5)),
        new Origin("tnk:millwright", "Millwright", "You maintained waterwheels, mills and machinery for your village.", "tnkprogression:textures/gui/origins/wrench.png", 0xb8a58b, List.of("+10% engineering activity XP.", "+1 block interaction reach while holding a wrench."), java.util.Map.of(Perk.ENGINEERING_XP, 0.1, Perk.WRENCH_REACH, 1.0)),
        new Origin("tnk:librarian", "Librarian", "You cared for dusty shelves and books containing forgotten knowledge.", "tnkprogression:textures/gui/origins/golden_book.png", 0xb896d6, List.of("10% of enchanting XP refunded; normal requirements apply.", "+15% XP recovered through grindstone disenchanting."), java.util.Map.of(Perk.ENCHANT_REFUND, 0.1, Perk.GRINDSTONE_XP, 0.15)),
        new Origin("tnk:apothecary", "Apothecary", "Your little shop supplied remedies, tonics and questionable mixtures.", "tnkprogression:textures/gui/origins/aquatic_elixir.png", 0x81bfb3, List.of("+15% duration for beneficial potions you drink.", "+10% healing from instant health potions you drink."), java.util.Map.of(Perk.POTION_DURATION, 0.15, Perk.POTION_HEAL, 0.1)),
        new Origin("tnk:hedge_herbalist", "Hedge Herbalist", "You learned which plants could heal, feed or poison a traveller.", "tnkprogression:textures/gui/origins/ghostflower.png", 0xa1bd80, List.of("+10% natural health regeneration.", "10% chance for an extra gathered flower or mushroom."), java.util.Map.of(Perk.NATURAL_HEAL, 0.1, Perk.HERB_CHANCE, 0.1)),
        new Origin("tnk:caravan_trader", "Caravan Trader", "You travelled between settlements, bargaining for your next meal.", "tnkprogression:textures/gui/origins/golden_ring.png", 0xd1b173, List.of("10% lower villager buying prices, rounded down.", "+15% vanilla trading XP."), java.util.Map.of(Perk.TRADE_DISCOUNT, 0.1, Perk.TRADE_XP, 0.15)),
        new Origin("tnk:wayfarer", "Wayfarer", "You rarely stayed anywhere long enough to call it home.", "tnkprogression:textures/gui/origins/portal_scroll.png", 0x9fbbc1, List.of("+5% movement speed.", "20% less fall damage."), java.util.Map.of(Perk.MOVE_SPEED, 0.05, Perk.FALL_REDUCTION, 0.2)),
        new Origin("tnk:deckhand", "Deckhand", "You worked aboard merchant ships and survived more than one wreck.", "tnkprogression:textures/gui/origins/blue_coral.png", 0x83b8ce, List.of("+15% swimming speed.", "+5 seconds of underwater breathing time."), java.util.Map.of(Perk.SWIM_SPEED, 0.15, Perk.AIR_TICKS, 100.0)),
        new Origin("tnk:town_watch", "Town Watch", "You guarded the gates and broke up trouble at the local tavern.", "tnkprogression:textures/gui/origins/spear.png", 0xc78d92, List.of("+1 armour point.", "+10% knockback resistance."), java.util.Map.of(Perk.ARMOUR, 1.0, Perk.KNOCKBACK_RESISTANCE, 0.1)),
        new Origin("tnk:street_urchin", "Street Urchin", "You survived by staying unnoticed and finding a way through tight places.", "tnkprogression:textures/gui/origins/bone_knife.png", 0xb4a2bf, List.of("+15% sneaking speed.", "10% shorter hostile detection range while sneaking."), java.util.Map.of(Perk.SNEAK_SPEED, 0.15, Perk.SNEAK_VISIBILITY_REDUCTION, 0.1)),
        new Origin("tnk:gravekeeper", "Gravekeeper", "You tended the cemetery and occasionally dealt with its restless residents.", "tnkprogression:textures/gui/origins/simple_skull.png", 0xb6b6aa, List.of("10% less damage from undead attackers.", "+15% vanilla XP from undead kills."), java.util.Map.of(Perk.UNDEAD_REDUCTION, 0.1, Perk.UNDEAD_XP, 0.15)),
        new Origin("tnk:temple_acolyte", "Temple Acolyte", "You served a small sanctuary, tending its visitors and protecting its grounds.", "tnkprogression:textures/gui/origins/book_of_star.png", 0xbba3d1, List.of("+2 maximum health (one heart).", "10% shorter harmful potion effects."), java.util.Map.of(Perk.HEALTH, 2.0, Perk.HARMFUL_DURATION_REDUCTION, 0.1)),
        new Origin("tnk:hedge_mage", "Hedge Mage", "Your magical education came from borrowed books and dangerous experiments.", "tnkprogression:textures/gui/origins/mythic_book.png", 0xaf89cb, List.of("+5% maximum mana.", "+5% mana regeneration. Requires Iron's Spells mana attributes."), java.util.Map.of(Perk.MANA, 0.05, Perk.MANA_REGEN, 0.05))
    );

    private static final java.util.Map<String,Origin> CURRENT_BY_ID = index(ALL);
    private static final java.util.Map<String,Origin> LEGACY_BY_ID = index(LEGACY);
    private static final List<Origin> KNOWN = java.util.stream.Stream.concat(ALL.stream(), LEGACY.stream())
        .collect(java.util.stream.Collectors.toMap(Origin::id, x -> x, (current, old) -> current, java.util.LinkedHashMap::new))
        .values().stream().toList();

    private static java.util.Map<String,Origin> index(List<Origin> origins) {
        return origins.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Origin::id, x -> x));
    }

    private OriginRules() {}

    public static List<Origin> all() { return ALL; }

    public static Optional<Origin> find(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(CURRENT_BY_ID.getOrDefault(id, LEGACY_BY_ID.get(id)));
    }

    public static boolean mayChoose(boolean prompted, boolean alreadyChosen, String id) {
        return prompted && !alreadyChosen && id != null && CURRENT_BY_ID.containsKey(id);
    }

    public static List<Origin> known() { return KNOWN; }

    public static Optional<Origin> find(String id, int revision) {
        if (id == null) return Optional.empty();
        if (revision < 2 && LEGACY_BY_ID.containsKey(id)) return Optional.of(LEGACY_BY_ID.get(id));
        return find(id);
    }

    public static double bonus(String id, int revision, Perk perk) {
        if (id == null) return 0;
        var current = CURRENT_BY_ID.get(id);
        if (revision >= 2 && current != null) return current.bonuses().getOrDefault(perk, 0.0);
        return switch (perk) {
            case PICKAXE_SPEED, ORE_CHANCE -> "tnk:miner".equals(id) ? .10 : 0;
            case AXE_SPEED, LOG_CHANCE -> "tnk:forester".equals(id) ? .10 : 0;
            case CROP_YIELD -> "tnk:farmer".equals(id) ? .10 : 0;
            case HEALTH -> "tnk:farmer".equals(id) ? 1 : 0;
            case ATTACK -> "tnk:soldier".equals(id) ? 1 : 0;
            case ARMOUR -> "tnk:soldier".equals(id) || "tnk:blacksmith".equals(id) ? 1 : 0;
            case FLAT_MOVE -> "tnk:wanderer".equals(id) ? .005 : 0;
            case FALL_REDUCTION -> "tnk:wanderer".equals(id) ? .15 : 0;
            case LUCK -> "tnk:fisher".equals(id) ? 1 : 0;
            case FISH_CHANCE -> "tnk:fisher".equals(id) ? .10 : 0;
            case ENCHANT_REFUND -> "tnk:librarian".equals(id) ? .20 : 0;
            case ANVIL_REFUND -> "tnk:librarian".equals(id) ? .10 : "tnk:blacksmith".equals(id) ? .20 : 0;
            default -> 0;
        };
    }

    // Legacy API retained for existing integrations; current profiles use bonus(id, revision, perk).
    public static double enchantingRefund(String id) { return bonus(id, 1, Perk.ENCHANT_REFUND); }
    public static double anvilRefund(String id) { return bonus(id, 1, Perk.ANVIL_REFUND); }

    /** Fractional carry keeps small XP/drop bonuses exact without rounding every action upward. */
    public static Refund extra(int amount, double fraction, double remainder) {
        if (amount <= 0 || !Double.isFinite(fraction) || fraction <= 0 || fraction > 1) return new Refund(0, 0);
        double value = amount * fraction + (Double.isFinite(remainder) ? Math.clamp(remainder, 0, Math.nextDown(1.0)) : 0);
        int whole = (int)Math.min(Integer.MAX_VALUE, Math.floor(value + 1e-9));
        return new Refund(whole, Math.clamp(value - whole, 0, Math.nextDown(1.0)));
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
