package carnetos.usbservice.db;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;
import carnetos.usbservice.entity.AlbumItem;
import carnetos.usbservice.entity.ArtistItem;
import carnetos.usbservice.entity.MediaItem;
import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.entity.MediaItem.ParseStatus;
import carnetos.usbservice.util.L;
import carnetos.usbservice.util.MediaFile;

/**
 * UsbMediaDB操作类
 */
public class UsbMediaDB {

	private static UsbMediaDB db = null;
	private static boolean DEBUG = false;

	private static void logMethodBegin(String methodName) {

		if (DEBUG) {
			Log.d(TAG, String.format("Begin %s.", methodName));
		}
	}

	private static void logMethodEnd(String methodName) {
		if (DEBUG) {
			Log.d(TAG, String.format("End %s.", methodName));
		}
	}

	/**
	 * 音频表
	 */
	public static final String TABLE_AUDIO = "AUDIO";

	/**
	 * 图片表
	 */
	public static final String TABLE_IMAGE = "IMAGE";

	/**
	 * 视频表
	 */
	public static final String TABLE_VIDEO = "VIDEO";

	/**
	 * 办公文件表
	 */
	public static final String TABLE_OFFICE = "OFFICE";

	/**
	 * 配置信息表
	 */
	public static final String TABLE_CONFIGURATION = "CONFIGURATION";

	/**
	 * AUDIO, IMAGE, VIDEO三张表中的字段名
	 */
	public static class MediaTableCols {

		/* 自增字段 */
		public static final String ID = "_id";
		/* 系统媒体库中媒体ID */
		public static final String MEDIA_ID = "mediaID";
		/* 系统媒体库中专辑ID */
		public static final String ALBUM_ID = "albumID";

		public static final String MEDIA_TYPE = "mediaType";

		public static final String NAME = "name";

		public static final String TITLE = "title";

		public static final String ALBUM = "album";

		public static final String ARTIST = "artist";

		public static final String FILE_PATH = "filePath";

		public static final String SIZE = "size";

		public static final String LAST_MODIFIED = "lastModified";

		public static final String DURATION = "duration";

		public static final String POSITION = "position";

		public static final String IS_PLAY_ITEM = "isPlayItem";

		public static final String PLAYLIST_INDEX = "playlistIndex";

		public static final String PARSE_STATUS = "parseStatus";

		public static final String UPDATE_TIME = "updateTime";

		public static final String SCAN_INDEX = "scanIndex";

		public static final String DELETE_FLAG = "deleteFlag";

		public static final String NAME_ALPHABET = "nameAlphabet";

		public static final String IS_COLLECTED = "isCollected";

	}

	/**
	 * 音乐专辑Cursor中包括的字段名
	 */
	public static class AlbumCols {

		public static final String ID = "_id";

		public static final String ALBUM = MediaTableCols.ALBUM;

		public static final String ITEMS_COUNT = "ITEMS_COUNT";
	}

	/**
	 * 音乐艺术家Cursor中包括的字段名
	 */
	public static class ArtistCols {

		public static final String ARTIST = MediaTableCols.ARTIST;

		public static final String ITEMS_COUNT = "ITEMS_COUNT";
	}

	/**
	 * 配置信息表中的字段名
	 */
	public static class ConfigurationCols {

		public static final String KEY = "key";

		public static final String VALUE = "value";
	}

	/**
	 * 媒体表全部字段数组
	 */
	private static final String MediaTableAllCols[] = { MediaTableCols.ID, MediaTableCols.MEDIA_ID, MediaTableCols.ALBUM_ID,
			MediaTableCols.MEDIA_TYPE, MediaTableCols.NAME, MediaTableCols.TITLE, MediaTableCols.ALBUM, MediaTableCols.ARTIST,
			MediaTableCols.FILE_PATH, MediaTableCols.SIZE, MediaTableCols.LAST_MODIFIED, MediaTableCols.DURATION, MediaTableCols.POSITION,
			MediaTableCols.IS_PLAY_ITEM, MediaTableCols.PLAYLIST_INDEX, MediaTableCols.PARSE_STATUS, MediaTableCols.UPDATE_TIME,
			MediaTableCols.SCAN_INDEX, MediaTableCols.DELETE_FLAG, MediaTableCols.NAME_ALPHABET, MediaTableCols.IS_COLLECTED };

	/**
	 * 媒体表全部字段在MediaTableAllCols中的Index
	 */
	private static class MediaTableAllColsIndex {

		protected static final int ID = 0;

		protected static final int MEDIA_ID = 1;

		protected static final int ALBUM_ID = 2;

		protected static final int MEDIA_TYPE = 3;

		protected static final int NAME = 4;

		protected static final int TITLE = 5;

		protected static final int ALBUM = 6;

		protected static final int ARTIST = 7;

		protected static final int FILE_PATH = 8;

		protected static final int SIZE = 9;

		protected static final int LAST_MODIFIED = 10;

		protected static final int DURATION = 11;

		protected static final int POSITION = 12;

		protected static final int IS_PLAY_ITEM = 13;

		protected static final int PLAYLIST_INDEX = 14;

		protected static final int PARSE_STATUS = 15;

		protected static final int UPDATE_TIME = 16;

		protected static final int SCAN_INDEX = 17;

		protected static final int DELETE_FLAG = 18;

		protected static final int NAME_ALPHABET = 19;

		protected static final int IS_COLLECTED = 20;
	}

	/**
	 * 专辑信息字段
	 */
	private static final String MediaTableAlbumCols[] = { MediaTableCols.ALBUM_ID + " AS _id", MediaTableCols.ALBUM,
			"COUNT(" + MediaTableCols.ALBUM + ") AS ITEMS_COUNT" };

	/**
	 * 专辑信息在MediaTableAlbumCols中的Index
	 */
	private static class MediaTableAlbumColsIndex {

		protected static final int ID = 0;

		protected static final int ALBUM = 1;

		protected static final int ITEMS_COUNT = 2;
	}

	/**
	 * 艺术家信息字段
	 */
	private static final String MediaTableArtistCols[] = { MediaTableCols.ARTIST, "COUNT(" + MediaTableCols.ARTIST + ") AS ITEMS_COUNT" };

	/**
	 * 艺术家信息在MediaTableArtistCols中的Index
	 */
	private static class MediaTableArtistColsIndex {

		protected static final int ARTIST = 0;

		protected static final int ITEMS_COUNT = 1;
	}

	/**
	 * 媒体信息表中全部字段字符串
	 */
	private static final String MEDIA_COLS_SQL = MediaTableCols.ID + ", " + MediaTableCols.MEDIA_ID + ", " + MediaTableCols.ALBUM_ID + ", "
			+ MediaTableCols.MEDIA_TYPE + ", " + MediaTableCols.NAME + ", " + MediaTableCols.TITLE + ", " + MediaTableCols.ALBUM + ", "
			+ MediaTableCols.ARTIST + ", " + MediaTableCols.FILE_PATH + ", " + MediaTableCols.SIZE + ", " + MediaTableCols.LAST_MODIFIED + ", "
			+ MediaTableCols.DURATION + ", " + MediaTableCols.POSITION + ", " + MediaTableCols.IS_PLAY_ITEM + ", " + MediaTableCols.PLAYLIST_INDEX
			+ ", " + MediaTableCols.PARSE_STATUS + ", " + MediaTableCols.UPDATE_TIME + ", " + MediaTableCols.SCAN_INDEX + ", "
			+ MediaTableCols.DELETE_FLAG + ", " + MediaTableCols.NAME_ALPHABET + ", " + MediaTableCols.IS_COLLECTED;

	/**
	 * 替换AUDIO表中数据内容对应的SQL文
	 */
	private static final String REPLACE_AUDIO_SQL = "REPLACE INTO " + TABLE_AUDIO + "(" + MEDIA_COLS_SQL
			+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

	/**
	 * 替换IMAGE表中数据内容对应的SQL文
	 */
	private static final String REPLACE_IMAGE_SQL = "REPLACE INTO " + TABLE_IMAGE + "(" + MEDIA_COLS_SQL
			+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

	/**
	 * 替换VIDEO表中数据内容对应的SQL文
	 */
	private static final String REPLACE_VIDEO_SQL = "REPLACE INTO " + TABLE_VIDEO + "(" + MEDIA_COLS_SQL
			+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

	/**
	 * 替换OFFICE表中数据内容对应的SQL文
	 */
	private static final String REPLACE_OFFICE_SQL = "REPLACE INTO " + TABLE_OFFICE + "(" + MEDIA_COLS_SQL
			+ ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);";

	/**
	 * Log输出模块标识
	 */
	private static final String TAG = "UsbMediaDB";

	/**
	 * UsbMediaDB的SQLiteOpenHelper实现类
	 */
	private static UsbMediaDatabaseHelper mDBHelper;

	/**
	 * UsbMediaDB实例化
	 * 
	 * @param context
	 *            上下文
	 */
	private UsbMediaDB(Context context) {
		mDBHelper = new UsbMediaDatabaseHelper(context);
		try {
			setConfigurationValue("CheckDB", "OK");
		} catch (Exception e) {
			mDBHelper.close();
			mDBHelper = new UsbMediaDatabaseHelper(context);
		}
	}

	/**
	 * 取得数据库引用
	 * 
	 * @author:
	 * @createTime: 2016-11-29 上午10:17:00
	 * @history:
	 * @param context
	 * @return UsbMediaDatabaseHelper
	 */
	public synchronized static UsbMediaDB getUsbMediaDB(Context context) {
		if (db == null)
			db = new UsbMediaDB(context);
		return db;
	}

	/**
	 * 关闭UsbMedia.DB
	 */
	public synchronized void closeDB() {
		logMethodBegin("closeDB");
		mDBHelper.close();
		logMethodEnd("closeDB");
	}

	/**
	 * 为Provider提供搜索功能
	 * 
	 * @author:
	 * @createTime: 2016-11-29 上午11:11:25
	 * @history: void
	 */
	public synchronized Cursor providerQuery(SQLiteQueryBuilder qb, Uri uri, String[] projecttion, String selection, String[] selectionArgs,
			String sortOrder) {
		SQLiteDatabase db = mDBHelper.getReadableDatabase();
		Cursor c = qb.query(db, projecttion, selection, selectionArgs, null, null, sortOrder);
		Log.d("tt", "selection:" + selection);
		if (selectionArgs != null)
			for (int i = 0; i < selectionArgs.length; i++) {
				Log.d("tt", "selectionArgs:" + selectionArgs[i]);
			}
		if (projecttion != null)
			for (int i = 0; i < projecttion.length; i++) {
				Log.d("tt", "projecttion:" + projecttion[i]);
			}

		if (c != null)
			Log.d("tt", "c:" + c.getCount());
		return c;
	}

	/**
	 * 取得存在ＤＢ中的配置信息
	 * 
	 * @param key
	 *            配置信息关键字 (不能为null及空)
	 * @return 配置信息
	 */
	public String getConfigurationValue(String key) {
		logMethodBegin("getConfigurationValue");
		String value = null;

		if (key != null && !key.isEmpty()) {
			SQLiteDatabase db = mDBHelper.getReadableDatabase();

			Cursor cursor = db.rawQuery("SELECT " + ConfigurationCols.VALUE + " FROM " + TABLE_CONFIGURATION + " WHERE " + ConfigurationCols.KEY
					+ " = ?", new String[] { key });

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					value = cursor.getString(0);
				}

				cursor.close();
			}
		}
		logMethodEnd("getConfigurationValue");
		return value;
	}

	/**
	 * 存储配置信息到ＤＢ中
	 * 
	 * @param key
	 *            配置信息关键字 (不能为null及空)
	 * @param value
	 *            配置信息
	 */
	public synchronized void setConfigurationValue(String key, String value) {
		logMethodBegin("setConfigurationValue");
		if (key != null && !key.isEmpty()) {
			SQLiteDatabase db = mDBHelper.getWritableDatabase();

			try {
				beginDBTransaction(db);

				db.execSQL("REPLACE INTO " + TABLE_CONFIGURATION + " (" + ConfigurationCols.KEY + ", " + ConfigurationCols.VALUE + ") VALUES(?, ?);",
						new Object[] { key, value });

				db.setTransactionSuccessful();
			} finally {
				endDBTransaction(db);
			}
		}
		logMethodEnd("setConfigurationValue");
	}

	/**
	 * 更新收藏字段
	 * 
	 * @param isCollected: 是否要收藏
	 * @param filePath: 要收藏文件的路径
	 * @return
	 */
	public synchronized boolean setCollectedValue(boolean isCollected, String filePath) {
		logMethodBegin("setCurrentPlayMediaItem");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		MediaItem mediaItem = getMediaItemByFilePath(filePath);

		Log.i("info3", " --- setPlayMediaItem_mediaItem_filePath --- " + mediaItem.getFilePath());

		int updateCount = 0;
		String table = getTableName(mediaItem.getMediaType());

		ContentValues values = new ContentValues();
		values = new ContentValues();
		values.put(MediaTableCols.IS_COLLECTED, isCollected ? 1 : 0);

		try {
			beginDBTransaction(db);

			updateCount = db.update(table, values, MediaTableCols.FILE_PATH + " = ?", new String[] { mediaItem.getFilePath() });

			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}

		logMethodEnd("setCurrentPlayMediaItem");
		return updateCount > 0;
	}

	/**
	 * 修改文件名
	 * 
	 * @param isCollected: 是否要收藏
	 * @param filePath: 要收藏文件的路径
	 * @return
	 */
	public synchronized boolean setFileName(String filePath) {
		logMethodBegin("setCurrentPlayMediaItem");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		MediaItem mediaItem = getMediaItemByFilePath(filePath);

		Log.i("info3", " --- setPlayMediaItem_mediaItem_filePath --- " + mediaItem.getFilePath());

		int updateCount = 0;
		String table = getTableName(mediaItem.getMediaType());

		ContentValues values = new ContentValues();
		values = new ContentValues();
		// values.put(MediaTableCols.IS_COLLECTED, isCollected ? 1 : 0);

		try {
			beginDBTransaction(db);

			updateCount = db.update(table, values, MediaTableCols.FILE_PATH + " = ?", new String[] { mediaItem.getFilePath() });

			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}

		logMethodEnd("setCurrentPlayMediaItem");
		return updateCount > 0;
	}

	/**
	 * 将一个媒体对象更新到DB中, 所有字段信息都会更新到DB中
	 * 
	 * @param mediaItem
	 *            媒体对象
	 */
	public synchronized void replaceMediaItem(MediaItem mediaItem) {
		logMethodBegin("replaceMediaItem");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();

		try {
			beginDBTransaction(db);
			if (mediaItem.getMediaType() != MediaType.UNKNOWN) {
				db.execSQL(getReplaceSql(mediaItem.getMediaType()), mediaItemToArray(mediaItem));
			}
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("replaceMediaItem");
	}

	/**
	 * 将多个媒体对象更新到DB中,所有字段信息都会更新到DB中
	 * 
	 * @param mediaItems
	 */
	public synchronized void replaceMediaItems(CopyOnWriteArrayList<MediaItem> mediaItems) {
		logMethodBegin("replaceMediaItems");
		Log.d("USBDB", "replaceMediaItems:" + mediaItems.size());
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		try {
			beginDBTransaction(db);

			for (MediaItem mediaItem : mediaItems) {
				if (mediaItem.getMediaType() != MediaType.UNKNOWN) {
					db.execSQL(getReplaceSql(mediaItem.getMediaType()), mediaItemToArray(mediaItem));
				}

				// if (mediaItem.getMediaType() != MediaType.UNKNOWN) {
				// MediaItem queryMediaItem =
				// queryMediaItem(mediaItem.getFilePath());
				// if (queryMediaItem == null) {
				// db.execSQL(getReplaceSql(mediaItem.getMediaType()),
				// mediaItemToArray(mediaItem));
				// }
				// }
			}
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("replaceMediaItems");
	}

	/**
	 * 从DB中删除指定媒体
	 * 
	 * @param mediaItem
	 *            媒体对象
	 */
	public synchronized void deleteMediaItem(MediaItem item) {
		logMethodBegin("deleteMediaItem");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		try {
			beginDBTransaction(db);
			db.delete(getTableName(item.getMediaType()), MediaTableCols.FILE_PATH + " = ?", new String[] { item.getFilePath() });
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("deleteMediaItem");
	}

	/**
	 * 从DB中删除指定媒体
	 * 
	 * @param mediaItems
	 *            媒体对象列表
	 */
	public synchronized void deleteMediaItems(List<MediaItem> mediaItems) {
		logMethodBegin("deleteMediaItems");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();

		try {
			beginDBTransaction(db);

			for (MediaItem mediaItem : mediaItems) {

				if (mediaItem.getID() != null) {
					db.delete(getTableName(mediaItem.getMediaType()), MediaTableCols.ID + " = ?", new String[] { Long.toString(mediaItem.getID()) });
				} else {
					db.delete(getTableName(mediaItem.getMediaType()), MediaTableCols.FILE_PATH + " = ?", new String[] { mediaItem.getFilePath() });
				}
			}
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("deleteMediaItems");
	}

	/**
	 * 删除指定媒体表中的伪删除数据
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @return 删除数据条数
	 */
	public synchronized int deletePseudoMediaItems(MediaType mediaType) {
		logMethodBegin("deletePseudoMediaItems");
		int count = 0;
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		try {
			// beginDBTransaction(db);
			count = db.delete(getTableName(mediaType), MediaTableCols.DELETE_FLAG + " = 1", null);
			// db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// endDBTransaction(db);
		}
		logMethodEnd("deletePseudoMediaItems");
		return count;
	}

	/**
	 * 将一个媒体对象更新到DB中,会更新所有字段,mediaItem.getID()属性不能为null及空
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return 更新ＤＢ影响记录条数
	 */
	public synchronized int updateMediaItem(MediaItem mediaItem) {
		logMethodBegin("updateMediaItem");
		int updateCount = 0;
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		try {
			beginDBTransaction(db);
			ContentValues values = mediaItemToContentValues(mediaItem);
			updateCount = db.update(getTableName(mediaItem.getMediaType()), values, MediaTableCols.ID + " = ?",
					new String[] { Long.toString(mediaItem.getID()) });
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updateMediaItem");
		return updateCount;
	}

	/**
	 * 将一个媒体对象的Metadata更新到DB中, mediaItem.getID()属性不能为null及空
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return 更新ＤＢ影响记录条数
	 */
	public synchronized int updateMediaItemMetadata(MediaItem mediaItem) {
		logMethodBegin("updateMediaItemMetadata");
		int updateCount = 0;
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		try {
			beginDBTransaction(db);
			ContentValues values = metadataToContentValues(mediaItem);
			updateCount = db.update(getTableName(mediaItem.getMediaType()), values, MediaTableCols.ID + " = ?",
					new String[] { Long.toString(mediaItem.getID()) });

			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updateMediaItemMetadata");
		return updateCount;
	}

	/**
	 * 将多个媒体对象更新到DB中,所有媒体对象的getID()属性不能为null及空
	 * 
	 * @param mediaItems
	 *            媒体对象表表
	 * @return 更新ＤＢ影响记录条数
	 */
	public synchronized int updateMediaItems(List<MediaItem> mediaItems) {
		logMethodBegin("updateMediaItems");
		int updateCount = 0;
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		try {
			beginDBTransaction(db);
			for (MediaItem mediaItem : mediaItems) {
				ContentValues values = mediaItemToContentValues(mediaItem);
				int count = db.update(getTableName(mediaItem.getMediaType()), values, MediaTableCols.ID + " = ?",
						new String[] { Long.toString(mediaItem.getID()) });
				updateCount = updateCount + count;
			}
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updateMediaItems");
		return updateCount;
	}

	/**
	 * 将多个媒体对象Metadata更新到DB中，媒体对象getID()属性不能为null及空
	 * 
	 * @param mediaItems
	 *            媒体对象列表
	 * @return 更新ＤＢ影响记录条数
	 */
	public synchronized int updateMediaItemsMetadata(List<MediaItem> mediaItems) {
		logMethodBegin("updateMediaItemsMetadata");
		int updateCount = 0;
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		try {
			beginDBTransaction(db);
			for (MediaItem mediaItem : mediaItems) {
				ContentValues values = metadataToContentValues(mediaItem);
				int count = db.update(getTableName(mediaItem.getMediaType()), values, MediaTableCols.ID + " = ?",
						new String[] { Long.toString(mediaItem.getID()) });
				updateCount = updateCount + count;
			}
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updateMediaItemsMetadata");
		return updateCount;
	}

	/**
	 * 更新当前播放位置, mediaItem.getID()属性不能为null及空
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return 1:成功,否则失败
	 */
	public synchronized int updateMediaItemPosition(MediaItem mediaItem) {
		logMethodBegin("updateMediaItemPosition");
		int updateCount = 0;
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MediaTableCols.POSITION, mediaItem.getPosition());
		try {
			beginDBTransaction(db);
			updateCount = db.update(getTableName(mediaItem.getMediaType()), values, MediaTableCols.ID + " = ?",
					new String[] { Long.toString(mediaItem.getID()) });
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updateMediaItemPosition");
		return updateCount;
	}

	/**
	 * 更新DB中所有媒体的伪删除字段对应的值
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @param deleteFlag
	 *            true:伪删除 false:媒体有效（UDisk中包含该媒体）
	 */
	public synchronized void updateAllMediaDeleteFlag(MediaType mediaType, boolean deleteFlag) {
		logMethodBegin("updateAllMediaDeleteFlag");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MediaTableCols.DELETE_FLAG, deleteFlag ? 1 : 0);
		try {
			beginDBTransaction(db);
			db.update(getTableName(mediaType), values, MediaTableCols.DELETE_FLAG + " = ?", new String[] { deleteFlag ? "0" : "1" });
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updateAllMediaDeleteFlag");
	}

	/**
	 * 更新数据库U盘里的媒体的伪删除字段对应的值
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @param deleteFlag
	 *            true:伪删除 false:媒体有效（UDisk中包含该媒体）
	 */
	public synchronized void updateUsbMediaDeleteFlag(String rootPath, MediaType mediaType, boolean deleteFlag) {
		logMethodBegin("updateUsbMediaDeleteFlag");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MediaTableCols.DELETE_FLAG, deleteFlag ? 1 : 0);
		try {
			beginDBTransaction(db);
			db.update(getTableName(mediaType), values, MediaTableCols.DELETE_FLAG + " = ? AND " + MediaTableCols.FILE_PATH + " LIKE ?", new String[] {
					deleteFlag ? "0" : "1", "%" + rootPath + "%" });
			db.setTransactionSuccessful();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updateUsbMediaDeleteFlag");
	}

	/**
	 * 更新DB中指定媒体的伪删除字段值
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @param deleteFlag
	 *            true:伪删除 false:媒体有效（Udisk中包含该媒体）
	 */
	public synchronized void updateMediaDeleteFlag(MediaItem mediaItem, boolean deleteFlag) {
		logMethodBegin("updateMediaDeleteFlag");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MediaTableCols.DELETE_FLAG, deleteFlag ? 1 : 0);
		try {
			beginDBTransaction(db);
			if (mediaItem.getID() == null) {
				db.update(getTableName(mediaItem.getMediaType()), values, MediaTableCols.FILE_PATH + " = ?", new String[] { mediaItem.getFilePath() });
			} else {
				db.update(getTableName(mediaItem.getMediaType()), values, MediaTableCols.ID + " = ?",
						new String[] { Long.toString(mediaItem.getID()) });
			}
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updateMediaDeleteFlag");
	}

	/**
	 * 更新DB中指定媒体的伪删除字段值
	 * 
	 * @param filePath
	 *            文件路径
	 * @param deleteFlag
	 *            true:伪删除 false:媒体有效（UDisk中包含该媒体）
	 */
	public synchronized void updateMediaDeleteFlag(String filePath, boolean deleteFlag) {
		logMethodBegin("updateMediaDeleteFlag:" + filePath);
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MediaTableCols.DELETE_FLAG, deleteFlag ? 1 : 0);
		try {
			beginDBTransaction(db);
			db.update(getTableName(MediaFile.getFileType(filePath)), values, MediaTableCols.FILE_PATH + " = ?", new String[] { filePath });
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodBegin("updateMediaDeleteFlag:" + filePath);
	}

	/**
	 * 更新媒体Metadata扫描状态
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @param id
	 *            媒体ID
	 * @param status
	 *            Metadata信息解析状态
	 * @return -1:更新失败: >=0成功更新的行数
	 */
	public synchronized int updateParseStatus(MediaType mediaType, long id, ParseStatus status) {
		logMethodBegin("updateParseStatus");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();
		ContentValues values = new ContentValues();
		values.put(MediaTableCols.PARSE_STATUS, status.value());
		String whereClause = MediaTableCols.ID + " = ?";
		String[] whereArgs = new String[] { Long.toString(id) };

		int ret = -1;
		try {
			beginDBTransaction(db);
			ret = db.update(getTableName(mediaType), values, whereClause, whereArgs);
			db.setTransactionSuccessful();
		} finally {
			try {
				endDBTransaction(db);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e(TAG, "ret = " + ret);
			}
		}
		logMethodEnd("updateParseStatus");
		return ret;
	}

	/**
	 * 清空指定DB中指定类型的所有媒体数据
	 * 
	 * @param mediaType
	 *            媒体类型
	 */
	public synchronized void clearTable(MediaType mediaType) {
		logMethodBegin("clearTable");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();

		try {
			beginDBTransaction(db);

			if (mediaType != MediaType.UNKNOWN) {
				db.delete(getTableName(mediaType), null, null);
			}

			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("clearTable");
	}

	/**
	 * 清空音乐播放列表
	 */
	public synchronized void clearPlayList() {
		logMethodBegin("clearPlayList");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(MediaTableCols.PLAYLIST_INDEX, 0);

		try {
			beginDBTransaction(db);

			db.update(TABLE_AUDIO, values, MediaTableCols.PLAYLIST_INDEX + " > 0", null);

			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("clearPlayList");
	}

	/**
	 * 取得指定媒体对象
	 * 
	 * @param path
	 *            文件全路径
	 * @return MediaItem 媒体对象, 为null表示DB中不存在
	 */
	public MediaItem queryMediaItem(String path) {
		logMethodBegin("queryMediaItem");
		MediaItem mediaItem = null;
		MediaType mediaType = MediaFile.getFileType(path);

		Cursor cursor = null;

		try {
			SQLiteDatabase db = mDBHelper.getReadableDatabase();

			cursor = db.query(getTableName(mediaType), MediaTableAllCols, MediaTableCols.FILE_PATH + " = ?", new String[] { path }, null, null, null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					mediaItem = cursorToMediaItem(cursor);
				}

				cursor.close();
			}
		} catch (Exception ex) {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}

		logMethodEnd("queryMediaItem");
		return mediaItem;
	}

	/**
	 * 取得DB中指定媒体类型的所有数据，包括伪删除媒体
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @return Cursor
	 */
	public Cursor queryAllMedia(MediaType mediaType) {
		logMethodBegin("queryAllMedia:" + mediaType);
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		Cursor c = db.query(getTableName(mediaType), MediaTableAllCols, null, null, null, null, MediaTableCols.SCAN_INDEX);
		logMethodEnd("queryAllMedia:" + mediaType);
		return c;
	}

	/**
	 * @Description: 是否被加在favorite列表中
	 * @param mediaType
	 * @return
	 */
	public Cursor queryIfIsFavorite(MediaType mediaType, String path) {
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		StringBuffer selectSQL = new StringBuffer();

		selectSQL.append("SELECT *");
		selectSQL.append(" FROM ");
		selectSQL.append(getTableName(mediaType));
		selectSQL.append(" WHERE ");
		selectSQL.append(MediaTableCols.FILE_PATH);
		selectSQL.append(" = ?");
		Cursor c = db.rawQuery(selectSQL.toString(), new String[] { path });
		return c;
	}

	/**
	 * 取得DB中指定媒体类型的数据
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @param deleteFlag
	 *            true:包括伪删除媒体 false:不包括伪删除媒体
	 * @return Cursor
	 */
	public Cursor queryAllMedia(MediaType mediaType, boolean deleteFlag) {
		logMethodBegin("queryAllMedia:" + mediaType + " " + deleteFlag);
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		Cursor c = db.query(getTableName(mediaType), MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = ?", new String[] { deleteFlag ? "1" : "0" },
				null, null, MediaTableCols.SCAN_INDEX);
		logMethodEnd("queryAllMedia:" + mediaType + " " + deleteFlag);
		return c;
	}

	/**
	 * 取得DB中指定媒体类型的数据，用于分页处理
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得个数, 如果小于0则返回全部
	 * @param deleteFlag
	 *            true:包括伪删除媒体 false:不包括伪删除媒体
	 * @return Cursor
	 */
	public Cursor queryAllMedia(String rootPath, MediaType mediaType, int startIndex, int count, boolean deleteFlag) {
		logMethodBegin("queryAllMedia:" + "start index:" + startIndex);
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		// 使用rawQuery对应[Reached MAX size for compiled-sql statement cache]
		StringBuffer selectSQL = new StringBuffer();

		selectSQL.append("SELECT ");
		selectSQL.append(MEDIA_COLS_SQL);
		selectSQL.append(" FROM ");
		selectSQL.append(getTableName(mediaType));
		selectSQL.append(" WHERE ");
		selectSQL.append(MediaTableCols.DELETE_FLAG);
		selectSQL.append(" = ?  AND ");
		selectSQL.append(MediaTableCols.FILE_PATH);
		selectSQL.append(" LIKE ? ");
		selectSQL.append(" ORDER BY ");
		selectSQL.append(MediaTableCols.SCAN_INDEX);
		selectSQL.append(" LIMIT ?, ?");
		Cursor c = db.rawQuery(selectSQL.toString(),
				new String[] { deleteFlag ? "1" : "0", new String(" %" + rootPath + "% "), Integer.toString(startIndex), Integer.toString(count), });
		logMethodEnd("queryAllMedia:" + "start index:" + startIndex);
		return c;
	}

	/**
	 * 取得音乐媒体的专辑列表
	 * 
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得个数, 如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor queryAlbums(int startIndex, int count) {
		logMethodBegin("queryAlbums");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableAlbumCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND "
				+ MediaTableCols.ALBUM_ID + " > -1", null, AlbumCols.ID + ", " + AlbumCols.ALBUM, null, MediaTableCols.SCAN_INDEX, limit);
		logMethodEnd("queryAlbums");
		return c;
	}

	/**
	 * 取得指定音乐专辑下的所有媒体，不包括伪删除媒体
	 * 
	 * @param album
	 *            专辑名
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得个数, 如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor queryAlbumMediaItems(long albumID, int startIndex, int count) {
		logMethodBegin("queryAlbumMediaItems");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND "
				+ MediaTableCols.ALBUM_ID + " = ?", new String[] { Long.toString(albumID) }, null, null, MediaTableCols.SCAN_INDEX, limit);
		logMethodEnd("queryAlbumMediaItems");
		return c;
	}

	/**
	 * 取得音乐媒体的艺术家列表
	 * 
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得个数, 如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor queryArtists(int startIndex, int count) {
		logMethodBegin("queryArtists");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableArtistCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND "
				+ MediaTableCols.ARTIST + " IS NOT NULL", null, ArtistCols.ARTIST, null, MediaTableCols.SCAN_INDEX, limit);
		logMethodEnd("queryArtists");
		return c;
	}

	/**
	 * 取得指定音乐艺术家下的所有媒体，不包括伪删除媒体
	 * 
	 * @param artist
	 *            艺术家名
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得个数, 如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor queryArtistMediaItems(String artist, int startIndex, int count) {
		logMethodBegin("queryArtistMediaItems");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND "
				+ MediaTableCols.ARTIST + " = ?", new String[] { artist }, null, null, MediaTableCols.SCAN_INDEX, limit);
		logMethodEnd("queryArtistMediaItems");
		return c;
	}

	/**
	 * 取得音乐播放列表
	 * 
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得个数, 如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor queryPlaylist(int startIndex, int count) {
		logMethodBegin("queryPlaylist");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PLAYLIST_INDEX + " > 0", null,
				null, null, MediaTableCols.PLAYLIST_INDEX, limit);
		logMethodBegin("queryPlaylist");
		return c;
	}

	/**
	 * 将一个媒体对象追加到播放列表中，本函数会维护MediaTableCols.PLAYLIST_INDEX字段， MediaTableCols.PLAYLIST_INDEX字段同时也用于播放列表中的数据排序。
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return 改变PlaylistIndex属性后的MediaItem对象
	 */
	public synchronized int addPlayList(MediaItem mediaItem) {
		logMethodBegin("queryPlaylist");
		long playlistIndex = getMaxPlaylistIndex();

		playlistIndex = playlistIndex + 1;

		int temp = updatePlayListItem(mediaItem, playlistIndex);
		logMethodEnd("addPlayList");
		return temp;
	}

	/**
	 * 将指定音乐专辑加入到播放列表
	 * 
	 * @param albumID
	 *            专辑ID
	 * @return 添加个数
	 */
	public synchronized int addPlayListForAlbum(long albumID) {
		logMethodBegin("addPlayListForAlbum");
		int count = 0;
		int startIndex = 0;
		Cursor cursorAlbums = null;
		List<MediaItem> medisItems = null;

		long playlistIndex = getMaxPlaylistIndex();

		while (true) {
			// 取得指定音乐专辑下的所有媒体，不包括伪删除媒体
			cursorAlbums = queryAlbumMediaItems(albumID, startIndex, 30);
			if (cursorAlbums != null) {
				medisItems = cursorToMediaItems(cursorAlbums);
				cursorAlbums.close();
			}

			if (medisItems == null || medisItems.size() == 0) {
				break;
			}

			startIndex = startIndex + 30;

			for (MediaItem mediaItem : medisItems) {
				playlistIndex = playlistIndex + 1;

				updatePlayListItem(mediaItem, playlistIndex);

				count = count + 1;
			}

			medisItems.clear();
		}
		logMethodEnd("addPlayListForAlbum");
		return count;
	}

	/**
	 * 将指定艺术家对应的音乐媒体加入到播放列表
	 * 
	 * @param artist
	 *            艺术家名
	 * @return 添加个数
	 */
	public synchronized int addPlayListForArtist(String artist) {
		logMethodBegin("addPlayListForArtist");
		int count = 0;
		int startIndex = 0;
		Cursor cursorArtist = null;
		List<MediaItem> medisItems = null;

		long playlistIndex = getMaxPlaylistIndex();

		while (true) {
			// 取得指定音乐专辑下的所有媒体，不包括伪删除媒体
			cursorArtist = queryArtistMediaItems(artist, startIndex, 30);
			if (cursorArtist != null) {
				medisItems = cursorToMediaItems(cursorArtist);
				cursorArtist.close();
			}

			if (medisItems == null || medisItems.size() == 0) {
				break;
			}

			startIndex = startIndex + 30;

			for (MediaItem mediaItem : medisItems) {
				playlistIndex = playlistIndex + 1;

				updatePlayListItem(mediaItem, playlistIndex);

				count = count + 1;
			}

			medisItems.clear();
		}
		logMethodEnd("addPlayListForArtist");
		return count;
	}

	/**
	 * 将一个媒体对象从播放列表中移除
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return 改变PlaylistIndex属性后的MediaItem对象
	 */
	public synchronized int removePlayList(MediaItem mediaItem) {
		logMethodBegin("removePlayList");
		int id = updatePlayListItem(mediaItem, 0);
		logMethodEnd("removePlayList");
		return id;
	}

	/**
	 * 按Title检索指定媒体类型
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @param key
	 *            关键字
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得条数，如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor fuzzyTitleSearch(MediaType mediaType, String key, int startIndex, int count) {
		logMethodBegin("fuzzyTitleSearch");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(getTableName(mediaType), MediaTableAllCols, MediaTableCols.NAME + " LIKE ? ", new String[] { "%" + key + "%" }, null,
				null, MediaTableCols.SCAN_INDEX, limit);

		logMethodEnd("fuzzyTitleSearch");
		return c;
	}

	/**
	 * 按Album检索指定媒体类型
	 * 
	 * @param key
	 *            关键字
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得条数，如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor fuzzyAlbumSearch(String key, int startIndex, int count) {
		logMethodBegin("fuzzyAlbumSearch");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableAlbumCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND "
				+ AlbumCols.ALBUM + " LIKE ?", new String[] { "%" + key + "%" }, AlbumCols.ID + ", " + AlbumCols.ALBUM, null,
				MediaTableCols.SCAN_INDEX, limit);
		logMethodEnd("fuzzyAlbumSearch");
		return c;
	}

	/**
	 * 在指定专辑下按歌名检索
	 * 
	 * @param albumID
	 *            专辑ID
	 * @param key
	 *            关键字(按title检索)
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得条数，如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor fuzzyAlbumChildSearch(long albumID, String key, int startIndex, int count) {
		logMethodBegin("fuzzyAlbumChildSearch");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND "
				+ MediaTableCols.ALBUM_ID + " = ? AND " + MediaTableCols.NAME + " LIKE ?", new String[] { Long.toString(albumID), "%" + key + "%" },
				null, null, MediaTableCols.SCAN_INDEX, limit);
		logMethodEnd("fuzzyAlbumChildSearch");
		return c;
	}

	/**
	 * 按Artist检索指定媒体类型
	 * 
	 * @param key
	 *            关键字
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得条数，如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor fuzzyArtistSearch(String key, int startIndex, int count) {
		logMethodBegin("fuzzyArtistSearch");
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableArtistCols, MediaTableCols.DELETE_FLAG + "  = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND "
				+ MediaTableCols.ARTIST + " LIKE ?", new String[] { "%" + key + "%" }, ArtistCols.ARTIST, null, MediaTableCols.SCAN_INDEX, limit);
		logMethodEnd("fuzzyArtistSearch");
		return c;
	}

	/**
	 * 在指定艺术家下按歌名检索
	 * 
	 * @param artist
	 *            艺术家
	 * @param key
	 *            关键字(按title检索)
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得条数，如果小于0则返回全部
	 * @return Cursor
	 */
	public Cursor fuzzyArtistChildSearch(String artist, String key, int startIndex, int count) {
		logMethodBegin("fuzzyArtistChildSearch:" + artist);
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND "
				+ MediaTableCols.ARTIST + " = ? AND " + MediaTableCols.NAME + " LIKE ?", new String[] { artist, "%" + key + "%" }, null, null,
				MediaTableCols.SCAN_INDEX, limit);
		logMethodEnd("fuzzyArtistChildSearch:" + artist);
		return c;
	}

	/**
	 * 按Title检索音频播放列表
	 * 
	 * @param key
	 *            关键字
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得条数，如果小于0则返回全部
	 * @return
	 */
	public Cursor fuzzyPlaylistSearch(String key, int startIndex, int count) {
		logMethodBegin("fuzzyPlaylistSearch:" + key);
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(startIndex, count);

		Cursor c = db.query(TABLE_AUDIO, MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PLAYLIST_INDEX + " > 0 AND "
				+ MediaTableCols.NAME + " LIKE ?", new String[] { "%" + key + "%" }, null, null, MediaTableCols.PLAYLIST_INDEX, limit);
		logMethodEnd("fuzzyPlaylistSearch:" + key);
		return c;
	}

	/**
	 * 取得指定媒体类型DB中下一个未解析Metadata信息的媒体文件
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @return MediaItem或null
	 */
	public MediaItem getNextNonParsedItem(MediaType mediaType, List<String> path) {
		logMethodBegin("getNextNonParsedItem:" + mediaType);
		MediaItem mediaItem = null;
		String where = "";
		if (path != null && path.size() > 0) {
			where = where + MediaTableCols.FILE_PATH + "  LIKE ";
			for (int i = 0; i < path.size(); i++) {
				where += " %" + path.get(i) + "% ";
				if (i != path.size() - 1) {
					where += " OR" + MediaTableCols.FILE_PATH + "  LIKE ";
				}

			}
		}
		Log.d("AAAA", "where:" + where);
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = "0, 1";

		Cursor cursor = db.query(getTableName(mediaType), MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS
				+ " = 0 ", null, null, null, MediaTableCols.SCAN_INDEX, limit);
		// L.d("zhang","getNextNonParsedItem cursor.getCount"+cursor.getCount());
		if (cursor != null) {
			if (cursor.moveToFirst()) {
				mediaItem = cursorToMediaItem(cursor);
			}
			cursor.close();
		}
		logMethodEnd("getNextNonParsedItem" + mediaType);
		return mediaItem;
	}

	/**
	 * 取得指定媒体类型DB中下一组未解析Metadata信息的媒体文件
	 * 
	 * @param mediaType
	 *            mediaType 媒体类型
	 * @param count
	 *            取得条数
	 * @return List<MediaItem>或null
	 */
	public List<MediaItem> getNextNonParsedItems(MediaType mediaType, int count) {
		logMethodBegin("getNextNonParsedItems:" + mediaType);
		List<MediaItem> mediaItems = null;
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String limit = getLimitSQL(0, count);

		Cursor cursor = db.query(getTableName(mediaType), MediaTableAllCols, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS
				+ " = 0", null, null, null, MediaTableCols.SCAN_INDEX, limit);

		if (cursor != null) {
			mediaItems = cursorToMediaItems(cursor);

			cursor.close();
		}
		logMethodEnd("getNextNonParsedItems:" + mediaType);
		return mediaItems;
	}

	/**
	 * 取得指定媒体类型在DB中的媒体数，不包括伪删除数据
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @return 总数
	 */
	public int getAllMediaCount(MediaType mediaType) {
		logMethodBegin("getAllMediaCount:" + mediaType);
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		Cursor cursor = db.query(getTableName(mediaType), new String[] { "COUNT(*)" }, MediaTableCols.DELETE_FLAG + " = 0", null, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}
		logMethodEnd("getAllMediaCount:" + mediaType);
		return count;
	}

	/**
	 * 取得音乐专辑在DB中的媒体数，不包括伪删除数据
	 * 
	 * @return 总数
	 */
	public int getAlbumCount() {
		logMethodBegin("getAlbumCount");
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String albumCountSQL = "SELECT COUNT(*) FROM (SELECT " + MediaTableCols.ALBUM + ", " + MediaTableCols.ALBUM_ID + " FROM " + TABLE_AUDIO
				+ " WHERE " + MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.ALBUM_ID + " > -1 AND " + MediaTableCols.PARSE_STATUS
				+ " = 5 GROUP BY " + MediaTableCols.ALBUM + ", " + MediaTableCols.ALBUM_ID + ");";

		Cursor cursor = db.rawQuery(albumCountSQL, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}
		logMethodEnd("getAlbumCount");
		Log.i("xxxxx", "=====UsbMediaDB=album==conut====" + count);
		return count;
	}

	/**
	 * 取得指定音频专辑下的媒体个数
	 * 
	 * @param albumID
	 *            专辑ID
	 * @return 总数
	 */
	public int getAlbumMediaItemsCount(long albumID) {
		logMethodBegin("getAlbumMediaItemsCount:" + albumID);
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		Cursor cursor = db.query(TABLE_AUDIO, new String[] { "COUNT(*)" }, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS
				+ " = 5 AND " + MediaTableCols.ALBUM_ID + " = ?", new String[] { Long.toString(albumID) }, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}
		logMethodEnd("getAlbumMediaItemsCount:" + albumID);
		return count;
	}

	/**
	 * 取得音乐艺术家在DB中的媒体数，不包括伪删除数据
	 * 
	 * @return 总数
	 */
	public int getArtistCount() {
		logMethodBegin("getArtistCount");
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String artistCountSQL = "SELECT COUNT(*) FROM (SELECT " + MediaTableCols.ARTIST + " FROM " + TABLE_AUDIO + " WHERE "
				+ MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND " + MediaTableCols.ARTIST
				+ " IS NOT NULL GROUP BY " + MediaTableCols.ARTIST + ");";

		Cursor cursor = db.rawQuery(artistCountSQL, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}
		logMethodEnd("getArtistCount");
		Log.i("xxxxx", "=====UsbMediaDB=artist==conut====" + count);
		return count;
	}

	/**
	 * 取得指定音频艺术家下的媒体个数
	 * 
	 * @param artist
	 *            艺术家
	 * @return 总数
	 */
	public int getArtistMediaItemsCount(String artist) {
		logMethodBegin("getArtistMediaItemsCount:" + artist);
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		Cursor cursor = db.query(TABLE_AUDIO, new String[] { "COUNT(*)" }, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS
				+ " = 5 AND " + MediaTableCols.ARTIST + " = ?", new String[] { artist }, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}
		logMethodEnd("getArtistMediaItemsCount:" + artist);
		return count;
	}

	/**
	 * 取得音频播放列表下的媒体个数
	 * 
	 * @return 总数
	 */
	public int getPlaylistMediaItemsCount() {
		logMethodBegin("getPlaylistMediaItemsCount");
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		Cursor cursor = db.query(TABLE_AUDIO, new String[] { "COUNT(*)" }, MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PLAYLIST_INDEX
				+ " > 0", null, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}
		logMethodEnd("getPlaylistMediaItemsCount");
		return count;
	}

	/**
	 * 取得按Title检索符合媒体对象的总数
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @param key
	 *            关键字
	 * @return 总数
	 */
	public int getFuzzyTitleSearchCount(MediaType mediaType, String key) {
		logMethodBegin("getFuzzyTitleSearchCount");
		int count = 0;

		// 解决搜索不完全 add by he 160406

		if (key.equals("%")) {
			return 0;
		}

		SQLiteDatabase db = mDBHelper.getReadableDatabase();
		String searchSQL = "SELECT COUNT(*) FROM (SELECT " + MediaTableCols.ID + " FROM " + getTableName(mediaType) + " WHERE "
				+ MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.NAME + " LIKE ?);" + "ESCAPE '\'";
		Cursor cursor = db.rawQuery(searchSQL, new String[] { "%" + key + "%" });

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}
		Log.w(TAG, "匹配 " + key + " 的歌曲数量:" + count);
		logMethodEnd("getFuzzyTitleSearchCount");
		return count;
	}

	/**
	 * 取得按专辑名检索符合专辑的总数
	 * 
	 * @param key
	 *            关键字
	 * @return 总数
	 */
	public int getFuzzyAlbumSearchCount(String key) {
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String searchSQL = "SELECT COUNT(*) FROM (SELECT " + MediaTableCols.ALBUM_ID + ", " + AlbumCols.ALBUM + " FROM " + TABLE_AUDIO + " WHERE "
				+ MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND " + AlbumCols.ALBUM + " LIKE ? GROUP BY "
				+ MediaTableCols.ALBUM_ID + ", " + AlbumCols.ALBUM + ");";

		Cursor cursor = db.rawQuery(searchSQL, new String[] { "%" + key + "%" });

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}

		return count;
	}

	/**
	 * 在指定专辑下按歌名检索符合条件的媒体总数
	 * 
	 * @param albumID
	 *            专辑ID
	 * @param key
	 *            关键字
	 * @return 总数
	 */
	public int getFuzzyAlbumChildSearchCount(long albumID, String key) {
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String searchSQL = "SELECT COUNT(*) FROM (SELECT " + MediaTableCols.ALBUM_ID + " FROM " + TABLE_AUDIO + " WHERE "
				+ MediaTableCols.DELETE_FLAG + " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND " + MediaTableCols.ALBUM_ID + " = ? AND "
				+ MediaTableCols.TITLE + " LIKE ? );";

		Cursor cursor = db.rawQuery(searchSQL, new String[] { Long.toString(albumID), "%" + key + "%" });

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}
			cursor.close();
		}
		return count;
	}

	/**
	 * 取得数据库中数量最多的路径
	 */
	public String[] getMaxSearchPaths() {
		String[] pahts = new String[] { null, null, null };
		pahts[0] = getMaxCountPath(MediaType.AUDIO);
		pahts[1] = getMaxCountPath(MediaType.VIDEO);
		pahts[2] = getMaxCountPath(MediaType.IMAGE);
		return pahts;
	}

	/**
	 * 取得单个媒体类型最多的路径统计
	 * 
	 * @author:
	 * @createTime: 2016-9-30 上午11:21:45
	 * @history:
	 * @param mediaType
	 * @return String
	 */
	private String getMaxCountPath(MediaType mediaType) {
		String path = null;
		try {
			SQLiteDatabase db = mDBHelper.getReadableDatabase();
			String searchSQL = "select rtrim(filePath,name) as path, count(*) as sum from " + mediaType.name() + "  where " + " deleteFlag = 0 "
					+ " group by path order by sum DESC";
			Cursor cursor = db.rawQuery(searchSQL, null);
			if (cursor != null) {
				cursor.moveToNext();
				path = cursor.getString(0);
				cursor.close();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return path;
	}

	/**
	 * 取得按艺术家检索符合艺术家的总数
	 * 
	 * @param key
	 *            关键字
	 * @return 总数
	 */
	public int getFuzzyArtistSearchCount(String key) {
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String searchSQL = "SELECT COUNT(*) FROM (SELECT " + ArtistCols.ARTIST + " FROM " + TABLE_AUDIO + " WHERE " + MediaTableCols.DELETE_FLAG
				+ " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND " + MediaTableCols.ARTIST + " LIKE ? GROUP BY " + ArtistCols.ARTIST + ");";

		Cursor cursor = db.rawQuery(searchSQL, new String[] { "%" + key + "%" });

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}

		return count;
	}

	/**
	 * 在指定艺术家下按歌名检索符合条件的媒体总数
	 * 
	 * @param artist
	 *            艺术家
	 * @param key
	 *            关键字
	 * @return 总数
	 */
	public int getFuzzyArtistChildSearchCount(String artist, String key) {
		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String searchSQL = "SELECT COUNT(*) FROM (SELECT " + ArtistCols.ARTIST + " FROM " + TABLE_AUDIO + " WHERE " + MediaTableCols.DELETE_FLAG
				+ " = 0 AND " + MediaTableCols.PARSE_STATUS + " = 5 AND " + MediaTableCols.ARTIST + " = ? AND " + MediaTableCols.TITLE + " LIKE ? );";

		Cursor cursor = db.rawQuery(searchSQL, new String[] { artist, "%" + key + "%" });

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}

		return count;
	}

	/**
	 * 取得按Title检索音频播放列表符合媒体数
	 * 
	 * @param key
	 *            关键字
	 * @return 总数
	 */
	public int getFuzzyPlaylistSearchCount(String key) {

		int count = 0;

		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		String searchSQL = "SELECT COUNT(*) FROM (SELECT " + MediaTableCols.ID + " FROM " + TABLE_AUDIO + " WHERE " + MediaTableCols.DELETE_FLAG
				+ " = 0 AND " + MediaTableCols.PLAYLIST_INDEX + " > 0 AND " + MediaTableCols.TITLE + " LIKE ?);";

		Cursor cursor = db.rawQuery(searchSQL, new String[] { "%" + key + "%" });

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				count = cursor.getInt(0);
			}

			cursor.close();
		}

		return count;
	}

	/**
	 * 取得当前播放媒体,取得的媒体如果IsDelete属性为true， 说明本次扫描还没有完成，当前播放媒体还没有表示位置编号(scanIndex)
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @return MediaItem对象，为null时表示没找到当前播放媒体
	 */
	public MediaItem getCurrentPlayMediaItem(MediaType mediaType) {
		logMethodBegin("getCurrentPlayMediaItem");
		MediaItem mediaItem = null;
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		// Cursor cursor = db.query(getTableName(mediaType), MediaTableAllCols,
		// MediaTableCols.IS_PLAY_ITEM + " = 1 AND " +
		// MediaTableCols.DELETE_FLAG + " = 0",
		// null, null, null, null);

		Cursor cursor = db.query(getTableName(mediaType), MediaTableAllCols, MediaTableCols.IS_PLAY_ITEM + " = 1", null, null, null, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				mediaItem = cursorToMediaItem(cursor);
			}

			cursor.close();
		}

		if (mediaItem != null && mediaItem.isDelete()) {
			File playFile = new File(mediaItem.getFilePath());
			if (!playFile.exists() || playFile.lastModified() != mediaItem.getLastModified() || playFile.length() != mediaItem.getSize()) {
				mediaItem = null;
			}
		}
		logMethodEnd("getCurrentPlayMediaItem");
		return mediaItem;
	}

	/**
	 * 设定当前播放媒体对象
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return true:设定成功, false:设定失败
	 */
	public synchronized boolean setCurrentPlayMediaItem(MediaItem mediaItem) {
		logMethodBegin("setCurrentPlayMediaItem");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();

		int updateCount = 0;
		String table = getTableName(mediaItem.getMediaType());

		// 更新取消当前播放媒体
		ContentValues values = new ContentValues();
		values.put(MediaTableCols.IS_PLAY_ITEM, 0);

		db.update(table, values, MediaTableCols.IS_PLAY_ITEM + " = 1", null);

		// 设定当前播放媒体
		values = new ContentValues();
		values.put(MediaTableCols.POSITION, mediaItem.getPosition());
		values.put(MediaTableCols.IS_PLAY_ITEM, 1);

		if (mediaItem.getID() != null) {
			updateCount = db.update(table, values, MediaTableCols.ID + " = ?", new String[] { mediaItem.getID().toString() });
		} else {
			updateCount = db.update(table, values, MediaTableCols.FILE_PATH + " = ?", new String[] { mediaItem.getFilePath() });
		}

		if (updateCount > 0) {
			mediaItem.setPlayItem(true);
		}
		logMethodEnd("setCurrentPlayMediaItem");
		return updateCount > 0;
	}

	/**
	 * @Description: 将文件路径转成MediaItem对象
	 * @param mediaType
	 * @return MediaItem实例
	 */
	public MediaItem getMediaItemByFilePath(String path) {
		MediaItem item = null;
		MediaType type = MediaFile.getFileType(path);
		String dbName = getTableName(type);
		if (dbName == null)
			return null;
		SQLiteDatabase db = mDBHelper.getReadableDatabase();
		StringBuffer selectSQL = new StringBuffer();

		selectSQL.append("SELECT *");
		selectSQL.append(" FROM ");
		selectSQL.append(dbName);
		selectSQL.append(" WHERE ");
		selectSQL.append(MediaTableCols.FILE_PATH);
		selectSQL.append(" = ?");
		Cursor c = db.rawQuery(selectSQL.toString(), new String[] { path });

		if (c.moveToFirst())
			item = cursorToMediaItem(c);
		c.close();

		return item;
	}

	/**
	 * @Description: 将文件名字转成MediaItem对象
	 * @param mediaType
	 * @return MediaItem实例
	 */
	public MediaItem getMediaItemByFileName(String name) {
		MediaItem item = null;
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		StringBuffer selectSQL = new StringBuffer();

		selectSQL.append("SELECT *");
		selectSQL.append(" FROM ");
		selectSQL.append(MediaType.AUDIO);
		selectSQL.append(" WHERE ");
		selectSQL.append(MediaTableCols.NAME);
		selectSQL.append(" LIKE ?");
		Cursor c = db.rawQuery(selectSQL.toString(), new String[] { "%" + name + "%" });

		if (c.moveToFirst())
			item = cursorToMediaItem(c);
		c.close();

		return item;
	}

	/**
	 * 将Cursor中当前行的数据转为MediaItem对象
	 * 
	 * @param cursor
	 *            queryAllMedia(), queryAlbumMediaItems(), queryArtistMediaItems()函数返回的Cursor
	 * @return MediaItem对象
	 */
	public MediaItem cursorToMediaItem(Cursor cursor) {
		MediaItem mediaItem = null;

		if (cursor != null) {
			mediaItem = new MediaItem(MediaType.getMediaType(cursor.getInt(MediaTableAllColsIndex.MEDIA_TYPE)),
					cursor.getString(MediaTableAllColsIndex.NAME), cursor.getString(MediaTableAllColsIndex.FILE_PATH),
					cursor.getLong(MediaTableAllColsIndex.SIZE), cursor.getLong(MediaTableAllColsIndex.LAST_MODIFIED),
					cursor.getString(MediaTableAllColsIndex.NAME_ALPHABET));

			mediaItem.setID(cursor.getLong(MediaTableAllColsIndex.ID));
			mediaItem.setMediaID(cursor.getLong(MediaTableAllColsIndex.MEDIA_ID));
			mediaItem.setTitle(cursor.getString(MediaTableAllColsIndex.TITLE));
			mediaItem.setAlbumID(cursor.getLong(MediaTableAllColsIndex.ALBUM_ID));
			mediaItem.setAlbum(cursor.getString(MediaTableAllColsIndex.ALBUM));
			mediaItem.setArtist(cursor.getString(MediaTableAllColsIndex.ARTIST));
			mediaItem.setDuration(cursor.getLong(MediaTableAllColsIndex.DURATION));
			mediaItem.setPosition(cursor.getLong(MediaTableAllColsIndex.POSITION));

			mediaItem.setPlayItem(cursor.getInt(MediaTableAllColsIndex.IS_PLAY_ITEM) == 1 ? true : false);

			mediaItem.setPlaylistIndex(cursor.getLong(MediaTableAllColsIndex.PLAYLIST_INDEX));

			mediaItem.setParseStatus(ParseStatus.status(cursor.getInt(MediaTableAllColsIndex.PARSE_STATUS)));

			mediaItem.setUpdateTime(cursor.getLong(MediaTableAllColsIndex.UPDATE_TIME));
			mediaItem.setScanIndex(cursor.getLong(MediaTableAllColsIndex.SCAN_INDEX));
			mediaItem.setDelete(cursor.getInt(MediaTableAllColsIndex.DELETE_FLAG) == 1 ? true : false);
			mediaItem.setCollected(cursor.getInt(MediaTableAllColsIndex.IS_COLLECTED) == 1 ? true : false);
		}

		return mediaItem;
	}

	/**
	 * 将Cursor中的数据转为List<MediaItem>对象
	 * 
	 * @param cursorqueryAllMedia
	 *            (), queryAlbumMediaItems(), queryArtistMediaItems()函数返回的Cursor
	 * @return List<MediaItem>对象
	 */
	public List<MediaItem> cursorToMediaItems(Cursor cursor) {
		List<MediaItem> mediaItems = null;

		if (cursor != null) {
			mediaItems = new ArrayList<MediaItem>();

			while (cursor.moveToNext()) {
				mediaItems.add(cursorToMediaItem(cursor));
			}
		}

		return mediaItems;
	}

	/**
	 * 将Cursor中当前行的数据转为AlbumItem对象
	 * 
	 * @param cursor
	 *            queryAlbums()函数返回的Cursor
	 * @return AlbumItem对象
	 */
	public AlbumItem cursorToAlbumItem(Cursor cursor) {
		AlbumItem albumItem = null;

		if (cursor != null) {
			albumItem = new AlbumItem(cursor.getLong(MediaTableAlbumColsIndex.ID), cursor.getString(MediaTableAlbumColsIndex.ALBUM),
					cursor.getInt(MediaTableAlbumColsIndex.ITEMS_COUNT));
		}

		return albumItem;
	}

	/**
	 * 将Cursor中的数据转为AlbumItem对象
	 * 
	 * @param cursor
	 *            queryAlbums()函数返回的Cursor
	 * @return List<AlbumItem>对象
	 */
	public List<AlbumItem> cursorToAlbumItems(Cursor cursor) {
		List<AlbumItem> albumItems = null;
		if (cursor != null) {
			albumItems = new ArrayList<AlbumItem>();

			while (cursor.moveToNext()) {
				albumItems.add(cursorToAlbumItem(cursor));
			}
		}
		return albumItems;
	}

	/**
	 * 将Cursor中当前行的数据转为ArtistItem对象
	 * 
	 * @param cursor
	 *            queryArtists()函数返回的Cursor
	 * @return ArtistItem对象
	 */
	public ArtistItem cursorToArtistItem(Cursor cursor) {
		ArtistItem artistItem = null;

		if (cursor != null) {
			artistItem = new ArtistItem(0, cursor.getString(MediaTableArtistColsIndex.ARTIST), cursor.getInt(MediaTableArtistColsIndex.ITEMS_COUNT));
		}

		return artistItem;
	}

	/**
	 * 将Cursor中的数据转为List<ArtistItem>对象
	 * 
	 * @param cursor
	 *            queryArtists()函数返回的Cursor
	 * @return List<ArtistItem>对象
	 */
	public List<ArtistItem> cursorToArtistItems(Cursor cursor) {
		List<ArtistItem> artistItems = null;
		if (cursor != null) {
			artistItems = new ArrayList<ArtistItem>();

			while (cursor.moveToNext()) {
				artistItems.add(cursorToArtistItem(cursor));
			}
		}
		return artistItems;
	}

	/**
	 * 检查指定文件中DB中是否存在，包括伪删除媒体
	 * 
	 * @param filePath
	 *            媒体文件全路径
	 * @return true:在DB中存在, false:在DB中不存
	 */
	public boolean isExistsMedia(String filePath) {
		logMethodBegin("isExistsMedia " + filePath);
		File mediaFile = new File(filePath);
		boolean ret = isExistsMedia(MediaFile.getFileType(filePath), filePath, mediaFile.lastModified(), mediaFile.length());
		logMethodEnd("isExistsMedia" + filePath);
		return ret;
	}

	/**
	 * 检查指定文件中DB中是否存在，包括伪删除媒体
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return true:在DB中存在, false:在DB中不存
	 */
	public boolean isExistsMedia(MediaItem mediaItem) {
		logMethodBegin("isExistsMedia");
		boolean ret = isExistsMedia(mediaItem.getMediaType(), mediaItem.getFilePath(), mediaItem.getLastModified(), mediaItem.getSize());
		logMethodEnd("isExistsMedia");
		return ret;
	}

	/**
	 * 判断文件是否存在DB中
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @param filePath
	 *            文件路径
	 * @param lastModifide
	 *            文件最后修改时间
	 * @param size
	 *            文件大小
	 * @return true:文件在DB中存在，false:文件在DB中不存在
	 */
	private boolean isExistsMedia(MediaType mediaType, String filePath, long lastModifide, long size) {
		logMethodBegin("isExistsMedia " + mediaType);
		boolean exists = false;
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		Cursor cursor = db.query(getTableName(mediaType), MediaTableAllCols, MediaTableCols.FILE_PATH + " = ? AND " + MediaTableCols.LAST_MODIFIED
				+ " = ? AND " + MediaTableCols.SIZE + " = ?", new String[] { filePath, Long.toString(lastModifide), Long.toString(size) }, null,
				null, null);

		if (cursor != null) {
			exists = (cursor.getCount() > 0);

			cursor.close();
		}
		logMethodEnd("isExistsMedia " + mediaType);
		return exists;
	}

	/**
	 * 更新媒体类型的PlaylistIndex属性
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @param playlistIndex
	 *            0:不在播放列表中, >0在播放列表中的位置
	 * @return 更新后的媒体对象(修改传入参数mediaItem.PlaylistIndex属性后的对象)
	 */
	private int updatePlayListItem(MediaItem mediaItem, long playlistIndex) {
		logMethodBegin("updatePlayListItem ");
		SQLiteDatabase db = mDBHelper.getWritableDatabase();

		String whereClause = null;
		String[] whereArgs = null;
		ContentValues values = new ContentValues();
		values.put(MediaTableCols.PLAYLIST_INDEX, Long.toString(playlistIndex));

		mediaItem.setPlaylistIndex(playlistIndex);

		if (mediaItem.getID() == null) {
			whereClause = MediaTableCols.FILE_PATH + " = ?";
			whereArgs = new String[] { mediaItem.getFilePath() };
		} else {
			whereClause = MediaTableCols.ID + " = ?";
			whereArgs = new String[] { mediaItem.getID().toString() };
		}
		int tempResult = -1;
		try {
			beginDBTransaction(db);
			tempResult = db.update(TABLE_AUDIO, values, whereClause, whereArgs);

			L.d("test", "db.update tempResult:" + tempResult);
			db.setTransactionSuccessful();
		} finally {
			endDBTransaction(db);
		}
		logMethodEnd("updatePlayListItem ");
		return (int) playlistIndex;
	}

	/**
	 * 取得指定媒体类型对应的表名
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @return 表名
	 */
	private String getTableName(MediaType mediaType) {
		String tableName = null;
		if (mediaType == MediaType.AUDIO) {
			tableName = TABLE_AUDIO;
		} else if (mediaType == MediaType.IMAGE) {
			tableName = TABLE_IMAGE;
		} else if (mediaType == MediaType.VIDEO) {
			tableName = TABLE_VIDEO;
		} else if (mediaType == MediaType.OFFICE) {
			tableName = TABLE_OFFICE;
		}

		return tableName;
	}

	/**
	 * 取得替换媒体表中数据内容对应的SQL文
	 * 
	 * @param mediaType
	 *            媒体类型
	 * @return "REPLCE INTO" SQL文
	 */
	private String getReplaceSql(MediaType mediaType) {
		String replaceSQL = null;

		if (mediaType == MediaType.AUDIO) {
			replaceSQL = REPLACE_AUDIO_SQL;
		} else if (mediaType == MediaType.IMAGE) {
			replaceSQL = REPLACE_IMAGE_SQL;
		} else if (mediaType == MediaType.VIDEO) {
			replaceSQL = REPLACE_VIDEO_SQL;
		} else if (mediaType == MediaType.OFFICE) {
			replaceSQL = REPLACE_OFFICE_SQL;
		}

		return replaceSQL;
	}

	/**
	 * 取得SQL文中LIMIT语句对应的内容
	 * 
	 * @param startIndex
	 *            开始位置
	 * @param count
	 *            取得数量，小于0时LIMIT语句返回null
	 * @return LIMIT语句中的内容
	 */
	private String getLimitSQL(int startIndex, int count) {
		String limit = null;
		if (startIndex >= 0 && count > 0) {
			limit = startIndex + ", " + count;
		}
		return limit;
	}

	/**
	 * 取得播放列表中最大的表示编号
	 * 
	 * @return 最大的表示编号
	 */
	private long getMaxPlaylistIndex() {
		long playlistIndex = 0;
		SQLiteDatabase db = mDBHelper.getReadableDatabase();

		// 取得当前播放列表最大编号
		Cursor cursor = db.rawQuery("SELECT MAX(" + MediaTableCols.PLAYLIST_INDEX + ") FROM " + TABLE_AUDIO, null);

		if (cursor != null) {
			if (cursor.moveToFirst()) {
				playlistIndex = cursor.getLong(0);
			}

			cursor.close();
		}

		return playlistIndex;
	}

	/**
	 * 将媒体对象转为Object[]
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return 全部字段对应的Object[]
	 */
	private Object[] mediaItemToArray(MediaItem mediaItem) {
		return new Object[] { mediaItem.getID(), mediaItem.getMediaID(), mediaItem.getAlbumID(), mediaItem.getMediaType().getId(),
				mediaItem.getName(), mediaItem.getTitle(), mediaItem.getAlbum(), mediaItem.getArtist(), mediaItem.getFilePath(), mediaItem.getSize(),
				mediaItem.getLastModified(), mediaItem.getDuration(), mediaItem.getPosition(), mediaItem.isPlayItem() ? 1 : 0,
				mediaItem.getPlaylistIndex(), mediaItem.getParseStatus().value(), mediaItem.getUpdateTime(), mediaItem.getScanIndex(),
				mediaItem.isDelete() ? 1 : 0, mediaItem.getNameAlphabet(), mediaItem.isCollected() ? 1 : 0 };
	}

	/**
	 * 将媒体对象中的Metadata内容转存到ContentValues对象中
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return 媒体对象Metadata字段对应的ContentValues对象
	 */
	private ContentValues metadataToContentValues(MediaItem mediaItem) {
		ContentValues values = new ContentValues();

		values.put(MediaTableCols.MEDIA_ID, mediaItem.getMediaID());
		values.put(MediaTableCols.ALBUM_ID, mediaItem.getAlbumID());
		values.put(MediaTableCols.MEDIA_TYPE, mediaItem.getMediaType().getId());
		values.put(MediaTableCols.TITLE, mediaItem.getTitle());
		// values.put(MediaTableCols.NAME_ALPHABET,
		// mediaItem.getNameAlphabet());
		values.put(MediaTableCols.ALBUM, mediaItem.getAlbum());
		values.put(MediaTableCols.ARTIST, mediaItem.getArtist());
		values.put(MediaTableCols.DURATION, mediaItem.getDuration());
		values.put(MediaTableCols.PARSE_STATUS, mediaItem.getParseStatus().value());
		values.put(MediaTableCols.UPDATE_TIME, mediaItem.getUpdateTime());

		return values;
	}

	/**
	 * 将媒体对象内容转存到ContentValues对象中
	 * 
	 * @param mediaItem
	 *            媒体对象
	 * @return 媒体对象全字段对应的ContentValues对象
	 */
	private ContentValues mediaItemToContentValues(MediaItem mediaItem) {
		ContentValues values = new ContentValues();

		values.put(MediaTableCols.MEDIA_ID, mediaItem.getMediaID());
		values.put(MediaTableCols.ALBUM_ID, mediaItem.getAlbumID());
		values.put(MediaTableCols.MEDIA_TYPE, mediaItem.getMediaType().getId());
		values.put(MediaTableCols.NAME, mediaItem.getName());
		values.put(MediaTableCols.TITLE, mediaItem.getTitle());
		values.put(MediaTableCols.ALBUM, mediaItem.getAlbum());
		values.put(MediaTableCols.ARTIST, mediaItem.getArtist());
		values.put(MediaTableCols.FILE_PATH, mediaItem.getFilePath());
		values.put(MediaTableCols.SIZE, mediaItem.getSize());
		values.put(MediaTableCols.LAST_MODIFIED, mediaItem.getLastModified());
		values.put(MediaTableCols.DURATION, mediaItem.getDuration());
		values.put(MediaTableCols.POSITION, mediaItem.getPosition());
		values.put(MediaTableCols.IS_PLAY_ITEM, mediaItem.isPlayItem() ? 1 : 0);
		values.put(MediaTableCols.PLAYLIST_INDEX, mediaItem.getPlaylistIndex());
		values.put(MediaTableCols.PARSE_STATUS, mediaItem.getParseStatus().value());
		values.put(MediaTableCols.UPDATE_TIME, mediaItem.getUpdateTime());
		values.put(MediaTableCols.SCAN_INDEX, mediaItem.getScanIndex());
		values.put(MediaTableCols.DELETE_FLAG, mediaItem.isDelete() ? 1 : 0);
		values.put(MediaTableCols.NAME_ALPHABET, mediaItem.getNameAlphabet());
		values.put(MediaTableCols.IS_COLLECTED, mediaItem.isCollected() ? 1 : 0);

		return values;
	}

	/**
	 * 开始一个ＤＢ事务
	 * 
	 * @param db
	 *            SQLiteDatabase
	 */
	private void beginDBTransaction(SQLiteDatabase db) {
		for (int i = 0; i < 2; i++) {
			try {
				if (db == null || !db.isOpen()) {
					db = mDBHelper.getWritableDatabase();
				}

				db.beginTransaction();
				break;
			} catch (Exception e) {
				e.printStackTrace();
				if (db != null && !db.isOpen()) {
					db.close();
				}
				db = null;
			}
		}
	}

	/**
	 * 结束一个ＤＢ事务
	 * 
	 * @param db
	 *            SQLiteDatabase
	 */
	private void endDBTransaction(SQLiteDatabase db) {
		try {
			if (db != null && db.isOpen()) {
				db.endTransaction();
			} else {
				Log.w(TAG, "endDBTransaction db is null/not Open");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * UsbMedia Helper类，完成DB创建及DB版本更新处理
	 */
	static class UsbMediaDatabaseHelper extends SQLiteOpenHelper {

		/**
		 * UsbMedia库中所有数据保存的DB文件名
		 */
		private static final String DATABASE_NAME = "UsbMedia.db";

		/**
		 * DB版本号
		 */
		private static final int DATABASE_VERSION = 1;

		/**
		 * 创建各媒体表中对应的字段定义信息
		 */
		private static final String CREATE_TABLE_MEDIA_CLOS = " (" + MediaTableCols.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ MediaTableCols.MEDIA_ID + " INTEGER DEFAULT (-1), " + MediaTableCols.ALBUM_ID + " INTEGER DEFAULT (-1), "
				+ MediaTableCols.MEDIA_TYPE + " INTEGER DEFAULT (0), " + MediaTableCols.NAME + " TEXT NOT NULL, " + MediaTableCols.TITLE
				+ " TEXT NOT NULL, " + MediaTableCols.ALBUM + " TEXT, " + MediaTableCols.ARTIST + " TEXT, " + MediaTableCols.FILE_PATH
				+ " TEXT NOT NULL, " + MediaTableCols.SIZE + " INTEGER, " + MediaTableCols.LAST_MODIFIED + " INTEGER, " + MediaTableCols.DURATION
				+ " INTEGER DEFAULT (-1), " + MediaTableCols.POSITION + " INTEGER DEFAULT (-1), " + MediaTableCols.IS_PLAY_ITEM
				+ " INTEGER DEFAULT (0), " + MediaTableCols.PLAYLIST_INDEX + " INTEGER DEFAULT (0), " + MediaTableCols.PARSE_STATUS
				+ " INTEGER DEFAULT (0), " + MediaTableCols.UPDATE_TIME + " INTEGER, " + MediaTableCols.SCAN_INDEX + " INTEGER NOT NULL, "
				// + MediaTableCols.DELETE_FLAG + " INTEGER DEFAULT (0))";

				+ MediaTableCols.DELETE_FLAG + " INTEGER DEFAULT (0), " + MediaTableCols.NAME_ALPHABET + " TEXT NOT NULL, "
				+ MediaTableCols.IS_COLLECTED + " INTEGER DEFAULT (0))";

		/**
		 * UsbMediaDatabaseHelper类实例化
		 * 
		 * @param context
		 *            上下文
		 */
		public UsbMediaDatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onCreate(android.database
		 * .sqlite.SQLiteDatabase)
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			logMethodBegin("onCreate ");
			/* Create Audio Table */
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_AUDIO + CREATE_TABLE_MEDIA_CLOS + ";");

			/* Create Image Table */
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_IMAGE + CREATE_TABLE_MEDIA_CLOS + ";");

			/* Create Video Table */
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_VIDEO + CREATE_TABLE_MEDIA_CLOS + ";");

			/* Create Office Table */
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_OFFICE + CREATE_TABLE_MEDIA_CLOS + ";");

			/* Create Configuration Table */
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONFIGURATION + " (" + ConfigurationCols.KEY + " TEXT NOT NULL PRIMARY KEY , "
					+ ConfigurationCols.VALUE + " TEXT);");

			/* Create AUDIO_FILE_PATH_INDEX Index */
			db.execSQL("CREATE INDEX IF NOT EXISTS AUDIO_FILE_PATH_INDEX ON " + TABLE_AUDIO + "(" + MediaTableCols.FILE_PATH + ");");

			/* Create IMAGE_FILE_PATH_INDEX Index */
			db.execSQL("CREATE INDEX IF NOT EXISTS IMAGE_FILE_PATH_INDEX ON " + TABLE_IMAGE + "(" + MediaTableCols.FILE_PATH + ");");

			/* Create VIDEO_FILE_PATH_INDEX Index */
			db.execSQL("CREATE INDEX IF NOT EXISTS VIDEO_FILE_PATH_INDEX ON " + TABLE_VIDEO + "(" + MediaTableCols.FILE_PATH + ");");

			/* Create OFFICE_FILE_PATH_INDEX Index */
			db.execSQL("CREATE INDEX IF NOT EXISTS OFFICE_FILE_PATH_INDEX ON " + TABLE_OFFICE + "(" + MediaTableCols.FILE_PATH + ");");

			logMethodEnd("onCreate ");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database
		 * .sqlite.SQLiteDatabase, int, int)
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			logMethodBegin("onUpgrade ");
			int version = oldVersion;

			if (version != DATABASE_VERSION) {
				db.execSQL("DROP INDEX IF EXISTS AUDIO_FILE_PATH_INDEX");
				db.execSQL("DROP INDEX IF EXISTS IMAGE_FILE_PATH_INDEX");
				db.execSQL("DROP INDEX IF EXISTS VIDEO_FILE_PATH_INDEX");
				db.execSQL("DROP INDEX IF EXISTS OFFICE_FILE_PATH_INDEX");

				db.execSQL("DROP TABLE IF EXISTS " + TABLE_AUDIO);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGE);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_VIDEO);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_OFFICE);
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFIGURATION);

				onCreate(db);
			}
			logMethodEnd("onUpgrade ");
		}

	}
}
