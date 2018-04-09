package comk.example.kcleung235.fyp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class ExampleDialog extends AppCompatDialogFragment {
    private TextView text;
    private EditText editText;
    private ExampleDialogListener listener;

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View mView = inflater.inflate(R.layout.edit_text_dialog,null);

        editText = mView.findViewById(R.id.editText);
        final Bundle bundle = getArguments();

        builder.setView(mView)
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String textString = editText.getText().toString();
                        int selStart = bundle.getInt("selStart");
                        int selEnd = bundle.getInt("selEnd");
                        listener.applyText(textString, selStart, selEnd);
                    }
                });

        CharSequence charSequence = bundle.getCharSequence("selectedText");
        text = mView.findViewById(R.id.text);
        text.setText(charSequence);

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        listener = (ExampleDialogListener) context;
    }

    public interface ExampleDialogListener{
        void applyText(String text, int selStart, int selEnd);
    }

}
