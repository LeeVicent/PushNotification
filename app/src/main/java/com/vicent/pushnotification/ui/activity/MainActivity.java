package com.vicent.pushnotification.ui.activity;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import android.view.animation.*;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.graphics.drawable.Drawable;
import android.widget.Toast;

import com.vicent.pushnotification.backstage.MainService;
import com.vicent.pushnotification.util.DatabaseHelper;

import com.vicent.pushnotification.R;
import com.vicent.pushnotification.util.ImageTool;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private RelativeLayout mainLayout;
    private AutoCompleteTextView title_auto;
    private AutoCompleteTextView content_auto;
    private ImageView alarm_iv;
    private TextView revoke_tv;
    private TextView push_tv;

    private int notifID = 0;
    private int notifID_intent = 0;
    private int notifCount = 0;   //已存在通知计数
    private int egg = 0, revoke_egg = 0;
    private int mHour,mMinute;
    private boolean isPushed = false;  //断言是否已通知，可用于标记当前Activity状态
    private String title_text;
    private String content_text;

    public static SharedPreferences.Editor save_editor;   //SharedPreferences保存
    public static SharedPreferences recover_pre;  //SharedPreferences恢复
    public static SharedPreferences.Editor save_editor_v;
    public static SharedPreferences recover_pre_v;

    private SpannableStringBuilder title;

    private DatabaseHelper dbHelper;  //数据库
    private SQLiteDatabase db;

    //恢复Dialog使用
    private boolean[] bools;
    private SpannableStringBuilder[] items;
    private int[] idArr;   //保存数据库所有id

    //使用Android提供的Handel执行异步操作
    private Handler handler;

    public static MainActivity instance = null;   //用于刷新

    //多线程标记
    public static final int WAIT_SERVICE_START = 1;  //等待主服务启动（多线程）
    public static final int COUNT_DOWN = 5;   //倒计时
    public static final int TIMING = 6;    //定时

    //图标标记
    public static final int IC_FAVOURITE = 10;
    public static final int IC_VISIBILITY_OFF = 11;
    public static final int IC_ALL_INCLUSIVE = 12;
    public static final int IC_CLEAR_ALL = 13;
    public static final int IC_BUILD = 14;
    public static final int IC_BRIGHTNESS_6 = 15;
    public static final int IC_SETTINGS = 16;
    public static final int IC_POWER_SETTINGS_NEW = 17;
    public static final int IC_NOTE_ADD = 18;
    public static final int IC_PHOTO = 19;
    public static final int IC_PHOTO_ALBUM = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;    //用于其他页面访问
        save_editor = getSharedPreferences("data", MODE_PRIVATE).edit();
        save_editor_v = getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE).edit();
        recover_pre = getSharedPreferences("data", MODE_PRIVATE);
        recover_pre_v = getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE);

        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
              setTheme(R.style.MainActivityTheme_Night);  //主题设定必须位于setContentView()之前调用
        }
        setContentView(R.layout.activity_main);
        findViews();
        setOnClickListeners();
        onNewIntent(getIntent());
        dbCreate();

        //恢复notifCount计数
        notifCount = recover_pre.getInt("notifCount", 0);
        notifID = recover_pre.getInt("notifID", 0);
        idArr = new int[notifCount];

        dbSelect();   //填写数组ID
        startService(new Intent(this, MainService.class));
        isInterrupt();

        bootCompleted();
        selfDefineIcon();
        setBg();
        Log.i(this.getClass().getName(), "MainActivity 初始化已完成");

    }


    //从通知点击进来
    //注意onNewIntent参数为传来的Intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        title_auto.setText(intent.getStringExtra("title_text"));
        content_auto.setText(intent.getStringExtra("content_text"));
        notifID_intent = intent.getIntExtra("notifID_intent", 0);
        isPushed = intent.getBooleanExtra("isPushed", false);

        title_text = intent.getStringExtra("title_text");
        content_text = intent.getStringExtra("content_text");
        if (isPushed) {
            revoke_tv.setTextColor(getResources().getColor(R.color.red));
            revoke_tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));  //加粗
        }

        title = new SpannableStringBuilder(getString(R.string.title_dialog));
        title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.red)), 0, getString(R.string.title_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, getString(R.string.title_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

    }

    //返回按键响应
    @Override
    public void onBackPressed() {
        //super.onBackPressed();   //super调用默认的back处理方式（销毁Activity）
        if (title_auto.length() != 0 || content_auto.length() != 0) {
            if (isPushed) {  //当前Activity处于修改状态（通知进来）
                if (title_auto.getText().toString().equals(title_text)
                        && content_auto.getText().toString().equals(content_text)) {  //检测用户是否修改
                    finish();
                } else {
                    modifDialog();
                }

            } else {  //当前Activity处于初次编辑状态（启动器进来）
                contentExistDialog();
            }
        } else {
            finish();
        }
    }


    private void findViews() {
        title_auto = (AutoCompleteTextView) findViewById(R.id.title_auto);
        content_auto = (AutoCompleteTextView) findViewById(R.id.content_auto);
        alarm_iv = (ImageView) findViewById(R.id.alarm_iv);
        push_tv = (TextView) findViewById(R.id.push_tv);
        mainLayout = (RelativeLayout)findViewById(R.id.mainLayout);

        //至于为什么增加一个按钮，，，，，，
        if (recover_pre.getBoolean("minimalistModel", false)) {
            alarm_iv.setVisibility(View.GONE);
            revoke_tv = (TextView) findViewById(R.id.revoke1_tv);
            findViewById(R.id.revoke_tv).setVisibility(View.GONE);
        } else {
            revoke_tv = (TextView) findViewById(R.id.revoke_tv);
            findViewById(R.id.revoke1_tv).setVisibility(View.GONE);
        }


        //以下代码提供换行
        title_auto.setSingleLine(false);
        title_auto.setHorizontalScrollBarEnabled(false);
        content_auto.setSingleLine(false);
        content_auto.setHorizontalScrollBarEnabled(false);
    }

    private void setOnClickListeners() {
        alarm_iv.setOnClickListener(this);
        revoke_tv.setOnClickListener(this);
        push_tv.setOnClickListener(this);
    }


    //菜单项    考虑使用PopupWindow
    public void moreVertOnClick(View view) {
       /* Context context = getBaseContext();
        context.setTheme(R.style.popMenu_style);*/
        PopupMenu popup = new PopupMenu(MainActivity.this, view);

        final MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_more, popup.getMenu());

        if (recover_pre.getBoolean("minimalistModel", false)) {
            popup.getMenu().findItem(R.id.autoCheck).setVisible(false);
            popup.getMenu().findItem(R.id.recoverByHand).setVisible(false);
            popup.getMenu().findItem(R.id.setting).setVisible(false);
            popup.getMenu().findItem(R.id.revokeAll).setVisible(false);
            popup.getMenu().findItem(R.id.minimalistModel).setChecked(true);
            popup.getMenu().findItem(R.id.changeTheme).setVisible(false);
        }
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            SpannableStringBuilder autoCheck = new SpannableStringBuilder(getString(R.string.autoCheck_menu));
            SpannableStringBuilder recoverByHand = new SpannableStringBuilder(getString(R.string.recoverByHand_menu));
            SpannableStringBuilder setting = new SpannableStringBuilder(getString(R.string.setting_menu));
            SpannableStringBuilder minimalistModel = new SpannableStringBuilder(getString(R.string.minimalistModel_menu));
            SpannableStringBuilder revokeAll = new SpannableStringBuilder(getString(R.string.revokeAll_menu));
            SpannableStringBuilder changeTheme = new SpannableStringBuilder(getString(R.string.changeTheme_menu));
            autoCheck.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.autoCheck_menu).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            recoverByHand.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.recoverByHand_menu).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            setting.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.setting_menu).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            minimalistModel.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.minimalistModel_menu).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            revokeAll.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.revokeAll_menu).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            changeTheme.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.changeTheme_menu).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            popup.getMenu().findItem(R.id.autoCheck).setTitle(autoCheck);
            popup.getMenu().findItem(R.id.recoverByHand).setTitle(recoverByHand);
            popup.getMenu().findItem(R.id.setting).setTitle(setting);
            popup.getMenu().findItem(R.id.revokeAll).setTitle(revokeAll);
            popup.getMenu().findItem(R.id.minimalistModel).setTitle(minimalistModel);
            popup.getMenu().findItem(R.id.changeTheme).setTitle(changeTheme);

        }
        if (recover_pre.getBoolean("autoCheck", false)) {
            popup.getMenu().findItem(R.id.autoCheck).setChecked(true);
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.changeTheme:
                        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
                            save_editor_v.putBoolean("changeTheme_setting", false).apply();
                            recreate();
                        } else {
                            save_editor_v.putBoolean("changeTheme_setting", true).apply();
                            recreate();
                        }
                        break;
                    case R.id.revokeAll:
                        isRevokeAllNotification();
                        break;
                    case R.id.recoverByHand:
                        if (recover_pre.getBoolean("neverReminder", true)) {
                            isRecoverByHandDialog();
                        }
                        else {
                            recoverDialog();
                        }
                        break;
                    case R.id.setting:
                        Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                        startActivity(intent);
                        break;
                    case R.id.minimalistModel:
                        if (recover_pre.getBoolean("minimalistModel", false)) {
                            MinimalistModel(false);
                        } else {
                            MinimalistModel(true);
                        }
                        break;
                    case R.id.autoCheck:
                        if (recover_pre.getBoolean("autoCheckNeverReminder", true)) {  //如果提示
                            autoCheckDialog();
                        } else {   //如果不再提示
                            if (recover_pre.getBoolean("autoCheck", false)) {
                                save_editor.putBoolean("autoCheck", false).apply();
                            } else {
                                save_editor.putBoolean("autoCheck", true).apply();
                            }

                        }
                        break;
                }
                return false;
            }
        });
        popup.show();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //闹钟按钮响应事件
            case R.id.alarm_iv:
                /*if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

                }else {
                    mainLayout.setBackground(Drawable.createFromPath("/storage/emulated/0/DCIM/Camera/IMG_20180123_154535.jpg"));
                }*/
                selfDefine();
                break;

            //撤销按钮响应事件
            case R.id.revoke_tv:
                if (isPushed) {
                    notifCount--;
                    if (notifCount == 0) {   //没有通知的话notifID也置为0
                        notifID = 0;
                    }
                    revokeNotifiction();
                    dbDelete(notifID_intent);
                    finish();
                }
                break;

            //撤销1按钮响应事件
            case R.id.revoke1_tv:
                if (isPushed) {
                    notifCount--;
                    if (notifCount == 0) {
                        notifID = 0;
                    }
                    revokeNotifiction();
                    dbDelete(notifID_intent);
                    finish();
                }
                break;

            //推送按钮响应事件
            case R.id.push_tv:
                if (getText()) {   //EditText内有内容
                    if (isPushed) {  //如果已经推送，则为修改，使用通知原ID进行推送
                        notifID = notifID_intent;
                        dbDelete(notifID);  //数据库删除原通知
                        dbInsert();   //插入修改后新通知
                        pushNotification(notifID, title_text, content_text);
                        finish();
                    } else {  //如果未推送，则创建新的通知
                        notifCount++;
                        notifID++;
                        pushNotification(notifID, title_text, content_text);
                        dbInsert();
                        finish();
                    }
                } else {
                    noContent();
                    egg();
                }
                break;
        }
    }


    // TODO 通知里的长文本可能需要展开，默认是不支持的

    //创建通知
    public void pushNotification(int notifID, String title_text, String content_text) {

        Intent intent = new Intent(this, MainActivity.class);  //单击消息意图
        intent.putExtra("notifID_intent", notifID);
        intent.putExtra("title_text", title_text);
        intent.putExtra("content_text", content_text);
        intent.putExtra("isPushed", true);
        //注意PendingIntent.getActivity的第二个参数，自行查阅文档
        PendingIntent pi = PendingIntent.getActivity(this, notifID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //使用兼容版本Notification 以兼容Android 8.0
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder notificationBulider = new NotificationCompat.Builder(this, null)
                .setContentTitle(title_text)
                .setContentText(content_text)
                .setSmallIcon(R.mipmap.polls_tap)   //小图标是必须设立的
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.polls_tap))
                .setContentIntent(pi)
                .setOngoing(recover_pre_v.getBoolean("notifOnGoing", true))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content_text))
                .setPriority(recover_pre.getInt("notifPriority", 2))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        Notification notifications = notificationBulider.build();   //这里有待验证
        manager.notify(notifID,notifications);

        //SharedPreferences形式持久化保存notifCount计数，防止Activity销毁后重新计数
        save_editor.putInt("notifCount", notifCount);
        if (!isPushed) {
            save_editor.putInt("notifID", notifID);
        }
        save_editor.apply();
    }

    //撤回通知
    public void revokeNotifiction() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notifID_intent);

        save_editor = getSharedPreferences("data", MODE_PRIVATE).edit();
        save_editor.putInt("notifCount", notifCount);
        save_editor.putInt("notifID", notifID);
        save_editor.apply();

    }


    //撤回所有通知
    public void revokeAllNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        for (int i = 0; i <= notifID; ++i){
            manager.cancel(i);
        }

        //全部置零
        notifID = 0;   //至于为什么不是下面直接写0，因为置零后Activity没有销毁，此时再次发送notifID没变
        notifCount = 0;
        save_editor.putInt("notifCount", notifCount);
        save_editor.putInt("notifID", notifID);
        save_editor.putBoolean("interrupt", false);
        save_editor.apply();

        //清空数据库表Notification
        db.delete("Notification", "id < ?", new String[] { "100" });   //还有人能用100条通知？
        Toast.makeText(this, getString(R.string.revokeAllNotification_toast), Toast.LENGTH_SHORT).show();
    }

    //断言EditText为空并取值
    public boolean getText() {
        if (title_auto.length() != 0 || content_auto.length() != 0) {
            title_text = title_auto.getText().toString();
            content_text = content_auto.getText().toString();
            return true;
        } else {
            return false;
        }
    }


    //用户未输入响应
    public void noContent() {
        //内容为空提醒
        Drawable drawable = getResources().getDrawable(R.drawable.ic_create);//获取图片资源
        //选择应用颜色对于这个图标无效
        if (recover_pre.getBoolean("changeTheme", false)) {
            drawable.setColorFilter(new LightingColorFilter(0xEEEEEFF, 0x009688));
        }
        drawable.setBounds(0, 0, 72, 72);
        //这里R.string.error_et无法引用，原因暂时未知
        title_auto.setError(getString(R.string.error_et), drawable);
        title_auto.requestFocus();   //修改聚焦

        //抖动动画
        TranslateAnimation animation = new TranslateAnimation(5, -5, 0, 0);
        animation.setInterpolator(new OvershootInterpolator());
        animation.setDuration(30);
        animation.setRepeatCount(7);
        animation.setRepeatMode(Animation.REVERSE);
        title_auto.startAnimation(animation);

        egg++;
    }


    //监听Activity内外单击事件
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            //如果点击位置在当前View外部则销毁当前视图
            if (!(event.getX() >= 0 && event.getY() >= 0)
                    || event.getX() >= mainLayout.getWidth() + 50 //微调
                    || event.getY() >= mainLayout.getHeight()) {
                //对话框存在内容
                if (title_auto.length() != 0 || content_auto.length() != 0) {
                    if (isPushed) {  //当前Activity处于修改状态（通知进来）
                        if (title_auto.getText().toString().equals(title_text)
                                && content_auto.getText().toString().equals(content_text)) {  //检测用户是否修改
                            finish();
                        } else {
                            modifDialog();
                        }

                    } else {  //当前Activity处于初次编辑状态（启动器进来）
                        contentExistDialog();
                    }

                } else {
                    finish();
                }
            }
        }
        return true;
    }


    //数据库创建
    public void dbCreate() {
        dbHelper = new DatabaseHelper(this, "Notification.db", null, 1);
        dbHelper.getWritableDatabase();
        db = dbHelper.getWritableDatabase();
    }

    //数据库插入
    public void dbInsert() {
        ContentValues values = new ContentValues();
        values.put("id", notifID);
        values.put("title", title_text);
        values.put("content", content_text);
        db.insert("Notification", null, values);
        Log.i(this.getClass().getName(), "insert success");
    }

    //数据库删除
    public void dbDelete(int notifID) {
        db.delete("Notification", "id = ?", new String[] { Integer.toString(notifID) });
        Log.i(this.getClass().getName(), "delete success");
    }

    //数据库读取(填写id数组)
    public void dbSelect() {
        int count = 0;
        Cursor cursor = db.query("Notification", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                idArr[count] = cursor.getInt(cursor.getColumnIndex("id"));
                count++;
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    //数据库读取(根据id返回标题)
    public String dbSelectGetTitle(int notifID) {
        String r = "";
        Cursor cursor = db.query("Notification", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                if (notifID == cursor.getInt(cursor.getColumnIndex("id"))) {
                    r = cursor.getString(cursor.getColumnIndex("title"));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();   //记得关闭cursor，防止被占用，不要直接返回
        return r;
    }

    //数据库读取(根据id返回内容)
    public String dbSelectGetContent(int notifID) {
        String r = "";
        Cursor cursor = db.query("Notification", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                if (notifID == cursor.getInt(cursor.getColumnIndex("id"))) {
                    r = cursor.getString(cursor.getColumnIndex("content"));
                }
            } while (cursor.moveToNext());
        }
        cursor.close();   //记得关闭cursor，防止被占用，不要直接返回
        return r;
    }


    //存在内容未推送对话框
    public void contentExistDialog() {
        SpannableStringBuilder message_dialog = new SpannableStringBuilder(getString(R.string.message_dialog) + "\n\n");
        SpannableStringBuilder messageInput_dialog = new SpannableStringBuilder(getString(R.string.messageInput_dialog) + "\n");
        SpannableStringBuilder title = new SpannableStringBuilder(getString(R.string.title) + ": ");
        SpannableStringBuilder content = new SpannableStringBuilder(getString(R.string.content) + ": ");
        SpannableStringBuilder titie_input = new SpannableStringBuilder(title_auto.getText().toString() + "\n");
        SpannableStringBuilder content_input = new SpannableStringBuilder(content_auto.getText().toString());

        messageInput_dialog.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.teal)), 0, getString(R.string.messageInput_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if ( title_auto.length() != 0) {   //如果标题存在，则变色
            titie_input.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.posColor)), 0, title_auto.getText().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if ( content_auto.length() != 0) {  //如果内容存在，则变色
            content_input.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.posColor)), 0, content_auto.getText().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        }
        AlertDialog.Builder dialog;
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            message_dialog.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.message_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            messageInput_dialog.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.messageInput_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.title).length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.content).length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
        } else {
            dialog = new AlertDialog.Builder(this);
        }
        message_dialog.append(messageInput_dialog); message_dialog.append(title); message_dialog.append(titie_input);
        message_dialog.append(content); message_dialog.append(content_input);


        dialog.setTitle(this.title);
        dialog.setMessage(message_dialog);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.positive_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, null);

        dialog.show();
    }


    //修改通知后未推送对话框
    public void modifDialog() {
        SpannableStringBuilder messageNotPushed_dialog = new SpannableStringBuilder(getString(R.string.messageNotPushed_dialog) + "\n\n");
        SpannableStringBuilder messageBefore_dialog = new SpannableStringBuilder(getString(R.string.messageBefore_dialog) + "\n");
        SpannableStringBuilder messageAfter_dialog = new SpannableStringBuilder(getString(R.string.messageAfter_dialog) + "\n");
        SpannableStringBuilder title = new SpannableStringBuilder(getString(R.string.title) + ": ");
        SpannableStringBuilder content = new SpannableStringBuilder(getString(R.string.content) + ": ");
        SpannableStringBuilder title_original = new SpannableStringBuilder(title_text + "\n");
        SpannableStringBuilder content_original = new SpannableStringBuilder(content_text + "\n\n");
        SpannableStringBuilder title_input = new SpannableStringBuilder(title_auto.getText().toString() + "\n");
        SpannableStringBuilder content_input = new SpannableStringBuilder(content_auto.getText().toString());

        messageBefore_dialog.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.teal)), 0, getString(R.string.messageBefore_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        messageAfter_dialog.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.teal)), 0, getString(R.string.messageAfter_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        if ( !title_auto.getText().toString().equals(title_text)) {   //如果标题被修改，则变色
            title_original.setSpan(new ForegroundColorSpan(Color.RED), 0, title_text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            title_input.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.posColor)), 0, title_auto.getText().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            title_original.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, title_text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            title_input.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, title_auto.getText().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if ( !content_auto.getText().toString().equals(content_text)) {  //如果内容被修改，则变色
            content_original.setSpan(new ForegroundColorSpan(Color.RED), 0, content_text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content_input.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.posColor)), 0, content_auto.getText().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            content_original.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, content_text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content_input.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, content_auto.getText().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        AlertDialog.Builder dialog;
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            messageNotPushed_dialog.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.messageNotPushed_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.title).length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.content).length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
        } else {
            dialog = new AlertDialog.Builder(this);
        }

        messageNotPushed_dialog.append(messageBefore_dialog); messageNotPushed_dialog.append(title); messageNotPushed_dialog.append(title_original);
        messageNotPushed_dialog.append(content); messageNotPushed_dialog.append(content_original); messageNotPushed_dialog.append(messageAfter_dialog);
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            //虽然一样，也得应用两次
            title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.title).length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.content).length() + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        messageNotPushed_dialog.append(title); messageNotPushed_dialog.append(title_input);
        messageNotPushed_dialog.append(content); messageNotPushed_dialog.append(content_input);

        dialog.setTitle(this.title);
        dialog.setMessage(messageNotPushed_dialog);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.positive_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, null);
        dialog.show();
    }

    //是否恢复对话框
    public void isRecoverDialog() {

        SpannableStringBuilder message = new SpannableStringBuilder(getString(R.string.message_isRecover_dialog));
        AlertDialog.Builder dialog;
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            message.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.message_isRecover_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
        } else {
            dialog = new AlertDialog.Builder(this);
        }
        dialog.setTitle(this.title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.recover_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                //使用数据库存储的ID全部发送
                for (int i = 0; i < notifCount; ++i) {
                    pushNotification(idArr[i], dbSelectGetTitle(idArr[i]), dbSelectGetContent(idArr[i]));
                }
                save_editor.putBoolean("interrupt", false).apply();
                finish();
            }
        });
        dialog.setNeutralButton(R.string.select2recover_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                recoverDialog();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, null);
        dialog.show();
    }

    //选择恢复对话框
    public void recoverDialog() {

        bools = new boolean[notifCount];
        items = new SpannableStringBuilder[notifCount];

        SpannableStringBuilder titleDialog = new SpannableStringBuilder(getString(R.string.title_needToRecover_dialog));
        titleDialog.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.green)), 0, getString(R.string.title_needToRecover_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        for (int i = 0; i < notifCount; ++i) {
            bools[i] = false;
            SpannableStringBuilder title = new SpannableStringBuilder(dbSelectGetTitle(idArr[i]) + "\n");
            SpannableStringBuilder content = new SpannableStringBuilder(dbSelectGetContent(idArr[i]) + "\n");
            title.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.teal)), 0, dbSelectGetTitle(idArr[i]).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.gery)), 0, dbSelectGetContent(idArr[i]).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content.setSpan(new RelativeSizeSpan(0.8f), 0, dbSelectGetContent(idArr[i]).length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            items[i] = title.append(content);
        }

        AlertDialog.Builder dialog;
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
        } else {
            dialog = new AlertDialog.Builder(this);
        }
        dialog.setTitle(titleDialog);
        dialog.setMultiChoiceItems(items, bools, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                bools[which] = isChecked;   //标记选中
            }
        });
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.push_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                for (int i = 0; i < items.length; i++) {
                    if (bools[i]) {
                        //逻辑
                        pushNotification(idArr[i], dbSelectGetTitle(idArr[i]), dbSelectGetContent(idArr[i]));
                    } else {
                        notifCount--;   //未推送，减去
                        dbDelete(idArr[i]);  //并从数据库删除
                    }
                }
                //至于ID增长问题，继续使用SharedPreference读取出的最大值自增
                if (notifCount == 0) {   //全部都没选的话
                    notifID = 0;
                }
                save_editor.putInt("notifCount", notifCount).apply();
                save_editor.putBoolean("interrupt", false).apply();
                finish();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, null);
        dialog.show();    }


    //手动恢复提示对话框
    public void isRecoverByHandDialog() {
        SpannableStringBuilder message = new SpannableStringBuilder(getString(R.string.message_recoverByHand_dialog));
        AlertDialog.Builder dialog;
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            message.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.message_recoverByHand_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
        } else {
            dialog = new AlertDialog.Builder(this);
        }
        dialog.setTitle(this.title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.select2recover_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                recoverDialog();
            }
        });
        dialog.setNeutralButton(R.string.neverReminder_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                save_editor.putBoolean("neverReminder", false).apply();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, null);
        dialog.show();
    }




    //自动检测提示对话框
    public void autoCheckDialog() {
        SpannableStringBuilder message = new SpannableStringBuilder(getString(R.string.message_autoCheck_dialog));
        AlertDialog.Builder dialog;
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            message.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.message_autoCheck_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
        } else {
            dialog = new AlertDialog.Builder(this);
        }
        dialog.setTitle(this.title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.open_autoCheck_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                save_editor.putBoolean("autoCheck", true).apply();
            }
        });
        dialog.setNeutralButton(R.string.neverReminder_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                save_editor.putBoolean("autoCheckNeverReminder", false).apply();
            }
        });
        dialog.setNegativeButton(R.string.close_autoCheck_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                save_editor.putBoolean("autoCheck", false).apply();
            }
        });
        dialog.show();
    }


    //撤销所有再次确认提示对话框
    public void isRevokeAllNotification() {
        SpannableStringBuilder message = new SpannableStringBuilder(getString(R.string.message_isRevokeAllNotification_dialog));
        AlertDialog.Builder dialog;
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            message.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.message_isRevokeAllNotification_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
        } else {
            dialog = new AlertDialog.Builder(this);
        }
        dialog.setTitle(this.title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.enter_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                revokeAllNotification();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, null);
        dialog.show();
    }



    //开机检测
    public void bootCompleted() {
        if (recover_pre.getBoolean("autoCheck", false)) {

        } else {   //自动检测关闭的情况下使用
            if (recover_pre.getBoolean("bootCompleted", false)) {
                SpannableStringBuilder message = new SpannableStringBuilder(getString(R.string.message_bootCompleted_dialog));
                AlertDialog.Builder dialog;
                if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
                    message.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.message_bootCompleted_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
                } else {
                    dialog = new AlertDialog.Builder(this);
                }
                dialog.setTitle(this.title);
                dialog.setMessage(message);
                dialog.setCancelable(false);
                dialog.setPositiveButton(R.string.recover_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        //使用数据库存储的ID全部发送
                        for (int i = 0; i < notifCount; ++i) {
                            pushNotification(idArr[i], dbSelectGetTitle(idArr[i]), dbSelectGetContent(idArr[i]));
                        }
                        save_editor.putBoolean("bootCompleted", false).apply();
                        finish();
                    }
                });
                dialog.setNeutralButton(R.string.select2recover_dialog, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        recoverDialog();
                    }
                });
                dialog.setNegativeButton(R.string.negative_dialog, null);
                dialog.show();
            }
        }
    }

    /**
     *  多线程
     *  1. 异常终止检测：由于所有Activity函数执行完后才执行Service，所以启动线程等待0.2s后MainService启动
     *     此时MainActivity在子线程内进行标志位"interrupt"的读取
     *     注意Android是线程不安全的，可以使用Android提供的异步方法进行多线程操作
     */

    @SuppressLint("HandlerLeak")
    public void isInterrupt() {
        handler = new Handler() {

            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    //异常终止检测
                    case WAIT_SERVICE_START:
                        if (recover_pre_v.getBoolean("notifBarEntrance", false)) {
                            notifBarEntrance();
                        }
                        //只测量一次
                        if (recover_pre.getBoolean("firstStart", true)) {
                            save_editor.putInt("mainLayoutHeight", mainLayout.getHeight()).apply();
                            save_editor.putInt("mainLayoutWidth", mainLayout.getWidth()).apply();
                            Log.i(this.getClass().getName(), Integer.toString(mainLayout.getHeight()));
                            Log.i(this.getClass().getName(), Integer.toString(mainLayout.getWidth()));
                        }
                        save_editor.putBoolean("firstStart", false).apply();
                        //极简模式，动态修改布局  //setTop这类函数只能onCreate()执行完成后生效
                        if (recover_pre.getBoolean("interrupt", true)) {  //异常终止
                            if (recover_pre.getBoolean("autoCheck", false)){  //自动检测开启
                                isRecoverDialog();
                            }
                        }
                        break;
                }

            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Message message = new Message();
                        message.what = WAIT_SERVICE_START;
                        handler.sendMessage(message);
                    }
                }, 1000);
            }
        }).start();
    }

    //时间选择对话框封装
    public void timePicker(int choose) {

        switch (choose) {
            case COUNT_DOWN:
                break;
            case TIMING:
                Calendar calendar = Calendar.getInstance();
                new TimePickerDialog(this,
                        // 绑定TimePickerDialog的监听器
                        new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view,
                                                  int hourOfDay, int minute) {
                                title_auto.setText("您选择了：" + hourOfDay + "时" + minute + "分");
                            }
                        }
                        // 设置初始时间
                        , calendar.get(Calendar.HOUR_OF_DAY)
                        , calendar.get(Calendar.MINUTE)
                        // true表示采用24小时制
                        , true).show();
                break;
        }


    }

    //极简模式(包括动画）   //动画只能用于移位，移位后单击事件等还是响应原来的位置
    public void MinimalistModel(boolean isOpen) {
        if (isOpen) {
            save_editor.putBoolean("minimalistModel", true).apply();
            ScaleAnimation alarmAnim1 = new ScaleAnimation(
                    1.0f, 0.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
            );
            alarmAnim1.setDuration(1500);
            alarmAnim1.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationRepeat(Animation animation) {}
                public void onAnimationStart(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    alarm_iv.setVisibility(View.GONE);
                }
            });
            alarm_iv.startAnimation(alarmAnim1);

            final TranslateAnimation revokeAnim = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_PARENT, -0.355f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
            );
            revokeAnim.setDuration(1500);
            //下面一行代码会和  revoke_tv.setVisibility(View.GONE)冲突，最终不消失
            /*revokeAnim.setFillAfter(true);   //停止在结束位置*/
            revokeAnim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    revoke_tv.clearAnimation();    //先清除动画，可以防止闪烁
                    revoke_tv.setVisibility(View.GONE);
                    revoke_tv = (TextView)findViewById(R.id.revoke1_tv);
                    revoke_tv.setVisibility(View.VISIBLE);
                    if (isPushed) {
                        revoke_tv.setTextColor(getResources().getColor(R.color.red));
                        revoke_tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));  //加粗
                    }
                }
            });
            revoke_tv.startAnimation(revokeAnim);

        } else {
            save_editor.putBoolean("minimalistModel", false).apply();
            alarm_iv.setVisibility(View.VISIBLE);
            TranslateAnimation alarmAnim2 = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f
            );
            alarmAnim2.setDuration(1500);
            alarm_iv.startAnimation(alarmAnim2);

            TranslateAnimation revokeAnim = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_PARENT, 0.355f,
                    Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
            );
            revokeAnim.setDuration(1500);
            revokeAnim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {}
                public void onAnimationRepeat(Animation animation) {}
                @Override
                public void onAnimationEnd(Animation animation) {
                    revoke_tv.clearAnimation();    //先清除动画，可以防止闪烁
                    revoke_tv.setVisibility(View.GONE);
                    revoke_tv = (TextView)findViewById(R.id.revoke_tv);
                    revoke_tv.setVisibility(View.VISIBLE);
                    if (isPushed) {
                        revoke_tv.setTextColor(getResources().getColor(R.color.red));
                        revoke_tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));  //加粗
                    }
                }
            });
            revoke_tv.startAnimation(revokeAnim);
        }
    }

    //自定义按钮对话框
    public void selfDefineDialog() {
        SpannableStringBuilder message = new SpannableStringBuilder(getString(R.string.message_SelfDefineIntro_dialog));
        AlertDialog.Builder dialog;
        if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
            message.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.light_gery)), 0, getString(R.string.message_SelfDefineIntro_dialog).length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            dialog = new AlertDialog.Builder(this, R.style.AlertDialog);
        } else {
            dialog = new AlertDialog.Builder(this);
        }
        dialog.setTitle(R.string.title_SelfDefineIntro_dialog);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.enter_dialog, null);
        dialog.show();
    }


    public void selfDefineIcon() {
        switch (recover_pre.getInt("selfDefine", 10)) {
            case IC_FAVOURITE:
                alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_favorite));
                break;
            case IC_VISIBILITY_OFF:
                alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_visibility_off));
                break;
            case IC_ALL_INCLUSIVE:
                if (recover_pre.getBoolean("autoCheck", false)) {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_all_inclusive));
                } else {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_all_inclusive_close));
                }
                break;
            case IC_CLEAR_ALL:
                alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_clear_all));
                break;
            case IC_BUILD:
                alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_build));
                break;
            case IC_BRIGHTNESS_6:
                alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_brightness_6));
                break;
            case IC_SETTINGS:
                alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_settings));
                break;
            case IC_POWER_SETTINGS_NEW:
                if (recover_pre_v.getBoolean("bootCompleted_setting", false)) {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_power_settings_new));
                } else {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_power_settings_new_close));
                }
                break;

            case IC_NOTE_ADD:
                if (recover_pre_v.getBoolean("notifBarEntrance", false)) {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_note_add));
                } else {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_note_add_close));
                }
                break;

            case IC_PHOTO:
                if (recover_pre_v.getBoolean("mainActivityBg", false)) {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_photo));
                } else {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_photo_close));
                }
                break;

            case IC_PHOTO_ALBUM:
                if (recover_pre_v.getBoolean("mainActivityBg", false)) {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_photo_album));
                } else {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_photo_album_close));
                }        }
    }

    public void selfDefine() {
        switch (recover_pre.getInt("selfDefine", 10)) {
            case IC_FAVOURITE:
                selfDefineDialog();
                break;

            case IC_VISIBILITY_OFF:
                if (recover_pre.getBoolean("minimalistModel", false)) {
                    MinimalistModel(false);
                } else {
                    MinimalistModel(true);
                }
                break;

            case IC_ALL_INCLUSIVE:
                if (recover_pre.getBoolean("autoCheckNeverReminder", true)) {  //如果提示
                    autoCheckDialog();
                } else {   //如果不再提示
                    if (recover_pre.getBoolean("autoCheck", false)) {
                        alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_all_inclusive_close));
                        save_editor.putBoolean("autoCheck", false).apply();
                    } else {
                        alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_all_inclusive));
                        save_editor.putBoolean("autoCheck", true).apply();
                    }
                }
                break;

            case IC_CLEAR_ALL:
                isRevokeAllNotification();
                break;

            case IC_BUILD:
                if (recover_pre.getBoolean("neverReminder", true)) {
                    isRecoverByHandDialog();
                }
                else {
                    recoverDialog();
                }
                break;

            case IC_BRIGHTNESS_6:
                if (recover_pre_v.getBoolean("changeTheme_setting", false)) {
                    save_editor_v.putBoolean("changeTheme_setting", false).apply();
                    recreate();
                } else {
                    save_editor_v.putBoolean("changeTheme_setting", true).apply();
                    recreate();
                }
                break;

            case IC_SETTINGS:
                Intent intent = new Intent(MainActivity.this, SettingActivity.class);
                startActivity(intent);
                break;

            case IC_POWER_SETTINGS_NEW:
                if (recover_pre_v.getBoolean("bootCompleted_setting", false)) {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_power_settings_new_close));
                    save_editor_v.putBoolean("bootCompleted_setting", false).apply();
                } else {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_power_settings_new));
                    save_editor_v.putBoolean("bootCompleted_setting", true).apply();
                }

            case IC_NOTE_ADD:
                if (recover_pre_v.getBoolean("notifBarEntrance", false)) {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_note_add_close));
                    NotificationManagerCompat manager = NotificationManagerCompat.from(this);
                    manager.cancel(1000);
                    save_editor_v.putBoolean("notifBarEntrance", false).apply();
                } else {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_note_add));
                    notifBarEntrance();
                    save_editor_v.putBoolean("notifBarEntrance", true).apply();
                }
                break;

            case IC_PHOTO:
                if (recover_pre_v.getBoolean("mainActivityBg", false)) {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_photo_close));
                    mainLayout.setBackground(null);
                    save_editor_v.putBoolean("mainActivityBg", false).apply();
                } else {
                    alarm_iv.setImageDrawable(getDrawable(R.drawable.ic_photo));
                    mainLayout.setBackground(Drawable.createFromPath(Environment.getExternalStorageDirectory() + "/APN/custom.jpg"));
                    save_editor_v.putBoolean("mainActivityBg", true).apply();
                }
                break;

            case IC_PHOTO_ALBUM:
                if (recover_pre_v.getBoolean("mainActivityBg", false)) {
                    startActivity(new Intent(this, ImageTool.class).putExtra("op", ImageTool.SELECT_PHOTO));
                } else {
                    Toast.makeText(this, R.string.openSettingFirst_toast, Toast.LENGTH_SHORT).show();
                }
                break;

        }
    }

    /**
     *  主界面背景设置
     */
    public void setBg() {
        if (recover_pre_v.getBoolean("mainActivityBg",false)) {
            mainLayout.setBackground(Drawable.createFromPath(Environment.getExternalStorageDirectory() + "/APN/custom.jpg"));
        }
    }

    /**
     *  常驻通知栏
     */
    public void notifBarEntrance () {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 1000, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder notificationBulider = new NotificationCompat.Builder(this, null)
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


    public void egg() {
        switch (egg) {
            case 2:
                Toast.makeText(this, R.string.first_toast, Toast.LENGTH_SHORT).show();
                break;
            case 6:
                Toast.makeText(this, R.string.second_toast, Toast.LENGTH_SHORT).show();
                break;
            case 10:
                Toast.makeText(this, R.string.third_toast, Toast.LENGTH_SHORT).show();
                break;
            case 16:
                Toast.makeText(this, R.string.fourth_toast, Toast.LENGTH_LONG).show();
                finish();
        }
    }

}



