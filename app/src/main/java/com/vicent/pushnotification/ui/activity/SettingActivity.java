package com.vicent.pushnotification.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.vicent.pushnotification.R;
import com.vicent.pushnotification.ui.fragment.SettingFragment;


/**
 * Created by LeeVicent on 2018/1/26.
 */

public class SettingActivity extends AppCompatActivity implements View.OnClickListener{
    private SharedPreferences.Editor save_editor;
    private SharedPreferences recover_pre_v;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        save_editor = getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE).edit();
        recover_pre_v = getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE);


        if (recover_pre_v.getBoolean("1", false) ) {
            setTheme(R.style.OtherActivityTheme_Night);  //主题设定必须位于setContentView()之前调用
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //布局替换
        getFragmentManager().beginTransaction().replace(R.id.setting_layout, new SettingFragment()).commit();
       /* findViews();*/
    }

  /*  private void findViews() {
        changeTheme_setting = ;
    }*/

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        }
    }



}
