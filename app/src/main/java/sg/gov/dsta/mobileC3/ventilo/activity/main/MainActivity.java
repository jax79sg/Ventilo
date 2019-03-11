package sg.gov.dsta.mobileC3.ventilo.activity.main;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import sg.gov.dh.mq.MQListener;
import sg.gov.dsta.mobileC3.ventilo.NoSwipeViewPager;
import sg.gov.dsta.mobileC3.ventilo.R;
import sg.gov.dsta.mobileC3.ventilo.activity.map.MapShipBlueprintFragment;
import sg.gov.dsta.mobileC3.ventilo.activity.report.ReportStatePagerAdapter;
import sg.gov.dsta.mobileC3.ventilo.activity.report.sitrep.SitRepInnerFragment;
import sg.gov.dsta.mobileC3.ventilo.activity.report.task.TaskInnerFragment;
import sg.gov.dsta.mobileC3.ventilo.helper.MqttHelper;
import sg.gov.dsta.mobileC3.ventilo.helper.RabbitMQHelper;
import sg.gov.dsta.mobileC3.ventilo.model.eventbus.PageEvent;
import sg.gov.dsta.mobileC3.ventilo.network.NetworkService;
import sg.gov.dsta.mobileC3.ventilo.network.NetworkServiceBinder;
import sg.gov.dsta.mobileC3.ventilo.network.rabbitmq.IMQListener;
import sg.gov.dsta.mobileC3.ventilo.util.JSONUtil;
import sg.gov.dsta.mobileC3.ventilo.util.constant.MainNavigationConstants;
import sg.gov.dsta.mobileC3.ventilo.util.constant.FragmentConstants;
import sg.gov.dsta.mobileC3.ventilo.util.constant.SharedPreferenceConstants;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // MQTT
    private static final String MQTT_TOPIC_TASK = "Task";
    private static final String MQTT_TOPIC_INCIDENT = "Incident";

    private Intent mMqttIntent;

    private NetworkService mNetworkService;
    //    private NetworkConnectivity mNetworkConnectivity;
    private MqttHelper mMqttHelper;

    private NoSwipeViewPager mNoSwipeViewPager;
    private BottomNavigationView mBottomNavigationView;
    private TextView mDataReceived;
    private Button mBtnPublish;

    private boolean mIsServiceRegistered;

    private IMQListener mIMQListener;
//    private MapView mMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        ownUserId = 10;
//        ownUserId = Long.parseLong(getIntent().getStringExtra("USER_ID"));

        MainStatePagerAdapter mainStatePagerAdapter = new MainStatePagerAdapter(
                getSupportFragmentManager());
        mNoSwipeViewPager = findViewById(R.id.viewpager_main_nav);
        mNoSwipeViewPager.setAdapter(mainStatePagerAdapter);
        mNoSwipeViewPager.setPagingEnabled(false);

        mBottomNavigationView = findViewById(R.id.btm_nav_view_main_nav);

        NoSwipeViewPager.OnPageChangeListener viewPagerPageChangeListener =
                setPageChangeListener(mBottomNavigationView);
        mNoSwipeViewPager.addOnPageChangeListener(viewPagerPageChangeListener);

        BottomNavigationView.OnNavigationItemSelectedListener
                navigationItemSelectedListener = setNavigationItemSelectedListener(mNoSwipeViewPager);
        mBottomNavigationView.setOnNavigationItemSelectedListener(navigationItemSelectedListener);

        mDataReceived = findViewById(R.id.dataReceived);
        mBtnPublish = findViewById(R.id.btn_mqtt_publish);

        mBtnPublish.setVisibility(View.GONE);
        mBtnPublish.setOnClickListener(publishOnClickListener());
    }

    private NoSwipeViewPager.OnPageChangeListener setPageChangeListener(
            final BottomNavigationView bottomNavigationView) {

        return new NoSwipeViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                switch (position) {
                    case MainNavigationConstants.BTM_NAV_MENU_MAP_POSITION_ID:
                        bottomNavigationView.setSelectedItemId(R.id.btn_nav_action_map);
                        return;

                    case MainNavigationConstants.BTM_NAV_MENU_TIMELINE_POSITION_ID:
                        bottomNavigationView.setSelectedItemId(R.id.btn_nav_action_timeline);
                        return;

                    case MainNavigationConstants.BTM_NAV_MENU_REPORT_POSITION_ID:
                        bottomNavigationView.setSelectedItemId(R.id.btn_nav_action_report);
                        return;

                    case MainNavigationConstants.BTM_NAV_MENU_RADIO_LINK_STATUS_POSITION_ID:
                        bottomNavigationView.setSelectedItemId(R.id.btn_nav_action_notification);
                        return;

                    case MainNavigationConstants.BTM_NAV_MENU_STREAM_POSITION_ID:
                        bottomNavigationView.setSelectedItemId(R.id.btn_nav_action_stream);
                        return;

                    default:
                        return;
                }
            }
        };
    }

    private BottomNavigationView.OnNavigationItemSelectedListener setNavigationItemSelectedListener(
            final NoSwipeViewPager viewPager) {

        return new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.btn_nav_action_map:
                        viewPager.setCurrentItem(MainNavigationConstants.BTM_NAV_MENU_MAP_POSITION_ID,
                                true);
                        return true;

                    case R.id.btn_nav_action_timeline:
                        viewPager.setCurrentItem(MainNavigationConstants.BTM_NAV_MENU_TIMELINE_POSITION_ID,
                                true);
                        return true;

                    case R.id.btn_nav_action_report:
                        viewPager.setCurrentItem(MainNavigationConstants.BTM_NAV_MENU_REPORT_POSITION_ID,
                                true);
                        return true;

                    case R.id.btn_nav_action_notification:
                        viewPager.setCurrentItem(MainNavigationConstants.BTM_NAV_MENU_RADIO_LINK_STATUS_POSITION_ID,
                                true);
                        return true;

                    case R.id.btn_nav_action_stream:
                        viewPager.setCurrentItem(MainNavigationConstants.BTM_NAV_MENU_STREAM_POSITION_ID,
                                true);
                        return true;

                    default:
                        return false;
                }
            }
        };
    }

    private View.OnClickListener publishOnClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mMqttHelper != null) {
                    mMqttHelper.publishMessage("Test Payload");
                }
            }
        };
    }

    private void setupMQListener() {
        if (mIMQListener == null) {
            mIMQListener = new IMQListener() {
                @Override
                public void onNewMessage(String message) {
                    Log.w("Debug", message);

                    System.out.println("Received new rabbitMqMessage");
                    System.out.println("JSONUtil.isJSONValid(message) is " + JSONUtil.isJSONValid(message));
//                String mqttMessageString = mqttMessage.toString();
                    boolean isJSON = false;

                    if (JSONUtil.isJSONValid(message)) {
                        try {
                            JSONObject mqttMessageJSON = new JSONObject(message);
                            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplication().getApplicationContext());
                            SharedPreferences.Editor editor = pref.edit();

                            System.out.println("Received valid mqttMessage");

                            switch (mqttMessageJSON.getString("key")) {

                                case FragmentConstants.KEY_TASK_ADD:
                                    int totalNumberOfTasks = pref.getInt(SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.TASK_TOTAL_NUMBER), 0);
                                    String totalNumberOfTasksKey = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.TASK_TOTAL_NUMBER);
                                    editor.putInt(totalNumberOfTasksKey, totalNumberOfTasks + 1);

                                    String taskInitials = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.HEADER_TASK).concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(String.valueOf(totalNumberOfTasks));

                                    editor.putInt(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_TASK_ID), mqttMessageJSON.getInt("id"));
                                    editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_TASK_ASSIGNER), mqttMessageJSON.getString("assigner"));
                                    editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_TASK_ASSIGNEE), mqttMessageJSON.getString("assignee"));
                                    editor.putInt(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_TASK_ASSIGNEE_AVATAR_ID), R.drawable.default_soldier_icon);
                                    editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_TASK_TITLE), mqttMessageJSON.getString("title"));
                                    editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_TASK_DESCRIPTION), mqttMessageJSON.getString("description"));
                                    editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_TASK_STATUS), mqttMessageJSON.getString("status"));
                                    editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_TASK_DATE), mqttMessageJSON.getString("date"));

                                    editor.apply();

                                    TaskInnerFragment taskInnerFragment = (TaskInnerFragment) ReportStatePagerAdapter.getPageReferenceMap().
                                            get(FragmentConstants.REPORT_TAB_TITLE_TASK_ID);

                                    if (taskInnerFragment != null) {
                                        Log.d(TAG, "Task: Refresh Data");
                                        taskInnerFragment.refreshData();
                                        taskInnerFragment.addItemInRecycler();
                                    }

                                case FragmentConstants.KEY_SITREP_ADD:
                                    int totalNumberOfSitRep = pref.getInt(SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SITREP_TOTAL_NUMBER), 0);
                                    String totalNumberOfSitRepKey = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SITREP_TOTAL_NUMBER);
                                    editor.putInt(totalNumberOfSitRepKey, totalNumberOfSitRep + 1);

                                    System.out.println("main activity totalNumberOfSitRep + 1 is " + (totalNumberOfSitRep + 1));

                                    String sitRepInitials = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.HEADER_SITREP).concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(String.valueOf(totalNumberOfSitRep));
                                    editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_ID), mqttMessageJSON.getInt("id"));
                                    editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_REPORTER), mqttMessageJSON.getString("reporter"));
                                    editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_REPORTER_AVATAR_ID), R.drawable.default_soldier_icon);
                                    editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_LOCATION), mqttMessageJSON.getString("location"));
                                    editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_ACTIVITY), mqttMessageJSON.getString("activity"));
                                    editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_PERSONNEL_T), mqttMessageJSON.getInt("personnel_t"));
                                    editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_PERSONNEL_S), mqttMessageJSON.getInt("personnel_s"));
                                    editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_PERSONNEL_D), mqttMessageJSON.getInt("personnel_d"));
                                    editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_NEXT_COA), mqttMessageJSON.getString("next_coa"));
                                    editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_REQUEST), mqttMessageJSON.getString("request"));

                                    editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                            concat(SharedPreferenceConstants.SUB_HEADER_SITREP_DATE), mqttMessageJSON.getString("date"));

                                    editor.apply();

                                    SitRepInnerFragment sitRepInnerFragment = (SitRepInnerFragment) ReportStatePagerAdapter.getPageReferenceMap().
                                            get(FragmentConstants.REPORT_TAB_TITLE_SITREP_ID);

                                    if (sitRepInnerFragment != null) {
                                        Log.d(TAG, "Sit Rep: Refresh Data");
                                        sitRepInnerFragment.refreshData();
                                        sitRepInnerFragment.addItemInRecycler();
                                    }
                            }

                            isJSON = true;

                        } catch (JSONException ex) {
                            Log.d(TAG, "JSONException: " + ex);
                        }

                    }
                }
            };

            RabbitMQHelper.getInstance().addRabbitListener(mIMQListener);
        }

    }

    public void setMqttCallback() {
        mMqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Debug", mqttMessage.toString());

                System.out.println("Received new mqttMessage");
                String mqttMessageString = mqttMessage.toString();
                boolean isJSON = false;

                if (JSONUtil.isJSONValid(mqttMessageString)) {
                    try {
                        JSONObject mqttMessageJSON = new JSONObject(mqttMessageString);
                        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplication().getApplicationContext());
                        SharedPreferences.Editor editor = pref.edit();

                        System.out.println("Received valid mqttMessage");

                        switch (mqttMessageJSON.getString("key")) {

                            case FragmentConstants.KEY_TASK_ADD:
                                int totalNumberOfTasks = pref.getInt(SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.TASK_TOTAL_NUMBER), 0);
                                String totalNumberOfTasksKey = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.TASK_TOTAL_NUMBER);
                                editor.putInt(totalNumberOfTasksKey, totalNumberOfTasks + 1);

                                String taskInitials = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.HEADER_TASK).concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(String.valueOf(totalNumberOfTasks));

                                editor.putInt(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_TASK_ID), mqttMessageJSON.getInt("id"));
                                editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_TASK_ASSIGNER), mqttMessageJSON.getString("assigner"));
                                editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_TASK_ASSIGNEE), mqttMessageJSON.getString("assignee"));
                                editor.putInt(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_TASK_ASSIGNEE_AVATAR_ID), R.drawable.default_soldier_icon);
                                editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_TASK_TITLE), mqttMessageJSON.getString("title"));
                                editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_TASK_DESCRIPTION), mqttMessageJSON.getString("description"));
                                editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_TASK_STATUS), mqttMessageJSON.getString("status"));
                                editor.putString(taskInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_TASK_DATE), mqttMessageJSON.getString("date"));

                                editor.apply();

                                TaskInnerFragment taskInnerFragment = (TaskInnerFragment) ReportStatePagerAdapter.getPageReferenceMap().
                                        get(FragmentConstants.REPORT_TAB_TITLE_TASK_ID);

                                if (taskInnerFragment != null) {
                                    Log.d(TAG, "Task: Refresh Data");
                                    taskInnerFragment.refreshData();
                                    taskInnerFragment.addItemInRecycler();
                                }

                            case FragmentConstants.KEY_SITREP_ADD:
                                int totalNumberOfSitRep = pref.getInt(SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SITREP_TOTAL_NUMBER), 0);
                                String totalNumberOfSitRepKey = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SITREP_TOTAL_NUMBER);
                                editor.putInt(totalNumberOfSitRepKey, totalNumberOfSitRep + 1);

                                System.out.println("main activity totalNumberOfSitRep + 1 is " + (totalNumberOfSitRep + 1));

                                String sitRepInitials = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.HEADER_SITREP).concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(String.valueOf(totalNumberOfSitRep));
                                editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_ID), mqttMessageJSON.getInt("id"));
                                editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_REPORTER), mqttMessageJSON.getString("reporter"));
                                editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_REPORTER_AVATAR_ID), R.drawable.default_soldier_icon);
                                editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_LOCATION), mqttMessageJSON.getString("location"));
                                editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_ACTIVITY), mqttMessageJSON.getString("activity"));
                                editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_PERSONNEL_T), mqttMessageJSON.getInt("personnel_t"));
                                editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_PERSONNEL_S), mqttMessageJSON.getInt("personnel_s"));
                                editor.putInt(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_PERSONNEL_D), mqttMessageJSON.getInt("personnel_d"));
                                editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_NEXT_COA), mqttMessageJSON.getString("next_coa"));
                                editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_REQUEST), mqttMessageJSON.getString("request"));

                                editor.putString(sitRepInitials.concat(SharedPreferenceConstants.SEPARATOR).
                                        concat(SharedPreferenceConstants.SUB_HEADER_SITREP_DATE), mqttMessageJSON.getString("date"));

                                editor.apply();

                                SitRepInnerFragment sitRepInnerFragment = (SitRepInnerFragment) ReportStatePagerAdapter.getPageReferenceMap().
                                        get(FragmentConstants.REPORT_TAB_TITLE_SITREP_ID);

                                if (sitRepInnerFragment != null) {
                                    Log.d(TAG, "Sit Rep: Refresh Data");
                                    sitRepInnerFragment.refreshData();
                                    sitRepInnerFragment.addItemInRecycler();
                                }
                        }

                        isJSON = true;

                    } catch (JSONException ex) {
                        Log.d(TAG, "JSONException: " + ex);
                    }

                }
                //
//                checkMqttTaskTopic(topic, mqttMessage.toString());
//                checkMqttIncidentTopic(topic, mqttMessage.toString());

//                mDataReceived.setText(mqttMessage.toString());

//                if (mqttMessage.toString().contains("PAYLOAD")) {
//                    MapFragment.isTrackAllies = true;
//                    MapFragment.coordString = mqttMessage.toString();
//                    System.out.println("message arrived is " + mqttMessage.toString());
//                }

//                if (!isJSON) {
//                    if (mqttMessage.toString().contains("publish")) {
//                        MapFragment.isTrackAllies = true;
//                        MapFragment.coordString = mqttMessage.toString();
//                        System.out.println("message arrived is " + mqttMessage.toString());
//                    }
//
//
//                    if (mqttMessage.toString().contains("Coords:")) {
//                        System.out.println("GOT THE STRING --> " + mqttMessage.toString());
//                        MapFragment.isTrackAllies = true;
//                        MapFragment.coordString = mqttMessage.toString();
//                    }
//                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

//    private void checkMqttTaskTopic(String topic, String message) {
//        if (MQTT_TOPIC_TASK.equalsIgnoreCase(topic)) {
//            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
//            SharedPreferences.Editor editor = pref.edit();
//
//            String subHeaderTaskKey = SharedPreferenceConstants.INITIALS.concat(SharedPreferenceConstants.SEPARATOR).
//                    concat(SharedPreferenceConstants.SUB_HEADER_TASK_TITLE);
//            editor.putString(subHeaderTaskKey, message);
//        }
//    }
//
//    private void checkMqttIncidentTopic(String topic, String message) {
//        if (MQTT_TOPIC_INCIDENT.equalsIgnoreCase(topic)) {
//
//        }
//    }

//    private void startMqtt() {
//        mqttHelper = new MqttHelper(getApplicationContext());
//        mqttHelper.setCallback(new MqttCallbackExtended() {
//            @Override
//            public void connectComplete(boolean b, String s) {
//
//            }
//
//            @Override
//            public void connectionLost(Throwable throwable) {
//
//            }
//
//            @Override
//            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
//                Log.w("Debug", mqttMessage.toString());
//                mDataReceived.setText(mqttMessage.toString());
//            }
//
//            @Override
//            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
//
//            }
//        });
//    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mMqttServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NetworkServiceBinder binder = (NetworkServiceBinder) service;
            mNetworkService = binder.getService();
//            mNetworkService.deactivate();
//            unbindService(this);
//            System.out.println("Connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mNetworkService.deactivate();
//            System.out.println("Disconnected");
//            Toast.makeText(getApplicationContext(), "Service Disconnected", Toast.LENGTH_LONG).show();
        }
    };

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (resultCode == REQUEST_TAKE_PHOTO) {
//            Toast.makeText(getActivity().getApplicationContext(), "Photo Captured; photo uri is " +
//                    data.getExtras().getBundle(MediaStore.EXTRA_OUTPUT), Toast.LENGTH_LONG);
//            mLinearLayoutCameraBtn.setVisibility(View.GONE);
//            mLinearLayoutSendToBtn.setVisibility(View.VISIBLE);
//            mRelLayoutEditFabs.setVisibility(View.VISIBLE);
//        }
//    }

    private void registerMqttBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(RabbitMQHelper.RABBITMQ_CONNECT_INTENT_ACTION);
//        filter.addAction(MqttHelper.MQTT_CONNECT_INTENT_ACTION);
        System.out.println("registerMqttBroadcastReceiver");
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
//                if (MqttHelper.MQTT_CONNECT_INTENT_ACTION.equalsIgnoreCase(intent.getAction())) {
//                    if (mMqttHelper == null) {
//                        mMqttHelper = MqttHelper.getInstance();
//                        setMqttCallback();
//                    }
//                }

                if (RabbitMQHelper.RABBITMQ_CONNECT_INTENT_ACTION.equalsIgnoreCase(intent.getAction())) {
                    if (RabbitMQHelper.connectionStatus == RabbitMQHelper.RabbitMQConnectionStatus.CONNECTED) {
                        System.out.println("RabbitMQConnectionStatus.CONNECTED");
                        setupMQListener();
                    }
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    public void onBackPressed() {
        System.out.println("Main Activity onBackPressed");
        int count = getSupportFragmentManager().getBackStackEntryCount();
        if (count == 0) {
            // Go to login page
            super.onBackPressed();
        } else {
            String taggedName = getSupportFragmentManager().getBackStackEntryAt(
                    getSupportFragmentManager().getBackStackEntryCount() - 1).getName();

            Log.d(TAG, "returned fragment matches" + taggedName);

            if (getSupportFragmentManager().findFragmentByTag(taggedName) instanceof MapShipBlueprintFragment) {
                mBottomNavigationView.setVisibility(View.VISIBLE);

//                System.out.println("getSupportFragmentManager().getBackStackEntryCount() is " + getSupportFragmentManager().getBackStackEntryCount());
//                String newTaggedName = getSupportFragmentManager().getBackStackEntryAt(
//                        getSupportFragmentManager().getBackStackEntryCount() - 2).getName();
//                String newTaggedName2 = getSupportFragmentManager().getBackStackEntryAt(
//                        getSupportFragmentManager().getBackStackEntryCount() - 3).getName();
//                System.out.println("newTaggedName is " + newTaggedName);
//                System.out.println("newTaggedName 2 is " + newTaggedName2);
//                System.out.println("MapFragment.class.getSimpleName() is " + MapFragment.class.getSimpleName());
//
//                System.out.println("getSupportFragmentManager().findFragmentByTag(\"android:switcher:\" + R.id.viewpager_main_nav + \":\" + 1) is " + getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager_main_nav + ":" + 1));
//                MapFragment returnedFragment = (MapFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.viewpager_main_nav + ":" + 1);
//                if (0 == mNoSwipeViewPager.getCurrentItem() && null != returnedFragment) {
//                    ((MapFragment) returnedFragment).onVisible();
//                }

                // Notify mapfragment of previous fragment (MapShipBlueprintFragment) that it was switched from
                EventBus.getDefault().post(PageEvent.getInstance().addPage(PageEvent.FRAGMENT_KEY, MapShipBlueprintFragment.class.getSimpleName()));

//                for (int i = 0; i < backStackCount; i++) {
//                    if (MapFragment.class.getSimpleName().equalsIgnoreCase(
//                            getSupportFragmentManager().getBackStackEntryAt(i).getName())) {
//
////                        MapFragment returnedFragment = (MapFragment) getSupportFragmentManager().
////                                findFragmentByTag(MapFragment.class.getSimpleName());
//                        returnedFragment.onVisible();
//                    }
//                }
            } else if (getSupportFragmentManager().findFragmentByTag(taggedName) instanceof TaskInnerFragment) {
                TaskInnerFragment returnedFragment = (TaskInnerFragment) getSupportFragmentManager().findFragmentByTag(taggedName);
                returnedFragment.refreshData();
            }

//            String fragmentTag = fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName();
//            Log.d(TAG, "fragmentTag is " + fragmentTag);
//            if (TaskInnerFragment.class.getSimpleName().equalsIgnoreCase(fragmentTag)) {
//
//                TaskInnerFragment returnedFragment = (TaskInnerFragment) fragmentManager.findFragmentByTag(fragmentTag);
//                returnedFragment.refreshUI();
//            }

            getSupportFragmentManager().popBackStack();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        mqttIntent = new Intent(getApplicationContext(), NetworkService.class);
//        startService(mqttIntent);
//        mIsServiceRegistered = getApplicationContext().bindService(mqttIntent, mMqttServiceConnection, Context.BIND_AUTO_CREATE);
//        registerMqttBroadcastReceiver();


//        subscribeToMQTT();
        subscribeToRabbitMQ();
    }

//    private void subscribeToMQTT() {
//        mMqttIntent = new Intent(getApplicationContext(), NetworkService.class);
//        startService(mMqttIntent);
//        mIsServiceRegistered = getApplicationContext().bindService(mMqttIntent, mMqttServiceConnection, Context.BIND_AUTO_CREATE);
//        registerMqttBroadcastReceiver();
//    }

    private void subscribeToRabbitMQ() {
        mMqttIntent = new Intent(getApplicationContext(), NetworkService.class);
        startService(mMqttIntent);
        mIsServiceRegistered = getApplicationContext().bindService(mMqttIntent, mMqttServiceConnection, Context.BIND_AUTO_CREATE);
        registerMqttBroadcastReceiver();

//        new RabbitMQTask().execute("");
//        RabbitMQHelper.getInstance(this.getApplicationContext()).startRabbitMQWithDefaultSetting();
//        if (RabbitMQHelper.connectionStatus == RabbitMQHelper.RabbitMQConnectionStatus.CONNECTED) {
//            setupMQListener();
//        }
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//
//        System.out.println("Service not registered");
//
//        if (mIsServiceRegistered) {
//            System.out.println("Service registered");
//            getApplicationContext().unbindService(mMqttServiceConnection);
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* Close service properly. Currently, the service is not destroyed, only the mqtt connection and
         * connection status are closed.
         */
        if (mIsServiceRegistered) {
//            stopService(mMqttIntent);
            getApplicationContext().unbindService(mMqttServiceConnection);
            mIsServiceRegistered = false;
        }
    }

//    private class RabbitMQTask extends AsyncTask<String, Void, String> {
//
//        protected String doInBackground(String... urls) {
//            RabbitMQHelper.getInstance().startRabbitMQWithDefaultSetting();
//            if (RabbitMQHelper.connectionStatus == RabbitMQHelper.RabbitMQConnectionStatus.CONNECTED) {
//                setupMQListener();
//            }
//            return "";
//        }
//
//        protected void onPostExecute(String result) {
//            // TODO: check this.exception
//            // TODO: do something with the result
//        }
//    }

//    public interface OnInitMQTTListener {
//        public void onInitMQTT(MqttHelper mqttHelper);
//    }


//    @Override
//    protected void onPause() {
//        super.onPause();
//
////        getApplicationContext().unbindService(mMqttServiceConnection);
//    }
}
