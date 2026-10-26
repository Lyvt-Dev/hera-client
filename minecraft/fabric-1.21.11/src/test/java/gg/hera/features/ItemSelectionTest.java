package gg.hera.features;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ItemSelectionTest {
    @Test void emptySelectionMarksNothing(){assertFalse(ItemSelection.matches(Set.of(),"minecraft:diamond"));}
    @Test void onlyExactRegistryIdsMatch(){var s=Set.of("minecraft:diamond");assertTrue(ItemSelection.matches(s,"minecraft:diamond"));assertFalse(ItemSelection.matches(s,"minecraft:diamond_sword"));}
    @Test void sanitizeNullMalformedAndDuplicates(){assertEquals(Set.of("minecraft:diamond"),ItemSelection.clean(Arrays.asList(null,"minecraft:diamond","bad id","minecraft:diamond")));}
    @Test void boundedSelection(){var ids=new ArrayList<String>();for(int i=0;i<600;i++)ids.add("test:item_"+i);assertEquals(512,ItemSelection.clean(ids).size());}
}
