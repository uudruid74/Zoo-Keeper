package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
            showNotificationURL(this,PID, Title, Message,UrlContent,Type);
        }
    }
    public Notify() { super("ZooKeeper Notify"); }

    public static void showNotificationURL (Context act,String ID, String Title, String Message, String UrlContent,String Type) {
        Log.d("showNotificationUrl", "String: " + Message + " URL: " + UrlContent + " Type: "+Type);
        if (Type == null)
            Type = "text/plain";

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(act)
                .setSmallIcon(R.drawable.ic_script)
                .setContentTitle(Title)
                .setContentText(Message);

        Intent resultIntent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(UrlContent);
        resultIntent.setDataAndType(uri,Type);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
            act,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );

        mBuilder.setContentIntent(resultPendingIntent);
        // Sets an ID for the notification
        int mNotificationId = Integer.valueOf(ID);
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
            (NotificationManager) act.getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }
}
