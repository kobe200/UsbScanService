package carnetos.usbservice.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import carnetos.usbservice.entity.MediaScanConfig;

import android.util.Log;


public class MediaScanConfigManager {

	//1：扫描非媒体文件  2：不扫描非媒体文件
	
	public static final int SCAN_OTHER_FILE = 1;
	public static final int NOT_SCAN_OTHER_FILE = 2;

	private static final String mediaScanConfigPath = "/system/etc/media_scan_config.xml";
	
	public static void init() {
		List<MediaScanConfig> mediaScanConfigInfos;
		mediaScanConfigInfos = getMediaScanConfigInfos();
		
		if (mediaScanConfigInfos != null) {
			if (mediaScanConfigInfos.get(0).getValue() == SCAN_OTHER_FILE) {
				MediaFile.addOtherTpye();
			}
		}
	}
	
	public static List<MediaScanConfig> getMediaScanConfigInfos() {
		List<MediaScanConfig> carLevelInfos = null;
		try {
			File CarLevelFile = new File(mediaScanConfigPath);
			InputStream is = new FileInputStream(CarLevelFile);
			carLevelInfos = MediaScanConfigManager.parseMediaScanConfig(is);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return carLevelInfos;
	}
	
	public static List<MediaScanConfig> parseMediaScanConfig(InputStream inputStream) throws Exception {
		List<MediaScanConfig> mediaScanConfigInfos = null;
		MediaScanConfig mediaScanConfigInfo = null;

		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		XmlPullParser parser = factory.newPullParser();
		parser.setInput(inputStream, "utf-8"); // 设置输入流 并指明编码方式

		int eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			switch (eventType) {
				case XmlPullParser.START_DOCUMENT :
					mediaScanConfigInfos = new ArrayList<MediaScanConfig>();
					break;
				case XmlPullParser.START_TAG :
					if ("item".equals(parser.getName())) {
						mediaScanConfigInfo = new MediaScanConfig();
					}
					if (parser.getName().equals("item_name")) {
						mediaScanConfigInfo.setName(parser.nextText());
					}
					if (parser.getName().equals("item_value")) {
						mediaScanConfigInfo.setValue(Integer.parseInt(parser.nextText()));
					}
					break;
				case XmlPullParser.END_TAG :
					if (parser.getName().equals("item")) {
						Log.i("info", "--- MediaScanConfigManager_mediaScanConfigInfo ---" + mediaScanConfigInfo.toString());
						mediaScanConfigInfos.add(mediaScanConfigInfo);
						mediaScanConfigInfo = null;
					}
					break;
			}
			eventType = parser.next();
		}
		return mediaScanConfigInfos;
	}

}
