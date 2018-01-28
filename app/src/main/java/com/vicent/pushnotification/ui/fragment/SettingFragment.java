package com.vicent.pushnotification.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.Toast;

import com.vicent.pushnotification.ui.activity.MainActivity;


import com.vicent.pushnotification.R;

import static android.content.Context.MODE_PRIVATE;

public class SettingFragment extends PreferenceFragment {
    private CheckBoxPreference changeTheme_setting;
    private String [] itemString;
    private int index;

    public static SharedPreferences.Editor save_editor;
    public static SharedPreferences recover_pre;
    public static SharedPreferences.Editor save_editor_v;
    public static SharedPreferences recover_pre_v;

    //对话框标记
    public static final int SELF_DEFINE_DIALOG = 1;

    //图标标记
    public static final int IC_FAVOURITE = 10;
    public static final int IC_VISIVILITY_OFF = 11;
    public static final int IC_ALL_INCLUSIVE = 12;
    public static final int IC_CLEAR_ALL = 13;
    public static final int IC_BUILD = 14;
    public static final int IC_BRIGHTNESS_6 = 15;
    public static final int IC_SETTINGS = 16;
    public static final int IC_POWER_SETTINGS_NEW = 17;

    public static Activity instance = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        save_editor = getActivity().getSharedPreferences("data", MODE_PRIVATE).edit();
        save_editor_v = getActivity().getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE).edit();
        recover_pre = getActivity().getSharedPreferences("data", MODE_PRIVATE);
        recover_pre_v = getActivity().getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE);

        addPreferencesFromResource(R.xml.preferences);
        findPreferences();
        instance = getActivity();
    }


    public void findPreferences() {
        changeTheme_setting = (CheckBoxPreference) findPreference("select_linkage");

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if ("changeTheme_setting".equals(preference.getKey())) {
            instance.recreate();    //重启当前Fragment所在Activity
            MainActivity.instance.recreate();   //重启其他界面（注意这种方法）
        }
        if ("actionBar_setting".equals(preference.getKey())) {
            instance.recreate();
        }
        if ("selfDefine".equals(preference.getKey())) {
          dialogs(SELF_DEFINE_DIALOG);
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void dialogs(int dialogNum) {
        switch (dialogNum) {
            case SELF_DEFINE_DIALOG:
                itemString = new String[8];
                itemString[0] = getString(R.string.define_text);
                itemString[1] = getString(R.string.minimalistModel_menu);
                itemString[2] = getString(R.string.autoCheck_menu);
                itemString[3] = getString(R.string.revokeAll_menu);
                itemString[4] = getString(R.string.recoverByHand_menu);
                itemString[5] = getString(R.string.changeTheme_menu);
                itemString[6] = getString(R.string.setting_menu);
                itemString[7] = getString(R.string.bootCompleted_title_setting);

                index = recover_pre.getInt("selfDefine", 0) - 10;

                SpannableStringBuilder titleDialog = new SpannableStringBuilder(getString(R.string.title_SelfDefine_dialog));
                titleDialog.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.green)), 0, getString(R.string.title_SelfDefine_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                AlertDialog.Builder dialog;
                if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
                    dialog = new AlertDialog.Builder(instance, R.style.AlertDialog);
                } else {
                    dialog = new AlertDialog.Builder(instance);
                }
                dialog.setTitle(titleDialog);
                dialog.setSingleChoiceItems(itemString, index, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        index = which;
                        save_editor.putInt("selfDefine", index + 10).apply();
                        MainActivity.instance.recreate();
                        dialog.cancel();
                    }
                });
                dialog.show();

        }

    }

}
