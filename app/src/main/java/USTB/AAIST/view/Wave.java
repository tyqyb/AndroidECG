/**
 * 连接蓝牙后跳转的绘图页面
 * **/

package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import USTB.AAIST.R;

public class Wave  extends View {
    private  float gap_grid;//网格
    private int width,height;//本页面宽，高
    private int xori;//原点x坐标
    private int grid_hori,grid_ver;//横 纵向线条
    private float gap_x;//两点间横坐标间距
    private int data_num_per_grid =18; //每小格子内的数据个数
    private float y_center;//中心y值
    private float x_change;//滑动查看时，x坐标变化
    private  static float x_changed;
    private int data_num;//总的数据个数
    private float offset_x_max;//x轴最大偏移量
    private  float rect_width;//下方矩形框的宽度
    private  float rect_gap_x;//下方矩形区域心电图数据间的横纵坐标间隙
    private  float multiple_for_rect_width;//矩形区域的宽与屏幕款的比
    private ArrayList refreshList = new ArrayList();//后加的数据点

    private ArrayList<Double> data_source;
    private  static  float startX;//手指touch屏幕时候的x坐标
    private  int rech_high=80;//下方用于显示心电图的矩形区域高度

    public Wave(Context context) {
        super(context);
        this.setBackgroundColor(getResources().getColor(R.color.DrawViewBackgroundColor));
    }
    public Wave(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.setBackgroundColor(getResources().getColor(R.color.DrawViewBackgroundColor));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed){
            xori = 0;
            gap_grid = 30.0f;
            width = getWidth();
            height = getHeight();
            grid_hori = height/(int)gap_grid;
            grid_ver = width/(int)gap_grid;
            y_center = (height )/2;
            gap_x = gap_grid/data_num_per_grid/5;
//            data_num = refreshList.size();
            x_change = 0.0f ;
            x_changed = 0.0f ;
            offset_x_max = width - gap_x * data_num;
            rect_gap_x = (float) width/data_num;
            rect_width = (float) width * width/(gap_x * data_num);
            multiple_for_rect_width = (float) width/rect_width;

//            Log.d("json","本页面宽： " + width +"  高:" + height);
//            Log.e("json","两点间横坐标间距:" + gap_x + "   矩形区域两点间横坐标间距：" + rect_gap_x);
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    /**多次调用进行绘制**/
    @Override
    protected void onDraw(Canvas canvas) {
        DrawGrid(canvas);
        DrawECGWave(canvas);
        //System.out.println("本页面宽： " + width +"  高:" + height);
        //绘制水平基线
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getResources().getColor(R.color.BaseGridLineColor));//水平基线颜色
        paint.setStrokeWidth(3.0f);
        Path mPath = new Path();
        mPath.moveTo(0, y_center);//将画笔移至某点起笔位置
        mPath.lineTo(width, y_center);//落笔位置实现一条直线绘画
        canvas.drawPath(mPath, paint);
        super.onDraw(canvas);
    }

    /**画背景网格**/
    private void DrawGrid(Canvas canvas){
        //横线
        for (int i = 1 ; i < grid_hori + 2 ; i ++){
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(getResources().getColor(R.color.GridLineColor));
            paint.setStrokeWidth(1.0f);
            Path path = new Path();
            path.moveTo(xori, gap_grid * (i-1) + (height-grid_hori*gap_grid)/2);
            path.lineTo(width,gap_grid * (i-1) + (height-grid_hori*gap_grid)/2);
            if ( i % 5 != 0 ){//第五条为实线，其余为虚线，以下为画虚线方法
                PathEffect effect = new DashPathEffect(new float[]{1,5},1);
                paint.setPathEffect(effect);
            }
            canvas.drawPath(path,paint);
        }
        //竖线
        for (int i = 1 ; i < grid_ver + 2 ; i ++){
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(getResources().getColor(R.color.GridLineColor));
            paint.setStrokeWidth(1.0f);
            Path path = new Path();
            path.moveTo(gap_grid * (i-1) + (width-grid_ver*gap_grid)/2, 0);
            path.lineTo(gap_grid * (i-1) + (width-grid_ver*gap_grid)/2,height);
            if ( i % 5 != 0 ){
                PathEffect effect = new DashPathEffect(new float[]{1,5},1);
                paint.setPathEffect(effect);
            }
            canvas.drawPath(path,paint);
        }
    }

    /**画数据图**/
    private void DrawECGWave(Canvas canvas){
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getResources().getColor(R.color.DrawLineColor));//曲线颜色
        paint.setStrokeWidth(2.0f);
        Path path = new Path();
        if(null == refreshList || refreshList.size()<=0){
            System.out.println("refreshList null，ending！");//没有接收到数据或发送端停止发送时终止绘图
            return;
        }

        int iXor = 1;//此处 iXor设置为0 ，未用上
        for (int i = 1 ; i < this.refreshList.size() ; i ++){
            float nnn = xori + gap_x * i +  x_changed;
            if (nnn >= 0 ){
                iXor = i;
                path.moveTo(nnn, (float) getY_coordinate((Double) refreshList.get(i)));
                break;
            }
        }

        for (int i = iXor; i < this.refreshList.size(); i ++){
            float nnn = xori + gap_x * i +  x_changed;
            if (nnn < width + gap_x){
                //   Log.d("TAG", "DrawECGWave: "+(float) getY_coordinate((Double) refreshList.get(i))+"    xori + gap_x * i +  x_changed:"+xori + gap_x * i +  x_changed);
                path.lineTo(xori + gap_x * i +  x_changed , (float) getY_coordinate((Double) refreshList.get(i)));
            }
        }
        canvas.drawPath(path,paint);
        if(refreshList.size()>6500){
            for(int i=0;i<488;i++){
                refreshList.remove(i);
            }
        }

    }

    public void showLine(double line) {
        // System.out.print("line:"+line);
        refreshList.add(line);
        invalidate();
    }
    /**将数值转换为y坐标**/
    private double getY_coordinate(Double data){
        x_changed += x_change;
        Double y_int = data;

        y_int = (y_int)*-1;
        double y_coor = 0.0f;

        y_coor = y_int *170 + y_center;
//        System.out.println("y_int"+y_int+"y_coor:"+y_coor+"y_center:"+y_center);
        return y_coor;
    }
}
