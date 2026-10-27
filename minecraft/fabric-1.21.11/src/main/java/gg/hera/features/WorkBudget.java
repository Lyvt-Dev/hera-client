package gg.hera.features;
import java.util.function.LongSupplier;
/** Cooperative game-thread work cap; a single operation cannot be preempted. */
public final class WorkBudget {
    private final int limit;private final long start,nanos;private final LongSupplier clock;private int used;
    public WorkBudget(int limit,long nanos){this(limit,nanos,System::nanoTime);}
    public WorkBudget(int limit,long nanos,LongSupplier clock){if(limit<1||nanos<1)throw new IllegalArgumentException();this.limit=limit;this.nanos=nanos;this.clock=clock;start=clock.getAsLong();}
    public boolean hasTime(){return used<limit&&clock.getAsLong()-start<nanos;}
    public boolean step(){if(!hasTime())return false;used++;return true;}
}
