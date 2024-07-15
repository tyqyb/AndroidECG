/**绘制波形
 * 20240711做了如下修改
 * 调整mWaveLineColor为纯黑色；解除mLinePaint.setAntiAlias(true);两句注释
 * **/

package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import java.util.ArrayList;
import USTB.AAIST.R;

public  class  WaveShowView extends View {
    private float mWidth = 0,mHeight = 0;//自身大小
    private int mBackGroundColor = Color.BLACK;
    private Paint mLinePaint;//画笔
    private Paint mWavePaint;//心电图的折现
    private Path mPath;//心电图的路径

    private ArrayList refreshList = new ArrayList();//后加的数据点
    private int row;//背景网格的行数和列数

    //心电
    private float MAX_VALUE = 7;
    private float WAVE_LINE_STROKE_WIDTH = 2;
    private int mWaveLineColor = Color.parseColor("#FF000000");//波形颜色
    private  float nowX=0.0F,nowY=0.0F;//目前的xy坐标

    //网格
    private final int GRID_SMALL_WIDTH = 2;//每一个网格的宽度和高度,包括线
    private final int GRID_BIG_WIDTH = 50;//每一个大网格的宽度和高度,包括线
    private int xSmallNum,ySmallNum,xBigNum,yBigNum;//小网格的横格，竖格，大网格的横格，竖格数量
    private final int GRID_LINE_WIDTH=2;//网格的线的宽度
//    private int mWaveSmallLineColor = Color.parseColor("#092100");//小网格颜色
//    private int mWaveBigLineColor = Color.parseColor("#1b4200");//小网格颜色

    public WaveShowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WaveShowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void init() {
//        nowX=0;
//        nowY=0;
//        refreshList.clear();
        Log.d("safsfasf","INIT");
        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(GRID_LINE_WIDTH);
        mLinePaint.setAntiAlias(true);//抗锯齿效果

        mWavePaint = new Paint();
        mWavePaint.setFakeBoldText(true);
        mWavePaint.setStyle(Paint.Style.STROKE);
        mWavePaint.setColor(mWaveLineColor);
        mWavePaint.setStrokeWidth(WAVE_LINE_STROKE_WIDTH);
        mWavePaint.setAntiAlias(true);//抗锯齿效果

        mPath = new Path();

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mWidth = getMeasuredWidth();//获取view的宽
        mHeight = getMeasuredHeight();//获取view的高

        row= (int) (mWidth/(GRID_SMALL_WIDTH));//获取行数

        //小网格
        xSmallNum = (int) (mHeight/GRID_SMALL_WIDTH);//横线个数=总高度/小网格高度

        ySmallNum = (int) (mWidth/GRID_SMALL_WIDTH);//竖线个数=总宽度/小网格宽度
        //大网格
        xBigNum = (int) (mHeight/GRID_BIG_WIDTH);//横线个数
        yBigNum = (int) (mWidth/GRID_BIG_WIDTH);//竖线个数
        System.out.println("mHeight: "+mHeight+"  mWidth "+  mWidth+"xSmallNum："+xSmallNum+" ySmallNum:"+ySmallNum+" xBigNum"+xBigNum+" yBigNum："+yBigNum+" row:"+row);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for(int i=0;i< refreshList.size();i++){
//         Log.d("sds",refreshList.get(i)+" ");
        }

        //绘制网格
        drawGrid(canvas);
        //绘制基线的
//        Paint paint = new Paint();
//        paint.setStyle(Paint.Style.STROKE);
//        paint.setColor(getResources().getColor(R.color.teal_200));
//        paint.setStrokeWidth(2.0f);
//        Path mPath = new Path();
//        mPath.moveTo(0, mHeight/2);//将画笔移至某点起笔位置
//        mPath.lineTo(mWidth, mHeight/2);//落笔位置实现一条直线绘画
//        canvas.drawPath(mPath, paint);
        //绘制波形
        drawWaveLine(canvas);
    }

    /**
     * 画折线
     * @param canvas
     */
    private void drawWaveLine(Canvas canvas) {
//        System.out.println("drawWaveLine");
//
//        System.out.println();
//        System.out.println("refreshList SIZE IS:"+refreshList.size());


        if(null == refreshList || refreshList.size()<=0){
            System.out.println("ending");
            return;
        }
        //在图像的中心去绘制点

        mPath.reset();
        mPath.moveTo(0f,mHeight/2);
        for (int i = 0;i<refreshList.size();i++){
            nowX = (float) (i/2.0);
            double dataValue = (double) refreshList.get(i);
//            if(dataValue>0){
//                if(dataValue>MAX_VALUE * 0.8){
//                    dataValue = MAX_VALUE * 0.8f;
//                }
//            }else {
//                if(dataValue< -MAX_VALUE * 0.8){
//                    dataValue = -MAX_VALUE * 0.8f;
//                }
//            }
//            nowY = (float) (mHeight/2 + dataValue *(mHeight/(MAX_VALUE*2)));
            //160.5   - 0.几的数据 *321/(10)
            nowY = (float) (mHeight/2 - dataValue *(mHeight/(MAX_VALUE)));
            //     nowY = (float) (mHeight/2 - dataValue * (mHeight / (MAX_VALUE * 2)) );

//            System.out.println("nowX:"+nowX+" nowY:"+nowY+" dataValue:"+dataValue);

            mPath.lineTo(nowX,nowY);
//            mPath.lineTo(i,(float) dataValue/10);
        }
        canvas.drawPath(mPath, mWavePaint);

        if(refreshList.size()>3500){

            for(int i=0;i<122;i++){
                refreshList.remove(i);
            }
        }
    }

    //画网格
    private void drawGrid(Canvas canvas){

        canvas.drawColor(mBackGroundColor);
        //画小网格
//        mLinePaint.setColor(mWaveSmallLineColor);
        //画横线
        for(int i = 0;i < xSmallNum + 1;i++){
            canvas.drawLine(0,i*GRID_SMALL_WIDTH,
                    mWidth, i*GRID_SMALL_WIDTH, mLinePaint);
        }
        //画竖线
        for(int i = 0;i < ySmallNum+1;i++){
            canvas.drawLine(i*GRID_SMALL_WIDTH,0,
                    i*GRID_SMALL_WIDTH,mHeight, mLinePaint);
        }

        //画大网格
//        mLinePaint.setColor(mWaveBigLineColor);
        //画横线
        for(int i = 0;i < xBigNum + 1;i++){
            canvas.drawLine(0,i*GRID_BIG_WIDTH,
                    mWidth, i*GRID_BIG_WIDTH, mLinePaint);
        }
        //画竖线
        for(int i = 0;i < yBigNum+1;i++){
            canvas.drawLine(i*GRID_BIG_WIDTH,0,
                    i*GRID_BIG_WIDTH,mHeight, mLinePaint);
        }
    }

    public void showLine(double line) {
        refreshList.add(line);

        invalidate();
    }

    //重置折现的坐标集合
    public void resetCanavas() {
        refreshList.clear();
    }
}
