package gg.hera.features;
/** Bounded pixel canvas with gap-free Bresenham strokes. */
public final class CrosshairCanvas {
 public static final int SIZE=32;private final int[] pixels=new int[SIZE*SIZE];
 public int[] copy(){return pixels.clone();}public int get(int x,int y){return pixels[y*SIZE+x];}
 public void restore(int[] values){if(values.length!=pixels.length)throw new IllegalArgumentException();System.arraycopy(values,0,pixels,0,pixels.length);}
 public void clear(){java.util.Arrays.fill(pixels,0);}
 public void line(int x,int y,int ex,int ey,int color){x=Math.clamp(x,0,SIZE-1);y=Math.clamp(y,0,SIZE-1);ex=Math.clamp(ex,0,SIZE-1);ey=Math.clamp(ey,0,SIZE-1);int dx=Math.abs(ex-x),sx=x<ex?1:-1,dy=-Math.abs(ey-y),sy=y<ey?1:-1,error=dx+dy;while(true){pixels[y*SIZE+x]=color;if(x==ex&&y==ey)break;int e=2*error;if(e>=dy){error+=dy;x+=sx;}if(e<=dx){error+=dx;y+=sy;}}}
}
