
package carnetos.usbservice.util;
import android.util.Log;
/**
 * Log统一管理类
 */
public class L
{

	private L()
	{
		/* cannot be instantiated */
		throw new UnsupportedOperationException("cannot be instantiated");
	}

	public static boolean isDebug = true;// 是否需要打印bug，可以在application的onCreate函数里面初始化
	private static final String TAG = "USB";
	public static final String CLIENT="zhang" ;
	
	private static final String WHO = " --USBService-- ";

	// 下面四个是默认tag的函数
	public static void i(String msg)
	{
		if (isDebug)
			Log.i(TAG, WHO+msg);
	}

	public static void d(String msg)
	{
		if (isDebug)
			Log.d(TAG, WHO+msg);
	}

	public static void e(String msg)
	{
		if (isDebug)
			Log.e(TAG, WHO+msg);
	}

	public static void v(String msg)
	{
		if (isDebug)
			Log.v(TAG, WHO+msg);
	}

	// 下面是传入自定义tag的函数
	public static void i(String tag, String msg)
	{
		if (isDebug)
			Log.i(tag,WHO+msg);
	}

	public static void d(String tag, String msg)
	{
		if (isDebug)
			Log.i(tag, WHO+msg);
	}

	public static void e(String tag, String msg)
	{
		if (isDebug)
			Log.i(tag, WHO+msg);
	}

	public static void v(String tag, String msg)
	{
		if (isDebug)
			Log.i(tag, WHO+msg);
	}
	
	
	public static void printHexString(byte[] b, String tag) {
		String temp = tag;
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			temp+=" [" + i + "]= "+hex.toUpperCase();
		//	Log.i("MCU", "my bytes[" + i + "]= " + hex.toUpperCase());
		}
		Log.i("MCU", temp);
	}
}
