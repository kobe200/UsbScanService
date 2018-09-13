
package carnetos.usbservice.main;

import carnetos.usbservice.entity.MediaItem;

/**
 * 媒体Metadata扫描接口定义。
 */
public interface IMedatataRetriever {

    /**
     * 注册Metadata监听。
     * 
     * @param listener IMetadataListener实现类
     */
    void addIMetadataListener(IMetadataListener listener);

    /**
     * 注销Metadata监听。
     * 
     * @param listener IMetadataListener实现类
     */
    void removeIMetadataListener(IMetadataListener listener);

    /**
     * 获取媒体的metadata,获取完成后通知相关监听者. 优先扫描该媒体。当mediaItem.getParseStatus() ==
     * ParseStatus.NONE的媒体才可以加入。
     * 
     * @param mediaItem 媒体对象
     */
    void retrieveMetadata(MediaItem mediaItem);

}
