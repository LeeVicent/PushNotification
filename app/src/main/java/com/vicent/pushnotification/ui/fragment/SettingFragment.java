package com.vicent.pushnotification.ui.fragment;

import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import com.vicent.pushnotification.R;

public class SettingFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
