package comk.example.kcleung235.fyp.Models;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;

import java.util.ArrayList;

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


//
//    private String computeSectionTitle(ArrayList<String> contentList, ArrayList<String> paragraph){
//        ArrayList<Integer> titleIndexList = new ArrayList<>();
//        String title = "";
//
//        for ( int i = 0; i < contentList.size(); i++ ){
//            String titleString = getTextStringFromTextline(contentList.get(i).replace(" ", ""));
//            TextView tv = findViewById(R.id.body);
//            tv.setText(titleString);
//
//            for ( int j = 0; j < paragraph.size(); j++ ){
//                String currentParagraphWord = getTextStringFromTextline(paragraph.get(j));
//
//                if ( titleString.contains(currentParagraphWord) ) {
//                    if (titleIndexList.isEmpty()){
//                        title += currentParagraphWord;
//                        titleIndexList.add(j);
//                        continue;
//                    }
//
//                    String lastWordInTitleString = paragraph.get(titleIndexList.get(titleIndexList.size()-1));
//                    lastWordInTitleString = getTextStringFromTextline(lastWordInTitleString);
//                    boolean inTitleContinuous = (titleString.indexOf(currentParagraphWord) > titleString.indexOf(lastWordInTitleString));
//
//                    int currentIndex = j;
//                    int indexOfLastTitleWordInPara = titleIndexList.get(titleIndexList.size()-1);
//                    boolean inParaContinuous = (currentIndex - indexOfLastTitleWordInPara == 1);
//
//                    boolean duplicated = false;
//                    for (int k = 0; k < titleIndexList.size() ; k++){
//                        int index = titleIndexList.get(k);
//                        String retrievedTitleWord = paragraph.get(index);
//                        retrievedTitleWord = getTextStringFromTextline(retrievedTitleWord);
//
//                        duplicated = retrievedTitleWord.equals(currentParagraphWord);
//                    }
//
//                    if (!duplicated && inTitleContinuous && inParaContinuous) {
//                        title += currentParagraphWord;
//                        titleIndexList.add(j);
//                    }
//                }
//            }
//
//            if (titleString.equals(title)) {
////                TextView tv1 = findViewById(R.id.ocrText2);
////                tv1.setText(title);
//                break;
//            }
//            else {
//                title = "";
//                titleIndexList.clear();
//            }
//        }
//
//        for (int x = 0; x < titleIndexList.size(); x++){
//            paragraph.remove(titleIndexList.get(x));
//        }
//        return title;
//    }
//
//    private int getPageNumberIndex(ArrayList<SpannableString> contentList, ArrayList<SpannableString> paragraphString) {
//        for (int i = 0; i < contentList.size() - 1; i++) {
//            String contentSectionString = contentList.get(i).toString();
//            String sectionPageNum = getNumStringFromTextline(contentSectionString);
//
//            String nextContenSectionString = contentList.get(i + 1).toString();
//            String nextSectionPageNum = getNumStringFromTextline(nextContenSectionString);
//
//            for (int j = 0; j < paragraphString.size(); j++) {
//                String pageString = paragraphString.get(j).toString();
//                String pageNumString = getNumStringFromTextline(pageString);
//
//                if (pageNumString != null && sectionPageNum != null && nextSectionPageNum != null) {
//                    if (Integer.valueOf(pageNumString) >= Integer.valueOf(sectionPageNum)
//                            && Integer.valueOf(pageNumString) < Integer.valueOf(nextSectionPageNum)) {
//
//                        return j;
//                    }
//                }
//            }
//        }
//        return -1;
//    }
//
//    private String getNumStringFromTextline(String srcWordString) {
//        int startIndex = -1;
//        int endIndex = -1;
//
//        String wordString = srcWordString.toLowerCase().replace(" ", "");
//
//        for (int i = 0 ; i< wordString.length(); i++){
//            if ( isNum(wordString.charAt(i)) ){
//                if ( startIndex == -1 ){
//                    startIndex = i;
//                }
//                else if ( consistText(wordString, startIndex, i) ){
//                    startIndex = i;
//                }
//                else {
//                    endIndex = i + 1;
//                }
//            } else {
//                wordString.replace(wordString.charAt(i), 'X');
//            }
//        }
//
//        if ( startIndex != -1 && endIndex != -1) {
//            String numString = wordString.substring(startIndex, endIndex);
//
//            if (consistText(numString, 0, numString.length())) {
//                return null;
//            }
//
//            return numString;
//        }
//        return null;
//    }
//
//    private String getTextStringFromTextline(String srcWordString) {
//        int startIndex = -1;
//        int endIndex = -1;
//
//        String wordString = srcWordString.toLowerCase();
//
//        for (int i = 0 ; i< wordString.length(); i++){
//            char in = wordString.charAt(i);
//            if ( in >= 'a' && in <= 'z' ){
//                if ( startIndex == -1 ){
//                    startIndex = i;
//                }
//                if ( startIndex != -1){
//                    endIndex = i + 1;
//                }
//            } else {
//                wordString.replace(wordString.charAt(i), ' ');
//            }
//        }
//
//        if ( startIndex != -1 && endIndex != -1) {
//            String textString = wordString.substring(startIndex, endIndex).replace(" ", "");
//            return textString;
//        }
//
//        return "";
//    }

}
