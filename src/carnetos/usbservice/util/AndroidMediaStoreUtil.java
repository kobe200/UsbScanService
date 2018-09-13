
package carnetos.usbservice.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import carnetos.usbservice.entity.MediaItem.MediaType;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * 提供删除Android系统媒体库中Audio、Video、Image媒体操作工具类
 */
public class AndroidMediaStoreUtil {

    private static final String TAG = "AndroidMediaStoreUtil";

    public static final boolean DEBUG = true;

    private AudioStoreUtil mAudioStoreUtil;

    private VideoStoreUtil mVideoStoreUtil;

    private ImageStoreUtil mImageStoreUtil;

    private WeakReference<Context> mContext;

    private ContentResolver getContentResolver() {
        if (mContext == null || mContext.get() == null)
            return null;
        return mContext.get().getContentResolver();
    }

    public AndroidMediaStoreUtil(Context context) {
        /* 保存参数 */
        mContext = new WeakReference<Context>(context);

        /* 初始化变量 */
        mAudioStoreUtil = new AudioStoreUtil();
        mVideoStoreUtil = new VideoStoreUtil();
        mImageStoreUtil = new ImageStoreUtil();
    }

    public void deleteAllMediaStore() {
        mAudioStoreUtil.deleteAll();
        mVideoStoreUtil.deleteAll();
        mImageStoreUtil.deleteAll();
    }

    public void deleteAllAudio() {
        mAudioStoreUtil.deleteAll();
    }

    public void deleteAllVideo() {
        mVideoStoreUtil.deleteAll();
    }

    public void deleteAllImage() {
        mImageStoreUtil.deleteAll();
    }

    public void deleteMediaByID(MediaType mediaType, String id) {
        if (mediaType == MediaType.AUDIO) {
            deleteAudioByID(id);
        } else if (mediaType == MediaType.VIDEO) {
            deleteVideoByID(id);
        } else if (mediaType == MediaType.IMAGE) {
            deleteVideoByID(id);
        }
    }

    public void deleteAudioByID(String id) {
        mAudioStoreUtil.deleteByID(id);
    }

    public void deleteVideoByID(String id) {
        mVideoStoreUtil.deleteByID(id);
    }

    public void deleteImageByID(String id) {
        mImageStoreUtil.deleteByID(id);
    }

    public void deleteAudioByAlbum(String album) {
        mAudioStoreUtil.deleteByAlbum(album);
    }

    public void deleteVideoByAlbum(String album) {
        mVideoStoreUtil.deleteByAlbum(album);
    }

    public void deleteAudioByArtist(String artist) {
        mAudioStoreUtil.deleteByArtist(artist);
    }

    public void deleteVideoByArtist(String artist) {
        mVideoStoreUtil.deleteByArtist(artist);
    }

    public void deleteAudioByDataAndSize(String data, String size) {
        mAudioStoreUtil.deleteMediaByDataAndSize(data, size);
    }

    public void deleteVideoByDataAndSize(String data, String size) {
        mVideoStoreUtil.deleteMediaByDataAndSize(data, size);
    }

    public void deleteImageByDataAndSize(String data, String size) {
        mImageStoreUtil.deleteMediaByDataAndSize(data, size);
    }

    public boolean exists(MediaType mediaType, long mediaID, String filePath) {
        boolean result = false;
        switch (mediaType) {
            case IMAGE:
                result = mImageStoreUtil.exists(mediaID, filePath);
                break;
            case VIDEO:
                result = mVideoStoreUtil.exists(mediaID, filePath);
                break;
            case AUDIO:
                result = mAudioStoreUtil.exists(mediaID, filePath);
                break;
            default:
                break;
        }
        return result;
    }

    public void deleteThumbnail(MediaType mediaType, long mediaID) {
        Log.d(TAG, "deleteThumbnail:" + mediaID);
        switch (mediaType) {
            case IMAGE:
                deleteImageThumbnail(mediaID);
                break;
            case VIDEO:
                deleteVideoThumbnail(mediaID);
                break;
            case AUDIO:
                break;
            default:
                break;
        }
    }

    private void deleteVideoThumbnail(long mediaID) {
        int affectedRows = mVideoStoreUtil.deleteThumbnail(mediaID);
        Log.d(TAG, String.format("deleteVideoThumbnail %d :%d", mediaID, affectedRows));
    }

    private void deleteImageThumbnail(long mediaID) {

        int ret = mImageStoreUtil.deleteThumbnail(mediaID);
        Log.d(TAG, String.format("deleteImageThumbnail imageid %d,affected rows :%d", mediaID, ret));
    }

    private void testDumpMediaInfo(Uri table) {
        Log.d(TAG, "\ntable: " + table.getEncodedPath());
        ContentResolver contentResolver = getContentResolver();
        if (contentResolver == null) {
            Log.d(TAG, "contentResolver == null");
            return;
        }
        Cursor cursor = contentResolver.query(table, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    int count = cursor.getColumnCount();
                    for (int i = 0; i < count; i++) {

                        String info = cursor.getString(cursor.getColumnIndexOrThrow(cursor
                                .getColumnName(i)));
                        Log.d(TAG, cursor.getColumnName(i) + ":" + info);
                    }
                    Log.d(TAG, "\n");
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }

    public void testDumpAllMediaInfo() {
        testDumpMediaInfo(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        testDumpMediaInfo(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI);

        testDumpMediaInfo(MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        testDumpMediaInfo(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI);

        testDumpMediaInfo(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        testDumpMediaInfo(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI);
        testDumpMediaInfo(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI);
        testDumpMediaInfo(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI);
        testDumpMediaInfo(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI);
    }

    /**
     * 提供删除Android系统媒体库中Audio媒体操作工具类
     */
    private final class AudioStoreUtil extends AudioVedioStoreUtilBase {

        protected AudioStoreUtil() {
            super();

            EXTERNAL_CONTENT_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            ARTIST = MediaStore.Audio.Media.ARTIST;
            ALBUM = MediaStore.Audio.Media.ALBUM;
        }

        @Override
        protected void deleteExtern() {
            // Nothing
        }

    }

    /**
     * 提供删除Android系统媒体库中Video媒体操作工具类
     */
    private final class VideoStoreUtil extends AudioVedioStoreUtilBase {

        protected VideoStoreUtil() {
            super();

            EXTERNAL_CONTENT_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            ARTIST = MediaStore.Video.Media.ARTIST;
            ALBUM = MediaStore.Video.Media.ALBUM;
        }

        @Override
        protected void deleteExtern() {
            clearOldThumbnails();
        }

        private void clearOldThumbnails() {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                Log.d(TAG, "contentResolver == null");
                return;
            }
            Cursor cursor = contentResolver.query(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    null, null, null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        String id = cursor.getString(cursor
                                .getColumnIndexOrThrow(MediaStore.Video.Thumbnails.VIDEO_ID));
                        Cursor thumbCursor = contentResolver.query(EXTERNAL_CONTENT_URI, null,
                                MediaStore.Video.Media._ID + "=" + id, null, null);
                        if (!thumbCursor.moveToFirst()) {
                            contentResolver.delete(
                                    MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                                    MediaStore.Video.Thumbnails.VIDEO_ID + "= " + id, null);
                        }
                        thumbCursor.close();
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }

        public int deleteThumbnail(long mediaID) {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                Log.d(TAG, "contentResolver == null");
                return 0;
            }
            return contentResolver.delete(MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI,
                    MediaStore.Video.Thumbnails.VIDEO_ID + "= " + mediaID, null);
        }

    }

    /**
     * 提供删除Android系统媒体库中Image媒体操作工具类
     */
    private final class ImageStoreUtil extends StoreUtilBase {

        protected ImageStoreUtil() {
            super();

            EXTERNAL_CONTENT_URI = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        @Override
        protected void deleteExtern() {
            clearOldThumbnails();
        }

        private void clearOldThumbnails() {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                Log.d(TAG, "contentResolver == null");
                return;
            }
            Cursor cursor = contentResolver.query(
                    MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, null, null, null, null);

            if (cursor != null) {

                if (cursor.moveToFirst()) {
                    do {
                        String id = cursor.getString(cursor
                                .getColumnIndexOrThrow(MediaStore.Images.Thumbnails.IMAGE_ID));

                        Cursor thumbCursor = contentResolver.query(EXTERNAL_CONTENT_URI, null,
                                MediaStore.Images.Media._ID + "=" + id, null, null);

                        if (!thumbCursor.moveToFirst()) {
                            contentResolver.delete(
                                    MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                                    MediaStore.Images.Thumbnails.IMAGE_ID + "= " + id, null);
                        }
                        thumbCursor.close();
                    } while (cursor.moveToNext());
                }

                cursor.close();
            }
        }

        public int deleteThumbnail(long mediaID) {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                Log.d(TAG, "contentResolver == null");
                return 0;
            }
            return contentResolver.delete(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI,
                    MediaStore.Images.Thumbnails.IMAGE_ID + "= " + mediaID, null);
        }
    }

    /**
     * 提供删除Android系统媒体库中Audio、Video媒体操作工具类的基类
     */
    private abstract class AudioVedioStoreUtilBase extends StoreUtilBase {

        protected String ARTIST = "artist";

        protected String ALBUM = "album";

        protected AudioVedioStoreUtilBase() {
            super();
        }

        protected void deleteByArtist(final String artist) {
            deleteMediaByInfo(ARTIST, artist);
        }

        protected void deleteByAlbum(final String album) {
            deleteMediaByInfo(ALBUM, album);
        }

    }

    /**
     * 提供删除Android系统媒体库中媒体操作工具类的基类
     */
    private abstract class StoreUtilBase {
        protected Uri EXTERNAL_CONTENT_URI;

        private String mUsbDiskPath;

        StoreUtilBase() {
            mUsbDiskPath = Configuration.getUsbMediaRootPath();
        }

        protected void deleteAll() {
            deleteAllMedia();
            deleteExtern();
        }

        protected void deleteByID(final String mediaID) {
            deleteMediaByInfo(BaseColumns._ID, mediaID);
        }

        protected void deleteAllMedia() {
            String[] mediaColumns = {
                    BaseColumns._ID, MediaColumns.DATA
            };

            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                Log.d(TAG, "contentResolver == null");
                return;
            }

            Cursor cursor = contentResolver.query(EXTERNAL_CONTENT_URI, mediaColumns, null, null,
                    null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        deleteMedia(cursor);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
        }

        protected void deleteMedia(Cursor cursor) {
            String id = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));

            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaColumns.DATA));
            deleteMedia(id, path);
        }

        protected void deleteMedia(String id, String path) {
            if (isInUdisk(path) && !new File(path).exists()) {
                // 确认该文件是udisk中的文件，并且文件已经不存在，才能删除该记录。
                // Clear the file path to prevent the _DELETE_FILE database
                // trigger in the media provider from deleting the file.
                // If the file is truly gone the delete is unnecessary, and we
                // want to avoid
                // accidentally deleting files that are really there.
                ContentResolver contentResolver = getContentResolver();
                if (contentResolver == null) {
                    Log.d(TAG, "contentResolver == null");
                    return;
                }
                ContentValues values = new ContentValues();
                values.put(MediaColumns.DATA, "");
                values.put(MediaColumns.DATE_MODIFIED, 0);
                int rowsUpdated = contentResolver.update(EXTERNAL_CONTENT_URI, values,
                        BaseColumns._ID + " = ?", new String[] {
                            id
                        });
                if (rowsUpdated > 0) {
                    debugLog("Begin deleteMedia record in mediaStore" + id + " " + path);
                    contentResolver.delete(EXTERNAL_CONTENT_URI, BaseColumns._ID + "=" + id, null);
                    debugLog("End deleteMedia record in mediaStore" + id + " " + path);
                }
            }
        }

        protected abstract void deleteExtern();

        protected void deleteMediaByInfo(String infoName, String infoValue) {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                Log.d(TAG, "contentResolver == null");
                return;
            }
            String[] mediaColumns = {
                    BaseColumns._ID, MediaColumns.DATA
            };
            String selection = infoName + " = ?";
            String[] selectionArgs = new String[] {
                infoValue
            };
            Cursor cursor = contentResolver.query(EXTERNAL_CONTENT_URI, mediaColumns, selection,
                    selectionArgs, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        deleteMedia(cursor);

                    } while (cursor.moveToNext());
                }

                cursor.close();
            }
        }

        protected void deleteMediaByDataAndSize(String data, String size) {
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver == null) {
                Log.d(TAG, "contentResolver == null");
                return;
            }
            String[] mediaColumns = {
                    BaseColumns._ID, MediaColumns.DATA
            };

            Cursor cursor = contentResolver.query(EXTERNAL_CONTENT_URI, mediaColumns,
                    MediaColumns.DATA + "=\"" + data + "\"and " + MediaColumns.SIZE + "=" + size,
                    null, null);

            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    do {
                        deleteMedia(cursor);

                    } while (cursor.moveToNext());
                }

                cursor.close();
            }
        }
 
        private boolean isInUdisk(String path) {
            return path.startsWith(mUsbDiskPath);
        }

        public boolean exists(long mediaID, String filePath) {
            boolean exsits = false;
            ContentResolver contentResolver = getContentResolver();
            if (contentResolver != null) {
                String[] mediaColumns = {
                        BaseColumns._ID, MediaColumns.DATA
                };
                String selection = BaseColumns._ID + " = ? AND " + MediaColumns.DATA + " = ?";
                String[] selectionArgs = new String[] {
                        Long.toString(mediaID), filePath
                };
                Cursor cursor = contentResolver.query(EXTERNAL_CONTENT_URI, mediaColumns,
                        selection, selectionArgs, null);

                if (cursor != null) {
                    exsits = cursor.getCount() > 0;
                    cursor.close();
                }

            } else {
                Log.d(TAG, "contentResolver == null");
            }
            return exsits;
        }
    }

    private void debugLog(String string) {
        if (DEBUG) {
            Log.d(TAG, string);
        }

    }
}
