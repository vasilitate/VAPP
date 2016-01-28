package com.vasilitate.vapp.sdk;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.vasilitate.vapp.sdk.network.request.PostLogsBody;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides a DB Helper, which is used to temporarily persist sent messages in a local DB,
 * before they are sent via REST API to a server and removed.
 */
class VappDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "vapp.db";
    private static final int DB_VERSION = 1;

    VappDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(SmsEntry.sqlCreateTable());
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    /**
     * Defines the SmsEntry table schema.
     */
    static abstract class SmsEntry implements BaseColumns {
        public static final String TABLE_NAME = "sms_entry";
        public static final String COLUMN_NAME_MESSAGE = "message";
        public static final String COLUMN_NAME_DDI = "ddi";

        public static String sqlCreateTable() {
            return String.format("CREATE TABLE %s (%s TEXT, %s TEXT)",
                    TABLE_NAME, COLUMN_NAME_MESSAGE, COLUMN_NAME_DDI);
        }
    }

    /**
     * Retrieves a list of all messages which have been sent but not logged on the server
     *
     * @return a list of log entries
     */
    List<PostLogsBody.LogEntry> retrieveSentSmsLogs() {
        List<PostLogsBody.LogEntry> logEntryList = new ArrayList<>();

        SQLiteDatabase readableDatabase = getReadableDatabase();
        Cursor cursor = readableDatabase.query(SmsEntry.TABLE_NAME, null, null, null, null, null, null);

        int columnIndexDdi = cursor.getColumnIndex(SmsEntry.COLUMN_NAME_DDI);
        int columnIndexMessage = cursor.getColumnIndex(SmsEntry.COLUMN_NAME_MESSAGE);

        if (cursor.moveToFirst()) {
            do {
                String message = cursor.getString(columnIndexMessage);
                String ddi = cursor.getString(columnIndexDdi);
                logEntryList.add(new PostLogsBody.LogEntry(message, ddi));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        readableDatabase.close();
        return logEntryList;
    }

    /**
     * Deletes all records of messages which have been sent.
     */
    void clearSentSmsLogs() {
        SQLiteDatabase writableDatabase = getWritableDatabase();
        writableDatabase.delete(SmsEntry.TABLE_NAME, null, null);
        writableDatabase.close();
    }

}
