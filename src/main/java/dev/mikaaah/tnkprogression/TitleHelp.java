package dev.mikaaah.tnkprogression;

import java.util.*;

/** The same server-owned acquisition rules are shown in the menu and checked for unlocks. */
public final class TitleHelp {
    private TitleHelp() {}
    public static List<String> describe(TitleConfig.Definition title, boolean legacy, Optional<SkillBridge.TitleSnapshot> skills) {
        if(legacy)return List.of("Legacy title: retained because you earned it before the catalogue cleanup.",
                "You can keep using its cosmetic badge and original passive benefits. It is no longer listed for new players.",title.description());
        var lines=new ArrayList<String>();
        lines.add(title.description());
        if(title.automatic()){
            lines.add("Unlock source: purchased skills in the Character skill tree. Unlocks automatically when all requirements are met.");
            if(skills.isEmpty())lines.add("Skill-tree data is unavailable. This title requires Pufferfish Skills and the configured Character category.");
            else {
                if(title.minSpentPoints()>0)lines.add("Spent Skill Points: "+skills.get().spent()+" / "+title.minSpentPoints()+".");
                for(String id:title.requiredSkills())lines.add(skillName(id)+": "+(skills.get().unlocked().contains(id)?"unlocked":"not unlocked")+".");
            }
        } else if(SpecialTitles.commandOnly(title.id()))lines.add("Unlock source: administrator assignment. Cosmetic only; this title grants no permissions.");
        else if(title.id().equals("tnk:beta_tester"))lines.add("Unlock source: pack staff. Awarded for participation in the TNK3 beta.");
        else lines.add("Unlock source: the command reward on the specified quest. Complete and claim that quest reward. This title does not unlock from activity ranks or kills alone.");
        return List.copyOf(lines);
    }
    private static String skillName(String id){
        if(id.equals("hunter_subclass_1_mastery"))return "Hunter / Ranger Mastery";
        return Arrays.stream(id.split("_")).map(s->s.isEmpty()?s:Character.toUpperCase(s.charAt(0))+s.substring(1)).collect(java.util.stream.Collectors.joining(" "));
    }
}
