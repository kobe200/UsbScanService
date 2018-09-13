package carnetos.usbservice.aidl;
import carnetos.usbservice.aidl.IMediaClient;
interface IMediaService
{
	//CLIENT-->MEDIASERVICE 向媒体扫描服务注册回调接口
	void registerCallback(IMediaClient ims);  
	//CLIENT-->MEDIASERVICE 向媒体扫描服务反注册回调接口
    void unregisterCallback(IMediaClient ims); 
    //CLIENT-->MEDIASERVICE 取得当前扫描的主要媒体类型
    int getStoreMediaType();
    //CLIENT-->MEDIASERVICE 设置扫描解析类型的优先级
    void setStoreMediaType(int mediaType);
    //CLIENT-->MEDIASERVICE 媒体扫描服务扫描是否完成
    boolean isFileScanFinished(String path);
    //CLIENT-->MEDIASERVICE 移除媒体文件
    boolean removeMediaFile(String path);
    	//SERVICE-->CLIENT 媒体文件收藏
	boolean mediaFileCollecte(boolean isCollected, String path);
    //CLIENT-->MEDIASERVICE 提高该文件的解析优先级
    void addPriorityMedaiItem(String path);
    //CLIENT-->MEDIASERVICE 是否抑制CPU占用，当UI界面出来时可调用该方法设置false加快扫描解析，UI界面隐藏时设置为true可降低CPU
    void setCPURefrain(boolean isRefrain);
}