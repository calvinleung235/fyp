package comk.example.kcleung235.fyp.Models;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import comk.example.kcleung235.fyp.R;

public class AmmendTextDialog extends AppCompatDialogFragment {
    private TextView text;
    private EditText editText;
    private AmmendTextDialogListener listener;

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View mView = inflater.inflate(R.layout.edit_text_dialog,null);

        final Bundle bundle = getArguments();
        CharSequence charSequence = bundle.getCharSequence("selectedText");

        editText = mView.findViewById(R.id.editText);
        text = mView.findViewById(R.id.text);
        text.setText(charSequence);

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

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        listener = (AmmendTextDialogListener) context;
    }

    public interface AmmendTextDialogListener {
        void applyText(String text, int selStart, int selEnd);
    }

}
