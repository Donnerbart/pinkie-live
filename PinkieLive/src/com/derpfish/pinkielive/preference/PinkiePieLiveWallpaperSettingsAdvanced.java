package com.derpfish.pinkielive.preference;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.derpfish.pinkielive.PinkiePieLiveWallpaper;
import com.derpfish.pinkielive.R;

public class PinkiePieLiveWallpaperSettingsAdvanced extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        getPreferenceManager().setSharedPreferencesName(PinkiePieLiveWallpaper.SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.livewallpaper_settings_advanced);
    }
}
