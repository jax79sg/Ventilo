package sg.gov.dsta.mobileC3.ventilo.network.jeroMQ;

import android.util.Log;

import com.google.gson.Gson;

import sg.gov.dsta.mobileC3.ventilo.model.sitrep.SitRepModel;
import sg.gov.dsta.mobileC3.ventilo.model.task.TaskModel;
import sg.gov.dsta.mobileC3.ventilo.model.user.UserModel;
import sg.gov.dsta.mobileC3.ventilo.util.GsonCreator;

public class JeroMQBroadcastOperation {

    private static final String TAG = JeroMQBroadcastOperation.class.getSimpleName();

    /**
     * Broadcasts insertion of data to connected devices in the network
     *
     * @param model
     */
    public static void broadcastDataInsertionOverSocket(Object model) {
        Log.d(TAG, "broadcastDataInsertionOverSocket");
        Gson gson = GsonCreator.createGson();
        String modelJson = gson.toJson(model);

        if (model instanceof UserModel) {
            JeroMQPublisher.getInstance().sendUserMessage(modelJson, JeroMQPublisher.TOPIC_INSERT);
        } else if (model instanceof SitRepModel) {
            JeroMQPublisher.getInstance().sendSitRepMessage(modelJson, JeroMQPublisher.TOPIC_INSERT);
        } else if (model instanceof TaskModel) {
            JeroMQPublisher.getInstance().sendTaskMessage(modelJson, JeroMQPublisher.TOPIC_INSERT);
        }
    }

    /**
     * Broadcasts update of data to connected devices in the network
     *
     * @param model
     */
    public static void broadcastDataUpdateOverSocket(Object model) {
        Log.d(TAG, "broadcastDataUpdateOverSocket");
        Gson gson = GsonCreator.createGson();
        String modelJson = gson.toJson(model);

        if (model instanceof UserModel) {
            JeroMQPublisher.getInstance().sendUserMessage(modelJson, JeroMQPublisher.TOPIC_UPDATE);
        } else if (model instanceof SitRepModel) {
            JeroMQPublisher.getInstance().sendSitRepMessage(modelJson, JeroMQPublisher.TOPIC_UPDATE);
        } else if (model instanceof TaskModel) {
            JeroMQPublisher.getInstance().sendTaskMessage(modelJson, JeroMQPublisher.TOPIC_UPDATE);
        }
    }

    /**
     * Broadcasts deletion of data to connected devices in the network
     *
     * @param model
     */
    public static void broadcastDataDeletionOverSocket(Object model) {
        Log.d(TAG, "broadcastDataDeletionOverSocket");
        Gson gson = GsonCreator.createGson();
        String modelJson = gson.toJson(model);

        if (model instanceof UserModel) {
            JeroMQPublisher.getInstance().sendUserMessage(modelJson, JeroMQPublisher.TOPIC_DELETE);
        } else if (model instanceof SitRepModel) {
            JeroMQPublisher.getInstance().sendSitRepMessage(modelJson, JeroMQPublisher.TOPIC_DELETE);
        } else if (model instanceof TaskModel) {
            JeroMQPublisher.getInstance().sendTaskMessage(modelJson, JeroMQPublisher.TOPIC_DELETE);
        }
    }
}