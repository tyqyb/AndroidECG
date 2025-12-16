//绘图曲线颜色方法类

package USTB.AAIST.utils;

import android.graphics.Color;

public class ChartColors {

        // DrawLine1（血糖图表）颜色方案
        public static final int GLU_LINE_COLOR = Color.parseColor("#2196F3");      // 蓝色
        public static final int GLU_FILL_COLOR = Color.parseColor("#64B5F6");      // 浅蓝色
        public static final int GLU_POINT_COLOR = Color.parseColor("#FF9800");     // 橙色
        public static final int GLU_FILL_ALPHA = 60;                               // 填充透明度

        // DrawLine2（尿酸图表）颜色方案
        public static final int URI_LINE_COLOR = Color.parseColor("#4CAF50");      // 绿色
        public static final int URI_FILL_COLOR = Color.parseColor("#81C784");      // 浅绿色
        public static final int URI_POINT_COLOR = Color.parseColor("#FF9800");     // 橙色
        public static final int URI_FILL_ALPHA = 80;                               // 填充透明度

        // 网格和坐标轴颜色
        public static final int AXIS_COLOR = Color.parseColor("#37474F");
        public static final int GRID_COLOR = Color.parseColor("#B0BEC5");
        public static final int MINOR_GRID_COLOR = Color.parseColor("#E0E0E0");
        public static final int TEXT_COLOR = Color.parseColor("#455A64");

        // 特殊状态颜色
        public static final int WARNING_COLOR = Color.parseColor("#FF5722");       // 警告色（红色）
        public static final int NORMAL_COLOR = Color.parseColor("#4CAF50");        // 正常色（绿色）
        public static final int THRESHOLD_COLOR = Color.parseColor("#FFC107");     // 阈值色（黄色）

}
