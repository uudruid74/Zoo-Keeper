package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Network;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class Notify extends IntentService {
    @Override
    protected void onHandleIntent(Intent intent) {
        String Message = intent.getStringExtra(ZooGate.EXTRA_MESSAGE);
        String UrlContent = intent.getStringExtra(ZooGate.EXTRA_URL);

        String Title = intent.getStringExtra(ZooGate.EXTRA_TITLE);
        if (Title == "null")
            Title = "Script Notice";
        else
            Title = "Script: " + Title;

        String Type = intent.getStringExtra(ZooGate.EXTRA_TYPE);
        if (Type == null)
            Type = "text/plain";

        String PID = intent.getStringExtra(ZooGate.EXTRA_PID);
        if (PID == null)
            PID="05";

        ZooGate.popupMessageTime(Message, Toast.LENGTH_LONG);
        if (UrlContent != null) {
            notificationCreate("Script: " + Title, Message, R.drawable.ic_script,
                    Integer.valueOf(PID), null, UrlContent, Type);
        }
    }
    public Notify() { super("ZooKeeper Notify"); }

    public static void notificationCreate(String Title, String Message, int icon, int mNotificationId,
                                   Class openClass, String URL, String Type) {
        Log.d("notificationCreate", "String: " + Message);

        if (Type == null) {
            if (openClass == null) {
                Type = "text/plain";
            } else {
                Type = ZooGate.ACTION_UPGRADE;
            }
        }
        if (Title == null)
            Title = ZooGate.myActivity.getString(R.string.app_name);

        Intent resultIntent;
        if (openClass == null) {
            resultIntent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(URL);
            resultIntent.setDataAndType(uri, Type);
        } else if (openClass == Network.class) {
            resultIntent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
        } else {
            resultIntent = new Intent(ZooGate.myActivity, openClass);
            resultIntent.putExtra(ZooGate.EXTRA_ACTION, Type);
            resultIntent.putExtra(ZooGate.EXTRA_URL, URL);
            resultIntent.putExtra(ZooGate.EXTRA_CANCEL, String.valueOf(mNotificationId));
            resultIntent.putExtra(ZooGate.EXTRA_DESCR, Title);
            if (mNotificationId == 2)
                Title = ZooGate.myActivity.getString(R.string.app_name);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ZooGate.myActivity)
                .setSmallIcon(icon)
                .setContentTitle(Title)
                .setContentText(Message);


        PendingIntent resultPendingIntent = PendingIntent.getActivity(
                ZooGate.myActivity,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // No Toasts on Backup/Restore ??
        if (mNotificationId != 5)
            ZooGate.popupMessage(Message);

        mBuilder.setContentIntent(resultPendingIntent);
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) ZooGate.myActivity.getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }
}
