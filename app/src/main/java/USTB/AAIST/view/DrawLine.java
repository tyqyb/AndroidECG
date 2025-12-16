//连接蓝牙后跳转至Recdata页面用于展示数据，绘制动态曲线
package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
    private boolean autoAdjustRange = true;
    //动态数据存储
    private final Queue<DataPoint> dataQueue = new LinkedList<>();
    private int MAX_DATA_POINTS = 50; // 最大显示点数
    private float maxValue = 1f; // 初始最大值
    private float minValue = 0f;   // 初始最小值
    private String dataLabel = "指标1"; // 数据标签
    //添加队列范围跟踪变量
    private float queueMaxValue = Float.MIN_VALUE;
    private float queueMinValue = Float.MAX_VALUE;
    //添加时间管理变量
    private float lastLabelSeconds = -1; // 上次显示标签的时间（秒）
    private static final float LABEL_INTERVAL = 1.0f; // 1秒间隔
    //画笔相关变量，protected便于DrawLine2访问
    protected Paint axisPaint, gridPaint, dataPaint, pointPaint, textPaint, fillPaint;
    protected Path dataPath;
    protected Path fillPath;
    protected RectF chartRect;
    //数据统计相关变量
    private int dataCount = 0;
    private long startTime = 0;
    private ScaleGestureDetector scaleDetector;
    private boolean isScaling = false;
    private float touchStartX, touchStartY;
    private float minTouchDistance = 50f; // 最小触摸距离
    private float scaleFactor = 1.0f;
    private float baseRange = 100f; // 基础范围
    // 添加新的变量用于控制刻度显示
    private int majorGridLines = 5;  // 主网格线数量
    private int minorGridLines = 4;  // 每个主网格线之间的小网格线数量
    private boolean showMinorGrid = true;  // 是否显示小网格线

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

    protected void init() {

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

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());// 初始化手势检测器
    }

    // 设置数据线颜色
    public void setDataLineColor(int color) {
        dataPaint.setColor(color);
        invalidate();
    }

    // 设置填充颜色
    public void setFillColor(int color) {
        fillPaint.setColor(color);
        invalidate();
    }

    // 设置填充透明度 (0-255)
    public void setFillAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        invalidate();
    }

    // 设置数据点颜色
    public void setPointColor(int color) {
        pointPaint.setColor(color);
        invalidate();
    }

    // 设置所有颜色（一次设置多个）
    public void setChartColors(int dataLineColor, int fillColor, int pointColor) {
        dataPaint.setColor(dataLineColor);
        fillPaint.setColor(fillColor);
        pointPaint.setColor(pointColor);
        invalidate();
    }

    // 设置所有颜色并指定填充透明度
    public void setChartColorsWithAlpha(int dataLineColor, int fillColor, int pointColor, int fillAlpha) {
        dataPaint.setColor(dataLineColor);
        fillPaint.setColor(fillColor);
        pointPaint.setColor(pointColor);
        fillPaint.setAlpha(fillAlpha);
        invalidate();
    }

    //添加从蓝牙接收到的数据点  @param value 数据值
    public void addDataPoint(float value, float currentSeconds) {
        String label = "";

        // 只在时间间隔满足时生成标签
        if (lastLabelSeconds < 0 || currentSeconds - lastLabelSeconds >= LABEL_INTERVAL) {
            label = formatTimeLabel(currentSeconds);
            lastLabelSeconds = currentSeconds;
        }

        // 创建数据点对象
        DataPoint point = new DataPoint(value, currentSeconds, label);
        dataQueue.offer(point);
        dataCount++;

        // 更新队列极值
        if (value > queueMaxValue) queueMaxValue = value;
        if (value < queueMinValue) queueMinValue = value;

        // 维护队列大小
        if (dataQueue.size() > MAX_DATA_POINTS) {
            DataPoint removed = dataQueue.poll();
            // 如果移除的点是极值点，需要重新计算范围
            if (removed.value == queueMaxValue || removed.value == queueMinValue) {
                recalculateQueueRange();
            }
        }

        updateDataRange(value);// 更新数据范围

        invalidate();// 强制重绘
        postInvalidate();
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
    protected static class DataPoint {
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

    //更智能地调整范围
    private void updateDataRange(float value) {
        if (!autoAdjustRange || dataQueue.isEmpty()) return;

        // 第一次数据点进入时，设置基于该值的初始范围
        if (dataQueue.size() == 1) {
            // 如果第一个值为0，设置默认范围
            if (value == 0) {
                minValue = -10f;
                maxValue = 10f;
            } else {
                // 设置±120%的范围
                float margin = Math.abs(value) * 1.2f;
                minValue = value - margin;
                maxValue = value + margin;

                // 确保最小范围
                if (maxValue - minValue < 10) {
                    float center = (minValue + maxValue) / 2;
                    minValue = center - 5;
                    maxValue = center + 5;
                }
            }
            return;
        }

        recalculateQueueRange();// 重新计算队列中的极值

        // 如果队列中只有一个数据点，或者所有点值相同
        if (queueMaxValue == queueMinValue) {
            if (queueMaxValue == 0) {
                minValue = -10;
                maxValue = 10;
            } else {
                float delta = Math.abs(queueMaxValue) * 1.2f; // 使用120%范围
                minValue = queueMinValue - delta;
                maxValue = queueMaxValue + delta;
            }
            return;
        }

        // 计算当前数据范围
        float currentRange = queueMaxValue - queueMinValue;

        // 添加120%的边距
        float margin = currentRange * 1.2f;
        float newMin = queueMinValue - margin;
        float newMax = queueMaxValue + margin;

        // 确保最小值合理
        if (newMin < 0 && queueMinValue >= 0) {
            newMin = 0;
        }

        // 平滑过渡（只在新范围变化较大时更新）
        float rangeChange = Math.abs(newMax - maxValue) + Math.abs(newMin - minValue);
        if (rangeChange > currentRange * 0.3f) { // 变化超过30%才更新
            float transitionFactor = 0.3f; // 30%的过渡
            minValue = minValue + (newMin - minValue) * transitionFactor;
            maxValue = maxValue + (newMax - maxValue) * transitionFactor;
        }

        // 确保有效范围
        if (maxValue - minValue < 1.0f) {
            float center = (minValue + maxValue) / 2;
            minValue = center - 0.5f;
            maxValue = center + 0.5f;
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

    //曲线及网格绘制
    private void drawGrid(Canvas canvas) {
        // 绘制网格线
        int totalMajorLines = majorGridLines;
        float majorGridSpacing = chartRect.height() / totalMajorLines;

        // 使用浮点数列表存储已显示的标签值，避免重复
        List<Float> drawnYValues = new ArrayList<>();
        float lastDrawnY = -1000; // 记录上次绘制的Y轴位置
        float labelSpacingThreshold = 40f; // Y轴标签最小间距(像素)

        // 绘制小网格线（浅灰色细线）
        if (showMinorGrid && totalMajorLines > 0) {
            Paint minorGridPaint = new Paint(gridPaint);
            minorGridPaint.setColor(Color.parseColor("#E0E0E0"));
            minorGridPaint.setStrokeWidth(0.5f);

            for (int i = 0; i <= totalMajorLines * (minorGridLines + 1); i++) {
                float y = chartRect.bottom - i * (majorGridSpacing / (minorGridLines + 1));
                if (y >= chartRect.top && y <= chartRect.bottom) {
                    // 跳过主网格线位置（会单独绘制）
                    if (i % (minorGridLines + 1) != 0) {
                        canvas.drawLine(chartRect.left, y, chartRect.right, y, minorGridPaint);
                    }
                }
            }
        }

        // 绘制主网格线和Y轴标签
        for (int i = 0; i <= totalMajorLines; i++) {
            float y = chartRect.bottom - i * majorGridSpacing;

            // 绘制主网格线
            if (i > 0 && i < totalMajorLines) { // 不绘制顶部和底部的网格线
                canvas.drawLine(chartRect.left, y, chartRect.right, y, gridPaint);
            }

            float value = minValue + (maxValue - minValue) * i / totalMajorLines;// 计算对应的数值

            // 格式化标签，根据范围决定显示小数位数
            String label;
            float range = maxValue - minValue;

            if (range < 0.1) {
                // 非常小的范围，显示4位小数
                label = String.format(Locale.getDefault(), "%.4f", value);
            } else if (range < 1) {
                // 小范围，显示3位小数
                label = String.format(Locale.getDefault(), "%.3f", value);
            } else if (range < 10) {
                // 中等范围，显示2位小数
                label = String.format(Locale.getDefault(), "%.2f", value);
            } else if (range < 100) {
                // 较大范围，显示1位小数
                label = String.format(Locale.getDefault(), "%.1f", value);
            } else {
                // 大范围，显示整数
                label = String.format(Locale.getDefault(), "%.0f", value);
            }

            // 检查标签是否与上一个太近
            boolean shouldDrawLabel = true;
            for (float drawnValue : drawnYValues) {
                if (Math.abs(y - drawnValue) < labelSpacingThreshold) {
                    shouldDrawLabel = false;
                    break;
                }
            }

            // 检查数值是否与已显示的标签值太接近（避免数值上的重复）
            boolean valueTooClose = false;
            for (float drawnYVal : drawnYValues) {
                if (Math.abs(y - drawnYVal) < labelSpacingThreshold) {
                    valueTooClose = true;
                    break;
                }
            }

            // 绘制Y轴标签
            if (shouldDrawLabel && !valueTooClose) {
                // 使用辅助文本画笔绘制标签
                Paint labelPaint = new Paint(textPaint);
                labelPaint.setTextSize(28f);
                labelPaint.setTextAlign(Paint.Align.RIGHT);
                labelPaint.setColor(Color.parseColor("#455A64")); // 深灰色

                canvas.drawText(label, chartRect.left - 15, y + 10, labelPaint);// 在Y轴左侧绘制数值标签

                drawnYValues.add(y);// 记录已绘制的标签位置
                lastDrawnY = y;
            }

            // 在主刻度位置绘制更粗的刻度标记
            Paint tickPaint = new Paint(axisPaint);
            tickPaint.setStrokeWidth(2f);
            canvas.drawLine(chartRect.left - 10, y, chartRect.left, y, tickPaint);
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
                if (!TextUtils.isEmpty(point.label) && !drawnTimes.contains(point.label)) {
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

    // 设置是否显示小网格线
    public void setShowMinorGrid(boolean show) {
        this.showMinorGrid = show;
        invalidate();
    }

    // 设置网格线数量
    public void setGridLines(int majorLines, int minorLines) {
        this.majorGridLines = majorLines;
        this.minorGridLines = minorLines;
        invalidate();
    }

    private void drawData(Canvas canvas) {
        if (dataQueue.isEmpty()) return;

        // 重置路径
        dataPath.reset();
        fillPath.reset();

        List<DataPoint> dataList = new ArrayList<>(dataQueue);

        // 计算x轴间距（注意处理只有一个点的情况）
        float xSpacing;
        if (dataList.size() > 1) {
            xSpacing = chartRect.width() / (dataList.size() - 1);
        } else {
            xSpacing = chartRect.width(); // 只有一个点时，放在中间
        }

        // 计算数据范围，避免在循环中重复计算
        float dataRange = maxValue - minValue;
        if (dataRange == 0) {
            dataRange = 1.0f; // 避免除零
        }

        boolean firstPoint = true;

        for (int i = 0; i < dataList.size(); i++) {
            DataPoint point = dataList.get(i);

            // 计算x坐标
            float x;
            if (dataList.size() > 1) {
                x = chartRect.left + i * xSpacing;
            } else {
                x = chartRect.left + chartRect.width() / 2; // 只有一个点时放在中间
            }

            // 修复：计算y坐标，需要将数据值映射到图表区域
            // 公式修正：chartRect.top + (1 - normalizedValue) * chartRect.height()
            float normalizedValue = (point.value - minValue) / dataRange;
            normalizedValue = Math.max(0, Math.min(1, normalizedValue)); // 限制在0-1之间

            float y = chartRect.top + (1 - normalizedValue) * chartRect.height(); // 修正y坐标计算：确保数据值越大，在图表上位置越高（屏幕坐标向下为正）
            y = Math.max(chartRect.top, Math.min(chartRect.bottom, y));// 确保y在图表区域内

            // 绘制数据点（只绘制部分点，避免性能问题）
            if (i % 5 == 0 || i == dataList.size() - 1) {
                canvas.drawCircle(x, y, 8, pointPaint);
            }

            // 创建折线路径
            if (firstPoint) {
                dataPath.moveTo(x, y);
                fillPath.moveTo(x, chartRect.bottom);
                fillPath.lineTo(x, y);
                firstPoint = false;
            } else {
                dataPath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        // 闭合填充路径
        if (!firstPoint) { // 确保至少有一个点
            fillPath.lineTo(chartRect.right, chartRect.bottom);
            fillPath.lineTo(chartRect.left, chartRect.bottom);
            fillPath.close();

            // 绘制填充区域和折线
            canvas.drawPath(fillPath, fillPaint);
            canvas.drawPath(dataPath, dataPaint);
        }

        // 绘制最新值标签
        if (!dataList.isEmpty()) {
            DataPoint lastPoint = dataList.get(dataList.size() - 1);
            float lastX;
            if (dataList.size() > 1) {
                lastX = chartRect.left + (dataList.size() - 1) * xSpacing;
            } else {
                lastX = chartRect.left + chartRect.width() / 2;
            }

            // 使用相同的公式计算最后一个点的y坐标
            float normalizedValue = (lastPoint.value - minValue) / dataRange;
            normalizedValue = Math.max(0, Math.min(1, normalizedValue));
            float lastY = chartRect.top + (1 - normalizedValue) * chartRect.height();
            lastY = Math.max(chartRect.top, Math.min(chartRect.bottom, lastY));

            Paint valuePaint = new Paint(textPaint);
            valuePaint.setTextSize(36f);
            valuePaint.setColor(Color.parseColor("#FF5722"));
            valuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            String valueText = String.format(Locale.getDefault(), "%.1f", lastPoint.value);// 显示当前值
            canvas.drawText(valueText, lastX, lastY - 30, valuePaint);// 签放在点的上方
        }
    }

    // 添加ScaleListener手势监测内部类
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            scaleFactor = Math.max(0.5f, Math.min(scaleFactor, 2.0f));// 限制缩放范围

            // 只调整最大值，保持最小值不变
            float currentRange = maxValue - minValue;
            float newRange = currentRange * scaleFactor;

            // 确保最小范围
            if (newRange < 1.0f) {
                newRange = 1.0f;
            }

            maxValue = minValue + newRange;// 只调整最大值

            // 缩放时禁用自动调整
            autoAdjustRange = false;
            postInvalidate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            isScaling = true;
            baseRange = maxValue - minValue;// 记录当前范围作为基准
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            isScaling = false;
            postInvalidate();
        }
    }

    // 手指触摸方法方法
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchStartX = event.getX();
                touchStartY = event.getY();
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // 双指触摸开始
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isScaling && event.getPointerCount() == 2) {
                    // 双指移动时开始缩放
                    scaleDetector.onTouchEvent(event);
                } else if (event.getPointerCount() == 1 && !isScaling) {
                    // 单指滑动调整范围：只调整最大值，保持最小值不变
                    float deltaY = event.getY() - touchStartY;
                    if (Math.abs(deltaY) > minTouchDistance) {
                        // 计算调整量（基于当前范围的百分比）
                        float currentRange = maxValue - minValue;
                        float rangeAdjust = currentRange * 0.1f;

                        if (deltaY > 0) {
                            // 向下滑动，减小最大值（范围变小）
                            maxValue = maxValue - rangeAdjust;
                            // 确保最大值始终大于最小值
                            if (maxValue <= minValue + 0.1f) {
                                maxValue = minValue + 0.1f;
                            }
                        } else {
                            // 向上滑动，增加最大值（范围变大）
                            maxValue = maxValue + rangeAdjust;
                            // 限制最大范围
                            if (maxValue > minValue + currentRange * 5) {
                                maxValue = minValue + currentRange * 5;
                            }
                        }

                        touchStartY = event.getY();
                        autoAdjustRange = false; // 手动调整后禁用自动调整
                        postInvalidate();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                isScaling = false;
                break;
        }
        return true;
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

    // 添加重置缩放的方法
    public void resetZoom() {
        scaleFactor = 1.0f;
        autoAdjustRange = true;
        postInvalidate();
    }

    //设置数据标签    @param label 标签文本
    public void setDataLabel(String label) {
        this.dataLabel = label;
    }

}
