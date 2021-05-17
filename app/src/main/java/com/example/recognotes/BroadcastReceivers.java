package com.example.recognotes;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class BroadcastReceivers extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // an Intent broadcast.
        String intentAction = intent.getAction();
        if (intentAction != null) {
            String toastMessage = "";
            switch (intentAction) {
                case Intent.ACTION_POWER_CONNECTED:
                    toastMessage = "Power Connected!";
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    toastMessage = "Power Disconnected!";
                    break;
                case Intent.ACTION_BATTERY_LOW:
                    toastMessage = "Battery Percentage Low!";
                    simpleAlert(context, toastMessage);
                    break;
            }
            if(!toastMessage.isEmpty())
                Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void simpleAlert(Context context, String massage)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(massage);
        builder.setMessage("Your Battery percentage is low,\nWe recommend connecting your phone to a power source immediately");
        builder.setCancelable(false);
        builder.setNeutralButton("OK", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.create();
        builder.show();
    }
}
