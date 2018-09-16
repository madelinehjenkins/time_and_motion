package org.gheskio.queue;

import android.app.AlertDialog;
import android.content.Context;


public class InfoDialog {
    public static void show(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.show();
    }
}