package gg.hera.ui;
/** A navigation rail, a searchable module list and a persistent detail pane. */
public record WorkspaceLayout(int x,int y,int width,int height,int rail,int list,int detailX,int detailWidth) {
    public static WorkspaceLayout of(int width,int height){
        int w=Math.min(880,width-32),h=Math.min(492,height-32),x=(width-w)/2,y=(height-h)/2;
        int rail=132,list=238,dx=x+rail+list;
        return new WorkspaceLayout(x,y,w,h,rail,list,dx,w-rail-list);
    }
    public int listX(){return x+rail;}
    public int listY(){return y+118;}
    public int bottom(){return y+height-48;}
    public int visibleRows(){return Math.max(1,(bottom()-listY())/54);}
}
