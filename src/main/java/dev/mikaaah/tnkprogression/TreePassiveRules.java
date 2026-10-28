package dev.mikaaah.tnkprogression;

import java.util.*;

public final class TreePassiveRules {
 private TreePassiveRules(){}
 public static final Map<String,Double> CAPS=Map.ofEntries(Map.entry("guard",.25),Map.entry("evasion",.15),Map.entry("burning_damage",.35),Map.entry("longshot",.15),Map.entry("well_fed_damage",.20),Map.entry("effect_damage",.20),Map.entry("potion_duration",.50),Map.entry("food_recovery",3.0),Map.entry("leech",1.0),Map.entry("ignite",.20),Map.entry("second_wind",1.0));
 public static Map<String,Double> aggregate(Set<String> tags,Map<String,Map<String,Double>> definitions){
  Map<String,Double> result=new HashMap<>();
  for(String tag:tags){
   if(!tag.startsWith("tnk_tree."))continue;
   for(var bonus:definitions.getOrDefault(tag.substring(9),Map.of()).entrySet()){
    double cap=CAPS.getOrDefault(bonus.getKey(),0.0),value=bonus.getValue();
    if(Double.isFinite(value)&&value>0&&cap>0)result.merge(bonus.getKey(),Math.min(value,cap),(a,b)->Math.min(cap,a+b));
   }
  }
  return Map.copyOf(result);
 }
 public static double damageMultiplier(Map<String,Double> stats,boolean burning,boolean fed,boolean beneficial,boolean projectile,double distance){
  double bonus=(burning?stats.getOrDefault("burning_damage",0.0):0)+(fed?stats.getOrDefault("well_fed_damage",0.0):0)+(beneficial?stats.getOrDefault("effect_damage",0.0):0);
  if(projectile)bonus+=stats.getOrDefault("longshot",0.0)*Math.clamp(distance/16,0,1);
  return 1+Math.clamp(bonus,0,.75);
 }
 public static int potionDuration(int base,double bonus){return base<=0?base:(int)Math.min(Integer.MAX_VALUE,base*(1+Math.clamp(bonus,0,.5)));}
}
