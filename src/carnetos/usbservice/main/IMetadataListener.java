
package carnetos.usbservice.main;

import carnetos.usbservice.entity.MediaItem;

/**
 * 媒体解析状态监听接口
 */
public interface IMetadataListener {

    /**
     * 前台mediaItem扫描metadata完成。
     * 
     * @param mediaItem 媒体对象
     */
    void onMetadataRetrieved(MediaItem mediaItem);

    /**
     * 后台mediaItem扫描metadata完成。
     * 
     * @param mediaItem 媒体对象
     */
    void onBackgroundMetadataRetrieved(MediaItem mediaItem);
    /**
     * 后台解析多媒体文件完成
     * @param count 多媒体解析数量
     */
    void onAllBkMetadataRetrieved(int count) ;
}
