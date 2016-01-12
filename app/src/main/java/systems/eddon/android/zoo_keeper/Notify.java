package systems.eddon.android.zoo_keeper;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class Notify extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        setContentView(R.layout.activity_notify);
        Intent intent = getIntent();
        ZooGate.myActivity = this;
        String Message = intent.getStringExtra(ZooGate.EXTRA_MESSAGE);
        String UrlContent = intent.getStringExtra(ZooGate.EXTRA_URL);
        String Type = intent.getStringExtra(ZooGate.EXTRA_TYPE);
        ZooGate.popupMessageTime(Message, Toast.LENGTH_LONG);
        if (UrlContent != null) {
            showNotificationURL(Message,UrlContent,Type);
        }
        finish();
    }
    private void showNotificationURL (String Message, String UrlContent,String Type) {
        Log.d("showNotificationUrl", "String: " + Message + " URL: " + UrlContent + " Type: "+Type);
        if (Type == null)
            Type = "text/plain";

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_script)
                .setContentTitle("Script Notice")
                .setContentText(Message);

        Intent resultIntent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(UrlContent);
        resultIntent.setDataAndType(uri,Type);

        PendingIntent resultPendingIntent = PendingIntent.getActivity(
            this,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );

        mBuilder.setContentIntent(resultPendingIntent);
        // Sets an ID for the notification
        int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());

    }
}
