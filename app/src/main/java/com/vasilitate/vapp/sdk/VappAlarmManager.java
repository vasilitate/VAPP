package com.vasilitate.vapp.sdk;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Date;

class VappAlarmManager {

    private static int REQUEST_CODE = 1001;

    static void addAlarm( Context context, Date nextSubscriptionDate ) {

        Intent intent = new Intent( context, SubscriptionAlarmReceiver.class);
        PendingIntent mAlarmSender = PendingIntent.getBroadcast( context, REQUEST_CODE, intent, 0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC_WAKEUP, nextSubscriptionDate.getTime(), mAlarmSender);
    }
}
