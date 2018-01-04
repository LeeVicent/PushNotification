package com.vicent.pushnotification.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.vicent.pushnotification.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText title;
    private EditText content;
    private NotificationManagerCompat manager;
    private int id = 1;
    private int notifID;
    private boolean isChecked;
    private boolean isCheckedBoot;
    private boolean isCheckedHideIcon;
    private boolean isCheckedHideNew;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private final List<Integer> positions = new ArrayList<>();
    private static final String TAG = "MainActivity";
    private static final String IS_CHECKED = "IS_CHECKED";
    private static final String IS_CHECKED_BOOT = "IS_CHECKED_BOOT";
    private static final String IS_CHECKED_HIDE_ICON = "IS_CHECKED_HIDE_ICON";
    private static final String IS_CHECKED_HIDE_NEW = "IS_CHECKED_HIDE_NEW";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void Test(){
        //Git测试
        int a = 10;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /**
         *如果从图标打开此活动，则传进来的 Intent 将不携带任何 Extra ，
         *若是这样，获取到的文本将都为 "" ，notifID 将产生一个新的值,即 id++
         */
        String head = intent.getStringExtra("title");
        String cont = intent.getStringExtra("content");
        notifID = intent.getIntExtra("id",id++);
        title.setText(head);
        content.setText(cont);
        Log.d(TAG, "onNewIntent: "+ head + " "+cont +" "+ notifID);
    }
}
