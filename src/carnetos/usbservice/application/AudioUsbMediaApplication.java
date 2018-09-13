
package carnetos.usbservice.application;

import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.util.Configuration;


/**
 * 音频应用。
 */
public class AudioUsbMediaApplication extends UsbMediaApplication {



	@Override
	public MediaType getScanMediaType(){
		return MediaType.AUDIO;
	}
	@Override
	public String[] getScanRootPath(){
		return new String[] { Configuration.getInnerSdRootPath(), Configuration.getOuterSdRootPath(), Configuration.getUsbMediaRootPath() };
	}
}
