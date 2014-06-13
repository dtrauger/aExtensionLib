package com.derektrauger.library.imaging;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

import com.derektrauger.library.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ImageManager {
    /* Static members */
    private static final String LOG_TAG = "ImageManager";
    private static final boolean LOG_CACHE_OPERATIONS = true;
    private static final int placeholder = Color.parseColor("#eeeeee");
    private static final Map<ImageView, String> imageViews = Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    private static LruCache<String, Bitmap> memoryCache;
    private static DiskLruImageCache diskCache;

    /* Instance members */
    private final ExecutorService downloadThreadPool;
    private final Context context;

    private ImageManagerCallback callback;

    public interface ImageManagerCallback {
        void onImageLoaded(ImageView imageView);
        void onImageDownloaded(String urlString);
        void onNullBitmap();
    }

    public static class ImageManagerOptions {
        public boolean roundedCorners = false;
        public boolean fadeIn = true;
        public int cornerRadius = 5;
        public int requestedWidth;
        public int requestedHeight;

        public ImageManagerOptions() {
            this(0, 0);
        }

        public ImageManagerOptions(ImageView imgView) {
            this(imgView.getWidth(), imgView.getHeight());
        }

        public ImageManagerOptions(int requestedWidth, int requestedHeight) {
            this.requestedWidth = requestedWidth;
            this.requestedHeight = requestedHeight;
        }
    }

    public ImageManager(DiskLruImageCache diskImageCache, Context context) {
        this.context = context;

        if (diskCache == null) {
            diskCache = diskImageCache;
        }

        downloadThreadPool = Executors.newFixedThreadPool(6);

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        if (memoryCache == null) {
            Log.d(LOG_TAG, "Initializing LruCache with size " + cacheSize + "KB");

            memoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(final String key, final Bitmap bitmap) {
                    // The cache size will be measured in bytes rather than
                    // number of items.
                    return Utils.getSizeInBytes(bitmap) / 1024;
                }
            };
        }
    }

    public Bitmap getBitmapFromCache(String urlString) {
        Bitmap bitmap = getBitmapFromLRUCache(urlString);

        if (bitmap != null) {
            return bitmap;
        }

        return getBitmapFromDiskCache(urlString);
    }

    public void addBitmapToCache(String key, Bitmap bitmap) {
        if (getBitmapFromLRUCache(key) == null) {
            memoryCache.put(key, bitmap);
        }

        String diskCacheKey = getDiskCacheKey(key);

        if (diskCache != null && !diskCache.containsKey(diskCacheKey)) {
            diskCache.put(diskCacheKey, bitmap);
        }
    }

    public void clearCache(String urlString) {
        memoryCache.remove(urlString);
        if (diskCache == null) return;
        File cacheFile = new File(diskCache.getCacheFolder(), getDiskCacheKey(urlString));
        if (cacheFile.isFile()) {
            cacheFile.delete();
        }
    }

    private Bitmap getBitmapFromLRUCache(String urlString) {
        Bitmap cachedBitmap = memoryCache.get(urlString);

        if (cachedBitmap == null) {
            return null;
        }

        if (LOG_CACHE_OPERATIONS) {
            Log.v(LOG_TAG, "Item loaded from LRU cache: " + urlString);
        }

        return cachedBitmap;
    }

    public Bitmap getBitmapFromDiskCache(String urlString) {
        String key = getDiskCacheKey(urlString);
        Bitmap cachedBitmap = diskCache.getBitmap(key);

        if (cachedBitmap == null) {
            return null;
        }

        if (LOG_CACHE_OPERATIONS) {
            Log.v(LOG_TAG, "image read from Disk cache: " + key);
        }

        return cachedBitmap;
    }

    private static String getDiskCacheKey(String urlString) {
    	String sanitizedKey = Utils.getMD5(urlString);
        sanitizedKey = sanitizedKey.replaceAll("[^a-z0-9_]", "");
        if (LOG_CACHE_OPERATIONS) {
            Log.d(LOG_TAG, "cache key: " + sanitizedKey.substring(0, Math.min(63, sanitizedKey.length())));
        }
        return sanitizedKey.substring(0, Math.min(63, sanitizedKey.length()));
    }

    private void queueJob(final String urlString) {
        downloadThreadPool.submit(new Runnable() {
            public void run() {
                downloadBitmap(urlString);

                if (LOG_CACHE_OPERATIONS) {
                    Log.d(LOG_TAG, "Image downloaded: " + urlString);
                }
                if (callback != null) {
                    callback.onImageDownloaded(urlString);
                }
            }
        });
    }

    private void queueJob(final String urlString, final ImageView imageView, final ImageManagerOptions options) {
        final Handler handler = new ImageManagerHandler(this, imageView, urlString, options);

        downloadThreadPool.submit(new Runnable() {
            public void run() {
                final BitmapDrawable drawable = downloadDrawable(urlString, options);
                Message message = handler.obtainMessage(1, drawable);

                if (LOG_CACHE_OPERATIONS) {
                    Log.d(LOG_TAG, "Image downloaded: " + urlString);
                }

                handler.sendMessage(message);
            }
        });
    }

    public void loadImage(final String urlString) {
        if (getBitmapFromCache(urlString) != null) {
        	if (callback != null) {
                callback.onImageDownloaded(urlString);
            }
            return;
        }
        queueJob(urlString);
    }

    public void loadImage(final String urlString, final ImageView imageView, final ImageManagerOptions options) {
        imageViews.put(imageView, urlString);

        Bitmap bitmap = getBitmapFromCache(urlString);

        if (bitmap != null) {
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
            // we do not want to fade in if the image is already cached
            // to make things smoother
            options.fadeIn = false;
            setImageDrawable(imageView, drawable, options);
            return;
        }

        imageView.setImageResource(placeholder);
        queueJob(urlString, imageView, options);
    }

    private void setImageDrawable(final ImageView imageView, BitmapDrawable bitmapDrawable, final ImageManagerOptions options) {
        if (options.roundedCorners) {
            BitmapProcessor processor = new BitmapProcessor(context);
            bitmapDrawable = processor.getRoundedCorners(bitmapDrawable, options.cornerRadius);
        }

        imageView.setImageDrawable(bitmapDrawable);

        if (options.fadeIn) {
            Utils.fadeIn(imageView);
        }

        if (callback != null) {
            callback.onImageLoaded(imageView);
        }
    }

    private void downloadBitmap(final String urlString) {
        InputStream inputStream = null;
        URL url = null;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Failed to download bitmap", e);
        }

        if (url == null) {
            callback.onNullBitmap();
            return;
        }

        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
//            urlConnection.setChunkedStreamingMode(0);
            urlConnection.setRequestProperty( "Accept-Encoding", "" );
            urlConnection.setDoOutput(false);
            inputStream = new BufferedInputStream(urlConnection.getInputStream());
        } catch (IOException e) {
//            Log.e(LOG_TAG, "Failed to download bitmap: " + urlString, e);
        }

        if (inputStream== null) {
            callback.onNullBitmap();
            return;
        }

        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        
        if (bitmap == null) {
        	Log.d(LOG_TAG, "null bitmap");
        	if (callback != null) {
                callback.onNullBitmap();
            }
        }
        
        addBitmapToCache(urlString, bitmap);
    }

    private BitmapDrawable downloadDrawable(final String urlString, ImageManagerOptions options) {
        Bitmap bitmap = BitmapProcessor.decodeSampledBitmapFromUrl(urlString, options.requestedWidth, options.requestedHeight);

        addBitmapToCache(urlString, bitmap);

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    public void setCallback(ImageManagerCallback callback) {
        this.callback = callback;
    }

    /**
* Drawable Handler inner class
*
*/
    private static final class ImageManagerHandler extends Handler {
        private final ImageView imageView;
        private final String url;
        private final ImageManagerOptions options;
        private final WeakReference<ImageManager> imageManager;

        private ImageManagerHandler(ImageManager imageManager, ImageView imageView, String url, ImageManagerOptions options) {
            this.imageManager = new WeakReference<ImageManager>(imageManager);
            this.imageView = imageView;
            this.url = url;
            this.options = options;
        }

        @Override
        public void handleMessage(Message msg) {
            String tag = imageViews.get(imageView);

            if (tag != null && tag.equals(url)) {
                if (msg.obj != null) {
                    imageManager.get().setImageDrawable(imageView, (BitmapDrawable) msg.obj, options);
                } else {
                    imageView.setImageResource(placeholder);
                    Log.e(LOG_TAG, "failed " + url);
                }
            }
        }
    }
}