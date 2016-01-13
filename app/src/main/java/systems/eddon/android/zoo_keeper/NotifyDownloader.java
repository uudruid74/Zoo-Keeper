package systems.eddon.android.zoo_keeper;

import android.app.DownloadManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;


/**
 * Created by ekl on 12/11/15.
 */
public class NotifyDownloader extends IntentService {

    private String url;
    private String extra_action;
    private String description;
    private static boolean unregistered = true;

    public static DownloadManager dm;
    public static SharedPreferences sp;

    private static HashMap<Long,String>    IdToPREF = new HashMap();
    private static HashSet<String>         DownloadFileSet;     // Pending set
    private static ArrayList<String>       DownloadFileList;    // Complete

    private static int      SuccessCount = 0;
    private static int      FailureCount = 0;

    public NotifyDownloader() {
        super("NotifyDownloader");
    }

    // match download ID to extra_actions
    final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
                long downloadId = intent.getLongExtra(
                        DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                if (!IdToPREF.containsKey(downloadId))
                    return;
                Log.d("onReceive", "Download OK.  ID=" + downloadId);
                DownloadManager.Query query = new DownloadManager.Query();
                //query.setFilterById(enqueue);
                Cursor c = dm.query(query);
                if (c.moveToFirst()) {
                    int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    Log.d("DM Sample", "Status Check: " + status);
                    switch (status) {
                        case DownloadManager.STATUS_PAUSED:
                        case DownloadManager.STATUS_PENDING:
                        case DownloadManager.STATUS_RUNNING:
                            break;
                        case DownloadManager.STATUS_SUCCESSFUL:
                            try {
                                ParcelFileDescriptor file = dm.openDownloadedFile(downloadId);
                                FileInputStream fis = new ParcelFileDescriptor.AutoCloseInputStream(file);
                                fileSuccessful(downloadId, fis);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case DownloadManager.STATUS_FAILED:
                            FailureCount++;
                            ZooGate.popupMessage("FAIL! Can't download this url:\n" +
                                c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI)));
                            notifyDownloadFail();
                            break;
                    }
                }
                c.close();
                IdToPREF.remove(downloadId);
                // If Empty, wait 5s and enable Check?, unregister receiver?
            }
        }
    };

    @Override
    protected void onHandleIntent(Intent intent) {
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        // Just in case we're called without the main app running, may not be necessary
        ZooGate.WILDLIFE_MIRROR = sp.getString(ZooGate.PREF_WILDLIFE_MIRROR, ZooGate.WILDLIFE_MIRROR_DEFAULT);
        ZooGate.SDCARD_DIR = Environment.getExternalStorageDirectory().toString();
        ZooGate.USER_DIR = "/" + sp.getString(ZooGate.PREF_DOWNLOAD_DIR, ZooGate.DEF_DOWNLOAD) + "/";
        ZooGate.DOWNLOAD_DIR = ZooGate.SDCARD_DIR + ZooGate.USER_DIR;
        ZooGate.INSTALL_DIR = ZooGate.ACTUAL_SD_STORAGE + ZooGate.USER_DIR;

        if (unregistered) {
            Log.d("onHandleIntent", "Registering Broadcast Receiver");
            ZooGate.myActivity.registerReceiver(receiver, new IntentFilter(
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            unregistered = false;
        }
        extra_action = intent.getStringExtra(ZooGate.EXTRA_ACTION);
        url = intent.getStringExtra(ZooGate.EXTRA_URL);
        description = intent.getStringExtra(ZooGate.EXTRA_DESCR);

        if (extra_action.equals(ZooGate.ACTION_UPGRADE)) {
            NavigationDrawerFragment.fetchUpdate();
        } else {
            dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            enqueueDownload();
        }
    }

    private void fileSuccessful (long downloadId, FileInputStream in) {
        final String prefname = IdToPREF.get(downloadId);
        if (prefname.equals(ZooGate.PREF_FILE_ROM_CHECK)) {
            final String nextRel = ZooGate.readStream(in).trim();
            final String checkTime = new Date().toString();
            // Update Preferences for record keeping
            ZooGate.updatePref(ZooGate.PREF_FILE_ROM_CHECK, nextRel);
            ZooGate.updatePref(ZooGate.PREF_LAST_CHECK_DATE, checkTime);
            // Update Check Time
            if (ZooGate.sectionNumber == 1) {
                Runnable updateTv = new Runnable() {
                    public void run() {
                        TextView tv = (TextView) ZooGate.myActivity.findViewById(R.id.last_check_date);
                        tv.setText(checkTime);
                    }
                };
                ZooGate.myActivity.runOnUiThread(updateTv);
            }
            // Update last Release name if changed
            Log.d("PREF_FILE_ROM_CHECK", "Now on " + ZooGate.releaseName + " but available is " + nextRel);
            if (!nextRel.equals(ZooGate.releaseName)) {
                notifyDownloadAvail();
                if (ZooGate.sectionNumber == 1) {
                    Runnable updateTv = new Runnable() {
                        public void run() {
                            TextView tv = (TextView) ZooGate.myActivity.findViewById(R.id.now_available);
                            tv.setText(nextRel);
                        }
                    };
                    ZooGate.myActivity.runOnUiThread(updateTv);
                }
                // Start the download chain
                if (sp.getBoolean(ZooGate.PREF_USER_DOWNLOAD, true)) {
                    startDownloadChain();
                }
            } else {
                ZooGate.popupMessage("Sorry, try again tomorrow!");
            }
            // Determine if "available" image icon needs updating
            if (!nextRel.equals(sp.getString(ZooGate.PREF_LAST_IMAGE_NAME,"Aardvark"))) {
                UpdateManagerFragment.spawnImageUpdate(sp);
            }
        }
        // Updates "available" icon
        else if (prefname.equals(ZooGate.PREF_LAST_IMAGE_NAME)) {
            if (ZooGate.sectionNumber == 1) {
                final Bitmap image = BitmapFactory.decodeStream(in);
                Runnable updateImage = new Runnable() {
                    public void run() {
                        Log.d("PREF_LAST_IMAGE_NAME", "New available image file for ROM: " + sp.getString(ZooGate.PREF_LAST_IMAGE_NAME,"error"));
                        ImageView myView = (ImageView) ZooGate.myActivity.findViewById(R.id.avail_release_image);
                        if (myView != null)
                            myView.setImageDrawable(new BitmapDrawable(ZooGate.myActivity.getResources(), image));
                    }
                };
                ZooGate.myActivity.runOnUiThread(updateImage);
            }
        }
        // Updates "background" for current release
        else if (prefname.equals(ZooGate.PREF_CURR_IMAGE_NAME)) {
            if ((ZooGate.myActivity != null) &&
                        (!ZooGate.myActivity.getClass().getSimpleName().equals("IntentService"))) {
                final Bitmap image = BitmapFactory.decodeStream(in);
                Log.d("PREF_CURR_IMAGE_NAME", "New background file for ROM: " + ZooGate.releaseName);
                ZooGate.updatePref(ZooGate.PREF_CURR_IMAGE_NAME, ZooGate.releaseName);
                Runnable update = new Runnable() {
                    public void run() {
                        View myView = ZooGate.myActivity.findViewById(R.id.drawer_layout);
                        if (myView != null)
                            myView.setBackground(
                                    new BitmapDrawable(ZooGate.myActivity.getResources(), image)
                            );
                    }
                };
                ZooGate.myActivity.runOnUiThread(update);
            }
        }
        else if (prefname.equals(ZooGate.PREF_FILE_ROM_NEXT)) {
            final String lastRel = sp.getString (ZooGate.PREF_FILE_ROM_CHECK, "Aardvark");
            final String oldRel = sp.getString(ZooGate.PREF_FILE_ROM_NEXT, "Aardvark");
            final String newRel = ZooGate.readStream(in).trim();
            final String filename = "Update-"+oldRel+"-to-"+newRel+".zip";
            Log.d("PREF_FILE_ROM_NEXT", "Need to fetch " + filename);
            DownloadFileList.add(filename);
            if (sp.getBoolean(ZooGate.PREF_USER_DOWNLOAD, true)
                        || sp.getBoolean(ZooGate.PREF_CHOICE_DOWNLOAD, true)) {
                startDownloadRom(filename);
            }
            // If chain not complete
            if (!newRel.equals(lastRel)) {
                Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
                intent.putExtra(ZooGate.EXTRA_URL, ZooGate.WILDLIFE_MIRROR + ZooGate.ROM_UPDATE_INF
                        + newRel );
                intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.PREF_FILE_ROM_NEXT);
                intent.putExtra(ZooGate.EXTRA_DESCR, "Following Update Chains");
                ZooGate.updatePref(ZooGate.PREF_FILE_ROM_NEXT, newRel); // save the "from", fetch "to"
                ZooGate.myActivity.startService(intent);
                new File(ZooGate.DOWNLOAD_DIR + newRel).deleteOnExit();
            }
        }
        else if (prefname.equals(ZooGate.PREF_FILE_ROM_UPDATE)) {
            final String complete = sp.getString (ZooGate.PREF_FILE_ROM_UPDATE, "Aardvark");
            Log.d("PREF_FILE_ROM_CHECK", "Starting MD5 download for " + complete);
            startDownloadMD5(complete);
        }
        else if (prefname.equals(ZooGate.PREF_FILE_ROM_MD5)) {
            final String downloadMD5 = ZooGate.readStream(in).substring(0,32);
            final String zipfilename = sp.getString(ZooGate.PREF_FILE_ROM_MD5, "Aardvark");
            Runnable checkMD5Sum = new Runnable() {
                public void run() {
                    String filename = ZooGate.DOWNLOAD_DIR + zipfilename;
                    String calcMD5Sum = "";
                    try {
                        calcMD5Sum = ZooGate.readShellCommand("md5sum " + filename).substring(0, 32);
                    }
                    catch (Exception e) {
                        Log.e("PREF_FILE_ROM_MD5", e.getLocalizedMessage());
                    }
                    final String userNoticeMatch = "MD5 Sums Match!";
                    final String userNoticeFail = "MD5 Sums Fail!";

                    if (calcMD5Sum.equals(downloadMD5)) {
                        Log.d("PREF_FILE_ROM_MD5", filename + ": " + userNoticeMatch + " [" + downloadMD5 + "]");
                        SuccessCount++;
                        if (sp.getBoolean(ZooGate.PREF_UPDATE_RECOVERY, true))
                            checkLastROM(zipfilename);
                    } else {
                        ZooGate.popupMessage(filename + ": " + userNoticeFail);
                        Log.d("PREF_FILE_ROM_MD5", "["+calcMD5Sum+"] vs [" + downloadMD5+"]");
                        FailureCount++;
                        ZooGate.updateSwitch(ZooGate.PREF_CHOICE_DOWNLOAD, false);
                        notifyDownloadFail();
                    }
                }
            };
            new Thread(checkMD5Sum).start();
        }
        else if (prefname.equals(ZooGate.PREF_FILE_ROM_CHANGELOG)) {
            String filename = sp.getString(ZooGate.PREF_FILE_ROM_CHECK, "Aardvark") + ".changelog";
            File log = new File(ZooGate.DOWNLOAD_DIR + filename);
            Log.d("PREF_FILE_ROM_CHANGELOG", log.getAbsolutePath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.fromFile(log);
            intent.setDataAndType(uri, "text/plain");
            ZooGate.myActivity.startActivity(intent);
        }
    }

    public static void fetchRomChangelog() {
        Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
        intent.putExtra(ZooGate.EXTRA_URL, ZooGate.WILDLIFE_MIRROR + ZooGate.ROM_UPDATE_INF
                + ZooGate.sp.getString(ZooGate.PREF_FILE_ROM_CHECK, "Aardvark") + ".changelog");
        intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.PREF_FILE_ROM_CHANGELOG);
        intent.putExtra(ZooGate.EXTRA_DESCR, "Fetching ROM Changelog");
        ZooGate.myActivity.startService(intent);
    }

    public static void startDownloadChain () {
        DownloadFileSet = new HashSet<String>();
        DownloadFileList = new ArrayList<String>();
        SuccessCount = FailureCount = 0;
        Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
        intent.putExtra(ZooGate.EXTRA_URL, ZooGate.WILDLIFE_MIRROR + ZooGate.ROM_UPDATE_INF
                + ZooGate.releaseName );
        intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.PREF_FILE_ROM_NEXT);
        // save the "from", fetch "to"
        ZooGate.updatePref(ZooGate.PREF_FILE_ROM_NEXT, ZooGate.releaseName);
        intent.putExtra(ZooGate.EXTRA_DESCR, "Checking Update Chains");
        ZooGate.myActivity.startService(intent);
        new File(ZooGate.DOWNLOAD_DIR + ZooGate.CURRENT_ROM).deleteOnExit();
    }

    private static void startDownloadRom (String filename) {
        Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
        intent.putExtra(ZooGate.EXTRA_URL, ZooGate.WILDLIFE_MIRROR + ZooGate.ROM_UPDATE_PRE
                + filename );
        intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.PREF_FILE_ROM_UPDATE);
        ZooGate.updatePref(ZooGate.PREF_FILE_ROM_UPDATE, filename); // save the filename
        intent.putExtra(ZooGate.EXTRA_DESCR, "Downloading ROM Update");
        ZooGate.myActivity.startService(intent);
    }

    private static void startDownloadMD5 (String filename) {
        Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
        intent.putExtra(ZooGate.EXTRA_URL, ZooGate.WILDLIFE_MIRROR + ZooGate.ROM_UPDATE_PRE
                + filename + ".md5" );
        intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.PREF_FILE_ROM_MD5);
        ZooGate.updatePref(ZooGate.PREF_FILE_ROM_MD5, filename); // save the filename
        intent.putExtra(ZooGate.EXTRA_DESCR, "Fetching MD5 for ROM");
        ZooGate.myActivity.startService(intent);
    }

    private void startRecoveryUpdates() {
        Runnable updateRecoveryScript = new Runnable() {
            @Override
            public void run() {
                Log.d("updateRecoveryScript", "Recovery will flash " + DownloadFileList.size() + " files");
                StringBuilder sb = new StringBuilder(); // ZooGate.RECOVERY_SCRIPT
                sb.append("mount /data\n");
                for (String filename: DownloadFileList) {
                    Log.d("updateRecoveryScript", "install "+filename);
                    sb.append("install " +
                            ZooGate.ACTUAL_SD_STORAGE + ZooGate.USER_DIR + filename + "\n");
                }
                sb.append("umount /data\n"
                    + "cmd rm " + ZooGate.RECOVERY_SCRIPT + "\n"
                    + "cmd rm /cache/recovery/command\n"
                //    + "wipe cache\n"
                );

                // If doing something weird, wipe cache and dalvik-cache
                if (!ZooGate.releaseName.equals(ZooGate.readFile("/system/etc/release"))) {
                    sb.append("wipe cache\nwipe dalvik\n");
                }
                ZooGate.writeRootFile(ZooGate.RECOVERY_SCRIPT, sb.toString());
                if (sp.getBoolean(ZooGate.PREF_UPDATE_AUTOREBOOT, false)) {
                    ZooGate.rebootRecovery();
                } else {
                    notifyRebootReady();
                }
            }
        };

        if ((!DownloadFileList.isEmpty()) && (SuccessCount > 0) && (FailureCount == 0)) {
            if (sp.getBoolean(ZooGate.PREF_UPDATE_RECOVERY, true)) {
                new Thread(updateRecoveryScript).start();
            } else {
                notifyDownloadAvail();
            }
        } else {
            notifySafetyFailure();
        }
    }

    private void checkLastROM (String zipname) {
        DownloadFileSet.remove(zipname);
        if (DownloadFileSet.isEmpty()) {
            ZooGate.popupMessage("All files downloaded successfully!");
            ZooGate.updateSwitch(ZooGate.PREF_CHOICE_DOWNLOAD, false);
            startRecoveryUpdates();
        }
    }

    // Need to save download ID here and test for it up top.
    private void enqueueDownload() {
        Uri thisUri = Uri.parse(url);
        String filename = thisUri.getLastPathSegment();
        String absolutefilename;

        // Silently skip these and use built-ins.
        if (filename.equals("Aardvark.jpg"))
            return;

        absolutefilename = ZooGate.DOWNLOAD_DIR + filename;
        Log.d("enqueueDownload",  url + " to " + ZooGate.DOWNLOAD_DIR + " as " + absolutefilename);

        // FIXME: Should we check for dup filenames?
        if (((DownloadFileSet != null) && DownloadFileSet.contains(filename)) ||
                    ((DownloadFileList != null) && (DownloadFileList.contains(filename)))) {
            Log.d("enqueueDownload", filename + " already pending");
        }
        /**
         * This bit is to fake success when file already exists
         */
        if (new File(absolutefilename).exists()) {
            Log.d("enqueueDownload","File exists - skipping!");
            long downloadId = 1000 + new Random().nextInt();
            IdToPREF.put(downloadId, extra_action);
            try {
                FileInputStream fis = new FileInputStream(new File(absolutefilename));
                fileSuccessful(downloadId, fis);
            }
            catch (Exception e) {
                Log.e("fileSuccessful", e.getLocalizedMessage());
            }
            IdToPREF.remove(downloadId);
            return;
        }

        // FIXME: Here is a great place to disable Check button
        File downloaddir = new File(Environment.getExternalStorageDirectory()+ ZooGate.USER_DIR);
        if (!downloaddir.exists())
            downloaddir.mkdirs();

        int Visible = DownloadManager.Request.VISIBILITY_VISIBLE;
        if (filename.endsWith(".zip"))
            Visible = DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED;

        Log.d("enqueueDownload", extra_action + " Description: " + description + " to " +ZooGate.USER_DIR + filename );
        DownloadFileSet.add(filename);
        DownloadFileList.add(filename);
        IdToPREF.put(dm.enqueue(new DownloadManager.Request(thisUri)
                        .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
                                DownloadManager.Request.NETWORK_MOBILE)
                        .setAllowedOverRoaming(false)
                        .setAllowedOverMetered(
                                ZooGate.sp.getBoolean(ZooGate.PREF_ALLOW_METERED, false) |
                                extra_action.equals(ZooGate.PREF_LAST_IMAGE_NAME) |
                                extra_action.equals(ZooGate.PREF_CURR_IMAGE_NAME) |
                                        ZooGate.sp.getBoolean(ZooGate.PREF_CHOICE_DOWNLOAD, false))
                        .setTitle(getString(R.string.app_name) + ": " + filename)
                        .setDescription(description)
                        .setNotificationVisibility(Visible)
                        .setDestinationInExternalPublicDir(ZooGate.USER_DIR, filename)),
                extra_action);
    }

    private void notifyDownloadFail() {
        notificationCreate("Download Failure of " + FailureCount + " files!", R.drawable.ic_fail, 01);
    }
    private void notifyDownloadAvail() {
        notificationCreate(getString(R.string.new_rom_available), R.drawable.ic_cloud_avail, 02);
    }
    private void notifyRebootReady() {
        notificationCreate("A New ROM Ready to Install!", R.drawable.ic_cloud_avail, 02);
    }
    private void notifySafetyFailure() {
        notificationCreate("Safety checks failed.  Recovery untouched.  S:"+SuccessCount+" F:"+FailureCount, R.drawable.ic_stop, 04);
    }
    private void notificationCreate(String Message, int icon, int mNotificationId) {
        Log.d("notificationCreate", "String: " + Message);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(icon)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(Message);

        Intent resultIntent = new Intent(this, ZooGate.class);
        resultIntent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.ACTION_UPGRADE);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
            this,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );

        mBuilder.setContentIntent(resultPendingIntent);
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }
}
