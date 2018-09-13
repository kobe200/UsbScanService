package carnetos.usbservice.aidl;
interface IMediaClient{

	//SERVICE-->CLIENT Audio文件有增加
	void onAudioFilesAdded();
	//SERVICE-->CLIENT Image文件有增加
	void onImageFilesAdded();
	//SERVICE-->CLIENT Video文件有增加
	void onVideoFilesAdded();
	//SERVICE-->CLIENT Office文件有增加
	void onOfficeFilesAdded();
	//SERVICE-->CLIENT 有媒体文件删除时通知
	void onMediaFilesRemoved(String path);
	//SERVICE-->CLIENT 清除对应媒体文件
	void onMediaFilesCleared(String rootPath);
	//SERVICE-->CLIENT 所有路径扫描完成
	void onFileScanFinished(String rootPath);
	//SERVICE-->CLIENT 当前媒体解析完成
	void onMetadataRetrieved(int mediaType,String path);
	//SERVICE-->CLIENT 后台队列媒体解析完成
	void onBackgroundMetadataRetrieved(int mediaType,String path);
	//SERVICE-->CLIENT 后台多媒体全部解析完成
	void onAllMetadataRetrieved(int count);
	//SERVICE-->CLIENT UsbDisk挂载
	void onUsbDiskMounted(String path);
	//SERVICE-->CLIENT UsbDisk卸载
	void onUsbDiskUnMounted(String path);
}

