package carnetos.usbservice.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import android.util.Log;

import carnetos.usbservice.aidl.UsbServiceManager;
import carnetos.usbservice.application.UsbMediaApplication;
import carnetos.usbservice.entity.MediaItem.MediaType;
import carnetos.usbservice.main.UsbMediaStore;
import carnetos.usbservice.main.UsbMediaStore.StoreManagerStatus;
import carnetos.usbservice.util.UsbDevice.UsbDeviceScanState;

public class SortUtil {

	private CopyOnWriteArrayList<MFile> shellSort(CopyOnWriteArrayList<MFile> lstFile){
		for ( int gap = lstFile.size() / 2 ; gap > 0 ; gap /= 2 ) {
			for ( int i = 0 ; i < gap ; i++ ) {
				for ( int j = i + gap ; j < lstFile.size() ; j += gap ) {
					if (lstFile.get(j).getValue() < lstFile.get(j - gap).getValue()) {
						MFile f = lstFile.get(j);
						int k = j - gap;
						while (k >= 0 && lstFile.get(k).getValue() > f.getValue()) {
							lstFile.set(k + gap, lstFile.get(k));
							k -= gap;
						}
						lstFile.set(k + gap, f);
					}
				}
			}
		}
		return lstFile;
	}

	public CopyOnWriteArrayList<MFile> pathSort(String path,MediaType mScanMediaType,int deep){
		MFile root = new MFile(path);
		paths.add(root);
		for ( int j = 0 ; j < deep ; j++ ) {
			CopyOnWriteArrayList<MFile> pathsTemp = new CopyOnWriteArrayList<MFile>();
			for ( int i = 0 ; i < paths.size() ; i++ ) {
				L.d("--- SortUtil_pathSort_deep, size ---" + j + ", " + i);
				MFile[] files = paths.get(i).listFiles();
				if (j == deep - 1)
					pathsTemp.addAll(pathSort(paths.get(i), files, mScanMediaType, false));
				else
					pathsTemp.addAll(pathSort(paths.get(i), files, mScanMediaType, true));
			}
			paths = pathsTemp;
		}
		listFiles.addAll(paths);
		return shellSort(listFiles);
	}

	CopyOnWriteArrayList<MFile> paths = new CopyOnWriteArrayList<MFile>();
	CopyOnWriteArrayList<MFile> listFiles = new CopyOnWriteArrayList<MFile>();

	public List<MFile> pathSort(MFile root,MFile[] files,MediaType mScanMediaType,boolean deep){
		List<MFile> path = new ArrayList<MFile>();
		for ( int i = 0 ; i < files.length ; i++ ) {
//			if (UsbMediaStore.mStoreManagerStatus == StoreManagerStatus.SPEED_LEVEL1) {
//				Log.i("info", " --- SpeedLevel_sleep --- ");
//				try {
//					Thread.currentThread().sleep(80);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
			
			MFile f = files[i];
			
			if (f.getAbsolutePath().contains("MXNavi")) {
				continue;
			}
			
			if (!f.isHidden() && f.canRead()) {
				if (f.isFile()) {
					if (mScanMediaType == MediaType.ALL) {
						if (MediaFile.isMediaFile(f.getAbsolutePath())) {
							MediaType temp = MediaFile.getFileType(f.getAbsolutePath());
							//是媒体文件
							if (UsbServiceManager.getInstance().getUsbMediaStore().getStoreMediaType() == temp) {
								//如果当前媒体文件
								f.addValue(150);
							}
							f.addValue(150);
							if (temp == MediaType.AUDIO)
								f.setMediaType(MediaType.AUDIO);
							else if (temp == MediaType.IMAGE)
								f.setMediaType(MediaType.IMAGE);
							else if (temp == MediaType.VIDEO)
								f.setMediaType(MediaType.VIDEO);
							else if(temp == MediaType.OFFICE)
								f.setMediaType(MediaType.OFFICE);
							listFiles.add(f);
						}
					} else {
						if (MediaFile.getFileType(f.getAbsolutePath()) == mScanMediaType) {
							//是对应文件
							f.addValue(100);
							f.setMediaType(mScanMediaType);
							listFiles.add(f);
						}
					}
				} else if (f.isDirectory()) {
					f.addValue(root.getValue());
					f = checkName(f, mScanMediaType);
					path.add(f);
					if (!deep) {
						listFiles.add(f);
					}
				}
			}
		}
		return path;
	}

	private String[]	musicIndex	= new String[] { "Media", "media", "音乐", "music", "Music", "歌曲", "媒体", "无损", "歌", "酷狗", "ongs", "mp", "Mp", "MP",
			"KuGou"				};
	private String[]	videoIndex	= new String[] { "视频", "video", "Video", "高清", "1080P", "720P", "你懂的", "电影", "mp", "Mp", "MP", "movie", "Movie",
			"flv", "MKV", "avi"	};
	private String[]	imageIndex	= new String[] { "drawable", "JPG", "图片", "照片", "photo", "Photo", "picture", "Picture", "美食", "壁纸", "jpg",
			"wallpaper", "好看", "高清" };
	private String[]	allIndex	= new String[] { "drawable", "Media", "media", "图片", "照片", "photo", "Photo", "picture", "Picture", "美食", "壁纸",
			"jpg", "wallpaper", "好看", "高清", "视频", "video", "Video", "高清", "1080P", "720P", "你懂的", "电影", "mp", "Mp", "MP", "movie", "Movie", "flv",
			"MKV", "avi", "音乐", "music", "Music", "歌曲", "媒体", "无损", "歌", "酷狗", "ongs", "mp", "Mp", "MP", "KuGou" };
	private String[] officeIndex = new String[] { "txt", "pdf", "doc", "xls", "ppt", "docx", "xlsx", "pptx"};


	private boolean checkName(String path,String[] ex){
		for ( int i = 0 ; i < ex.length ; i++ ) {
			if (path.indexOf(ex[i]) != -1) {
				return true;
			}
		}
		return false;
	}

	private MFile checkName(MFile f,MediaType mScanMediaType){
		String path = f.getAbsolutePath();
		if (mScanMediaType == MediaType.AUDIO) {
			if (checkName(path, musicIndex)) {
				f.addValue(10);
				f.setMediaType(mScanMediaType);
			}
		} else if (mScanMediaType == MediaType.IMAGE) {
			if (checkName(path, imageIndex)) {
				f.addValue(10);
				f.setMediaType(mScanMediaType);
			}
		} else if (mScanMediaType == MediaType.VIDEO) {
			if (checkName(path, videoIndex)) {
				f.addValue(10);
				f.setMediaType(mScanMediaType);
			}
		} else if (mScanMediaType == MediaType.OFFICE){
			if (checkName(path, officeIndex)) {
				f.addValue(10);
				f.setMediaType(mScanMediaType);
			}
		} else if (mScanMediaType == MediaType.ALL) {
			if (checkName(path, allIndex)) {
				f.addValue(10);
				if (checkName(path, musicIndex)) {
					if (UsbServiceManager.getInstance().getUsbMediaStore().getStoreMediaType() == MediaType.AUDIO) {
						f.addValue(10);
					}
					f.setMediaType(MediaType.AUDIO);
				} else if (checkName(path, imageIndex)) {
					if (UsbServiceManager.getInstance().getUsbMediaStore().getStoreMediaType() == MediaType.IMAGE) {
						f.addValue(10);
					}
					f.setMediaType(MediaType.IMAGE);
				} else if (checkName(path, videoIndex)) {
					if (UsbServiceManager.getInstance().getUsbMediaStore().getStoreMediaType() == MediaType.VIDEO) {
						f.addValue(10);
					}
					f.setMediaType(MediaType.VIDEO);
				}
			}
		}
		return f;
	}
}
