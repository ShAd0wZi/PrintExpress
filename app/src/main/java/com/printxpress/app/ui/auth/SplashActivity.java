package com.printxpress.app.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.printxpress.app.R;
import com.printxpress.app.ui.home.HomeActivity;

public class SplashActivity extends AppCompatActivity {

    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private final Runnable navigateRunnable = () -> {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Intent intent;
        if (com.printxpress.app.util.SessionManager.isLoggedIn(SplashActivity.this)
                && com.printxpress.app.util.SessionManager.isAdmin(SplashActivity.this)) {
            intent = new Intent(SplashActivity.this, com.printxpress.app.ui.admin.AdminDashboardActivity.class);
        } else {
            // Deferred Authentication: Always allow guests/customers to browse the home and products.
            intent = new Intent(SplashActivity.this, HomeActivity.class);
        }
        startActivity(intent);
        finish();
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        splashHandler.postDelayed(navigateRunnable, 2000); // 2 seconds splash
    }

    @Override
    protected void onDestroy() {
        splashHandler.removeCallbacks(navigateRunnable);
        super.onDestroy();
    }
}