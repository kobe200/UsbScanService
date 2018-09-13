
package carnetos.usbservice.main;

/**
 * 模糊查找接口。
 */
public interface IFuzzySearch {

    /**
     * 查找类型
     */
    public static enum SearchType {
        /**
         * 按标题
         */
        MEDIA_TITLE,

        /**
         * 按专辑
         */
        ALBUM,

        /**
         * 按艺术家
         */
        ARTIST
    };

    /**
     * 查看标题是否匹配
     * 
     * @param mediaTitle 要查找的标题。
     * @return 如果匹配返回true,否则返回false.
     */
    boolean isMatch(String mediaTitle);

    /**
     * Check if a media item is a match by specified type.
     * 
     * @param key
     * @param type SearchType
     * @return true if it is a match.
     */
    boolean isMatch(String key, SearchType type);

}
