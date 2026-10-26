package gg.hera.ui;
/** Layout in scaled GUI coordinates, shared by drawing, hit boxes and pagination. */
public record ModuleLayout(int x,int y,int width,int height,int side,int contentX,int contentWidth,int gridY,int columns,int rows,int cardWidth,int cardHeight) {
    public static ModuleLayout of(int screenWidth,int screenHeight) {
        int w=Math.max(220,Math.min(700,screenWidth-24)),h=Math.max(160,Math.min(450,screenHeight-24));
        int x=(screenWidth-w)/2,y=(screenHeight-h)/2,side=w>=540?116:82;
        int cx=x+side+16,cw=w-side-32,columns=cw>=440?3:cw>=250?2:1;
        int cardHeight=h>=300?72:58,gy=y+(h>=300?94:76);
        int rows=Math.max(1,(h-(gy-y)-40+8)/(cardHeight+8));
        return new ModuleLayout(x,y,w,h,side,cx,cw,gy,columns,rows,(cw-(columns-1)*8)/columns,cardHeight);
    }
    public int pageSize(){return columns*rows;}
    public int maxPage(int count){return Math.max(0,(count-1)/pageSize());}
}
