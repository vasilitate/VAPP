package com.vasilitate.vapp.sdk;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.vasilitate.vapp.R;

/**
 * Provides utility functions for posting status bar notifications
 */
final class VappNotificationManager {

    static final int VAPP_NOTIFICATION_ID = 763091368;


    static void post(Context context ) {

        Intent notificationIntent = new Intent(context, VappProgressActivity.class);
        notificationIntent.putExtra( VappActions.EXTRA_NOTIFICATION_INVOKED, true);
        PendingIntent contentIntent = PendingIntent.getActivity( context,
                (int) (Math.random() * 100),
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(context.getString(R.string.notification_completed_title))
                .setContentText(context.getString(R.string.notification_completed_message))
                .setContentIntent(contentIntent)
                .build();

        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        NotificationManagerCompat.from(context).notify(VAPP_NOTIFICATION_ID, notification);
    }

    static void removeNotification(Context context) {

        NotificationManagerCompat.from(context).cancelAll();
    }
}
