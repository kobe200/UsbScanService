package carnetos.usbservice.main;

import carnetos.usbservice.aidl.UsbService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UsbReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context,Intent intent){
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			context.startService(new Intent(context,UsbService.class));
		}
	}
}
