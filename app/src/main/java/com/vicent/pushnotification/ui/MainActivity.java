package com.vicent.pushnotification.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.vicent.pushnotification.R;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText title;
    private EditText content;
    private TextView revocation;
    private TextView push;
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
        findViews();
    }


    private void findViews() {
        title = (EditText)findViewById(R.id.title);
        content = (EditText)findViewById(R.id.content);
        revocation = (TextView)findViewById(R.id.revocation);
        push = (TextView)findViewById(R.id.push);
    }

    public void onRevocationClick(View view) {
        Toast.makeText(
                MainActivity.this,
                "测试消息",
                Toast.LENGTH_SHORT
        );
        revocation.setText("呵呵");
    }

    public void Test(){
        //Git测试2
        int a = 10;
    }

}
