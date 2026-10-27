package gg.hera.features;
/** Follow-up requires both server observations for this exact target/crystal. */
public final class DTapGate {
    private int target=-1,crystal=-1;private boolean pop,gone;
    public void begin(int target,int crystal){this.target=target;this.crystal=crystal;pop=false;gone=false;}
    public void status(int entity,byte status){if(entity==target&&target>=0&&status==35)pop=true;}
    public void removed(int entity){if(entity==crystal&&crystal>=0)gone=true;}
    public boolean ready(){return target>=0&&crystal>=0&&pop&&gone;}
    public void reset(){target=-1;crystal=-1;pop=false;gone=false;}
}
