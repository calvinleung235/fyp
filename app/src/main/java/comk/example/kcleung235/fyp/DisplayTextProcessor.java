package comk.example.kcleung235.fyp;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;

import java.util.ArrayList;

/**
 * Created by calvinleung on 8/4/2018.
 */

public class DisplayTextProcessor {

    public DisplayTextProcessor(){};

    public SpannableStringBuilder getPageNumText(ArrayList<SpannableString> contentList, ArrayList<SpannableString> paragraphString){
        return new SpannableStringBuilder(paragraphString.get(computePageNumIndex(contentList, paragraphString)).toString());
    }

    public SpannableStringBuilder getTitleText(ArrayList<SpannableString> contentList, ArrayList<SpannableString> paragraphString){
        String title = "";

        int pageNumIndex = computePageNumIndex(contentList, paragraphString);
        if (pageNumIndex != -1) {
            for (int i = 0; i < pageNumIndex; i++) {
                title += paragraphString.get(i).toString() + " ";
            }
        }
        return new SpannableStringBuilder(title.substring(0, title.length()));
    }

    public SpannableStringBuilder getBodyText (ArrayList<SpannableString> contentList, ArrayList<SpannableString> paragraphString){
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        int pageNumIndex = computePageNumIndex(contentList, paragraphString);
        for ( int i = 0; i < paragraphString.size() ; i++ ){
            if (i <= pageNumIndex) continue;

            spannableStringBuilder.append(paragraphString.get(i));
            spannableStringBuilder.append(" ");
        }

        return spannableStringBuilder;
    }

    private int computePageNumIndex(ArrayList<SpannableString> contentList, ArrayList<SpannableString> paragraphString) {
        for (int i = 0; i < contentList.size() - 1; i++) {
            String contentSectionString = contentList.get(i).toString();
            String sectionPageNum = computeNumStringFromTextline(contentSectionString);

            String nextContenSectionString = contentList.get(i + 1).toString();
            String nextSectionPageNum = computeNumStringFromTextline(nextContenSectionString);

            for (int pageNumIndex = 0; pageNumIndex < paragraphString.size(); pageNumIndex++) {
                String pageString = paragraphString.get(pageNumIndex).toString();
                String pageNumString = computeNumStringFromTextline(pageString);

                if (pageNumString != null && sectionPageNum != null && nextSectionPageNum != null) {
                    if (Integer.valueOf(pageNumString) >= Integer.valueOf(sectionPageNum)
                            && Integer.valueOf(pageNumString) < Integer.valueOf(nextSectionPageNum)) {

                        return pageNumIndex;
                    }
                }
            }
        }
        return -1;
    }

    private String computeNumStringFromTextline(String srcWordString) {
        int startIndex = -1;
        int endIndex = -1;

        String wordString = srcWordString.toLowerCase().replace(" ", "");

        for (int i = 0 ; i< wordString.length(); i++){
            if ( isNum(wordString.charAt(i)) ){
                if ( startIndex == -1 ){
                    startIndex = i;
                }
                else if ( consistText(wordString, startIndex, i) ){
                    startIndex = i;
                }
                else {
                    endIndex = i + 1;
                }
            } else {
                wordString.replace(wordString.charAt(i), 'X');
            }
        }

        if ( startIndex != -1 && endIndex != -1) {
            String numString = wordString.substring(startIndex, endIndex);

            if (consistText(numString, 0, numString.length())) {
                return null;
            }

            return numString;
        }
        return null;
    }

    private boolean isNum(char c){
        if (c >= '0' && c <= '9' ){
            return true;
        } else {
            return false;
        }
    }

    private boolean consistText(String arr, int startIndex, int currentIndex){
        for ( int i = startIndex ; i < currentIndex ; i++){
            if ( !isNum(arr.charAt(i)) ){
                return true;
            }
        }

        return false;
    }

}
