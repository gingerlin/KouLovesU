package com.hch.koulovesu;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.hch.koulovesu.DiskLruCache.Snapshot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.Environment;

public class BitmapCache {

	public static final int TYPE_USER_THUMBNAIL = 0;
	private static final int TYPE_COUNT = 1;
	private static final String[] DIRECTORIES_NAME = { "user_thumbnail" };

	private static final int DISK_CACHE_SIZE_DEFAULT = 1024 * 1024 * 10; // 10MB

	private static DiskLruCache[] mDiskLruCaches;

	private static boolean mDiskCacheStarting = true;
	private static final Object mDiskCacheLock = new Object();

	private static Context context;

	public static void initialize(final Context context) {

		BitmapCache.context = context;

		new Thread(new Runnable() {
			@Override
			public void run() {
				synchronized (mDiskCacheLock) {
					try {
						mDiskLruCaches = new DiskLruCache[TYPE_COUNT];

						mDiskLruCaches[TYPE_USER_THUMBNAIL] = DiskLruCache
								.open(getBitmapDiskCacheDirectory(DIRECTORIES_NAME[TYPE_USER_THUMBNAIL]),
										Utils.getAppVersion(context), 1,
										DISK_CACHE_SIZE_DEFAULT);

						mDiskCacheStarting = false; // Finished initialization
						mDiskCacheLock.notifyAll(); // Wake any waiting threads
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	public static boolean isBitmapCached(String id, int type) {
		File file = new File(
				(getBitmapDiskCacheDirectory(DIRECTORIES_NAME[type])
						+ File.separator + id + ".0"));
		return file.exists();
	}

	public static void put(String key, int type, Bitmap bitmap) {
		synchronized (mDiskCacheLock) {
			try {
				if (mDiskLruCaches != null
						&& mDiskLruCaches[type].get(key) == null) {
					DiskLruCache.Editor editor = mDiskLruCaches[type].edit(key);
					if (editor != null) {
						try {
							OutputStream outputStream = editor
									.newOutputStream(0);

							boolean success = bitmap.compress(
									CompressFormat.PNG, 100, outputStream);
							outputStream.close();
							if (success) {
								editor.commit();
							} else {
								editor.abort();
							}
						} catch (IOException e) {
							editor.abort();
						}
					}
					mDiskLruCaches[type].flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static Bitmap get(String key, int type) {
		synchronized (mDiskCacheLock) {
			// Wait while disk cache is started from background thread
			while (mDiskCacheStarting) {
				try {
					mDiskCacheLock.wait();
				} catch (InterruptedException e) {
				}
			}
			if (mDiskLruCaches != null) {
				Snapshot snapShot;
				try {
					snapShot = mDiskLruCaches[type].get(key);
					if (snapShot != null) {

						InputStream is = snapShot.getInputStream(0);
						Bitmap bitmap = BitmapFactory.decodeStream(is);
						is.close();
						return bitmap;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}
		return inSampleSize;
	}

	public static File getBitmapDiskCacheDirectory(String uniqueName) {
		String cachePath;
		if (Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState())
				|| !Environment.isExternalStorageRemovable()) {
			cachePath = context.getExternalCacheDir().getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}
		return new File(cachePath + File.separator + "bitmap" + File.separator
				+ uniqueName);
	}
}
