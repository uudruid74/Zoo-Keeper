package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    TextView    Mirror;
    TextView    Directory;
    Switch      AllowMetered;
    Switch      AlwaysNotify;

    TextView    Release;
    TextView    GpsNtp;
    String      CurrentGps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        CurrentGps = ZooGate.readShellCommand("grep NTP_SERVER= /system/etc/gps.conf").trim().substring(11);

        Mirror = (TextView) findViewById(R.id.mirror_text);
        Mirror.setText(ZooGate.sp.getString(ZooGate.PREF_WILDLIFE_MIRROR, ZooGate.WILDLIFE_MIRROR_DEFAULT));
        Directory = (TextView) findViewById(R.id.download_text);
        Directory.setText(ZooGate.sp.getString(ZooGate.PREF_DOWNLOAD_DIR, ZooGate.DEF_DOWNLOAD));
        AllowMetered = (Switch) findViewById(R.id.metered_downloads);
        AllowMetered.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_ALLOW_METERED, false));
        AlwaysNotify = (Switch) findViewById(R.id.notify_on_nothing);
        AlwaysNotify.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_ALWAYS_NOTIFY, false));
        Release = (TextView) findViewById(R.id.force_text);
        GpsNtp = (TextView) findViewById(R.id.gps_ntp_text);
        if (!CurrentGps.equals(getString(R.string.default_ntp_server))) {
            GpsNtp.setText(CurrentGps);
        }
    }

    @Override
    public void onBackPressed() {
        SharedPreferences.Editor edit = ZooGate.sp.edit();

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

        // Force current release
        if (Release.getText().toString().trim().length() > 2) {
            ZooGate.releaseName = Release.getText().toString().trim();
            edit.putString(ZooGate.PREF_FORCE_VERSION, ZooGate.releaseName);
        } else {
            ZooGate.releaseName = ZooGate.readFile("/system/etc/release");
            edit.remove(ZooGate.PREF_FORCE_VERSION);
        }

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
