package com.vicent.pushnotification.backstage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.vicent.pushnotification.ui.activity.MainActivity;

import static android.content.Context.MODE_PRIVATE;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        SharedPreferences spGet = context.getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences spGet_v = context.getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE);
        SharedPreferences.Editor spSet = context.getSharedPreferences("data", MODE_PRIVATE).edit();
        if (spGet.getInt("notifCount", 0) != 0) {
            spSet.putBoolean("bootCompleted", true).apply();
        } else {
            spSet.putBoolean("bootCompleted", false).apply();
        }
        if (spGet_v.getBoolean("2", false)) {
            context.startActivity(new Intent(context, MainActivity.class));
        }
        Log.i(this.getClass().getName(), "推动通知已启动");
        //记得注释掉默认的throw
    }
}
