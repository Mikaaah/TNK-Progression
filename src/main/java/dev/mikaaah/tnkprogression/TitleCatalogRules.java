package dev.mikaaah.tnkprogression;

import java.util.*;

/** Retired badges remain valid saved rewards, outside the new-player catalogue. */
public final class TitleCatalogRules {
    private TitleCatalogRules() {}
    public record Split(List<TitleConfig.Definition> active, List<TitleConfig.Definition> legacy) {}
    public static boolean retiredBuiltin(String id) {
        return id != null && (id.startsWith("tnk:mastery_") || id.startsWith("tnk:letsdo_"));
    }
    static void requireUniqueIds(List<TitleConfig.Definition> definitions) {
        var ids=new HashSet<String>();
        for(var d:definitions)if(d==null || d.id()==null || !ids.add(d.id()))throw new IllegalArgumentException("Invalid/duplicate title ID in catalogue");
    }
    public static Split split(List<TitleConfig.Definition> definitions, List<TitleConfig.Definition> archived) {
        requireUniqueIds(definitions);requireUniqueIds(archived);
        var legacy = new LinkedHashMap<String, TitleConfig.Definition>();
        archived.forEach(d -> legacy.put(d.id(), d));
        var active = new ArrayList<TitleConfig.Definition>();
        var labels = new HashSet<String>();
        for (var d : definitions) {
            String label = d.label().trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (retiredBuiltin(d.id()) || legacy.containsKey(d.id()) || !labels.add(label)) legacy.put(d.id(), d);
            else active.add(d);
        }
        return new Split(List.copyOf(active), List.copyOf(legacy.values()));
    }
}
