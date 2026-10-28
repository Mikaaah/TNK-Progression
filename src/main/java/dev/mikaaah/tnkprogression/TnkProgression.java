package dev.mikaaah.tnkprogression;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
@Mod("tnkprogression")
public final class TnkProgression {
 public static final Logger LOG=LogUtils.getLogger();
 public TnkProgression(net.neoforged.bus.api.IEventBus modBus){BalanceConfig.reload();ProgressionConfig.reload();TitleConfig.reload();TitlePerks.reload();NeoForge.EVENT_BUS.register(ProgressionEvents.class);NeoForge.EVENT_BUS.register(MobLevels.class);NeoForge.EVENT_BUS.register(TreePassives.class);NeoForge.EVENT_BUS.register(LevelBadges.class);NeoForge.EVENT_BUS.register(Titles.class);NeoForge.EVENT_BUS.register(TitlePerks.class);NeoForge.EVENT_BUS.register(CharacterState.class);NeoForge.EVENT_BUS.register(ActivityEvents.class);NeoForge.EVENT_BUS.register(CombatProgression.class);modBus.addListener(LevelBadgePayload::register);NeoForge.EVENT_BUS.addListener(ProgressionCommands::register);NeoForge.EVENT_BUS.addListener(ProgressionSmoke::run);}
}
