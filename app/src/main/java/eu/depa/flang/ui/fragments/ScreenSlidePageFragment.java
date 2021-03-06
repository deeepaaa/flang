package eu.depa.flang.ui.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import eu.depa.flang.R;

public class ScreenSlidePageFragment extends Fragment {

    public ScreenSlidePageFragment(){}
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        int pos = getArguments().getInt("pos");
        int[] fragments = {R.layout.intro_1,
                R.layout.intro_2,
                R.layout.intro_3};
        return inflater.inflate(
                fragments[pos], container, false);
    }
}