package gg.hera.ui;
/** Layout in scaled GUI coordinates, shared by drawing, hit boxes and pagination. */
public record ModuleLayout(int x,int y,int width,int height,int side,int contentX,int contentWidth,int gridY,int columns,int rows,int cardWidth,int cardHeight) {
    public static ModuleLayout of(int screenWidth,int screenHeight) {
        int w=Math.max(220,Math.min(570,screenWidth-32)),h=Math.max(160,Math.min(350,screenHeight-32));
        int x=(screenWidth-w)/2,y=(screenHeight-h)/2,side=w>=480?96:78;
        int cx=x+side+16,cw=w-side-32,columns=cw>=280?2:1;
        int cardHeight=h>=300?56:50,gy=y+74;
        int rows=Math.max(1,(h-(gy-y)-40+8)/(cardHeight+8));
        return new ModuleLayout(x,y,w,h,side,cx,cw,gy,columns,rows,(cw-(columns-1)*8)/columns,cardHeight);
    }
    public int pageSize(){return columns*rows;}
    public int maxPage(int count){return Math.max(0,(count-1)/pageSize());}
}
