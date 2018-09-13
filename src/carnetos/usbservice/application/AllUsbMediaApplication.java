package carnetos.usbservice.application;

import java.io.File;
import java.util.List;

import android.content.Intent;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import carnetos.usbservice.aidl.UsbService;
import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.util.Configuration;
import carnetos.usbservice.util.L;

public class AllUsbMediaApplication extends UsbMediaApplication {

	@Override
	public MediaType getScanMediaType(){
		return MediaType.ALL;
	}

	@Override
	public void onCreate(){
		super.onCreate();
		L.d("AllUsbMediaApplication onCreate");
		startService(new Intent(getApplicationContext(),UsbService.class));
	}

	@Override
	public String[] getScanRootPath(){
		return new String[] { Configuration.getInnerSdRootPath(), Configuration.getOuterSdRootPath(), Configuration.getUsbMediaRootPath() };
	}



}