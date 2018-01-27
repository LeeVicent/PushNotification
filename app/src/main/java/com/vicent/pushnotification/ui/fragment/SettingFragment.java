package com.vicent.pushnotification.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.widget.Toast;
import com.vicent.pushnotification.ui.activity.MainActivity;


import com.vicent.pushnotification.R;

public class SettingFragment extends PreferenceFragment {
    private CheckBoxPreference changeTheme_setting;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        findPreferences();
    }


    public void findPreferences() {
        CheckBoxPreference checkBox = (CheckBoxPreference) findPreference("select_linkage");

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if ("1".equals(preference.getKey())) {
            getActivity().recreate();    //重启当前Fragment所在Activity
            MainActivity.instance.recreate();   //重启其他界面（注意这种方法）
        }
        if ("3".equals(preference.getKey())) {
            Toast.makeText(getActivity(), "4566", Toast.LENGTH_SHORT).show();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }


}
