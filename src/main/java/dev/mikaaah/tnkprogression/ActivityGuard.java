package dev.mikaaah.tnkprogression;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.util.FakePlayer;

/** Server-owned receipts and per-section provenance persist across logout/restart. */
public final class ActivityGuard {
    private ActivityGuard() {}
    public static boolean player(ServerPlayer p) { return !(p instanceof FakePlayer) && !p.isCreative() && !p.isSpectator(); }
    public static long now(ServerPlayer p) { return p.server.overworld().getGameTime(); }
    public static boolean ready(ServerPlayer p, String key, long delay) {
        CompoundTag n = ProgressionAPI.profile(p).getCompound("activity_clocks");
        long now = now(p);
        if (n.contains(key) && !ActivityRules.elapsed(now, n.getLong(key), delay)) return false;
        n.putLong(key, now); ProgressionAPI.profile(p).put("activity_clocks", n);
        ProgressionData.get(p.server).setDirty(); return true;
    }
    // 4096 positions per 16³ section, 512 bytes per touched section rather than a string per block.
    public static boolean marked(ServerLevel world, BlockPos pos) { return bit(world, pos, false); }
    public static void mark(ServerLevel world, BlockPos pos) { bit(world, pos, true); }
    private static boolean bit(ServerLevel world, BlockPos pos, boolean write) {
        var data = ProgressionData.get(world.getServer()); var sections = data.entry("mining_provenance_v2");
        String key = world.dimension().location() + "/" + (pos.getX() >> 4) + "/" + (pos.getY() >> 4) + "/" + (pos.getZ() >> 4);
        long[] bits = sections.getLongArray(key);
        int index = (pos.getX() & 15) | ((pos.getZ() & 15) << 4) | ((pos.getY() & 15) << 8);
        boolean marked = bits.length == 64 && (bits[index >> 6] & (1L << (index & 63))) != 0;
        // Honor legacy placement records too; never delete the tombstone on break.
        marked |= data.entry("placed").getBoolean(world.dimension().location() + "/" + pos.asLong());
        if (write && !marked) {
            if (bits.length != 64) bits = new long[64];
            bits[index >> 6] |= 1L << (index & 63); sections.putLongArray(key, bits); data.setDirty();
        }
        return marked;
    }
    public static double combatRepetition(ServerPlayer p, String key) {
        var profile = ProgressionAPI.profile(p); var windows = profile.getCompound("combat_windows");
        long now = now(p), duration = ProgressionConfig.current.combatWindowTicks;
        for (String k : java.util.Set.copyOf(windows.getAllKeys())) {
            var w = windows.getCompound(k);
            if (now < w.getLong("start") || now - w.getLong("start") >= duration) windows.remove(k);
        }
        // Bounded transient window; never evict provenance or reward claims.
        if (!windows.contains(key) && windows.size() >= 256) {
            String oldest = windows.getAllKeys().stream().min(java.util.Comparator.comparingLong(k -> windows.getCompound(k).getLong("start"))).orElseThrow();
            windows.remove(oldest);
        }
        var w = windows.getCompound(key); if (!w.contains("start")) w.putLong("start", now);
        long count = w.getLong("count"); w.putLong("count", count + 1); windows.put(key, w);
        profile.put("combat_windows", windows); ProgressionData.get(p.server).setDirty();
        return ActivityRules.repetition(count);
    }
}
