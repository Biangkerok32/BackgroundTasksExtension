package com.kumaraswamy.tasks;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

class UTILS {
    public static String getClassName(Object componentName) {
        String regex = "[^.$@a-zA-Z0-9]";
        String componentNameString = componentName.toString().replaceAll(regex, "");

        if (componentName instanceof String && componentNameString.contains(".")) {
            return componentNameString;
        } else if (componentName instanceof String) {
            return BackgroundTasks.BASE + componentNameString;
        } else if (componentName instanceof Component) {
            return componentName.getClass().getName().replaceAll(regex, "");
        } else {
            throw new YailRuntimeError("Component is invalid.", "BackgroundTasks");
        }
    }

    public static Component newInstance(Constructor<?> constructor, ComponentContainer container) {
        Component mComponent = null;

        try {
            mComponent = (Component) constructor.newInstance(container);
        } catch(Exception e) {
            e.printStackTrace();
        }

        return mComponent;
    }

    public static boolean isEmptyOrNull(Object object) {
        if(object instanceof String) {
            return object.toString().replaceAll(" ", "").isEmpty();
        }
        return object == null;
    }

    public static Method getMethod(Method[] methods, String name, int parameterCount) {
        name = name.replaceAll("[^a-zA-Z0-9]", "");
        for (Method method : methods) {
            int methodParameterCount = method.getParameterTypes().length;
            if (method.getName().equals(name) && methodParameterCount == parameterCount) {
                return method;
            }
        }

        return null;
    }

    public static void startService(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            activity.startForegroundService(serviceIntent(activity));
        else {
            activity.startService(serviceIntent(activity));
        }
    }

    public static void stopService(Activity activity) {
        activity.stopService(serviceIntent(activity));
    }

    private static Intent serviceIntent(Activity activity) {
        return new Intent(activity, BackgroundService.class);
    }
}