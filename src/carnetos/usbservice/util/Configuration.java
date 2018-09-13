package carnetos.usbservice.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.os.Process;

/**
 * UsbMedia库运行时配置信息类
 */
@SuppressLint("SdCardPath")
public class Configuration {

	/**
	 * 线程优先级
	 */
	public static final int THREAD_PRIORITY = Process.THREAD_PRIORITY_BACKGROUND;

	/**
	 * USB设备挂载根目录
	 */
	public static final String USB_MEDIA_ROOT_PATH = "/storage/udisk";
//	public static final String USB_INNER_SD_PATH = "";
	public static final String USB_INNER_SD_PATH = "storage/emulated/0";
	public static final String USB_OUTER_SD_PATH = "/storage/extsd";

	/**
	 * 6.0U盘挂载的识别号
	 */
	public static String UDISK_NUMBER = "/storage/0000-0000";
	
	/**
	 * 配置忽略扫描的路径
	 */
	public static List<String> UNSCAN_DEVICE = new ArrayList<String>() ;
	{
		UNSCAN_DEVICE.add("storage/emulated/0") ;
		UNSCAN_DEVICE.add("storage/extsd0") ;
	}
	public static Map<String, UsbDevice> DEVICE = new HashMap<String, UsbDevice>() {

		{
			put(USB_MEDIA_ROOT_PATH, new UsbDevice(USB_MEDIA_ROOT_PATH, false));
			put(USB_INNER_SD_PATH, new UsbDevice(USB_INNER_SD_PATH, true));
			put(USB_OUTER_SD_PATH, new UsbDevice(USB_OUTER_SD_PATH, false));
		}
	};

	/**
	 * 取得USB设备挂载根目录
	 * 
	 * @return USB设备挂载根目录全路径
	 */
	public static String getUsbMediaRootPath(){
		return USB_MEDIA_ROOT_PATH;
	}

	public static String getInnerSdRootPath(){
		return USB_INNER_SD_PATH;
	}

	public static String getOuterSdRootPath(){
		return USB_OUTER_SD_PATH;
	}

	/**
	 * 判定指定目录是否为Windows回收站目录
	 * 
	 * @param path
	 *            USB设备子目录全路径
	 * @return true:是Windows回收站目录，false:不是Windows回收站目录
	 */
	public static boolean isWindowsRecycler(String path){
		boolean blnRet = false;

		String strRecycleNT4 = getUsbMediaRootPath() + File.separator + "Recycler";
		String strRecycleNT6 = getUsbMediaRootPath() + File.separator + "$Recycle.Bin";

		// 检查是否为Windows操作系统默认回收站目录名
		if (path.compareToIgnoreCase(strRecycleNT4) == 0 || path.compareToIgnoreCase(strRecycleNT6) == 0) {
			blnRet = true;
		}

		// 检查是否为通过desktop.ini设定的回收站目录
		if (!blnRet) {
			String strDesktopIniFile = path + File.separator + "desktop.ini";
			File fileDisktopIni = new File(strDesktopIniFile);

			if (fileDisktopIni.exists() && fileDisktopIni.canRead()) {
				BufferedReader reader = null;
				String strLine = null;

				try {
					reader = new BufferedReader(new FileReader(strDesktopIniFile));

					while ((strLine = reader.readLine()) != null) {
						if (strLine.contains("CLSID={645FF040-5081-101B-9F08-00AA002F954E}")) {
							blnRet = true;
							break;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
						reader = null;
					}
				}
			}
		}

		return blnRet;
	}
}
