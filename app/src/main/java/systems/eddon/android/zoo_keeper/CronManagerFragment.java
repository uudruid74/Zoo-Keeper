package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormatSymbols;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Created by ekl on 12/9/15.
 */
public class CronManagerFragment extends Fragment {
    TextView dailyhour;
    TextView dailymin;
    TextView hourlymin;
    TextView weeklyday;
    TextView weeklyhour;
    TextView monthlyday;
    TextView monthlyhour;
    TextView fulltimezone;
    Button editdaily;
    Button edithourly;
    Button editweekly;
    Button editmonthly;
    Button advancededit;
    Button logbutton;
    Button clearlogbutton;

    Switch master;


    // Preference items
    private static final String CRON_DAILYHOUR     = "cron_daily_hour";
    private static final String CRON_DAILYMIN      = "cron_daily_min";
    private static final String CRON_HOURLYMIN     = "cron_hourly_min";
    private static final String CRON_WEEKLYDAY     = "cron_weekly_day";
    private static final String CRON_WEEKLYHOUR    = "cron_weekly_hour";
    private static final String CRON_MONTHLYDAY    = "cron_monthly_day";
    private static final String CRON_MONTHLYHOUR   = "cron_monthly_hour";

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static CronManagerFragment newInstance(int sectionNumber) {
        CronManagerFragment fragment = new CronManagerFragment();
        Bundle args = new Bundle();
        args.putInt(ZooGate.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public CronManagerFragment() {
    }

    View.OnKeyListener newBackButton = ZooGate.newBackButton;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_cron_manager, container, false);
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(newBackButton);

        master = (Switch) rootView.findViewById(R.id.cron_switch);
        master.setChecked(ZooGate.sp.getBoolean(ZooGate.PREF_USER_CRON_SERVICES, true));
        master.setOnClickListener(new CompoundButton.OnClickListener() {
            @Override
            public void onClick(View button) {
                Switch witch = (Switch) button;
                if (!witch.isChecked()) {
                    if (ZooGate.sp.getBoolean(ZooGate.PREF_USER_AUTO_UPDATES, true)) {
                        ZooGate.popupMessage("Auto Updates require Cron.\nLongpress to turn off Cron");
                        witch.setChecked(true);
                    } else {
                        witch.setChecked(false);
                    }
                } else {
                    witch.setChecked(true);
                }
            }
        });
        master.setLongClickable(true);
        master.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                master.setChecked(false);
                return true;
            }
        });

        TimeZone tz = Calendar.getInstance().getTimeZone();

        fulltimezone = (TextView) rootView.findViewById(R.id.full_timezone);
        fulltimezone.setText("TimeZone:  " + tz.getDisplayName() + " (" + findTZS() + ")" );
        dailyhour = (TextView) rootView.findViewById(R.id.daily_hour);
        dailyhour.setText(String.valueOf(ZooGate.sp.getInt(CRON_DAILYHOUR, 3)));
        dailyhour.setOnKeyListener(newBackButton);
        dailymin = (TextView) rootView.findViewById(R.id.daily_min);
        dailymin.setText(String.valueOf(ZooGate.sp.getInt(CRON_DAILYMIN, 0)));
        dailymin.setOnKeyListener(newBackButton);
        hourlymin = (TextView) rootView.findViewById(R.id.hourly_min);
        hourlymin.setText(String.valueOf(ZooGate.sp.getInt(CRON_HOURLYMIN, 10)));
        hourlymin.setOnKeyListener(newBackButton);
        weeklyday = (TextView) rootView.findViewById(R.id.weekly_day);
        weeklyday.setText(String.valueOf(ZooGate.sp.getInt(CRON_WEEKLYDAY, 1)));
        weeklyday.setOnKeyListener(newBackButton);
        weeklyhour = (TextView) rootView.findViewById(R.id.weekly_hour);
        weeklyhour.setText(String.valueOf(ZooGate.sp.getInt(CRON_WEEKLYHOUR, 4)));
        weeklyhour.setOnKeyListener(newBackButton);
        monthlyday = (TextView) rootView.findViewById(R.id.monthly_day);
        monthlyday.setText(String.valueOf(ZooGate.sp.getInt(CRON_MONTHLYDAY, 1)));
        monthlyday.setOnKeyListener(newBackButton);
        monthlyhour = (TextView) rootView.findViewById(R.id.monthly_hour);
        monthlyhour.setText(String.valueOf(ZooGate.sp.getInt(CRON_MONTHLYHOUR, 5)));
        monthlyhour.setOnKeyListener(newBackButton);
        editdaily = (Button) rootView.findViewById(R.id.edit_daily);
        editdaily.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editFile("daily");
            }
        });
        edithourly = (Button) rootView.findViewById(R.id.edit_hourly);
        edithourly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editFile("hourly");
            }
        });
        editweekly = (Button) rootView.findViewById(R.id.edit_weekly);
        editweekly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editFile("weekly");
            }
        });
        editmonthly = (Button) rootView.findViewById(R.id.edit_monthly);
        editmonthly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editFile("monthly");
            }
        });
        advancededit = (Button) rootView.findViewById(R.id.advanced_edit);
        advancededit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editFile("advanced");
            }
        });
        logbutton = (Button) rootView.findViewById(R.id.log_button);
        logbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                viewFile("cronlog.txt");
            }
        });
        clearlogbutton = (Button) rootView.findViewById(R.id.clear_log_button);
        clearlogbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ZooGate.popupMessage("Long Press to clear the log!");
            }
        });
        clearlogbutton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ZooGate.writeRootFile("/sdcard/Cron/cronlog.txt","");
                ZooGate.popupMessage("Log File Cleared!");
                return true;
            }
        });
        return rootView;
    }

    public static String findTZS() {
        String date = ZooGate.readShellCommand("date");
        String[] elements = date.split(" ");
        String label = elements[4];
        TimeZone tz = Calendar.getInstance().getTimeZone();
        boolean dlt = tz.useDaylightTime();
        int offset = tz.getDSTSavings()/600000;
        DateFormatSymbols dfs = DateFormatSymbols.getInstance();
        String[][] z = dfs.getZoneStrings();
        for (String[] za: z) {
            if (dlt) {
                if (za[4].equals(label)) {
                    return za[2] + offset + za[4];
                } else if (za[2].equals(label)) {
                    return za[2] + offset + za[4];
                }
            }
        }
        return "UTC";
    }
    private void editFile(String period) {
        File log = new File(ZooGate.ACTUAL_SD_STORAGE + "/Cron/"+period);
        Log.d("editFile", log.getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_EDIT);
        Uri uri = Uri.fromFile(log);
        intent.setDataAndType(uri, "text/plain");
        ZooGate.myActivity.startActivity(intent);
    }
    private void viewFile(String period) {
        File log = new File(ZooGate.ACTUAL_SD_STORAGE + "/Cron/"+period);
        Log.d("editFile", log.getAbsolutePath());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.fromFile(log);
        intent.setDataAndType(uri, "text/plain");
        ZooGate.myActivity.startActivity(intent);
    }

    public void updateCrontab() {
        File crondir = new File(ZooGate.ACTUAL_SD_STORAGE + "/Cron/");
        String log = crondir.getAbsolutePath() + "/cronlog.txt";
        File master = new File(crondir,"simple");
        Log.d("updatecrontab", "simple file is " + master.getAbsolutePath());
        try {
            String h, m, d;

            FileOutputStream write = new FileOutputStream(master);
            StringBuffer sb = new StringBuffer();

            SharedPreferences.Editor e = ZooGate.sp.edit();
            e.putBoolean(ZooGate.PREF_USER_CRON_SERVICES, true);
            e.putInt(CRON_DAILYHOUR, Integer.valueOf(h = dailyhour.getText().toString()));
            e.putInt(CRON_DAILYMIN, Integer.valueOf(m = dailymin.getText().toString()));
            sb.append(m + " " + h + " * * *    run-parts /system/etc/cron.daily >>" + log + " 2>&1\n");

            e.putInt(CRON_HOURLYMIN, Integer.valueOf(m = hourlymin.getText().toString()));
            sb.append(m + " * * * *   run-parts /system/etc/cron.hourly >>" + log + " 2>&1\n");

            e.putInt(CRON_WEEKLYDAY, Integer.valueOf(d = weeklyday.getText().toString()));
            e.putInt(CRON_WEEKLYHOUR, Integer.valueOf(h = weeklyhour.getText().toString()));
            sb.append("20 " + h + " * * " + d + "   run-parts /system/etc/cron.weekly >>" + log + " 2>&1\n");

            e.putInt(CRON_MONTHLYDAY, Integer.valueOf(d = monthlyday.getText().toString()));
            e.putInt(CRON_MONTHLYHOUR, Integer.valueOf(h = monthlyhour.getText().toString()));
            sb.append("20 " + h + " " + d + " * *   run-parts /system/etc/cron.monthly >>" + log + " 2>&1\n");
            e.apply();
            write.write(sb.toString().getBytes(), 0, sb.length());
            write.close();
            ZooGate.writeCronTab();
        }
        catch (Exception e){
            Log.e("installCron", e.getLocalizedMessage());
        }
    }

    @Override
    public void onDetach() {
         if (master.isChecked()) {
             updateCrontab();
         } else {
             ZooGate.updateSwitch(ZooGate.PREF_USER_AUTO_UPDATES, false);
             ZooGate.updateSwitch(ZooGate.PREF_USER_CRON_SERVICES, false);
             ZooGate.killCron();
         }
        super.onDetach();
    }

    @Override
    public void onAttach(Activity activity) {
        ZooGate.myActivity = activity;
        super.onAttach(activity);
        ((ZooGate) activity).onSectionAttached(
                getArguments().getInt(ZooGate.ARG_SECTION_NUMBER));
    }
}
