package de.nichtverstehen;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
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
import de.nichtverstehen.util.ListUtils;

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

        List<PackageInfo> apps = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        final ApplicationArrayAdapter appsListAdapter = new ApplicationArrayAdapter(this, apps);
        appSpinner.setAdapter(appsListAdapter);
        appSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                PackageInfo appInfo = appsListAdapter.getItem(position);
                if (appInfo.activities == null) {
                    onNothingSelected(parent);
                    return;
                }
                activitySpinner.setAdapter(new ActivityArrayAdapter(ActivityShortcut.this,
                        Arrays.asList(appInfo.activities)));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                activitySpinner.setAdapter(new ActivityArrayAdapter(ActivityShortcut.this,
                        new ArrayList<ActivityInfo>()));
            }
        });

        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTest();
            }
        });

        /*IntentFilter filter = new IntentFilter(INSTALL_SHORTCUT_ACTION);
        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                Intent shIntent = (Intent) intent.getExtras().get(Intent.EXTRA_SHORTCUT_INTENT);
                Log.e("Act", intent.toString());
            }
        }, filter);*/
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

class ResourceUtils {
    public static CharSequence getPackageString(PackageManager pm, String pkg, int resId, ApplicationInfo app) {
        if (resId == 0) return null;
        return pm.getText(pkg, resId, app);
    }
    public static Drawable getPackageImage(PackageManager pm, String pkg, int resId, ApplicationInfo app) {
        if (resId == 0) return null;
        return pm.getDrawable(pkg, resId, app);
    }
}

class ApplicationArrayAdapter extends CustomizableArrayAdapter<PackageInfo> {
    PackageManager packageManager;

    @Override
    public void prepareItemView(int position, PackageInfo item, View view) {
        TextView labelView = (TextView) view.findViewById(R.id.app_label);
        ImageView iconView = (ImageView) view.findViewById(R.id.app_icon);
        TextView descriptionView = (TextView) view.findViewById(R.id.app_descr);

        CharSequence label = getLabel(item);
        CharSequence description = ResourceUtils.getPackageString(packageManager, item.packageName,
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

    protected List<PackageInfo> prepareData(List<PackageInfo> objects) {
        Collections.sort(objects, new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo lhs, PackageInfo rhs) {
                CharSequence ll = getLabel(lhs), rl = getLabel(rhs);
                if (ll != null && rl != null) {
                    return ll.toString().compareTo(rl.toString());
                }
                else {
                    return lhs.packageName.compareTo(rhs.packageName);
                }
            }
        });
        return objects;
    }

    public ApplicationArrayAdapter(Context context, List<PackageInfo> objects) {
        super(context, new ArrayList<PackageInfo>());
        this.packageManager = context.getPackageManager();
        resetData(prepareData(objects));
        setViewResource(R.layout.app_row);
        setDropDownViewResource(R.layout.app_row_dropdown);
    }
}

class ActivityArrayAdapter extends CustomizableArrayAdapter<ActivityInfo> {
    PackageManager packageManager;

    @Override
    public void prepareItemView(int position, ActivityInfo item, View view) {
        TextView labelView = (TextView) view.findViewById(R.id.app_label);
        ImageView iconView = (ImageView) view.findViewById(R.id.app_icon);
        TextView descriptionView = (TextView) view.findViewById(R.id.app_descr);

        CharSequence label = getLabel(item);
        CharSequence description = getDescription(item);
        Drawable icon = getIcon(item);
        CharSequence className = item.name;

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
        return ResourceUtils.getPackageString(packageManager, activity.packageName,
                activity.labelRes, activity.applicationInfo);
    }

    public CharSequence getDescription(ActivityInfo activity) {
        return ResourceUtils.getPackageString(packageManager, activity.packageName,
                activity.descriptionRes, activity.applicationInfo);
    }

    public Drawable getIcon(ActivityInfo activity) {
        return ResourceUtils.getPackageImage(packageManager, activity.packageName,
                activity.icon, activity.applicationInfo);
    }

    protected List<ActivityInfo> prepareData(List<ActivityInfo> objects) {
        List<ActivityInfo> filtered = ListUtils.filter(objects,
                new ListUtils.Predicate<ActivityInfo>() {
                    @Override
                    public boolean apply(ActivityInfo item) {
                        return item.exported;
                    }
                });
        Collections.sort(filtered, new Comparator<ActivityInfo>() {
                @Override
                public int compare(ActivityInfo lhs, ActivityInfo rhs) {
                    CharSequence ll = getLabel(lhs), rl = getLabel(rhs);
                    if (ll != null && rl != null) {
                        return ll.toString().compareTo(rl.toString());
                    } else if (ll == null && rl == null) {
                        return lhs.name.compareTo(rhs.name);
                    } else {
                        return ll != null ? -1 : 1;
                    }
                }
            });
        return filtered;
    }

    public ActivityArrayAdapter(Context context, List<ActivityInfo> objects) {
        super(context, new ArrayList<ActivityInfo>());
        this.packageManager = context.getPackageManager();
        resetData(prepareData(objects));
        setViewResource(R.layout.app_row);
        setDropDownViewResource(R.layout.app_row_dropdown);
    }
}
