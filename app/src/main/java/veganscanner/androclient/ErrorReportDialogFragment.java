package veganscanner.androclient;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

public class ErrorReportDialogFragment extends DialogFragment {
    private Listener listener;

    public static ErrorReportDialogFragment create(final Listener listener) {
        final ErrorReportDialogFragment instance = new ErrorReportDialogFragment();
        instance.listener = listener;
        return instance;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
        // TODO: this is a bad way to tell the information, Activity instead of a Dialog would solve the problem
        Toast.makeText(
                getActivity(),
                R.string.error_report_dialog_info_toast,
                Toast.LENGTH_SHORT)
                .show();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.error_report_dialog_title);

        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listener != null) {
                    listener.onSendClicked(input.getText().toString());
                }
            }
        });
        //"Отменить"
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        return builder.create();
    }


    // TODO: this Listener design isn't really nice, you should use an Activity instead of Dialog
    public static interface Listener {
        void onSendClicked(final String errorReportText);
    }

}