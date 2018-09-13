package carnetos.usbservice.util;

public class UsbDevice {

	private boolean isMount = false;
	private String rootPath = null;
	private UsbDeviceScanState scanState = UsbDeviceScanState.NONE;


	public enum UsbDeviceScanState {
		NONE, SCANING, FINISH
	}

	public UsbDevice(String rootPath, boolean isMount) {
		this.rootPath = rootPath;
		this.isMount = isMount;
	}

	public UsbDeviceScanState getScanState(){

		return scanState;
	}

	public void setScanState(UsbDeviceScanState scanState){

		this.scanState = scanState;
	}

	public boolean isMount(){
		return isMount;
	}

	public void setMount(boolean isMount){
		this.isMount = isMount;
	}

	public String getRootPath(){
		return rootPath;
	}

	public void setRootPath(String rootPath){
		this.rootPath = rootPath;
	}

}
