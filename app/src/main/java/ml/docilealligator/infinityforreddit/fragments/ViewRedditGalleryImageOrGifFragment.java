package ml.docilealligator.infinityforreddit.fragments;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.github.piasy.biv.BigImageViewer;
import com.github.piasy.biv.loader.ImageLoader;
import com.github.piasy.biv.loader.glide.GlideImageLoader;

import java.io.File;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Named;

import me.saket.bettermovementmethod.BetterLinkMovementMethod;
import ml.docilealligator.infinityforreddit.BuildConfig;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.SaveMemoryCenterInisdeDownsampleStrategy;
import ml.docilealligator.infinityforreddit.SetAsWallpaperCallback;
import ml.docilealligator.infinityforreddit.activities.ViewRedditGalleryActivity;
import ml.docilealligator.infinityforreddit.asynctasks.SaveBitmapImageToFile;
import ml.docilealligator.infinityforreddit.asynctasks.SaveGIFToFile;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.CopyTextBottomSheetFragment;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.SetAsWallpaperBottomSheetFragment;
import ml.docilealligator.infinityforreddit.bottomsheetfragments.UrlMenuBottomSheetFragment;
import ml.docilealligator.infinityforreddit.customviews.GlideGifImageViewFactory;
import ml.docilealligator.infinityforreddit.databinding.FragmentViewRedditGalleryImageOrGifBinding;
import ml.docilealligator.infinityforreddit.post.Post;
import ml.docilealligator.infinityforreddit.services.DownloadMediaService;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;
import ml.docilealligator.infinityforreddit.utils.Utils;

public class ViewRedditGalleryImageOrGifFragment extends Fragment {

    public static final String EXTRA_REDDIT_GALLERY_MEDIA = "ERGM";
    public static final String EXTRA_SUBREDDIT_NAME = "ESN";
    public static final String EXTRA_INDEX = "EI";
    public static final String EXTRA_MEDIA_COUNT = "EMC";
    public static final String EXTRA_IS_NSFW = "EIN";
    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 0;

    @Inject
    @Named("default")
    SharedPreferences mSharedPreferences;
    @Inject
    Executor mExecutor;

    private ViewRedditGalleryActivity activity;
    private RequestManager glide;
    private Post.Gallery media;
    private String subredditName;
    private boolean isNsfw;
    private boolean isDownloading = false;
    private boolean isUseBottomCaption = false;
    private boolean isFallback = false;
    private Handler handler;
    private FragmentViewRedditGalleryImageOrGifBinding binding;

    public ViewRedditGalleryImageOrGifFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        BigImageViewer.initialize(GlideImageLoader.with(activity.getApplicationContext()));

        binding = FragmentViewRedditGalleryImageOrGifBinding.inflate(inflater, container, false);

        ((Infinity) activity.getApplication()).getAppComponent().inject(this);

        setHasOptionsMenu(true);

        media = getArguments().getParcelable(EXTRA_REDDIT_GALLERY_MEDIA);
        subredditName = getArguments().getString(EXTRA_SUBREDDIT_NAME);
        isNsfw = getArguments().getBoolean(EXTRA_IS_NSFW, false);
        glide = Glide.with(activity);
        handler = new Handler(Looper.getMainLooper());

        if (activity.typeface != null) {
            binding.titleTextViewViewRedditGalleryImageOrGifFragment.setTypeface(activity.typeface);
            binding.captionTextViewViewRedditGalleryImageOrGifFragment.setTypeface(activity.typeface);
            binding.captionUrlTextViewViewRedditGalleryImageOrGifFragment.setTypeface(activity.typeface);
        }

        binding.imageViewViewRedditGalleryImageOrGifFragment.setImageViewFactory(new GlideGifImageViewFactory(new SaveMemoryCenterInisdeDownsampleStrategy(Integer.parseInt(mSharedPreferences.getString(SharedPreferencesUtils.POST_FEED_MAX_RESOLUTION, "5000000")))));

        binding.imageViewViewRedditGalleryImageOrGifFragment.setImageLoaderCallback(new ImageLoader.Callback() {
            @Override
            public void onCacheHit(int imageType, File image) {

            }

            @Override
            public void onCacheMiss(int imageType, File image) {

            }

            @Override
            public void onStart() {

            }

            @Override
            public void onProgress(int progress) {

            }

            @Override
            public void onFinish() {

            }

            @Override
            public void onSuccess(File image) {
                binding.progressBarViewRedditGalleryImageOrGifFragment.setVisibility(View.GONE);

                final SubsamplingScaleImageView view = binding.imageViewViewRedditGalleryImageOrGifFragment.getSSIV();

                if (view != null) {
                    view.setOnImageEventListener(new SubsamplingScaleImageView.DefaultOnImageEventListener() {
                        @Override
                        public void onImageLoaded() {
                            view.setMinimumDpi(80);
                            view.setDoubleTapZoomDpi(240);
                            view.setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_FIXED);
                            view.setQuickScaleEnabled(true);
                            view.resetScaleAndCenter();
                        }

                        @Override
                        public void onImageLoadError(Exception e) {
                            e.printStackTrace();
                            // For issue #558
                            // Make sure it's not stuck in a loop if it comes to that
                            // Fallback url should be empty if it's not an album item
                            if (!isFallback && media.hasFallback()) {
                                binding.imageViewViewRedditGalleryImageOrGifFragment.cancel();
                                isFallback = true;
                                loadImage();
                            } else {
                                isFallback = false;
                            }
                        }
                    });
                }
            }

            @Override
            public void onFail(Exception error) {
                binding.progressBarViewRedditGalleryImageOrGifFragment.setVisibility(View.GONE);
                binding.loadImageErrorLinearLayoutViewRedditGalleryImageOrGifFragment.setVisibility(View.VISIBLE);
            }
        });

        //loadImage();

        String caption = media.caption;
        String captionUrl = media.captionUrl;
        boolean captionIsEmpty = TextUtils.isEmpty(caption);
        boolean captionUrlIsEmpty = TextUtils.isEmpty(captionUrl);
        boolean captionTextOrUrlIsNotEmpty = !captionIsEmpty || !captionUrlIsEmpty;

        binding.imageViewViewRedditGalleryImageOrGifFragment.setOnClickListener(view -> {
            if (activity.isActionBarHidden()) {
                activity.getWindow().getDecorView().setSystemUiVisibility(0);
                activity.setActionBarHidden(false);
                if (activity.isUseBottomAppBar()) {
                    binding.bottomAppBarMenuViewRedditGalleryImageOrGifFragment.setVisibility(View.VISIBLE);
                }
                if (captionTextOrUrlIsNotEmpty) {
                    binding.captionLayoutViewRedditGalleryImageOrGifFragment.setVisibility(View.VISIBLE);
                }
            } else {
                hideAppBar();
            }
        });

        binding.captionLayoutViewRedditGalleryImageOrGifFragment.setOnClickListener(view -> hideAppBar());

        binding.loadImageErrorLinearLayoutViewRedditGalleryImageOrGifFragment.setOnClickListener(view -> {
            binding.progressBarViewRedditGalleryImageOrGifFragment.setVisibility(View.VISIBLE);
            binding.loadImageErrorLinearLayoutViewRedditGalleryImageOrGifFragment.setVisibility(View.GONE);
            loadImage();
        });

        if (activity.isUseBottomAppBar()) {
            binding.bottomAppBarMenuViewRedditGalleryImageOrGifFragment.setVisibility(View.VISIBLE);
            if (media.mediaType == Post.Gallery.TYPE_GIF) {
                binding.titleTextViewViewRedditGalleryImageOrGifFragment.setText(getString(R.string.view_reddit_gallery_activity_gif_label,
                        getArguments().getInt(EXTRA_INDEX) + 1, getArguments().getInt(EXTRA_MEDIA_COUNT)));
            } else {
                binding.titleTextViewViewRedditGalleryImageOrGifFragment.setText(getString(R.string.view_reddit_gallery_activity_image_label,
                        getArguments().getInt(EXTRA_INDEX) + 1, getArguments().getInt(EXTRA_MEDIA_COUNT)));
            }
            binding.downloadImageViewViewRedditGalleryImageOrGifFragment.setOnClickListener(view -> {
                if (isDownloading) {
                    return;
                }
                isDownloading = true;
                requestPermissionAndDownload();
            });
            binding.shareImageViewViewRedditGalleryImageOrGifFragment.setOnClickListener(view -> {
                if (media.mediaType == Post.Gallery.TYPE_GIF) {
                    shareGif();
                } else {
                    shareImage();
                }
            });
            binding.wallpaperImageViewViewRedditGalleryImageOrGifFragment.setOnClickListener(view -> {
                setWallpaper();
            });
        }

        if (captionTextOrUrlIsNotEmpty) {
            isUseBottomCaption = true;

            if (!activity.isUseBottomAppBar()) {
                binding.bottomAppBarMenuViewRedditGalleryImageOrGifFragment.setVisibility(View.GONE);
            }

            binding.captionLayoutViewRedditGalleryImageOrGifFragment.setVisibility(View.VISIBLE);

            if (!captionIsEmpty) {
                binding.captionTextViewViewRedditGalleryImageOrGifFragment.setVisibility(View.VISIBLE);
                binding.captionTextViewViewRedditGalleryImageOrGifFragment.setText(caption);
                binding.captionTextViewViewRedditGalleryImageOrGifFragment.setOnClickListener(view -> hideAppBar());
                binding.captionTextViewViewRedditGalleryImageOrGifFragment.setOnLongClickListener(view -> {
                    if (activity != null
                            && !activity.isDestroyed()
                            && !activity.isFinishing()
                            && binding.captionTextViewViewRedditGalleryImageOrGifFragment.getSelectionStart() == -1
                            && binding.captionTextViewViewRedditGalleryImageOrGifFragment.getSelectionEnd() == -1) {
                        CopyTextBottomSheetFragment.show(
                                activity.getSupportFragmentManager(), caption, null);
                    }
                    return true;
                });
            }
            if (!captionUrlIsEmpty) {
                String scheme = Uri.parse(captionUrl).getScheme();
                String urlWithoutScheme = "";
                if (!TextUtils.isEmpty(scheme)) {
                    urlWithoutScheme = captionUrl.substring(scheme.length() + 3);
                }

                binding.captionUrlTextViewViewRedditGalleryImageOrGifFragment.setText(TextUtils.isEmpty(urlWithoutScheme) ? captionUrl : urlWithoutScheme);

                BetterLinkMovementMethod.linkify(Linkify.WEB_URLS, binding.captionUrlTextViewViewRedditGalleryImageOrGifFragment).setOnLinkLongClickListener((textView, url) -> {
                    if (activity != null && !activity.isDestroyed() && !activity.isFinishing()) {
                        UrlMenuBottomSheetFragment urlMenuBottomSheetFragment = UrlMenuBottomSheetFragment.newInstance(captionUrl);
                        urlMenuBottomSheetFragment.show(activity.getSupportFragmentManager(), null);
                    }
                    return true;
                });
                binding.captionUrlTextViewViewRedditGalleryImageOrGifFragment.setVisibility(View.VISIBLE);
                binding.captionUrlTextViewViewRedditGalleryImageOrGifFragment.setHighlightColor(Color.TRANSPARENT);
            }
        } else {
            binding.captionLayoutViewRedditGalleryImageOrGifFragment.setVisibility(View.GONE);
        }

        return binding.getRoot();
    }

    private void hideAppBar() {
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
        activity.setActionBarHidden(true);
        if (activity.isUseBottomAppBar()) {
            binding.bottomAppBarMenuViewRedditGalleryImageOrGifFragment.setVisibility(View.GONE);
        }
        binding.captionLayoutViewRedditGalleryImageOrGifFragment.setVisibility(View.GONE);
    }

    private void loadImage() {
        if (isFallback) {
            binding.imageViewViewRedditGalleryImageOrGifFragment.showImage(Uri.parse(media.fallbackUrl));
        } else {
            binding.imageViewViewRedditGalleryImageOrGifFragment.showImage(Uri.parse(media.url));
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.view_reddit_gallery_image_or_gif_fragment, menu);
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Utils.setTitleWithCustomFontToMenuItem(activity.typeface, item, null);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_download_view_reddit_gallery_image_or_gif_fragment) {
            if (isDownloading) {
                return false;
            }
            isDownloading = true;
            requestPermissionAndDownload();
            return true;
        } else if (itemId == R.id.action_share_view_reddit_gallery_image_or_gif_fragment) {
            if (media.mediaType == Post.Gallery.TYPE_GIF) {
                shareGif();
            } else {
                shareImage();
            }
            return true;
        } else if (itemId == R.id.action_copy_clipboard_view_reddit_gallery_image_or_gif_fragment) {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", media.hasFallback() ? media.fallbackUrl : media.url);
            clipboard.setPrimaryClip(clip);
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                Toast.makeText(getActivity(), "Copied", Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        return false;
    }

    private void requestPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                // Permission is not granted
                // No explanation needed; request the permission
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE);
            } else {
                // Permission has already been granted
                download();
            }
        } else {
            download();
        }
    }

    private void download() {
        isDownloading = false;

        PersistableBundle extras = new PersistableBundle();
        extras.putString(DownloadMediaService.EXTRA_URL, media.hasFallback() ? media.fallbackUrl : media.url); // Retrieve original instead of the one additionally compressed by reddit
        extras.putInt(DownloadMediaService.EXTRA_MEDIA_TYPE, media.mediaType == Post.Gallery.TYPE_GIF ? DownloadMediaService.EXTRA_MEDIA_TYPE_GIF: DownloadMediaService.EXTRA_MEDIA_TYPE_IMAGE);
        extras.putString(DownloadMediaService.EXTRA_FILE_NAME, media.fileName);
        extras.putString(DownloadMediaService.EXTRA_SUBREDDIT_NAME, subredditName);
        extras.putInt(DownloadMediaService.EXTRA_IS_NSFW, isNsfw ? 1 : 0);

        //TODO: contentEstimatedBytes
        JobInfo jobInfo = DownloadMediaService.constructJobInfo(activity, 5000000, extras);
        ((JobScheduler) activity.getSystemService(Context.JOB_SCHEDULER_SERVICE)).schedule(jobInfo);

        Toast.makeText(activity, R.string.download_started, Toast.LENGTH_SHORT).show();
    }

    //TODO: Find a way to share original image, Glide messes with the size and quality,
    // compression should be up to the app being shared with (WhatsApp for example)
    private void shareImage() {
        glide.asBitmap().load(media.hasFallback() ? media.fallbackUrl : media.url).into(new CustomTarget<Bitmap>() {
            @Override
            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                if (activity.getExternalCacheDir() != null) {
                    Toast.makeText(activity, R.string.save_image_first, Toast.LENGTH_SHORT).show();
                    SaveBitmapImageToFile.SaveBitmapImageToFile(mExecutor, handler, resource, activity.getExternalCacheDir().getPath(),
                            media.fileName,
                            new SaveBitmapImageToFile.SaveBitmapImageToFileListener() {
                                @Override
                                public void saveSuccess(File imageFile) {
                                    Uri uri = FileProvider.getUriForFile(activity,
                                            BuildConfig.APPLICATION_ID + ".provider", imageFile);
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                    shareIntent.setType("image/*");
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
                                }

                                @Override
                                public void saveFailed() {
                                    Toast.makeText(activity,
                                            R.string.cannot_save_image, Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(activity,
                            R.string.cannot_get_storage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {

            }
        });
    }

    private void shareGif() {
        Toast.makeText(activity, R.string.save_gif_first, Toast.LENGTH_SHORT).show();
        glide.asGif().load(media.url).listener(new RequestListener<>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, @NonNull Target<GifDrawable> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(GifDrawable resource, Object model, Target<GifDrawable> target, DataSource dataSource, boolean isFirstResource) {
                if (activity.getExternalCacheDir() != null) {
                    SaveGIFToFile.saveGifToFile(mExecutor, handler, resource, activity.getExternalCacheDir().getPath(), media.fileName,
                            new SaveGIFToFile.SaveGIFToFileListener() {
                                @Override
                                public void saveSuccess(File imageFile) {
                                    Uri uri = FileProvider.getUriForFile(activity,
                                            BuildConfig.APPLICATION_ID + ".provider", imageFile);
                                    Intent shareIntent = new Intent();
                                    shareIntent.setAction(Intent.ACTION_SEND);
                                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                                    shareIntent.setType("image/*");
                                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
                                }

                                @Override
                                public void saveFailed() {
                                    Toast.makeText(activity,
                                            R.string.cannot_save_gif, Toast.LENGTH_SHORT).show();
                                }
                            });
                } else {
                    Toast.makeText(activity,
                            R.string.cannot_get_storage, Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        }).submit();
    }

    private void setWallpaper() {
        if (media.mediaType != Post.Gallery.TYPE_GIF) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                SetAsWallpaperBottomSheetFragment setAsWallpaperBottomSheetFragment = new SetAsWallpaperBottomSheetFragment();
                Bundle bundle = new Bundle();
                bundle.putInt(SetAsWallpaperBottomSheetFragment.EXTRA_VIEW_PAGER_POSITION, activity.getCurrentPagePosition());
                setAsWallpaperBottomSheetFragment.setArguments(bundle);
                setAsWallpaperBottomSheetFragment.show(activity.getSupportFragmentManager(), setAsWallpaperBottomSheetFragment.getTag());
            } else {
                ((SetAsWallpaperCallback) activity).setToBoth(activity.getCurrentPagePosition());
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(activity, R.string.no_storage_permission, Toast.LENGTH_SHORT).show();
                isDownloading = false;
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED && isDownloading) {
                download();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        activity = (ViewRedditGalleryActivity) context;
    }

    @Override
    public void onResume() {
        super.onResume();
        SubsamplingScaleImageView ssiv = binding.imageViewViewRedditGalleryImageOrGifFragment.getSSIV();
        if (ssiv == null || !ssiv.hasImage()) {
            loadImage();
        }

        if (activity.isActionBarHidden()) {
            activity.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE);
        } else {
            activity.getWindow().getDecorView().setSystemUiVisibility(0);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        binding.imageViewViewRedditGalleryImageOrGifFragment.cancel();
        SubsamplingScaleImageView subsamplingScaleImageView = binding.imageViewViewRedditGalleryImageOrGifFragment.getSSIV();
        if (subsamplingScaleImageView != null) {
            subsamplingScaleImageView.recycle();
        }
    }

//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        binding.imageViewViewRedditGalleryImageOrGifFragment.cancel();
//        isFallback = false;
//        SubsamplingScaleImageView subsamplingScaleImageView = binding.imageViewViewRedditGalleryImageOrGifFragment.getSSIV();
//        if (subsamplingScaleImageView != null) {
//            subsamplingScaleImageView.recycle();
//        }
//    }
}
