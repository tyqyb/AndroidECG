/**ECG的保存视图
 *与WH_ECGView相似
 * **/

package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import USTB.AAIST.R;

public class Ecg_View extends View {
    private int horizontalBigGridLine = 8, verticalBigGridLine = 8;//设置大网格实线个数
    private  String TAG="Ecg_View";
    private int width;//背景页面宽度
    private int height;//背景页面高度
    private int widthOfSmallGrid;//小网格的宽度
    private int baseline;//中心y轴线y=height/2

    public   ArrayList<Double> refreshList = new ArrayList<Double>();//模拟心电数据
    private float nowX;
    private float nowY;//当前的X，Y坐标值
    private double max_Value = 0.3;//最大幅度值
//private double max_Value = 30;//最大幅度值

//    private float mWidth = 0,mHeight = 0;//自身大小
//    private int row;//背景网格的行数和列数
//    //绘制网格，已被下面代码中的绘制替代
//    private final int GRID_SMALL_WIDTH = 10;//每一个网格的宽度和高度,包括线
//    private float MAX_VALUE = 20;
//    public void showLine(double line) {
//        refreshList.add(line);
//        for(int i=0;i<refreshList.size();i++){
//            System.out.print(refreshList.get(i)+",");
//        }
//        postInvalidate();
//    }


   //初始化new才会调用这个方法
    public Ecg_View(Context context) {
        super(context);
        //只调用了一次  refreshList.size()为0
        System.out.println("ECG视图，刷新列表为：" +refreshList.size());
//        this.setBackgroundColor(getResources().getColor(R.color.blue));
    }

    //发送随机数据到refreshList
    public Ecg_View(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
       this.setBackgroundColor(getResources().getColor(R.color.DrawViewBackgroundColor));
    }

    //获取View的页面宽度高度以及小网格的宽度、基线位置y坐标值
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        widthOfSmallGrid = w / (verticalBigGridLine * 100);
        baseline = h / 2;
         System.out.println("基线"+baseline+"高"+height+"宽"+width+"小网格的宽度"+widthOfSmallGrid);

    }

    //执行画图操作
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //这里多次调用
//        Log.d("onDraw","this.refreshList size:"+this.refreshList.size());
//        System.out.println("onDraw  in  refrshList size "+refreshList.size());
        drawElectrocardiogram(canvas);
        drawWaveLine(canvas);
        //绘制基线
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getResources().getColor(R.color.BaseGridLineColor));
        paint.setStrokeWidth(2.0f);
        Path mPath = new Path();
        mPath.moveTo(0, baseline);//将画笔移至某点起笔位置
        mPath.lineTo(width, baseline);//落笔位置实现一条直线绘画
        canvas.drawPath(mPath, paint);
    }

    //画心电
    private void drawElectrocardiogram(Canvas canvas) {
//        Log.d("drawElectrocardiogram","this.refreshList size:"+refreshList.size());//日志打印功能同下
        System.out.println("画心电程序，刷新列表的大小 "+refreshList.size());
        Path electrocarPath = new Path();
        Paint electrocarPaint = new Paint();

        CornerPathEffect effect = new CornerPathEffect(30);
        electrocarPaint.setPathEffect(effect);
    //这里多次调用
//        System.out.println();
//        System.out.print(refreshList.size()+"refreshList.size()");
        electrocarPaint.setColor(getResources().getColor(R.color.DrawLineColor));
        electrocarPaint.setStyle(Paint.Style.STROKE);
        electrocarPaint.setAntiAlias(true);
        electrocarPaint.setStrokeWidth(2);
        electrocarPath.moveTo(0, baseline);
        for (int i = 0; i < refreshList.size(); i++) {
            double dataValue = refreshList.get(i);
            System.out.print(dataValue+",");
        }
        System.out.println();

        for (int i = 0; i < refreshList.size(); i++) {
            //这里没有调用到
//            System.out.print(refreshList.get(i)+",");
            //widthOfSmallGrid :2   baseline 132
            //-0.086625
            double dataValue = -refreshList.get(i);

            nowX = i * widthOfSmallGrid;
            nowY = (float) (baseline - dataValue * (baseline / (max_Value *10)));
//            nowY = (float) (dataValue * (baseline / (max_Value *10))- baseline );
//            Log.d("drawElectrocardiogram", "nowx:"+nowX +"   nowY:"+nowY+" dataValue:"+dataValue);
            electrocarPath.lineTo(nowX, nowY);

        }
        canvas.drawPath(electrocarPath, electrocarPaint);
        if(refreshList.size()>4000){
            refreshList.clear();
        }

        //20240711解除下面两行注释
//        Ecg_View.this.invalidate();
//        invalidate();

    }

    //画心电折线，参数canvas
    private void drawWaveLine(Canvas canvas) {
        if(null == refreshList || refreshList.size()<=0){
            return;
        }
        Path mPath =new Path();
        Paint mWavePaint = new Paint();
        mWavePaint.setColor(getResources().getColor(R.color.DrawLineColor));
        mWavePaint.setStyle(Paint.Style.STROKE);
        mWavePaint.setAntiAlias(true);
        mWavePaint.setStrokeWidth(2);
        mPath.reset();
        mPath.moveTo(0f,height/2);
        Log.d(TAG, "drawWaveLine: "+refreshList.size());
        for (int i = 0; i < refreshList.size(); i++) {
            double dataValue = refreshList.get(i);
            System.out.print(dataValue+",");
        }
        System.out.println();

        for (int i = 0;i<refreshList.size();i++){
            nowX = i* widthOfSmallGrid;
            double dataValue =  refreshList.get(i);

            if(dataValue>0){
                if(dataValue>max_Value * 0.8){
                    dataValue = max_Value * 0.8f;
                }
            }else {
                if(dataValue< -max_Value * 0.8){
                    dataValue = -max_Value * 0.8f;
                }
            }
            nowY = (float) (height/2 + dataValue *(height/(max_Value*2)));
            Log.d(TAG, "nowX: "+nowX+" nowY: "+nowY+" dataValue: "+dataValue);
            //nowX:672.0 nowY:74.868126 dataValue:-0.15912500000000002
            mPath.lineTo(nowX,nowY);
        }
        canvas.drawPath(mPath, mWavePaint);
        if(refreshList.size()>widthOfSmallGrid){
            refreshList.remove(0);
            //500个写入，同上
//            for(int i=0;i<500;i++){
//                refreshList.remove(i);
//            }
        }
    }

}
