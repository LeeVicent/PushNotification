package com.vicent.pushnotification.ui.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import com.vicent.pushnotification.R;
import com.vicent.pushnotification.ui.fragment.SettingFragment;


/**
 * Created by LeeVicent on 2018/1/26.
 */

public class SettingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //布局替换
        getFragmentManager().beginTransaction().replace(R.id.setting_layout, new SettingFragment()).commit();

    }
}
