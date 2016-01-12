package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

/**
 * Created by ekl on 12/9/15.
 */
public class AdBlockerFragment extends Fragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String WHITELIST = "/storage/emulated/0/Android/hosts.whitelist";
    private static final String BLACKLIST = "/storage/emulated/0/Android/hosts.blacklist";
    private static final String SCRIPT_INSTALL = "buildHosts.sh";
    private static final String SCRIPT_ALLOW = "noblocking.sh";
    private static final int ADTIMEOUT = 15 * 1000;

    Switch ad_switch;
    boolean master_on;
    TextView whitelist;
    String origwhitelist;
    String origblacklist;
    TextView blacklist;
    boolean needsUpdate = false;
    private Handler handler;

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AdBlockerFragment newInstance(int sectionNumber) {
        AdBlockerFragment fragment = new AdBlockerFragment();
        Bundle args = new Bundle();
        args.putInt(ZooGate.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public AdBlockerFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        handler = new Handler();
        master_on = ZooGate.sp.getBoolean(ZooGate.PREF_USER_BLOCK_ADS, true);
        View rootView = inflater.inflate(R.layout.fragment_ad_blocker, container, false);
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(ZooGate.newBackButton);

        ad_switch = (Switch) rootView.findViewById(R.id.ad_switch);
        ad_switch.setChecked(master_on);
        ad_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (master_on != isChecked) {
                    master_on = isChecked;
                    needsUpdate = true;
                    updateSavedData();
                    needsUpdate = false;
                }
            }
        });
        ad_switch.setLongClickable(true);
        ad_switch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Runnable resetAdBlocker = new Runnable() {
                    public void run() {
                        master_on = true;
                        needsUpdate = true;
                        updateSavedData();
                        ad_switch.setChecked(master_on);
                        ZooGate.popupMessage(getString(R.string.ad_blocker_reinstalled));
                    }
                };
                ad_switch.setChecked(false);
                master_on = false;
                needsUpdate = true;
                updateSavedData();
                handler.postDelayed(resetAdBlocker, ADTIMEOUT);
                ZooGate.popupMessage(getString(R.string.ad_blocker_tempmsg));
                return true;
            }
        });
        whitelist = (TextView) rootView.findViewById(R.id.whitelist);
        blacklist = (TextView) rootView.findViewById(R.id.blacklist);

        whitelist.setText(origwhitelist = ZooGate.readFile(WHITELIST));
        blacklist.setText(origblacklist = ZooGate.readFile(BLACKLIST));

        whitelist.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String newdata = whitelist.getText().toString().trim();
                    if (!newdata.equals(origwhitelist)) {
                        ZooGate.writeRootFile(WHITELIST, whitelist.getText().toString().trim());
                        needsUpdate = true;
                        origwhitelist = newdata;
                    }
                }
            }
        });
        blacklist.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String newdata = blacklist.getText().toString().trim();
                    if (!newdata.equals(origblacklist)) {
                        ZooGate.writeRootFile(BLACKLIST, blacklist.getText().toString().trim());
                        needsUpdate = true;
                        origblacklist = newdata;
                    }
                }
            }
        });

        return rootView;
    }

    private void updateSavedData()
    {
        /* Save whitelist and blacklist */
        if (needsUpdate) {
            ZooGate.updateSwitch(ZooGate.PREF_USER_BLOCK_ADS, master_on);
            if (master_on) {
            /* run script to generate new file, saving orig first if not exists */
                ZooGate.runLocalRootCommand(SCRIPT_INSTALL);
            } else {
            /* run script to cp built-in to old (saves orig if needed) */
                ZooGate.runLocalRootCommand(SCRIPT_ALLOW);
            }
            needsUpdate = false;
        }
    }

    @Override
    public void onDetach() {
        updateSavedData();
        super.onDetach();
    }

    @Override
    public void onPause() {
        updateSavedData();
        super.onPause();
    }

    @Override
    public void onAttach(Activity activity) {
        ZooGate.myActivity = activity;
        super.onAttach(activity);
        ((ZooGate) activity).onSectionAttached(
                getArguments().getInt(ZooGate.ARG_SECTION_NUMBER));
    }
}
