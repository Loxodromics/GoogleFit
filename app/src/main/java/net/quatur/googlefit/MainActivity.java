package net.quatur.googlefit;

import android.content.Intent;
import android.content.IntentSender;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataType;

public class MainActivity extends AppCompatActivity implements GoogleFitApiHelper.RevokeGoogleFitPermissionsListener {

    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    private boolean authInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button requestStepsButton = (Button)findViewById(R.id.request_steps_button);
        requestStepsButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GoogleFitApiHelper.getInstance().buildFitnessClient(MainActivity.this, connectionCallbacks, onConnectionFailedListener);
                GoogleFitApiHelper.getInstance().connect();
            }
        });

        Button logoutButton = (Button)findViewById(R.id.logout_button);
        logoutButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GoogleFitApiHelper.getInstance().revokeFitPermissions(MainActivity.this, MainActivity.this);
                GoogleFitApiHelper.getInstance().disconnect();
            }
        });

        TextView outputTextview = (TextView)findViewById(R.id.output_textview);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                if (!GoogleFitApiHelper.getInstance().isConnecting() && !GoogleFitApiHelper.getInstance().isConnected()) {
                    GoogleFitApiHelper.getInstance().connect();
                }
            } else {
                // The user cancelled auth. Allow the subclass to handle updating the UI, etc.
                handleAuthCancelled();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!GoogleFitApiHelper.getInstance().hasClient()) {
            GoogleFitApiHelper.getInstance().buildFitnessClient(this, connectionCallbacks, onConnectionFailedListener);
        }
    }

    private GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {

        @Override
        public void onConnected(Bundle bundle) {
            Log.i(TAG, "Google Fit connected");

            subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE);
        }

        @Override
        public void onConnectionSuspended(int i) {
            // If your connection to the sensor gets lost at some point,
            // you'll be able to determine the reason and react to it here.
            if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                Log.i(TAG, "Connection lost.  Cause: Network Lost.");
            } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
            }
        }
    };

    private GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            Log.i(TAG, "Connection failed. Cause: " + result.toString());
            if (!result.hasResolution()) {
                GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), MainActivity.this, 0).show();
                return;
            }
            if (!authInProgress) {
                try {
                    Log.i(TAG, "Attempting to resolve failed connection");
                    authInProgress = true;
                    result.startResolutionForResult(MainActivity.this, REQUEST_OAUTH_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    Log.e(TAG, "Exception while starting resolution activity", e);
                }
            }
        }
    };

    private void subscribe(final DataType dataType) {
        PendingResult<Status> res = GoogleFitApiHelper.getInstance().subscribe(dataType);
        res.setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                String msg;
                if (status.isSuccess()) {
                    if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {
                        msg = "Already sub to " + dataType.getName();
                    } else {
                        msg = "Successfully subscribed! " + dataType.getName();
                    }
                } else {
                    msg = "There was a problem subscribing." + dataType.getName();
                }
                Log.i(TAG, msg);
            }
        });
    }

    @Override
    public void onRevokedFitPermissions() {
        Log.i(TAG, "revoked fit permissions");
//        GoogleFitApiHelper.getInstance().disconnect();
    }

    void handleAuthCancelled() {
        Log.i(TAG, "Auth cancelled");
    }
}
