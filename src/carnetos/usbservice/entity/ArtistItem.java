
package carnetos.usbservice.entity;

import carnetos.usbservice.main.IFuzzySearch;

import java.util.Locale;

/**
 * 艺术家对象类
 */
public class ArtistItem implements IFuzzySearch {

    /**
     * 艺术家ID
     */
    private final long mID;

    /**
     * 艺术家
     */
    private final String mArtist;

    /**
     * 艺术家对应的媒体总数
     */
    private final int mItemsCount;

    public ArtistItem(long id, String artist, int itemsCount) {
        mID = id;
        mArtist = artist;
        mItemsCount = itemsCount;
    }

    public String getArtist() {
        return mArtist;
    }

    public long getID() {
        return mID;
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

        if (mArtist != null && mediaTitle != null && !mediaTitle.isEmpty()) {
            isMatch = mArtist.toLowerCase(Locale.US).contains(mediaTitle.toLowerCase(Locale.US));
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
            } else if (type == SearchType.ARTIST && mArtist != null) {
                isMatch = mArtist.toLowerCase(Locale.US).contains(key.toLowerCase(Locale.US));
            }
        }

        return isMatch;
    }

}
