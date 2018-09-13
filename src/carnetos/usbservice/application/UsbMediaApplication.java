package carnetos.usbservice.application;

import android.app.Application;

import carnetos.usbservice.entity.MediaItem;
import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.main.IUsbMediaStore;
import carnetos.usbservice.main.UsbMediaStore;
import carnetos.usbservice.util.MediaScanConfigManager;

/**
 * 使用UsbMedia库应用程序的Application基类
 */
public abstract class UsbMediaApplication extends Application {
	public static UsbMediaApplication	App	= null;

	@Override
	public void onCreate(){
		super.onCreate();
		App = this;
		MediaScanConfigManager.init();
	}

	@Override
	public void onTerminate(){
		App = null;
		super.onTerminate();
	}
	/**
	 * 取得当前扫描的媒体类型 {@link MediaType}
	 * 
	 * @author:tang
	 * @createTime: 2016-9-26 上午10:35:14
	 * @history:
	 * @return MediaType
	 */
	public abstract MediaType getScanMediaType();
	public abstract String[] getScanRootPath();

}
