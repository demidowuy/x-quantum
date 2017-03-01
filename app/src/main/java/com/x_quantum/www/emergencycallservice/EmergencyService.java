package com.x_quantum.www.emergencycallservice;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.os.*;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by ZeD on 26.02.2017.
 */

public class EmergencyService extends Service {
    private Looper srvLooper;
    private ServiceHandler srvHandler;

    @Override
    public void onCreate() {
        HandlerThread thread = new HandlerThread("EmergencyService",    // Предотвращаем блок cpu
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        srvLooper = thread.getLooper();                            // запускаем сервис через handler
        srvHandler = new ServiceHandler(srvLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message message = srvHandler.obtainMessage();
        message.arg1 = startId;
        srvHandler.sendMessage(message);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void addNotification() { // создание уведомления по примеру с developer.android.com

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setOnlyAlertOnce(true)
                        .setContentTitle("Привет")  // параметры уведомления
                        .setContentText("Привет");

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(contentIntent);

        // Добавляем уведомление
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    private final class ServiceHandler extends Handler {

        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            try {
                //Вызываем рут 1 раз
                Runtime.getRuntime().exec(new String[]{"su", "-c", "pm grant " + getApplicationContext().getPackageName() + " android.permission.DUMP"}); //Предоставляем нашему пакету доступ к дамп команде
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Проблема с рут доступом", Toast.LENGTH_LONG);
                return;
            }
            final Runtime rt = Runtime.getRuntime();
            final Handler h = new Handler();
            final int delay = 2000; // проверяем каждые 2 секунды
            h.postDelayed(new Runnable() {
                public void run() {
                    try {
                        java.lang.Process process;
                        // Выполняем команду без рут привилегий
                        process = rt.exec(new String[]{"/system/bin/sh", "-c", "pm dump com.android.phone | grep EmergencyDialer | tail -n 1"}); // получаем последнее событие в com.android.phone связанное с Emergency dialer
                        BufferedReader bufferedReader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));
                        String line;
                        while ((line = bufferedReader.readLine()) != null) {
                            if (line.contains("MOVE_TO_FOREGROUND")) {  // проверяем отображается ли Emergency dialer пользователю
                                addNotification(); // пишем "привет"
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    h.postDelayed(this, delay); // ждем 2 секунды
                }
            }, delay);
        }
    }
}
