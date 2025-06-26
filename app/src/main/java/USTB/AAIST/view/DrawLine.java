//连接蓝牙后跳转至Recdata页面用于展示数据，绘制指标1曲线
package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.HashSet;
import java.util.List;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;
import android.graphics.Typeface;

public class DrawLine extends View {
    //动态调整纵轴范围
    private static final float RANGE_ADJUST_THRESHOLD = 0.8f;
    private boolean autoAdjustRange = true;
    //动态数据存储
    private final Queue<DataPoint> dataQueue = new LinkedList<>();
    private final Queue<String> labelQueue = new LinkedList<>();
    private int MAX_DATA_POINTS = 50; // 最大显示点数
    private float maxValue = 100f; // 初始最大值
    private float minValue = 0f;   // 初始最小值
    private String dataLabel = "指标1"; // 数据标签
    //添加队列范围跟踪变量
    private float queueMaxValue = Float.MIN_VALUE;
    private float queueMinValue = Float.MAX_VALUE;
    //添加时间管理变量
    private float lastLabelSeconds = -1; // 上次显示标签的时间（秒）
    private static final float LABEL_INTERVAL = 1.0f; // 1秒间隔
    //画笔相关变量
    private Paint axisPaint, gridPaint, dataPaint, pointPaint, textPaint, fillPaint;
    private Path dataPath;
    private Path fillPath;
    private RectF chartRect;
    //数据统计相关变量
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

    //添加从蓝牙接收到的数据点  @param value 数据值
    public void addDataPoint(float value, float currentSeconds) {
        String label = "";

        // 只在时间间隔满足时生成标签
        if (lastLabelSeconds < 0 || currentSeconds - lastLabelSeconds >= LABEL_INTERVAL) {
            // 格式化为整数秒（如 1s, 2s）
            label = formatTimeLabel(currentSeconds);
            lastLabelSeconds = currentSeconds;
        }

        // 创建数据点对象（包含值、时间和标签）
        DataPoint point = new DataPoint(value, currentSeconds, label);
        dataQueue.offer(point);
        dataCount++;

        // 维护队列大小
        if (dataQueue.size() > MAX_DATA_POINTS) {
            DataPoint removed = dataQueue.poll();
            // 如果移除的点是极值点，需要重新计算范围
            if (removed.value == queueMaxValue || removed.value == queueMinValue) {
                recalculateQueueRange();
            }
        }

        // 更新队列极值
        if (value > queueMaxValue) queueMaxValue = value;
        if (value < queueMinValue) queueMinValue = value;

        updateDataRange(value);// 更新数据范围
        postInvalidate();// 重绘视图
    }

    // 重新计算整个队列的范围
    private void recalculateQueueRange() {
        queueMaxValue = Float.MIN_VALUE;
        queueMinValue = Float.MAX_VALUE;

        for (DataPoint point : dataQueue) {
            float value = point.value;
            if (value > queueMaxValue) queueMaxValue = value;
            if (value < queueMinValue) queueMinValue = value;
        }
    }

    // DataPoint内部类
    private static class DataPoint {
        float value;
        float time;
        String label;

        DataPoint(float value, float time, String label) {
            this.value = value;
            this.time = time;
            this.label = label;
        }
    }

    //时间格式化方法
    private String formatTimeLabel(float totalSeconds) {
        int seconds = (int) totalSeconds;

        if (seconds < 60) {
            return String.format(Locale.getDefault(), "%ds", seconds);// 小于60秒：显示秒数
        } else if (seconds < 3600) {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return String.format(Locale.getDefault(), "%dm%02ds", minutes, remainingSeconds);// 60秒-1小时：显示分钟和秒
        } else {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            int remainingSeconds = seconds % 60;
            return String.format(Locale.getDefault(), "%dh%02dm%02ds", hours, minutes, remainingSeconds);// 1小时以上：显示小时、分钟和秒
        }
    }

    private void updateDataRange(float value) {
        if (!autoAdjustRange || dataQueue.isEmpty()) return;

        // 处理所有点值相同的情况
        if (queueMaxValue == queueMinValue) {
            if (queueMaxValue == 0) {
                minValue = -1;
                maxValue = 1;
            } else {
                float delta = Math.abs(queueMaxValue) * 0.2f; // 20% of the value
                minValue = queueMinValue - delta;
                maxValue = queueMaxValue + delta;
            }
            return;
        }

        // 计算当前数据范围
        float currentRange = queueMaxValue - queueMinValue;

        // 添加15%的缓冲区域
        float margin = currentRange * 0.15f;
        float newMin = queueMinValue - margin;
        float newMax = queueMaxValue + margin;

        // 确保最小值不为负数（如果数据都是非负）
        if (newMin < 0 && queueMinValue >= 0) {
            newMin = 0;
        }

        // 应用平滑过渡（避免范围跳动）
        float transitionFactor = 0.3f; // 30%的过渡
        minValue = minValue + (newMin - minValue) * transitionFactor;
        maxValue = maxValue + (newMax - maxValue) * transitionFactor;

        // 确保有效范围
        float minRange = Math.max(currentRange * 0.5f, 10f); // 最小范围为10或当前范围的50%
        if (maxValue - minValue < minRange) {
            float center = (minValue + maxValue) / 2;
            minValue = center - minRange / 2;
            maxValue = center + minRange / 2;
        }
    }

    //设置最大数据点数  @param maxPoints 最大点数
    public void setMaxPoints(int maxPoints) {
        this.MAX_DATA_POINTS = maxPoints;
    }

    //图表大小设置
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 设置默认尺寸
        int defaultWidth = 600;
        int defaultHeight = 400;

        int width = resolveSize(defaultWidth, widthMeasureSpec);
        int height = resolveSize(defaultHeight, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    //留出图表边距
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 计算图表绘制区域（留出边距）
        int padding = 80;
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
        bgPaint.setColor(Color.parseColor("#FFFFFF"));
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

            // 根据不同的数据范围设置y的精度
            String label;
            float range = maxValue - minValue;
            if (range > 1000) {
                label = String.format(Locale.getDefault(), "%.0f", value);
            } else if (range > 100) {
                label = String.format(Locale.getDefault(), "%.0f", value);//原%.1f
            } else {
                label = String.format(Locale.getDefault(), "%.0f", value);//原%.2f
            }
            // 使用辅助文本画笔绘制标签
            Paint labelPaint = new Paint(textPaint);
            labelPaint.setTextSize(28f);
            labelPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(label, chartRect.left - 10, y + 10, labelPaint);
        }

        canvas.drawLine(chartRect.left, chartRect.bottom, chartRect.right, chartRect.bottom, axisPaint);// 绘制X轴
        canvas.drawLine(chartRect.left, chartRect.top, chartRect.left, chartRect.bottom, axisPaint);// 绘制Y轴

        // 绘制X轴标签（只显示部分标签）
        if (!dataQueue.isEmpty()) {
            List<DataPoint> dataList = new ArrayList<>(dataQueue);
            float xSpacing = chartRect.width() / (dataList.size() - 1);

            // 记录已绘制的标签值，避免重复
            Set<String> drawnTimes = new HashSet<>();
            float lastDrawnX = -1000; // 记录上次绘制的位置
            for (int i = 0; i < dataList.size(); i++) {
                DataPoint point = dataList.get(i);

                // 只绘制非空标签且未绘制过的时间点
                if (!TextUtils.isEmpty(point.label) && !drawnTimes.contains(point.time)) {
                    float x = chartRect.left + i * xSpacing;

                    // 确保标签不会重叠
                    if (Math.abs(x - lastDrawnX) < 100) { // 100像素是最小间距
                        continue; // 跳过太近的标签
                    }
                    Paint labelPaint = new Paint(textPaint);
                    labelPaint.setTextSize(30f);
                    labelPaint.setTextAlign(Paint.Align.CENTER);

                    canvas.drawText(point.label, x, chartRect.bottom + 40, labelPaint);// 绘制标签

                    drawnTimes.add(point.label);// 标记已绘制
                    lastDrawnX = x; // 更新最后绘制位置
                }
            }
        }
    }

    private void drawData(Canvas canvas) {
        if (dataQueue.isEmpty()) return;

        // 重置路径
        dataPath.reset();
        fillPath.reset();

        List<DataPoint> dataList = new ArrayList<>(dataQueue);
        float xSpacing = chartRect.width() / (dataList.size() - 1);

        for (int i = 0; i < dataList.size(); i++) {
            DataPoint point = dataList.get(i);
            float x = chartRect.left + i * xSpacing;
            float y = chartRect.bottom - ((point.value - minValue) / (maxValue - minValue)) * chartRect.height();
            y = Math.min(chartRect.bottom, Math.max(chartRect.top, y));

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

        canvas.drawPath(fillPath, fillPaint);// 绘制填充区域
        canvas.drawPath(dataPath, dataPaint);// 绘制折线

        // 绘制最新值
        if (!dataList.isEmpty()) {
            float lastValue = dataList.get(dataList.size() - 1).value;
            float lastX = chartRect.right;
            float lastY = chartRect.bottom - ((lastValue - minValue) / (maxValue - minValue)) * chartRect.height();

            Paint valuePaint = new Paint(textPaint);
            valuePaint.setTextSize(36f);
            valuePaint.setColor(Color.parseColor("#FF5722"));
            valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            //canvas.drawText(String.format(Locale.getDefault(), "%.1f", lastValue), lastX - 40, lastY - 20, valuePaint);//绘制数据值标签，但没必要
        }
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

    // 获取当前数据点数量
    public int getDataCount() {
        return dataCount;
    }

    // 获取当前数据队列的值（浮点数列表）
    public List<Float> getDataPoints() {
        List<Float> values = new ArrayList<>();
        for (DataPoint point : dataQueue) {
            values.add(point.value);
        }
        return values;
    }

    // 如果需要获取完整的数据点对象，可以添加这个方法
    public List<DataPoint> getDataPointObjects() {
        return new ArrayList<>(dataQueue);
    }

    //清空所有数据
    public void clearData() {
        dataQueue.clear();
        //labelQueue.clear();
        dataCount = 0;
        maxValue = 100f;
        minValue = 0f;
        queueMaxValue = Float.MIN_VALUE;
        queueMinValue = Float.MAX_VALUE;
        startTime = System.currentTimeMillis();
        lastLabelSeconds = -1; // 重置时间标签
        postInvalidate();
    }

    //设置数据标签    @param label 标签文本
    public void setDataLabel(String label) {
        this.dataLabel = label;
    }

    //绘制图表标题，没用到噢
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
}
