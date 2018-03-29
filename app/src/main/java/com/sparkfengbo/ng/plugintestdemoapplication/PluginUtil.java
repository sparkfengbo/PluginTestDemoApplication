package com.sparkfengbo.ng.plugintestdemoapplication;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created by fengbo on 2018/3/21.
 */

public class PluginUtil {

    /** 单例 **/
    private static PluginUtil sPluginUtil;

    private static final String DATA_RAW_INTENT = "data_raw_intent";
    /** Asset文件夹中APK文件名，测试写死 **/
    private final String mAPKFileName = "app-debug.apk";
    /** APK文件从Asset拷贝到手机本地的存储路径 **/
    private String mLocalApkCopyPath;


    public static PluginUtil getInst() {
        if (sPluginUtil == null) {
            synchronized (PluginUtil.class) {
                if (sPluginUtil == null) {
                    sPluginUtil = new PluginUtil();
                }
            }
        }
        return sPluginUtil;
    }

    private PluginUtil() {

    }

    /**
     * 根据程序启动原理，Hook掉AMS在APP的代理，ActivityMangerNative的IActivityManager
     */
    public void hookAMSNative() {
        try {
            Class<?> activeManagerNativeClass = Class.forName("android.app.ActivityManagerNative");

            Field gDefaultFiled = activeManagerNativeClass.getDeclaredField("gDefault");

            gDefaultFiled.setAccessible(true);

            Object gDefault = gDefaultFiled.get(null);

            /*****/

            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field instFiled = singletonClass.getDeclaredField("mInstance");
            instFiled.setAccessible(true);

            Object iActivetManagerInstance = instFiled.get(gDefault);

            Class<?> iActivityManagerClass = Class.forName("android.app.IActivityManager");

            Object amsNativeProxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new
                    Class[]{iActivityManagerClass}, new AMSNativeProxyHandler(iActivetManagerInstance));
            instFiled.set(gDefault, amsNativeProxy);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void hookHHandler() {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field currentActivityThreadField = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            currentActivityThreadField.setAccessible(true);
            Object currentActivityThread = currentActivityThreadField.get(null);

            Field hHandlerField = activityThreadClass.getDeclaredField("mH");
            hHandlerField.setAccessible(true);
            Object rawHHandler = hHandlerField.get(currentActivityThread);

            if (rawHHandler instanceof Handler) {
                Log.e("fengbo", "correct object  is Handler");
                Class<?> handlerClass = Class.forName("android.os.Handler");
                Field callbackField = handlerClass.getDeclaredField("mCallback");
                callbackField.setAccessible(true);
                callbackField.set(rawHHandler, new HHandlerCallBack((Handler) rawHHandler));
            } else {
                Log.e("fengbo", "wrong object  not Handler");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private class HHandlerCallBack implements Handler.Callback {

        Handler rawHHandler;

        public HHandlerCallBack(Handler rawHHandler) {
            this.rawHHandler = rawHHandler;
        }

        @Override
        public boolean handleMessage(Message msg) {
            Log.e("fengbo", "准备 handle Message ： " + msg.what);
            if (msg.what == 100) {
                    dealIntentForPerformLaunchActivity(msg);
            }
            rawHHandler.handleMessage(msg);
            return true;
        }

        private void dealIntentForPerformLaunchActivity(Message msg) {
            Object activityRecordClient = msg.obj;

            if (activityRecordClient != null) {
                try {
                    Field intentField = activityRecordClient.getClass().getDeclaredField("intent");
                    intentField.setAccessible(true);

                    Object stubIntentObj = intentField.get(activityRecordClient);

                    if (stubIntentObj instanceof Intent) {
                        Intent stubIntent = (Intent) stubIntentObj;
                        if (stubIntent.getParcelableExtra(DATA_RAW_INTENT) instanceof  Intent) {
                            Intent rawIntent = stubIntent.getParcelableExtra(DATA_RAW_INTENT);
                            stubIntent.setComponent(rawIntent.getComponent());

                            intentField.set(activityRecordClient  , stubIntent);
                        }
                    }
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class AMSNativeProxyHandler implements InvocationHandler {

        Object mRawIActivityManager;

        public AMSNativeProxyHandler(Object iActivityManagerInstance) {
            mRawIActivityManager = iActivityManagerInstance;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

            Log.e("fengbo", "我进行代理了 ，代理的方法是 ： " + method.getName());

            /**
             * 需要处理 API23
             *
             * http://weishu.me/2016/03/21/understand-plugin-framework-activity-management/
             */

            if ("startActivity".equals(method.getName())) {
                Log.e("fengbo", "你想干嘛？startActivity 呀？");
                Intent rawIntent;
                int index = 0;
                for (int i = 0; i < args.length; i++) {
                    if (args[i] instanceof Intent) {
                        index = i;
                        break;
                    }
                }

                rawIntent = (Intent) args[index];

                Intent stubIntent = new Intent();

                String pkg = "com.sparkfengbo.ng.plugintestdemoapplication";
                stubIntent.setComponent(new ComponentName(pkg, StubEmptyActivity.class.getCanonicalName()));
                stubIntent.putExtra(DATA_RAW_INTENT, rawIntent);
                args[index] = stubIntent;

                return method.invoke(mRawIActivityManager, args);
            }
            return method.invoke(mRawIActivityManager, args);
        }
    }


    /**
     * 通过DexClassLoader将apk中的dex添加到
     */
    public void loadAPK(Context context) {
        File file = new File(mLocalApkCopyPath);

        if (!file.exists()) {
            Log.e("fengbo", file.getAbsolutePath() + "  not exits");
            return;
        }

        PathClassLoader pathClassLoader = (PathClassLoader) context.getClassLoader();

        DexClassLoader dexClassLoader = new DexClassLoader(mLocalApkCopyPath, context.getCacheDir().getAbsolutePath()
                , context.getCacheDir().getAbsolutePath(), pathClassLoader);


        try {

            //包含关系 BaseDexClassLoader -》DexPathList pathList-》<DexPathList$Element> Element[] dexElements
            Class<?> baseClassLoaderCls = Class.forName("dalvik.system.BaseDexClassLoader");
            Class<?> dexPathLisCls = Class.forName("dalvik.system.DexPathList");

            Field pathList = baseClassLoaderCls.getDeclaredField("pathList");
            pathList.setAccessible(true);

            Field dexElements = dexPathLisCls.getDeclaredField("dexElements");
            dexElements.setAccessible(true);


            //获取PathClassLoader的pathList
            Object pclPathList = pathList.get(pathClassLoader);  //zip file "/data/app/com.sparkfengbo.ng
            // .plugintestdemoapplication-2/base.apk"

            //获取PathClassLoader的pathList的dexElements
            Object pclDexElements = dexElements.get(pclPathList);

            //获取DexClassLoader的pathList
            Object dclPathList = pathList.get(dexClassLoader);   //zip file /storage/emulated/0/Android/data/com
            // .sparkfengbo.ng.plugintestdemoapplication/cache/android_demo.apk"

            //获取DexClassLoader的pathList的dexElements
            Object dclDexElements = dexElements.get(dclPathList);

            Class<?> elementCls = pclDexElements.getClass().getComponentType();

            Log.e("fengbo", "before combine " + Array.getLength(pclDexElements) + "   " + Array.getLength
                    (dclDexElements));

            int originDexEleListLength = Array.getLength(pclDexElements);

            int dexFileEleListLength = Array.getLength(dclDexElements);

            Object result = Array.newInstance(elementCls, originDexEleListLength + dexFileEleListLength);

            for (int i = 0; i < Array.getLength(result); i++) {

                if (i < originDexEleListLength) {
                    Array.set(result, i, Array.get(pclDexElements, i));
                } else {
                    Array.set(result, i, Array.get(dclDexElements, i - originDexEleListLength));
                }
            }

            //将PathClassLoader的pathList下的dexElements替换为我们新复制的result
            dexElements.set(pclPathList, result);

            pclDexElements = dexElements.get(pclPathList);

            //可以看到，我们最终的PathClassLoader下的dexElements变成了2
            Log.e("fengbo", "after combine " + Array.getLength(pclDexElements) + "   " + Array.getLength
                    (dclDexElements));

        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * 将Asset文件夹下的 apk复制到 sdcard中
     */
    public void copyAPKFileFromAssets(Context context) {
        mLocalApkCopyPath = context.getExternalCacheDir().getAbsolutePath() + File.separator + mAPKFileName;
        Log.e("fengbo", "mLocalApkCopyPath : " + mLocalApkCopyPath);

        InputStream in = null;
        FileOutputStream fos = null;
        try {

            File file = new File(mLocalApkCopyPath);

            if (file.exists()) {
                file.delete();
                Log.e("fengbo", file.getAbsolutePath() + "  already exits and delete");
            } else {
                Log.e("fengbo", file.getAbsolutePath() + "  not exits");
            }

            AssetManager manager = context.getAssets();

            for (String temp : manager.list("")) {
                Log.e("fengbo", "assets file " + temp);
            }

            in = manager.open(mAPKFileName);
            fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int byteCount = 0;
            while ((byteCount = in.read(buffer)) != -1) {
                fos.write(buffer, 0, byteCount);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getLocalApkCopyPath() {
        Log.e("fengbo", "getLocalApkCopyPath : " + mLocalApkCopyPath);
        return this.mLocalApkCopyPath;
    }
}
