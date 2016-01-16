package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    TextView    Mirror;
    TextView    Directory;
    Switch      AllowMetered;
    Switch      AlwaysNotify;

    TextView    GpsNtp;
    String      CurrentGps;
    int         cancelId = 0;
    String      downloadURL;
    String      action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Intent launchIntent = this.getIntent();
        if (launchIntent != null)
            onNewIntent(launchIntent);
        else
            adjustGui();
    }
    private void adjustGui() {
        CurrentGps = ZooGate.readShellCommand("grep NTP_SERVER= /system/etc/gps.conf").trim().substring(11);

        Mirror = (TextView) findViewById(R.id.mirror_text);
        Mirror.setText(ZooGate.sp.getString(ZooGate.PREF_WILDLIFE_MIRROR, ZooGate.WILDLIFE_MIRROR_DEFAULT));
        Directory = (TextView) findViewById(R.id.download_text);
        Directory.setText(ZooGate.sp.getString(ZooGate.PREF_DOWNLOAD_DIR, ZooGate.DEF_DOWNLOAD));
        AllowMetered = (Switch) findViewById(R.id.metered_downloads);
        AllowMetered.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_ALLOW_METERED, false));
        AllowMetered.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ((isChecked) && (cancelId == 2)) {
                    ZooGate.sp.edit().putBoolean(ZooGate.PREF_ALLOW_METERED, isChecked).commit();
                    retryDownload();
                }
            }
        });
        AlwaysNotify = (Switch) findViewById(R.id.notify_on_nothing);
        AlwaysNotify.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_ALWAYS_NOTIFY, false));
        GpsNtp = (TextView) findViewById(R.id.gps_ntp_text);
        if (!CurrentGps.equals(getString(R.string.default_ntp_server))) {
            GpsNtp.setText(CurrentGps);
        }
    }

    public void retryDownload() {
        cancelId = 0;
        Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
        intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.ACTION_RESTART);
        intent.putExtra(ZooGate.EXTRA_CANCEL, "2");
        intent.putExtra(ZooGate.EXTRA_DESCR, action);
        intent.putExtra(ZooGate.EXTRA_URL, downloadURL);
        ZooGate.myActivity.startService(intent);
    }
    @Override
    protected void onNewIntent(Intent launchIntent) {
        if (launchIntent != null) {
            String cancelNotification = launchIntent.getStringExtra(ZooGate.EXTRA_CANCEL);
            if (cancelNotification != null) {
                cancelId = Integer.valueOf(cancelNotification);
                if (cancelId != 2) {
                    ((NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE))
                            .cancel(cancelId);
                } else {
                    downloadURL = launchIntent.getStringExtra(ZooGate.EXTRA_URL);
                    action = launchIntent.getStringExtra(ZooGate.EXTRA_ACTION);
                }
            }
        }
        adjustGui();
    }
    @Override
    public void onBackPressed() {
        SharedPreferences.Editor edit = ZooGate.sp.edit();

        if (cancelId == 2)
            retryDownload();

        // Download Mirror
        if (Mirror.getText().toString().trim().length() > 3) {
            ZooGate.WILDLIFE_MIRROR = Mirror.getText().toString().trim();
            ZooGate.updatePref(ZooGate.PREF_WILDLIFE_MIRROR, ZooGate.WILDLIFE_MIRROR);
        } else {
            edit.remove(ZooGate.PREF_WILDLIFE_MIRROR);
            ZooGate.WILDLIFE_MIRROR = ZooGate.WILDLIFE_MIRROR_DEFAULT;
        }

        // Destination Directory
        if (Directory.getText().toString().trim().length() > 1) {
            ZooGate.updatePref(ZooGate.PREF_DOWNLOAD_DIR, Directory.getText().toString().trim());
            ZooGate.USER_DIR = "/" + Directory.getText().toString().trim() + "/";
        } else {
            edit.remove(ZooGate.PREF_DOWNLOAD_DIR);
            ZooGate.USER_DIR = ZooGate.DEF_DOWNLOAD;
        }
        ZooGate.DOWNLOAD_DIR = ZooGate.SDCARD_DIR + ZooGate.USER_DIR;
        ZooGate.INSTALL_DIR = ZooGate.ACTUAL_SD_STORAGE + ZooGate.USER_DIR;

        // Switches
        edit.putBoolean(ZooGate.PREF_ALLOW_METERED, AllowMetered.isChecked());
        edit.putBoolean(ZooGate.PREF_ALWAYS_NOTIFY, AlwaysNotify.isChecked());

        // GPS
        String ntpserver = GpsNtp.getText().toString().trim();
        if (ntpserver.equals("")) {
            ntpserver=getString(R.string.default_ntp_server);
        }
        if (!ntpserver.equals(CurrentGps)) {
            Log.d("ntpserver", "ntpserver=["+ntpserver+"]  CurrentGps=[" +CurrentGps+"]");
            ZooGate.runShellCommand("su -c mount -o remount,rw /system");
            ZooGate.runShellCommand("su -c sed -E -i 's/NTP_SERVER=.+/NTP_SERVER="+ntpserver+"/' /system/etc/gps.conf");
            ZooGate.runShellCommand("su -c mount -o remount,ro /system");
        }
        edit.apply();
        super.onBackPressed();
    }
}
