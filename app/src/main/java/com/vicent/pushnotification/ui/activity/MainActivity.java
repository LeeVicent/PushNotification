package com.vicent.pushnotification.ui.activity;


import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.vicent.pushnotification.R;

import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private AutoCompleteTextView title_auto;
    private AutoCompleteTextView content_auto;
    private ImageView alarm_iv;
    private TextView revoke_tv;
    private TextView push_tv;

    private int mHour, mMinute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        setOnClickListeners();
    }

    private void findViews(){
        title_auto = (AutoCompleteTextView) findViewById(R.id.title_auto);
        content_auto = (AutoCompleteTextView) findViewById(R.id.content_auto);
        alarm_iv = (ImageView) findViewById(R.id.alarm_iv);
        revoke_tv = (TextView) findViewById(R.id.revoke_tv);
        push_tv = (TextView) findViewById(R.id.push_tv);
    }

    private void setOnClickListeners(){
        alarm_iv.setOnClickListener(this);
        revoke_tv.setOnClickListener(this);
        push_tv.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            //闹钟按钮响应事件
            case R.id.alarm_iv:
                Calendar calendar = Calendar.getInstance();
                new TimePickerDialog( this,
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
                        ,true).show();
                break;

            //撤销按钮响应事件
            case R.id.revoke_tv:

            //推送按钮响应事件
                break;
            case R.id.push_tv:
                break;
        }
    }

    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    //创建通知
    public void createNotification() {



    }

}



