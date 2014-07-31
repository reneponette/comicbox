package com.reneponette.comicbox.ui;

import java.util.List;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.db.FileInfoDAO;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class FileSettingsActivity extends PreferenceActivity {
	/**
	 * Determines whether to always show the simplified settings UI, where
	 * settings are presented in a single list. When false, settings are shown
	 * as a master/detail two-pane view on tablets. When true, a single pane is
	 * shown on tablets.
	 */
	private static final boolean ALWAYS_SIMPLE_PREFS = true;
	private static final String FILE_INFO = "file_info";

	private FileInfo info;

	public static Intent newIntent(Context context, FileInfo fileInfo) {
		Intent intent = new Intent();
		intent.setClass(context, FileSettingsActivity.class);
		intent.putExtra(FILE_INFO, fileInfo);
		return intent;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		info = getIntent().getExtras().getParcelable(FILE_INFO);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		setupSimplePreferencesScreen();
	}

	@Override
	public void onBackPressed() {
		FileInfoDAO.instance().insertOrUpdate(info);
		setResult(RESULT_OK);
		super.onBackPressed();
	}

	/**
	 * Shows the simplified settings UI if the device configuration if the
	 * device configuration dictates that a simplified, single-pane UI should be
	 * shown.
	 */
	private void setupSimplePreferencesScreen() {
		if (!isSimplePreferences(this)) {
			return;
		}

		// In the simplified UI, fragments are not used at all and we instead
		// use the older PreferenceActivity APIs.

		addPreferencesFromResource(R.xml.pref_container);

		// 파일 설정 섹션
		PreferenceCategory fakeHeader;
		fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_file);
		getPreferenceScreen().addPreference(fakeHeader);

		addPreferencesFromResource(R.xml.pref_file);
		Preference pref = findPreference("read_direction");
		((ListPreference) pref).setValueIndex(info.getMeta().readDirection.ordinal());
		bindPreferenceSummaryToValue(pref);
		pref = findPreference("two_pages");
		((CheckBoxPreference) pref).setChecked(info.getMeta().pagesPerScan == 2);
		bindPreferenceSummaryToValue(pref);
//		pref = findPreference("autocrop");
//		((CheckBoxPreference) pref).setChecked(info.getMeta().autocrop);
//		bindPreferenceSummaryToValue(pref);

		// 뷰어 설정 섹선
		fakeHeader = new PreferenceCategory(this);
		fakeHeader.setTitle(R.string.pref_header_viewer);
		getPreferenceScreen().addPreference(fakeHeader);
		addPreferencesFromResource(R.xml.pref_viewer);

	}

	/** {@inheritDoc} */
	@Override
	public boolean onIsMultiPane() {
		return isXLargeTablet(this) && !isSimplePreferences(this);
	}

	/**
	 * Helper method to determine if the device has an extra-large screen. For
	 * example, 10" tablets are extra-large.
	 */
	private static boolean isXLargeTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
	}

	/**
	 * Determines whether the simplified settings UI should be shown. This is
	 * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
	 * doesn't have newer APIs like {@link PreferenceFragment}, or the device
	 * doesn't have an extra-large screen. In these cases, a single-pane
	 * "simplified" settings UI should be shown.
	 */
	private static boolean isSimplePreferences(Context context) {
		return ALWAYS_SIMPLE_PREFS || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB || !isXLargeTablet(context);
	}

	/** {@inheritDoc} */
	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void onBuildHeaders(List<Header> target) {
		if (!isSimplePreferences(this)) {
			loadHeadersFromResource(R.xml.pref_headers, target);
		}
	}

	/**
	 * A preference value change listener that updates the preference's summary
	 * to reflect its new value.
	 */
	private Preference.OnPreferenceChangeListener bindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
		@Override
		public boolean onPreferenceChange(Preference preference, Object value) {
			String stringValue = value.toString();

			if (preference instanceof ListPreference) {
				// For list preferences, look up the correct display value in
				// the preference's 'entries' list.
				ListPreference listPreference = (ListPreference) preference;
				int index = listPreference.findIndexOfValue(stringValue);

				// Set the summary to reflect the new value.
				preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

				if (preference.getKey().equals("read_direction")) {
					ReadDirection rd;
					switch (Integer.parseInt(value + "")) {
					case 0:
						rd = ReadDirection.LTR;
						break;
					case 1:
						rd = ReadDirection.RTL;
						break;
					default:
						rd = ReadDirection.NOTSET;
						break;
					}
					info.getMeta().readDirection = rd;
				}

			} else if (preference instanceof CheckBoxPreference) {
				Boolean b = (Boolean) value;
				if (preference.getKey().equals("autocrop")) {
					info.getMeta().autocrop = b;
				} else {
					if (b)
						info.getMeta().pagesPerScan = 2;
					else
						info.getMeta().pagesPerScan = 1;
				}
			} else {
				// For all other preferences, set the summary to the value's
				// simple string representation.
				preference.setSummary(stringValue);
			}
			return true;
		}
	};

	/**
	 * Binds a preference's summary to its value. More specifically, when the
	 * preference's value is changed, its summary (line of text below the
	 * preference title) is updated to reflect the value. The summary is also
	 * immediately updated upon calling this method. The exact display format is
	 * dependent on the type of preference.
	 * 
	 * @see #bindPreferenceSummaryToValueListener
	 */
	private void bindPreferenceSummaryToValue(Preference preference) {
		// Set the listener to watch for value changes.
		preference.setOnPreferenceChangeListener(bindPreferenceSummaryToValueListener);

		if (preference instanceof CheckBoxPreference)
			return;

		// 시작하면서 셋팅된 값의 서머리 셋
		bindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager
				.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
	}

	// /**
	// * This fragment shows general preferences only. It is used when the
	// * activity is showing a two-pane settings UI.
	// */
	// @TargetApi(Build.VERSION_CODES.HONEYCOMB)
	// public static class GeneralPreferenceFragment extends PreferenceFragment
	// {
	// @Override
	// public void onCreate(Bundle savedInstanceState) {
	// super.onCreate(savedInstanceState);
	// addPreferencesFromResource(R.xml.pref_general);
	//
	// // Bind the summaries of EditText/List/Dialog/Ringtone preferences
	// // to their values. When their values change, their summaries are
	// // updated to reflect the new value, per the Android Design
	// // guidelines.
	// bindPreferenceSummaryToValue(findPreference("example_text"));
	// bindPreferenceSummaryToValue(findPreference("example_list"));
	// }
	// }

}
