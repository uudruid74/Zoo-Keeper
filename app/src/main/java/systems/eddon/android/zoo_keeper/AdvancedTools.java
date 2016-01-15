package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

public class AdvancedTools extends Activity {
    EditText Release;
    Button Restore;
    Button RestoreOnBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advanced_tools);
        Button snapshot = (Button) findViewById(R.id.snapshot_button);
        snapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Runnable enableButtons = new Runnable() {
                    public void run() {
                        if (new File(ZooGate.ACTUAL_SD_STORAGE, "ZooKeeper/snapshot").listFiles().length > 4) {
                            Restore.setEnabled(true);
                            Restore.setAlpha(1f);
                            RestoreOnBoot.setEnabled(true);
                            RestoreOnBoot.setAlpha(1f);
                        }
                    }
                };
                ZooGate.popupMessage("Backing up all your data ...");
                ZooGate.readShellCommandNotify("5", "Backing up apps & data",
                        "su -c /data/media/0/ZooKeeper/backup.sh", enableButtons);
            }
        });
        Button ReInstall = (Button) findViewById(R.id.reinstall_button);
        ReInstall.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick (View v){
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

        File snapshotDir = new File(ZooGate.ACTUAL_SD_STORAGE, "ZooKeeper/snapshot");
        if(snapshotDir.exists() && snapshotDir.listFiles().length > 4)
        {
            Restore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ZooGate.popupMessage("Longpress to overwrite all your data!");
                }
            });
            Restore.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Runnable restoreComplete = new Runnable() {
                        public void run() {
                            Notify.showNotificationURL(ZooGate.myActivity,"5","Restoring Backup",
                                    "Restore Complete", null, null);
                        }
                    };
                    ZooGate.popupMessage("Set your phone down.  This will take awhile!");
                    ZooGate.readShellCommandNotify("5", "Restoring Backup",
                            "/data/media/0/ZooKeeper/restore.sh",
                            restoreComplete);
                    ZooGate.popupMessage("Data Restore Complete");
                    return true;
                }
            });

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
        }
        else
        {
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
