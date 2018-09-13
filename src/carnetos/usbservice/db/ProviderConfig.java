package carnetos.usbservice.db;

import java.util.HashMap;

import carnetos.usbservice.db.UsbMediaDB.MediaTableCols;

import android.content.UriMatcher;
import android.net.Uri;

public class ProviderConfig {

	/*
	 * provider的AUTHORITY
	 */
	public static final String AUTHORITY = "carnetos.usbservice.provider";
	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/carnetos.usb";
	/*
	 * AUDIO URI
	 */
	public static final Uri AUDIO_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + UsbMediaDB.TABLE_AUDIO);
	/*
	 * VIDEO URI
	 */
	public static final Uri VIDEO_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + UsbMediaDB.TABLE_VIDEO);
	/*
	 * IMAGE URI
	 */
	public static final Uri	IMAGE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + UsbMediaDB.TABLE_IMAGE);


	/*
	 * OFFICE URI
	 */
	public static final Uri	OFFICE_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + UsbMediaDB.TABLE_OFFICE);

	
	/*
	 * 匹配项
	 */
	public static final int AUDIOS = 0x00;
	public static final int AUDIO = AUDIOS + 1;
	public static final int VIDEOS = AUDIOS + 2;
	public static final int VIDEO = AUDIOS + 3;
	public static final int IMAGES = AUDIOS + 4;
	public static final int IMAGE = AUDIOS + 5;
	public static final int OFFICE = AUDIOS + 6;
	public static final int OFFICES = AUDIOS + 7;

	public static UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH) {

		{
			addURI(AUTHORITY, UsbMediaDB.TABLE_AUDIO, AUDIOS);
			addURI(AUTHORITY, UsbMediaDB.TABLE_AUDIO + "/#", AUDIO);

			addURI(AUTHORITY, UsbMediaDB.TABLE_IMAGE, IMAGES);
			addURI(AUTHORITY, UsbMediaDB.TABLE_IMAGE + "/#", IMAGE);

			addURI(AUTHORITY, UsbMediaDB.TABLE_VIDEO, VIDEOS);
			addURI(AUTHORITY, UsbMediaDB.TABLE_VIDEO + "/#", VIDEO);

			addURI(AUTHORITY, UsbMediaDB.TABLE_OFFICE, OFFICES);
			addURI(AUTHORITY, UsbMediaDB.TABLE_OFFICE + "/#", OFFICE);
		}
	};
	public static HashMap<String, String> map = new HashMap<String, String>() {
		{
			put(MediaTableCols.ID, MediaTableCols.ID);
			put(MediaTableCols.MEDIA_ID, MediaTableCols.MEDIA_ID);
			put(MediaTableCols.ALBUM_ID, MediaTableCols.ALBUM_ID);
			put(MediaTableCols.MEDIA_TYPE, MediaTableCols.MEDIA_TYPE);
			put(MediaTableCols.NAME, MediaTableCols.NAME);
			put(MediaTableCols.TITLE, MediaTableCols.TITLE);
			put(MediaTableCols.ALBUM, MediaTableCols.ALBUM);
			put(MediaTableCols.ARTIST, MediaTableCols.ARTIST);
			put(MediaTableCols.FILE_PATH, MediaTableCols.FILE_PATH);
			put(MediaTableCols.SIZE, MediaTableCols.SIZE);
			put(MediaTableCols.LAST_MODIFIED, MediaTableCols.LAST_MODIFIED);
			put(MediaTableCols.DURATION, MediaTableCols.DURATION);
			put(MediaTableCols.POSITION, MediaTableCols.POSITION);
			put(MediaTableCols.IS_PLAY_ITEM, MediaTableCols.IS_PLAY_ITEM);
			put(MediaTableCols.PLAYLIST_INDEX, MediaTableCols.PLAYLIST_INDEX);
			put(MediaTableCols.PARSE_STATUS, MediaTableCols.PARSE_STATUS);
			put(MediaTableCols.UPDATE_TIME, MediaTableCols.UPDATE_TIME);
			put(MediaTableCols.SCAN_INDEX, MediaTableCols.SCAN_INDEX);
			put(MediaTableCols.DELETE_FLAG, MediaTableCols.DELETE_FLAG);
			put(MediaTableCols.NAME_ALPHABET, MediaTableCols.NAME_ALPHABET);
			put(MediaTableCols.IS_COLLECTED, MediaTableCols.IS_COLLECTED);

		}
	};
}
