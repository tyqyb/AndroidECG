/**
 * 连接蓝牙后跳转至Recdata页面用于展示数据，曲线绘图，绘制指标1
 **/
package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.List;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import android.graphics.Typeface;

public class DrawLine extends View {
    //动态调整纵轴范围
    private static final float RANGE_ADJUST_THRESHOLD = 0.8f;
    private boolean autoAdjustRange = true;
    // 动态数据存储
    private final Queue<Float> dataQueue = new LinkedList<>();
    private final Queue<String> labelQueue = new LinkedList<>();
    private int MAX_DATA_POINTS = 50; // 最大显示点数
    private float maxValue = 100f; // 初始最大值
    private float minValue = 0f;   // 初始最小值
    private String dataLabel = "指标"; // 数据标签

    //横轴标签相关
    private long lastLabelTime = 0;
    private static final long LABEL_INTERVAL = 1000; // 1秒间隔

    //画笔
    private Paint axisPaint, gridPaint, dataPaint, pointPaint, textPaint, fillPaint;
    private Path dataPath;
    private Path fillPath;
    private RectF chartRect;

    // 数据统计
    private int dataCount = 0;
    private long startTime = 0;
    private long lastUpdateTime = 0;
    private final int UPDATE_THRESHOLD = 100; // 100ms更新间隔

    public DrawLine(Context context) {
        super(context);
        init();
    }

    public DrawLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DrawLine(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    //调整纵轴范围
    public void setAutoAdjustRange(boolean enabled) {
        this.autoAdjustRange = enabled;
    }

    public void setAutoAdjustEnabled(boolean enabled) {
        this.autoAdjustRange = enabled;
    }

    private void init() {

        // 坐标轴画笔
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#37474F"));
        axisPaint.setStrokeWidth(3f);

        // 网格线画笔
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#B0BEC5"));
        gridPaint.setStrokeWidth(1f);

        // 数据线画笔
        dataPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dataPaint.setColor(Color.parseColor("#2196F3"));
        dataPaint.setStyle(Paint.Style.STROKE);
        dataPaint.setStrokeWidth(4f);

        // 数据点画笔
        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.parseColor("#FF9800"));
        pointPaint.setStyle(Paint.Style.FILL);

        // 文本画笔
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.parseColor("#37474F"));
        textPaint.setTextSize(32f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        // 填充画笔
        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setColor(Color.parseColor("#64B5F6"));
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAlpha(100);

        // 创建路径
        dataPath = new Path();
        fillPath = new Path();
        chartRect = new RectF();
    }

    /**
     * 添加从蓝牙接收到的数据点
     * @param value 数据值
     */
    public void addDataPoint(float value) {
        // 生成时间标签 (相对时间)
        long currentTime = System.currentTimeMillis();
        long elapsedTime = System.currentTimeMillis() - startTime;
        float seconds = elapsedTime / 1000.0f;
        String label = String.format(Locale.getDefault(), "%.1fs", seconds);

        // 添加新数据
        dataQueue.offer(value);
        labelQueue.offer(label);// 可能为null
        dataCount++;

        // 维护队列大小
        if (dataQueue.size() > MAX_DATA_POINTS) {
            dataQueue.poll();
            labelQueue.poll();
        }
        updateDataRange(value);// 更新数据范围
        postInvalidate();// 重绘视图
    }

    private void updateDataRange(float value) {
        if (!autoAdjustRange) return;
        float currentRange = maxValue - minValue;// 计算当前数据范围

        // 更智能的范围调整算法
        float buffer = (maxValue - minValue) * 0.2f; // 20%缓冲区域
        if (value > maxValue - buffer) {
            maxValue = value + buffer;
        } else if (value < minValue + buffer) {
            minValue = Math.max(0, value - buffer);
        }

        // 防止数据点超出视图
        if (value > maxValue) maxValue = value;
        if (value < minValue) minValue = value;

        // 限制最大范围防止异常值
        if (maxValue > Float.MAX_VALUE / 2) {
            maxValue = 1000;
            minValue = 0;
        }

        // 如果数据范围过小，设置合理的最小范围
        if (maxValue - minValue < 10) {
            maxValue = minValue + 10;
        }
    }


    /**
     * 清空所有数据
     */
    public void clearData() {
        dataQueue.clear();
        labelQueue.clear();
        dataCount = 0;
        maxValue = 100f;
        minValue = 0f;
        startTime = System.currentTimeMillis();
        postInvalidate();
    }

    /**
     * 设置数据标签
     * @param label 标签文本
     */
    public void setDataLabel(String label) {
        this.dataLabel = label;
    }

    /**
     * 设置最大数据点数
     * @param maxPoints 最大点数
     */
    public void setMaxPoints(int maxPoints) {
        this.MAX_DATA_POINTS = maxPoints;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 设置默认尺寸
        int defaultWidth = 600;
        int defaultHeight = 400;

        int width = resolveSize(defaultWidth, widthMeasureSpec);
        int height = resolveSize(defaultHeight, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // 计算图表绘制区域（留出边距）
        int padding = 60;
        chartRect.set(padding, padding, w - padding, h - padding);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawBackground(canvas);// 绘制背景
        drawGrid(canvas);// 绘制网格和坐标轴
        drawData(canvas);// 绘制数据点和折线
    }

    private void drawBackground(Canvas canvas) {
        // 绘制浅灰色背景
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#FAFAFA"));
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        // 绘制白色图表区域
        Paint chartBgPaint = new Paint();
        chartBgPaint.setColor(Color.WHITE);
        chartBgPaint.setStyle(Paint.Style.FILL);
        chartBgPaint.setShadowLayer(10, 0, 5, Color.parseColor("#FAFAFA"));
        setLayerType(LAYER_TYPE_SOFTWARE, chartBgPaint);
        canvas.drawRoundRect(
                chartRect.left - 10,
                chartRect.top - 10,
                chartRect.right + 10,
                chartRect.bottom + 10,
                20, 20, chartBgPaint
        );
    }

    private void drawGrid(Canvas canvas) {
        // 绘制网格线
        int gridLines = 5;
        float gridSpacing = chartRect.height() / gridLines;

        // 修改Y轴标签格式 - 使用整数格式
        for (int i = 0; i <= gridLines; i++) {
            float y = chartRect.bottom - i * gridSpacing;
            float value = minValue + (maxValue - minValue) * i / gridLines;

            // 使用整数格式避免科学计数法
            String label;
            if (Math.abs(value) > 1000 || (Math.abs(value) < 0.001 && value != 0)) {
                label = String.format(Locale.getDefault(), "%.1f", value);
            } else {
                label = String.format(Locale.getDefault(), "%d", (int)value);
            }

            // 使用辅助文本画笔绘制标签
            Paint labelPaint = new Paint(textPaint);
            labelPaint.setTextSize(28f);
            labelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(label, chartRect.left - 10, y + 10, labelPaint);
        }

        // 绘制X轴
        canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.right, chartRect.bottom, axisPaint);

        // 绘制Y轴
        canvas.drawLine(chartRect.left, chartRect.top, chartRect.left, chartRect.bottom, axisPaint);

        // 绘制X轴标签（只显示部分标签）
        if (!dataQueue.isEmpty()) {
            List<Float> dataList = new ArrayList<>(dataQueue);
            List<String> labelList = new ArrayList<>(labelQueue);

            float xSpacing = chartRect.width() / (dataList.size() - 1);
            int step = Math.max(1, dataList.size() / 10); // 最多显示10个标签

            for (int i = 0; i < dataList.size(); i++) {
                if (i % step == 0 || i == dataList.size() - 1) {
                    float x = chartRect.left + i * xSpacing;
                    String label = labelList.get(i);

                    Paint labelPaint = new Paint(textPaint);
                    labelPaint.setTextSize(30f);
                    labelPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(label, x, chartRect.bottom + 40, labelPaint);
                }
            }
        }
    }

    private void drawData(Canvas canvas) {
        if (dataQueue.isEmpty()) return;

        // 重置路径
        dataPath.reset();
        fillPath.reset();

        List<Float> dataList = new ArrayList<>(dataQueue);
        float xSpacing = chartRect.width() / (dataList.size() - 1);

        for (int i = 0; i < dataList.size(); i++) {
            float x = chartRect.left + i * xSpacing;
            float y = chartRect.bottom - ((dataList.get(i) - minValue) / (maxValue - minValue)) * chartRect.height();
            y = Math.min(chartRect.bottom, Math.max(chartRect.top, y));// 确保数据点在可见范围内

            // 绘制数据点（只绘制部分点，避免性能问题）
            if (i % 5 == 0 || i == dataList.size() - 1) {
                canvas.drawCircle(x, y, 8, pointPaint);
            }

            // 创建折线路径
            if (i == 0) {
                dataPath.moveTo(x, y);
                fillPath.moveTo(x, chartRect.bottom);
                fillPath.lineTo(x, y);
            } else {
                dataPath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        // 闭合填充路径
        fillPath.lineTo(chartRect.right, chartRect.bottom);
        fillPath.lineTo(chartRect.left, chartRect.bottom);
        fillPath.close();

        // 绘制填充区域
        canvas.drawPath(fillPath, fillPaint);

        // 绘制折线
        canvas.drawPath(dataPath, dataPaint);

        // 绘制最新值
        if (!dataList.isEmpty()) {
            float lastValue = dataList.get(dataList.size() - 1);
            float lastX = chartRect.right;
            float lastY = chartRect.bottom - ((lastValue - minValue) / (maxValue - minValue)) * chartRect.height();

            Paint valuePaint = new Paint(textPaint);
            valuePaint.setTextSize(36f);
            valuePaint.setColor(Color.parseColor("#FF5722"));
            valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", lastValue), lastX - 40, lastY - 20, valuePaint);        }
    }


    //添加设置范围的方法
    public void setYRange(float min, float max, boolean reset) {
        if (reset) {
            minValue = min;
            maxValue = max;
        } else {
            minValue = Math.min(minValue, min);
            maxValue = Math.max(maxValue, max);
        }

        // 确保有效范围
        if (maxValue - minValue < 1) {
            maxValue = minValue + 1;
        }

        postInvalidate();
    }

    private void drawTitleAndStats(Canvas canvas) {
        Paint titlePaint = new Paint(textPaint);
        titlePaint.setTextSize(36f);
        titlePaint.setColor(Color.parseColor("#37474F"));
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        //String title = "蓝牙数据实时监测";
       //canvas.drawText(title, getWidth() / 2, 40, titlePaint);

        // 绘制统计信息
        Paint statsPaint = new Paint(textPaint);
        statsPaint.setTextSize(28f);
        statsPaint.setColor(Color.parseColor("#78909C"));

        String stats = String.format("数据点数: %d | 范围: %.1f-%.1f", dataQueue.size(), minValue, maxValue);
        canvas.drawText(stats, getWidth() / 2, 80, statsPaint);
    }

    // 获取当前数据点数量
    public int getDataCount() {
        return dataCount;
    }

    // 获取当前数据队列
    public List<Float> getDataPoints() {
        return new ArrayList<>(dataQueue);
    }
}
