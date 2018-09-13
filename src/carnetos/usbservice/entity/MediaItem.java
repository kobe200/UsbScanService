package carnetos.usbservice.entity;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Locale;

/**
 * 媒体对象类
 */
public class MediaItem implements Parcelable {

	private static final long INVALID = -1;

	private boolean isSameFile = false;

	public boolean isSameFile(){

		return isSameFile;
	}

	public void setSameFile(boolean isSameFile){

		this.isSameFile = isSameFile;
	}

	public enum ParseStatus {
		/**
		 * 还未做任何处理
		 */
		NONE (0),
		/**
		 * 已经在扫描的队列中
		 */
		INQUEUE (1),
		/**
		 * 正在扫描。
		 */
		PARSING (2),
		/**
		 * 媒体扫描失败
		 */
		PARSE_FAILED (3),
		/**
		 * 扫描完后，从媒体库中查询属性并赋值.
		 */
		QUERING (4),
		/**
		 * 扫描并且查询完毕。
		 */
		FINISHED (5);

		private int	mValue;

		private ParseStatus(int value) {
			mValue = value;
		}

		public int value(){
			return mValue;
		}

		public static ParseStatus status(int value){
			ParseStatus parseStatus = ParseStatus.NONE;

			if (value == 0) {
				parseStatus = ParseStatus.NONE;
			} else if (value == 1) {
				parseStatus = ParseStatus.INQUEUE;
			} else if (value == 2) {
				parseStatus = ParseStatus.PARSING;
			} else if (value == 3) {
				parseStatus = ParseStatus.PARSE_FAILED;
			} else if (value == 4) {
				parseStatus = ParseStatus.QUERING;
			} else if (value == 5) {
				parseStatus = ParseStatus.FINISHED;
			}

			return parseStatus;
		}
	}

	public static enum MediaType {
		UNKNOWN (0), VIDEO (1), AUDIO (2), IMAGE (3), ALL (4), OFFICE(5);

		private int	mIndex	= -1;

		private MediaType(int id) {
			mIndex = id;
		}

		public int getId(){
			return mIndex;
		}

		public static MediaType getMediaType(int id){
			MediaType type = UNKNOWN;
			if (UNKNOWN.getId() == id) {
				type = UNKNOWN;
			} else if (VIDEO.getId() == id) {
				type = VIDEO;
			} else if (AUDIO.getId() == id) {
				type = AUDIO;
			} else if (IMAGE.getId() == id) {
				type = IMAGE;
			} else if (ALL.getId() == id) {
				type = ALL;
			} else if(OFFICE.getId() == id){
				type = OFFICE;
			}
			return type;
		}
	};

	/**
	 * UsbMediaDB中的ID
	 */
	private Long mID;

	/**
	 * 系统媒体库DB中的ID
	 */
	private long mMediaID = INVALID;

	/**
	 * 系统媒体库DB的专辑ID
	 */
	private long mAlbumID = INVALID;

	/**
	 * 媒体文件类型
	 */
	private final MediaType mMediaType;

	/**
	 * 媒体文件名
	 */
	private String mName;

	/**
	 * 媒体Title或文件名
	 */
	private String mTitle;

	/**
	 * 唱片
	 */
	private String mAlbum;

	/**
	 * 艺术家
	 */
	private String mArtist;

	/**
	 * 媒体文件路径
	 */
	private final String mFilePath;

	/**
	 * 媒体文件大小
	 */
	private long mSize;

	/**
	 * 媒体文件最后修改时间
	 */
	private long mLastModified;

	/**
	 * 时长
	 */
	private long mDuration = INVALID;

	/**
	 * 当前已播放时长
	 */
	private long mPosition;

	/**
	 * 是否为当前播放媒体
	 */
	private boolean mIsPlayItem;

	/**
	 * 是否为追加到播放列表中(如果为0则未加入到播放列表)
	 */
	private long mPlaylistIndex;

	/**
	 * DB中数据更新时间
	 */
	private long mUpdateTime;

	/**
	 * 被扫描到的顺序
	 */
	private long mScanIndex;
	
	/**
	 * 是否为伪删除对象
	 */
	private boolean mIsDelete;

	/**
	 * 是否为伪删除对象
	 */
	private String mNameAlphabet;
	
	/**
	 * 是否为收藏对象
	 */
	private boolean mIsCollected;

	/**
	 * 媒体文件解析状态
	 */
	private ParseStatus mParseStatus = ParseStatus.NONE;

	public MediaItem(MediaType mediaType, String name, String filePath, long size, long lastModified, String nameAlphabet) {

		this.mMediaType = mediaType;
		this.mName = name;
		this.mTitle = name;
		this.mFilePath = filePath;
		this.mSize = size;
		this.mLastModified = lastModified;
		this.mNameAlphabet = nameAlphabet;
	}

	public MediaType getMediaType(){
		return mMediaType;
	}

	public synchronized Long getID(){
		return mID;
	}

	public synchronized void setID(Long id){
		mID = id;
	}

	public synchronized long getMediaID(){
		return mMediaID;
	}

	public synchronized void setMediaID(long mediaID){
		this.mMediaID = mediaID;
	}

	public synchronized String getName(){
		return mName;
	}

	public synchronized void setName(String name){
		this.mName = name;
	}

	public synchronized String getTitle(){
		return mTitle;
	}

	public synchronized void setTitle(String title){
		this.mTitle = title;
	}

	public synchronized String getAlbum(){
		return mAlbum;
	}

	public synchronized void setAlbum(String album){
		this.mAlbum = album;
	}

	public synchronized long getAlbumID(){
		return mAlbumID;
	}

	public synchronized void setAlbumID(long albumID){
		this.mAlbumID = albumID;
	}

	public synchronized String getArtist(){
		return mArtist;
	}

	public synchronized void setArtist(String artist){
		this.mArtist = artist;
	}

	public String getFilePath(){
		return mFilePath;
	}

	public synchronized long getSize(){
		return mSize;
	}

	public synchronized void setSize(long length){
		mSize = length;

	}

	public synchronized long getLastModified(){
		return mLastModified;
	}

	public synchronized void setLastModified(long lastModified){
		mLastModified = lastModified;
	}

	public synchronized long getDuration(){
		return mDuration;
	}

	public synchronized void setDuration(long duration){
		this.mDuration = duration;
	}

	public synchronized long getPosition(){
		return mPosition;
	}

	public synchronized void setPosition(long position){
		this.mPosition = position;
	}

	public synchronized boolean isPlayItem(){
		return mIsPlayItem;
	}

	public synchronized void setPlayItem(boolean isPlayItem){
		mIsPlayItem = isPlayItem;
	}

	public boolean isAddPlaylist(){
		return mPlaylistIndex > 0;
	}

	public long getPlaylistIndex(){
		return mPlaylistIndex;
	}

	public void setPlaylistIndex(long playlistIndex){
		this.mPlaylistIndex = playlistIndex;
	}

	public synchronized ParseStatus getParseStatus(){
		return mParseStatus;
	}

	public synchronized void setParseStatus(ParseStatus parseStatus){
		this.mParseStatus = parseStatus;
	}

	public synchronized long getUpdateTime(){
		return mUpdateTime;
	}

	public synchronized void setUpdateTime(long updateTime){
		this.mUpdateTime = updateTime;
	}

	public long getScanIndex(){
		return mScanIndex;
	}

	public void setScanIndex(long scanIndex){
		this.mScanIndex = scanIndex;
	}
	
	public synchronized boolean isDelete(){
		return mIsDelete;
	}
	
	public synchronized void setDelete(boolean deleteFlag){
		this.mIsDelete = deleteFlag;
	}

	public synchronized void setNameAlphabet(String nameAlphabet){
		this.mNameAlphabet = nameAlphabet;
	}

	public synchronized String getNameAlphabet(){
		return mNameAlphabet;
	}
	
	public synchronized boolean isCollected(){
		return mIsCollected;
	}
	
	public synchronized void setCollected(boolean isCollected){
		this.mIsCollected = isCollected;
	}
	// ////////////////// interface IFuzzySearch ////////////////

	public static enum SearchType {
		/**
		 * 按标题
		 */
		MEDIA_TITLE,

		/**
		 * 按专辑
		 */
		ALBUM,

		/**
		 * 按艺术家
		 */
		ARTIST
	};

	public final boolean isMatch(String mediaTitle){
		boolean isMatch = false;

		if (mName != null && mediaTitle != null && !mediaTitle.isEmpty()) {
			isMatch = mName.toLowerCase(Locale.US).contains(mediaTitle.toLowerCase(Locale.US));
		}

		return isMatch;
	}

	public synchronized boolean isMatch(String key,SearchType type){
		boolean isMatch = false;

		if (key != null) {
			if (type == SearchType.MEDIA_TITLE) {
				isMatch = isMatch(key);
			} else if (type == SearchType.ALBUM && mAlbum != null) {
				isMatch = mAlbum.toLowerCase(Locale.US).contains(key.toLowerCase(Locale.US));
			} else if (type == SearchType.ARTIST && mArtist != null) {
				isMatch = mArtist.toLowerCase(Locale.US).contains(key.toLowerCase(Locale.US));
			}
		}
		return isMatch;
	}

	@Override
	public synchronized String toString(){
		String value = "MeidaType:" + mMediaType.getId() + "\t" + mName  + "\t" + mTitle + "\t" + mAlbum + "\t" + mArtist + "\t" + mDuration + "\t" + mFilePath + "\t"
				+ mParseStatus.value();

		return value;
	}

	@Override
	public int describeContents(){
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest,int flags){
		dest.writeInt(this.getMediaType().getId());
		dest.writeInt(this.getParseStatus().value());
		dest.writeLong(this.getID());
		dest.writeLong(this.getMediaID());
		dest.writeLong(this.getAlbumID());
		dest.writeLong(this.getDuration());
		dest.writeLong(this.getLastModified());
		dest.writeLong(this.getPosition());
		dest.writeLong(this.getScanIndex());
		dest.writeLong(this.getSize());
		dest.writeLong(this.getUpdateTime());
		dest.writeString(this.getAlbum());
		dest.writeString(this.getArtist());
		dest.writeString(this.getFilePath());
		dest.writeString(this.getName());
		dest.writeString(this.getTitle());
		dest.writeString(this.getNameAlphabet());
		dest.writeBooleanArray(new boolean[] { this.isDelete(), this.isPlayItem(), this.isCollected() });
		dest.writeLong(this.getPlaylistIndex());
	}

	public static final Parcelable.Creator<MediaItem> CREATOR = new Parcelable.Creator<MediaItem>() {

		// 重写Creator
		@Override
		public MediaItem createFromParcel(Parcel source) {
			MediaType mediaType = MediaType.getMediaType(source.readInt());
			ParseStatus parseStatus = ParseStatus.status(source.readInt());
			long ID = source.readLong();
			long mediaID = source.readLong();
			long albumID = source.readLong();
			long duration = source.readLong();
			long lastModified = source.readLong();
			long position = source.readLong();
			long scanIndex = source.readLong();
			long size = source.readLong();
			long updateTime = source.readLong();
			String album = source.readString();
			String artist = source.readString();
			String filePath = source.readString();
			String name = source.readString();
			String title = source.readString();
			String nameAlphabet = source.readString();

			boolean[] booleanValues = new boolean[3];
			source.readBooleanArray(booleanValues);
			long playlistIndex = source.readLong();
			MediaItem mediaItem = new MediaItem(mediaType, name, filePath,
					size, lastModified, nameAlphabet);
			mediaItem.setTitle(title);
			mediaItem.setAlbum(album);
			mediaItem.setMediaID(mediaID);
			mediaItem.setAlbumID(albumID);
			mediaItem.setArtist(artist);
			mediaItem.setScanIndex(scanIndex);
			mediaItem.setPosition(position);
			mediaItem.setUpdateTime(updateTime);
			mediaItem.setParseStatus(parseStatus);
			mediaItem.setDuration(duration);
			mediaItem.setID(ID);
			mediaItem.setDelete(booleanValues[0]);
			mediaItem.setPlayItem(booleanValues[1]);
			mediaItem.setCollected(booleanValues[2]);
			mediaItem.setPlaylistIndex(playlistIndex);
			mediaItem.setNameAlphabet(nameAlphabet);
			return mediaItem;
		}

		@Override
		public MediaItem[] newArray(int size) {
			return new MediaItem[size];
		}

	};

}
