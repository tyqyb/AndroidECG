package USTB.AAIST;
// 横屏可视化展示数据
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import java.util.List;
import USTB.AAIST.utils.DataFormatUtil;

public class ChartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);
        Button button = findViewById(R.id.btn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<Double> list = DataFormatUtil.getRes();
                for(int i=0;i<list.size();i++){
                    System.out.println(list.get(i) +",");
                }
            }
        });
    }
}