package com.vicent.pushnotification.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.nfc.Tag;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.vicent.pushnotification.ui.activity.MainActivity;


import com.vicent.pushnotification.R;
import com.vicent.pushnotification.util.ImageTool;

import static android.content.Context.MODE_PRIVATE;

public class SettingFragment extends PreferenceFragment {
    private Preference selectPhoto;
    private String [] itemString;
    private int index;

    public static SharedPreferences.Editor save_editor;
    public static SharedPreferences recover_pre;
    public static SharedPreferences.Editor save_editor_v;
    public static SharedPreferences recover_pre_v;

    //对话框标记
    public static final int SELF_DEFINE_DIALOG = 1;


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
        selectPhoto = (Preference) findPreference("mainActivityBgSelect");
        selectPhoto.setShouldDisableView(true);
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
        if ("bootCompleted_setting".equals(preference.getKey())) {
            MainActivity.instance.recreate();
        }
        if ("selfDefine".equals(preference.getKey())) {
            dialogs(SELF_DEFINE_DIALOG);
        }
        if ("mainActivityBg".equals(preference.getKey())) {
            MainActivity.instance.recreate();
        }
        if ("mainActivityBgSelect".equals(preference.getKey())) {
            startActivity(new Intent(instance, ImageTool.class).putExtra("op", ImageTool.SELECT_PHOTO));
        }
        if ("history".equals(preference.getKey())) {
            Toast.makeText(instance, R.string.constructing_toast, Toast.LENGTH_SHORT).show();
        }
        if ("notifBarEntrance".equals(preference.getKey())) {
            if (recover_pre_v.getBoolean("notifBarEntrance", false)) {
                notifBarEntrance();
            } else {
                NotificationManagerCompat manager = NotificationManagerCompat.from(MainActivity.instance);
                manager.cancel(1000);
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public void dialogs(int dialogNum) {
        switch (dialogNum) {
            case SELF_DEFINE_DIALOG:
                itemString = new String[11];
                itemString[0] = getString(R.string.define_text);
                itemString[1] = getString(R.string.minimalistModel_menu);
                itemString[2] = getString(R.string.autoCheck_menu);
                itemString[3] = getString(R.string.revokeAll_menu);
                itemString[4] = getString(R.string.recoverByHand_menu);
                itemString[5] = getString(R.string.changeTheme_menu);
                itemString[6] = getString(R.string.setting_menu);
                itemString[7] = getString(R.string.bootCompleted_title_setting);
                itemString[8] = getString(R.string.notifBarEntrance_title_setting);
                itemString[9] = getString(R.string.mainActivityBg_title_setting);
                itemString[10] = getString(R.string.mainActivityBgSelect_title_setting);

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


    // TODO 考虑把这个放在MainActivity里，只需要使用preference进行判断就好了
/*    public static void pushNotification(int notifID, String title_text, String content_text) {

        Intent intent = new Intent(instance, MainActivity.class);  //单击消息意图
        //注意PendingIntent.getActivity的第二个参数，自行查阅文档
        PendingIntent pi = PendingIntent.getActivity(instance, notifID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //使用兼容版本Notification 以兼容Android 8.0
        RemoteViews rv = new RemoteViews(instance.getPackageName(),R.layout.custom_notif);

        NotificationManagerCompat manager = NotificationManagerCompat.from(instance);
        NotificationCompat.Builder notificationBulider = new NotificationCompat.Builder(instance, null)
                .setSmallIcon(R.mipmap.polls_tap)
                .setContent(rv);//小图标是必须设立的
        Notification notifications = notificationBulider.build();   //这里有待验证
        manager.notify(notifID,notifications);

    }*/

  /*  public void pushNotification(int notifID, String title_text, String content_text) {

        Intent intent = new Intent(instance, MainActivity.class);  //单击消息意图
        intent.putExtra("notifID_intent", notifID);
        intent.putExtra("title_text", title_text);
        intent.putExtra("content_text", content_text);
        intent.putExtra("isPushed", true);
        //注意PendingIntent.getActivity的第二个参数，自行查阅文档
        PendingIntent pi = PendingIntent.getActivity(instance, notifID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //使用兼容版本Notification 以兼容Android 8.0
        NotificationManagerCompat manager = NotificationManagerCompat.from(instance);
        NotificationCompat.Builder notificationBulider = new NotificationCompat.Builder(instance, null)
                .setContentTitle(title_text)
                .setContentText(content_text)
                .setSmallIcon(R.mipmap.polls_tap)   //小图标是必须设立的
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.polls_tap))
                .setContentIntent(pi)
                .setOngoing(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content_text))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        Notification notifications = notificationBulider.build();   //这里有待验证
        manager.notify(notifID,notifications);

    }*/


    /**
     *  常驻通知栏   必须复制代码，考虑进程异常终止问题
     */
    public void notifBarEntrance () {
        Intent intent = new Intent(MainActivity.instance, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(MainActivity.instance, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManagerCompat manager = NotificationManagerCompat.from(MainActivity.instance);
        NotificationCompat.Builder notificationBulider = new NotificationCompat.Builder(MainActivity.instance, null)
                .setContentTitle(getString(R.string.notifBarEntrance_title))
                .setContentText(getString(R.string.notifBarEntrance_content))
                .setSmallIcon(R.mipmap.polls_tap)   //小图标是必须设立的
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_add))
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notifBarEntrance_content)))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        Notification notifications = notificationBulider.build();   //这里有待验证
        manager.notify(1000,notifications);
    }




}
