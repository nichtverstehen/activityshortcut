package de.nichtverstehen;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import de.nichtverstehen.util.CustomizableArrayAdapter;
import de.nichtverstehen.util.Predicate;

import java.util.*;

public class ActivityShortcut extends Activity {

    Spinner appSpinner;
    Spinner activitySpinner;
    PackageManager packageManager;
    public final static String INSTALL_SHORTCUT_ACTION = "com.android.launcher.action.INSTALL_SHORTCUT";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        packageManager = getPackageManager();
        appSpinner = (Spinner) findViewById(R.id.app_spinner);
        activitySpinner = (Spinner) findViewById(R.id.activity_spinner);
        final Button testButton = (Button) findViewById(R.id.test_item);

        List<PackageInfo> apps = packageManager.getInstalledPackages(
                PackageManager.GET_ACTIVITIES | PackageManager.GET_INTENT_FILTERS);
        final ApplicationArrayAdapter appsListAdapter = new ApplicationArrayAdapter(this, apps);
        appSpinner.setAdapter(appsListAdapter);
        appSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PackageInfo appInfo = appsListAdapter.getItem(position);
                activitySpinner.setAdapter(new ActivityArrayAdapter(ActivityShortcut.this, appInfo.activities));
            }

            public void onNothingSelected(AdapterView<?> parent) {
                activitySpinner.setAdapter(null);
            }
        });

        testButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onTest();
            }
        });

        IntentFilter filter = new IntentFilter(INSTALL_SHORTCUT_ACTION);
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Intent shIntent = (Intent) intent.getExtras().get(Intent.EXTRA_SHORTCUT_INTENT);
                Log.e("Act", intent.toString());
            }
        }, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.done_item) {
            onDone();
            return true;
        }
        if (item.getItemId() == R.id.test_item) {
            onTest();
        }
        return super.onOptionsItemSelected(item);
    }

    ActivityInfo getCurrentActivity() {
        return (ActivityInfo) activitySpinner.getSelectedItem();
    }
    Intent makeIntent() {
        ActivityInfo activity = getCurrentActivity();
        if (activity == null)
            return null;

        Intent testIntent = new Intent(Intent.ACTION_MAIN);
        testIntent.setClassName(activity.packageName, activity.name);
        testIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return testIntent;
    }

    private void onDone() {
        ActivityInfo activity = getCurrentActivity();
        Intent shortcutIntent = makeIntent();
        if (activity == null || shortcutIntent == null)
            return;

        CharSequence activityLabel = packageManager.getApplicationLabel(activity.applicationInfo);

        final Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, activityLabel);
        try {
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(createPackageContext(activity.packageName, 0),
                    activity.applicationInfo.icon));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        intent.setAction(INSTALL_SHORTCUT_ACTION);
        sendBroadcast(intent);

        finish();
    }

    private void onTest() {
        Intent shortcutIntent = makeIntent();
        startActivity(shortcutIntent);
    }
}

class ApplicationArrayAdapter extends CustomizableArrayAdapter<PackageInfo> {
    PackageManager packageManager;

    private void init(Context context) {
        setDropDownViewResource(R.layout.app_row_dropdown);
        this.packageManager = context.getPackageManager();
    }

    @Override
    public void prepareItemView(View view, PackageInfo item) {
        TextView labelView = (TextView) view.findViewById(R.id.app_label);
        ImageView iconView = (ImageView) view.findViewById(R.id.app_icon);
        TextView descriptionView = (TextView) view.findViewById(R.id.app_descr);

        CharSequence label = getLabel(item);
        CharSequence description = packageManager.getText(item.packageName,
                item.applicationInfo.descriptionRes, item.applicationInfo);
        CharSequence packageName = item.packageName;
        Drawable icon = packageManager.getApplicationIcon(item.applicationInfo);

        String info = packageName + (description != null ? " " + description : "");

        labelView.setText(label);
        descriptionView.setText(info);
        iconView.setImageDrawable(icon);
    }

    public CharSequence getLabel(PackageInfo item) {
        return packageManager.getApplicationLabel(item.applicationInfo);
    }

    private Collection<PackageInfo> prepareObjects(List<PackageInfo> objects) {
        Collections.sort(objects, new Comparator<PackageInfo>() {
            public int compare(PackageInfo lhs, PackageInfo rhs) {
                if (lhs.applicationInfo.labelRes != 0 && rhs.applicationInfo.labelRes != 0) {
                    return getLabel(lhs).toString().compareTo(getLabel(rhs).toString());
                }
                else {
                    return 0;
                }
            }
        });
        return objects;
    }

    public ApplicationArrayAdapter(Context context) {
        super(context, R.layout.app_row);
        init(context);
    }

    public ApplicationArrayAdapter(Context context, PackageInfo[] objects) {
        super(context, R.layout.app_row);
        init(context);

        if (objects != null) {
            addAll(prepareObjects(Arrays.asList(objects)));
        }
    }

    public ApplicationArrayAdapter(Context context, List<PackageInfo> objects) {
        super(context, R.layout.app_row);
        init(context);
        if (objects != null) {
            addAll(prepareObjects(objects));
        }
    }
}

class ActivityArrayAdapter extends CustomizableArrayAdapter<ActivityInfo> {
    PackageManager packageManager;

    private void init(Context context) {
        setDropDownViewResource(R.layout.app_row_dropdown);
        this.packageManager = context.getPackageManager();
    }

    @Override
    public void prepareItemView(View view, ActivityInfo item) {
        TextView labelView = (TextView) view.findViewById(R.id.app_label);
        ImageView iconView = (ImageView) view.findViewById(R.id.app_icon);
        TextView descriptionView = (TextView) view.findViewById(R.id.app_descr);

        CharSequence label = getLabel(item);
        CharSequence description = packageManager.getText(item.packageName,
                item.descriptionRes, item.applicationInfo);
        CharSequence className = item.name;
        Drawable icon = packageManager.getDrawable(item.packageName,
                item.icon, item.applicationInfo);

        CharSequence title;
        CharSequence info;
        if (label != null) {
            title = label;
            info = className + (description != null ? " " + description : "");
        }
        else {
            title = className;
            info = description;
        }

        labelView.setText(title);

        descriptionView.setVisibility(info != null ? View.VISIBLE : View.GONE);
        descriptionView.setText(info);

        iconView.setVisibility(icon != null ? View.VISIBLE : View.GONE);
        iconView.setImageDrawable(icon);
    }

    public CharSequence getLabel(ActivityInfo activity) {
        return packageManager.getText(activity.packageName,
                activity.labelRes, activity.applicationInfo);
    }

    private Collection<ActivityInfo> prepareObjects(List<ActivityInfo> objects) {
        List<ActivityInfo> filtered = de.nichtverstehen.util.Collections.filter(
                objects,
                new Predicate<ActivityInfo>() {
                    public boolean apply(ActivityInfo item) {
                        return item.exported;
                    }
                });
        Collections.sort(filtered, new Comparator<ActivityInfo>() {
            public int compare(ActivityInfo lhs, ActivityInfo rhs) {
                if (lhs.labelRes != 0 && rhs.labelRes != 0) {
                    return getLabel(lhs).toString().compareTo(getLabel(rhs).toString());
                } else if (lhs.labelRes == 0 && rhs.labelRes == 0) {
                    return lhs.name.compareTo(rhs.name);
                } else {
                    return lhs.labelRes != 0 ? -1 : 1;
                }
            }
        });
        return filtered;
    }

    public ActivityArrayAdapter(Context context) {
        super(context, R.layout.app_row);
        init(context);
    }

    public ActivityArrayAdapter(Context context, ActivityInfo[] objects) {
        super(context, R.layout.app_row);
        init(context);

        if (objects != null) {
            addAll(prepareObjects(Arrays.asList(objects)));
        }
    }

    public ActivityArrayAdapter(Context context, List<ActivityInfo> objects) {
        super(context, R.layout.app_row);
        init(context);
        if (objects != null) {
            addAll(prepareObjects(objects));
        }
    }
}
