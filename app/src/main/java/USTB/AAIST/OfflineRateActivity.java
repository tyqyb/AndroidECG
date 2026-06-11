package USTB.AAIST;
/**离线查看心电图**/
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import java.util.ArrayList;
import USTB.AAIST.view.ECG_allData_View;
import USTB.AAIST.view.WH_ECGView;
import android.util.Log;

public class OfflineRateActivity extends AppCompatActivity {
    private static final String TAG="In OfflineRateActivity";
    private WH_ECGView ecgView;
    private ECG_allData_View allData_view;
    private  ArrayList<Double> orginate_data_source;//原始信号
    private ArrayList<Double> data_source;//滤波信号

    //提示框输入的id通过edit传入String orginatepath=中的第二个id参数，参考的是ECG中的同样的用法
//    final EditText edit = new EditText(OfflineRateActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline_rate);
        // 隐藏标题栏，setContentView后调用
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
        ecgView = (WH_ECGView)findViewById(R.id.ecg_data_ecgView);
        allData_view = (ECG_allData_View)findViewById(R.id.allData_ecgView);
        Intent intent = getIntent();
        if ("action".equals(intent.getAction())) {
            data_source = (ArrayList<Double>) intent.getSerializableExtra("offline_ratedata");//主要绿色的键值名称要和ECGchart中的Intent键值名称一致，否则拿不到数据
            orginate_data_source= (ArrayList<Double>) intent.getSerializableExtra("offline_orginateratedata");

            Log.e(TAG, "判断Data—source:"+ data_source.size());
            ecgView.setData(data_source);
            allData_view.setData(data_source);
        }
    }
}