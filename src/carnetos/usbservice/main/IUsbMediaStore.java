package carnetos.usbservice.main;

import android.content.Context;

import carnetos.usbservice.db.UsbMediaDB;
import carnetos.usbservice.entity.MediaItem;
import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.main.UsbMediaStore.StoreManagerStatus;

/**
 * USB媒体库接口。
 */
public interface IUsbMediaStore {

	/**
	 * 切换媒体管理类型
	 * 
	 * @author:tang
	 * @createTime: 2016-9-27 下午7:52:15
	 * @history:
	 * @param mediaType
	 *            void
	 */
	public abstract void setStoreMediaType(MediaType mediaType);

	/**
	 * 应用程序退出时调用，已在UsbMediaApplication类中调用
	 * 
	 * @param context
	 *            ApplictionContext
	 */
	public abstract void onAppTerminate(Context context);

	/**
	 * 添加IMediaStoreChangeListener监听，用于监听MediaStore中数据变化
	 * 
	 * @param listener
	 *            IMediaStoreChangeListener实现类
	 */
	public abstract void addMediaStoreChangeListener(IMediaStoreChangeListener listener);

	/**
	 * 移除IMediaStoreChangeListener监听, 取消监听MediaStore中数据变化
	 * 
	 * @param listener
	 *            IMediaStoreChangeListener实现类
	 */
	public abstract void removeMediaStoreChangeListener(IMediaStoreChangeListener listener);

	/**
	 * 开始UsbMediaStore的媒体管理功能
	 * 
	 * @param context
	 *            UsbMediaApplication.ApplictionContext
	 */
	public abstract void startStoreManager(Context context);

	/**
	 * 停止UsbMediaStore的媒体管理功能
	 */
	public abstract void stopStoreManager();

	/**
	 * 判断UsbDisk设备是否已挂载
	 * 
	 * @return true:已挂载，false:未挂载
	 */
	public abstract boolean isUsbMounted(String rootPath);

	/**
	 * 判断UsbMediaStore管理模块是否完成媒体扫描
	 * 
	 * @return true:已完成媒体扫描，false:未完成媒体扫描
	 */
	public abstract boolean isFileScanFinished(String rootPath);

	/**
	 * 判断UsbMediaStore管理模块是否开始运行
	 * 
	 * @return true:已开始运行，false:未开始运行
	 */
	public abstract boolean isStarted();

	public abstract boolean isFinish();

	/**
	 * 取得UsbMediaStore管理模块管理的媒体类型
	 * 
	 * @return MediaType {UNKNOWN, VIDEO, AUDIO, IMAGE}
	 */
	public abstract MediaType getStoreMediaType();

	/**
	 * 取得UsbMediaStore管理模块的运行状态
	 * 
	 * @return StoreManagerStatus(NONE, START, PAUSE, STOP)
	 */
	public abstract StoreManagerStatus getStoreManagerStatus();

	/**
	 * 取得UsbMediaDB对象完成媒体数据取得及数据更新等操作
	 * 
	 * @return UsbMediaDB操作类
	 */
	public abstract UsbMediaDB getUsbMediaDB();

	/**
	 * 取得IMedatataRetriever接口对象
	 * 
	 * @return IMedatataRetriever 媒体Metadata扫描接口
	 */
	public abstract IMedatataRetriever getMetadataRetriever();

	/**
	 * 获取指定文件的媒体信息
	 * 
	 * @param filePath
	 *            媒体全路径
	 * @return MediaItem媒体对象类
	 */
	public abstract MediaItem getMetadata(String filePath);

	/**
	 * 删除指定媒体对象，同时删除UDisk设备上的文件
	 * 
	 * @param mediaItem
	 *            媒体对象类
	 * @return true:删除成功, false:删除失败
	 */
	public abstract boolean removeMediaFile(String path);

	public abstract void setCPURefrain(boolean isRefrain);

	public abstract boolean mediaFileCollecte(boolean isCollected, String path);
	
	public abstract void addPriorityMedaiItem(String path);

}
