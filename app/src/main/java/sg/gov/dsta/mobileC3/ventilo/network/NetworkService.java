package sg.gov.dsta.mobileC3.ventilo.network;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import sg.gov.dsta.mobileC3.ventilo.application.MainApplication;
import sg.gov.dsta.mobileC3.ventilo.helper.MqttHelper;
import sg.gov.dsta.mobileC3.ventilo.helper.RabbitMQHelper;
import sg.gov.dsta.mobileC3.ventilo.network.jeroMQ.JeroMQPubSubBrokerProxy;
import sg.gov.dsta.mobileC3.ventilo.network.jeroMQ.JeroMQSubscriber;
import sg.gov.dsta.mobileC3.ventilo.network.jeroMQ.JeroMQPublisher;
import sg.gov.dsta.mobileC3.ventilo.network.rabbitmq.RabbitMQ;
import sg.gov.dsta.mobileC3.ventilo.network.rabbitmq.RabbitMQAsyncTask;

public class NetworkService extends IntentService {

    private static final String TAG = NetworkService.class.getSimpleName();

    private final IBinder mBinder = new NetworkServiceBinder(this);

    // receiver that notifies the Service when the user changes data use preferences
//    private BackgroundDataChangeIntentReceiver dataEnabledReceiver;
//    private ConnectionStateMonitor mConnectionStateMonitor;

//    private static MqttHelper mqttHelper;
    public static RabbitMQHelper rabbitMQHelper;

    public static boolean mIsServiceRegistered;

    //checks if the service is trying to connect, to prevent race conditions between startService and dataEnabledReceiver
//    private boolean tryingToConnect;

    public NetworkService() {
        super(NetworkService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "Creating network service...");
        createConnection();
    }

    private void createConnection() {
//        mqttHelper = MqttHelper.getInstance();
//        mqttHelper.connectionStatus = MqttHelper.MQTTConnectionStatus.INITIAL;
        // register to be notified whenever the user changes their preferences
        //  relating to background data use - so that we can respect the current
        //  preference

        rabbitMQHelper = RabbitMQHelper.getInstance();
        rabbitMQHelper.connectionStatus = RabbitMQHelper.RabbitMQConnectionStatus.INITIAL;
    }

//    private boolean isOnline() {
//        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
//        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable() && cm.getActiveNetworkInfo().isConnected();
//    }

    public void deactivate() {
        // disconnect immediately
//        mqttHelper.disconnectFromBroker();

        if (rabbitMQHelper != null) {
            rabbitMQHelper.closeConnection();
        }
//        for (LinkedList<NotificationMessage> msg : notificationMap.values()) {
////            msg = new LinkedList<>();
////        }

        // try not to leak the listener
//        if (dataEnabledReceiver != null) {
//            unregisterReceiver(dataEnabledReceiver);
//            dataEnabledReceiver = null;
//        }

//        if (mConnectionStateMonitor != null) {
//            mConnectionStateMonitor.disableMonitor();
//            mConnectionStateMonitor = null;
//        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved; Service is destroyed.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Log.i(TAG, "onHandleIntent");

        if (rabbitMQHelper != null) {
//            RabbitMQAsyncTask rabbitMQAsyncTask = new RabbitMQAsyncTask();
//            rabbitMQAsyncTask.runRabbitMQ();




//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//
//                }
//            }, RabbitMQ.class.getSimpleName()).start();





            mIsServiceRegistered = true;
            Log.i(TAG, "mIsServiceRegistered is " + mIsServiceRegistered);

            System.out.println("rabbitMQHelper not null");
            boolean isRabbitMQConnected = NetworkService.rabbitMQHelper.startRabbitMQWithDefaultSetting();
            if (isRabbitMQConnected) {
                System.out.println("rabbitMQHelper notify");
                notifyRabbitMQConnectedBroadcastIntent();
            }

        }
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        super.onStartCommand(intent, startId, startId);

        // START_NOT_STICKY - Service will NOT be left running after it has been killed
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsServiceRegistered = false;
        Log.i(TAG, "RabbitMQ network service destroyed.");
    }

    private static void notifyRabbitMQConnectedBroadcastIntent() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(RabbitMQHelper.RABBITMQ_CONNECT_INTENT_ACTION);
//        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        LocalBroadcastManager.getInstance(MainApplication.getAppContext()).sendBroadcast(broadcastIntent);
    }



//    private void notifyMqttConnectedBroadcastIntent() {
//        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction(MqttHelper.MQTT_CONNECT_INTENT_ACTION);
////        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
//    }

//    private void notifyRabbitMQConnectedBroadcastIntent() {
//        Intent broadcastIntent = new Intent();
//        broadcastIntent.setAction(RabbitMQHelper.RABBITMQ_CONNECT_INTENT_ACTION);
////        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
//    }

//    private class ConnectionStateMonitor extends ConnectivityManager.NetworkCallback {
//
//        private final NetworkRequest mNetworkRequest;
//        private ConnectivityManager mConnectivityManager;
//
//        public ConnectionStateMonitor() {
//            mNetworkRequest = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).
//                    addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();
//        }
//
//        public void disableMonitor() {
//            mConnectivityManager.unregisterNetworkCallback(this);
//        }
//
//        public void enable() {
//            mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//            mConnectivityManager.registerNetworkCallback(mNetworkRequest , this);
//        }
//
//        // Likewise, you can have a disable method that simply calls ConnectivityManager#unregisterCallback(networkRequest) too.
//
//        @Override
//        public void onAvailable(Network network) {
//            // Do what you need to do here
//            // we protect against the phone switching off while we're doing this
//            //  by requesting a wake lock - we request the minimum possible wake
//            //  lock - just enough to keep the CPU running until we've finished
//            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
//            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
//            wl.acquire();
//
////            Log.d("SharedSense", "MQTT Connection: Background Data changed!");
////            if (isOnline()) {
////                if (!tryingToConnect && (mqttHelper.getMqttClient() == null
////                        || !mqttHelper.getMqttClient().isConnected()) ) {
////                    // user has allowed background data - we start again - picking
////                    //  up where we left off in handleStart before
////                    mqttHelper.connectionStatus = MqttHelper.MQTTConnectionStatus.INITIAL;
////                    mqttHelper.defineConnectionToBroker();
////
////                    boolean isMqttConnected = mqttHelper.handleStart();
////                    if (isMqttConnected) {
////                        notifyMqttConnectedBroadcastIntent();
////                    }
////                }
////            } else {
////                // user has disabled background data
////                mqttHelper.connectionStatus = MqttHelper.MQTTConnectionStatus.NOT_CONNECTED_DATA_DISABLED;
////                // disconnect from the broker
////                mqttHelper.disconnectFromBroker();
////            }
//
//            // we're finished - if the phone is switched off, it's okay for the CPU
//            //  to sleep now
//            wl.release();
//        }
//
//        @Override
//        public void onLost(Network network) {
//            super.onLost(network);
//        }
//    }
}
