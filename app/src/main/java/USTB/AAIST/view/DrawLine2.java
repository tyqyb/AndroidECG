//连接蓝牙后跳转至Recdata页面用于展示数据，绘制指标2曲线
package USTB.AAIST.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;

//方法继承自DrawLine
public class DrawLine2 extends DrawLine {
    public DrawLine2(Context context) {super(context);}
    public DrawLine2(Context context, AttributeSet attrs) {super(context, attrs);}
    public DrawLine2(Context context, AttributeSet attrs, int defStyleAttr) {super(context, attrs, defStyleAttr);}

    // 可以在这里重写或添加DrawLine2特有的方法，例如，可以设置不同的颜色、样式等
    @Override
    protected void init() {
        super.init();

        // 可以在这里为DrawLine2设置不同的颜色或样式
        // 例如，将数据线颜色改为绿色
        dataPaint.setColor(Color.parseColor("#4CAF50")); // 绿色
        fillPaint.setColor(Color.parseColor("#81C784")); // 浅绿色填充
        pointPaint.setColor(Color.parseColor("#FF9800")); // 橙色点

        // 设置数据标签为指标2
        //setDataLabel("指标2");
    }
}