package com.sparkfengbo.ng.plugintestdemoapplication;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by fengbo on 2018/3/21.
 */

public class PluginBaseApplication extends Application {

    private AssetManager assetManager;
    private Resources newResource;
    private Resources.Theme mTheme;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("fengbo", "BaseApplicationonCreate");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        //应该放在线程中去做
        PluginUtil.getInst().copyAPKFileFromAssets(base);
        PluginUtil.getInst().loadAPK(base);
        dealResource(base);
        PluginUtil.getInst().hookHHandler();
        PluginUtil.getInst().hookAMSNative();
//        PluginUtil.getInst().hookActivityResource(base);

    }

    private void dealResource(Context context) {

        try {
            //创建我们自己的Resource
            String apkPath = PluginUtil.getInst().getLocalApkCopyPath();
            String mPath = context.getPackageResourcePath();

            assetManager = getAssets();
            Method addAssetPathMethod = assetManager.getClass().getDeclaredMethod("addAssetPath", String.class);
            addAssetPathMethod.setAccessible(true);
            addAssetPathMethod.invoke(assetManager, apkPath);
            addAssetPathMethod.invoke(assetManager, mPath);


            Method ensureStringBlocks = assetManager.getClass().getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocks.setAccessible(true);
            ensureStringBlocks.invoke(assetManager);

            Resources supResource = getResources();
            Log.e("fengbo", "supResource = " + supResource);
            newResource = new Resources(assetManager, supResource.getDisplayMetrics(), supResource.getConfiguration());
            Log.e("fengbo", "设置 getResource = " + getResources());
            mTheme = newResource.newTheme();
            mTheme.setTo(super.getTheme());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public AssetManager getAssets() {
        return assetManager == null ? super.getAssets() : assetManager;
    }

    @Override
    public Resources getResources() {
        return newResource == null ? super.getResources() : newResource;
    }

    @Override
    public Resources.Theme getTheme() {
        return mTheme == null ? super.getTheme() : mTheme;
    }

}
