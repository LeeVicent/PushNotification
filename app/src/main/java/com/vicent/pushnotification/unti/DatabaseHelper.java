package com.vicent.pushnotification.unti;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by LeeVincent 2018/1/21.
 */

public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String CREATE_NOTIFICATION = "create table Notification ("
            + "id integer primary key,"
            + "title text,"
            + "content text)";

    public Context mContext;

    public DatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        mContext = context;   //获取上下文
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_NOTIFICATION);
        Log.i(this.getClass().getName(), "Create db 'Notification' success");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
