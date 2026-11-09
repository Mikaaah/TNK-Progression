package dev.mikaaah.tnkprogression;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
/** Legacy smoke entry point forwards to the direct XP harness. */
public final class ProgressionSmoke {public static void run(ServerStartedEvent event){DirectXpSmoke.run(event);}}
