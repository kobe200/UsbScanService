package carnetos.usbservice.db;

import carnetos.usbservice.entity.MediaItem;
import carnetos.usbservice.util.L;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * 公开数据库
 * 
 * @desc: CookooUsbService
 * @author:
 * @createTime: 2016-11-29 上午9:43:43
 * @history:
 * @version: v1.0
 */
public class UsbProvider extends ContentProvider {

	private UsbMediaDB mUsbMediaDB = null;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {

		switch (ProviderConfig.matcher.match(uri)) {
			case ProviderConfig.AUDIO :

			case ProviderConfig.AUDIOS :

			case ProviderConfig.VIDEO :

			case ProviderConfig.VIDEOS :

			case ProviderConfig.IMAGE :

			case ProviderConfig.IMAGES :

			case ProviderConfig.OFFICE:
				
			case ProviderConfig.OFFICES:
				break;
			default :
				throw new IllegalArgumentException("Unknow URI " + uri);
		}

		return -1;
	}

	@Override
	public String getType(Uri uri) {
		switch (ProviderConfig.matcher.match(uri)) {
		case ProviderConfig.AUDIO:
		case ProviderConfig.AUDIOS:
		case ProviderConfig.VIDEO:
		case ProviderConfig.VIDEOS:
		case ProviderConfig.IMAGE:
		case ProviderConfig.IMAGES:
		case ProviderConfig.OFFICE:
		case ProviderConfig.OFFICES:
			return ProviderConfig.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknow URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		return null;
	}

	@Override
	public boolean onCreate() {
		mUsbMediaDB = UsbMediaDB.getUsbMediaDB(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setProjectionMap(ProviderConfig.map);
		switch (ProviderConfig.matcher.match(uri)) {
			case ProviderConfig.AUDIO :
				L.d("tt", "ProviderConfig.AUDIO");
			case ProviderConfig.AUDIOS :
				L.d("tt", "ProviderConfig.AUDIOS");
				qb.setTables(UsbMediaDB.TABLE_AUDIO);
				break;
			case ProviderConfig.VIDEO :
			case ProviderConfig.VIDEOS :
				qb.setTables(UsbMediaDB.TABLE_VIDEO);
				break;
			case ProviderConfig.IMAGE :
			case ProviderConfig.IMAGES :
				qb.setTables(UsbMediaDB.TABLE_IMAGE);
				break;
			case ProviderConfig.OFFICE:
			case ProviderConfig.OFFICES:
				qb.setTables(UsbMediaDB.TABLE_OFFICE);
				break;
			default :
				throw new IllegalArgumentException("Unknow URI " + uri);
		}
		if (mUsbMediaDB != null) {
			Cursor c = mUsbMediaDB.providerQuery(qb, uri, projection,
					selection, selectionArgs, sortOrder);
			if (c != null)
				c.setNotificationUri(getContext().getContentResolver(), uri);
			return c;
		}
		return null;
	}

	// 更新列表 
	/**
	 * 该方法仅供外部用于 将音乐添加到收藏列表以及从收藏列表中删除操作
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		Long id_ = null;
		int resutlID=-1 ;
		switch (ProviderConfig.matcher.match(uri)) {
			case ProviderConfig.AUDIO :
				Log.d("tt", "AUDIO ProviderConfig.AUDIO");
				break;
			case ProviderConfig.AUDIOS :

				Log.d("tt", "AUDIO ProviderConfig.AUDIOS");
				id_ = values.getAsLong("_id");
				String filePath = values.getAsString("filePath");
				MediaItem mediaItem = new MediaItem(MediaItem.MediaType.AUDIO, null, filePath, 0, 0, null);

				mediaItem.setID(id_);
				if (values.getAsString("action") != null && "del".equals(values.getAsString("action"))) {// 从列表中删除
					resutlID = mUsbMediaDB.removePlayList(mediaItem);
				} else if (values.getAsString("action") != null && "addPlayList".equals(values.getAsString("action"))) {// 保存到收藏列表
					resutlID = mUsbMediaDB.addPlayList(mediaItem);
				}

				break;
			case ProviderConfig.VIDEO :
				Log.d("tt", "AUDIO ProviderConfig.VIDEO");
				break;
			case ProviderConfig.VIDEOS :
				Log.d("tt", "AUDIO ProviderConfig.VIDEO");
				break;
			case ProviderConfig.IMAGE :
				Log.d("tt", "AUDIO ProviderConfig.IMAGE");
			case ProviderConfig.IMAGES :
				Log.d("tt", "AUDIO ProviderConfig.IMAGES");
			case ProviderConfig.OFFICE:
			case ProviderConfig.OFFICES:
			default :
				Log.d("tt", "AUDIO ProviderConfig.AUDIO");
				throw new IllegalArgumentException("Unknow URI " + uri);
		}

		L.d("test", "It wil update resutlID: " + resutlID);
		return resutlID;

	}

}
