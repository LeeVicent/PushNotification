package com.vicent.pushnotification.backstage;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.vicent.pushnotification.ui.activity.MainActivity;

public class MainService extends Service {


    private MyBinder myBinder = new MyBinder();

    class MyBinder extends Binder {
        public boolean get() {
            return false;
        }
    }



    //Activity的onCreate()等执行完成后才执行Service的onCreate()
    //所以没法在Services里检测上次运行状况，因为Activity已执行完成
    //考虑多线程等待0.5秒后MainActivity才开始读取标识位
    @Override
    public void onCreate() {
        super.onCreate();
        MainActivity m = new MainActivity();
        SharedPreferences spGet = getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences.Editor spSet = getSharedPreferences("data", MODE_PRIVATE).edit();
        if (spGet.getInt("notifCount", 0) != 0) {
            /*MainActivity.isAbnormalTermination = true;*/
            spSet.putBoolean("interrupt", true).apply();
            Log.i(this.getClass().getName(), "异常终止");
            Log.i(this.getClass().getName(), "Service      " + Boolean.toString(spGet.getBoolean("interrupt", true)));
        } else {
            spSet.putBoolean("interrupt", false).apply();
        }
        Log.i(this.getClass().getName(), "进程检测服务已初始化");
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(this.getClass().getName(), "进程监测服务已启动");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(this.getClass().getName(), "服务终止！");
        super.onDestroy();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.

        return myBinder;

    }
}
