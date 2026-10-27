package gg.hera.features;
import java.util.*;
import java.util.function.*;
/** Retains only the nearest K candidates, without sorting the entire world. */
public final class NearestTargets<T> {
    private final int limit;private final Comparator<T> order;private final PriorityQueue<T> heap;
    public NearestTargets(int limit,ToDoubleFunction<T> distance,ToIntFunction<T> id){if(limit<1)throw new IllegalArgumentException();this.limit=limit;order=Comparator.comparingDouble(distance).thenComparingInt(id);heap=new PriorityQueue<>(limit,order.reversed());}
    public void clear(){heap.clear();}
    public void offer(T value){if(heap.size()<limit)heap.add(value);else if(order.compare(value,heap.peek())<0){heap.poll();heap.add(value);}}
    public List<T> snapshot(){var result=new ArrayList<>(heap);result.sort(order);return List.copyOf(result);}
}
