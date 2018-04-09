package comk.example.kcleung235.fyp;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity implements ExampleDialog.ExampleDialogListener{

    TextView body;
    TextView title;
    TextView pageNum;
    SpannableStringBuilder titleText;
    SpannableStringBuilder bodyText;
    SpannableStringBuilder pageNumText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        body = findViewById(R.id.body);
        title = findViewById(R.id.title);
        pageNum = findViewById(R.id.pageNumber);

        OcrString ocrString = getIntent().getParcelableExtra("ocrString");

        titleText = ocrString.getTitleText();
        bodyText = ocrString.getBodyText();
        pageNumText = ocrString.getPageNumText();

        title.setText(titleText);
        body.setText(bodyText);
        pageNum.setText(pageNumText);

        ActionMode.Callback callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater menuInflater = getMenuInflater();
                menuInflater.inflate(R.menu.main_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                menu.removeItem(android.R.id.selectAll);
                menu.removeItem(android.R.id.cut);
                menu.removeItem(android.R.id.copy);
                menu.removeItem(android.R.id.shareText);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.ammendText:{
                        final CharSequence selectedText;
                        final int selStart;
                        final int selEnd;
                        TextView currentTextView = (TextView)getCurrentFocus();
                        selStart = currentTextView.getSelectionStart();
                        selEnd = currentTextView.getSelectionEnd();
                        selectedText = currentTextView.getText().subSequence(selStart, selEnd);
                        Bundle bundle = new Bundle();
                        bundle.putCharSequence("selectedText", selectedText);
                        bundle.putInt("selStart", selStart);
                        bundle.putInt("selEnd", selEnd);

                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction ft = fragmentManager.beginTransaction();

                        ExampleDialog dialog = new ExampleDialog();
                        dialog.setArguments(bundle);
                        dialog.show(getSupportFragmentManager(), "dialog");

                        ft.commit();
                        actionMode.finish();
                        return true;
                    }
                    case R.id.bold:{
                        TextView currentTextView = (TextView)getCurrentFocus();
                        final int selStart = currentTextView.getSelectionStart();
                        final int selEnd = currentTextView.getSelectionEnd();

                        if ( currentTextView == title ){
                            for (int i = selStart ; i < selEnd ; i++){
                                titleText.setSpan(new StyleSpan(Typeface.BOLD), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(titleText);
                        } else if ( currentTextView == body ){
                            for (int i = selStart ; i < selEnd ; i++){
                                bodyText.setSpan(new StyleSpan(Typeface.BOLD), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(bodyText);
                        } else {
                            for (int i = selStart ; i < selEnd ; i++){
                                pageNumText.setSpan(new StyleSpan(Typeface.BOLD), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(pageNumText);
                        }
                        actionMode.finish();
                        return true;
                    }
                    case R.id.italic:{
                        TextView currentTextView = (TextView)getCurrentFocus();
                        final int selStart = currentTextView.getSelectionStart();
                        final int selEnd = currentTextView.getSelectionEnd();

                        if ( currentTextView == title ){
                            for (int i = selStart ; i < selEnd ; i++){
                                titleText.setSpan(new StyleSpan(Typeface.ITALIC), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(titleText);
                        } else if ( currentTextView == body ){
                            for (int i = selStart ; i < selEnd ; i++){
                                bodyText.setSpan(new StyleSpan(Typeface.ITALIC), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(bodyText);
                        } else {
                            for (int i = selStart ; i < selEnd ; i++){
                                pageNumText.setSpan(new StyleSpan(Typeface.ITALIC), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(pageNumText);
                        }
                        actionMode.finish();
                        return true;
                    }
                    case R.id.underline:{
                        TextView currentTextView = (TextView)getCurrentFocus();
                        final int selStart = currentTextView.getSelectionStart();
                        final int selEnd = currentTextView.getSelectionEnd();

                        if ( currentTextView == title ){
                            for (int i = selStart ; i < selEnd ; i++){
                                titleText.setSpan(new UnderlineSpan(), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(titleText);
                        } else if ( currentTextView == body ){
                            for (int i = selStart ; i < selEnd ; i++){
                                bodyText.setSpan(new UnderlineSpan(), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(bodyText);
                        } else {
                            for (int i = selStart ; i < selEnd ; i++){
                                pageNumText.setSpan(new UnderlineSpan(), i, i+1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                            currentTextView.setText(pageNumText);
                        }
                        actionMode.finish();
                        return true;
                    }
                    case R.id.reset:{
                        TextView currentTextView = (TextView)getCurrentFocus();
                        final int selStart = currentTextView.getSelectionStart();
                        final int selEnd = currentTextView.getSelectionEnd();

                        if ( currentTextView == title ){
                            Object spansToRemove[] = titleText.getSpans(selStart, selEnd, Object.class);
                            for(Object span: spansToRemove){
                                if(span instanceof CharacterStyle) {
                                    titleText.removeSpan(span);
                                }
                            }
                            currentTextView.setText(titleText);
                        } else if ( currentTextView == body ){
                            Object spansToRemove[] = bodyText.getSpans(selStart, selEnd, Object.class);
                            for(Object span: spansToRemove){
                                if(span instanceof CharacterStyle) {
                                    bodyText.removeSpan(span);
                                }
                            }
                            currentTextView.setText(bodyText);
                        } else {
                            Object spansToRemove[] = pageNumText.getSpans(selStart, selEnd, Object.class);
                            for(Object span: spansToRemove){
                                if(span instanceof CharacterStyle){
                                    pageNumText.removeSpan(span);
                                }
                            }
                            currentTextView.setText(pageNumText);
                        }
                        actionMode.finish();
                        return true;
                    }
                    case R.id.delete:{
                        TextView currentTextView = (TextView)getCurrentFocus();
                        final int selStart = currentTextView.getSelectionStart();
                        final int selEnd = currentTextView.getSelectionEnd();

                        if ( currentTextView == title ){
                            if (titleText.subSequence(selEnd, selEnd+1).equals(" ")){
                                titleText.delete(selStart, selEnd+1);
                            } else {
                                titleText.delete(selStart, selEnd);
                            }
                        } else if ( currentTextView == body ){
                            if (bodyText.subSequence(selEnd, selEnd+1).equals(" ")){
                                bodyText.delete(selStart, selEnd+1);
                            } else {
                                bodyText.delete(selStart, selEnd);
                            }
                            currentTextView.setText(bodyText);
                        } else {
                            if (pageNumText.subSequence(selEnd, selEnd+1).equals(" ")){
                                pageNumText.delete(selStart, selEnd+1);
                            } else {
                                pageNumText.delete(selStart, selEnd);
                            }
                            currentTextView.setText(pageNumText);
                        }
                        actionMode.finish();
                        return true;
                    }
                    default: return false;
                }
            }
            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
            }
        };
        title.setCustomSelectionActionModeCallback(callback);
        body.setCustomSelectionActionModeCallback(callback);
        pageNum.setCustomSelectionActionModeCallback(callback);
    }

    @Override
    public void applyText(String text, int selStart, int selEnd) {
        if (title.isFocused()) {

            titleText.replace(selStart, selEnd, text);
            title.setText(titleText);
        } else if (body.isFocused()) {

            bodyText.replace(selStart, selEnd, text);
            body.setText(bodyText);
        } else {

            pageNumText.replace(selStart, selEnd, text);
            pageNum.setText(pageNumText);
        }
    }
}
