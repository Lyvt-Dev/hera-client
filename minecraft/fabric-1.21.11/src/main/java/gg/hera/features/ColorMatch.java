package gg.hera.features;
/** Squared RGB distance; alpha is not a dye property. Stable first-match tie breaking. */
public final class ColorMatch {
 private ColorMatch(){}
 public static int nearest(int color,int[] palette){int best=-1;long distance=Long.MAX_VALUE;for(int i=0;i<palette.length;i++){int rgb=palette[i];long r=(color>>16&255)-(rgb>>16&255),g=(color>>8&255)-(rgb>>8&255),b=(color&255)-(rgb&255);long d=r*r+g*g+b*b;if(d<distance){distance=d;best=i;}}return best;}
}
