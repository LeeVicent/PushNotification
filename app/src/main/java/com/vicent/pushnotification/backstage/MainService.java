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
            spSet.putBoolean("interrupt", true).apply();
            Log.i(this.getClass().getName(), "异常终止");
        } else {
            spSet.putBoolean("interrupt", false).apply();
        }
        Log.i(this.getClass().getName(), "进程检测服务已初始化");
    }



    //对于国产Rom，低优先级服务很有可能会被系统回收（省电优化，而不仅仅是内存不足）
    //这里存在一个问题，也就是进程会被优化掉（kill），再次启动时进程号会更换，但是这种清理方式不一会清理通知
    //任务管理器清理可能会彻底清理进程，并且清理所有通知（一加会，小米/魅族不会）取决于Rom实现
    //通知携带的Intent依然被保留着
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (flags == START_NOT_STICKY) {

        }

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
