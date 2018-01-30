package com.vicent.pushnotification.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.vicent.pushnotification.R;
import com.vicent.pushnotification.ui.activity.MainActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by LeeVicent on 2018/1/28.
 * 工具类Activity
 */

public class ImageTool extends AppCompatActivity {

    private Uri imageUri;
    private static final String IMAGE_FILE_LOCATION = Environment.getExternalStorageDirectory() + "/APN/custom.jpg";

    public static final int SELECT_PHOTO = 1;
    public static final int TAKE_PHOTO = 2;
    public static final int CROP_PHOTO = 5;

    public static SharedPreferences.Editor save_editor;   //SharedPreferences保存
    public static SharedPreferences recover_pre;  //SharedPreferences恢复
    public static SharedPreferences.Editor save_editor_v;
    public static SharedPreferences recover_pre_v;

    private int layoutHeight;
    private int layoutWidth;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        save_editor = getSharedPreferences("data", MODE_PRIVATE).edit();
        save_editor_v = getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE).edit();
        recover_pre = getSharedPreferences("data", MODE_PRIVATE);
        recover_pre_v = getSharedPreferences("com.vicent.pushnotification_preferences", MODE_PRIVATE);

        //用于确定裁剪尺寸
        layoutHeight = recover_pre.getInt("mainLayoutHeight", 300);
        layoutWidth = recover_pre.getInt("mainLayoutWidth", 300);
        requestPermission();


    }


    //权限申请
    public void requestPermission () {
        //如果还没有权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            //申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intent = getIntent();
            call(intent.getIntExtra("op", 0));
        }
    }


    /**
     * 选择调用的函数
     */
    public void call(int option) {
        //运行时权限获取，这些“危险权限”依然需要在Manifest文件声明
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            switch (option) {
                case SELECT_PHOTO:
                    Intent intent = new Intent("android.intent.action.GET_CONTENT");
                    intent.setType("image/*");
                    startActivityForResult(intent, SELECT_PHOTO);
                    break;

                case TAKE_PHOTO:
                    File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
                    try {
                        if (outputImage.exists()) {
                            outputImage.delete();
                        }
                        outputImage.createNewFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //SDK版本判断  大于7.0
                    if (Build.VERSION.SDK_INT >= 24) {
                        //7.0开始 需要使用FileProvider内容提供器，注册于于Manifest文件
                        imageUri = FileProvider.getUriForFile(getApplication(), "com.vicent.albumncamera.fileprovider", outputImage);
                    } else {
                        //7.0以下 将Java的File对象转换为Android的uri对象
                        imageUri = Uri.fromFile(outputImage);   //资源定位绑定到本地图片
                    }
                    //启动相机  //隐式Intent，系统去寻找能够响应此Intent的Activity
                    Intent intent1 = new Intent("android.media.action.IMAGE_CAPTURE");
                    intent1.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent1, TAKE_PHOTO);
                    break;

                default:
                    break;
            }

        }
    }


    /**
     *
     * @param requestCode  请求码：由主调Activity确定，回调onActivityResult时用于确认处理主调Activity逻辑
     * @param resultCode   结果码：由被调Activity确定，回调onActivityResult时用于确认被调Activity执行结果
     * @param data 被调Activity返回的Intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        //根据imageUri,使用BitmapFactory格式化为bitmap对象
                        //这是Uri转为Bitmap的方式，注意记下来
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

            case SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    File nf = new File(Environment.getExternalStorageDirectory()+"/APN");
                    nf.mkdir();
                    File outputImage = new File(Environment.getExternalStorageDirectory(), "/APN/custom.jpg");
                    try {
                        outputImage.createNewFile();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    imageUri = Uri.fromFile(outputImage);   //资源定位绑定到本地图片
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    //设置要裁剪的Uri与类型
                    intent.setDataAndType(data.getData(), "image/*");
                    // 设置裁剪
                    intent.putExtra("crop", "true");
                    // aspectX , aspectY :宽高的比例       //裁剪按照比例裁剪
                    intent.putExtra("aspectX", layoutWidth);
                    intent.putExtra("aspectY", layoutHeight);
                    // outputX , outputY : 裁剪图片宽高     //这个像素用来压缩，最终按照上面的比例选择，分辨率为outputX*outputY
                    intent.putExtra("outputX", layoutWidth);
                    intent.putExtra("outputY", layoutHeight);
                    //设置了true的话直接返回bitmap，可能会很占内存
                    intent.putExtra("return-data", false);
                    //设置输出的格式
                    intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
                    //设置输出的地址     //这里输出的是系统裁剪的原图
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(intent, CROP_PHOTO);
                }
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, R.string.cropCancel_toast, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case CROP_PHOTO:
                if (resultCode == RESULT_OK) {
                    MainActivity.instance.recreate();
                    finish();
                }
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, R.string.cropCancel_toast, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = getIntent();
                    call(intent.getIntExtra("op", 0));
                } else {
                    Toast.makeText(this, R.string.permissionCancel_toast, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainActivity.instance.recreate();
    }
}


