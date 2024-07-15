package USTB.AAIST;
//启动界面
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import android.os.Build;

/**
 * BUG日志
 * 当软件打开上级返回到splash页面后，再次进入软件卡停在该页面，
 * 只能通过杀掉后台进程重新进入，跳转函数应该重写
 * 20240701解决该bug，对应res->anim->fade_in与fade_out两个函数
 **/

public class SplashPage extends AppCompatActivity {
    //新页面跳转逻辑20240701
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        //全屏显示
        getSupportActionBar().hide();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_splash), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().setStatusBarColor(0x00FFFFFF);
//        }
//
//        WindowInsetsControllerCompat windowInsetsController =
//                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
//        if (windowInsetsController != null) {
//            windowInsetsController.setAppearanceLightStatusBars(false);
//        }

        //底部状态栏的颜色
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            getWindow().setNavigationBarColor(0xFFFFFFFF);
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            getWindow().setNavigationBarContrastEnforced(false);
//        }
        new Handler().postDelayed(() -> {
            startActivities(new Intent[]{new Intent().setClass(this, MainActivity.class)});
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }, 1000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }

//原页面跳转逻辑
//    private Handler mHandler=new Handler();
//    private  int count=2;//延时2s
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_splash);
//
//        //全屏显示
//        getSupportActionBar().hide();
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        mHandler.post(runnable);//延迟函数调用
//    }
//
//    //延迟函数，进入主界面
//    private Runnable runnable  = new Runnable() {
//        @Override
//        public void run() {
//            mHandler.postDelayed(this,1000);
//            count--;
//            if(count==0){
//                Intent intent =new Intent();
//                intent.setClass(SplashPage.this,MainActivity.class);
//                startActivity(intent);
//            }
//        }
//
//    };

}