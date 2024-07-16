/**呼吸波数据展示**/
package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import USTB.AAIST.R;

public class RespiratoryAllDataView extends View {
    private int width,height;//本页面宽，高
    private ArrayList<Double> data_source;
    private int data_num;//总的数据个数
    private float rect_gap_x;//下方矩形区域心电图数据间的横坐标间隙
    private float rectY_center;//下方矩形区域心电图的中心Y值

    public RespiratoryAllDataView(Context context, AttributeSet attrs){
        super(context,attrs);
        this.setBackgroundColor(getResources().getColor(R.color.trans));//透明背景色
    }
    public RespiratoryAllDataView(Context context){
        super(context);
        this.setBackgroundColor(getResources().getColor(R.color.trans));//透明背景色
    }

/**获取View的页面宽度高度以及小网格的宽度、基线位置y坐标值**/
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed){
            width = getWidth();
            height = getHeight();
            data_num = data_source.size();
            rect_gap_x = (float) width/data_num;
            rectY_center = (float)height/2;
//            Log.v("json","两点间横坐标间距:" + gap_x + "矩形区域两点间横坐标间距：" + rect_gap_x);
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        DrawAllData(canvas);
    }
/**画下方矩形区域的心电图**/
    private void DrawAllData(Canvas canvas){
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getResources().getColor(R.color.DrawLineColor));
        paint.setStrokeWidth(1.0f);
        Path path = new Path();
        path.moveTo((float) 0, (float) getRectY_coordinate(data_source.get(0)));
        for (int i = 1 ; i < this.data_source.size() ; i ++){
            path.lineTo(rect_gap_x * i, (float) getRectY_coordinate(data_source.get(i)));
        }
        canvas.drawPath(path,paint);
    }
/**将数值转换为y坐标，下方矩形显示呼吸波的区域**/
    private double getRectY_coordinate(double data){
        double y_int = data;
        y_int = (y_int - 2048) *(-1);
        double y_coor = 0.0f;
        y_coor = y_int/8 + rectY_center;
//        Log.v("json","<rectY_center> " + rectY_center + " < y_coor >" + y_coor +"  height:" + height +" rect_hight " + rect_high);
        System.out.println("y_coor:"+y_coor/6);
        return y_coor/2;
    }

/**暴露接口，设置数据源**/
    public void setData(ArrayList<Double> data){
        this.data_source = data;
    }
}
