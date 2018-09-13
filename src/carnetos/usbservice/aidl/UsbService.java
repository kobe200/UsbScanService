package carnetos.usbservice.aidl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.util.Log;

/**
 * 媒体扫描服务远端
 * 
 * @desc: CookooUsbService
 * @author:
 * @createTime: 2016-11-19 下午3:07:45
 * @history:
 * @version: v1.0
 */
public class UsbService extends Service {

	@Override
	public void onCreate(){
		super.onCreate();
		/*
		 * 初始化UsbServiceManager
		 */
		Log.d("MMM", "UsbService onCreate");
		UsbServiceManager.getInstance().startListener();
	};

	@Override
	public IBinder onBind(Intent intent){
		Log.d("MMM", "UsbService onBind");
		return UsbServiceManager.getInstance().getIMediaService();
	}

	@Override
	public void onDestroy(){
		UsbServiceManager.getInstance().destory();
		super.onDestroy();
	};
}
