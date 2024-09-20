/**启动界面**/
package USTB.AAIST;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

public class SplashPage extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);//全屏显示
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        //状态栏颜色
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#107C8A"));

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
}