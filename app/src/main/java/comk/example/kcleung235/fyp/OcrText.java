package comk.example.kcleung235.fyp;

import android.graphics.Typeface;

/**
 * Created by calvinleung on 26/1/2018.
 */

public class OcrText {

    private String text;
    private Typeface typeFace;
    private int textSize;

    public OcrText(){
    }

    public OcrText(char[] charSequence){
        this.text = charToString(charSequence);
    }

    public String charToString(char[] charSequence){
        String result = "";
        for (int i = 0 ; i < charSequence.length ; i++){
            result += charSequence[i];
        }
        return result;
    }

    public void setText( String t ){
        this.text = t ;
    }

    public void getTypeFace(Typeface tf){
        this.typeFace = tf ;
    }

    public void getTextSize(int ts){
        this.textSize = ts ;
    }

    public String getText(){
        return this.text;
    }

    public Typeface getTypeFace(){
        return this.typeFace;
    }

    public int getTextSize(){
        return this.textSize;
    }

}
