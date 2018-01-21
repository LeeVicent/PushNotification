package com.vicent.pushnotification.ui.activity;


import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.view.animation.*;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.graphics.drawable.Drawable;
import android.widget.Toast;


import com.vicent.pushnotification.R;

import java.util.ArrayList;
import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private RelativeLayout mainLayout;
    private AutoCompleteTextView title_auto;
    private AutoCompleteTextView content_auto;
    private ImageView alarm_iv;
    private TextView revoke_tv;
    private TextView push_tv;

    private int notifCount = 0;   //已存在通知计数
    private int egg = 0;
    private int mHour,mMinute;
    private String title_text;
    private String content_text;

    private SharedPreferences.Editor save_editor;   //SharedPreferences保存
    private SharedPreferences recover_pre;  //SharedPreferences恢复

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        setOnClickListeners();
        onNewIntent(getIntent());

        //恢复notifCount计数
        recover_pre = getSharedPreferences("data", MODE_PRIVATE);
        notifCount = recover_pre.getInt("notifCount", 0);
    }



    //监听Activity内外单击事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //如果点击位置在当前View外部则销毁当前视图
            if (!(event.getX() >= -10 && event.getY() >= -10)
                    || event.getX() >= mainLayout.getWidth() + 10  //微调
                    || event.getY() >= mainLayout.getHeight() + 20) {
                //对话框存在内容
                if (title_auto.length() != 0 || content_auto.length() != 0) {
                    contentExistDialog();
                } else {
                    finish();
                }
            }
        }
        return true;
    }


    //返回按键响应
    @Override
    public void onBackPressed() {
        //super.onBackPressed();这句话一定要注掉,不然又去调用默认的back处理方式
        if (title_auto.length() != 0 || content_auto.length() != 0) {
            contentExistDialog();
        } else {
            finish();
        }
    }



    //注意onNewIntent参数为传来的Intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        title_auto.setText(intent.getStringExtra("title_text"));
        content_auto.setText(intent.getStringExtra("content_text"));
    }

    private void findViews() {
        title_auto = (AutoCompleteTextView) findViewById(R.id.title_auto);
        content_auto = (AutoCompleteTextView) findViewById(R.id.content_auto);
        alarm_iv = (ImageView) findViewById(R.id.alarm_iv);
        revoke_tv = (TextView) findViewById(R.id.revoke_tv);
        push_tv = (TextView) findViewById(R.id.push_tv);
        mainLayout = (RelativeLayout)findViewById(R.id.mainLayout);
    }

    private void setOnClickListeners() {
        alarm_iv.setOnClickListener(this);
        revoke_tv.setOnClickListener(this);
        push_tv.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //闹钟按钮响应事件
            case R.id.alarm_iv:
                Calendar calendar = Calendar.getInstance();
                new TimePickerDialog(this,
                        // 绑定TimePickerDialog的监听器
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view,
                                                  int hourOfDay, int minute) {
                                title_auto.setText("您选择了：" + hourOfDay + "时" + minute
                                        + "分");
                            }
                        }
                        // 设置初始时间
                        , calendar.get(Calendar.HOUR_OF_DAY)
                        , calendar.get(Calendar.MINUTE)
                        // true表示采用24小时制
                        , true).show();
                break;

            //撤销按钮响应事件
            case R.id.revoke_tv:
                revokeNotifiction();
                break;

            //推送按钮响应事件
            case R.id.push_tv:
                if (getText()) {   //EditText内有内容
                    createNotification();
                    finish();
                } else {
                    noContent();
                }
                egg();
                break;
        }
    }


    //创建通知
    public void createNotification() {
        notifCount++;

        Intent intent = new Intent(this, MainActivity.class);  //单击消息意图
        intent.putExtra("title_text", title_text);
        intent.putExtra("content_text", content_text);
        //注意PendingIntent.getActivity的第二个参数，自行查阅文档
        PendingIntent pi = PendingIntent.getActivity(this, notifCount, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(title_text)
                .setContentText(content_text)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(pi)
                .build();
        manager.notify(notifCount, notification);

        //SharedPreferences形式持久化保存notifCount计数，防止Activity销毁后重新计数
        save_editor = getSharedPreferences("data", MODE_PRIVATE).edit();
        save_editor.putInt("notifCount", notifCount);
        save_editor.apply();
    }

    //撤回通知
    public void revokeNotifiction() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(1);
    }

    //断言EditText为空并取值
    public boolean getText() {
        if (title_auto.length() != 0 || content_auto.length() != 0) {
            title_text = title_auto.getText().toString();
            content_text = content_auto.getText().toString();
            Log.i(this.getClass().getName(), "不为空");
            return true;
        } else {
            Log.i(this.getClass().getName(), "为空");
            return false;
        }
    }

    //警告对话框封装
    public void contentExistDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.title_dialog);
        dialog.setMessage(R.string.message_dialog);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.positive_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        dialog.show();
    }


    //用户未输入响应
    public void noContent() {
        //内容为空提醒
        Drawable drawable = getResources().getDrawable(R.drawable.ic_create);//获取图片资源
        drawable.setBounds(0, 0, 72, 72);
        //这里R.string.error_et无法引用，原因暂时未知
        title_auto.setError(getString(R.string.error_et), drawable);
        title_auto.requestFocus();   //修改聚焦

        //抖动动画
        TranslateAnimation animation = new TranslateAnimation(5, -5, 0, 0);
        animation.setInterpolator(new OvershootInterpolator());
        animation.setDuration(30);
        animation.setRepeatCount(7);
        animation.setRepeatMode(Animation.REVERSE);
        title_auto.startAnimation(animation);

        egg++;
    }





    //彩蛋
    public void egg() {
        switch (egg) {
            case 3:
                Toast.makeText(this, R.string.first_toast, Toast.LENGTH_SHORT).show();
                break;
            case 8:
                Toast.makeText(this, R.string.second_toast, Toast.LENGTH_SHORT).show();
                break;
            case 12:
                Toast.makeText(this, R.string.third_toast, Toast.LENGTH_SHORT).show();
                break;
            case 18:
                Toast.makeText(this, R.string.fourth_toast, Toast.LENGTH_LONG).show();
                finish();
        }
    }

}



