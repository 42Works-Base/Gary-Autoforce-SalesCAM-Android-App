package com.autoforcecam.app.fcm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.autoforcecam.app.R;
import com.autoforcecam.app.utils.SessionManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.json.JSONObject;
import java.util.Map;
import java.util.Random;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onNewToken(String token) {

        SessionManager sessionManager = new SessionManager(this);
        sessionManager.setFCMToken(token);

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        SessionManager sessionManager = new SessionManager(this);
        if(sessionManager.isUserLoggedIn()) {

            String title = "";
            String body = "";

            try {

                Map<String, String> params = remoteMessage.getData();
                JSONObject object = new JSONObject(params);
                title = object.get("title").toString();
                body = object.get("body").toString();

            } catch (Exception e) {
            }

            sendNotification(title, body);

        }

    }

    private void sendNotification(String title, String messageBody) {

        try {

            int notifyId = new Random().nextInt(99999);

            Intent intent = null;
            NotificationCompat.Builder notificationBuilder = null;

            String channelId = getString(R.string.FCMChannel);

            if (intent == null) {

                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                notificationBuilder =
                        new NotificationCompat.Builder(this, channelId)
                                .setContentTitle(title)
                                .setContentText(messageBody)
                                .setAutoCancel(true)
                                .setSound(defaultSoundUri);

                notificationBuilder.setColor(this.getResources().getColor(R.color.colorPrimary));
                notificationBuilder.setSmallIcon(R.drawable.ic_notify_small);

            } else {

                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                PendingIntent pendingIntent = null;

                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);



                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    pendingIntent=  PendingIntent.getActivity(this, notifyId, intent, PendingIntent.FLAG_MUTABLE);
                }
                else
                {
                    pendingIntent = PendingIntent.getActivity(this, notifyId,
                            intent, PendingIntent.FLAG_ONE_SHOT);
                }


                notificationBuilder =
                        new NotificationCompat.Builder(this, channelId)
                                .setContentTitle(title)
                                .setContentText(messageBody)
                                .setAutoCancel(true)
                                .setSound(defaultSoundUri)
                                .setContentIntent(pendingIntent);

                notificationBuilder.setColor(this.getResources().getColor(R.color.colorPrimary));
                notificationBuilder.setSmallIcon(R.drawable.ic_notify_small);

            }

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                NotificationChannel channel = new NotificationChannel(channelId,
                        title, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);

            }

            notificationManager.notify(notifyId, notificationBuilder.build());

        } catch (Exception e){}

    }
}
