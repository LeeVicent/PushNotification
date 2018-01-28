package com.vicent.pushnotification.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;

import com.vicent.pushnotification.R;
import com.vicent.pushnotification.ui.fragment.SettingFragment;


/**
 * Created by LeeVicent on 2018/1/26.
 */

public class SettingActivity extends AppCompatActivity{
    private SharedPreferences.Editor save_editor;
    private SharedPreferences recover_pre_v;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        save_editor = getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE).edit();
        recover_pre_v = getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE);


        if (recover_pre_v.getBoolean("changeTheme_setting", false) ) {
            setTheme(R.style.OtherActivityTheme_Night);  //主题设定必须位于setContentView()之前调用
            if (recover_pre_v.getBoolean("actionBar_setting", false) ) {
                Window window = getWindow();
                //取消设置透明状态栏,使 ContentView 内容不再覆盖状态栏
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                //需要设置这个 flag 才能调用 setStatusBarColor 来设置状态栏颜色
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                //设置状态栏颜色
                window.setStatusBarColor(getResources().getColor(R.color.grey_2));
            }
        } else {
            if (recover_pre_v.getBoolean("actionBar_setting", false) ) {
                Window window = getWindow();
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(getResources().getColor(R.color.light_grey1));
            }
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //布局替换
        getFragmentManager().beginTransaction().replace(R.id.setting_layout, new SettingFragment()).commit();
    }





}
