import java.nio.file.*;
import net.puffish.skillsmod.api.json.*;
import net.puffish.skillsmod.api.config.ConfigContext;
import net.puffish.skillsmod.config.*;
import net.puffish.skillsmod.config.colors.*;
import net.puffish.skillsmod.api.util.Result;
public class ThemeFormatCheck {
 static ConfigContext context = new ConfigContext(){public net.minecraft.server.MinecraftServer getServer(){return null;} public void emitWarning(String message){throw new AssertionError(message);}};
 static void ok(Result<?,?> r){if(r.getSuccess().isEmpty())throw new AssertionError(r.getFailure().orElse(null));}
 public static void main(String[] args)throws Exception{
  var root=JsonElement.parseFile(Path.of(args[0]),JsonPath.create("theme")).getSuccess().orElseThrow().getAsObject().getSuccess().orElseThrow();
  var frames=root.getObject("frames").getSuccess().orElseThrow();
  for(String role:new String[]{"minor","notable","hub","keystone"}) ok(FrameConfig.parse(frames.get(role).getSuccess().orElseThrow(),context));
  ok(BackgroundConfig.parse(root.get("background").getSuccess().orElseThrow(),context));
  ok(ColorsConfig.parse(root.get("colors").getSuccess().orElseThrow(),context));
  System.out.println("PUFFISH_0_19_2_THEME_PARSE_OK: 4 frame definitions / 20 states; background and colour config accepted");
 }
}
