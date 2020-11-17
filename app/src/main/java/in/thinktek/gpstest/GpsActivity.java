package in.thinktek.gpstest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import static in.thinktek.gpstest.MainActivity.distance;


public class GpsActivity extends AppCompatActivity
        implements LocationListener, ResultCallback<LocationSettingsResult> {

    private static final String TAG = GpsActivity.class.getSimpleName();

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private Boolean mRequestingLocationUpdates;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private static final int REQUEST_CHECK_SETTINGS = 3;

    CardView cardViewLocationCenter;
    TextView textViewLocationCenterTag;
    TextInputLayout textInputLayoutLocationCenterLat;
    EditText editTextLocationCenterLat;
    TextInputLayout textInputLayoutLocationCenterLan;
    EditText editTextLocationCenterLan;
    Button buttonRecordLocationCenter;

    double latitude = 0, longitude = 0, savedLatitude = 0, savedLongitude = 0;
    boolean classroomLocationRecordCenter = false;

    // Flag permission
    private boolean mFlagGranted = true;

    public static Location MY_CURRENT_LOCATION;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        mRequestingLocationUpdates = false;

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        cardViewLocationCenter = findViewById(R.id.card_view_location_center);
        textViewLocationCenterTag = findViewById(R.id.text_view_location_center_tag);
        textInputLayoutLocationCenterLat = findViewById(R.id.text_input_layout_location_center_lat);
        editTextLocationCenterLat = findViewById(R.id.edit_text_location_center_lat);
        textInputLayoutLocationCenterLan = findViewById(R.id.text_input_layout_location_center_lan);
        editTextLocationCenterLan = findViewById(R.id.edit_text_location_center_lan);
        buttonRecordLocationCenter = findViewById(R.id.button_record_location_center);

        // Kick off the process of building the LocationCallback, LocationRequest, and
        // LocationSettingsRequest objects.
        createLocationCallback();
        createLocationRequest();
        buildLocationSettingsRequest();

        buttonRecordLocationCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                double distance = distance(latitude,longitude,savedLatitude,savedLongitude);
                textViewLocationCenterTag.setText("Current Latitude is - "+String.valueOf(latitude)+"\n"+"Current Longitude is - "+String.valueOf(longitude)+
                        "\n\nPrevious Latitude is - "+String.valueOf(savedLatitude)+"\n"+"Previous Longitude is - "+String.valueOf(savedLongitude)+
                        "\n\nDistance - "+String.valueOf(distance)
                );
                textViewLocationCenterTag.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                if (classroomLocationRecordCenter) {
                    classroomLocationRecordCenter = false;
                    stopLocationUpdates();
                } else {
                    classroomLocationRecordCenter = true;
                    startButtonLocationUpdates(view.getContext());
                }
                setViews();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we remove location updates. Here, we resume receiving
        // location updates if the user has requested them.
        if (mRequestingLocationUpdates && checkPermissions()) {
            startLocationUpdates(this);
        } else if (!checkPermissions()) {
            requestPermissions();
        }

        Log.d(TAG, "onResume");

        if(mFlagGranted){
            startLocationUpdates(this);
        }

        updateUI();
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale,
                    android.R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(GpsActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(GpsActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove location updates to save battery.
        stopLocationUpdates();
    }

    public void startButtonLocationUpdates(Context context) {
        savedLatitude = latitude;
        savedLongitude = longitude;

        Log.i(TAG, "startButtonLocationUpdates");
        if (!mRequestingLocationUpdates) {
            Log.i(TAG, "startButtonLocationUpdates !mRequestingLocationUpdates");
            mRequestingLocationUpdates = true;
            startLocationUpdates(context);
        }
    }

    private void startLocationUpdates(final Context context) {
        // When request permission mFlagGranted false
        mFlagGranted = false;

        Log.i(TAG, "startLocationUpdates");
        // Begin by checking if the device has the necessary location settings.
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        Log.i(TAG, "All location settings are satisfied.");

                        //noinspection MissingPermission
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                                        != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                                mLocationCallback, Looper.myLooper());

                        updateUI();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int statusCode = ((ApiException) e).getStatusCode();
                        switch (statusCode) {
                            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " +
                                        "location settings ");
                                try {
                                    // Show the dialog by calling startResolutionForResult(), and check the
                                    // result in onActivityResult().
                                    ResolvableApiException rae = (ResolvableApiException) e;
                                    rae.startResolutionForResult(GpsActivity.this, REQUEST_CHECK_SETTINGS);
                                } catch (IntentSender.SendIntentException sie) {
                                    Log.i(TAG, "PendingIntent unable to execute request.");
                                }
                                break;

                            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                String errorMessage = "Location settings are inadequate, and cannot be " +
                                        "fixed here. Fix in Settings.";
                                Log.e(TAG, errorMessage);
                                Toast.makeText(GpsActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                mRequestingLocationUpdates = false;
                        }

                        updateUI();
                    }
                });

        // after get permission mFlagGranted true
        mFlagGranted = true;
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        Log.d(TAG, "onResult");

        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.d(TAG, "All location settings are satisfied.");
                if (MY_CURRENT_LOCATION == null) {
                    Log.d(TAG, "onResult SUCCESS MY_CURRENT_LOCATION == null");
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED
                            && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                            != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }

                    mFusedLocationClient.getLastLocation()
                            .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    // Got last known location. In some rare situations, this can be null.
                                    if (location != null) {
                                        // Logic to handle location object
                                        MY_CURRENT_LOCATION = location;
                                        Log.d(TAG, "Reached new location code");
                                        latitude = MY_CURRENT_LOCATION.getLatitude();
                                        longitude = MY_CURRENT_LOCATION.getLongitude();

                                        updateUI();
                                    }
                                }
                            });

                    //MY_CURRENT_LOCATION = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    //USER_LAT = MY_CURRENT_LOCATION.getLatitude();
                    //USER_LON = MY_CURRENT_LOCATION.getLongitude();
                }
                startLocationUpdates(this);
                break;

            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.d(TAG,
                        "Location settings are not satisfied. Show the user a dialog to" +
                                "upgrade location settings ");

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException e) {
                    Log.d(TAG, "PendingIntent unable to execute request.");
                }
                break;

            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.d(TAG,
                        "Location settings are inadequate, and cannot be fixed here. Dialog " +
                                "not created.");
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        MY_CURRENT_LOCATION = location;
        latitude = MY_CURRENT_LOCATION.getLatitude();
        longitude = MY_CURRENT_LOCATION.getLongitude();

        Log.d(TAG, "onLocationChanged");
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void createLocationCallback() {
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                mCurrentLocation = locationResult.getLastLocation();
                updateUI();
            }
        };
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }

    private void stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            return;
        }

        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        mRequestingLocationUpdates = false;
                    }
                });
    }

    private void updateUI() {
        if (mCurrentLocation != null) {
            latitude = mCurrentLocation.getLatitude();
            longitude = mCurrentLocation.getLongitude();
            setViews();
        }
    }

    private void setViews() {
        if(classroomLocationRecordCenter){
            editTextLocationCenterLat.setText(String.valueOf(latitude));
            editTextLocationCenterLan.setText(String.valueOf(longitude));

            buttonRecordLocationCenter.setText("RECORDING");
        }else{
            buttonRecordLocationCenter.setText("LOCKED");
        }
    }
}