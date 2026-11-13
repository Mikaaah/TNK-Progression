package dev.mikaaah.tnkprogression;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
@Mod("tnkprogression")
public final class TnkProgression {
 public static final Logger LOG=LogUtils.getLogger();
 public TnkProgression(net.neoforged.bus.api.IEventBus modBus){AscensionItem.ITEMS.register(modBus);BalanceConfig.reload();ProgressionConfig.reload();DirectXpConfig.reload();TitleConfig.reload();TitlePerks.reload();NeoForge.EVENT_BUS.register(ProgressionEvents.class);NeoForge.EVENT_BUS.register(AbyssIntegration.class);NeoForge.EVENT_BUS.register(MobLevels.class);NeoForge.EVENT_BUS.register(TreePassives.class);NeoForge.EVENT_BUS.register(SourceTreeBonuses.class);NeoForge.EVENT_BUS.register(LevelBadges.class);NeoForge.EVENT_BUS.register(Titles.class);NeoForge.EVENT_BUS.register(TitlePerks.class);NeoForge.EVENT_BUS.register(CharacterState.class);NeoForge.EVENT_BUS.register(Origins.class);NeoForge.EVENT_BUS.register(OriginPerks.class);NeoForge.EVENT_BUS.register(OriginGameplay.class);NeoForge.EVENT_BUS.register(ActivityEvents.class);NeoForge.EVENT_BUS.register(CombatProgression.class);NeoForge.EVENT_BUS.register(ExplorationXp.class);NeoForge.EVENT_BUS.register(CookingXp.class);modBus.addListener(LevelBadgePayload::register);NeoForge.EVENT_BUS.addListener(ProgressionCommands::register);NeoForge.EVENT_BUS.addListener(DirectXpSmoke::run);NeoForge.EVENT_BUS.addListener(BossActivitySmoke::run);NeoForge.EVENT_BUS.addListener(BossActivitySmoke::fixture);}
}
