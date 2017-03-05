package com.zh.zhvideoplayer.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * 申请权限
 */
public class PermissionUtils {
    static Activity context;

    public PermissionUtils(Activity context) {
        this.context=context;
    }

    public static final int MY_PERMISSIONS_REQUEST_CALL_PHONE=200;
    public static void needPermission(int requestCode)
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            return;
        }
        requestAllPermissions(requestCode);
    }
    private static void requestAllPermissions( int requestCode)
    {
//        ActivityCompat.requestPermissions(context,
//                new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.READ_CONTACTS, Manifest.permission.GET_ACCOUNTS, Manifest.permission.ACCESS_FINE_LOCATION},
//                MY_PERMISSIONS_REQUEST_CALL_PHONE);

        ActivityCompat.requestPermissions(context,
                new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE},
                MY_PERMISSIONS_REQUEST_CALL_PHONE);

    }
    private static boolean requesCallPhonePermissions( int requestCode)
    {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED)
        {//没有权限
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.CALL_PHONE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
            return false;
        } else
        {
            return true;
        }
    }

    private static boolean requestReadSDCardPermissions( int requestCode)
    {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)
        {//没有权限
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
            return false;
        } else
        {
            return true;
        }
    }
    private static boolean requestCamerPermissions( int requestCode)
    {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED)
        {//没有权限
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
            return false;
        } else
        {
            return true;
        }
    }
    private static boolean requestReadConstantPermissions( int requestCode)
    {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED)
        {//没有权限
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
            return false;
        } else
        {
            return true;
        }
    }
    private static boolean requestGET_ACCOUNTSPermissions( int requestCode)
    {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED)
        {//没有权限
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
            return false;
        } else
        {
            return true;
        }
    }
    private static boolean requestLocationPermissions( int requestCode)
    {
        if (ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED)
        {//没有权限
            ActivityCompat.requestPermissions(context,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_CALL_PHONE);
            return false;
        } else
        {
            return true;
        }
    }
}
