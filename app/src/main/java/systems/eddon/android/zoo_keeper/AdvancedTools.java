package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

public class AdvancedTools extends Activity {
    EditText Release;
    Button Restore;
    Button RestoreOnBoot;
    public static IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_tools);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ZooGate.ACTION_REPAINT);
        onNewIntent(this.getIntent());
    }

    @Override
    protected void onResume() {
        Log.d("onResume", "registerReceiver");
        registerReceiver(mIntentReceiver, mIntentFilter);
        adjustGui();
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d("onPause", "unregisterReceiver");
        unregisterReceiver(mIntentReceiver);
        super.onPause();
    }
    @Override
    protected void onNewIntent(Intent launchIntent) {
        if (launchIntent != null) {
            String cancelNotification = launchIntent.getStringExtra(ZooGate.EXTRA_CANCEL);
            if (cancelNotification != null)
                ((NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE))
                        .cancel(Integer.valueOf(cancelNotification));
        }
        adjustGui();
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("onReceive","Got intent!");
            onNewIntent(intent);
        }
    };

    private void adjustGui() {
        Log.d("adjustGui","Resetting Interface");
        Button snapshot = (Button) findViewById(R.id.snapshot_button);
        snapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
                intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.ACTION_BACKUP);
                ZooGate.myActivity.startService(intent);
            }
        });
        Button ReInstall = (Button) findViewById(R.id.reinstall_button);
        ReInstall.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View v) {
                                             ZooGate.popupMessage("Longpress to erase all Cron & ZooKeeper data");
                                         }
                                     }

        );
        ReInstall.setOnLongClickListener(new View.OnLongClickListener() {
                                             @Override
                                             public boolean onLongClick (View v){
                                                 Runnable p2 = new Runnable() {
                                                     public void run() {
                                                         ZooGate.popupMessage("Installing new files!");
                                                         ZooGate.installCron();
                                                     }
                                                 };
                                                 if (!ZooGate.cronlock) {
                                                     ZooGate.cronlock = true;
                                                     ZooGate.popupMessage("Erasing old data for reinstall");
                                                     ZooGate.runLocalRootCommand2("destroy.sh", p2);
                                                 }
                                                 return true;
                                             }
                                         }
        );

        Restore = (Button) findViewById(R.id.restorenow_button);
        RestoreOnBoot = (Button) findViewById(R.id.restoreonboot_button);

        File snapshotDate = new File(ZooGate.ACTUAL_SD_STORAGE, "ZooKeeper/snapshot/full-backup.txt");
        TextView BackupDate = (TextView) findViewById(R.id.backup_date);
        if(snapshotDate.exists())
        {
            BackupDate.setText(ZooGate.readFile(snapshotDate.getAbsolutePath()));

            Restore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ZooGate.popupMessage("Longpress to overwrite all your data!");
                }
            });
            Restore.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
                    intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.ACTION_RESTORE);
                    ZooGate.myActivity.startService(intent);
                    return true;
                }
            });
            Restore.setEnabled(true);
            Restore.setAlpha(1f);

            RestoreOnBoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ZooGate.popupMessage("Longpress to overwrite data on boot!");
                }
            });
            if (new File("/sdcard/ZooKeeper/restore-on-boot").exists()) {
                RestoreOnBoot.setEnabled(false);
                RestoreOnBoot.setAlpha(0.5f);
            }
            RestoreOnBoot.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Button b = (Button) v;
                    b.setEnabled(false);
                    b.setAlpha(0.5f);
                    ZooGate.popupMessage("OK! Delete /sdcard/ZooKeeper/restore-on-boot to abort");
                    ZooGate.writeRootFile("/sdcard/ZooKeeper/restore-on-boot", "1");
                    return true;
                }
            });
            RestoreOnBoot.setEnabled(true);
            RestoreOnBoot.setAlpha(1f);
        }
        else
        {
            BackupDate.setText("No Backup Found");

            Restore.setEnabled(false);
            Restore.setAlpha(0.5f);
            RestoreOnBoot.setEnabled(false);
            RestoreOnBoot.setAlpha(0.5f);
        }

        // TODO: Set a preference that we can do this?
        Button CleanInstall = (Button) findViewById(R.id.verifyclean_button);
        CleanInstall.setEnabled(false);
        CleanInstall.setAlpha(0.5f);

        ZooGate.releaseName=ZooGate.readFile("/system/etc/release");
        ZooGate.sp.edit().remove(ZooGate.PREF_FORCE_VERSION).apply();

        Release=(EditText) findViewById(R.id.force);
    }

    @Override
    public void onBackPressed() {
        String newRelease = Release.getText().toString().trim();
        if (newRelease.length() > 2) {
            ZooGate.releaseName = newRelease;
            ZooGate.sp.edit().putString(ZooGate.PREF_FORCE_VERSION, ZooGate.releaseName);
        }
        super.onBackPressed();
    }
}
