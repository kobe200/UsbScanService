package carnetos.usbservice.main;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import carnetos.usbservice.aidl.UsbServiceManager;
import carnetos.usbservice.db.UsbMediaDB;
import carnetos.usbservice.entity.MediaItem;
import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.entity.MediaItem.ParseStatus;
import carnetos.usbservice.entity.MediaItems;
import carnetos.usbservice.util.AndroidMediaStoreUtil;
import carnetos.usbservice.util.CharacterParser;
import carnetos.usbservice.util.Configuration;
import carnetos.usbservice.util.L;
import carnetos.usbservice.util.MFile;
import carnetos.usbservice.util.MediaFile;
import carnetos.usbservice.util.SortUtil;
import carnetos.usbservice.util.UsbDevice;
import carnetos.usbservice.util.UsbDevice.UsbDeviceScanState;
import carnetos.usbservice.util.UsbDiskUtil;

/**
 * 获取U盘中的媒体文件，通知有文件加入到列表中。 监听Usbmount,Usbunmount事件。
 */
public class UsbMediaStore implements IUsbMediaStore {

	/**
	 * UsbMediaStore运行状态 未开始、速度一级、速度二级、完成
	 */
	public static enum StoreManagerStatus {
		NONE, SPEED_LEVEL1, SPEED_LEVEL2, SPEED_LEVEL3, FINISH
	}

	private static final String TAG = "UsbMediaStore";

	/**
	 * 保存UsbMediaApplication的Context
	 */
	private WeakReference<Context> mContext;

	/**
	 * 当前UsbMediaStore管理的媒体类型这个会变，跟随页面，给出接口设置该类型
	 */
	private MediaType mStoreMediaType = MediaType.AUDIO;

	/**
	 * 当前UsbMediaStore扫描的媒体类型这个不会变跟随Application
	 */
	private MediaType mScanMediaType;

	/**
	 * UsbMediaDB操作类
	 */
	private UsbMediaDB mUsbMediaDB;

	/**
	 * 接收UDisk设备挂载与御载通知
	 */
	private UsbDiskReceiver mUsbEventReceiver;

	/**
	 * UsbMediaStore运行状态
	 */
	public static StoreManagerStatus mStoreManagerStatus;

	/**
	 * 完成UDisk设备媒体文件扫描并加入到UsbMediaDB中
	 */
	// private List<ScanUsbMediaAsyncTask> mScanUsbMediaAsyncTasks = new
	// ArrayList<UsbMediaStore.ScanUsbMediaAsyncTask>();

	private List<ScanTask> mScanUsbMediaTasks = new ArrayList<ScanTask>();

	/**
	 * 完成对插入到UsbMediaDB中未进行Metadata解析的媒体进行Metadata解析
	 */
	private ObtainMediaInfoTask mObtainMediaInfoTask;

	/**
	 * 保存注册的IMediaStoreChangeListener监听实例
	 */
	private CopyOnWriteArrayList<IMediaStoreChangeListener> mMediaStoreChangeListeners;

	/**
	 * 保存ObtainMediaInfoAsyncTask中优先请求解析的媒体信息
	 */
	private CopyOnWriteArrayList<MediaItem> mPriorityMedaiItems;

	/**
	 * 媒体Metadata扫描接口实现类
	 */
	private MedatataRetriever mMedatataRetriever;

	private float mLastSystemTime = 0, mLastIdleTime = 0;
	/**
	 * 允许多媒体扫描解析时CPU空闲率允许20%，即总的CPU使用率不能大于80%
	 */
	private float mCpuAllowIdleRate = 0.25f;
	/**
	 * 提供删除Android系统媒体库中Audio、Video、Image媒体操作工具类
	 */
	private AndroidMediaStoreUtil mAndroidMediaStoreUtil;

	private boolean isBootComplete = false;

	/**
	 * UsbMediaStore实例化
	 * 
	 * @param context
	 *            上下文
	 * @param mediaType
	 *            UsbMediaStore管理的媒体类型
	 * @param scanMediaType
	 *            UsbMediaStore扫描的媒体类型
	 */
	public UsbMediaStore(Context context, MediaType mediaType, MediaType scanMediaType) {
		L.d("UsbMediaStore");
		/* 保存参数 */
		mContext = new WeakReference<Context>(context);
		mStoreMediaType = mediaType;
		mScanMediaType = scanMediaType;
		/* 完成初始化相关工作 */
		initMediaStore(context);
	}

	@Override
	public void onAppTerminate(Context context) {
		/* 停止所有后台处理 */
		stopStoreManager();

		/* 反初始化全局变量 */
		mPriorityMedaiItems.clear();
		mPriorityMedaiItems = null;

		mMediaStoreChangeListeners.clear();
		mMediaStoreChangeListeners = null;

		mUsbMediaDB.closeDB();
	}

	@Override
	public void addMediaStoreChangeListener(IMediaStoreChangeListener listener) {
		mMediaStoreChangeListeners.add(listener);
	}

	@Override
	public void removeMediaStoreChangeListener(IMediaStoreChangeListener listener) {
		mMediaStoreChangeListeners.remove(listener);
	}

	@Override
	public void startStoreManager(Context context) {
		L.d("startStoreManager ThreadID: " + Thread.currentThread().getId());
		if (mStoreManagerStatus != StoreManagerStatus.SPEED_LEVEL1 && mStoreManagerStatus != StoreManagerStatus.SPEED_LEVEL2
				&& mStoreManagerStatus != StoreManagerStatus.SPEED_LEVEL3) {
			// 首次运行, 主动检测UsbDisk状态
			if (mStoreManagerStatus == StoreManagerStatus.NONE) {
				setStoreManagerStatus(StoreManagerStatus.SPEED_LEVEL1);
				UsbDiskUtil.initUsbDiskMounted(context);
			}
			// 注册接收UDisk设备挂载与御载通知
			registerBroadcastEvents(mContext.get());

			// 注册开机完成（即点击了警告按钮）的监听
			mContext.get().getContentResolver().registerContentObserver(Settings.System.CONTENT_URI, true, bootCompleteResolver);
			/* 执行处理逻辑 */
			startStoreTask();
		}
	}

	@Override
	public StoreManagerStatus getStoreManagerStatus() {
		return mStoreManagerStatus;
	}

	@Override
	public IMedatataRetriever getMetadataRetriever() {
		return mMedatataRetriever;
	}

	@Override
	public boolean isUsbMounted(String rootPath) {
		return UsbDiskUtil.isMounted(rootPath);
	}

	@Override
	public boolean isFileScanFinished(String rootPath) {
		return UsbDiskUtil.isScanFinished(rootPath);
	}

	@Override
	public boolean isStarted() {
		return mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL1 || mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL2
				|| mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL3;
	}

	@Override
	public MediaItem getMetadata(String filePath) {
		MediaItem mediaItem = mUsbMediaDB.queryMediaItem(filePath);
		if (mediaItem != null && mediaItem.getParseStatus() == ParseStatus.NONE) {
			addPriorityMedaiItem(mediaItem);
		}
		return mediaItem;
	}

	@Override
	public MediaType getStoreMediaType() {
		return mStoreMediaType;
	}

	@Override
	public UsbMediaDB getUsbMediaDB() {
		return mUsbMediaDB;
	}

	@Override
	public boolean removeMediaFile(String path) {
		boolean blnRet = false;
		File mediaFile = new File(path);
		ArrayList<MediaItem> mediaItems = new ArrayList<MediaItem>();
		MediaItem item = mUsbMediaDB.getMediaItemByFilePath(path);
		if (mediaFile.delete()) {
			mUsbMediaDB.deleteMediaItem(item);
			blnRet = true;
			mediaItems.add(item);
			notifyMediaFilesRemoved(UsbDiskUtil.getRootPath(item.getFilePath()), mediaItems);
		}
		return blnRet;
	}

	/**
	 * 将指定媒体添加到高优先级Metata解析列表中
	 * 
	 * @param mediaItem
	 *            媒体对象
	 */
	private void addPriorityMedaiItem(MediaItem mediaItem) {
		boolean blnHave = false;

		if (mediaItem != null && mediaItem.getParseStatus() == ParseStatus.NONE) {
			try {
				// 检查当前媒体是否已经在高优先级Metata解析列表中
				for (MediaItem item : mPriorityMedaiItems) {
					if (item.getFilePath().equals(mediaItem.getFilePath())) {
						blnHave = true;
						break;
					}
				}
				if (!blnHave) {
					// 最多保存15条优先解析的媒体
					if (mPriorityMedaiItems.size() > 15) {
						mPriorityMedaiItems.remove(0);
					}
					mPriorityMedaiItems.add(mediaItem);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 完成初始化相关工作
	 * @param context 上下文
	 */
	private void initMediaStore(Context context) {
		L.d("initMediaStore");
		/* 初始化局变量 */
		mStoreManagerStatus = StoreManagerStatus.NONE;
		mUsbMediaDB = UsbMediaDB.getUsbMediaDB(context);
		mAndroidMediaStoreUtil = new AndroidMediaStoreUtil(mContext.get());
		mMediaStoreChangeListeners = new CopyOnWriteArrayList<IMediaStoreChangeListener>();
		mPriorityMedaiItems = new CopyOnWriteArrayList<MediaItem>();
		mMedatataRetriever = new MedatataRetriever();
		int bootComplete = Settings.System.getInt(mContext.get().getContentResolver(), "system_warning_click", 0);
		if (bootComplete == 1) {
			isBootComplete = true;
		}
	}

	/**
	 * 注册接收UDisk设备挂载与御载通知
	 * @param context 上下文
	 */
	private void registerBroadcastEvents(Context context) {
		mUsbEventReceiver = new UsbDiskReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_EJECT);
		//媒体文件发生变化
		filter.addAction(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		filter.addDataScheme("file");
		context.registerReceiver(mUsbEventReceiver, filter);
	}

	/**
	 * 反注册接收UDisk设备挂载与御载通知
	 * @param context 上下文
	 */
	private void unregisterBroadcastEvents(Context context) {
		if (mUsbEventReceiver != null) {
			context.unregisterReceiver(mUsbEventReceiver);
			mUsbEventReceiver = null;
		}
	}

	/**
	 * 发送有媒体文件添加通知
	 * @param items 媒体对象列表
	 */
	private synchronized void notifyMediaFilesAdded(String rootPath, MediaItems[] temp) {
		if (temp == null) {
			L.d("notifyMediaFilesAdded() dispatchMediaItem : temp == null");
			return;
		} else {
			L.d("AAAA", "--- ScanWaitTime ---" + getScanWaitTime());
			L.d("AAAA", "notifyMediaFilesAdded AUDIO:" + temp[0].size() + " IMAGE:" + temp[1].size() + " VIDEO:" + temp[2].size());
		}
		for (IMediaStoreChangeListener listener : mMediaStoreChangeListeners) {
			if (listener != null && UsbDiskUtil.isMounted(rootPath)) {
				try {
					if (temp[0].getType() == MediaType.AUDIO && temp[0].size() > 0) {
						listener.onAudioFilesAdded(temp[0]);
					}
					if (temp[1].getType() == MediaType.IMAGE && temp[1].size() > 0) {
						listener.onImageFilesAdded(temp[1]);
					}
					if (temp[2].getType() == MediaType.VIDEO && temp[2].size() > 0) {
						listener.onVideoFilesAdded(temp[2]);
					}
					if (temp[3].getType() == MediaType.OFFICE && temp[3].size() > 0) {
						listener.onOfficeFilesAdded(temp[3]);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				L.d("notifyMediaFilesAdded()  UsbDiskUtil.isMounted(rootPath)：false! rootPath:" + rootPath);
			}
		}
	}

	/**
	 * 发送有媒体文件移除通知
	 * @param items 媒体对象列表
	 */
	private void notifyMediaFilesRemoved(String rootPath, List<MediaItem> items) {
		for (IMediaStoreChangeListener listener : mMediaStoreChangeListeners) {
			if (listener != null && UsbDiskUtil.isMounted(rootPath)) {
				try {
					listener.onMediaFilesRemoved(items);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 发送清空媒体通知
	 */
	private void notifyMediaFilesCleared(String root) {
		for (IMediaStoreChangeListener listener : mMediaStoreChangeListeners) {
			if (listener != null) {
				try {
					listener.onMediaFilesCleared(root);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 发送UDisk媒体文件扫描完成通知
	 */
	private void notifyFileScanFinished(String root) {
		UsbDiskUtil.setScanFinished(root);
		removeScanTask(root);
		for (IMediaStoreChangeListener listener : mMediaStoreChangeListeners) {
			if (listener != null) {
				try {
					listener.onFileScanFinished(root);
				} catch (Exception e) {
					Log.i("info", "--- UsbMediaStore_notifyFileScanFinished ---" + e.toString());
					// e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 发送UDisk设备挂载通知
	 */
	private void notifyOnUsbDiskMounted(String root) {
		Log.i("info", "--- notifyOnUsbDiskMounted ---");
		for (IMediaStoreChangeListener listener : mMediaStoreChangeListeners) {
			if (listener != null) {
				try {
					if (listener instanceof UsbServiceManager) {
						listener.onUsbDiskMounted(root);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 发送UDisk设备卸载通知
	 */
	private void notifyOnUsbDiskUnMounted(String root) {
		L.d("usbstate", "=====================notifyOnUsbDiskUnMounted=====================");
		for (IMediaStoreChangeListener listener : mMediaStoreChangeListeners) {
			if (listener != null) {
				try {
					if (listener instanceof UsbServiceManager) {
						listener.onUsbDiskUnMounted(root);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 开启一个扫描线程
	 */
	private void startScanTask(String path) {
		L.d("AAAA", "startScanTask :" + path);
		ScanTask workTask = new ScanTask(path);
		workTask.future = ThreadPoolManager.getInstance().execute(workTask);
		mScanUsbMediaTasks.add(workTask);
	}
	
	/**
	 * 从线程队列内获取指定路径扫描线程
	 * @param path
	 * @return
	 */
	private ScanTask getScanTask(String path){
		ScanTask temp = null;
		for (int i = 0; i < mScanUsbMediaTasks.size(); i++) {
			temp = mScanUsbMediaTasks.get(i);
			if (path.contains(temp.getRootPath())) {
				break;
			}
			temp = null;
		}
		return temp;
	}

	/**
	 * 从线程队列中移除
	 * 
	 * @author:
	 * @createTime: 2016-11-30 下午2:40:22
	 * @history:
	 * @param path
	 *            void
	 */
	private void removeScanTask(String path) {
		ScanTask temp = getScanTask(path);
		if (temp != null) {
			temp.setTaskStop();
			cancelAsyncTask(temp.future);
			temp.cancelled();
			mScanUsbMediaTasks.remove(temp);
		}
	}

	/**
	 * 停止一个扫描线程
	 */
	private void stopScanTask(String path) {
		synchronized (lock) {
			removeScanTask(path);
			if (mScanMediaType == MediaType.ALL) {
				L.d("==================================updateUsbMediaDeleteFlag===============================");
				mUsbMediaDB.updateUsbMediaDeleteFlag(path, MediaType.AUDIO, true);
				mUsbMediaDB.updateUsbMediaDeleteFlag(path, MediaType.IMAGE, true);
				mUsbMediaDB.updateUsbMediaDeleteFlag(path, MediaType.VIDEO, true);
				mUsbMediaDB.updateUsbMediaDeleteFlag(path, MediaType.OFFICE, true);
				L.d("===============================updateUsbMediaDeleteFlag END==================================");
			} else {
				mUsbMediaDB.updateUsbMediaDeleteFlag(path, mScanMediaType, true);
			}
			// 发送清空媒体通知
			notifyMediaFilesCleared(path);
		}
	}

	private Object lock = new Object();

	/**
	 * 开启所有相关Task
	 */
	private void startStoreTask() {
		synchronized (lock) {
			L.d("startStoreTask() Run....");
			// 开启[完成UDisk设备媒体文件扫描并加入到UsbMediaDB中]Task
			if (mScanUsbMediaTasks != null) {
				mScanUsbMediaTasks.clear();
			}
			for (UsbDevice device : Configuration.DEVICE.values()) {
				if (device.isMount() && device.getScanState() == UsbDeviceScanState.NONE) {
					startScanTask(device.getRootPath());
				}
			}
			// 开启[完成对插入到UsbMediaDB中未进行Metadata解析的媒体进行Metadata解析]Task
			if (mObtainMediaInfoTask == null) {
				mObtainMediaInfoTask = new ObtainMediaInfoTask("thread_parse");
				addMediaStoreChangeListener(mObtainMediaInfoTask);
				mObtainMediaInfoTask.future = ThreadPoolManager.getInstance().execute(mObtainMediaInfoTask);

				// mObtainMediaInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
			} else {
				Log.w(TAG, "mObtainMediaInfoAsyncTask != null");
			}
			L.d("startStoreTask() Done.");
		}
	}

	/**
	 * 清理所有相关Task
	 */
	private void releaseStoreTask() {
		L.d("====================releaseStoreTask=======================");
		// 取消[完成对插入到UsbMediaDB中未进行Metadata解析的媒体进行Metadata解析]Task中的监听
		removeMediaStoreChangeListener(mObtainMediaInfoTask);
		// 停止[完成对插入到UsbMediaDB中未进行Metadata解析的媒体进行Metadata解析]Task
		cancelAsyncTask(mObtainMediaInfoTask.future);
		mObtainMediaInfoTask.cancelled();
		// 停止[完成UDisk设备媒体文件扫描并加入到UsbMediaDB中]Task
		for (int i = 0; i < mScanUsbMediaTasks.size(); i++) {
			cancelAsyncTask(mScanUsbMediaTasks.get(i).future);
			mScanUsbMediaTasks.get(i).cancelled();
		}
		mScanUsbMediaTasks = null;
		mObtainMediaInfoTask = null;
	}

	/**
	 * 取消指定线程的运行
	 * 
	 * @param future
	 *            
	 */
	private void cancelAsyncTask(Future<?> future) {
		if (future != null) {
			future.cancel(true);
		}
	}

	/**
	 * 设定UsbMediaStore运行状态
	 * 
	 * @param status StoreManagerStatus {NONE, START, PAUSE, STOP}
	 */
	private void setStoreManagerStatus(StoreManagerStatus status) {
		mStoreManagerStatus = status;
	}

	/**
	 * 接收UDisk设备挂载与御载通知类
	 */
	private class UsbDiskReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			String dataString = intent.getDataString();
			Log.i("info", "--- UsbDiskReceiver_action ---" + action);
			L.i("===dataString====" + dataString.toString());
			// file:///storage/00B1-6906
			if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
				notifyOnUsbDiskMounted(dataString);
				// 通知systemUI显示USB图标
				if (dataString != null) {
					String randomUsbPath = dataString + "--" + Math.random() + "";
					Settings.System.putString(context.getContentResolver(), "system_usb_mount", randomUsbPath);
				}
				if (dataString.contains(Configuration.getOuterSdRootPath())) {
					L.d("外置SD挂载 dataString:" + dataString);
					// 外置SD挂载
					UsbDiskUtil.setMounted(Configuration.getOuterSdRootPath());
					if (UsbDiskUtil.isScanNone(Configuration.getOuterSdRootPath())) {
						// 开启线程扫描
						startScanTask(Configuration.getOuterSdRootPath());
						UsbDiskUtil.setScaning(Configuration.getOuterSdRootPath());
					}
				} else if (dataString.contains(Configuration.getUsbMediaRootPath())) {
					L.d("U盘挂载 dataString:" + dataString);
					// U盘挂载
					UsbDiskUtil.setMounted(Configuration.getUsbMediaRootPath());
					if (UsbDiskUtil.isScanNone(Configuration.getUsbMediaRootPath())
							|| UsbDiskUtil.isScanFinished(Configuration.getUsbMediaRootPath())) {
						// 开启线程扫描
						startScanTask(Configuration.getUsbMediaRootPath());
						UsbDiskUtil.setScaning(Configuration.getUsbMediaRootPath());
					}
				} else {
					dataString = dataString.replace("file://", "").trim();
					for (String str : Configuration.UNSCAN_DEVICE) {
						if (dataString != null && dataString.contains(str)) {
							return;
						}
					}

					Configuration.UDISK_NUMBER = dataString;
					UsbDiskUtil.setMounted(dataString);
					if (UsbDiskUtil.isScanNone(dataString)) {
						// 开启线程扫描
						Log.i("info", " --- UsbMediaStore_UsbDiskReceiver_path --- " + dataString);

						startScanTask(dataString);
						UsbDiskUtil.setScaning(dataString);
					}
				}
			} else if (Intent.ACTION_MEDIA_EJECT.equals(action)) {
				notifyOnUsbDiskUnMounted(dataString);
				// 通知systemUI显示USB图标
				Settings.System.putString(context.getContentResolver(), "system_usb_mount", "MEDIA_EJECT");
				if (dataString.contains(Configuration.getOuterSdRootPath())) {
					// 外置SD卸载

					UsbDiskUtil.setUnMounted(Configuration.getOuterSdRootPath());
					stopScanTask(Configuration.getOuterSdRootPath());
					UsbDiskUtil.setScanNone(Configuration.getOuterSdRootPath());
				} else if (dataString.contains(Configuration.getUsbMediaRootPath())) {
					// U盘卸载
					UsbDiskUtil.setUnMounted(Configuration.getUsbMediaRootPath());
					stopScanTask(Configuration.getUsbMediaRootPath());
					UsbDiskUtil.setScanNone(Configuration.getUsbMediaRootPath());
				} else {
					dataString = dataString.replace("file://", "").trim();
					UsbDiskUtil.setUnMounted(dataString);
					stopScanTask(dataString);
					UsbDiskUtil.setScanNone(dataString);
				}

				L.d("test", "ActiveCount_1: " + ThreadPoolManager.getInstance().getActiveCount());
			}else if(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE.equals(action)){
				dataString = dataString.replace("file://", "").trim();
				File file = new File(dataString);
				L.d("usbdisk", "======ACTION_MEDIA_SCANNER_SCAN_FILE file======" + file);
				if(file == null || !file.isDirectory()){
					return;
				}
				ScanTask scanTask = getScanTask(dataString);
				L.d("usbdisk", "======ACTION_MEDIA_SCANNER_SCAN_FILE===scanTask===" + scanTask);
				if(scanTask == null){
					L.d("usbdisk", "======ACTION_MEDIA_SCANNER_SCAN_FILE===startScanTask===");
					startScanTask(dataString);
				}
			}
		}
	}

	/**
	 * 异步扫描UDisk目录下的所有媒体文件，同时将需要管理的媒体文件添加 到媒体对应的全局List<MediaItem>中。
	 */

	public class ScanTask implements Runnable {
		private String threadName;
		private Future<?> future;
		private boolean isStop = true;
		private int mTotalAudioCount = 0;
		private int mTotalImgCount = 0;
		private int mTotalVideoCount = 0;

		/**
		 * CPU占用控制
		 */
		private Object mScanRunWait = new Object();

		/**
		 * 扫描到多少个媒体文件发送一次通报
		 */
		private final int MAX_MEDIA_CHANGE_NOTIFY_COUNT = 30;

		/**
		 * 扫描文件两次通报之间允许的最小时间间隔毫秒
		 */
		private final long MIN_MEDIA_CHANGE_NOTIFY_TIME = 2000;

		/**
		 * 记录扫描到的媒体文件总数
		 */
		private int mScanFileCount = 0;

		/**
		 * 记录上次发送onMediaFilesAdded通知时间
		 */
		private long mLastNotifyTime = 0;

		/**
		 * FilenameFilter实现类，用于媒体文件过滤及入DB处理
		 */
		private MediaAutoFillFilter mMediaAutoFillFilter = new MediaAutoFillFilter();

		/**
		 * 保存扫描到的媒体用于向传递到onMediaFilesAdded通知中，同时为了减少DB操作提高性能
		 */
		private CopyOnWriteArrayList<MediaItem> mMediaItems;
		/**
		 * 由于快速扫描的机制，会导致重复推送到UI层
		 */
		private CopyOnWriteArrayList<MediaItem> mFastMediaItems;

		private String rootPath = null;

		public String getRootPath() {
			return rootPath;
		}

		public void setTaskStop() {
			this.isStop = false;
		}

		public ScanTask(String rootPath) {
			this.threadName = rootPath;
			this.rootPath = rootPath;
		}

		@Override
		public void run() {
			Thread.currentThread().setName(threadName);
			L.i("Thread_Name: " + Thread.currentThread().getName() + "");
			L.i("ScanUsbMediaAsyncTask doInBackground()");
			try {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_LESS_FAVORABLE);
				mMediaItems = new CopyOnWriteArrayList<MediaItem>();
				mFastMediaItems = new CopyOnWriteArrayList<MediaItem>();
				// 将DB中所有数据设为伪删除
				if (mScanMediaType == MediaType.ALL) {
					mUsbMediaDB.updateUsbMediaDeleteFlag(getRootPath(), MediaType.AUDIO, true);
					mUsbMediaDB.updateUsbMediaDeleteFlag(getRootPath(), MediaType.IMAGE, true);
					mUsbMediaDB.updateUsbMediaDeleteFlag(getRootPath(), MediaType.VIDEO, true);
					mUsbMediaDB.updateUsbMediaDeleteFlag(getRootPath(), MediaType.OFFICE, true);
				} else {
					mUsbMediaDB.updateUsbMediaDeleteFlag(getRootPath(), mScanMediaType, true);
				}

				scanLastPath(mStoreMediaType);
				long time = System.currentTimeMillis();
				L.d("AAAA", "--------------------BEGIN TIME ----------------------mStoreMediaType:" + mStoreMediaType);
				// 开始扫描上一次扫描记录的最多文件的目录
				SortUtil sort = new SortUtil();

				CopyOnWriteArrayList<MFile> files;
				try {
					files = sort.pathSort(getRootPath(), mScanMediaType, 3);
				} catch (NullPointerException e) {
					Log.i("info3", " --- ScanUsbMediaAsyncTask doInBackground NullPointerException --- ");
					files = sort.pathSort(getRootPath(), mScanMediaType, 3);
				}

				L.d("AAAA", "--------------------SORT TIME ----------------files size------" + files.size());
				boolean flag = true;
				while (flag && isStop) {
					int index = findNextFile(files, mStoreMediaType);
					// L.d("-------------------->>index:" + index);
					if (index == -1) {
						flag = false;
						L.d("--------------------no find!-------------------" + " mStoreMediaType:" + mStoreMediaType.toString());
						// 如果为-1说明找不到要扫描的文件了
					} else {
						MFile f = files.get(index);
						f.setScaned(true);
						files.remove(index);
						if (f.isDirectory()) {
							scanDirectory(f.getAbsolutePath());
						} else {
							scanFile(f, getScanWaitTime(), false);
						}
					}
					if (future.isCancelled()){
						break;
					}
				}

				L.d("--------------------SCAN TIME ---------------------time:" + (System.currentTimeMillis() - time));
				L.d("--------------------scan finish-------------------- Scan total FileCount:" + mScanFileCount);
				if (!future.isCancelled()) {
					// 删除媒体表中的伪删除数据
					if (mScanMediaType == MediaType.ALL) {
						deletePseudoMediaItems(getRootPath(), MediaType.AUDIO);
						deletePseudoMediaItems(getRootPath(), MediaType.IMAGE);
						deletePseudoMediaItems(getRootPath(), MediaType.VIDEO);
					} else {
						deletePseudoMediaItems(getRootPath(), mScanMediaType);
					}
				}
			} catch (Exception e) {
				L.e("ScanUsbMediaAsyncTask doInBackground() + Exception:" + e.toString());
				// e.printStackTrace();
			}

			// 扫描完成唤醒解析线程
			synchronized (mObtainMediaInfoTask.getAddFileNotify()) {
				// Log.i("info", "--- UsbMediaStore_AddFileNotify ---");
				mObtainMediaInfoTask.getAddFileNotify().notifyAll();
			}

			postExecute();
			// return mScanFileCount;
		}

		protected void cancelled() {
			L.d("ScanUsbMediaAsyncTask onCancelled()");
			// 结束的wait
			synchronized (mScanRunWait) {
				mScanRunWait.notifyAll();
			}
		}

		protected void postExecute() {
			// 通知文件扫描完成
			if (mMediaItems.size() > 0) {
				mUsbMediaDB.replaceMediaItems(mMediaItems);
				// 发送有媒体文件添加通知
				mProgressUpdate(mMediaItems);
				mMediaItems.clear();
			}
			writePathsToUsbDisk();
			mFastMediaItems.clear();
			notifyFileScanFinished(getRootPath());
		}

		/**
		 * 直接搜索上一次统计文件最多的目录，每个类型最多出来20条，加快速度出数据， 因为扫描时有加去重处理所以不比担心重复问题
		 * 
		 * @author:
		 * @createTime: 2016-9-30 上午11:05:12
		 * @history:
		 * @param mediaType
		 *            void
		 */
		private void scanLastPath(MediaType mediaType) {
			String[] paths = new String[] { null, null, null };
			// L.d("--- scanLastPath_paths ---" + paths.toString());
			try {
				paths[0] = UsbDiskUtil.readFileFromUsbDisk(".audio.cfg");
				L.d("--- scanLastPath_paths[0] ---" + paths[0]);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				paths[1] = UsbDiskUtil.readFileFromUsbDisk(".video.cfg");
				L.d("--- scanLastPath_paths[1] ---" + paths[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				paths[2] = UsbDiskUtil.readFileFromUsbDisk(".image.cfg");
				L.d("--- scanLastPath_paths[2] ---" + paths[0]);
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (mediaType == MediaType.AUDIO) {
				scanLastPath(paths[0], MediaType.AUDIO);
				scanLastPath(paths[1], MediaType.VIDEO);
				scanLastPath(paths[2], MediaType.IMAGE);
			} else if (mediaType == MediaType.VIDEO) {
				scanLastPath(paths[1], MediaType.VIDEO);
				scanLastPath(paths[0], MediaType.AUDIO);
				scanLastPath(paths[2], MediaType.IMAGE);
			} else if (mediaType == MediaType.IMAGE) {
				scanLastPath(paths[2], MediaType.IMAGE);
				scanLastPath(paths[1], MediaType.VIDEO);
				scanLastPath(paths[0], MediaType.AUDIO);
			}
		}

		private void scanLastPath(String path, MediaType mediaType) {
			if (path != null) {
				File f = new File(path);
				if (f.exists() && f.isDirectory()) {
					File[] files = f.listFiles();
					int count = 0;
					for (int i = 0; i < files.length && count < 10; i++) {
						File file = files[i];
						if (file.isFile()) {
							MediaType temp = MediaFile.getFileType(file.getAbsolutePath());
							if (temp == mediaType) {
								if (scanFile(files[i], getScanWaitTime(), true)) {
									count++;
								}
							}
						}
					}
				}
			}
		}

		/**
		 * 寻找下一个扫描的文件地址
		 * 
		 * @author:
		 * @createTime: 2016-9-30 上午10:36:24
		 * @history:
		 * @param files
		 * @param mStoreMediaType
		 * @return int
		 */
		private int findNextFile(CopyOnWriteArrayList<MFile> files, MediaType mStoreMediaType) {
			int index = -1;
			// L.d("files size::" + files.size());
			MediaType type = mStoreMediaType;
			// 是否是第一次遍历完成，且没有找到相应类型
			boolean findOther = false;
			MFile f = null;
			for (int i = files.size() - 1; i >= 0; i--) {
				f = files.get(i);
				if (!findOther && f.getMediaType() == type) {
					index = i;
					if (f.isScaned()) {
						// L.d("files isScaned: i:" + f.getAbsolutePath());
						continue;
					}
					return index;
				}
				// 遍历完一遍了都没有找到想要的类型
				if (!findOther && i == 0) {
					findOther = true;
					i = files.size();
				}
				if (findOther && i < files.size()) {
					// 任意类型都OK
					index = i;
					if (f.isScaned()) {
						continue;
					}
					return index;
				}
			}
			return index;
		}

		private synchronized void mProgressUpdate(CopyOnWriteArrayList<MediaItem> values) {
			// L.d("mProgressUpdate--" + values.size());
			// 记录上次发送onMediaFilesAdded通知时间
			mLastNotifyTime = System.currentTimeMillis();
			// 发送有媒体文件添加通知
			MediaItems[] temp = dispatchMediaItem(values);
			notifyMediaFilesAdded(getRootPath(), temp);
			// 再次记录上次发送onMediaFilesAdded通知时间，防止接收实现类中处理时间过长，通知频繁
			mLastNotifyTime = System.currentTimeMillis();
		}

		/**
		 * 扫描指定目录下的媒体
		 * 
		 * @param path
		 *            要扫描的目录
		 */
		private void scanDirectory(String path) {
			// Log.i(TAG, "ScanUsbMediaAsyncTask:scanDirectory " + path);
			File dirPath = new File(path);
			dirPath.list(mMediaAutoFillFilter);
		}

		/**
		 * 扫描指定文件，如果为需要管理的媒体文件自动加入到对应的全局List<MediaItem>中，
		 * 同时调用AsyncTask.publishProgress()函数更新进度。 并定时定量的将数据保存到数据库
		 * 
		 * @param file
		 *            文件
		 */
		private boolean scanFile(File file, int cupSleepTime, boolean isFast) {

			MediaItem mediaItem = null;
			MediaType mediaType = MediaFile.getFileType(file);
			// L.d("scanFile:" + file.getAbsolutePath());
			if (mediaType == MediaType.UNKNOWN || file.isHidden() || !file.canRead()) {
				return false;
			}
			L.d(" --- getAbsolutePath --- " + file.getAbsolutePath());
			// 如果媒体扫描类型是ALL或者是当前文件是指定扫描类型
			if ((mediaType == mScanMediaType || mScanMediaType == MediaType.ALL)) {
				// 取得DB中已存在的当前文件信息
				mediaItem = mUsbMediaDB.queryMediaItem(file.getAbsolutePath());
				String pinyinString = CharacterParser.getInstance().getSelling(file.getName().substring(0, file.getName().lastIndexOf(".")));
				// L.d(" --- scanFile_pinyinString --- " + pinyinString);
				if (mediaItem == null) {
					// DB中不存在当前文件信息则创建新的媒体对象
					mediaItem = new MediaItem(mediaType, file.getName(), file.getAbsolutePath(), file.length(), file.lastModified(), pinyinString);
				} else {
					// 文件重名，内容不同需要重新扫描
					if (mediaItem.getLastModified() != file.lastModified() || mediaItem.getSize() != file.length()) {
						// 文件重名，内容不同需要重新扫描, 删除原有缩略图
						if (mediaItem.getMediaID() > -1) {
							mAndroidMediaStoreUtil.deleteThumbnail(mediaItem.getMediaType(), mediaItem.getMediaID());
						}
						// 创建新的对象，使用相同ID
						long mediaItemID = mediaItem.getID();
						mediaItem = new MediaItem(mediaType, file.getName(), file.getAbsolutePath(), file.length(), file.lastModified(), pinyinString);
						mediaItem.setID(mediaItemID);
					}
				}
				mediaItem.setUpdateTime(System.currentTimeMillis());
				mediaItem.setScanIndex(mScanFileCount);
				mediaItem.setDelete(false);

				// 保存扫描到的媒体
				if (isFast) {
					// 如果是快速扫描，则将对象加入快速扫描对列，以便当不是快速扫描时进行对比去重
					mFastMediaItems.add(mediaItem);
					mMediaItems.add(mediaItem);
				} else {
					if (mFastMediaItems.size() > 0) {
						int index = findTheSameItem(mediaItem, mFastMediaItems);
						if (index == -1) {
							// 在快速扫描队列没找到，通知UI层
							mMediaItems.add(mediaItem);
						} else {
							// 找到了就移除快速扫描队列的对象
							mFastMediaItems.remove(index);
						}
					} else {
						mMediaItems.add(mediaItem);
						// L.d("mMediaItems add:" + mediaItem.getFilePath());
					}
				}
				mScanFileCount = mScanFileCount + 1;

				// cpuRunWait(cupSleepTime);

				/* 判断是否需要发送有媒体文件添加通知 */
				if (!future.isCancelled()
						&& (mScanFileCount == 10 || mMediaItems.size() >= MAX_MEDIA_CHANGE_NOTIFY_COUNT || (mMediaItems.size() > 0 && System
								.currentTimeMillis() - mLastNotifyTime >= MIN_MEDIA_CHANGE_NOTIFY_TIME))) {
					/* 将当前扫描完成的数据存入DB */
					mUsbMediaDB.replaceMediaItems(mMediaItems);
					L.d("mUsbMediaDB replaceMediaItems:" + mMediaItems.size());
					// 发送有媒体文件添加通知
					mProgressUpdate(mMediaItems);
					mMediaItems.clear();
					// 统计路径
					writePathsToUsbDisk();
				}
			}
			return true;
		}

		protected MediaItems[] dispatchMediaItem(List<MediaItem> items) {
			MediaItems[] temp = new MediaItems[] { new MediaItems(MediaType.AUDIO), new MediaItems(MediaType.IMAGE), new MediaItems(MediaType.VIDEO),
					new MediaItems(MediaType.OFFICE) };
			for (int i = 0; i < items.size(); i++) {
				MediaType type = items.get(i).getMediaType();
				if (type == MediaType.AUDIO) {
					temp[0].add(items.get(i));
				} else if (type == MediaType.IMAGE) {
					temp[1].add(items.get(i));
				} else if (type == MediaType.VIDEO) {
					temp[2].add(items.get(i));
				} else if (type == MediaType.OFFICE) {
					temp[3].add(items.get(i));
				}
			}
			mTotalAudioCount += temp[0].size();
			mTotalImgCount += temp[1].size();
			mTotalVideoCount += temp[2].size();

			return temp;
		}

		int audioFileLen;
		int videoFileLen;
		int imageFileLen;

		/**
		 * 将可疑路径写入U盘
		 * 
		 * @author:
		 * @createTime: 2016-9-30 下午3:11:16
		 * @history: void
		 */
		public void writePathsToUsbDisk() {
			if (!UsbDiskUtil.isMounted(getRootPath()))
				return;
			String[] tempPaths = mUsbMediaDB.getMaxSearchPaths();
			try {
				if (tempPaths[0] != null) {
					audioFileLen = UsbDiskUtil.getFileLen(".audio.cfg", Configuration.UDISK_NUMBER);
					if (audioFileLen > 1024) {
						UsbDiskUtil.writeFileToUsbDisk(".audio.cfg", tempPaths[0], true);
					}
					UsbDiskUtil.writeFileToUsbDisk(".audio.cfg", tempPaths[0], false);
				}
			} catch (Exception e) {
			}
			try {
				if (tempPaths[1] != null) {
					videoFileLen = UsbDiskUtil.getFileLen(".video.cfg", Configuration.UDISK_NUMBER);
					if (videoFileLen > 1024) {
						UsbDiskUtil.writeFileToUsbDisk(".video.cfg", tempPaths[1], true);
					}
					UsbDiskUtil.writeFileToUsbDisk(".video.cfg", tempPaths[1], false);
				}
			} catch (Exception e) {
			}
			try {
				if (tempPaths[2] != null) {
					imageFileLen = UsbDiskUtil.getFileLen(".image.cfg", Configuration.UDISK_NUMBER);
					if (imageFileLen > 1024) {
						UsbDiskUtil.writeFileToUsbDisk(".image.cfg", tempPaths[2], true);
					}
					UsbDiskUtil.writeFileToUsbDisk(".image.cfg", tempPaths[2], false);
				}
			} catch (Exception e) {
			}
		}

		private int findTheSameItem(MediaItem item, CopyOnWriteArrayList<MediaItem> itmes) {
			for (int i = 0; i < itmes.size(); i++) {
				if (item.getFilePath().equals(itmes.get(i).getFilePath())) {
					return i;
				}
			}
			return -1;
		}

		/**
		 * 删除UsbMediaDB中的伪删除数据及对应的系统媒体库中的内容
		 */
		private void deletePseudoMediaItems(String rootPath, MediaType mScanMediaType) {
			final int count = 30;
			Cursor cursor = null;
			List<MediaItem> medisItems = null;
			int startIndex = 0;
			while (!future.isCancelled()) {
				cursor = mUsbMediaDB.queryAllMedia(rootPath, mScanMediaType, startIndex, count, true);
				if (cursor != null && cursor.getCount() > 0) {
					medisItems = mUsbMediaDB.cursorToMediaItems(cursor);
					cursor.close();
				}
				if (medisItems == null || medisItems.size() == 0) {
					break;
				}
				startIndex = startIndex + count;
				for (MediaItem mediaItem : medisItems) {
					if (mediaItem.getMediaID() > -1) {
						mAndroidMediaStoreUtil.deleteMediaByID(mediaItem.getMediaType(), Long.toString(mediaItem.getMediaID()));
					}
				}
				medisItems.clear();
			}
			mUsbMediaDB.deletePseudoMediaItems(mScanMediaType);
		}

		private byte getMultiples() {
			if (mStoreMediaType == MediaType.AUDIO) {
				if (mTotalAudioCount > 50 && mTotalAudioCount < 200) {
					return 2;
				} else if (mTotalAudioCount > 200) {
					return 3;
				}
			}
			if (mStoreMediaType == MediaType.IMAGE) {
				if (mTotalImgCount > 50 && mTotalImgCount < 200) {
					return 2;
				} else if (mTotalImgCount > 200) {
					return 3;
				}
			}
			return 1;
		}

		/**
		 * 降CPU占用
		 * 
		 * @param millis
		 *            等多少毫秒
		 */
		private void cpuRunWait(long millis) {
			millis *= getMultiples();
			// Log.i("info", " --- UsbMediaStore_cpuRunWait_scan --- " + millis);
			if (!future.isCancelled()) {
				// 降低CUP占用
				synchronized (mScanRunWait) {
					try {
						// L.d("cpu", "" + getProcessCpuRate());
						mScanRunWait.wait(millis);
					} catch (InterruptedException e) {
					}
				}
			}
		}

		/**
		 * 提供目录及文件过虑功能，同时完成目录的递归及媒体文件的发现管理
		 */
		private class MediaAutoFillFilter implements FilenameFilter {

			@Override
			public boolean accept(File dir, String filename) {
				File file = new File(dir.getAbsolutePath() + File.separator + filename);
				if (!future.isCancelled() && !file.isHidden() && file.canRead()) {
					if (file.isFile()) {
						scanFile(file, getScanWaitTime(), false);
					} else if (file.isDirectory() && !Configuration.isWindowsRecycler(file.getPath())) {
						scanDirectory(file.getAbsolutePath());
					}
					// 降低CUP占用
					cpuRunWait(getScanWaitTime());
				}
				return false;
			}
		}
	}

	/**
	 * 完成对插入到UsbMediaDB中未进行Metadata解析的媒体进行Metadata解析操作Task
	 */

	public class ObtainMediaInfoTask implements Runnable, IMediaStoreChangeListener {

		private String threadName;
		private boolean isCancelled;
		private Future<?> future;

		public ObtainMediaInfoTask(String rootPath) {
			this.threadName = rootPath;
		}

		/**
		 * 等待媒体媒体类连接建立完成及一个媒体文件Metadata解析完成
		 */
		private Object mMetadataRunWait = new Object();

		/**
		 * 等待ScanUsbMediaAsyncTask中有新媒体被扫描到，解析动作恢复
		 */
		private Object mAddFileNotify = new Object();

		/**
		 * 媒体Metadata解析完成的总数 (本次：U盘插拔一次解析的文件)
		 */
		private int mObtainMediaInfoCount = 0;

		// boolean isMetadataGet = false ;

		/**
		 * 查询媒体Metadata类
		 */
		private QueryMetadata mQueryMetadata;

		/**
		 * 记录当前解析媒体对象
		 */
		private MediaItem mCurrentScanMediaItem;

		/**
		 * 记录当前解析媒体对象对应系统媒体库的ＵＲＩ
		 */
		private Uri mCurrentScanMediaItemUri;

		/**
		 * 记录当前解析媒体是否为优先级媒体
		 */
		private boolean mIsPriorityMediaItem;

		/**
		 * 保存应用请求删除的媒体对象,防止解析过程中多解析多通报问题
		 */
		private ConcurrentHashMap<String, MediaItem> mDeleteMediaItems;

		@Override
		public void run() {

			Log.i("AAAA", "ObtainMediaInfoAsyncTask doInBackground() ThreadID: " + Thread.currentThread().getId());
			try {
				Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_LESS_FAVORABLE);
				/* 变量初始化 */
				ContentResolver contentResolver = mContext.get().getContentResolver();
				mQueryMetadata = new QueryMetadata();
				mDeleteMediaItems = new ConcurrentHashMap<String, MediaItem>();

				ClientProxy client = new ClientProxy();
				MediaScannerConnection connection = new MediaScannerConnection(mContext.get(), client);
				connection.connect();

				// 等待MediaScannerConnection连接完成
				while (!connection.isConnected() && !future.isCancelled()) {
					 Log.d("AAAA", "----connection.isConnected----");
					synchronized (mMetadataRunWait) {
						try {
							mMetadataRunWait.wait(200);
						} catch (InterruptedException e) {
						}
					}
				}

				/* 开始循环解析媒体 */
				while (!future.isCancelled()) {
					// 取得要解析的媒体对象
					mCurrentScanMediaItem = getNextMediaItem();
					// 降CPU占用
					 Log.d("AAAA", "mCurrentScanMediaItem:"+mCurrentScanMediaItem.getFilePath());
					cpuRunWait(getPraseWaitTime());

					if (!future.isCancelled()) {
						// 设定媒体解析状态为解析中
						mCurrentScanMediaItem.setParseStatus(ParseStatus.PARSING);
						mCurrentScanMediaItemUri = null;
						String pathTemp = UsbDiskUtil.getRootPath(mCurrentScanMediaItem.getFilePath());
						if (UsbDiskUtil.isMounted(pathTemp)) {// 为了防止USB或者SD卡拔出之后还进行没有必要的解析
							connection.scanFile(mCurrentScanMediaItem.getFilePath(), null);
						} else {
							mPriorityMedaiItems.remove(mCurrentScanMediaItem);
							mUsbMediaDB.deleteMediaItem(mCurrentScanMediaItem);
							continue;
						}

						// 等待文件解析处理完成
						synchronized (mMetadataRunWait) {
							try {
								mMetadataRunWait.wait();
							} catch (InterruptedException e) {
							}
						}
					}

					// 降CPU占用
					cpuRunWait(getPraseWaitTime());

					if (!future.isCancelled()) {
						/* 完成媒体Metadata信息取得及设定处理 */
						MediaType mediaType = mCurrentScanMediaItem.getMediaType();
						// 检索媒体解析是否正常
						if (mCurrentScanMediaItemUri == null) {
							// 系统媒体库处理异常，则更新当前媒体解析状态为异常
							mCurrentScanMediaItem.setParseStatus(ParseStatus.PARSE_FAILED);
						} else {
							// 系统媒体库处理正常则更新当前媒体对应的Metadata等信息
							if (mediaType == MediaType.AUDIO) {
								mQueryMetadata.queryAudio(contentResolver, mCurrentScanMediaItem, mCurrentScanMediaItemUri);
							} else if (mediaType == MediaType.IMAGE) {
								mQueryMetadata.queryImage(contentResolver, mCurrentScanMediaItem, mCurrentScanMediaItemUri);
							} else if (mediaType == MediaType.VIDEO) {
								mQueryMetadata.queryVideo(contentResolver, mCurrentScanMediaItem, mCurrentScanMediaItemUri);
							}
							mCurrentScanMediaItem.setParseStatus(ParseStatus.FINISHED);
						}
					}
					// 降CPU占用
					cpuRunWait(getPraseWaitTime());

					if (!future.isCancelled()) {
						// 当解析完成媒体已经被删除时不发送通知处理等
						if (mDeleteMediaItems.containsKey(mCurrentScanMediaItem.getFilePath())) {
							mDeleteMediaItems.remove(mCurrentScanMediaItem.getFilePath());
						} else {
							mUsbMediaDB.updateMediaItemMetadata(mCurrentScanMediaItem);
							// 通知Metadata信息取得完成
							if (mIsPriorityMediaItem) {
								// 如果是优先解析媒体则从优先解析队列中移除
								mPriorityMedaiItems.remove(mCurrentScanMediaItem);
								mOnProgressUpdate(new NotifyMediaItem(true, mCurrentScanMediaItem));
							} else {
								mOnProgressUpdate(new NotifyMediaItem(false, mCurrentScanMediaItem));
							}
							if (mCurrentScanMediaItem.getFilePath() == null) {
								continue;
							}
							mObtainMediaInfoCount = mObtainMediaInfoCount + 1;
						}
					}
				}
				connection.disconnect();
				L.d("AAAA", "ObtainMediaInfoAsyncTask doInBackground() + disconnect!!!!");
			} catch (Exception e) {
				L.d("AAAA", "ObtainMediaInfoAsyncTask doInBackground() + Exception>>" + e.toString());
				// e.printStackTrace();
			}
			// return mObtainMediaInfoCount;
		}

		private void mOnProgressUpdate(NotifyMediaItem... values) {
			if (values[0].isPriorityMediaItem) {
				mMedatataRetriever.notifyMetadataRetrieved(values[0].mediaItem);
			} else {
				mMedatataRetriever.notifyBackgroundMetadataRetrieved(values[0].mediaItem);
			}
			// Log.d("AAAA",  "notifyMetadataRetrieved:"+values[0].mediaItem.getName());
		}

		protected void cancelled() {
			Log.i(TAG, "ObtainMediaInfoAsyncTask onCancelled()");

			// 结束getNextMediaItem()函数中的wait
			synchronized (mAddFileNotify) {
				mAddFileNotify.notifyAll();
			}

			// 结束doInBackground()函数中的wait
			synchronized (mMetadataRunWait) {
				mMetadataRunWait.notifyAll();
			}
		}

		@Override
		public void onMediaFilesRemoved(List<MediaItem> items) {
			for (MediaItem mediaItem : items) {
				mDeleteMediaItems.put(mediaItem.getFilePath(), mediaItem);
			}
		}

		@Override
		public void onMediaFilesCleared(String root) {
			// Nothing
		}

		@Override
		public void onUsbDiskMounted(String root) {
			// Nothing
		}

		@Override
		public void onUsbDiskUnMounted(String root) {
			// Nothing
		}

		@Override
		public void onFileScanFinished(String root) {
			// 不通知解析线程，让其一直睡眠等待
		}

		public Object getAddFileNotify() {
			return mAddFileNotify;
		}

		/**
		 * 取得下一个要解析的媒体，本函数为同步函数
		 * 
		 * @return null:所有媒体都解析完成, !null:要解析的媒体
		 */
		private MediaItem getNextMediaItem() {
			// 取得高优先扫描媒体文件
			MediaItem mediaItme = findNextForPriority();

			// 取得低优先扫描媒体文件
			if (mediaItme == null) {
				mIsPriorityMediaItem = false;
				mediaItme = findNextForAllMedia();
			} else {
				mIsPriorityMediaItem = true;
			}

			// Log.i("AAAA", "--- getNextMediaItem_1 ---"+Thread.currentThread().getId());
			// 当前所有媒体文件媒体信息已取得完成，则继续等待
			if (mediaItme == null) {
				if (mObtainMediaInfoCount > 0) {
					mMedatataRetriever.notifyAllBackgroundMetadataRetrieved(mObtainMediaInfoCount, mObtainMediaInfoCount, mObtainMediaInfoCount);
					mObtainMediaInfoCount = 0;
				}
				try {
					synchronized (mAddFileNotify) {
						mAddFileNotify.wait();
					}
				} catch (InterruptedException e) {
				}

				if (!future.isCancelled()) {
					// 当文件扫描task扫描到新文件时，继续进行媒体文件详细信息取得处理
					mediaItme = getNextMediaItem();
				}
			}
			// Log.i("AAAA", "--- getNextMediaItem_2 ---");
			// 当取得的媒体对象已经被移除时，重新取得下个被移除媒体对象
			if (mediaItme != null && mDeleteMediaItems.containsKey(mediaItme.getFilePath())) {
				mDeleteMediaItems.remove(mediaItme.getFilePath());
				mediaItme = getNextMediaItem();
			}
			return mediaItme;
		}

		/**
		 * 取得高优先扫描媒体文件
		 * 
		 * @return 媒体对象
		 */
		private MediaItem findNextForPriority() {
			// L.d("test", "--- findNextForPriority ---");
			MediaItem mediaItem = null;
			try {
				// 取得高优先扫描媒体文件
				if (mPriorityMedaiItems.size() > 0) {
					mediaItem = mPriorityMedaiItems.get(mPriorityMedaiItems.size() - 1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return mediaItem;
		}

		/**
		 * 取得低优先扫描媒体文件
		 * 
		 * @return 媒体对象
		 */
		private MediaItem findNextForAllMedia() {
			// L.d("test", "--- findNextForAllMedia ---");
			MediaItem mediaItem = null;
			// 根据当前管理的媒体类型取出下一个要解析的对象
			mediaItem = mUsbMediaDB.getNextNonParsedItem(mStoreMediaType, UsbDiskUtil.getMountedPath());
			if (mediaItem != null)
				return mediaItem;
			// 当管理的媒体类型已经没有了要解析的，那就解析其他的吧
			mediaItem = mUsbMediaDB.getNextNonParsedItem(MediaType.AUDIO, UsbDiskUtil.getMountedPath());
			if (mediaItem != null)
				return mediaItem;
			mediaItem = mUsbMediaDB.getNextNonParsedItem(MediaType.IMAGE, UsbDiskUtil.getMountedPath());
			if (mediaItem != null)
				return mediaItem;
			mediaItem = mUsbMediaDB.getNextNonParsedItem(MediaType.VIDEO, UsbDiskUtil.getMountedPath());
			if (mediaItem != null)
				return mediaItem;
			return mediaItem;
		}

		/**
		 * 降CPU占用
		 * 
		 * @param millis
		 *            等多少毫秒
		 */
		private void cpuRunWait(long millis) {
			if (!future.isCancelled()) {
				synchronized (mMetadataRunWait) {
					try {
						mMetadataRunWait.wait(millis);
					} catch (InterruptedException e) {
					}
				}
			}
		}

		long logTime = 0;

		/**
		 * 媒体Metadata解析回调接收类
		 */
		private class ClientProxy implements MediaScannerConnectionClient {

			@Override
			public void onMediaScannerConnected() {
				Log.i("AAAA", "ClientProxy onMediaScannerConnected() ThreadID: " + Thread.currentThread().getId());
				// 通知doInBackground()函数中的wait，表示连接处理完成
				synchronized (mMetadataRunWait) {
					mMetadataRunWait.notifyAll();
				}
			}

			@Override
			public void onScanCompleted(String path, Uri uri) {
				// Log.i("AAAA", "ClientProxy onScanCompleted() ThreadID: " + Thread.currentThread().getId());
				if (mCurrentScanMediaItem.getFilePath().equals(path)) {
					mCurrentScanMediaItemUri = uri;
					// 通知doInBackground()函数中的wait，表示一个文件解析处理完成
					synchronized (mMetadataRunWait) {
						mMetadataRunWait.notifyAll();
					}
				}
			}
			
			
		}

		/**
		 * 查询媒体Metadata类
		 */
		private class QueryMetadata {

			protected void queryImage(ContentResolver resolver, MediaItem mediaItem, Uri uri) {
				String[] projection = new String[] { MediaStore.Images.Media._ID, MediaStore.Images.Media.TITLE };
				Cursor cursor = resolver.query(uri, projection, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						mediaItem.setMediaID(cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID)));
						mediaItem.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.TITLE)));
					}
					cursor.close();
				}
			}

			protected void queryVideo(ContentResolver resolver, MediaItem mediaItem, Uri uri) {
				String[] projection = new String[] { MediaStore.Video.Media._ID, MediaStore.Video.Media.TITLE, MediaStore.Video.Media.ALBUM,
						MediaStore.Video.Media.ARTIST, MediaStore.Video.Media.DURATION };
				Cursor cursor = resolver.query(uri, projection, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						mediaItem.setMediaID(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
						mediaItem.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)));
						mediaItem.setAlbum(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ALBUM)));
						mediaItem.setArtist(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.ARTIST)));
						mediaItem.setDuration(cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)));
					}
					cursor.close();
				}
			}

			protected void queryAudio(ContentResolver resolver, MediaItem mediaItem, Uri uri) {
				String[] projection = new String[] { MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM,
						MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DURATION };
				Cursor cursor = resolver.query(uri, projection, null, null, null);
				if (cursor != null) {
					if (cursor.moveToFirst()) {
						mediaItem.setMediaID(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
						mediaItem.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
						mediaItem.setAlbum(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)));
						mediaItem.setAlbumID(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
						mediaItem.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
						mediaItem.setDuration(cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
					}
					cursor.close();
				}
			}
		}

		/**
		 * 用于保存媒体解析通知参数使用
		 */
		private class NotifyMediaItem {

			/**
			 * 媒体解析是否为优先解析媒体
			 */
			public boolean isPriorityMediaItem;

			/**
			 * 媒体完成的媒体对象
			 */
			public MediaItem mediaItem;

			/**
			 * NotifyMediaItem实例化
			 * @param priority 是否为优先解析媒体
			 * @param item 媒体对象
			 */
			public NotifyMediaItem(boolean priority, MediaItem item) {
				isPriorityMediaItem = priority;
				mediaItem = item;
			}
		}

		@Override
		public void onAudioFilesAdded(List<MediaItem> items) {
			synchronized (mAddFileNotify) {
				mAddFileNotify.notifyAll();
			}
		}

		@Override
		public void onImageFilesAdded(List<MediaItem> items) {
			synchronized (mAddFileNotify) {
				mAddFileNotify.notifyAll();
			}
		}

		@Override
		public void onVideoFilesAdded(List<MediaItem> items) {
			synchronized (mAddFileNotify) {
				mAddFileNotify.notifyAll();
			}
		}

		@Override
		public void onOfficeFilesAdded(List<MediaItem> items) {
			synchronized (mAddFileNotify) {
				mAddFileNotify.notifyAll();
			}
		}
	}

	/**
	 * 媒体Metadata扫描接口实现类
	 */
	private class MedatataRetriever implements IMedatataRetriever {

		private CopyOnWriteArrayList<IMetadataListener> mIMetadataListeners;

		protected MedatataRetriever() {
			mIMetadataListeners = new CopyOnWriteArrayList<IMetadataListener>();
		}

		@Override
		public void addIMetadataListener(IMetadataListener listener) {
			L.d(">>>>addIMetadataListener");
			mIMetadataListeners.add(listener);
		}

		@Override
		public void removeIMetadataListener(IMetadataListener listener) {
			L.d(">>>>removeIMetadataListener");
			if (listener != null) {
				mIMetadataListeners.remove(listener);
			}
		}

		@Override
		public void retrieveMetadata(MediaItem mediaItem) {
			addPriorityMedaiItem(mediaItem);
		}

		protected void notifyMetadataRetrieved(MediaItem mediaItem) {

			for (IMetadataListener listener : mIMetadataListeners) {
				if (!UsbDiskUtil.isMounted(UsbDiskUtil.getRootPath(mediaItem.getFilePath()))) {
					break;
				}
				try {
					listener.onMetadataRetrieved(mediaItem);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		protected void notifyBackgroundMetadataRetrieved(MediaItem mediaItem) {
			for (IMetadataListener listener : mIMetadataListeners) {
				if (!UsbDiskUtil.isMounted(UsbDiskUtil.getRootPath(mediaItem.getFilePath()))) {
					break;
				}
				try {
					listener.onBackgroundMetadataRetrieved(mediaItem);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * 后台媒体数据解析
		 * 
		 * @param count
		 */
		protected void notifyAllBackgroundMetadataRetrieved(int countUsb, int countOutSd, int countInnerSd) {
			L.d(">>>>notifyAllBackgroundMetadataRetrieved :mObtainMediaInfoCount" + countUsb);
			for (IMetadataListener listener : mIMetadataListeners) {
				int count = 0;
				if (UsbDiskUtil.isMounted(Configuration.USB_INNER_SD_PATH)) {
					count = count + countInnerSd;
				}
				if (UsbDiskUtil.isMounted(Configuration.USB_MEDIA_ROOT_PATH)) {
					count = count + countUsb;
				}
				if (UsbDiskUtil.isMounted(Configuration.USB_OUTER_SD_PATH)) {
					count = count + countOutSd;
				}
				try {
					listener.onAllBkMetadataRetrieved(count);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public boolean isFinish() {
		return mStoreManagerStatus == StoreManagerStatus.FINISH;
	}

	@Override
	public void setStoreMediaType(MediaType mediaType) {
		L.d("setStoreMediaType:" + mediaType.getId());
		// 需要变更解析媒体的优先级
		this.mStoreMediaType = mediaType;
	}

	/**
	 * 调整扫描速率
	 * 
	 * @author:
	 * @createTime: 2016-11-30 下午5:59:15
	 * @history:
	 * @param isRefrain
	 *            void
	 */
	public void setCPURefrain(boolean isRefrain) {
		Log.i("AAAA", "setCPURefrain isRefrain:" + isRefrain);
		if (isBootComplete) {
			if (isRefrain) {
				setStoreManagerStatus(StoreManagerStatus.SPEED_LEVEL2);
			} else {
				setStoreManagerStatus(StoreManagerStatus.SPEED_LEVEL3);
			}
		} else {
			setStoreManagerStatus(StoreManagerStatus.SPEED_LEVEL1);
		}
	}

	@Override
	public boolean mediaFileCollecte(boolean isCollected, String path) {
		return mUsbMediaDB.setCollectedValue(isCollected, path);
	}

	@Override
	public void stopStoreManager() {
		releaseStoreTask();
		unregisterBroadcastEvents(mContext.get());
		mContext.get().getContentResolver().unregisterContentObserver(bootCompleteResolver);
	}

	@Override
	public void addPriorityMedaiItem(String path) {
		L.i(" --- addPriorityMedaiItem_path --- " + path);
		MediaItem item = mUsbMediaDB.getMediaItemByFilePath(path);
		addPriorityMedaiItem(item);
	}

	private int getScanWaitTime() {
		// if (mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL1) {
		// return 30;
		// } else if (mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL2) {
		// return 20;
		// } else if (mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL3) {
		// return 6;
		// }
		// return 30;
		return 1;
	}

	private int getPraseWaitTime() {
		// if (mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL1) {
		// return 1000;
		// } else if (mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL2) {
		// return 550;
		// } else if (mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL3) {
		// return 350;
		// }
		// return 1000;
		return 50;
	}

	private ContentObserver bootCompleteResolver = new ContentObserver(new Handler()) {

		public void onChange(boolean selfChange, Uri uri) {

			String observerString = uri.getLastPathSegment();
			if (observerString != null && "system_warning_click".equals(observerString)) {
				int bootComplete = Settings.System.getInt(mContext.get().getContentResolver(), observerString, 0);
				if (bootComplete == 1) {
					isBootComplete = true;
					if (mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL2 || mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL3) {
						// 不调整
					} else {
						setStoreManagerStatus(StoreManagerStatus.SPEED_LEVEL2);
					}

				} else {
					isBootComplete = false;
					setStoreManagerStatus(StoreManagerStatus.SPEED_LEVEL1);
				}
			}
		};
	};

}
