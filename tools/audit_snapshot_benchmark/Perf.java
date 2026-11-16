import java.util.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.lang.management.ManagementFactory;
import com.sun.management.ThreadMXBean;

public class Perf {
 static final String PREFIX="dev.mikaaah.tnkprogression.";
 static Class<?> cls(String n)throws Exception{return Class.forName(PREFIX+n,false,Perf.class.getClassLoader());}
 static Object gson;
 static Method json;
 static Method help;
 static Constructor<?> activity,snapshot,title,perk,definition;
 static final String[] KEYS={"combat","mining","farming","cooking","fishing","exploration","quest"};
 static Object toObject(String name)throws Exception{
  try(var in=Perf.class.getResourceAsStream("/data/tnkprogression/"+name)){
   String s=new String(in.readAllBytes(),StandardCharsets.UTF_8);
   return gson.getClass().getMethod("fromJson",String.class,Class.class).invoke(gson,s,Map.class);
  }
 }
 @SuppressWarnings("unchecked") static List<String> info(String key)throws Exception{return (List<String>)help.invoke(null,null,key);}
 static String serialize(Object o)throws Exception{return (String)json.invoke(gson,o);}
 static Object state(List<Object> activities,List<Object> titles)throws Exception{return snapshot.newInstance(42,100,1234L,42000L,"Maximum level",73,30,"tnk:prepared","tnk:skill_delver","mining",false,0L,activities,titles,List.of("mining  +20 XP","quest  +150 XP"),"tnk:miner",1,false,false,0L);}
 static List<Object> activities(boolean includeInfo)throws Exception{
  var result=new ArrayList<Object>();
  for(String k:KEYS)result.add(activity.newInstance(k,12345.0,20.0,40L,includeInfo?info(k):List.of()));
  return result;
 }
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
  Class<?> gc=Class.forName("com.google.gson.Gson");gson=gc.getConstructor().newInstance();json=gc.getMethod("toJson",Object.class);
  help=cls("ActivityHelp").getMethod("describe",Class.forName("net.minecraft.server.level.ServerPlayer",false,Perf.class.getClassLoader()),String.class);
  activity=cls("CharacterState$Activity").getConstructors()[0];snapshot=cls("CharacterState$Snapshot").getConstructors()[0];
  title=Arrays.stream(cls("CharacterState$Title").getConstructors()).filter(c->c.getParameterCount()==9).findFirst().orElseThrow();
  perk=cls("CharacterState$Perk").getConstructors()[0];definition=cls("TitleConfig$Definition").getConstructors()[0];
  Method desc=cls("TitleHelp").getMethod("describe",cls("TitleConfig$Definition"),boolean.class,Optional.class);
  Object ss=cls("SkillBridge$TitleSnapshot").getConstructors()[0].newInstance(30,Set.of("miner_mastery"));
  Map<String,Object> catalog=(Map<String,Object>)toObject("default_titles.json");
  Map<String,Object> perks=(Map<String,Object>)((Map<?,?>)toObject("default_title_perks.json")).get("titles");
  List<Object> titles=new ArrayList<>();
  for(var raw:(List<Map<String,Object>>)catalog.get("titles")){
   String id=(String)raw.get("id"),category=(String)raw.get("skillCategory");
   Object def=definition.newInstance(id,raw.get("label"),raw.get("artwork"),raw.get("style"),raw.get("colour"),raw.get("description"),category,((Double)raw.get("minSpentPoints")).intValue(),raw.get("requiredSkills"));
   var ps=new ArrayList<Object>();
   for(var p:(List<Map<String,Object>>)perks.getOrDefault(id,List.of()))ps.add(perk.newInstance(p.get("description"),!((String)p.get("attribute")).startsWith("irons_spellbooks:")));
   Object details=desc.invoke(null,def,false,category.isBlank()?Optional.empty():Optional.of(ss));
   titles.add(title.newInstance(id,raw.get("label"),raw.get("description"),"tnkprogression.title."+id.replace(':','.'),Integer.parseInt((String)raw.get("colour"),16),Set.of("tnk:prepared","tnk:skill_delver","tnk:initiate","tnk:disciplined").contains(id),ps,false,details));
  }
  int total=0;for(String k:KEYS){var list=info(k);total+=list.size();System.out.println(k+"_help_lines="+list.size());}
  System.out.println("help_lines_total="+total);
  System.out.println("active_titles="+titles.size());
  System.out.println("full_json_utf8="+serialize(state(activities(true),titles)).getBytes(StandardCharsets.UTF_8).length);
  System.out.println("current_partial_utf8="+serialize(state(activities(true),List.of())).getBytes(StandardCharsets.UTF_8).length);
  System.out.println("lean_partial_utf8="+serialize(state(activities(false),List.of())).getBytes(StandardCharsets.UTF_8).length);
  ThreadMXBean mx=(ThreadMXBean)ManagementFactory.getThreadMXBean();mx.setThreadAllocatedMemoryEnabled(true);
  int warm=args.length>0?Integer.parseInt(args[0]):2000,iter=args.length>1?Integer.parseInt(args[1]):10000,rounds=args.length>2?Integer.parseInt(args[2]):3;
  System.out.println("java_version="+System.getProperty("java.version"));
  System.out.println("gson_source="+gc.getProtectionDomain().getCodeSource().getLocation());
  System.out.println("warmup_each="+warm+" iterations_each="+iter+" rounds_each="+rounds);
  for(boolean include:new boolean[]{true,false})for(int i=0;i<warm;i++)serialize(state(activities(include),List.of()));
  for(int round=0;round<rounds;round++){
   for(boolean include:new boolean[]{true,false}){
    long bytes=mx.getThreadAllocatedBytes(Thread.currentThread().threadId()),start=System.nanoTime();long sink=0;
    for(int i=0;i<iter;i++)sink+=serialize(state(activities(include),List.of())).length();
    long elapsed=System.nanoTime()-start,used=mx.getThreadAllocatedBytes(Thread.currentThread().threadId())-bytes;
    System.out.printf(Locale.ROOT,"%s round=%d micros_per_fixture=%.3f bytes_alloc_per_fixture=%.1f sink=%d%n",include?"current":"lean",round,elapsed/1000.0/iter,used/(double)iter,sink);
   }
  }
 }
}
