package ml.docilealligator.infinityforreddit.settings;

import static android.app.Activity.RESULT_OK;
import static android.content.Intent.ACTION_OPEN_DOCUMENT_TREE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Named;

import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.RedditDataRoomDatabase;
import ml.docilealligator.infinityforreddit.asynctasks.BackupSettings;
import ml.docilealligator.infinityforreddit.asynctasks.DeleteAllPostLayouts;
import ml.docilealligator.infinityforreddit.asynctasks.DeleteAllReadPosts;
import ml.docilealligator.infinityforreddit.asynctasks.DeleteAllSortTypes;
import ml.docilealligator.infinityforreddit.asynctasks.DeleteAllSubreddits;
import ml.docilealligator.infinityforreddit.asynctasks.DeleteAllThemes;
import ml.docilealligator.infinityforreddit.asynctasks.DeleteAllUsers;
import ml.docilealligator.infinityforreddit.asynctasks.RestoreSettings;
import ml.docilealligator.infinityforreddit.customviews.preference.CustomFontPreferenceFragmentCompat;
import ml.docilealligator.infinityforreddit.events.RecreateActivityEvent;
import ml.docilealligator.infinityforreddit.readpost.ReadPostDao;
import ml.docilealligator.infinityforreddit.utils.APIUtils;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;

/**
 * A simple {@link Fragment} subclass.
 */
public class AdvancedPreferenceFragment extends CustomFontPreferenceFragmentCompat {

    private static final int SELECT_BACKUP_SETTINGS_DIRECTORY_REQUEST_CODE = 1;
    private static final int SELECT_RESTORE_SETTINGS_DIRECTORY_REQUEST_CODE = 2;
    @Inject
    RedditDataRoomDatabase mRedditDataRoomDatabase;
    @Inject
    @Named("default")
    SharedPreferences mSharedPreferences;
    @Inject
    @Named("current_account")
    SharedPreferences mCurrentAccountSharedPreferences;
    @Inject
    @Named("sort_type")
    SharedPreferences mSortTypeSharedPreferences;
    @Inject
    @Named("post_layout")
    SharedPreferences mPostLayoutSharedPreferences;
    @Inject
    @Named("light_theme")
    SharedPreferences lightThemeSharedPreferences;
    @Inject
    @Named("dark_theme")
    SharedPreferences darkThemeSharedPreferences;
    @Inject
    @Named("post_feed_scrolled_position_cache")
    SharedPreferences postFeedScrolledPositionSharedPreferences;
    @Inject
    @Named("amoled_theme")
    SharedPreferences amoledThemeSharedPreferences;
    @Inject
    @Named("main_activity_tabs")
    SharedPreferences mainActivityTabsSharedPreferences;
    @Inject
    @Named("nsfw_and_spoiler")
    SharedPreferences nsfwAndBlurringSharedPreferences;
    @Inject
    @Named("bottom_app_bar")
    SharedPreferences bottomAppBarSharedPreferences;
    @Inject
    @Named("post_history")
    SharedPreferences postHistorySharedPreferences;
    @Inject
    Executor executor;
    private Handler handler;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.advanced_preferences, rootKey);

        ((Infinity) activity.getApplication()).getAppComponent().inject(this);

        EditTextPreference setApiKeyPreference = findPreference(SharedPreferencesUtils.SET_API_KEY);
        EditTextPreference setApiUsernamePreference = findPreference(SharedPreferencesUtils.SET_API_USERNAME);
        Preference deleteSubredditsPreference = findPreference(SharedPreferencesUtils.DELETE_ALL_SUBREDDITS_DATA_IN_DATABASE);
        Preference deleteUsersPreference = findPreference(SharedPreferencesUtils.DELETE_ALL_USERS_DATA_IN_DATABASE);
        Preference deleteSortTypePreference = findPreference(SharedPreferencesUtils.DELETE_ALL_SORT_TYPE_DATA_IN_DATABASE);
        Preference deletePostLaoutPreference = findPreference(SharedPreferencesUtils.DELETE_ALL_POST_LAYOUT_DATA_IN_DATABASE);
        Preference deleteAllThemesPreference = findPreference(SharedPreferencesUtils.DELETE_ALL_THEMES_IN_DATABASE);
        Preference deletePostFeedScrolledPositionsPreference = findPreference(SharedPreferencesUtils.DELETE_FRONT_PAGE_SCROLLED_POSITIONS_IN_DATABASE);
        Preference deleteReadPostsPreference = findPreference(SharedPreferencesUtils.DELETE_READ_POSTS_IN_DATABASE);
        Preference deleteAllLegacySettingsPreference = findPreference(SharedPreferencesUtils.DELETE_ALL_LEGACY_SETTINGS);
        Preference resetAllSettingsPreference = findPreference(SharedPreferencesUtils.RESET_ALL_SETTINGS);
        Preference backupSettingsPreference = findPreference(SharedPreferencesUtils.BACKUP_SETTINGS);
        Preference restoreSettingsPreference = findPreference(SharedPreferencesUtils.RESTORE_SETTINGS);

        handler = new Handler(Looper.getMainLooper());

        if (setApiKeyPreference != null) {
            setApiKeyPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                APIUtils.setClientId((String)newValue);
                return true;
            });
        }

        if (setApiUsernamePreference != null) {
            setApiUsernamePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                APIUtils.setUserAgent((String)newValue);
                return true;
            });
        }

        if (deleteSubredditsPreference != null) {
            deleteSubredditsPreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> DeleteAllSubreddits.deleteAllSubreddits(executor, handler, mRedditDataRoomDatabase,
                                        () -> Toast.makeText(activity, R.string.delete_all_subreddits_success, Toast.LENGTH_SHORT).show()))
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (deleteUsersPreference != null) {
            deleteUsersPreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> DeleteAllUsers.deleteAllUsers(executor, handler, mRedditDataRoomDatabase,
                                        () -> Toast.makeText(activity, R.string.delete_all_users_success, Toast.LENGTH_SHORT).show()))
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (deleteSortTypePreference != null) {
            deleteSortTypePreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> DeleteAllSortTypes.deleteAllSortTypes(executor, handler,
                                mSharedPreferences, mSortTypeSharedPreferences, () -> {
                                    Toast.makeText(activity, R.string.delete_all_sort_types_success, Toast.LENGTH_SHORT).show();
                                    EventBus.getDefault().post(new RecreateActivityEvent());
                                }))
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (deletePostLaoutPreference != null) {
            deletePostLaoutPreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> DeleteAllPostLayouts.deleteAllPostLayouts(executor, handler,
                                mSharedPreferences, mPostLayoutSharedPreferences, () -> {
                                    Toast.makeText(activity, R.string.delete_all_post_layouts_success, Toast.LENGTH_SHORT).show();
                                    EventBus.getDefault().post(new RecreateActivityEvent());
                                }))
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (deleteAllThemesPreference != null) {
            deleteAllThemesPreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> DeleteAllThemes.deleteAllThemes(executor, handler,
                                mRedditDataRoomDatabase, lightThemeSharedPreferences,
                                        darkThemeSharedPreferences, amoledThemeSharedPreferences, () -> {
                                    Toast.makeText(activity, R.string.delete_all_themes_success, Toast.LENGTH_SHORT).show();
                                    EventBus.getDefault().post(new RecreateActivityEvent());
                                }))
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (deletePostFeedScrolledPositionsPreference != null) {
            deletePostFeedScrolledPositionsPreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> {
                            postFeedScrolledPositionSharedPreferences.edit().clear().apply();
                            Toast.makeText(activity, R.string.delete_all_front_page_scrolled_positions_success, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (deleteReadPostsPreference != null) {
            executor.execute(() -> {
                ReadPostDao readPostDao = mRedditDataRoomDatabase.readPostDao();
                int tableCount = readPostDao.getReadPostsCount(activity.accountName);
                long tableEntrySize = readPostDao.getMaxReadPostEntrySize();
                long tableSize = tableEntrySize * tableCount / 1024;
                handler.post(() -> deleteReadPostsPreference.setSummary(getString(R.string.settings_read_posts_db_summary, tableSize, tableCount)));
            });
            deleteReadPostsPreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> DeleteAllReadPosts.deleteAllReadPosts(executor, handler,
                                mRedditDataRoomDatabase, () -> {
                            Toast.makeText(activity, R.string.delete_all_read_posts_success, Toast.LENGTH_SHORT).show();
                        }))
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (deleteAllLegacySettingsPreference != null) {
            deleteAllLegacySettingsPreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> {
                            SharedPreferences.Editor editor = mSharedPreferences.edit();
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_1_TITLE_LEGACY);
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_2_TITLE_LEGACY);
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_3_TITLE_LEGACY);
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_1_POST_TYPE_LEGACY);
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_2_POST_TYPE_LEGACY);
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_3_POST_TYPE_LEGACY);
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_1_NAME_LEGACY);
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_2_NAME_LEGACY);
                            editor.remove(SharedPreferencesUtils.MAIN_PAGE_TAB_3_NAME_LEGACY);
                            editor.remove(SharedPreferencesUtils.NSFW_KEY_LEGACY);
                            editor.remove(SharedPreferencesUtils.BLUR_NSFW_KEY_LEGACY);
                            editor.remove(SharedPreferencesUtils.BLUR_SPOILER_KEY_LEGACY);
                            editor.remove(SharedPreferencesUtils.CONFIRM_TO_EXIT_LEGACY);
                            editor.remove(SharedPreferencesUtils.OPEN_LINK_IN_APP_LEGACY);
                            editor.remove(SharedPreferencesUtils.AUTOMATICALLY_TRY_REDGIFS_LEGACY);
                            editor.remove(SharedPreferencesUtils.DO_NOT_SHOW_REDDIT_API_INFO_AGAIN_LEGACY);
                            editor.remove(SharedPreferencesUtils.HIDE_THE_NUMBER_OF_AWARDS_LEGACY);
                            editor.remove(SharedPreferencesUtils.HIDE_COMMENT_AWARDS_LEGACY);

                            SharedPreferences.Editor sortTypeEditor = mSortTypeSharedPreferences.edit();
                            sortTypeEditor.remove(SharedPreferencesUtils.SORT_TYPE_ALL_POST_LEGACY);
                            sortTypeEditor.remove(SharedPreferencesUtils.SORT_TIME_ALL_POST_LEGACY);
                            sortTypeEditor.remove(SharedPreferencesUtils.SORT_TYPE_POPULAR_POST_LEGACY);
                            sortTypeEditor.remove(SharedPreferencesUtils.SORT_TIME_POPULAR_POST_LEGACY);

                            SharedPreferences.Editor postLayoutEditor = mPostLayoutSharedPreferences.edit();
                            postLayoutEditor.remove(SharedPreferencesUtils.POST_LAYOUT_ALL_POST_LEGACY);
                            postLayoutEditor.remove(SharedPreferencesUtils.POST_LAYOUT_POPULAR_POST_LEGACY);

                            SharedPreferences.Editor currentAccountEditor = mCurrentAccountSharedPreferences.edit();
                            currentAccountEditor.remove(SharedPreferencesUtils.APPLICATION_ONLY_ACCESS_TOKEN_LEGACY);

                            editor.apply();
                            sortTypeEditor.apply();
                            postLayoutEditor.apply();
                            currentAccountEditor.apply();
                            Toast.makeText(activity, R.string.delete_all_legacy_settings_success, Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (resetAllSettingsPreference != null) {
            resetAllSettingsPreference.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(activity, R.style.MaterialAlertDialogTheme)
                        .setTitle(R.string.are_you_sure)
                        .setPositiveButton(R.string.yes, (dialogInterface, i)
                                -> {
                            boolean disableNsfwForever = mSharedPreferences.getBoolean(SharedPreferencesUtils.DISABLE_NSFW_FOREVER, false);
                            mSharedPreferences.edit().clear().apply();
                            mainActivityTabsSharedPreferences.edit().clear().apply();
                            nsfwAndBlurringSharedPreferences.edit().clear().apply();

                            if (disableNsfwForever) {
                                mSharedPreferences.edit().putBoolean(SharedPreferencesUtils.DISABLE_NSFW_FOREVER, true).apply();
                            }

                            Toast.makeText(activity, R.string.reset_all_settings_success, Toast.LENGTH_SHORT).show();
                            EventBus.getDefault().post(new RecreateActivityEvent());
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
                return true;
            });
        }

        if (backupSettingsPreference != null) {
            backupSettingsPreference.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(ACTION_OPEN_DOCUMENT_TREE);
                startActivityForResult(intent, SELECT_BACKUP_SETTINGS_DIRECTORY_REQUEST_CODE);
                return true;
            });
        }

        if (restoreSettingsPreference != null) {
            restoreSettingsPreference.setOnPreferenceClickListener(preference -> {
                Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("application/zip");
                chooseFile = Intent.createChooser(chooseFile, "Choose a backup file");
                startActivityForResult(chooseFile, SELECT_RESTORE_SETTINGS_DIRECTORY_REQUEST_CODE);
                return true;
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_BACKUP_SETTINGS_DIRECTORY_REQUEST_CODE) {
                Uri uri = data.getData();
                BackupSettings.backupSettings(activity, executor, handler, activity.getContentResolver(), uri,
                        mRedditDataRoomDatabase, mSharedPreferences, lightThemeSharedPreferences, darkThemeSharedPreferences,
                        amoledThemeSharedPreferences, mSortTypeSharedPreferences, mPostLayoutSharedPreferences,
                        postFeedScrolledPositionSharedPreferences, mainActivityTabsSharedPreferences,
                        nsfwAndBlurringSharedPreferences, bottomAppBarSharedPreferences, postHistorySharedPreferences,
                        new BackupSettings.BackupSettingsListener() {
                            @Override
                            public void success() {
                                Toast.makeText(activity, R.string.backup_settings_success, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void failed(String errorMessage) {
                                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            } else if (requestCode == SELECT_RESTORE_SETTINGS_DIRECTORY_REQUEST_CODE) {
                Uri uri = data.getData();
                RestoreSettings.restoreSettings(activity, executor, handler, activity.getContentResolver(), uri,
                        mRedditDataRoomDatabase, mSharedPreferences, lightThemeSharedPreferences, darkThemeSharedPreferences,
                        amoledThemeSharedPreferences, mSortTypeSharedPreferences, mPostLayoutSharedPreferences,
                        postFeedScrolledPositionSharedPreferences, mainActivityTabsSharedPreferences,
                        nsfwAndBlurringSharedPreferences, bottomAppBarSharedPreferences, postHistorySharedPreferences,
                        new RestoreSettings.RestoreSettingsListener() {
                            @Override
                            public void success() {
                                Toast.makeText(activity, R.string.restore_settings_success, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void failed(String errorMessage) {
                                Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
            }
        }
    }
}
