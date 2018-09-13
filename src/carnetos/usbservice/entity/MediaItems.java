package carnetos.usbservice.entity;

import java.util.ArrayList;
import java.util.Collection;

import carnetos.usbservice.entity.MediaItem.MediaType;

/**
 * 媒体对象集合
 * 
 * @desc: UsbMedia
 * @author:
 * @createTime: 2016-9-27 下午4:34:07
 * @history:
 * @version: v1.0
 */
@SuppressWarnings("serial")
public class MediaItems extends ArrayList<MediaItem> {

	/**
	 * 该媒体集合是属于那个类型{@link MediaType}
	 */
	private MediaType	type	= MediaType.AUDIO;

	/**
	 * 取得当前 媒体集合的类型
	 * 
	 * @author:
	 * @createTime: 2016-9-27 下午4:39:48
	 * @history:
	 * @return MediaType
	 */
	public MediaType getType(){
		return type;
	}

	/**
	 * 设置当前媒体集合的类型
	 * 
	 * @author:
	 * @createTime: 2016-9-27 下午4:40:04
	 * @history:
	 * @param type
	 *            void
	 */
	public void setType(MediaType type){
		this.type = type;
	}

	/**
	 * 构造方法
	 * 
	 * @param type
	 */
	public MediaItems(MediaType type) {
		this.type = type;
	}

	/**
	 * 增加媒体类型对比，如果不是对应类型返回false
	 */
	@Override
	public boolean add(MediaItem object){
		if (type != object.getMediaType())
			return false;
		return super.add(object);
	}

	/**
	 * 增加媒体类型对比，如果不是对应类型返回false
	 */
	@Override
	public void add(int index,MediaItem object){
		if (type != object.getMediaType())
			return;
		super.add(index, object);
	}

	/**
	 * 增加媒体类型对比，如果不是对应类型返回false,增加的集合只能是{@link MediaItems}
	 */
	@Override
	public boolean addAll(Collection<? extends MediaItem> collection){
		if (!(collection instanceof MediaItems))
			return false;
		if (((MediaItems) collection).getType() != type)
			return false;
		return super.addAll(collection);
	}

	/**
	 * 增加媒体类型对比，如果不是对应类型返回false,增加的集合只能是{@link MediaItems}
	 */
	@Override
	public boolean addAll(int index,Collection<? extends MediaItem> collection){
		if (!(collection instanceof MediaItems))
			return false;
		if (((MediaItems) collection).getType() != type)
			return false;
		return super.addAll(index, collection);

	}

}
