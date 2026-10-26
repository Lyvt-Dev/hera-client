package gg.hera.features;

import java.util.*;

/** Exact registry IDs: an empty selection intentionally matches nothing. */
public final class ItemSelection {
    private ItemSelection() {}
    public static LinkedHashSet<String> clean(Collection<String> input) {
        var result=new LinkedHashSet<String>();
        if(input!=null) for(String id:input) {
            if(id!=null && id.length()<=160 && id.matches("[a-z0-9_.-]+:[a-z0-9/._-]+"))result.add(id);
            if(result.size()>=512)break;
        }
        return result;
    }
    public static boolean matches(Set<String> selected,String id){return selected!=null && selected.contains(id);}
}
