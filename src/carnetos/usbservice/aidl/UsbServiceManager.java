package carnetos.usbservice.aidl;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import carnetos.usbservice.application.AllUsbMediaApplication;
import carnetos.usbservice.entity.MediaItem;
import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.main.IMediaStoreChangeListener;
import carnetos.usbservice.main.IMetadataListener;
import carnetos.usbservice.main.IUsbMediaStore;
import carnetos.usbservice.main.UsbMediaStore;
import carnetos.usbservice.util.L;
import carnetos.usbservice.util.UsbDiskUtil;

/**
 * 为媒体扫描程序提供模块化的数据支持,主要实现 1.媒体扫描库中的接口{@link IMetadataListener}和{@link IMediaStoreChangeListener}
 * 
 * @desc: UsbServiceManager
 * @author:tang
 * @createTime: 2016-9-27 下午2:29:03
 * @history:
 * @version: v1.0
 */
public class UsbServiceManager implements IMetadataListener, IMediaStoreChangeListener {

	/**
	 * 删除锁
	 */
	private Object removeLock = new Object();

	/**
	 * USB媒体库接口对象
	 */
	private IUsbMediaStore mUsbMediaStore;

	/**
	 * 客户端远程回调
	 */
	private static RemoteCallbackList<IMediaClient> mediaClients = new RemoteCallbackList<IMediaClient>();
	/**
	 * 单例模式的对象
	 */
	private static UsbServiceManager instance = null;
	/*
	 * 音频更新
	 */
	private static final byte MSG_AUDIO_CHANGED = 0x00;
	/*
	 * 图片更新
	 */
	private static final byte MSG_IMAGE_CHANGED = MSG_AUDIO_CHANGED + 1;
	/*
	 * 视频更新
	 */
	private static final byte MSG_VIDEO_CHANGED = MSG_AUDIO_CHANGED + 2;
	/*
	 * 媒体删除
	 */
	private static final byte MSG_MEDIA_REMOVED = MSG_AUDIO_CHANGED + 3;
	/*
	 * 媒体解析
	 */
	private static final byte MSG_MEDIA_RETRIEVED = MSG_AUDIO_CHANGED + 4;
	/*
	 * 后台媒体解析
	 */
	private static final byte MSG_BK_MEDIA_RETRIEVED = MSG_AUDIO_CHANGED + 5;
	/*
	 * 媒体清除
	 */
	private static final byte MSG_MEDIA_CLEARED = MSG_AUDIO_CHANGED + 6;
	/*
	 * 扫描完成
	 */
	private static final byte MSG_SCAN_FINISHED = MSG_AUDIO_CHANGED + 7;

	/*
	 * 后台媒体解析
	 */
	private static final byte MSG_ALL_BK_MEDIA_RETRIEVED = MSG_AUDIO_CHANGED + 8;
	
	/*
	 * UsbDisk挂载
	 */
	private static final byte MSG_USBDISK_MOUNTED = MSG_AUDIO_CHANGED + 9;
	
	/*
	 * UsbDisk卸载
	 */
	private static final byte MSG_USBDISK_UNMOUNTED = MSG_AUDIO_CHANGED + 10;
	
	/*
	 *  办公文件更新
	 */
	private static final byte MSG_OFFICE_CHANGED = MSG_AUDIO_CHANGED + 11;
	
	
	/**
	 * 主线程消息处理
	 */
	private Handler handler = new Handler() {

		public void handleMessage(android.os.Message msg) {
			// 处理发送事务
			switch (msg.what) {
				case MSG_AUDIO_CHANGED :
					notifyAudioFilesAdded();
					break;
				case MSG_BK_MEDIA_RETRIEVED :
					notifyBkMetadataRetrieved((MediaItem) msg.obj);
					break;
				case MSG_MEDIA_RETRIEVED :
					notifyMetadataRetrieved((MediaItem) msg.obj);
					break;
				case MSG_IMAGE_CHANGED :
					notifyImageFilesAdded();
					break;
				case MSG_MEDIA_CLEARED :
					notifyMediaFilesCleared((String) msg.obj);
					break;
				case MSG_MEDIA_REMOVED :
					notifyMediaFilesRemoved((String) msg.obj);
					break;
				case MSG_SCAN_FINISHED :
					notifyFileScanFinished((String) msg.obj);
					break;
				case MSG_VIDEO_CHANGED :
					notifyVidoeFilesAdded();
					break;
				case MSG_OFFICE_CHANGED:
					notifyOfficeFilesAdded();
					break;
				case MSG_ALL_BK_MEDIA_RETRIEVED :
					L.d("tt", " MSG_ALL_BK_MEDIA_RETRIEVED msg.arg1:" + (Integer) msg.obj);
					notifyAllBkMetadataRetrieved((Integer) msg.obj);
					break;
				case MSG_USBDISK_MOUNTED :
					notifyUsbDiskMounted((String) msg.obj);
					break;
				case MSG_USBDISK_UNMOUNTED :
					notifyUsbDiskUnMounted((String) msg.obj);
					break;
			}
		};
	};

	/**
	 * 私有化构造方法
	 */
	private UsbServiceManager() {
		L.d("UsbServiceManager");
		//初始化数据
		/* 初始化全局变量 */
		mUsbMediaStore = new UsbMediaStore(AllUsbMediaApplication.App,MediaType.AUDIO,AllUsbMediaApplication.App.getScanMediaType());
		/* 开启MediaStore管理服务 */
		mUsbMediaStore.startStoreManager(AllUsbMediaApplication.App);
	}

	/**
	 * 设置数据监听，将Manager绑定扫描接口
	 * 
	 * @author:
	 * @createTime: 2016-11-26 下午4:58:20
	 * @history: void
	 */
	public void startListener(){
		/*
		 * 监听媒体扫描结果
		 */
		mUsbMediaStore.addMediaStoreChangeListener(this);
		/*
		 * 监听解析结果
		 */
		mUsbMediaStore.getMetadataRetriever().addIMetadataListener(this);
	}

	/*
	 * 取得回调监听和远程对象
	 */
	public IBinder getIMediaService(){
		L.d("getIMediaService");
		return mediaService.asBinder();
	}

	/**
	 * 取得USB媒体库接口.
	 * 
	 * @return USB媒体库接口对象
	 */
	public final IUsbMediaStore getUsbMediaStore(){
		return mUsbMediaStore;
	}

	/**
	 * 停止USB媒体库所有功能模块，通常在应用程序退了时调用本方法
	 */
	public void stopMediaStore(){
		if (mUsbMediaStore != null) {
			mUsbMediaStore.stopStoreManager();
		}
	}

	/*
	 * 设置扫描类型
	 */
	private void setMediaType(int type){
		if (mUsbMediaStore != null) {
			mUsbMediaStore.setStoreMediaType(MediaType.getMediaType(type));
		}
	}

	/*
	 * 取得优先的媒体
	 */
	private int getMediaType(){
		if (mUsbMediaStore != null) {
			mUsbMediaStore.getStoreMediaType();
		}
		return 0;
	}

	/*
	 * 是否扫描完成
	 */
	private boolean isScanFinished(String path){
		return UsbDiskUtil.isScanFinished(path);
	}

	/*
	 * 调整CPU
	 */
	private void setCpuRefrain(boolean refrain){
		if (mUsbMediaStore != null) {
			mUsbMediaStore.setCPURefrain(refrain);
		}
	}

	/*
	 * 移除
	 */
	private boolean removeMedia(String path){
		if (mUsbMediaStore != null) {
			return mUsbMediaStore.removeMediaFile(path);
		}
		return false;
	}
	
	/*
	 * 收藏
	 */
	private boolean mediaCollecte(boolean isCollected, String path){
		if (mUsbMediaStore != null) {
			return mUsbMediaStore.mediaFileCollecte(isCollected, path);
		}
		return false;
	}

	/*
	 * 优先解析
	 */
	private void addPriorityItem(String path){
		if (mUsbMediaStore != null) {
			 mUsbMediaStore.addPriorityMedaiItem(path);
		}
	}

	private IMediaService.Stub	mediaService	= new IMediaService.Stub() {

		@Override
		public void unregisterCallback(IMediaClient ims) throws RemoteException{
			mediaClients.unregister(ims);
		}

		@Override
		public void registerCallback(IMediaClient ims) throws RemoteException{
			mediaClients.register(ims);
			
			Log.i("info", "--- registerCallback_0 ---" + mediaClients.beginBroadcast());
			mediaClients.finishBroadcast();
			
			Log.i("info", "--- registerCallback_1 ---" + mediaClients.getRegisteredCallbackCount());

			
			
			List<String> mountedPath = UsbDiskUtil.getMountedPath();
			for (String pathString : mountedPath) {
				onUsbDiskMounted(pathString);
				if (UsbDiskUtil.isScanFinished(pathString)) {
					onFileScanFinished(pathString);
				}
				
			}
		}

		@Override
		public void setStoreMediaType(int mediaType) throws RemoteException{
			//设置解析扫描优先的类型
			setMediaType(mediaType);
		}

		@Override
		public boolean isFileScanFinished(String path) throws RemoteException{
			//查询是否扫描该路径完成
			return isScanFinished(path);
		}

		@Override
		public int getStoreMediaType() throws RemoteException{
			//获取当前优先的媒体类型
			return getMediaType();
		}

		@Override
		public void setCPURefrain(boolean isRefrain) throws RemoteException{
			//是否CPU压制 true 压制 false 不压制
			setCpuRefrain(isRefrain);
		}

		@Override
		public boolean removeMediaFile(String path) throws RemoteException{
			//删除某个文件
			return removeMedia(path);
		}

		@Override
		public void addPriorityMedaiItem(String path) throws RemoteException{
			//设置优先解析的文件
			addPriorityItem(path);
		}

		@Override
		public boolean mediaFileCollecte(boolean isCollected, String path) throws RemoteException {
			return mediaCollecte(isCollected, path);
		}
	};

	/**
	 * 回去媒体数据中心的对象
	 * 
	 * @author:tang
	 * @createTime: 2016-9-27 下午2:43:54
	 * @history:
	 * @return UsbServiceManager
	 */
	public static UsbServiceManager getInstance(){
		if (instance == null)
			instance = new UsbServiceManager();
		return instance;
	}

	/**
	 * GC对象
	 * 
	 * @author:tang
	 * @createTime: 2016-9-27 下午4:18:48
	 * @history: void
	 */
	public void destory(){
		mUsbMediaStore.onAppTerminate(AllUsbMediaApplication.App);
		mediaClients.kill();
		mediaService = null;
		handler = null;
		instance = null;
	}

	/*
	 * 通知客户端Audio文件更新
	 */
	private void notifyAudioFilesAdded(){
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				mediaClients.getBroadcastItem(i).onAudioFilesAdded();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}

	@Override
	public void onAudioFilesAdded(List<MediaItem> items){
		L.d("onAudioFilesAdded:" + items.size());
		handler.obtainMessage(MSG_AUDIO_CHANGED).sendToTarget();
	}

	/*
	 * 通知客户端Image文件更新
	 */
	private void notifyImageFilesAdded(){
		L.d(L.CLIENT,"notifyImageFilesAdded" );
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				mediaClients.getBroadcastItem(i).onImageFilesAdded();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}

	@Override
	public void onImageFilesAdded(List<MediaItem> items){
		L.d(L.CLIENT,"onImageFilesAdded:" + items.size());
		handler.obtainMessage(MSG_IMAGE_CHANGED).sendToTarget();
	}

	/*
	 * 通知客户端Vidoe文件更新
	 */
	private void notifyVidoeFilesAdded(){
		L.d(L.CLIENT,"client notifyVidoeFilesAdded" );
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				mediaClients.getBroadcastItem(i).onVideoFilesAdded();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}

	@Override
	public void onVideoFilesAdded(List<MediaItem> items){
		L.d(L.CLIENT, "onVideoFilesAdded:" + items.size());
		handler.obtainMessage(MSG_VIDEO_CHANGED).sendToTarget();
	}

	/**
	 * 某文件移除
	 * 
	 * @author:
	 * @createTime: 2016-11-29 下午4:46:44
	 * @history:
	 * @param path
	 *            void
	 */
	private void notifyMediaFilesRemoved(String path){
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				mediaClients.getBroadcastItem(i).onMediaFilesRemoved(path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}

	@Override
	public void onMediaFilesRemoved(List<MediaItem> items){
		synchronized (removeLock) {
			for ( int i = 0 ; i < items.size() ; i++ ) {
				handler.obtainMessage(MSG_MEDIA_REMOVED, items.get(i).getFilePath()).sendToTarget();
			}
		}
	}

	/*
	 * 通知媒体解析
	 */
	private void notifyMetadataRetrieved(MediaItem mediaItem){
		if (mediaItem == null)
			return;
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
//				L.d(L.CLIENT,"onMetadataRetrieved:" + mediaItem.getName());
				mediaClients.getBroadcastItem(i).onMetadataRetrieved(mediaItem.getMediaType().getId(), mediaItem.getFilePath());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}

	@Override
	public void onMetadataRetrieved(MediaItem mediaItem){
	
		handler.obtainMessage(MSG_MEDIA_RETRIEVED, mediaItem).sendToTarget();
	}

	/*
	 * 后台解析完成
	 */
	private synchronized void notifyBkMetadataRetrieved(MediaItem mediaItem){
		Log.i("testinfo", "--- onBackgroundMetadataRetrieved_mediaItem == null? ---" + (mediaItem == null));

		
		if (mediaItem == null)
			return;
		
		Log.i("testinfo", "--- onBackgroundMetadataRetrieved_mediaItem.getName() ---" + mediaItem.getName());

		
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				L.d(L.CLIENT,"onMetadataRetrieved:" + mediaItem.getName());
				if (mediaItem.getFilePath() != null) {
					mediaClients.getBroadcastItem(i).onMetadataRetrieved(mediaItem.getMediaType().getId(), mediaItem.getFilePath());
				}
			} catch (Exception e) {
				L.d("UsbServiceManager_notifyBkMetadataRetrieved_Exception: " + e.toString());
//				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}

	@Override
	public void onBackgroundMetadataRetrieved(MediaItem mediaItem){
//		L.d("onMetadataRetrieved:" + mediaItem.getName());
		handler.obtainMessage(MSG_BK_MEDIA_RETRIEVED, mediaItem).sendToTarget();
	}


	/*
	 * 通知客户端所有的多媒体文件更新
	 */
	private void notifyAllBkMetadataRetrieved(int count  ){
//		L.d("tt","notify client:notifyAllBkMetadataRetrieved count:"+count);
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				L.d("tt","notifyAllBkMetadataRetrieved onAllMetadataRetrieved:"+count);
				mediaClients.getBroadcastItem(i).onAllMetadataRetrieved(count);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}
	
	
	@Override
	public void onUsbDiskMounted(String path){
		Log.i("info", "--- onUsbDiskMounted ---" );

		handler.obtainMessage(MSG_USBDISK_MOUNTED, path).sendToTarget();
	}
	
	private void notifyUsbDiskMounted(String path){
		Log.i("info", "--- notifyUsbDiskMounted ---" );
		
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				Log.i("info", "--- mediaClients.getBroadcastItem(i) ---" + mediaClients.getBroadcastItem(i));

				mediaClients.getBroadcastItem(i).onUsbDiskMounted(path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}
	

	@Override
	public void onUsbDiskUnMounted(String path){
		handler.obtainMessage(MSG_USBDISK_UNMOUNTED, path).sendToTarget();
	}
	
	private void notifyUsbDiskUnMounted(String path){
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				mediaClients.getBroadcastItem(i).onUsbDiskUnMounted(path);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}
	

	/**
	 * 通知U盘拔出需要清除该目录下所有文件
	 * 
	 * @author:
	 * @createTime: 2016-11-29 下午4:50:11
	 * @history:
	 * @param rootPath
	 *            void
	 */
	private void notifyMediaFilesCleared(String rootPath){
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				mediaClients.getBroadcastItem(i).onMediaFilesCleared(rootPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}

	@Override
	public void onMediaFilesCleared(String path){
		//U盘拔出等情景
		handler.obtainMessage(MSG_MEDIA_CLEARED, path).sendToTarget();
	}

	/*
	 * 扫描完成
	 */
	private void notifyFileScanFinished(String rootPath){
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				L.d(L.CLIENT,">>>>>>>>>>>>notifyFileScanFinished rootPath:" + rootPath);
				mediaClients.getBroadcastItem(i).onFileScanFinished(rootPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}

	@Override
	public void onFileScanFinished(String path){
		handler.obtainMessage(MSG_SCAN_FINISHED, path).sendToTarget();
	}

	@Override
	public void onAllBkMetadataRetrieved(int count) {
		L.d(L.CLIENT,">>>>>>>>>>onAllBkMetadataRetrieved MSG_ALL_BK_MEDIA_RETRIEVED count"+count);
		handler.obtainMessage(MSG_ALL_BK_MEDIA_RETRIEVED, count).sendToTarget() ;

	}

	@Override
	public void onOfficeFilesAdded(List<MediaItem> items) {
		// TODO Auto-generated method stub
		L.d(L.CLIENT, "onVideoFilesAdded:" + items.size());
		handler.obtainMessage(MSG_OFFICE_CHANGED).sendToTarget();
	}

	/*
	 * 通知客户端Office文件更新
	 */
	private void notifyOfficeFilesAdded(){
		L.d(L.CLIENT,"client notifyOfficeFilesAdded" );
		int N = mediaClients.beginBroadcast();
		for ( int i = 0 ; i < N ; i++ ) {
			try {
				mediaClients.getBroadcastItem(i).onOfficeFilesAdded();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		mediaClients.finishBroadcast();
	}
}
