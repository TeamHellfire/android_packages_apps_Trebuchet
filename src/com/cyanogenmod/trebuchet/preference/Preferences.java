/*
 * Copyright (C) 2011 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.trebuchet.preference;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Display;
import android.view.IWindowManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.cyanogenmod.trebuchet.LauncherApplication;
import com.cyanogenmod.trebuchet.LauncherModel;
import com.cyanogenmod.trebuchet.preference.DoubleNumberPickerPreference;
import com.cyanogenmod.trebuchet.R;

import java.util.List;

public class Preferences extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "Trebuchet.Preferences";

    private static SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = getSharedPreferences(PreferencesProvider.PREFERENCES_KEY,
                Context.MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preferences_headers, target);
        updateHeaders(target);
    }

    private void updateHeaders(List<Header> headers) {
        int i = 0;
        while (i < headers.size()) {
            Header header = headers.get(i);

            // Version preference
            if (header.id == R.id.preferences_application_version) {
                header.title = getString(R.string.application_name) + " " + getString(R.string.application_version);
            }

            // Increment if not removed
            if (headers.get(i) == header) {
                i++;
            }
        }
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (adapter == null) {
            super.setListAdapter(null);
        } else {
            super.setListAdapter(new HeaderAdapter(this, getHeaders()));
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(PreferencesProvider.PREFERENCES_CHANGED, true);
        editor.commit();
    }

    public static class HomescreenFragment extends PreferenceFragment {
        private static DoubleNumberPickerPreference mHomescreenGrid;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_homescreen);

            PreferenceScreen preferenceScreen = getPreferenceScreen();

            mHomescreenGrid = (DoubleNumberPickerPreference)
                    findPreference("ui_homescreen_grid");
            mHomescreenGrid.setDefault1(LauncherModel.getCellCountY());
            mHomescreenGrid.setDefault2(LauncherModel.getCellCountX());
            mHomescreenGrid.setMax1(LauncherModel.getMaxCellCountY());
            mHomescreenGrid.setMax2(LauncherModel.getMaxCellCountX());

            if (LauncherApplication.isScreenLarge()) {
                preferenceScreen.removePreference(findPreference("ui_homescreen_grid"));
            }
        }
    }

    public static class DrawerFragment extends PreferenceFragment {
        private static DoubleNumberPickerPreference mPortraitAppGrid;
        private static DoubleNumberPickerPreference mLandscapeAppGrid;
        private static Preference mDrawerColor;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences_drawer);
            mPortraitAppGrid = (DoubleNumberPickerPreference)
                    findPreference("ui_drawer_grid");
            mLandscapeAppGrid = (DoubleNumberPickerPreference)
                    findPreference("ui_drawer_grid_land");
            mDrawerColor = (Preference) findPreference("ui_drawer_background");
        }

        public void onResume() {
            super.onResume();

            boolean landscape = LauncherApplication.isScreenLandscape(getActivity());

            Resources r = getActivity().getResources();

            WindowManager wm = (WindowManager) getActivity().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getRealSize(size);

            boolean hasNavBar = false;
            boolean hasSysNavBar = false;
            IWindowManager windowManager = IWindowManager.Stub.asInterface(
                    ServiceManager.getService(Context.WINDOW_SERVICE));
            try {
                hasNavBar = windowManager.hasNavigationBar();
                hasSysNavBar = windowManager.hasSystemNavBar();
            } catch (RemoteException e) {
            }

            final float cellWidth = r.getDimension(R.dimen.apps_customize_cell_width);
            final float cellHeight = r.getDimension(R.dimen.apps_customize_cell_height);
            DisplayMetrics displayMetrics = r.getDisplayMetrics();
            final float screenWidth = r.getConfiguration().screenWidthDp * displayMetrics.density;
            final float screenHeight = r.getConfiguration().screenHeightDp * displayMetrics.density;
            final float systemBarHeight = r.getDimension(hasSysNavBar ?
                    com.android.internal.R.dimen.navigation_bar_height :
                    com.android.internal.R.dimen.status_bar_height);
            final float navigationBarHeight = hasNavBar ?
                    r.getDimension(com.android.internal.R.dimen.navigation_bar_height) : 0;
            final float tabBarHeight = r.getDimension(R.dimen.apps_customize_tab_bar_height)
                    + r.getDimension(R.dimen.apps_customize_tab_bar_margin_top);

            int cellCountXPort = (int) ((landscape ? size.y : screenWidth) / cellWidth);
            int cellCountYPort = (int) (((landscape ? (screenWidth - systemBarHeight - navigationBarHeight) : screenHeight) - tabBarHeight) / cellHeight);

            int cellCountXLand = (int) ((landscape ? screenWidth : size.y) / cellWidth);
            int cellCountYLand = (int) (((landscape ? screenHeight : (screenWidth - systemBarHeight - navigationBarHeight)) - tabBarHeight) / cellHeight);

            mPortraitAppGrid.setMax1(cellCountYPort);
            mPortraitAppGrid.setMax2(cellCountXPort);

            mLandscapeAppGrid.setMax1(cellCountYLand);
            mLandscapeAppGrid.setMax2(cellCountXLand);
        }

        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            boolean value;

            if (preference == mDrawerColor) {
                ColorPickerDialog cp = new ColorPickerDialog(getActivity(),
                        mDrawerColorListener, PreferencesProvider.Interface.Drawer.getDrawerColor());
                cp.setDefaultColor(0xff000000);
                cp.show();
                return true;
            }
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        ColorPickerDialog.OnColorChangedListener mDrawerColorListener =
            new ColorPickerDialog.OnColorChangedListener() {
                public void colorChanged(int color) {
                    mPreferences.edit().putInt("ui_drawer_background",
                            color).commit();
                }
                public void colorUpdate(int color) {
                }
        };
    }

    public static class DockFragment extends PreferenceFragment {
        private static NumberPickerPreference mHotseatSize;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_dock);

            mHotseatSize = (NumberPickerPreference)
                    findPreference("ui_dock_icons");
            mHotseatSize.setMax(LauncherModel.getMaxCellCountX() + 1);
            mHotseatSize.setDefault(LauncherModel.getCellCountX());
            CheckBoxPreference bottomDock = (CheckBoxPreference)
                    findPreference("ui_land_dock_bottom");
            bottomDock.setChecked(PreferencesProvider.Interface.Dock.getLandscapeDockOnBottom());
        }
    }

    public static class GeneralFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_general);
        }
    }

    public static class GesturesFragment extends PreferenceFragment implements OnPreferenceChangeListener {
        private ListPreference mHomescreenDoubleTap;
        private ListPreference mHomescreenSwipeUp;
        private ListPreference mHomescreenSwipeDown;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences_gestures);

            PreferenceScreen prefSet = getPreferenceScreen();

            mHomescreenDoubleTap = (ListPreference) prefSet.findPreference("ui_homescreen_doubletap");
            mHomescreenDoubleTap.setOnPreferenceChangeListener(this);
            mHomescreenDoubleTap.setSummary(mHomescreenDoubleTap.getEntry());
            mHomescreenSwipeDown = (ListPreference) prefSet.findPreference("ui_homescreen_swipe_down");
            mHomescreenSwipeDown.setOnPreferenceChangeListener(this);
            mHomescreenSwipeDown.setSummary(mHomescreenSwipeDown.getEntry());
            mHomescreenSwipeUp = (ListPreference) prefSet.findPreference("ui_homescreen_swipe_up");
            mHomescreenSwipeUp.setOnPreferenceChangeListener(this);
            mHomescreenSwipeUp.setSummary(mHomescreenSwipeUp.getEntry());
        }

        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference == mHomescreenDoubleTap) {
                CharSequence doubleTapIndex[] = mHomescreenDoubleTap.getEntries();
                int doubleTapValue = Integer.parseInt((String) newValue);
                if (doubleTapValue == 6) {
                    // Pick an application
                    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                    pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
                    startActivityForResult(pickIntent, 0);
                }
                CharSequence doubleTapSummary = doubleTapIndex[doubleTapValue];
                mHomescreenDoubleTap.setSummary(doubleTapSummary);
                return true;
            } else if (preference == mHomescreenSwipeDown) {
                CharSequence homeSwipeDownIndex[] = mHomescreenSwipeDown.getEntries();
                int hSDValue = Integer.parseInt((String) newValue);
                if (hSDValue == 6) {
                    // Pick an application
                    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                    pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
                    startActivityForResult(pickIntent, 2);
                }
                CharSequence homeSDSummary = homeSwipeDownIndex[hSDValue];
                mHomescreenSwipeDown.setSummary(homeSDSummary);
                return true;
            } else if (preference == mHomescreenSwipeUp) {
                CharSequence homeSwipeUpIndex[] = mHomescreenSwipeUp.getEntries();
                int hSUValue = Integer.parseInt((String) newValue);
                if (hSUValue == 6) {
                    // Pick an application
                    Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                    Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
                    pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
                    startActivityForResult(pickIntent, 1);
                }
                CharSequence homeSUSummary = homeSwipeUpIndex[hSUValue];
                mHomescreenSwipeUp.setSummary(homeSUSummary);
                return true;
            }
            return false;
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (data != null) {
                if (requestCode == 0) {
                    mPreferences.edit().putString("double_tap_gesture_app",
                            data.toUri(0)).commit();
                } else if (requestCode == 1) {
                    mPreferences.edit().putString("swipe_up_gesture_app",
                            data.toUri(0)).commit();
                } else if (requestCode == 2) {
                    mPreferences.edit().putString("swipe_down_gesture_app",
                            data.toUri(0)).commit();
                }
            }
        }
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        private static final int HEADER_TYPE_NORMAL = 0;
        private static final int HEADER_TYPE_CATEGORY = 1;

        private static final int HEADER_TYPE_COUNT = HEADER_TYPE_CATEGORY + 1;

        private static class HeaderViewHolder {
            ImageView icon;
            TextView title;
            TextView summary;
        }

        private LayoutInflater mInflater;

        static int getHeaderType(Header header) {
            if (header.id == R.id.preferences_application_section) {
                return HEADER_TYPE_CATEGORY;
            } else {
                return HEADER_TYPE_NORMAL;
            }
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false; // because of categories
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != HEADER_TYPE_CATEGORY;
        }

        @Override
        public int getViewTypeCount() {
            return HEADER_TYPE_COUNT;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);

            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            Header header = getItem(position);
            int headerType = getHeaderType(header);
            View view = null;

            if (convertView == null) {
                holder = new HeaderViewHolder();
                switch (headerType) {
                    case HEADER_TYPE_CATEGORY:
                        view = new TextView(getContext(), null,
                                android.R.attr.listSeparatorTextViewStyle);
                        holder.title = (TextView) view;
                        break;

                    case HEADER_TYPE_NORMAL:
                        view = mInflater.inflate(
                                R.layout.preference_header_item, parent,
                                false);
                        holder.icon = (ImageView) view.findViewById(R.id.icon);
                        holder.title = (TextView)
                                view.findViewById(com.android.internal.R.id.title);
                        holder.summary = (TextView)
                                view.findViewById(com.android.internal.R.id.summary);
                        break;
                }
                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            // All view fields must be updated every time, because the view may be recycled
            switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    holder.title.setText(header.getTitle(getContext().getResources()));
                    break;

                case HEADER_TYPE_NORMAL:
                    holder.icon.setImageResource(header.iconRes);
                    holder.title.setText(header.getTitle(getContext().getResources()));
                    CharSequence summary = header.getSummary(getContext().getResources());
                    if (!TextUtils.isEmpty(summary)) {
                        holder.summary.setVisibility(View.VISIBLE);
                        holder.summary.setText(summary);
                    } else {
                        holder.summary.setVisibility(View.GONE);
                    }
                    break;
            }

            return view;
        }
    }
}
