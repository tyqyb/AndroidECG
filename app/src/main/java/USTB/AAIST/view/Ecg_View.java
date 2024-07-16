/**
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
    private int baseline;//中心轴线y=height/2
    public   ArrayList<Double> refreshList = new ArrayList<Double>();//模拟心电数据
    private float nowX;
    private float nowY;//当前的X，Y坐标值
    private double max_Value = 0.3;//最大幅度值，30

/**初始化new才会调用这个方法**/
    public Ecg_View(Context context) {
        super(context);
//        System.out.println("ECG视图，刷新列表为：" +refreshList.size());//只调用了一次, refreshList.size()为0
        this.setBackgroundColor(getResources().getColor(R.color.DrawViewBackgroundColor));
    }

/**发送数据到refreshList**/
    public Ecg_View(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
       this.setBackgroundColor(getResources().getColor(R.color.DrawViewBackgroundColor));
    }

/**获取View的页面宽度高度以及小网格的宽度、基线位置y坐标值**/
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
        widthOfSmallGrid = w / (verticalBigGridLine * 100);
        baseline = h / 2;
//        System.out.println("基线"+baseline+"高"+height+"宽"+width+"小网格的宽度"+widthOfSmallGrid);
    }

/**执行画图操作，多次调用**/
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawElectrocardiogram(canvas);
//        drawWaveLine(canvas);

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

    /**绘图方法1与2的异同在于：
     *2代码较1多了 mPath.reset(); 比1少了effect = new CornerPathEffect(30); electrocarPaint.setPathEffect(effect);
     *for (int i = 0; i < refreshList.size(); i++) {中，方法2给datavalue×0.8
     *
     *主用方法1，方法2后续测试
     * **/

    /**画心电程序方法1**/
    private void drawElectrocardiogram(Canvas canvas) {
        if(refreshList == null || refreshList.size()<=0){
            return;
        }

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

        //Logcat打印数据列表
//        for (int i = 0; i < refreshList.size(); i++) {
//            double dataValue = refreshList.get(i);
//            System.out.print(dataValue+",");
//        }
//        System.out.println();
        for (int i = 0; i < refreshList.size(); i++) {
            //这里没有调用到
            double dataValue = -refreshList.get(i);
            nowX = i * widthOfSmallGrid;
            nowY = (float) (baseline - dataValue * (baseline / (max_Value *10)));
            electrocarPath.lineTo(nowX, nowY);
        }
        canvas.drawPath(electrocarPath, electrocarPaint);
        if(refreshList.size()>4000){
            refreshList.clear();
        }
//        Ecg_View.this.invalidate();
//        invalidate();
    }

    /**画心电程序方法2**/
    private void drawWaveLine(Canvas canvas) {
        if(null == refreshList || refreshList.size()<=0){
            System.out.println("RefreshList<0,ending!");
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
//        Log.d(TAG, "drawWaveLine: "+refreshList.size());
//        for (int i = 0; i < refreshList.size(); i++) {
//            double dataValue = refreshList.get(i);
//            System.out.print(dataValue+",");
//        }
//        System.out.println();

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
//            Log.d(TAG, "nowX: "+nowX+" nowY: "+nowY+" dataValue: "+dataValue);
            mPath.lineTo(nowX,nowY);
        }
        canvas.drawPath(mPath, mWavePaint);
        if(refreshList.size()>widthOfSmallGrid){
            refreshList.remove(0);
            //每秒500个包，移除，同上
//            for(int i=0;i<500;i++){
//                refreshList.remove(i);
//            }
        }
    }

}
