package carnetos.usbservice.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.util.EncodingUtils;

import carnetos.usbservice.util.UsbDevice.UsbDeviceScanState;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

/**
 * 与UsbDisk相关的辅助类。
 */
public final class UsbDiskUtil {

	/**
	 * 存储mount信息的文件
	 */
	private static final String	MOUNTS_FILE	= "/proc/mounts";

	/**
	 * 禁止实例化
	 */
	private UsbDiskUtil() {
		// Nothing
	}

	/**
	 * 检查UDisk是否挂载。
	 * 
	 * @return true:UDisk已经挂载, false:UDisk未挂载
	 */
	public static void initUsbDiskMounted(Context context){
		String udiskPath = Configuration.getUsbMediaRootPath();
		String outer = Configuration.getOuterSdRootPath();
		//默认三路径
		String strLine = null;
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(MOUNTS_FILE));
			while ((strLine = reader.readLine()) != null) {
				if (strLine.contains(udiskPath) || strLine.contains("mnt/media_rw/udisk")) {
					Configuration.DEVICE.get(Configuration.getUsbMediaRootPath()).setMount(true);
				}
				if (strLine.contains(outer)) {
					Configuration.DEVICE.get(Configuration.getOuterSdRootPath()).setMount(true);
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
		
		L.d("--- UsbDiskUtil_initUsbDiskMounted ---");
		int currentVersion = android.os.Build.VERSION.SDK_INT; 
		if (currentVersion >= Build.VERSION_CODES.M){//6.0系统
			//上电启动检测到有设备
			List<String> otherPath = getMountedDevice(context);
			for ( int i = 0 ; i < otherPath.size() ; i++ ) {
				L.d("--- UsbDiskUtil_initUsbDiskMounted_Path ---" + otherPath.toString());
				Configuration.DEVICE.put(otherPath.get(i), new UsbDevice(otherPath.get(i),true));
			}
		}
	}

	/**
	 * 6.0取得挂载路径
	 * 
	 * @author:
	 * @createTime: 2017-3-2 上午11:47:09
	 * @history:
	 * @param context
	 *            void
	 */
	@SuppressLint("NewApi") public static List<String> getMountedDevice(Context context){
		List<String> temp = new ArrayList<String>();
		StorageManager mStorageManager = context
				.getSystemService(StorageManager.class);
		List<VolumeInfo> volumes = mStorageManager.getVolumes();
		for ( VolumeInfo vol : volumes ) {
			if (vol.getType() == VolumeInfo.TYPE_PUBLIC) {
				// 6.0中外置sd和usb标示为公共的
				File usbFile = vol.getPath();
				if (usbFile != null) {
					String usbPath = usbFile.getAbsolutePath();
					Configuration.UDISK_NUMBER = usbPath;
					//通知systemUI显示USB图标
					String randomUsbPath = usbPath + "--" + Math.random() + "";
					Settings.System.putString(context.getContentResolver(), "system_usb_mount", randomUsbPath);
					L.d("--- UsbDiskUtil_getMountedDevice_usbPath ---" + usbPath);
					temp.add(usbPath);
				}
			}
		}
		return temp;
	}
	
	/**
	 * 
	 * @return 是否为6.0系统
	 */
	public static boolean isDeviceVersion6() {
		int currentVersion = android.os.Build.VERSION.SDK_INT;
		if (currentVersion == Build.VERSION_CODES.M) {// 6.0系统
			return true;
		}
		return false;
	}

	/**
	 * 判断改路径是否挂载
	 * 
	 * @author:
	 * @createTime: 2016-11-29 下午7:33:52
	 * @history:
	 * @param path
	 * @return boolean
	 */
	public static boolean isMounted(String path){
		if (Configuration.DEVICE.get(path) != null)
			return Configuration.DEVICE.get(path).isMount();
		return false;
	}

	/*
	 * 挂载
	 */
	public static void setMounted(String path){
		if (Configuration.DEVICE.get(path) != null) {
			Configuration.DEVICE.get(path).setMount(true);
		} else {
			Configuration.DEVICE.put(path, new UsbDevice(path,true));
		}

	}
	
	
	public static List<String> getMountedPath()
	{
		List<String> temp =new ArrayList<String>();
		for ( UsbDevice device : Configuration.DEVICE.values() ) {
			if (device.isMount()) {
				temp.add(device.getRootPath());
			}
		}
		return temp;
	}

	/*
	 * 卸载
	 */
	public static void setUnMounted(String path){
		if (Configuration.DEVICE.get(path) != null)
			Configuration.DEVICE.get(path).setMount(false);

	}

	/*
	 * 该路径是否扫描完
	 */
	public static boolean isScanFinished(String path){
		if (TextUtils.isEmpty(path)) {
			return true;
		}
		if (Configuration.DEVICE.get(path) != null) {
			if (Configuration.DEVICE.get(path).getScanState() == UsbDeviceScanState.FINISH){
				return true;
			}
		}
		return false;
	}

	/*
	 * 该路径是否没扫描
	 */
	public static boolean isScanNone(String path){
		if (Configuration.DEVICE.get(path) != null) {
			if (Configuration.DEVICE.get(path).getScanState() == UsbDeviceScanState.NONE)
				return true;
		}
		return false;
	}

	/*
	 * 回归初始
	 */
	public static void setScanNone(String path){
		if (Configuration.DEVICE.get(path) != null)
			Configuration.DEVICE.get(path).setScanState(UsbDeviceScanState.NONE);
	}

	/*
	 * 扫描完成
	 */
	public static void setScanFinished(String path){
		if (Configuration.DEVICE.get(path) != null)
			Configuration.DEVICE.get(path).setScanState(UsbDeviceScanState.FINISH);
	}

	/*
	 * 扫描中
	 */
	public static void setScaning(String path){
		if (Configuration.DEVICE.get(path) != null)
			Configuration.DEVICE.get(path).setScanState(UsbDeviceScanState.SCANING);
	}

	/*
	 * 取得根目录
	 */
	public static String getRootPath(String path){
		if (path != null) {
			for ( UsbDevice device : Configuration.DEVICE.values() ) {
				if (path.contains(device.getRootPath())) {
					return device.getRootPath();
				}
			}
		}
		return null;
	}

	/**
	 * 写入单行字符串到U盘上的文件
	 * 
	 * @author:
	 * @createTime: 2016-9-30 上午10:22:43
	 * @history:
	 * @param name
	 * @param content
	 *            void
	 */
	public static synchronized void writeFileToUsbDisk(String name,String content, boolean isBeyondLength) throws IOException{
		L.d("--- writeFileToUsbDisk_content ---" + content);
		
		if (content == null)
			return;
		File file = null;
		if (isDeviceVersion6()) {
			String arr[] = content.split("/", 4);
			file = new File("mnt/media_rw/" + arr[2] + "/" + name);
		} else {
			file = new File("mnt/media_rw/udisk/" + name);
		}
//		if (!file.exists()) {
//			file.createNewFile();
//		}
		FileOutputStream fos = new FileOutputStream(file, !isBeyondLength);//如果文件长度超过1024，则以覆盖的方式写入
		byte[] bytes = content.getBytes();
		fos.write(bytes);
		fos.flush();
		fos.close();
	}
	

	/**
	 * 读取文件内容
	 * 
	 * @author:
	 * @createTime: 2016-9-30 上午10:31:54
	 * @history:
	 * @param name
	 * @return
	 * @throws IOException
	 *             String
	 */
	public static String readFileFromUsbDisk(String name) throws IOException{
		File file = new File("mnt/media_rw/udisk/" + name);
		if (!file.exists()){
			return readFileFromUsbDisk(name, Configuration.UDISK_NUMBER);
		}
		FileInputStream fis = new FileInputStream(file);
		int length = fis.available();
		byte[] buffer = new byte[length];
		fis.read(buffer);
		String res = EncodingUtils.getString(buffer, "UTF-8");
		fis.close();
		if (!TextUtils.isEmpty(res)) {
			res = res.substring(res.lastIndexOf("/storage", res.length() - 1));
		}
		L.d("--- readFileFromUsbDisk_content_res_1 ---" + res + "---");
		return res;
	}
	
	/***
	 * 如果为6.0的系统，根据传入的路径，获取U盘的标识号
	 * 
	 * @param name:缓存文件名
	 * @param path:带有U盘标识号的路径
	 * @return
	 * @throws IOException
	 */
	public static String readFileFromUsbDisk(String name, String path) throws IOException{
		String arr[] = path.split("/", 3);
		File file = new File("mnt/media_rw/" + arr[2] + "/" + name);
		L.d("--- readFileFromUsbDisk_path_6.0 ---" + file.toString());
		
		if (!file.exists())
			return null;
		FileInputStream fis = new FileInputStream(file);
		int length = fis.available();
		byte[] buffer = new byte[length];
		fis.read(buffer);
		String res = EncodingUtils.getString(buffer, "UTF-8");
		fis.close();
		if (!TextUtils.isEmpty(res)) {
			res = res.substring(res.lastIndexOf("/storage", res.length() - 1));
		}
		L.d("--- readFileFromUsbDisk_content_res ---" + res + "---");
		
		return res;
	}

	public static int getFileLen(String name, String path) throws IOException{
		String arr[] = path.split("/", 3);
		File file = new File("mnt/media_rw/" + arr[2] + "/" + name);
		if (!file.exists())
			return 0;
		FileInputStream fis = new FileInputStream(file);
		int length = fis.available();
		fis.close();
		return length;
	}
}
