package com.rcmade.foregroundservice;

import java.io.Console;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.ServiceCompat;
import android.content.pm.ServiceInfo;
import java.net.HttpURLConnection;
import java.net.URL;

import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.HeadlessJsTaskService;

import static com.rcmade.foregroundservice.Constants.NOTIFICATION_CONFIG;
import static com.rcmade.foregroundservice.Constants.TASK_CONFIG;

// NOTE: headless task will still block the UI so don't do heavy work, but this is also good
// since they will share the JS environment
// Service will also be a singleton in order to quickly find out if it is running
public class ForegroundService extends Service {

    private static ForegroundService mInstance = null;
    private static Bundle lastNotificationConfig = null;
    private int running = 0;

    private static ReactContext reactContext;

    public static void setReactContext(ReactContext context) {
        reactContext = context;
    }

    public static boolean isServiceCreated() {
        try {
            return mInstance != null && mInstance.ping();
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static ForegroundService getInstance() {
        if (isServiceCreated()) {
            return mInstance;
        }
        return null;
    }

    public int isRunning() {
        return running;
    }

    private boolean ping() {
        return true;
    }

    @Override
    public void onCreate() {
        // Log.e("ForegroundService", "destroy called");
        running = 0;
        mInstance = this;
    }

    @Override
    public void onDestroy() {
        // Log.e("ForegroundService", "destroy called");
        this.handler.removeCallbacks(this.runnableCode);
        running = 0;
        mInstance = null;
    }

    @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

    if(serviceTypeString==null)return 0;

    int flags = 0;
    // accept tokens separated by | or comma
    String[] parts = serviceTypeString.split("[|,]");for(
    String raw:parts)
    {
        String token = raw.trim();
        if (token.length() == 0)
            continue;
        switch (token) {
            case "dataSync":
            case "datasync":
                flags |= ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC; // 1
                break;
            case "mediaPlayback":
            case "media_playback":
            case "mediaplayback":
                flags |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK; // 2
                break;
            case "phoneCall":
            case "phonecall":
                flags |= ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL; // 4
                break;
            case "location":
                flags |= ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION; // 8
                break;
            case "connectedDevice":
            case "connecteddevice":
                flags |= ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE; // 16
                break;
            case "camera":
                flags |= ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA; // 64
                break;
            case "microphone":
                flags |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE; // 128
                break;
            default:
                // unknown token - ignore but log
                Log.w("ForegroundService", "Unknown serviceType token: " + token);
                break;
        }
    }return flags;
    }

    private boolean startService(Bundle notificationConfig) {
        try {
            int id = (int) notificationConfig.getDouble("id");
            String foregroundServiceType = notificationConfig.getString("ServiceType");

            Notification notification = NotificationHelper
                .getInstance(getApplicationContext())
                .buildNotification(getApplicationContext(), notificationConfig);

            int typeFlags = 0;
            try {
                // Map tokens to flags (supports combined values like "location|dataSync")
                typeFlags = mapServiceTypeTokensToFlags(foregroundServiceType);
            } catch (Exception e) {
                Log.w("ForegroundService", "Failed to map serviceType tokens: " + e.getMessage());
                typeFlags = 0;
            }

            Log.i("ForegroundService", "Starting foreground: api=" + Build.VERSION.SDK_INT + " ServiceType='" + foregroundServiceType + "' flags=" + typeFlags);

            // Use ServiceCompat to pass flags safely across API levels (requires androidx.core)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // ServiceCompat will call startForeground with flags where supported
                    ServiceCompat.startForeground(this, id, notification, typeFlags);
                } else {
                    startForeground(id, notification);
                }
            } catch (Throwable t) {
                Log.w("ForegroundService", "ServiceCompat.startForeground failed, falling back to startForeground(): " + t.getMessage());
                startForeground(id, notification);
            }

          // quick native ping check (run on background thread)
        // Create a final copy of typeFlags for use in the lambda
        final int finalTypeFlags = typeFlags;
        new Thread(() -> {
            try {
                URL url = new URL("https://www.google.com/");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                Log.i("ForegroundService", "Native ping result code=" + code + " (flags=" + finalTypeFlags + ")");
                conn.disconnect();
            } catch (Throwable ex) {
                Log.w("ForegroundService", "Native ping failed: " + ex.getMessage() + " (flags=" + finalTypeFlags + ")");
            }
}).start();

            running += 1;
            lastNotificationConfig = notificationConfig;
            return true;

        } catch (Exception e) {
            if (reactContext != null) {
                Log.e("ForegroundService", "Failed to start service: " + e.getMessage());
                reactContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                        .emit("onServiceError", e.getMessage());
            }
            return false;
        }
    }

    // private int getServiceTypeForAndroid10(String customServiceType) {
    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    // // switch (customServiceType) {
    // // case "camera":
    // // return 8; // ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
    // // case "connectedDevice":
    // // return 32; // ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
    // // case "dataSync":
    // // return 16; // ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    // // case "health":
    // // return 64; // ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
    // // case "location":
    // // return 1; // ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
    // // case "mediaPlayback":
    // // return 2; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
    // // case "mediaProjection":
    // // return 4; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    // // case "microphone":
    // // return 128; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    // // case "phoneCall":
    // // return 256; // ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
    // // case "remoteMessaging":
    // // return 1024; // ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
    // // case "shortService":
    // // return 2048; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
    // // case "specialUse":
    // // return 4096; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    // // case "systemExempted":
    // // return 8192; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
    // // default:
    // // return 1; // Default to location
    // // }
    // }
    // return 0; // This won't be used for Android < 10
    // }

    private int getServiceTypeForAndroid10(String customServiceType) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                switch (customServiceType) {
                    case "camera":
                        return 64; // ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                    case "connectedDevice":
                        return 16; // ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                    case "dataSync":
                        return 1; // ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    case "health":
                        return 256; // ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                    case "location":
                        return 8; // ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    case "manifest":
                        return -1; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                    case "mediaPlayback":
                        return 2; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    case "mediaProcessing":
                        return 8192; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                    case "mediaProjection":
                        return 32; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    case "microphone":
                        return 128; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                    case "phoneCall":
                        return 4; // ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    case "remoteMessaging":
                        return 512; // ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                    case "shortService":
                        return 2048; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                    case "specialUse":
                        return 1073741824; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
                    case "systemExempted":
                        return 1024; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                    default:
                        return 1; // Default to location
                }
            }
            return 0; // This won't be used for Android < 10
        }

    private int mapServiceType(String customServiceType) {
            // Use direct integer constants instead of ServiceInfo constants
            switch (customServiceType) {
                case "camera":
                    return 64; // ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                case "connectedDevice":
                    return 16; // ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                case "dataSync":
                    return 1; // ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                case "health":
                    return 256; // ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                case "location":
                    return 8; // ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                case "manifest":
                    return -1; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST
                case "mediaPlayback":
                    return 2; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                case "mediaProcessing":
                    return 8192; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
                case "mediaProjection":
                    return 32; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                case "microphone":
                    return 128; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                case "phoneCall":
                    return 4; // ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                case "remoteMessaging":
                    return 512; // ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
                case "shortService":
                    return 2048; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
                case "specialUse":
                    return 1073741824; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                case "systemExempted":
                    return 1024; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                default:
                    throw new IllegalArgumentException("Unknown foreground service type: " + customServiceType);
            }
        }
    // private int mapServiceType(String customServiceType) {
    // // Use direct integer constants instead of ServiceInfo constants
    // switch (customServiceType) {
    // case "camera":
    // return 8; // ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
    // case "connectedDevice":
    // return 32; // ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
    // case "dataSync":
    // return 16; // ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
    // case "health":
    // return 64; // ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
    // case "location":
    // return 1; // ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
    // case "mediaPlayback":
    // return 2; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
    // case "mediaProjection":
    // return 4; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
    // case "microphone":
    // return 128; // ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
    // case "phoneCall":
    // return 256; // ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
    // case "remoteMessaging":
    // return 1024; // ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING
    // case "shortService":
    // return 2048; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
    // case "specialUse":
    // return 4096; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
    // case "systemExempted":
    // return 8192; // ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
    // default:
    // throw new IllegalArgumentException("Unknown foreground service type: " +
    // customServiceType);
    // }
    // }

    public Bundle taskConfig;
    private Handler handler = new Handler();
    private Runnable runnableCode=new Runnable(){@Override public void run(){final Intent service=new Intent(getApplicationContext(),ForegroundServiceTask.class);service.putExtras(taskConfig);try{getApplicationContext().startService(service);}catch(Exception e){Log.e("ForegroundService","Failed to start foreground service in loop: "+e.getMessage());}

    int delay=(int)taskConfig.getDouble("delay");

    int loopDelay=(int)taskConfig.getDouble("loopDelay");Log.d("SuperLog",""+loopDelay);handler.postDelayed(this,loopDelay);}};

    @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            String action = intent.getAction();

            /**
             * From the docs: Every call to this method will result in a
             * corresponding call to the target service's
             * Service.onStartCommand(Intent, int, int) method, with the intent
             * given here. This provides a convenient way to submit jobs to a
             * service without having to bind and call on to its interface.
             */
            //Log.d("ForegroundService", "onStartCommand flags: " + String.valueOf(flags) + "  " + String.valueOf(startId));
            if (action != null) {
                if (action.equals(Constants.ACTION_FOREGROUND_SERVICE_START)) {
                    if (intent.getExtras() != null && intent.getExtras().containsKey(NOTIFICATION_CONFIG)) {
                        Bundle notificationConfig = intent.getExtras().getBundle(NOTIFICATION_CONFIG);
                        startService(notificationConfig);
                    }
                }

                if (action.equals(Constants.ACTION_UPDATE_NOTIFICATION)) {
                    if (intent.getExtras() != null && intent.getExtras().containsKey(NOTIFICATION_CONFIG)) {
                        Bundle notificationConfig = intent.getExtras().getBundle(NOTIFICATION_CONFIG);

                        if (running <= 0) {
                            Log.d("ForegroundService", "Update Notification called without a running service, trying to restart service.");
                            startService(notificationConfig);
                        } else {
                            try {
                                int id = (int) notificationConfig.getDouble("id");

                                Notification notification = NotificationHelper
                                        .getInstance(getApplicationContext())
                                        .buildNotification(getApplicationContext(), notificationConfig);

                                NotificationManager mNotificationManager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
                                mNotificationManager.notify(id, notification);

                                lastNotificationConfig = notificationConfig;
                            } catch (Exception e) {
                                Log.e("ForegroundService", "Failed to update notification: " + e.getMessage());
                            }
                        }
                    }
                } else if (action.equals(Constants.ACTION_FOREGROUND_RUN_TASK)) {
                    if (running <= 0 && lastNotificationConfig == null) {
                        Log.e("ForegroundService", "Service is not running to run tasks.");
                        stopSelf();
                        return START_NOT_STICKY;
                    } else {
                        // try to re-start service if it was killed
                        if (running <= 0) {
                            Log.d("ForegroundService", "Run Task called without a running service, trying to restart service.");
                            if (!startService(lastNotificationConfig)) {
                                Log.e("ForegroundService", "Service is not running to run tasks.");
                                return START_REDELIVER_INTENT;
                            }
                        }

                        if (intent.getExtras() != null && intent.getExtras().containsKey(TASK_CONFIG)) {
                            taskConfig = intent.getExtras().getBundle(TASK_CONFIG);

                            try {
                                if (taskConfig.getBoolean("onLoop") == true) {
                                    this.handler.post(this.runnableCode);
                                } else {
                                    this.runHeadlessTask(taskConfig);
                                }
                            } catch (Exception e) {
                                Log.e("ForegroundService", "Failed to start task: " + e.getMessage());
                            }
                        }
                    }
                } else if (action.equals(Constants.ACTION_FOREGROUND_SERVICE_STOP)) {
                    if (running > 0) {
                        running -= 1;

                        if (running == 0) {
                            stopSelf();
                            lastNotificationConfig = null;
                        }
                    } else {
                        Log.d("ForegroundService", "Service is not running to stop.");
                        stopSelf();
                        lastNotificationConfig = null;
                    }
                    return START_NOT_STICKY;
                } else if (action.equals(Constants.ACTION_FOREGROUND_SERVICE_STOP_ALL)) {
                    running = 0;
                    mInstance = null;
                    lastNotificationConfig = null;
                    stopSelf();
                    return START_NOT_STICKY;
                }
            }

            // service to restart automatically if it's killed
            return START_REDELIVER_INTENT;
        }

    public void runHeadlessTask(Bundle bundle) {
            final Intent service = new Intent(getApplicationContext(), ForegroundServiceTask.class);
            service.putExtras(bundle);

            int delay = (int) bundle.getDouble("delay");

            if (delay <= 0) {
                try {
                    getApplicationContext().startService(service);
                } catch (Exception e) {
                    Log.e("ForegroundService", "Failed to start delayed headless task: " + e.getMessage());
                }
                // wakelock should be released automatically by the task
                // Shouldn't be needed, it's called automatically by headless
                //HeadlessJsTaskService.acquireWakeLockNow(getApplicationContext());
            } else {
                new Handler().postDelayed(new Runnable() {
                    @Override
                public void run() {
                        if (running <= 0) {
                            return;
                        }
                        try {
                            getApplicationContext().startService(service);
                        } catch (Exception e) {
                            Log.e("ForegroundService", "Failed to start delayed headless task: " + e.getMessage());
                        }
                    }
                }, delay);
            }
        }
}