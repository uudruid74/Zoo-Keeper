package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by ekl on 12/9/15.
 */
public class CurrentInfoFragment extends Fragment {
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static CurrentInfoFragment newInstance(int sectionNumber) {
        CurrentInfoFragment fragment = new CurrentInfoFragment();
        Bundle args = new Bundle();
        args.putInt(ZooGate.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public CurrentInfoFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (ZooGate.sp == null)
            ZooGate.sp = PreferenceManager.getDefaultSharedPreferences(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_zoo_gate, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.current_release);
        textView.setText(ZooGate.releaseName);
        ((TextView) rootView.findViewById(R.id.rom_title)).append(" " + Build.VERSION.RELEASE);
        ((TextView) rootView.findViewById(R.id.OS_release)).setText(
                ZooGate.readShellCommand("getprop ro.modversion")
        );
        ((TextView) rootView.findViewById(R.id.device_string)).setText(
                Build.MANUFACTURER + " " +
                        Build.DEVICE + " (" + Build.HARDWARE + "/" + Build.BOARD + ")"
        );
        ((TextView) rootView.findViewById(R.id.OS_display)).setText(
                Build.DISPLAY + " " + Build.ID
        );
        ((TextView) rootView.findViewById(R.id.OS_fingerprint)).setText(
                Build.FINGERPRINT
        );
        ((TextView) rootView.findViewById(R.id.OS_Kernel)).setText(
                System.getProperty("os.version")
        );
        ((TextView) rootView.findViewById(R.id.OS_serial)).setText(
                Build.SERIAL
        );
        ((TextView) rootView.findViewById(R.id.OS_radio)).setText(
                Build.getRadioVersion()
        );
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        ZooGate.myActivity = activity;
        super.onAttach(activity);
        ((ZooGate) activity).onSectionAttached(
                getArguments().getInt(ZooGate.ARG_SECTION_NUMBER));
    }
}
