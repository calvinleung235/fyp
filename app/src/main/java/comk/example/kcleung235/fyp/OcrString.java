package comk.example.kcleung235.fyp;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

/**
 * Created by calvinleung on 9/4/2018.
 */

public class OcrString implements Parcelable{
    private SpannableStringBuilder titleText;
    private SpannableStringBuilder bodyText;
    private SpannableStringBuilder pageNumText;

    public OcrString(SpannableStringBuilder titleText, SpannableStringBuilder bodyText, SpannableStringBuilder pageNumText){
        this.titleText = titleText;
        this.bodyText = bodyText;
        this.pageNumText = pageNumText;
    }

    protected OcrString(Parcel parcel){
        titleText = new SpannableStringBuilder(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel));
        bodyText = new SpannableStringBuilder(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel));
        pageNumText = new SpannableStringBuilder(TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel));
    }

    public static final Creator<OcrString> CREATOR = new Creator<OcrString>() {
        @Override
        public OcrString createFromParcel(Parcel in) {
            return new OcrString(in);
        }

        @Override
        public OcrString[] newArray(int size) {
            return new OcrString[size];
        }
    };

    public SpannableStringBuilder getTitleText() { return titleText; }

    public void setTitleText(SpannableStringBuilder titleText) { this.titleText = titleText; }

    public SpannableStringBuilder getBodyText() {
        return bodyText;
    }

    public void setBodyText(SpannableStringBuilder bodyText) { this.bodyText = bodyText; }

    public SpannableStringBuilder getPageNumText() { return pageNumText; }

    public void setPageNumText(SpannableStringBuilder pageNumText) { this.pageNumText = pageNumText; }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        TextUtils.writeToParcel(titleText, parcel, i);
        TextUtils.writeToParcel(bodyText, parcel, i);
        TextUtils.writeToParcel(pageNumText, parcel, i);
    }
}
