package carnetos.usbservice.util;

import carnetos.usbservice.entity.MediaItem.MediaType;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;

/**
 * 媒体文件常用处理函数（判断文件类型，取得文件名）
 */
public class MediaFile {

	private static HashMap<String, MediaType>	mMediaTypeMap;

	static {
		mMediaTypeMap = new HashMap<String, MediaType>();

		/* Add Audio File Type */
		mMediaTypeMap.put("MP3", MediaType.AUDIO);
		mMediaTypeMap.put("WAV", MediaType.AUDIO);
		mMediaTypeMap.put("FLAC", MediaType.AUDIO);
        mMediaTypeMap.put("AMR", MediaType.AUDIO);
        mMediaTypeMap.put("AWB", MediaType.AUDIO);
        mMediaTypeMap.put("APE", MediaType.AUDIO);
		
		mMediaTypeMap.put("MKA", MediaType.AUDIO);
		mMediaTypeMap.put("M4A", MediaType.AUDIO);
		mMediaTypeMap.put("ADTS", MediaType.AUDIO);
//		mMediaTypeMap.put("OGG", MediaType.AUDIO);
		mMediaTypeMap.put("AAC", MediaType.AUDIO);
		mMediaTypeMap.put("MID", MediaType.AUDIO);
		mMediaTypeMap.put("MIDI", MediaType.AUDIO);
		mMediaTypeMap.put("XMF", MediaType.AUDIO);
		mMediaTypeMap.put("RTTTL", MediaType.AUDIO);
		mMediaTypeMap.put("SMF", MediaType.AUDIO);
		mMediaTypeMap.put("IMY", MediaType.AUDIO);
		mMediaTypeMap.put("RTX", MediaType.AUDIO);
		mMediaTypeMap.put("OTA", MediaType.AUDIO);
		mMediaTypeMap.put("WMA", MediaType.AUDIO);
		mMediaTypeMap.put("RA", MediaType.AUDIO);
		mMediaTypeMap.put("M3U", MediaType.AUDIO);
		mMediaTypeMap.put("PLS", MediaType.AUDIO);

		/* Add Video File Type */
		mMediaTypeMap.put("MP4", MediaType.VIDEO);
		mMediaTypeMap.put("MOV", MediaType.VIDEO);
//        mMediaTypeMap.put("M4A", MediaType.VIDEO);
        mMediaTypeMap.put("F4V", MediaType.VIDEO);
        mMediaTypeMap.put("3GP", MediaType.VIDEO);
        mMediaTypeMap.put("AVI", MediaType.VIDEO);
        mMediaTypeMap.put("MKV", MediaType.VIDEO);
//        mMediaTypeMap.put("MKA", MediaType.VIDEO);
        mMediaTypeMap.put("FLV", MediaType.VIDEO);
        mMediaTypeMap.put("MPG", MediaType.VIDEO);
//        mMediaTypeMap.put("VOB", MediaType.VIDEO);
        mMediaTypeMap.put("TS", MediaType.VIDEO);
        mMediaTypeMap.put("M2TS", MediaType.VIDEO);
        mMediaTypeMap.put("WEBM", MediaType.VIDEO);
        mMediaTypeMap.put("RMVB", MediaType.VIDEO);
		
        mMediaTypeMap.put("MPEG", MediaType.VIDEO);
		mMediaTypeMap.put("M4V", MediaType.VIDEO);
        mMediaTypeMap.put("3GPP2", MediaType.VIDEO);
        mMediaTypeMap.put("DIVX", MediaType.VIDEO);
        mMediaTypeMap.put("ADTS", MediaType.VIDEO);
		mMediaTypeMap.put("WMV", MediaType.VIDEO);
		mMediaTypeMap.put("ASF", MediaType.VIDEO);

		/* Add Image File Type */
		mMediaTypeMap.put("JPG", MediaType.IMAGE);
		mMediaTypeMap.put("JPEG", MediaType.IMAGE);
		mMediaTypeMap.put("GIF", MediaType.IMAGE);
		mMediaTypeMap.put("PNG", MediaType.IMAGE);
		mMediaTypeMap.put("BMP", MediaType.IMAGE);
		mMediaTypeMap.put("WBMP", MediaType.IMAGE);
		

	}
	
	public static void addOtherTpye() {
		
		/* Add Office file Type */
		mMediaTypeMap.put("TXT", MediaType.OFFICE);
		mMediaTypeMap.put("PDF", MediaType.OFFICE);
		mMediaTypeMap.put("DOC", MediaType.OFFICE);
		mMediaTypeMap.put("XLS", MediaType.OFFICE);
		mMediaTypeMap.put("PPT", MediaType.OFFICE);
		mMediaTypeMap.put("DOCX", MediaType.OFFICE);
		mMediaTypeMap.put("XLSX", MediaType.OFFICE);
		mMediaTypeMap.put("PPTX", MediaType.OFFICE);
		
		//mMediaTypeMap.put("ZIP", MediaType.OFFICE);
		//mMediaTypeMap.put("RAR", MediaType.OFFICE);
		//mMediaTypeMap.put("APK", MediaType.OFFICE);
		//mMediaTypeMap.put("HTML", MediaType.OFFICE);
		//mMediaTypeMap.put("EXE", MediaType.OFFICE);
		//mMediaTypeMap.put("LOG", MediaType.OFFICE);
	}

	/**
	 * 取得文件类型
	 * 
	 * @param path
	 *            文件
	 * @return MediaType {UNKNOWN(0), VIDEO(1), AUDIO(2), IMAGE(3)}
	 */
	public static MediaType getFileType(String path){
		MediaType mediaType = MediaType.UNKNOWN;
		int lastDot = path.lastIndexOf(".");
		String fileExtend = null;
		if (lastDot > 0) {
			fileExtend = path.substring(lastDot + 1).toUpperCase(Locale.US);
			if (mMediaTypeMap.containsKey(fileExtend)) {
				mediaType = mMediaTypeMap.get(fileExtend);
			}
		}

		return mediaType;
	}

	/**
	 * 取得文件类型
	 * 
	 * @param file
	 *            文件
	 * @return MediaType {UNKNOWN(0), VIDEO(1), AUDIO(2), IMAGE(3)}
	 */
	public static MediaType getFileType(File file){
		return getFileType(file.getAbsolutePath());
	}

	/**
	 * 判断是否为图片文件
	 * 
	 * @param path
	 *            文件
	 * @return true:是图片文件, false:不是图片文件
	 */
	public static boolean isImageFile(String path){
		return getFileType(path) == MediaType.IMAGE;
	}

	/**
	 * 判断是否为图片文件
	 * 
	 * @param file
	 *            文件
	 * @return true:是图片文件, false:不是图片文件
	 */
	public static boolean isImageFile(File file){
		return getFileType(file) == MediaType.IMAGE;
	}



	/**
	 * 判断是否为音频文件
	 * 
	 * @param path
	 *            文件
	 * @return true:是音频文件, false:不是音频文件
	 */
	public static boolean isAudioFile(String path){
		return getFileType(path) == MediaType.AUDIO;
	}

	/**
	 * 判断是否为音频文件
	 * 
	 * @param file
	 *            文件
	 * @return true:是音频文件, false:不是音频文件
	 */
	public static boolean isAudioFile(File file){
		return getFileType(file) == MediaType.AUDIO;
	}

	/**
	 * 判断是否为视频文件
	 * 
	 * @param path
	 *            文件
	 * @return true:是视频文件, false:不是视频文件
	 */
	public static boolean isVideoFile(String path){
		return getFileType(path) == MediaType.VIDEO;
	}

	/**
	 * 判断是否为视频文件
	 * 
	 * @param file
	 *            文件
	 * @return true:是视频文件, false:不是视频文件
	 */
	public static boolean isVideoFile(File file){
		return getFileType(file) == MediaType.VIDEO;
	}
	
	/**
	 * 判断是否为办公文件
	 * 
	 * @param file
	 *            文件
	 * @return true:是办公文件, false:不是办公文件
	 */
	public static boolean isOfficeFile(File file) {
		return getFileType(file) == MediaType.OFFICE;
	}
	
	/**
	 * 判断是否为办公文件
	 * 
	 * @param path
	 *            文件
	 * @return true:是办公文件, false:不是办公文件
	 */
	public static boolean isOfficeFile(String path) {
		return getFileType(path) == MediaType.OFFICE;
	}

	/**
	 * 判断是否为媒体文件
	 * 
	 * @param path
	 *            文件
	 * @return true:是媒体文件, false:不是媒体文件
	 */
	public static boolean isMediaFile(String path){
		return getFileType(path) != MediaType.UNKNOWN;
	}

	/**
	 * 判断是否为媒体文件
	 * 
	 * @param file
	 *            文件
	 * @return true:是媒体文件, false:不是媒体文件
	 */
	public static boolean isMediaFile(File file){
		return getFileType(file) != MediaType.UNKNOWN;
	}

	/**
	 * 取得文件显示用title内容
	 * 
	 * @param path
	 *            文件名
	 * @return 文件Title
	 */
	public static String getFileTitle(String path){
		/* 代码实现考虑性能问题所以没有使用File对象等进行代码精简 */

		// extract file name after last slash
		int lastSlash = path.lastIndexOf('/');
		if (lastSlash >= 0) {
			lastSlash++;
			if (lastSlash < path.length()) {
				path = path.substring(lastSlash);
			}
		}
		// truncate the file extension (if any)
		int lastDot = path.lastIndexOf('.');
		if (lastDot > 0) {
			path = path.substring(0, lastDot);
		}
		return path;
	}

	/**
	 * 取得文件显示用title内容
	 * 
	 * @param file
	 *            文件
	 * @return 文件Title
	 */
	public static String getFileTitle(File file){
		return getFileTitle(file.getAbsolutePath());
	}

}
