//连接蓝牙后跳转至Recdata页面用于展示数据，方法继承自DrawLine
package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

public class DrawLine2 extends DrawLine {

    public DrawLine2(Context context) {
        super(context);
        initDrawLine2Colors();
    }
    public DrawLine2(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDrawLine2Colors();
    }
    public DrawLine2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDrawLine2Colors();
    }

    // 设置颜色方案
    private void initDrawLine2Colors() {
        // 方法1：使用setChartColorsWithAlpha设置所有颜色和透明度
        setChartColorsWithAlpha(
                Color.parseColor("#4CAF50"),     // 数据线颜色：绿色
                Color.parseColor("#81C784"),     // 填充颜色：浅绿色
                Color.parseColor("#FF9800"),     // 数据点颜色：橙色
                80                               // 填充透明度：80（范围0-255，值越小越透明）
        );

        // 或者使用方法2：分别设置各个颜色
        /*
        setDataLineColor(Color.parseColor("#4CAF50"));      // 绿色数据线
        setFillColor(Color.parseColor("#81C784"));          // 浅绿色填充
        setPointColor(Color.parseColor("#FF9800"));         // 橙色数据点
        setFillAlpha(80);                                   // 填充透明度80
        */
    }

}