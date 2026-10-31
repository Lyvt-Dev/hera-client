package gg.hera.features;
/** Identity-based camera session: respawn can replace a player without replacing the world. */
public final class CameraSession {
 private Object world,player;
 public boolean matches(Object candidateWorld,Object candidatePlayer){return world!=null&&player!=null&&candidateWorld==world&&candidatePlayer==player;}
 public boolean update(Object nextWorld,Object nextPlayer,boolean alive){boolean reset=nextWorld!=world||nextPlayer!=player||nextWorld==null||nextPlayer==null||!alive;world=nextWorld;player=nextPlayer;return reset;}
}
