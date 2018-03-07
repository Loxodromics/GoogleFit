package net.quatur.googlefit;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.fitness.result.SessionStopResult;


/**
 * Created by philipp on 05.03.18.
 */

public class GoogleFitApiHelper {

    public static final String TAG = GoogleFitApiHelper.class.getSimpleName();
    private static GoogleFitApiHelper INSTANCE = null;
    private GoogleApiClient apiClient;

    private GoogleFitApiHelper() {
    }

    public static synchronized GoogleFitApiHelper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GoogleFitApiHelper();
        }
        return INSTANCE;
    }

    public void connect() {

        Log.i(TAG, "connect");
        apiClient.connect();
    }

    public void buildFitnessClient(Context context, GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        Log.i(TAG, "buildFitnessClient");
        apiClient = new GoogleApiClient.Builder(context)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.CONFIG_API)
                .addApi(Fitness.RECORDING_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(onConnectionFailedListener)
                .build();
    }

    public boolean hasClient() {
        return (apiClient != null);
    }

    public boolean isConnected() {
        return apiClient.isConnected();
    }

    public void unregisterListeners(GoogleApiClient.ConnectionCallbacks connectionCallbacks, GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
        if (connectionCallbacks != null) {
            Log.i(TAG, "unregisterConnectionCallbacks");
            apiClient.unregisterConnectionCallbacks(connectionCallbacks);
        }

        if (onConnectionFailedListener != null) {
            Log.i(TAG, "unregisterConnectionFailedListener");
            apiClient.unregisterConnectionFailedListener(onConnectionFailedListener);
        }
    }

    public void disconnect() {
        Log.i(TAG, "disconnect");
        apiClient.disconnect();
    }

    public boolean isConnecting() {
        return apiClient.isConnecting();
    }

    public void revokeFitPermissions(final Activity activity, final RevokeGoogleFitPermissionsListener revokePermissionsListener) {
        if (apiClient != null && apiClient.isConnected()) {
            Log.i(TAG, "disableFit");
            PendingResult<Status> pendingResult = Fitness.ConfigApi.disableFit(apiClient);
            pendingResult.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    Log.i(TAG, "Disconnect fit " + status.toString() + ", code " + status.getStatus().getStatusCode());
                    if (status.isSuccess()) {
                        Toast.makeText(activity, "googlefit disconnect success", Toast.LENGTH_LONG).show();
                        revokePermissionsListener.onRevokedFitPermissions();
                    } else {
                        Toast.makeText(activity, "googlefit disconnect failed", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Toast.makeText(activity, "googlefit disconnect failed notconnected", Toast.LENGTH_LONG).show();
        }

    }

    public PendingResult<SessionStopResult> stopSession(String oldSessionId) {
        Log.i(TAG, "stopSession: " + oldSessionId);
        return Fitness.SessionsApi.stopSession(apiClient, oldSessionId);
    }

    public PendingResult<Status> subscribe(final DataType dataType) {
        Log.i(TAG, "subscribe: " + dataType.getName());
        return Fitness.RecordingApi.subscribe(apiClient, dataType);
    }

    public PendingResult<DailyTotalResult> readDailyTotal(final DataType dataType) {
        Log.i(TAG, "readDailyTotal: " + dataType.getName());
        return Fitness.HistoryApi.readDailyTotal(apiClient, dataType);
    }

    public interface RevokeGoogleFitPermissionsListener {

        void onRevokedFitPermissions();
    }
}