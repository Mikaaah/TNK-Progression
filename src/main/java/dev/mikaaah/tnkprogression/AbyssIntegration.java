package dev.mikaaah.tnkprogression;

import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Optional Spiral 2.0.5 adapter. No loot listeners, item writes, ticks or world entity scans. */
public final class AbyssIntegration {
    static final String CONTEXT = "tnkprogression_abyss_v1";
    static final String RUN = "spiral_abyss_run", DAMAGE = "spiral_abyss_damage_scale";
    private static final ResourceLocation DIMENSION = ResourceLocation.parse("spiral_abyss:spiral_abyss");
    private static final ResourceLocation HEALTH = ResourceLocation.parse("spiral_abyss:health");
    private AbyssIntegration() {}

    /** Resolve only common/server APIs, once; Spiral may be absent on either physical side. */
    private static final class Api {
        static final Method GET, SESSIONS, LEVEL, ENDLESS, CONTROLLER, BOUNDS, CENTER;
        static final Method RUN_ID, FLOOR, WAVE, ENEMIES;
        static {
            Method get=null, sessions=null, level=null, endless=null, controller=null, bounds=null,
                    center=null, run=null, floor=null, wave=null, enemies=null;
            try {
                Class<?> parties=Class.forName("com.pla.spiral_abyss.world.AbyssParties");
                Class<?> session=Class.forName("com.pla.spiral_abyss.world.ArenaSession");
                Class<?> control=Class.forName("com.pla.spiral_abyss.encounter.FloorController");
                Class<?> area=Class.forName("com.pla.spiral_abyss.world.ArenaBounds");
                get=parties.getMethod("get", MinecraftServer.class); sessions=parties.getMethod("sessions");
                level=session.getMethod("level"); endless=session.getMethod("endless");
                controller=session.getMethod("controller"); bounds=session.getMethod("bounds"); center=area.getMethod("center");
                run=control.getMethod("runId"); floor=control.getMethod("currentFloor");
                wave=control.getMethod("currentWave"); enemies=control.getMethod("enemies");
            } catch (ClassNotFoundException ignored) {
                // No hard Spiral dependency.
            } catch (ReflectiveOperationException | LinkageError failure) {
                TnkProgression.LOG.error("Spiral API is incompatible; normal-world progression remains active", failure);
                get=null;
            }
            GET=get; SESSIONS=sessions; LEVEL=level; ENDLESS=endless; CONTROLLER=controller;
            BOUNDS=bounds; CENTER=center; RUN_ID=run; FLOOR=floor; WAVE=wave; ENEMIES=enemies;
        }
    }
    record Run(UUID id, int floor, int wave, Object controller) {}
    private static boolean dimension(Entity entity) {
        return entity.level() instanceof ServerLevel && entity.level().dimension().location().equals(DIMENSION);
    }
    /** An untrusted floor tag or proximity alone never identifies an arena encounter. */
    private static Run liveRun(Mob mob) {
        if (!dimension(mob) || !mob.getPersistentData().hasUUID(RUN) || Api.GET==null) return null;
        try {
            Object parties=Api.GET.invoke(null, ((ServerLevel)mob.level()).getServer());
            for (Object session:(Collection<?>)Api.SESSIONS.invoke(parties)) {
                if (Api.LEVEL.invoke(session)!=mob.level() || (boolean)Api.ENDLESS.invoke(session)) continue;
                Object controller=Api.CONTROLLER.invoke(session);
                UUID id=(UUID)Api.RUN_ID.invoke(controller);
                int floor=(int)Api.FLOOR.invoke(controller), wave=(int)Api.WAVE.invoke(controller);
                if (id==null || !id.equals(mob.getPersistentData().getUUID(RUN)) || !AbyssRules.normalFloor(floor)) continue;
                Vec3 center=(Vec3)Api.CENTER.invoke(Api.BOUNDS.invoke(session));
                if (Math.abs(mob.getX()-center.x)>128 || Math.abs(mob.getZ()-center.z)>128) continue;
                return new Run(id, floor, wave, controller);
            }
        } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot validate Spiral run", failure); }
        return null;
    }

    /** RETURN hook: both native scaling and the rotation bridge's HEAD hook have completed. */
    public static void scaled(Mob mob, int floor, Object category, double partyScale) {
        if (!ProgressionConfig.current.abyss.enabled || !AbyssRules.partyScale(partyScale)) return;
        Run run=liveRun(mob);
        if (run==null || run.floor()!=floor || assigned(mob)) return;
        String kind=category instanceof Enum<?> value ? value.name() : "";
        boolean boss=kind.equals("BOSS");
        if (boss && !AbyssRules.bossSlot(floor, run.wave())) return;
        var data=mob.getPersistentData();
        // The native category is authoritative: MINI_BOSS and CHAMPION_MOB are not full bosses.
        if (boss!=data.getBoolean("spiral_abyss_wave_boss")) return;
        int target=ProgressionConfig.current.abyss.floorLevels[floor-1];
        boolean bridge=data.contains("tnk_abyss_floor",Tag.TAG_INT) && data.getInt("tnk_abyss_floor")==floor
                && data.contains("tnk_abyss_target_level",Tag.TAG_INT)
                && data.getInt("tnk_abyss_target_level")>=1 && data.getInt("tnk_abyss_target_level")<=100
                && data.contains("tnk_abyss_boss",Tag.TAG_BYTE) && data.getBoolean("tnk_abyss_boss")==boss
                && data.getString("tnk_abyss_rotation").matches("[0-9]{4}-(0[1-9]|1[0-2])")
                && ResourceLocation.tryParse(data.getString("tnk_abyss_theme"))!=null;
        if (bridge) target=data.getInt("tnk_abyss_target_level");
        var saved=new CompoundTag();
        saved.putUUID("run",run.id()); saved.putInt("floor",floor); saved.putInt("wave",run.wave());
        saved.putInt("target",target); saved.putBoolean("boss",boss); saved.putDouble("party",partyScale);
        saved.putInt("level",AbyssRules.level(target,boss,BossLevels.baseLevel(mob)));
        saved.putBoolean("bridge",bridge);
        if (bridge) {
            saved.putString("theme",data.getString("tnk_abyss_theme"));
            saved.putString("rotation",data.getString("tnk_abyss_rotation"));
        }
        saved.putBoolean("healthBaseline",ProgressionConfig.current.scaleMobs && ProgressionConfig.current.abyss.tnkHealthBaseline);
        saved.putBoolean("damageBaseline",ProgressionConfig.current.scaleMobs && ProgressionConfig.current.abyss.tnkDamageBaseline);
        data.put(CONTEXT,saved);
    }

    /** Saved assignments need not have a live session after restart; they never reroll. */
    public static boolean assigned(Entity mob) {
        if (!dimension(mob) || !mob.getPersistentData().hasUUID(RUN)) return false;
        var saved=mob.getPersistentData().getCompound(CONTEXT);
        return saved.getBoolean("assigned") && valid(mob,saved);
    }
    private static boolean valid(Entity mob, CompoundTag saved) {
        return mob.getPersistentData().hasUUID(RUN) && saved.hasUUID("run") && saved.getUUID("run").equals(mob.getPersistentData().getUUID(RUN))
                && AbyssRules.normalFloor(saved.getInt("floor")) && saved.getInt("wave")>=1 && saved.getInt("wave")<=3
                && saved.getInt("target")>=1 && saved.getInt("target")<=100
                && saved.getInt("level")>=1 && saved.getInt("level")<=110 && AbyssRules.partyScale(saved.getDouble("party"))
                && (!saved.getBoolean("boss") || AbyssRules.bossSlot(saved.getInt("floor"),saved.getInt("wave")));
    }
    public static boolean prepareJoin(Mob mob) {
        if (assigned(mob)) return true;
        if (!ProgressionConfig.current.abyss.enabled) return false;
        var data=mob.getPersistentData(); var saved=data.getCompound(CONTEXT);
        if (!valid(mob,saved)) return false;
        Run run=liveRun(mob);
        if (run==null || run.floor()!=saved.getInt("floor") || run.wave()!=saved.getInt("wave")) return false;
        // Only a new/captured assignment is rebased. Reloads never reapply Spiral multipliers.
        if (saved.getBoolean("healthBaseline")) {
            var health=mob.getAttribute(Attributes.MAX_HEALTH);
            if (health!=null) {
                health.removeModifier(HEALTH);
                if (!data.getBoolean("SkipRaisingHealth")) health.addPermanentModifier(new AttributeModifier(
                        HEALTH,saved.getDouble("party")-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            data.remove("spiral_abyss_health_damage_ratio");
        }
        if (saved.getBoolean("damageBaseline")) data.putDouble(DAMAGE,saved.getDouble("party"));
        data.putInt("tnk_level",saved.getInt("level"));
        saved.putBoolean("assigned",true); data.put(CONTEXT,saved);
        return true;
    }
    public static boolean skipHealth(Mob mob) {
        return assigned(mob) && mob.getPersistentData().getBoolean("SkipRaisingHealth");
    }
    public static boolean healthBaseline(Mob mob) { return mob.getPersistentData().getCompound(CONTEXT).getBoolean("healthBaseline"); }
    public static boolean damageBaseline(Mob mob) { return mob.getPersistentData().getCompound(CONTEXT).getBoolean("damageBaseline"); }
    public static boolean fullBoss(Mob mob) { return assigned(mob) && mob.getPersistentData().getCompound(CONTEXT).getBoolean("boss"); }

    /** Native phase transfer completed; preserve the root's snapshot rather than today's rotation. */
    public static void phase(Mob root, Mob child) {
        if (!assigned(root) || assigned(child)) return;
        var saved=root.getPersistentData().getCompound(CONTEXT).copy();
        saved.putBoolean("assigned",false); child.getPersistentData().put(CONTEXT,saved);
    }
    /** Called only for mobs Spiral has adopted, after its native run/host/phase initialization. */
    public static void adopted(Mob mob) {
        if (!ProgressionConfig.current.abyss.enabled || assigned(mob)) return;
        Run run=liveRun(mob); if (run==null) return;
        if (!valid(mob,mob.getPersistentData().getCompound(CONTEXT))) {
            try {
                // A bounded current-stage roster, never a world-wide scan or nearest-player guess.
                for (Object entity:(List<?>)Api.ENEMIES.invoke(run.controller())) {
                    if (!(entity instanceof Mob root) || !assigned(root)) continue;
                    var saved=root.getPersistentData().getCompound(CONTEXT).copy();
                    if (!saved.getUUID("run").equals(run.id())) continue;
                    boolean phase=mob.getPersistentData().contains("spiral_abyss_phase_boss")
                            && mob.getPersistentData().getBoolean("spiral_abyss_wave_boss");
                    saved.putBoolean("boss",phase); saved.putBoolean("assigned",false);
                    saved.putInt("level",AbyssRules.level(saved.getInt("target"),phase,BossLevels.baseLevel(mob)));
                    mob.getPersistentData().put(CONTEXT,saved); break;
                }
            } catch (ReflectiveOperationException failure) { throw new IllegalStateException("Cannot read Spiral reinforcement roster",failure); }
        }
        ProgressionEvents.join(new net.neoforged.neoforge.event.entity.EntityJoinLevelEvent(mob,mob.level()));
    }
    /** Split/conversion events already establish ownership; never infer it from proximity. */
    public static void inherit(Mob root, Mob child) {
        if (!assigned(root) || child.level()!=root.level()) return;
        var from=root.getPersistentData(); var to=child.getPersistentData();
        // Do not assign Spiral's native RUN here: its discovery code skips already-owned
        // mobs, which would leave split/conversion children outside the stage roster.
        var saved=from.getCompound(CONTEXT).copy(); saved.putBoolean("assigned",false);
        to.put(CONTEXT,saved);
        to.putInt("tnk_level",ProgressionAPI.mobLevel(root));
        to.putBoolean("tnk_boss_level_v1",true);
        // The child did not inherit attributes: party scaling must be installed once.
        var health=child.getAttribute(Attributes.MAX_HEALTH);
        if (health!=null && from.getCompound(CONTEXT).getBoolean("healthBaseline") && !to.getBoolean("SkipRaisingHealth")) {
            health.removeModifier(HEALTH);
            health.addPermanentModifier(new AttributeModifier(HEALTH,from.getCompound(CONTEXT).getDouble("party")-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
    private static Entity owner(Entity source) {
        Set<UUID> visited=new HashSet<>();
        for (int depth=0;source!=null && depth<8 && visited.add(source.getUUID());depth++) {
            if (assigned(source)) return source;
            Entity next=CombatProgression.magicOwner(source);
            if (next==null) next=source instanceof OwnableEntity own?own.getOwner():source instanceof TraceableEntity trace?trace.getOwner():null;
            if (next==null) return null; source=next;
        }
        return null;
    }
    @SubscribeEvent public static void damage(LivingIncomingDamageEvent event) {
        var source=event.getSource(); Entity attacker=owner(source.getEntity());
        if (attacker==null) attacker=owner(source.getDirectEntity());
        if (!(attacker instanceof Mob mob)) return;
        var saved=mob.getPersistentData().getCompound(CONTEXT);
        if (!saved.getBoolean("damageBaseline")) return;
        boolean melee=source.getDirectEntity()==mob && (source.is(DamageTypes.MOB_ATTACK) || source.is(DamageTypes.MOB_ATTACK_NO_AGGRO));
        // No TNK attack-attribute modifier on arena mobs: every attributable damage path
        // receives this factor once, even a fixed area attack using the MOB_ATTACK type.
        double factor=melee || ProgressionConfig.current.abyss.scaleIndirectDamage
                ? 1+ProgressionConfig.current.damagePerLevel*(ProgressionAPI.mobLevel(mob)-1) : 1;
        // Spiral scales sources whose getEntity has RUN; owned indirect sources may lack it.
        if (source.getEntity()==null || !source.getEntity().getPersistentData().hasUUID(RUN)) factor*=saved.getDouble("party");
        event.setAmount((float)(event.getAmount()*factor));
    }
}
