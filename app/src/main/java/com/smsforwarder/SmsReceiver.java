package com.smsforwarder;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.text.TextUtils;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        if (!MainActivity.isForwardingEnabled(context)) {
            return;
        }

        String destination = MainActivity.getDestinationNumber(context);
        if (TextUtils.isEmpty(destination)) {
            MainActivity.appendLog(context, context.getString(R.string.log_missing_destination));
            return;
        }

        if (intent.getExtras() == null) {
            return;
        }

        Object[] pdus = (Object[]) intent.getExtras().get("pdus");
        String format = intent.getStringExtra("format");
        if (pdus == null || pdus.length == 0) {
            return;
        }

        StringBuilder messageBuilder = new StringBuilder();
        String sender = context.getString(R.string.unknown_sender);
        for (Object pdu : pdus) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            sender = smsMessage.getDisplayOriginatingAddress();
            messageBuilder.append(smsMessage.getMessageBody());
        }

        String originalMessage = messageBuilder.toString();
        boolean forwardAll = MainActivity.isForwardAllEnabled(context);
        String keyword = MainActivity.getKeyword(context);
        boolean matchesKeyword = !TextUtils.isEmpty(keyword)
                && originalMessage.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));

        if (!forwardAll && !matchesKeyword) {
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            MainActivity.appendLog(context, context.getString(R.string.log_missing_sms_permission));
            return;
        }

        String forwardedMessage = context.getString(R.string.forward_prefix, sender) + "\n" + originalMessage;
        SmsManager smsManager = SmsManager.getDefault();
        List<String> parts = smsManager.divideMessage(forwardedMessage);
        smsManager.sendMultipartTextMessage(destination, null, parts, null, null);

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        String logEntry = context.getString(R.string.log_forwarded_entry, timestamp, sender, destination);
        MainActivity.appendLog(context, logEntry);
        NotificationHelper.ensureChannels(context);
        NotificationHelper.showForwardedNotification(
                context,
                context.getString(R.string.notification_forwarded_title),
                context.getString(R.string.notification_forwarded_text, sender, destination)
        );
    }
}
