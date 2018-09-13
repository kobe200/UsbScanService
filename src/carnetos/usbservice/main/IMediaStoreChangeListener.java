package carnetos.usbservice.main;

import carnetos.usbservice.entity.MediaItem;

import java.util.List;

/**
 * 媒体改变（添加，删除，清空）回调接口。
 */
public interface IMediaStoreChangeListener {

	/**
	 * 回掉接口：UsbDisk挂载。
	 */
	void onUsbDiskMounted(String root);

	/**
	 * 回掉接口：UsbDisk卸载。
	 */
	void onUsbDiskUnMounted(String root);

	/**
	 * 回掉接口：音乐媒体文件添加。
	 * 
	 * @param items
	 *            媒体对象集合
	 */
	void onAudioFilesAdded(List<MediaItem> items);

	/**
	 * 回掉接口：图片媒体文件添加。
	 * 
	 * @param items
	 *            媒体对象集合
	 */
	void onImageFilesAdded(List<MediaItem> items);

	/**
	 * 回掉接口：视频媒体文件添加。
	 * 
	 * @param items
	 *            媒体对象集合
	 */
	void onVideoFilesAdded(List<MediaItem> items);

	/** 
	 * @Description 
	 * @param items  
	 */
	  	
	void onOfficeFilesAdded(List<MediaItem> items);
	
	/**
	 * 回掉接口：有媒体文件删除。
	 * 
	 * @param items
	 *            媒体对象集合
	 */
	void onMediaFilesRemoved(List<MediaItem> items);

	/**
	 * 回掉接口：媒体文件列表清空。
	 */
	void onMediaFilesCleared(String root);

	/**
	 * 回掉接口：媒体文件列举完成。
	 */
	void onFileScanFinished(String root);

}
