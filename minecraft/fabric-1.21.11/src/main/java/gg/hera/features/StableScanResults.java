package gg.hera.features;
import java.util.*;
/** Keep the published scan visible while the next scan is assembled. */
public final class StableScanResults<T> {
    private List<T> visible=List.of();private boolean complete;
    public List<T> visible(){return visible;}
    public void progress(Collection<T> values){if(!complete)visible=List.copyOf(values);}
    public void commit(Collection<T> values){visible=List.copyOf(values);complete=true;}
    public void clear(){visible=List.of();complete=false;}
}
