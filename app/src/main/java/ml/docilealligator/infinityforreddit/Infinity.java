package ml.docilealligator.infinityforreddit;

import android.app.Activity;
import android.app.Application;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.evernote.android.state.StateSaver;
import com.livefront.bridge.Bridge;
import com.livefront.bridge.SavedStateHandler;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Named;

import ml.docilealligator.infinityforreddit.activities.LockScreenActivity;
import ml.docilealligator.infinityforreddit.broadcastreceivers.NetworkWifiStatusReceiver;
import ml.docilealligator.infinityforreddit.broadcastreceivers.WallpaperChangeReceiver;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.events.ChangeAppLockEvent;
import ml.docilealligator.infinityforreddit.events.ChangeNetworkStatusEvent;
import ml.docilealligator.infinityforreddit.events.ToggleSecureModeEvent;
import ml.docilealligator.infinityforreddit.font.ContentFontFamily;
import ml.docilealligator.infinityforreddit.font.FontFamily;
import ml.docilealligator.infinityforreddit.font.TitleFontFamily;
import ml.docilealligator.infinityforreddit.utils.MaterialYouUtils;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;
import ml.docilealligator.infinityforreddit.utils.Utils;

public class Infinity extends Application implements LifecycleObserver {
    public Typeface typeface;
    public Typeface titleTypeface;
    public Typeface contentTypeface;
    private AppComponent mAppComponent;
    private NetworkWifiStatusReceiver mNetworkWifiStatusReceiver;
    private boolean appLock;
    private long appLockTimeout;
    private boolean canStartLockScreenActivity = false;
    private boolean isSecureMode;
    @Inject
    @Named("default")
    SharedPreferences mSharedPreferences;
    @Inject
    @Named("security")
    SharedPreferences mSecuritySharedPreferences;
    @Inject
    @Named("internal")
    SharedPreferences mInternalSharedPreferences;
    @Inject
    @Named("light_theme")
    SharedPreferences lightThemeSharedPreferences;
    @Inject
    @Named("dark_theme")
    SharedPreferences darkThemeSharedPreferences;
    @Inject
    @Named("amoled_theme")
    SharedPreferences amoledThemeSharedPreferences;
    @Inject
    RedditDataRoomDatabase redditDataRoomDatabase;
    @Inject
    CustomThemeWrapper customThemeWrapper;
    @Inject
    Executor executor;

    @Override
    public void onCreate() {
        super.onCreate();
        infinity = this;

        mAppComponent = DaggerAppComponent.factory()
                .create(this);

        mAppComponent.inject(this);

        appLock = mSecuritySharedPreferences.getBoolean(SharedPreferencesUtils.APP_LOCK, false);
        appLockTimeout = Long.parseLong(mSecuritySharedPreferences.getString(SharedPreferencesUtils.APP_LOCK_TIMEOUT, "600000"));
        isSecureMode = mSecuritySharedPreferences.getBoolean(SharedPreferencesUtils.SECURE_MODE, false);

        try {
            if (mSharedPreferences.getString(SharedPreferencesUtils.FONT_FAMILY_KEY, FontFamily.Default.name()).equals(FontFamily.Custom.name())) {
                typeface = Typeface.createFromFile(getExternalFilesDir("fonts") + "/font_family.ttf");
            }
            if (mSharedPreferences.getString(SharedPreferencesUtils.TITLE_FONT_FAMILY_KEY, TitleFontFamily.Default.name()).equals(TitleFontFamily.Custom.name())) {
                titleTypeface = Typeface.createFromFile(getExternalFilesDir("fonts") + "/title_font_family.ttf");
            }
            if (mSharedPreferences.getString(SharedPreferencesUtils.CONTENT_FONT_FAMILY_KEY, ContentFontFamily.Default.name()).equals(ContentFontFamily.Custom.name())) {
                contentTypeface = Typeface.createFromFile(getExternalFilesDir("fonts") + "/content_font_family.ttf");
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.unable_to_load_font, Toast.LENGTH_SHORT).show();
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityPreCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activity instanceof CustomFontReceiver) {
                    ((CustomFontReceiver) activity).setCustomFont(typeface, titleTypeface, contentTypeface);
                }
            }

            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
                if (isSecureMode) {
                    activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {

            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (canStartLockScreenActivity && appLock
                        && System.currentTimeMillis() - mSecuritySharedPreferences.getLong(SharedPreferencesUtils.LAST_FOREGROUND_TIME, 0) >= appLockTimeout
                        && !(activity instanceof LockScreenActivity)) {
                    Intent intent = new Intent(activity, LockScreenActivity.class);
                    activity.startActivity(intent);
                }
                canStartLockScreenActivity = false;
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });

        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);

        Bridge.initialize(getApplicationContext(), new SavedStateHandler() {
            @Override
            public void saveInstanceState(@NonNull Object target, @NonNull Bundle state) {
                StateSaver.saveInstanceState(target, state);
            }

            @Override
            public void restoreInstanceState(@NonNull Object target, @Nullable Bundle state) {
                StateSaver.restoreInstanceState(target, state);
            }
        });

        EventBus.builder().addIndex(new EventBusIndex()).installDefaultEventBus();

        EventBus.getDefault().register(this);

        mNetworkWifiStatusReceiver =
                new NetworkWifiStatusReceiver(() -> EventBus.getDefault().post(new ChangeNetworkStatusEvent(Utils.getConnectedNetwork(getApplicationContext()))));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(mNetworkWifiStatusReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION), RECEIVER_NOT_EXPORTED);
            registerReceiver(new WallpaperChangeReceiver(mSharedPreferences), new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED), RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(mNetworkWifiStatusReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            registerReceiver(new WallpaperChangeReceiver(mSharedPreferences), new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED));
        }

        if (mSharedPreferences.getBoolean(SharedPreferencesUtils.ENABLE_MATERIAL_YOU, false)) {
            int sentryColor = mInternalSharedPreferences.getInt(SharedPreferencesUtils.MATERIAL_YOU_SENTRY_COLOR, 0);
            boolean sentryColorHasChanged = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (sentryColor != getColor(android.R.color.system_accent1_100)) {
                    sentryColorHasChanged = true;
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(this);
                WallpaperColors wallpaperColors = wallpaperManager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);

                if (wallpaperColors != null) {
                    int colorPrimaryInt = wallpaperColors.getPrimaryColor().toArgb();
                    if (sentryColor != colorPrimaryInt) {
                        sentryColorHasChanged = true;
                    }
                }
            }

            if (sentryColorHasChanged) {
                MaterialYouUtils.changeThemeASync(this, executor, new Handler(Looper.getMainLooper()),
                        redditDataRoomDatabase, customThemeWrapper,
                        lightThemeSharedPreferences, darkThemeSharedPreferences,
                        amoledThemeSharedPreferences, mInternalSharedPreferences, null);
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void appInForeground() {
        canStartLockScreenActivity = true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void appInBackground() {
        if (appLock) {
            mSecuritySharedPreferences.edit().putLong(SharedPreferencesUtils.LAST_FOREGROUND_TIME, System.currentTimeMillis()).apply();
        }
    }

    public AppComponent getAppComponent() {
        return mAppComponent;
    }

    public CustomThemeWrapper getCustomThemeWrapper() {
        return customThemeWrapper;
    }

    @Subscribe
    public void onToggleSecureModeEvent(ToggleSecureModeEvent secureModeEvent) {
        isSecureMode = secureModeEvent.isSecureMode;
    }

    @Subscribe
    public void onChangeAppLockEvent(ChangeAppLockEvent changeAppLockEvent) {
        appLock = changeAppLockEvent.appLock;
        appLockTimeout = changeAppLockEvent.appLockTimeout;
    }

    static Infinity infinity;
    public static Infinity getInstance() {
        return infinity;
    }
}
