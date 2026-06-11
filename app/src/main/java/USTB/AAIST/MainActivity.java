package USTB.AAIST;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private LinearLayout cECG_Layout;
    private LinearLayout Recdata_Layout;
    private MyBluetoothManager myBluetoothManager;// 使用单例管理蓝牙在页面跳转时的状态传递
    // 页面类型常量
    public static final int PAGE_ECG = 1;
    public static final int PAGE_RECDATA = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myBluetoothManager = MyBluetoothManager.getInstance(getApplicationContext());
        // 隐藏标题栏
        if (getSupportActionBar()!=null){
            getSupportActionBar().hide();
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        initUI();
        handleIntent(getIntent());// 处理从BLE页面返回的意图
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    //用户意图处理
    private void handleIntent(Intent intent) {
        if (intent != null) {
            // 检查是否有目标页面需要跳转
            if (intent.hasExtra("target_page")) {
                int targetPage = intent.getIntExtra("target_page", -1);
                // isDeviceConnected()，不仅检查连接状态，还检查设备地址的有效性
                if (targetPage != -1 && MyBluetoothManager.getInstance(getApplicationContext()).isDeviceConnected()) {
                    navigateToTargetPage(targetPage);
                }
            }
        }
    }

    //初始化控件并设置点击事件
    private void initUI() {
        cECG_Layout = findViewById(R.id.cECG_Layout);
        Recdata_Layout = findViewById(R.id.recdataid);

        cECG_Layout.setOnClickListener(view -> navigateToTargetPage(PAGE_ECG));// 心电绘制点击事件
        Recdata_Layout.setOnClickListener(view -> navigateToTargetPage(PAGE_RECDATA));// 数据接收点击事件
    }

    //导航到目标页面
    private void navigateToTargetPage(int pageType) {
        if (myBluetoothManager.isConnected()) {
            Intent intent = new Intent();
            switch (pageType) {
                case PAGE_ECG:
                    intent.setClass(this, ECGChart.class);
                    break;
                case PAGE_RECDATA:
                    intent.setClass(this, Recdata.class);
                    intent.putExtra("DEVICE_ADDRESS", myBluetoothManager.getDeviceAddress());
                    break;
            }
            startActivity(intent);
        } else {
            Toast.makeText(this, "请先连接蓝牙设备", Toast.LENGTH_SHORT).show();
            Intent bleIntent = new Intent(this, BLE.class);
            bleIntent.putExtra("target_page", pageType);
            startActivity(bleIntent);
        }
    }

    //菜单事件处理
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.ble) {
            Intent bleIntent = new Intent(this, BLE.class);
            startActivity(bleIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = new MenuInflater(this);
        //menuInflater.inflate(R.menu.blemenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
}