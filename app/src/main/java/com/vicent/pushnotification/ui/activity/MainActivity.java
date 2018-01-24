package com.vicent.pushnotification.ui.activity;


import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import android.widget.TimePicker;
import android.view.animation.*;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.graphics.drawable.Drawable;
import android.widget.Toast;
import com.vicent.pushnotification.unti.DatabaseHelper;

import com.vicent.pushnotification.R;

import java.util.Calendar;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
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
    private boolean isAbnormalStoped = true;   //断言程序是否异常终止（thread而不是Activity）
    private String title_text;
    private String content_text;

    private SharedPreferences.Editor save_editor;   //SharedPreferences保存
    private SharedPreferences recover_pre;  //SharedPreferences恢复

    private DatabaseHelper dbHelper;  //数据库
    private SQLiteDatabase db;

    //恢复Dialog使用
    private boolean[] bools;
    private String[] items;
    private int[] idArr;   //保存数据库所有id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        setOnClickListeners();
        onNewIntent(getIntent());
        dbCreate();
        save_editor = getSharedPreferences("data", MODE_PRIVATE).edit();

        //恢复notifCount计数
        recover_pre = getSharedPreferences("data", MODE_PRIVATE);
        notifCount = recover_pre.getInt("notifCount", 0);
        notifID = recover_pre.getInt("notifID", 0);
        isAbnormalStoped = recover_pre.getBoolean("isAbnormalStoped", false);
        idArr = new int[notifCount];
        dbSelect();

        if (isAbnormalStoped) {   //如果为false，则表明上次正常退出
            //此处为异常处理代码
            isRecoverDialog();
        } else {
           //此处为正常处理代码
            save_editor.putBoolean("isAbnormalStoped", true);   //如果程序退出之前都没有置为false，则发生异常
            save_editor.apply();
        }
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

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        save_editor.putBoolean("isAbnormalStoped", false);
        save_editor.apply();
        Log.i(this.getClass().getName(), "正常终止MainActivity");
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
        revoke_tv = (TextView) findViewById(R.id.revoke_tv);
        push_tv = (TextView) findViewById(R.id.push_tv);
        mainLayout = (RelativeLayout)findViewById(R.id.mainLayout);

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


    public void moreVertOnClick(View view) {
        Toast.makeText(this, "测试", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //闹钟按钮响应事件
            case R.id.alarm_iv:

                revokeAllNotification();
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

            //推送按钮响应事件
            case R.id.push_tv:
                if (getText()) {   //EditText内有内容
                    if (isPushed) {  //如果已经推送，则为修改，使用通知原ID进行推送
                        notifID = notifID_intent;
                        dbDelete(notifID);
                        dbInsert();
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
                }
                egg();
                break;
        }
    }


    // TODO 通知里的长文本可能需要展开，默认是不支持的

    //创建通知(根据id）
    public void pushNotification(int notifID, String title_text, String content_text) {
        isPushed = true;  //已推送

        Intent intent = new Intent(this, MainActivity.class);  //单击消息意图
        intent.putExtra("notifID_intent", notifID);
        intent.putExtra("title_text", title_text);
        intent.putExtra("content_text", content_text);
        intent.putExtra("isPushed", isPushed);
        //注意PendingIntent.getActivity的第二个参数，自行查阅文档
        PendingIntent pi = PendingIntent.getActivity(this, notifID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        //使用兼容版本Notification 以兼容Android 8.0
        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        NotificationCompat.Builder notificationBulider = new NotificationCompat.Builder(this, null)
                .setContentTitle(title_text)
                .setContentText(content_text)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentIntent(pi)
                .setOngoing(true)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content_text))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        Notification notifications = notificationBulider.build();   //这里有待验证
        manager.notify(notifID,notifications);

        //SharedPreferences形式持久化保存notifCount计数，防止Activity销毁后重新计数
        save_editor.putInt("notifCount", notifCount);
        save_editor.putInt("notifID", notifID);
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
        save_editor.apply();

        //清空数据库表Notification
        db.delete("Notification", "id < ?", new String[] { "100" });   //还有人能用100条通知？
        Toast.makeText(this, getString(R.string.revokeAllNotification), Toast.LENGTH_SHORT).show();
    }

    //断言EditText为空并取值
    public boolean getText() {
        if (title_auto.length() != 0 || content_auto.length() != 0) {
            title_text = title_auto.getText().toString();
            content_text = content_auto.getText().toString();
            Log.i(this.getClass().getName(), "不为空");
            return true;
        } else {
            Log.i(this.getClass().getName(), "为空");
            return false;
        }
    }


    //用户未输入响应
    public void noContent() {
        //内容为空提醒
        Drawable drawable = getResources().getDrawable(R.drawable.ic_create);//获取图片资源
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

        message_dialog.append(messageInput_dialog); message_dialog.append(title); message_dialog.append(titie_input);
        message_dialog.append(content); message_dialog.append(content_input);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.title_dialog);
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
        }
        if ( !content_auto.getText().toString().equals(content_text)) {  //如果内容被修改，则变色
            content_original.setSpan(new ForegroundColorSpan(Color.RED), 0, content_text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            content_input.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.posColor)), 0, content_auto.getText().toString().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        messageNotPushed_dialog.append(messageBefore_dialog); messageNotPushed_dialog.append(title); messageNotPushed_dialog.append(title_original);
        messageNotPushed_dialog.append(content); messageNotPushed_dialog.append(content_original); messageNotPushed_dialog.append(messageAfter_dialog);
        messageNotPushed_dialog.append(title); messageNotPushed_dialog.append(title_input);
        messageNotPushed_dialog.append(content); messageNotPushed_dialog.append(content_input);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.title_dialog);
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

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.title_dialog);
        dialog.setMessage(R.string.message_isRecover_dialog);
        dialog.setCancelable(false);
        dialog.setPositiveButton(R.string.recover_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                for (int i = 0; i < notifCount; ++i) {
                    pushNotification(idArr[i], dbSelectGetTitle(idArr[i]), dbSelectGetContent(idArr[i]));
                }
                finish();
            }
        });
        dialog.setNeutralButton(R.string.neutral_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                recoverDialog();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                revokeAllNotification();
            }
        });
        dialog.show();
    }

    //选择恢复对话框
    public void recoverDialog() {

        bools = new boolean[notifCount];
        items = new String[notifCount];

        for (int i = 0; i < notifCount; ++i) {
            bools[i] = false;
            items[i] = getString(R.string.title) + ": " + dbSelectGetTitle(idArr[i]) + "\n"
                         + getString(R.string.content) + ": " + dbSelectGetContent(idArr[i]) + "\n";
        }

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.needToRecover_dialog);
        dialog.setMultiChoiceItems(items, bools,new DialogInterface.OnMultiChoiceClickListener() {
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
                if (notifCount == 0) {   //全部都没选的话
                    notifID = 0;
                }

                save_editor.putInt("notifCount", notifCount);
                save_editor.apply();
            }
        });
        dialog.setNegativeButton(R.string.negative_dialog, null);
        dialog.show();
    }


    //时间选择对话框封装

    public void timePicker() {
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



