package systems.eddon.android.zoo_keeper;

import android.app.Activity;

import android.app.ActionBar;
import android.app.Application;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class ZooGate extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * The URL name and other info to send to other activities via Intents
     */
    public static final String WILDLIFE_MIRROR_DEFAULT  = "https://eddon.systems/Download/WildLife/";
    public static final String ACTUAL_SD_STORAGE        = "/data/media/0";
    public static final String RECOVERY_SCRIPT          = "/cache/recovery/openrecoveryscript";
    public static final String DEF_DOWNLOAD             = "Download/OTA";

    public static       String WILDLIFE_MIRROR;
    public static       String DOWNLOAD_DIR;
    public static       String USER_DIR;
    public static       String SDCARD_DIR;
    public static       String INSTALL_DIR;

    public static final String CURRENT_ROM      = "WL-current-rom";
    public static final String ROM_UPDATE_URL   = "ROM/.meta/" + CURRENT_ROM;
    public static final String ROM_UPDATE_INF   = "ROM/.meta/";     // append current
    public static final String ROM_UPDATE_PRE   = "ROM/download/";  // append filename
    public static final String ROM_IMAGE_URL    = "Images/";

    public static String releaseName            = "Aardvark";    // getString is not static!

    /** These Intent data items use EXTRA_ACTION as the PREF that you want to
     *  request.  The NotifyDownloader will repond by setting the preference item
     *  to the data requested.
     */
    public static final String EXTRA_URL        = "URL";
    public static final String EXTRA_MESSAGE    = "Message";
    public static final String EXTRA_ACTION     = "Action";
    public static final String EXTRA_DESCR      = "Description";
    public static final String EXTRA_TYPE       = "Type";
    public static final String ACTION_UPGRADE   = "Upgrade";

    public static Handler handler;
    public static Activity myActivity;
    public static SharedPreferences sp;

    /**
     * Preferences
     */
    // the switch states
    public static final String PREF_USER_BLOCK_ADS      = "ad_block_master_switch";
    public static final String PREF_USER_AUTO_UPDATES   = "auto_update_master_switch";
    public static final String PREF_USER_CRON_SERVICES  = "cron_services_master_switch";
    // Download on check if available
    public static final String PREF_USER_DOWNLOAD       = "auto_download_switch";
    // Manage install script for recovery (and /etc/newrelease)
    public static final String PREF_UPDATE_RECOVERY     = "update_recovery_script";
    // NOTE: /etc/newrelease is written only after download complete and installer written
    // This is to facilitate alternate reboot strategies via other cron job
    // Auto reboot on completion
    public static final String PREF_UPDATE_AUTOREBOOT   = "update_auto_reboot";

    // User readable string to convert into chron time
    public static final String PREF_UPDATE_TIME         = "update_chron_time";
    // Date of last check
    public static final String PREF_LAST_CHECK_DATE     = "last_check_date";

    // last downloaded ROM
    public static final String PREF_FILE_ROM_UPDATE     = "rom_update_status";
    // release name of last ROM
    public static final String PREF_FILE_ROM_CHECK      = "rom_check_status";
    // next in the chain
    public static final String PREF_FILE_ROM_NEXT       = "rom_check_next";
    // and its MD5 file
    public static final String PREF_FILE_ROM_MD5        = "rom_md5_status";
    // and changelog file
    public static final String PREF_FILE_ROM_CHANGELOG  = "rom_changelog_status";

    // name of graphic for background
    public static final String PREF_CURR_IMAGE_NAME     = "curr_image_name";
    // name of graphic for next available
    public static final String PREF_LAST_IMAGE_NAME     = "last_image_name";

    // User Preferences
    public static final String PREF_WILDLIFE_MIRROR     = "mirror_url";
    public static final String PREF_DOWNLOAD_DIR        = "download_dir";

    // User pressed download button, override switches
    public static final String PREF_CHOICE_DOWNLOAD     = "download_override";
    public static final String PREF_FORCE_VERSION       = "force_version";
    public static final String PREF_ALLOW_METERED       = "allow_metered";

    public static final String ARG_SECTION_NUMBER       = "section_number";


    // quick updater routines
    public static void updatePref (String prefname, String newValue)
    {
        SharedPreferences.Editor edit =  sp.edit();
        edit.putString(prefname, newValue);
        edit.apply();
    }
    public static void updateSwitch (String prefname, boolean newValue)
    {
        SharedPreferences.Editor edit =  sp.edit();
        edit.putBoolean(prefname, newValue);
        edit.apply();
    }

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;
    public static int sectionNumber = 0;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int startPanel = 0;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zoo_gate);

        handler = new Handler();
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        ZooGate.releaseName = sp.getString(PREF_FORCE_VERSION, readFile("/system/etc/release"));
        sp.edit().remove(PREF_FORCE_VERSION).apply();

        // We'll have some UI for setting this later
        ZooGate.WILDLIFE_MIRROR = sp.getString(PREF_WILDLIFE_MIRROR, ZooGate.WILDLIFE_MIRROR_DEFAULT);
        ZooGate.SDCARD_DIR = Environment.getExternalStorageDirectory().toString();
        ZooGate.USER_DIR = "/" + sp.getString(PREF_DOWNLOAD_DIR, DOWNLOAD_DIR) + "/";
        ZooGate.DOWNLOAD_DIR = ZooGate.SDCARD_DIR + ZooGate.USER_DIR;
        ZooGate.INSTALL_DIR = ZooGate.ACTUAL_SD_STORAGE + ZooGate.USER_DIR;

        if (!ZooGate.releaseName.equals(
                sp.getString(ZooGate.PREF_CURR_IMAGE_NAME, getString(R.string.unknown_info)))) {
            Log.d("ZooGate.onCreate", "releaseName = " + ZooGate.releaseName + " !=  " +
                    sp.getString(ZooGate.PREF_CURR_IMAGE_NAME, getString(R.string.unknown_info)));
            spawnImageUpdate();
        } else {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            String filename = ZooGate.DOWNLOAD_DIR + releaseName + ".jpg";
            Bitmap bitmap = BitmapFactory.decodeFile(filename, options);
            if (new File(filename).exists()) {
                View myView = ZooGate.myActivity.findViewById(R.id.drawer_layout);
                myView.setBackground(new BitmapDrawable(ZooGate.myActivity.getResources(), bitmap));
            }
        }

        Intent launchIntent = this.getIntent();
        if (launchIntent != null) {
            String taskAction = launchIntent.getStringExtra(EXTRA_ACTION);
            if ((taskAction != null) && (taskAction.equals(ACTION_UPGRADE)))
                startPanel = 1;
        }
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
        installCron();
        onNavigationDrawerItemSelected(startPanel);
    }

    private void spawnImageUpdate() {
        Log.d("spawnImageUpdate", ZooGate.releaseName + ".jpg");
        if (!ZooGate.releaseName.equals("Aardvark")) {
            Intent intent = new Intent(this, NotifyDownloader.class);
            intent.putExtra(ZooGate.EXTRA_URL, ZooGate.WILDLIFE_MIRROR + ZooGate.ROM_IMAGE_URL
                    + ZooGate.releaseName + ".jpg");
            intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.PREF_CURR_IMAGE_NAME);
            intent.putExtra(ZooGate.EXTRA_DESCR, "Fetching background image");
            startService(intent);
        } else {
            View myView = ZooGate.myActivity.findViewById(R.id.drawer_layout);
            myView.setBackground(getResources().getDrawable(R.drawable.current_release,null));
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getFragmentManager();
        sectionNumber = position;
        Fragment selectedTask;
        switch (position) {
            case 1:
                selectedTask = UpdateManagerFragment.newInstance(position + 1);
                break;
            case 2:
                selectedTask = AdBlockerFragment.newInstance(position + 1);
                break;
            case 3:
                selectedTask = CronManagerFragment.newInstance(position + 1);
                break;
            case 4:
                selectedTask = AboutFragment.newInstance(position + 1);
                break;
            default:
                selectedTask = CurrentInfoFragment.newInstance(position + 1);
                break;
        }
        fragmentManager.beginTransaction()
                .replace(R.id.container, selectedTask)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = "WildLife Zoo-Keeper";
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
            case 4:
                mTitle = getString(R.string.title_section4) ;
                break;
            case 5:
                mTitle = getString(R.string.title_section5);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.zoo_gate, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, 69);
            return true;
        }
        if (id == R.id.action_help) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("https://eddon.systems/joomla/index.php/news-main/projects/zoo-keeper-app/31-zookeeper");
            intent.setData(uri);
            ZooGate.myActivity.startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        spawnImageUpdate();
        TextView temp = (TextView) findViewById(R.id.current_release);
        if (temp != null)
            temp.setText(ZooGate.releaseName);
        sp.edit().remove(PREF_FORCE_VERSION).apply();
    }

    public static void forceCron() {
        runShellCommand("su -c /system/etc/init.d/20crond restart");
        ZooGate.sp.edit().putBoolean(ZooGate.PREF_USER_CRON_SERVICES, true).apply();
    }

    public static void killCron() {
        runShellCommand("su -c /system/etc/init.d/20crond kill");
        ZooGate.sp.edit()
                .putBoolean(ZooGate.PREF_USER_CRON_SERVICES, false)
                .putBoolean(ZooGate.PREF_USER_AUTO_UPDATES, false)
                .apply();
    }

    public static View.OnKeyListener newBackButton = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                ZooGate z = (ZooGate) ZooGate.myActivity;
                z.onNavigationDrawerItemSelected(0);
                return true;
            }

            return false;
        }
    };

    public static void installCron() {
        Runnable part2 = new Runnable() {
            public void run() {
                File crondir = new File(ZooGate.SDCARD_DIR + "/Cron/");
                String log = crondir.getAbsolutePath() + "/cronlog.txt";

                if (!crondir.exists()) {
                    Log.d("installCron", "Creating " + crondir.toString());
                    crondir.mkdir();
                }
                File simple = new File(crondir,"simple");
                if (!simple.exists()) {
                    try {
                        FileOutputStream write = new FileOutputStream(simple);
                        StringBuffer sb = new StringBuffer();
                        sb.append("0 3 * * *    run-parts /system/etc/cron.daily >>"+log+" 2>&1\n");
                        sb.append("10 * * * *   run-parts /system/etc/cron.hourly >>"+log+" 2>&1\n");
                        sb.append("20 4 * * 1   run-parts /system/etc/cron.weekly >>"+log+" 2>&1\n");
                        sb.append("20 5 1 * *   run-parts /system/etc/cron.monthly >>"+log+" 2>&1\n");
                        write.write(sb.toString().getBytes(), 0, sb.length());
                        write.close();
                    }
                    catch (Exception e){
                        Log.e("installCron", e.getLocalizedMessage());
                    }
                }
                File update = new File(crondir,"systemupdate");
                if (!update.exists()) {
                    try {
                        FileOutputStream write = new FileOutputStream(update);
                        StringBuffer sb = new StringBuffer();
                        sb.append("5 2 * * *    am startservice -n "+
                        "\"systems.eddon.android.zoo_keeper/.NotifyDownloader\""+
                                " --es Action Upgrade >>"+log+" 2>&1");
                        write.write(sb.toString().getBytes(), 0, sb.length());
                        write.close();
                    }
                    catch (Exception e){
                        Log.e("installCron", e.getLocalizedMessage());
                    }
                }
                writeCronTab();
            }
        };

        File systemcrondir = new File("/system/etc/crontabs");
        if (!systemcrondir.exists()) {
            runLocalRootCommand2("installCron.sh", part2);
        } else {
            new Thread(part2).start();
        }
    }

    public static void writeCronTab() {
        Log.d("writeCrontab","Starting updates and refreshing cron!");
        File crondir = new File(ZooGate.SDCARD_DIR + "/Cron/");
        writeRootFile("/system/etc/timezone", CronManagerFragment.findTZS());
        File master = new File(crondir,"master");
        Log.d("writeCronTab", "master file is " + master.getAbsolutePath());
        try {
            FileOutputStream write = new FileOutputStream(master);
            StringBuffer sb = new StringBuffer();
            sb.append(ZooGate.readFile(ZooGate.SDCARD_DIR + "/Cron/simple").trim());
            sb.append("\n");
            sb.append(ZooGate.readFile(ZooGate.SDCARD_DIR + "/Cron/systemupdate").trim());
            sb.append("\n");
            sb.append(ZooGate.readFile(ZooGate.SDCARD_DIR + "/Cron/advanced").trim());
            sb.append("\n");
            write.write(sb.toString().getBytes(), 0, sb.length());
            write.close();
            forceCron();
        }
        catch (Exception e){
            Log.e("writeCronTab", e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
    public static String readFile(String file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            String ls = System.getProperty("line.separator");

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }

            return stringBuilder.toString().trim();
        }
        catch (Exception e)
        {
            Log.e("readFile", file + " - " + e.getLocalizedMessage() );
            return "";          // must stay NULL
        }
    }

    public static String readStream(InputStream reader) {
        try {
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            int count = 0;

            while ((count < 5) && (reader.available() <= 0)) {
                count++;
                try { Thread.sleep(200); } catch(Exception ex) {}
            }

            while(reader.available() > 0) {
                byte[] buffer = new byte[reader.available()];
                int read_count = reader.read(buffer);
                if ( read_count <= 0 ) break;
                Log.d("readStream", "Result: " + new String(buffer, "UTF8"));
                stringBuilder.append(new String(buffer, "UTF8"));
            }
            return stringBuilder.toString().trim();
        }
        catch (Exception e)
        {
            Log.e("readStream", e.getLocalizedMessage() );
            return "";          // NULL for consistency
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private static void copyAsset(String filename) {
        ContextWrapper contextWrapper = new ContextWrapper(ZooGate.myActivity.getBaseContext());
        File directory = contextWrapper.getDir("scripts", Context.MODE_PRIVATE);
        copyAsset(filename,directory,null);
    }

    private static void copyAsset(String filename, File directory, String destination) {
        AssetManager assetManager = ZooGate.myActivity.getAssets();
        try {
            if (destination == null) {
                destination = filename;
            }
            InputStream in = assetManager.open("scripts/" + filename);
            File outFile = new File(directory, destination);
            FileOutputStream out = new FileOutputStream(outFile);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            runShellCommand("/system/bin/chmod 755 " + directory.getAbsolutePath() + "/" + destination);
        } catch(IOException e) {
            Log.e("copyAsset", "Failed to copy asset file: " + filename, e);
        }
    }

    public static void popupMessage (final String errorText) {
        popupMessageTime(errorText, Toast.LENGTH_SHORT);
    }

    public static void popupMessageTime(final String errorText, final int Length) {
        if (myActivity == null)
            return;
        Log.d("popupMessage", myActivity.getClass().getSimpleName() + ":popupMessage() = " + errorText);

        if (myActivity.getClass().getSimpleName().equals("IntentService")) {
            Intent i = new Intent();
            i.setClass(myActivity, Notify.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            myActivity.startActivity(i);
        }
        Runnable doPopupMessage = new Runnable() {
            public void run() {
                Toast.makeText(myActivity.getApplicationContext(), errorText, Length).show();
            }
        };
        myActivity.runOnUiThread(doPopupMessage);
    }

    public static void runShellCommand(final String command) {
        Runnable thread = new Runnable() {
            public void run() {
                Process p;
                Log.d("runShellCommand", command);
                try {
                    p = Runtime.getRuntime().exec(command);
                    p.waitFor();
                }
                catch (Exception e) {
                    Log.e("runShellCommand", e.getLocalizedMessage());
                }
            }
        };
        new Thread(thread).start();
    }

    public static String readShellCommand(final String command) {
        Log.d("readShellCommand", command);
        try {
            int BUFF_LEN = 4096;
            Process p = Runtime.getRuntime().exec(command);
            char[] buffer = new char[BUFF_LEN];
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
            int exit = p.waitFor();
            int read;
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            Log.d("readShellCommand", "exit: " + exit + " returned: " + output);
            return output.toString();
        }
        catch (Exception e)
        {
            Log.e("readShellCommand", e.getLocalizedMessage());
            return "";
        }
    }

    public static void writeRootFile(final String filename, final String content) {
        Log.d("writeRootFile", filename + " length: " + content.length());
        Runnable thread = new Runnable() {
            public void run() {
                try {
                    Process p;
                    if (filename.contains("/system/")) {
                        p = Runtime.getRuntime().exec("su -c mount -o remount,rw /system");
                        p.waitFor();
                    }
                    p = Runtime.getRuntime().exec("su -c cat >" + filename);
                    OutputStream os = p.getOutputStream();
                    os.write(content.getBytes());
                    os.flush();
                    os.close();
                    p.waitFor();
                    if (filename.contains("/system/")) {
                        p = Runtime.getRuntime().exec("su -c mount -o remount,ro /system");
                        p.waitFor();
                    }
                } catch (Exception e) {
                    Log.e("writeRootFile", e.getLocalizedMessage());
                }
            }
        };
        new Thread(thread).start();
    }

    public static void appendRootFile(String filename, String content) {
        Process p;
        Log.d("appendRootFile", filename + " length: " + content.length());
        try {
            p = Runtime.getRuntime().exec("su -c cat >>"+filename);
            OutputStream os = p.getOutputStream();
            os.write(content.getBytes());
            os.flush();
            os.close();
            p.waitFor();
        }
        catch (Exception e)
        {
            Log.e("appendRootFile", e.getLocalizedMessage());
        }
    }

    public static void runLocalRootCommand(final String command) {
        runLocalRootCommand2(command, null);
    }
    public static void runLocalRootCommand2(final String command, final Runnable chain) {
        Log.d("runLocalRootCommand", command);
        Runnable thread = new Runnable() {
            public void run() {
                StringBuilder sb = new StringBuilder();
                sb.append("su -c ");
                ContextWrapper contextWrapper = new ContextWrapper(ZooGate.myActivity.getBaseContext());
                File directory = contextWrapper.getDir("scripts", Context.MODE_PRIVATE);
                sb.append(directory.getAbsolutePath());
                sb.append("/");
                sb.append(command);
                if (!(new File(sb.toString()).exists()))
                    copyAsset(command);
                runShellCommand(sb.toString());
                if (chain != null) {
                    chain.run();
                }
            }
        };
        new Thread(thread).start();
    }

    public static void rebootRecovery() {
        StringBuilder sb = new StringBuilder();
        sb.append ("su -c reboot recovery");
        runShellCommand(sb.toString());
    }

}
