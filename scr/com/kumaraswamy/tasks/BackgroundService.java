package com.kumaraswamy.tasks;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import bsh.EvalError;
import bsh.Interpreter;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.kumaraswamy.tasks.UTILS.isEmptyOrNull;

public class BackgroundService extends Service {

    private static final String TAG = "BackgroundTasks";

    private final HashMap<String, Component> componentsBuiltMap = new HashMap<>();

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "BackgroundService";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Service is started",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(BackgroundTasks.contentTitle)
                    .setContentText(BackgroundTasks.contentText)
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build();

            startForeground(1, notification);
        }
        try {
            startTask();
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }
    }

    public void startTask() throws IllegalAccessException, InvocationTargetException, InstantiationException {

        Log.e("BACKGROUND", "starttask");
//


        //--------------------------------------------------------------------

        int index = 0;

        for(String function: BackgroundTasks.processTable) {
            ArrayList<Object> processValues = BackgroundTasks.processFunctions.get(index);

            if(function.equals("create")) {
                createComponent(processValues);
            } else if(function.equals("executeFunction")) {
                executeFunction(processValues);
            } else {
                invokeFunction(processValues);
            }
            index++;
        }
        //--------------------------------------------------------------------

        Log.e("BACKGROUND", "END");
    }

    public void createComponent(ArrayList<Object> processValues) {
        final String componentID = String.valueOf(processValues.get(0));
        final String componentName = UTILS.getClassName(processValues.get(1));


        Class<?> mClass;
        Constructor<?> mConstructor = null;
        try {
            mClass = Class.forName(componentName);
            mConstructor = mClass.getConstructor(ComponentContainer.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            e.printStackTrace();
        }

        Component component = UTILS.newInstance(mConstructor, BackgroundTasks.componentContainer);

        if(!isEmptyOrNull(component)) {
            Log.e("BACKGROUND", "Put to hashmap");
            componentsBuiltMap.put(componentID, component);
        }

        Log.e("BACKGROUND", "ID " + componentID);
    }

    public void invokeFunction(ArrayList<Object> processValues) {
        final String ID = String.valueOf(processValues.get(0));
        final String functionName = String.valueOf(processValues.get(1));
        final long time = (long) processValues.get(2);
        final long timeRemaining = time - System.currentTimeMillis();
        final String whenFinish = String.valueOf(processValues.get(4));

        final List<Object> arrayList = new ArrayList<>(Arrays.asList((Object[]) processValues.get(3)));

        try {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    invoke(ID, functionName, arrayList);
                    if(!whenFinish.isEmpty()) {
                        callFunction(whenFinish);
                    }
                }
            }, timeRemaining);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to invoke function, negative time, id: " + ID + ", function: " + functionName);
        }
    }

    public void invoke(final String ID, final String functionName, final List<Object> arrayList) {
        Component component = componentsBuiltMap.get(ID);
        Method[] mMethods = component.getClass().getMethods();

        try {
            Object[] mParameters = arrayList.toArray();

            Method mMethod = UTILS.getMethod(mMethods, functionName, mParameters.length);

            Class<?>[] mRequestedMethodParameters = mMethod.getParameterTypes();
            ArrayList<Object> mParametersArrayList = new ArrayList<>();

            for (int i = 0; i < mRequestedMethodParameters.length; i++) {
                if ("int".equals(mRequestedMethodParameters[i].getName())) {
                    mParametersArrayList.add(Integer.parseInt(mParameters[i].toString()));
                } else if ("float".equals(mRequestedMethodParameters[i].getName())) {
                    mParametersArrayList.add(Float.parseFloat(mParameters[i].toString()));
                } else if ("double".equals(mRequestedMethodParameters[i].getName())) {
                    mParametersArrayList.add(Double.parseDouble(mParameters[i].toString()));
                } else if ("java.lang.String".equals(mRequestedMethodParameters[i].getName())) {
                    mParametersArrayList.add(mParameters[i].toString());
                } else if ("boolean".equals(mRequestedMethodParameters[i].getName())) {
                    mParametersArrayList.add(Boolean.parseBoolean(mParameters[i].toString()));
                } else {
                    mParametersArrayList.add(mParameters[i]);
                }
            }

            mMethod.invoke(component, mParametersArrayList.toArray());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void callFunction(String functionID) {
        ArrayList<Object> arrayList = BackgroundTasks.hashMap.get(functionID);

        final String componentID = (String) arrayList.get(0);
        final String functionName = (String) arrayList.get(1);
        final List<Object> parameters = new ArrayList<>(Arrays.asList((Object[]) arrayList.get(2)));
        final List<String> valuesAsCodes = new ArrayList<>(Arrays.asList((String[]) arrayList.get(3)));

        for(String index: valuesAsCodes) {
            int number = Integer.parseInt(index);
            number--;
            parameters.set(number, executeCode(String.valueOf(parameters.get(number))));
        }

        invoke(componentID, functionName, parameters);
    }

    public Object executeCode(String code) {
        Interpreter interpreter = new Interpreter();
        Object result = null;

        try {
            interpreter.set("context", getApplicationContext());
            result = interpreter.eval(code);
        } catch (EvalError evalError) {
            Log.e(TAG, evalError.getErrorText());
        }

        return isEmptyOrNull(result) ? "" : result;
    }

    public void executeFunction(ArrayList<Object> arrayList) {
        final String functionID = String.valueOf(arrayList.get(0));
        final long tillTime = (long) arrayList.get(1);
        final long delay = (long) arrayList.get(2);
        final long interval = (long) arrayList.get(3);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(System.currentTimeMillis() >= tillTime) {
                    timer.cancel();
                    return;
                }
                callFunction(functionID);
            }
        }, delay, interval);
    }


    @Override
    public void onStart(Intent intent, int startid) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}