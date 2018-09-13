
package carnetos.usbservice.entity;

import carnetos.usbservice.main.IFuzzySearch;

import java.util.Locale;

/**
 * 专辑对象类
 */
public class AlbumItem implements IFuzzySearch {

    /**
     * 专辑ID
     */
    private final long mID;

    /**
     * 专辑名
     */
    private final String mAlbum;

    /**
     * 专辑对应的媒体总数
     */
    private final int mItemsCount;

    public AlbumItem(long id, String albumName, int itemsCount) {
        mID = id;
        mAlbum = albumName;
        mItemsCount = itemsCount;
    }

    public long getID() {
        return mID;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public int getItemsCount() {
        return mItemsCount;
    }

    /*
     * (non-Javadoc)
     * @see com.neusoft.bsd.libusbmedia.IFuzzySearch#isMatch(java.lang.String)
     */
    @Override
    public boolean isMatch(String mediaTitle) {
        boolean isMatch = false;

        if (mAlbum != null && mediaTitle != null && !mediaTitle.isEmpty()) {
            isMatch = mAlbum.toLowerCase(Locale.US).contains(mediaTitle.toLowerCase(Locale.US));
        }

        return isMatch;
    }

    /*
     * (non-Javadoc)
     * @see com.neusoft.bsd.libusbmedia.IFuzzySearch#isMatch(java.lang.String,
     * com.neusoft.bsd.libusbmedia.IFuzzySearch.SearchType)
     */
    @Override
    public boolean isMatch(String key, SearchType type) {
        boolean isMatch = false;

        if (key != null) {
            if (type == SearchType.MEDIA_TITLE) {
                isMatch = isMatch(key);
            } else if (type == SearchType.ALBUM && mAlbum != null) {
                isMatch = mAlbum.toLowerCase(Locale.US).contains(key.toLowerCase(Locale.US));
            }
        }

        return isMatch;
    }

}
