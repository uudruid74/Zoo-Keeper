package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by ekl on 12/9/15.
 */
public class AboutFragment extends Fragment {
    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AboutFragment newInstance(int sectionNumber) {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        args.putInt(ZooGate.ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public AboutFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        String versionString = "v.1.0.0";

        try {
            PackageInfo pInfo = ZooGate.myActivity.getPackageManager()
                    .getPackageInfo(ZooGate.myActivity.getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            versionString = "v." + version + "." + verCode;
        }
        catch (Exception e) {
            Log.e("onCreateView", e.getLocalizedMessage());
        }
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(ZooGate.newBackButton);
        TextView textView = (TextView) rootView.findViewById(R.id.about_text);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        TextView version = (TextView) rootView.findViewById(R.id.version_text);
        version.setText(versionString);
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
