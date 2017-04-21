package com.avenueinfotech.newlocation;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.avenueinfotech.newlocation.utils.GPSTracker;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback, OnMyLocationButtonClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        ActivityCompat.OnRequestPermissionsResultCallback, LocationListener {

    InterstitialAd mInterstitialAd;
    private InterstitialAd interstitial;

    private int PLACE_PICKER_REQUEST = 1;
    private AutoCompleteAdapter mAdapter;

    private TextView mTextView;
    private AutoCompleteTextView mPredictTextView;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

//    Button btnShow;
//    TextView textView;

    LocationManager locationManager;
    String provider;
    double latitude, logitude;

    private GoogleMap mMap;

    Button satbutton;
    Button norbutton;

    private GoogleApiClient mApiClient;
    ConnectionDetector cd;
    private ProgressDialog dialog;
    private static GPSTracker gps;
    final int REQUEST_LOCATION = 199;
    public final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;
    String[] PERMISSIONS = {android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE};


    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String SELECTED_STYLE = "selected_style";

    private int mSelectedStyleId = R.string.style_label_night;

    private int mStyleIds[] = {
            R.string.style_label_retro,
            R.string.style_label_night,
            R.string.style_label_grayscale,
            R.string.style_label_no_pois_no_transit,
            R.string.style_label_default,
    };

    private static final LatLng SYDNEY = new LatLng(-33.8688, 151.2091);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-1183672799205641~1735508619");

//        btnShow = (Button) findViewById(R.id.address);
//        textView = (TextView) findViewById(R.id.addresstxt);
        // Load an ad into the AdMob banner view.
        AdView adView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .setRequestAgent("android_studio:ad_template").build();
        adView.loadAd(adRequest);

        // Prepare the Interstitial Ad
        interstitial = new InterstitialAd(MainActivity.this);
// Insert the Ad Unit ID
        interstitial.setAdUnitId(getString(R.string.admob_interstitial_id));

        interstitial.loadAd(adRequest);
// Prepare an Interstitial Ad Listener
        interstitial.setAdListener(new AdListener() {
            public void onAdLoaded() {
                // Call displayInterstitial() function
                displayInterstitial();
            }
        });

        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(ActivityRecognition.API)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        mApiClient.connect();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, LOCATION_PERMISSION_REQUEST_CODE);

        } else {
            getLocation();
        }

//        btnShow.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                    return;
//                }
//                Location myLocation = locationManager.getLastKnownLocation(provider);
//                latitude = myLocation.getLatitude();
//                logitude = myLocation.getLongitude();
//
//                new GetAddress().execute(String.format("%.4f,%.4f",latitude,logitude));
//            }
//        });

        gps = new GPSTracker(this);

        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_ID_MULTIPLE_PERMISSIONS);
        } else {

            if (!gps.canGetLocation()) {
                switchGPS();
            }

//            GeneralUtils.createDirectory();
        }

        dialog = new ProgressDialog(MainActivity.this);
        dialog.setCancelable(false);

        cd = new ConnectionDetector(this);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
        if (cd.isConnected()) {
            Toast.makeText(MainActivity.this, "Internet Connected", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Please Enable Internet", Toast.LENGTH_LONG).show();
        }


        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



//        mTextView = (TextView) findViewById(R.id.textview);

//        mPredictTextView = (AutoCompleteTextView) findViewById(R.id.predicttextview);
//        mAdapter = new AutoCompleteAdapter(this);
//        mPredictTextView.setAdapter(mAdapter);
//
//        mPredictTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                AutoCompletePlace place = (AutoCompletePlace) parent.getItemAtPosition(position);
//                findPlaceById(place.getId());
//            }
//        });

        satellite();
        normal();
    }


    private void displayInterstitial() {

        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }



    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public void switchGPS() {
        {
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(15000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            // **************************
            builder.setAlwaysShow(true); // this is the key ingredient
            // **************************

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                    .checkLocationSettings(mApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates state = result
                            .getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can
                            // initialize location
                            // requests here.
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be
                            // fixed by showing the user
                            // a dialog.
                            try {
                                // Show the dialog by calling
                                // startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have
                            // no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            });
        }
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), false);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        final Location location = locationManager.getLastKnownLocation(provider);
        if (location == null)
            Log.e("ERROR", "Location is null");

    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (mApiClient != null)
//            mApiClient.connect();
//    }
//
//    @Override
//    protected void onStop() {
//        if (mApiClient != null && mApiClient.isConnected()) {
//            mAdapter.setGoogleApiClient(null);
//            mApiClient.disconnect();
//        }
//        super.onStop();
//    }

    private void findPlaceById(String id) {
        if (TextUtils.isEmpty(id) || mApiClient == null || !mApiClient.isConnected())
            return;

        Places.GeoDataApi.getPlaceById(mApiClient, id).setResultCallback(new ResultCallback<PlaceBuffer>() {
            @Override
            public void onResult(PlaceBuffer places) {
                if (places.getStatus().isSuccess()) {
                    Place place = places.get(0);
                    displayPlace(place);
                    mPredictTextView.setText("");
                    mAdapter.clear();
                }

                //Release the PlaceBuffer to prevent a memory leak
                places.release();
            }
        });
    }

    private void guessCurrentPlace() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi.getCurrentPlace(mApiClient, null);
        result.setResultCallback( new ResultCallback<PlaceLikelihoodBuffer>() {
            @Override
            public void onResult( PlaceLikelihoodBuffer likelyPlaces ) {

                PlaceLikelihood placeLikelihood = likelyPlaces.get( 0 );
                String content = "";
                if( placeLikelihood != null && placeLikelihood.getPlace() != null && !TextUtils.isEmpty( placeLikelihood.getPlace().getName() ) )
                    content = "Most likely you are at: " + placeLikelihood.getPlace().getName() + "\n";
                if( placeLikelihood != null )
                    content += "Percent of being there: " + (int) ( placeLikelihood.getLikelihood() * 100 ) + "%";
                mTextView.setText( content );

                likelyPlaces.release();
            }
        });
    }

    private void displayPlacePicker() {
        if( mApiClient == null || !mApiClient.isConnected() )
            return;

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult( builder.build((Activity) getApplicationContext()), PLACE_PICKER_REQUEST );
        } catch ( GooglePlayServicesRepairableException e ) {
            Log.d( "PlacesAPI Demo", "GooglePlayServicesRepairableException thrown" );
        } catch ( GooglePlayServicesNotAvailableException e ) {
            Log.d( "PlacesAPI Demo", "GooglePlayServicesNotAvailableException thrown" );
        }
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if( requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK ) {
            displayPlace( PlacePicker.getPlace( data, this ) );
        }
    }

    private void displayPlace( Place place ) {
        if( place == null )
            return;

        String content = "";
        if( !TextUtils.isEmpty( place.getName() ) ) {
            content += "Name: " + place.getName() + "\n";
        }
        if( !TextUtils.isEmpty( place.getAddress() ) ) {
            content += "Address: " + place.getAddress() + "\n";
        }
        if( !TextUtils.isEmpty( place.getPhoneNumber() ) ) {
            content += "Phone: " + place.getPhoneNumber();
        }

        mTextView.setText( content );
    }

//    private class GetAddress extends AsyncTask<String, Void, String> {
//        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
//
//        @Override
//        protected String doInBackground(String... strings) {
//            try {
//                double latitude = Double.parseDouble(strings[0].split(",")[0]);
//                double logitude = Double.parseDouble(strings[0].split(",")[1]);
//                String response;
//                HttpDataHandler http = new HttpDataHandler();
//                String url = String.format("https://maps.googleapis.com/maps/api/geocode/json?latlng=%.4f,%.4f&sensor=false", latitude, logitude);
//                response = http.GetHTTPData(url);
//                return response;
//            } catch (Exception ex) {
//
//            }
//            return null;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
////            dialog.setMessage("Please wait...");
////            dialog.setCanceledOnTouchOutside(true);
////            dialog.show();
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//            try {
//                JSONObject jsonObject = new JSONObject(s);
//                String address = ((JSONArray) jsonObject.get("results")).getJSONObject(0).get("formatted_address").toString();
//                textView.setText(address);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//            if (dialog.isShowing())
//                dialog.dismiss();
//        }
//
//    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Store the selected map style, so we can assign it when the activity resumes.
        outState.putInt(SELECTED_STYLE, mSelectedStyleId);
        super.onSaveInstanceState(outState);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        //      mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 14));


        setSelectedStyle();

        if (mMap != null) {

            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    Geocoder gc = new Geocoder(MainActivity.this);
                    LatLng ll = marker.getPosition();
                    double lat = ll.latitude;
                    double lng = ll.longitude;
                    List<Address> list = null;
                    try {
                        list = gc.getFromLocation(lat, lng, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Address add = list.get(0);
                    marker.setTitle(add.getAddressLine(1));
                    marker.showInfoWindow();


                }
            });

            mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    View v = getLayoutInflater().inflate(R.layout.info_window, null);

                    TextView tvLocality = (TextView) v.findViewById(R.id.tv_locality);
                    TextView tvLat = (TextView) v.findViewById(R.id.tv_lat);
                    TextView tvLng = (TextView) v.findViewById(R.id.tv_lng);
//                    TextView tvSnippet = (TextView) v.findViewById(R.id.tv_snippet);


                    LatLng ll = marker.getPosition();
                    tvLocality.setText(marker.getTitle());
                    tvLat.setText("Latiude: " + ll.latitude);
                    tvLng.setText("Longitude: " + ll.longitude);
//                    tvSnippet.setText(marker.getSnippet());


                    return v;
                }
            });
        }

        mMap.setOnMyLocationButtonClickListener(this);
        enableMyLocation();

        mMap.getUiSettings().setZoomControlsEnabled(true);

//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        mMap.setMyLocationEnabled(true);
    }

    private void setSelectedStyle() {
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.mapstyle_night));
    }

    private void goToLocationZoom(double lat, double lng, float zoom) {
        LatLng ll = new LatLng(lat, lng);
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 14);
        mMap.animateCamera(update);


    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    android.Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (mMap != null) {
            // Access to the location has been granted to the app.
            mMap.setMyLocationEnabled(true);
        }
    }

//    public void satelite(View v) {
//
//        satbutton = (Button)findViewById(R.id.satellite);
//        satbutton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
//            }
//        });
//
//    }



//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.styled_map, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.menu_style_choose) {
//            showStylesDialog();
//        }
//        return true;
//    }
@Override
public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu, menu);
    return super.onCreateOptionsMenu(menu);
}


    public boolean onOptionsItemSelected(MenuItem item) {

//        if( id == R.id.action_place_picker ) {
//            displayPlacePicker();
//            return true;
//        } else if( id == R.id.action_guess_current_place ) {
//            guessCurrentPlace();
//            return true;
//        }


//    private void setSelectedStyle() {
//        MapStyleOptions style;
//        switch (mSelectedStyleId) {
        switch (item.getItemId()){
            case R.id.mapTypeNone:
                // Sets the retro style via raw resource JSON.
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.mapstyle_labels));

                break;
            case R.id.mapTypeNight:
                // Sets the night style via raw resource JSON.
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(MainActivity.this, R.raw.mapstyle_night));

                break;
            case R.id.mapTypeSatellite:
                // Sets the grayscale style via raw resource JSON.
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                break;
            case R.id.mapTypeNormal:
                // Sets the grayscale style via raw resource JSON.
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                break;
//            case R.string.style_label_no_pois_no_transit:
//                // Sets the no POIs or transit style via JSON string.
//                style = new MapStyleOptions("[" +
//                        "  {" +
//                        "    \"featureType\":\"poi.business\"," +
//                        "    \"elementType\":\"all\"," +
//                        "    \"stylers\":[" +
//                        "      {" +
//                        "        \"visibility\":\"off\"" +
//                        "      }" +
//                        "    ]" +
//                        "  }," +
//                        "  {" +
//                        "    \"featureType\":\"transit\"," +
//                        "    \"elementType\":\"all\"," +
//                        "    \"stylers\":[" +
//                        "      {" +
//                        "        \"visibility\":\"off\"" +
//                        "      }" +
//                        "    ]" +
//                        "  }" +
//                        "]");
//                break;
//            case R.string.style_label_default:
//                // Removes previously set style, by setting it to null.
//                style = null;
//                break;
            default:
                break;
        }
//        mMap.setMapStyle(style);
        return super.onOptionsItemSelected(item);

    }

    private void satellite() {
        satbutton = (Button)findViewById(R.id.satellite);
        satbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            }
        });
    }

    private void normal() {
        norbutton = (Button)findViewById(R.id.normal);
        norbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            }
        });
    }




//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.string.style_label_retro:
//                MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_retro);
//                break;
//            case R.string.style_label_night:
//                MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_night);
//                break;
//            case R.string.style_label_grayscale:
//                MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_grayscale);
//                break;
//            case R.string.style_label_default:
//                MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_night);
//                break;
//
//            default:
//                break;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Display the missing permission error dialog when the fragments resume.
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            mPermissionDenied = false;
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    Marker marker;

    public void geoLocate(View view) throws IOException {

        EditText et  = (EditText) findViewById(R.id.editText);
        String location = et.getText().toString();

        Geocoder gc = new Geocoder(this);
        List<Address> list = gc.getFromLocationName(location, 1);
        Address address = list.get(0);
        String locality = address.getLocality();
//        String area = address.getLocality();


        Toast.makeText(this, locality, Toast.LENGTH_LONG).show();


        double lat = address.getLatitude();
        double lng = address.getLongitude();
        goToLocationZoom(lat, lng, 10);

        setMarker(locality, lat, lng);

    }

    private void setMarker(String locality, double lat, double lng) {
        if (marker != null) {
            marker.remove();
        }

        MarkerOptions options = new MarkerOptions()
                .title(locality)
                .draggable(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.redloca))
                .position(new LatLng(lat, lng))
                .snippet("I am here");

        marker = mMap.addMarker(options);
    }


    @Override
    public void onLocationChanged(Location location) {
        if(location == null){
            Toast.makeText(this, "Cant get current location", Toast.LENGTH_LONG).show();
        } else {
            LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
            CameraUpdate update = CameraUpdateFactory.newLatLngZoom(ll, 18);
            mMap.animateCamera(update);
//            textView.setText("Long: " + location.getLongitude() + " Lat: " + location.getLatitude() +  " Alt:" + location.getAltitude() +  " Acc:" + location.getAccuracy()+  "m" );



        }

    }

    LocationRequest mLocationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(390000);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mApiClient, mLocationRequest, this);
//        Log.i("LOG", "onConnection(" + bundle + ")");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
