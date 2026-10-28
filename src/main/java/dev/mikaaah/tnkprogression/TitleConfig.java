package dev.mikaaah.tnkprogression;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

/** Server-owned title catalog, independently editable without rebuilding the mod. */
public final class TitleConfig {
    public record Definition(String id,String label,String artwork,String style,String colour,String description,
                             String skillCategory,int minSpentPoints,List<String> requiredSkills) {
        public String translationKey(){return "tnkprogression.title."+id.replace(':','.').replace('/','.');}
        public int rgb(){return Integer.parseInt(colour,16);}
        public boolean automatic(){return skillCategory!=null && !skillCategory.isBlank() && (minSpentPoints>0 || !requiredSkills.isEmpty());}
    }
    public record Catalog(boolean autoEquipFirst,List<Definition> titles) {}
    public static Catalog current=new Catalog(true,List.of());
    private static Map<String,Definition> byId=Map.of();
    private static final Map<String,String> LEGACY_TREE_SKILLS=Map.of("wayfarer_10","hunter_subclass_1_mastery","delver_10","miner_mastery","homesteader_10","cook_mastery","artificer_10","blacksmith_mastery","survivor_10","blacksmith_defensive_keystone_1","adventurer_10","hunter_mastery");
    public static Optional<Definition> get(String id){return Optional.ofNullable(id==null?null:byId.get(id));}
    public static boolean reload(){
        Path path=FMLPaths.CONFIGDIR.get().resolve("tnk-titles.json");
        try {
            Gson gson=new GsonBuilder().setPrettyPrinting().create();
            if(!Files.exists(path))try(var in=TitleConfig.class.getResourceAsStream("/data/tnkprogression/default_titles.json")){
                if(in==null)throw new IllegalStateException("Missing bundled title catalog");Files.copy(in,path);
            }
            Catalog next=gson.fromJson(Files.readString(path),Catalog.class);
            if(next==null || next.titles()==null || next.titles().size()>256)throw new IllegalArgumentException("Invalid catalog size");
            boolean migrated=false;List<Definition> updated=new ArrayList<>();
            for(var d:next.titles()){
                if(d!=null && "tnk:character".equals(d.skillCategory()) && d.id()!=null && d.id().startsWith("tnk:skill_") && d.requiredSkills()!=null && d.requiredSkills().size()==1 && LEGACY_TREE_SKILLS.containsKey(d.requiredSkills().getFirst())){
                    String node=LEGACY_TREE_SKILLS.get(d.requiredSkills().getFirst());
                    d=new Definition(d.id(),d.label(),d.artwork(),d.style(),d.colour(),"Unlock "+node.replace('_',' ')+" in the character tree.",d.skillCategory(),d.minSpentPoints(),List.of(node));migrated=true;
                }
                updated.add(d);
            }
            next=new Catalog(next.autoEquipFirst(),updated);
            Map<String,Definition> map=new LinkedHashMap<>();Set<String> translations=new HashSet<>();
            for(var d:next.titles()){
                if(d==null || d.id()==null || d.id().length()>100 || ResourceLocation.tryParse(d.id())==null
                    || !validLabel(d.label(),40) || !validLabel(d.description(),200)
                    || d.colour()==null || !d.colour().matches("[0-9a-fA-F]{6}")
                    || d.artwork()==null || !d.artwork().matches("[A-Z0-9_]{1,32}") || d.style()==null || d.style().length()>32
                    || d.requiredSkills()==null || d.requiredSkills().size()>64 || d.requiredSkills().stream().anyMatch(s->!validLabel(s,100))
                    || d.minSpentPoints()<0 || d.minSpentPoints()>100000
                    || (d.skillCategory()!=null && !d.skillCategory().isBlank() && ResourceLocation.tryParse(d.skillCategory())==null)
                    || map.putIfAbsent(d.id(),d)!=null || !translations.add(d.translationKey()))throw new IllegalArgumentException("Invalid/duplicate title "+d);
            }
            if(migrated)Files.writeString(path,gson.toJson(next));
            current=new Catalog(next.autoEquipFirst(),List.copyOf(next.titles()));byId=Map.copyOf(map);return true;
        }catch(Exception e){TnkProgression.LOG.error("Keeping previous title catalog",e);return false;}
    }
    static boolean validLabel(String text,int max){return text!=null && !text.isBlank() && text.length()<=max && text.codePoints().noneMatch(c->Character.isISOControl(c) || c==0xA7);}
    public static boolean qualifies(Definition d,int spent,Set<String> unlocked){
        return d.automatic() && spent>=d.minSpentPoints() && unlocked.containsAll(d.requiredSkills());
    }
}
