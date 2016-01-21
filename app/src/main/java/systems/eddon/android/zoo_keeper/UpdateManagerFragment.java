package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by ekl on 12/9/15.
 */
public class UpdateManagerFragment extends Fragment {
    private Button FetchNow;
    private Button RebootNow;
    private Button ChangeLog;
    private Switch InstallRecovery;
    private Switch AutoReboot;
    private Switch AutoUpdateMaster;
    private Switch AutoDownload;
    private TextView NowAvailable;
    private TextView LastChecked;
    private EditText CheckTime;
    private ImageView NextReleaseImage;

    private String nextRelease;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static UpdateManagerFragment newInstance(int sectionNumber) {
        UpdateManagerFragment fragment = new UpdateManagerFragment();
        Bundle args = new Bundle();
        args.putInt(ZooGate.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onDetach() {
        updateChronData();
        super.onDetach();
    }

    public UpdateManagerFragment() {
    }

    public static void spawnImageUpdate(SharedPreferences sp) {
        String lastRom = sp.getString(ZooGate.PREF_FILE_ROM_CHECK, "Aardvark");
        String lastImage = sp.getString(ZooGate.PREF_LAST_IMAGE_NAME, "");
        Log.d("spawnImageUpdate", "lastRom: " + lastRom + " lastImage: " + lastImage);
        if (!lastRom.equals(lastImage)) {
            Log.d("spawnImageUpdate", "Creating Intent to fetch image");
            Intent intent = new Intent(ZooGate.myActivity, NotifyDownloader.class);
            intent.putExtra(ZooGate.EXTRA_URL, ZooGate.WILDLIFE_MIRROR + ZooGate.ROM_IMAGE_URL
                    + lastRom + ".jpg");
            // Bug: If we fail, we never update image?
            ZooGate.updatePref(ZooGate.PREF_LAST_IMAGE_NAME, lastRom);
            intent.putExtra(ZooGate.EXTRA_ACTION, ZooGate.PREF_LAST_IMAGE_NAME);
            intent.putExtra(ZooGate.EXTRA_DESCR, "Fetching Image");
            ZooGate.myActivity.startService(intent);
        }
    }

    private void setButtonStates() {
        nextRelease = ZooGate.sp.getString(ZooGate.PREF_FILE_ROM_CHECK, getString(R.string.unknown_info));
        AutoUpdateMaster.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_USER_AUTO_UPDATES, true));
        NowAvailable.setText(nextRelease);
        Log.d("setButtonStates", "nextRelease: " + nextRelease);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        String filename = ZooGate.DOWNLOAD_DIR + nextRelease + ".jpg";
        if (new File(filename).exists()) {
            Log.d("setButtonStates","File: " + filename + " already exists");
            Bitmap image = BitmapFactory.decodeFile(filename, options);
            NextReleaseImage.setImageDrawable(new BitmapDrawable(ZooGate.myActivity.getResources(), image));
        } else {
            spawnImageUpdate(ZooGate.sp);
        }
        LastChecked.setText(ZooGate.sp.getString(ZooGate.PREF_LAST_CHECK_DATE, getString(R.string.unknown_date)));
        CheckTime.setText(ZooGate.sp.getString(ZooGate.PREF_UPDATE_TIME, "2"));
        if (AutoUpdateMaster.isChecked())
            ZooGate.forceCron();
        AutoDownload.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_USER_DOWNLOAD, true));
        InstallRecovery.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_UPDATE_RECOVERY, true));
        AutoReboot.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_UPDATE_AUTOREBOOT, false));
        if (!InstallRecovery.isChecked())
            AutoReboot.setChecked(false);
        if ((!AutoUpdateMaster.isChecked()) || (NowAvailable.equals(ZooGate.releaseName))) {
            FetchNow.setText(getString(R.string.action_example));
            FetchNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ZooGate.popupMessage("Checking For Updates ...");
                    ZooGate.updateSwitch(ZooGate.PREF_CHOICE_DOWNLOAD, false);
                    NavigationDrawerFragment.fetchUpdate();
                }
            });
        } else {
            FetchNow.setText(getString(R.string.fetch_rom));
            FetchNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ZooGate.popupMessage("Attempting Upgrade ...");
                    ZooGate.updateSwitch(ZooGate.PREF_CHOICE_DOWNLOAD, true);
                    NavigationDrawerFragment.fetchUpdate();
                }
            });
        }
        if (ZooGate.sp.getString(ZooGate.PREF_FILE_ROM_CHECK,"").equals(ZooGate.releaseName)) {
            ChangeLog.setAlpha(0.5f);
        } else {
            ChangeLog.setAlpha(1.0f);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_update_manager, container, false);
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(ZooGate.newBackButton);

        ZooGate.sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        ZooGate.updateSwitch(ZooGate.PREF_CHOICE_DOWNLOAD, false);

        AutoUpdateMaster = (Switch) rootView.findViewById(R.id.update_switch);
        AutoUpdateMaster.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ZooGate.updateSwitch(ZooGate.PREF_USER_AUTO_UPDATES, isChecked);
                ZooGate.forceCron();
                setButtonStates();
            }
        });
        NowAvailable = (TextView) rootView.findViewById(R.id.now_available);
        LastChecked = (TextView) rootView.findViewById(R.id.last_check_date);
        CheckTime = (EditText) rootView.findViewById(R.id.check_time);
        CheckTime.setText(ZooGate.sp.getString(ZooGate.PREF_UPDATE_TIME,"2"));
        CheckTime.setOnKeyListener(ZooGate.newBackButton);

        FetchNow = (Button) rootView.findViewById(R.id.button_fetch);
        AutoDownload = (Switch) rootView.findViewById(R.id.download_switch);
        AutoDownload.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ZooGate.updateSwitch(ZooGate.PREF_USER_DOWNLOAD, isChecked);
                setButtonStates();
            }
        });
        ChangeLog = (Button) rootView.findViewById(R.id.button_changelog);
        ChangeLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZooGate.popupMessage("Fetching Changelog ...");
                NotifyDownloader.fetchRomChangelog();
            }
        });
        RebootNow = (Button) rootView.findViewById(R.id.button_reboot);
        RebootNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZooGate.popupMessage("Longpress button to reboot!");
            }
        });
        RebootNow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ZooGate.popupMessage("Rebooting to Recovery ...");
                ZooGate.rebootRecovery();
                return true;
            }
        });
        InstallRecovery = (Switch) rootView.findViewById(R.id.install_after_fetch_switch);
        InstallRecovery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ZooGate.updateSwitch(ZooGate.PREF_UPDATE_RECOVERY, isChecked);
                setButtonStates();
            }
        });
        AutoReboot = (Switch) rootView.findViewById(R.id.auto_reboot_switch);
        AutoReboot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ZooGate.updateSwitch(ZooGate.PREF_UPDATE_AUTOREBOOT, isChecked);
                setButtonStates();
            }
        });
        NextReleaseImage = (ImageView) rootView.findViewById (R.id.avail_release_image);
        NextReleaseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZooGate.popupMessage("Let's look that up!");
                final String urlstring = "https://en.wikipedia.org/wiki/" +
                        NowAvailable.getText().toString();
                final Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(urlstring));
                ZooGate.myActivity.startActivity(intent);
            }
        });
        setButtonStates();
        return rootView;
    }

    private void updateChronData() {
        Log.d("updateCronData","SystmUpdate Change");
        File crondir = new File(ZooGate.ACTUAL_SD_STORAGE + "/ZooKeeper/Cron/");
        String log = crondir.getAbsolutePath() + "/cronlog.txt";

        File update = new File(crondir,"systemupdate");
        Log.d("updatecrontab", "systemupdate file is " + update.getAbsolutePath());
        try {
            String h, m, d;

            FileOutputStream write = new FileOutputStream(update);
            StringBuffer sb = new StringBuffer();
            String[] time = CheckTime.getText().toString().trim().split(" |:");

            if ((time.length > 0) && (time[0].length() > 0) && time[0].matches("\\d+")
                                && Integer.valueOf(time[0]) < 24)
                h = time[0];
            else h = "2";
            if ((time.length > 1) && (time[1].length() > 0) && time[1].matches("\\d+")
                                && Integer.valueOf(time[1]) < 60)
                m = time[1];
            else m = "5";
            if (Integer.valueOf(m) < 10)
                m = "0" + Integer.valueOf(m);

            ZooGate.updatePref(ZooGate.PREF_UPDATE_TIME, h+":"+m);
            sb.append(m + " " + h + " * * *  am startservice -n \""+
                    "systems.eddon.android.zoo_keeper/.NotifyDownloader\" --es Action Upgrade "+
                    ">>"+log+" 2>&1");
            write.write(sb.toString().getBytes(), 0, sb.length());
            write.close();
        }
        catch (Exception e){
            Log.e("installCron", e.getLocalizedMessage());
        }
        ZooGate.writeCronTab();
    }
    @Override
    public void onAttach(Activity activity) {
        ZooGate.myActivity = activity;
        super.onAttach(activity);
        ((ZooGate) activity).onSectionAttached(
                getArguments().getInt(ZooGate.ARG_SECTION_NUMBER));
    }
}
