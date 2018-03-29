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
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //拷贝应该放在线程中去做
        PluginUtil.getInst().copyAPKFileFromAssets(base);
        PluginUtil.getInst().loadAPK(base);
        dealResource(base);
        PluginUtil.getInst().hookHHandler();
        PluginUtil.getInst().hookAMSNative();
    }


    /**
     * 用来处理插件中的资源
     *
     *
     * 原理
     *
     *  参考ContextImpl的构造方法中对mResources的构造（下面的博客参考的是5.0，我发现7.0的构造有修改）
     *
     * 尚未解决的问题：
     *
     *  如果插件中的layout中使用了v7包的组件，那么运行会报错:  error inflate
     *
     * 参考：
     *  <a href="https://blog.csdn.net/yulong0809/article/details/59489396">Android插件化资源的使用及动态加载 附demo</>
     *
     *  这篇文章的代码地址是
     *
     *  <a href="https://github.com/ljqloveyou123/LiujiaqiAndroid">demo</>
     * @param context
     */
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
