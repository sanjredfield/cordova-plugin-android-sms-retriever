package org.cordova.plugin.android.sms.retriever;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;

import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaPluginAndroidSmsRetriever extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("onSmsReceived")) {
            this.onSmsReceived(callbackContext, (args != null && args.optBoolean(0, false)));
            return true;
        }

        callbackContext.error("Android SMS Retriever Plugin: Unknown action (" + action + ")");
        return false;
    }

    private void onSmsReceived(CallbackContext callbackContext, boolean notifySetupSteps) {

        final Context context = this.cordova.getActivity().getApplicationContext();
        SmsRetrieverClient smsRetrieverClient = SmsRetriever.getClient(context);
        smsRetrieverClient.startSmsRetriever();

        // Documentation from Google:
        // Starts SmsRetriever, which waits for ONE matching SMS message until timeout
        // (5 minutes). The matching SMS message will be sent via a Broadcast Intent
        // with action SmsRetriever#SMS_RETRIEVED_ACTION.
        Task<Void> task = smsRetrieverClient.startSmsRetriever();

        // Listen for success/failure of the start Task. If in a background thread, this
        // can be made blocking using Tasks.await(task, [timeout]);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                if (notifySetupSteps) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "SMS_RETRIEVER_SETUP");
                    pluginResult.setKeepCallback(true); // keeping callback
                    callbackContext.sendPluginResult(pluginResult);
                }

                // Registrar broadcast receiver
                IntentFilter intent = new IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION);
                context.registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
                            Bundle extras = intent.getExtras();
                            Status status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);

                            switch (status.getStatusCode()) {
                            case CommonStatusCodes.SUCCESS:

                                // Get SMS message contents
                                String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                                callbackContext.success(message);
                                break;
                            case CommonStatusCodes.TIMEOUT:

                                callbackContext.error("SMS_RETRIEVER_TIMEOUT:Waiting for SMS timed out (5 minutes)");
                                break;
                            }
                        }
                    }
                }, intent);
            }
        });

        task.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                callbackContext.error("SMS_RETRIEVER_SETUP_FAILED:Failed to start retriever. " + e.getMessage());
            }
        });

    }
}
