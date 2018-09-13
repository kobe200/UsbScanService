
package carnetos.usbservice.application;

import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.util.Configuration;


/**
 * 图片应用。
 */
public class ImageUsbMediaApplication extends UsbMediaApplication {


	@Override
	public MediaType getScanMediaType(){
		return MediaType.IMAGE;
	}
	@Override
	public String[] getScanRootPath(){
		return new String[] { Configuration.getInnerSdRootPath(), Configuration.getOuterSdRootPath(), Configuration.getUsbMediaRootPath() };
	}
}
