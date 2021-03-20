package com.kumaraswamy.tasks;

import android.app.Activity;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.annotations.androidmanifest.ServiceElement;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.OnPauseListener;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.ArrayList;
import java.util.HashMap;

@UsesLibraries(libraries = "beanshell.jar")
@UsesPermissions(permissionNames = "android.permission.FOREGROUND_SERVICE, oppo.permission.OPPO_COMPONENT_SAFE")
@UsesServices(services = {@ServiceElement(name = "com.kumaraswamy.tasks.BackgroundService")})
@DesignerComponent(
        version = 1,
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        versionName = "1.0 A",
        iconName = "aiwebres/icon.png"
)
@SimpleObject(external = true)

public class BackgroundTasks extends AndroidNonvisibleComponent implements OnDestroyListener, OnPauseListener {

    public static final String BASE = "com.google.appinventor.components.runtime.";

    public static ArrayList<String> processTable = new ArrayList<>();
    public static ArrayList<ArrayList<Object>> processFunctions = new ArrayList<>();
    private static final ArrayList<String> componentID = new ArrayList<>();

    public static String contentTitle, contentText = "";

    public static HashMap<String, ArrayList<Object>> hashMap = new HashMap<>();

    public static ComponentContainer componentContainer;

    private boolean startWhenInterrupt = false;

    private static Activity activity;

    public BackgroundTasks(ComponentContainer container) {
        super(container.$form());

        componentContainer = container;
        activity = container.$context();

        form.registerForOnDestroy(this);
        form.registerForOnPause(this);
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_TEXTAREA)
    @SimpleProperty
    public void ActionTitle(String title) {
        contentTitle = title;
    }

    @SimpleProperty
    public String ActionTitle() {
        return contentTitle;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_TEXTAREA)
    @SimpleProperty
    public void ActionText(String text) {
        contentText = text;
    }

    @SimpleProperty
    public String ActionText() {
        return contentText;
    }

    @SimpleFunction(description = "Create a component in the background")
    public void CreateComponent(final Object component, final String id) {
        ArrayList<Object> values = new ArrayList<>();
        values.add(id);
        values.add(component);

        processTable.add("create");
        processFunctions.add(values);
        componentID.add(id);
    }

    @SimpleFunction(description = "Call a function in the background")
    public void InvokeFunction(final String id, final String functionName, final YailList values, final long time, final String whenFinish) {
        if(componentID.contains(id)) {
            ArrayList<Object> arrayList = new ArrayList<>();
            arrayList.add(id);
            arrayList.add(functionName);
            arrayList.add(time);
            arrayList.add(values.toArray());
            arrayList.add(whenFinish);

            processTable.add("invoke");
            processFunctions.add(arrayList);
            componentID.add(id);
        } else {
            throw new YailRuntimeError("Expected a created component ID but got '" + id + "'.", "Background Tasks");
        }
    }

    @SimpleFunction(description = "Create a function in the background")
    public void CreateFunction(final String functionID, final String id, /*COMPONENT ID --> */ final String functionName, /* <<-- FUNCTION NAME */ final YailList values, final YailList interpretValues) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(id);
        arrayList.add(functionName);
        arrayList.add(values.toArray());
        arrayList.add(interpretValues.toStringArray());

        hashMap.put(functionID, arrayList);
    }

    @SimpleFunction(description = "Execute a function till particular time")
    public void ExecuteFunction(String functionID, long tillTime, long delay, long interval) {
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(functionID);
        arrayList.add(tillTime);
        arrayList.add(delay);
        arrayList.add(interval);

        processTable.add("executeFunction");
        processFunctions.add(arrayList);
    }

    @SimpleFunction(description = "Start the service")
    public void StartService() {
        UTILS.startService(activity);
    }

    @SimpleFunction(description = "Stop the service")
    public void StopService() {
        UTILS.stopService(activity);
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
    @SimpleProperty
    public void StartWhenInterrupt(boolean bool) {
        startWhenInterrupt = bool;
    }

    @SimpleProperty
    public boolean StartWhenInterrupt() {
        return startWhenInterrupt;
    }

    @Override
    public void onDestroy() {
        if(startWhenInterrupt) {
            StartService();
        }
    }

    @Override
    public void onPause() {
        if(startWhenInterrupt) {
            StartService();
        }
    }
}
