package carnetos.usbservice.application;

import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.util.Configuration;

/**
 * 视频应用。
 */
public class VideoUsbMediaApplication extends UsbMediaApplication {

	@Override
	public MediaType getScanMediaType(){
		return MediaType.VIDEO;
	}

	@Override
	public String[] getScanRootPath(){
		return new String[] { Configuration.getInnerSdRootPath(), Configuration.getOuterSdRootPath(), Configuration.getUsbMediaRootPath() };
	}
}
