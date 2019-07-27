package io.github.xausky.unitymodmanager.dialog;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import io.github.xausky.unitymodmanager.R;

/**
 * Created by xausky on 18-3-7.
 */

public class ConfirmDialog extends AlertDialog {
    public ConfirmDialog(@NonNull Context context, OnClickListener listener) {
        super(context);
        this.setButton(BUTTON_POSITIVE, context.getText(R.string.confirm), listener);
        this.setButton(BUTTON_NEGATIVE, context.getText(R.string.cancel), listener);
        this.setTitle(R.string.prompt);
    }
}
