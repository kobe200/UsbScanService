/**
 * 
 */

package carnetos.usbservice.util;

import java.io.File;

import carnetos.usbservice.entity.MediaItem.MediaType;

/**
 * @author tang
 */
public class MFile extends File {

	private int value = 0;
	private boolean isScaned = false;
	private MediaType mediaType = MediaType.UNKNOWN;

	public MFile(String path) {
		super(path);
		// TODO Auto-generated constructor stub
	}

	@Override
	public MFile[] listFiles(){
		// TODO Auto-generated method stub
		File[] temp = super.listFiles();
		MFile[] t = new MFile[temp.length];
		for ( int i = 0 ; i < temp.length ; i++ ) {
			t[i] = new MFile(temp[i].getAbsolutePath());
		}
		return t;
	}

	public int getValue(){
		return value;
	}

	public void addValue(int value){
		this.value += value;
	}

	public boolean isScaned(){

		return isScaned;
	}

	public void setScaned(boolean isScaned){

		this.isScaned = isScaned;
	}

	public MediaType getMediaType(){

		return mediaType;
	}

	public void setMediaType(MediaType mediaType){

		this.mediaType = mediaType;
	}

}
