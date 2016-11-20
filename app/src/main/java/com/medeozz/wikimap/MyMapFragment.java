package com.medeozz.wikimap;

// android.*
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

// com.afollestad.materialdialogs.*
import com.afollestad.materialdialogs.MaterialDialog;

// com.medeozz.wikimap.model.*
import com.medeozz.wikimap.model.WikiArticle;

// com.nispok.snackbar.*
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.enums.SnackbarType;

// com.squareup.*
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

// java.*
import java.io.IOException;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// org.json.*
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// com.google.maps.*
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

// com.google.android.gms.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

public class MyMapFragment extends Fragment implements
        MainActivity.OnMyClickListener,
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback,
        ClusterManager.OnClusterClickListener<WikiArticle>,
        ClusterManager.OnClusterItemClickListener<WikiArticle> {

    //<editor-fold desc="== globale Variablen ==">
    private static final boolean IS_RELEASE = false;
    private static final int BIG_IMAGE_DOWNLOAD_SIZE = 600; // px
    private static final int LIST_IMAGE_DOWNLOAD_SIZE = 500; // px
    private static final int THUMBNAIL_IMAGE_DOWNLOAD_SIZE = 60; // px
    private static final int THUMBNAIL_IMAGE_DISPLAY_SIZE_IN_MM = 7; // mm
    private static final float MARKER_TEXT_SIZE = 14f; // sp
    private int thumbnailImageDisplaySize;
    private int bigImageDisplaySize;
    private static final int FASTEST_INTERVAL_IN_MS = 1000; // ms
    private static final int DISPLACEMENT_IN_M = 0; // m
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private static final String TAG_USERNAME = "wom8fm3ldm352p3";
    private static final String TAG = "WIKIMAP";
    private GoogleMap googleMap;
    private Location lastLocation;
    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private static SharedPreferences shPref;
    private boolean haveToCleanupMap = false;
    private boolean afterAppStart = true;
    private boolean firstMapMove = true;
    private int numArticles = 0;
    private MaterialDialog downloadProgress;
    private MaterialDialog waitForInfoWindowProgress;
    private ClusterManager<WikiArticle> clusterManager;
    private List<LatLng> mItems = new ArrayList<>();
    private boolean downloadedSnackbar = true;
    private OkHttpClient okHttpClient = new OkHttpClient();
    private String thumbnailStoragePath;
    private File mFolder;
    private DbHelper db;
    private DownloadManager downloadManager;
    private boolean isWorking = false;
    private MyStateManager stateManager;
    //</editor-fold>

    //<editor-fold desc="onCreate() etc.">
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.layout_map, container, false);

        ClickableFrameLayout clickableFrameLayout1 = (ClickableFrameLayout) v.findViewById(R.id.open_drawer);
        clickableFrameLayout1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOpenDrawerClick();
            }
        });

        ClickableFrameLayout clickableFrameLayout2 = (ClickableFrameLayout) v.findViewById(R.id.scroll_map);
        clickableFrameLayout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onMapScrollingClick();
            }
        });

        ClickableFrameLayout clickableFrameLayout3 = (ClickableFrameLayout) v.findViewById(R.id.scroll_map_once);
        clickableFrameLayout3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLocation();
            }
        });

        LinearLayout linearLayout = (LinearLayout) v.findViewById(R.id.num_marker_click);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(v.getContext(), CardviewActivity.class));
            }
        });

        ClickableFrameLayout clickableFrameLayout4 = (ClickableFrameLayout) v.findViewById(R.id.download_articles);
        clickableFrameLayout4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDownloadArticlesClick();
            }
        });

        return v;
    }

    private void onOpenDrawerClick() {
        ((MaterialNavigationDrawer)this.getActivity()).openDrawer();
    }

    private void onMapScrollingClick() {

        ImageView iv = (ImageView) getActivity().findViewById(R.id.mapscrolling_icon);

        if(stateManager.getMapScrolling()) { // der Wert von mapScrolling wird in MyMapFragment über das Interface geändert (s.u.)
            iv.setImageResource(R.drawable.ic_location_disabled_white);
        } else {
            iv.setImageResource(R.drawable.ic_action_location_dark);
        }

        boolean mapScrolling = stateManager.getMapScrolling();
        stateManager.setMapScrolling(!mapScrolling);
        displayLocation();
    }

    private void onDownloadArticlesClick() {

        if(!isOnline()) { // nichts machen, wenn offline
            showNoInternetAlert();
            return;
        }

        if(isWorking) return; // nichts machen, wenn bereits geklickt wurde

        if(haveToCleanupMap) {
            cleanupMap(false);
            haveToCleanupMap = false;
        }

        buildURL();
        isWorking = true;
        //Log.e(TAG, "onDownloadArticlesClicked() in MyMapFragment.");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        //Log.e(TAG, "onAttach()");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //stopLocationUpdates();
        //Log.e(TAG, "onDetach()");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SupportMapFragment mapFragment = (SupportMapFragment) getFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance(); // https://developer.android.com/reference/com/google/android/gms/maps/SupportMapFragment.html#SupportMapFragment%28%29
            mapFragment.getMapAsync(this);
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.add(R.id.map, mapFragment).commit();
        }
        //Log.e(TAG, "onCreate()");

        PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, false);
        shPref = PreferenceManager.getDefaultSharedPreferences(getActivity());

        db = new DbHelper(getActivity());

        stateManager = ((WikiMapApplication) getActivity().getApplicationContext() ).getStateManager();

        downloadManager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        getActivity().registerReceiver(onDownloadNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        if (playServicesAvailable()) {
            buildGoogleApiClient();
            createLocationRequest();
        } else {
            Toast.makeText(getActivity(), "GooglePlayServices not available! 01", Toast.LENGTH_LONG).show();
        }

        thumbnailStoragePath = Environment.getExternalStorageDirectory() + "/WikiMap/thumbs";
        mFolder = new File(thumbnailStoragePath);
        boolean success = true;
        if (!mFolder.exists()) {
            success = mFolder.mkdirs();
        }
        if(!success) {
            Log.e(TAG + "_onCreate", " mkdirs() ging nicht!");
        }

        setThumbnailSize();
        setBigImageDisplaySize();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (googleApiClient != null) {
            googleApiClient.connect();
        } else {
            Log.e(TAG, "onStart(): googleApiClient = null");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        //Log.e(TAG, "onStop()");
    }

    @Override
    public void onPause() {
        super.onPause();
        //stopLocationUpdates();
        //Log.e(TAG, "onPause()");
    }

    @Override
    public void onResume() {
        super.onResume();

        if(isOnline()) new fetchWikiListImageUrlTask().execute();

        Log.e(TAG, "onResume()");
        setThumbnailSize();
        setBigImageDisplaySize();

        // Resuming the periodic location updates
        //if (googleApiClient.isConnected()) {
        //    startLocationUpdates();
        //}
        //Toast.makeText(getApplicationContext(), "onResume()", Toast.LENGTH_LONG).show();

        //<editor-fold desc="Snackbar mit Versioninfo beim ersten Start nach Update">
        try {
            // aktuelle Version holen
            PackageInfo packageInfo;
            packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            final String currentVersionName = packageInfo.versionName;
            final int currentVersionCode = packageInfo.versionCode;

            int lastVersion = 0;
            if (shPref.contains("lastVersion")) {
                lastVersion = shPref.getInt("lastVersion", 0);
            }

            final int oldVersionCode = lastVersion;

            // nach Update oder Erstinstallation
            if (currentVersionCode > oldVersionCode) {

                if(isOnline()) {
                    new watchDogTask().execute(currentVersionName);
                    shPref.edit().putBoolean("hasToStartwatchDogTask", false).apply();
                } else {
                    shPref.edit().putBoolean("hasToStartwatchDogTask", true).apply();
                }

                // die Anzeige der Snackbar um Sekunden verzögern
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        showMyAwesomeSnackbar("Version " + currentVersionName + "   Have fun!", false);
                    }
                }, 4000);

                // aktuelle Version abspeichern
                shPref.edit().putInt("lastVersion", currentVersionCode).apply();

                // "154 Artikel auf früheren Downloads wiederhergestellt" für diese Sitzung NICHT anzeigen
                downloadedSnackbar = false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        //</editor-fold>
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        getActivity().unregisterReceiver(onDownloadNotificationClick);
        db.close();
        Log.e(TAG, "onDestroy()");
        getActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        getActivity().finish();
    }
    //</editor-fold>

    //<editor-fold desc="Sonstiges">
    @Override
    public void onMyBackPressed() {
        // HOME-Taste simulieren, damit evtl. vorhandene Downloads beendet werden können
        if(isWorking) {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME));
        } else {
            getActivity().finish();
        }
        //Log.e(TAG, "onMyBackPressed() in MyMapFragment.");
    }

    @Override
    public void onMaptypeNormalClicked() {
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    @Override
    public void onMaptypeHybridClicked() {
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }

    @Override
    public void onMaptypeTerrainClicked() {
        googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
    }

    @Override
    public void onDeleteMarkersClicked() {
        cleanupMap(true);
    }

    public void showMyAwesomeSnackbar(String message, boolean stick) {

        Snackbar.SnackbarDuration snackbarDuration;
        if(stick) snackbarDuration = Snackbar.SnackbarDuration.LENGTH_INDEFINITE;
        else snackbarDuration = Snackbar.SnackbarDuration.LENGTH_LONG;

        int textcolor;
        int snackbarcolor;
        if(googleMap.getMapType() == 1 || googleMap.getMapType() == 3) { // Normal und Terrain
            textcolor = R.color.lightorange;
            snackbarcolor = R.color.almostBlack;
        } else {
            textcolor = R.color.black;
            snackbarcolor = R.color.white;
        }

        Snackbar.with(getActivity()) // context
                .text(message) // text to display
                .type(SnackbarType.MULTI_LINE)
                .textColorResource(textcolor)
                .colorResource(snackbarcolor) // background color
                .duration(snackbarDuration)
                .margin(25, 30)
                .swipeToDismiss(true)
                //.textTypeface(TypefaceUtils.load(getActivity().getAssets(), "fonts/Roboto-Regular.ttf"))
                .show(getActivity()); // activity where it is displayed
    }

    protected boolean isOnline() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public void showNoInternetAlert() {
        new MaterialDialog.Builder(getActivity())
                .cancelable(true)
                .title(R.string.you_are_offline)
                .iconRes(R.drawable.ic_action_warning)
                .positiveText(R.string.ok) // rechts
                .content(R.string.please_try_again)
                .show();
    }

    private void setThumbnailSize() {

        // mm in Pixel umrechnen
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, THUMBNAIL_IMAGE_DISPLAY_SIZE_IN_MM, getResources().getDisplayMetrics());

        thumbnailImageDisplaySize = Math.round(px);

        // jetzt noch an Preferences anpassen
        String thumbSize = "normal";
        if (shPref.contains("pref_thumbsize")) {
            thumbSize = shPref.getString("pref_thumbsize", "normal");
        }
        switch (thumbSize) {
            case "huge":  thumbnailImageDisplaySize = (int) Math.round(thumbnailImageDisplaySize * 1.5); break;
            case "big":  thumbnailImageDisplaySize = (int) Math.round(thumbnailImageDisplaySize * 1.25); break;
            case "normal": break; // nichts ändern
            case "small": thumbnailImageDisplaySize = (int) Math.round(thumbnailImageDisplaySize * 0.75); break;
            case "tiny": thumbnailImageDisplaySize = (int) Math.round(thumbnailImageDisplaySize * 0.5); break;
        }

    }

    private void setBigImageDisplaySize() {

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int widthPixels = metrics.widthPixels;
        int heightPixels = metrics.heightPixels;
        float widthDpi = metrics.xdpi;
        float heightDpi = metrics.ydpi;
        float widthInches = widthPixels / widthDpi;
        float heightInches = heightPixels / heightDpi;
        double diagonalInches = Math.sqrt( (widthInches * widthInches) + (heightInches * heightInches) );  // a² + b² = c²

        int bigPicSize_in_mm;
        if (diagonalInches >= 9.5) { // TODO: für mehr Displaygrößen testen
            // 10"-12" Tablet
            bigPicSize_in_mm = 100;
            //Log.i(TAG, "10 Zoll, diagonalInches = " + diagonalInches);
        } else if (diagonalInches >= 6.5) {
            // 7"-9" Tablet
            bigPicSize_in_mm = 85;
            //Log.i(TAG, "7 Zoll, diagonalInches = " + diagonalInches);
        } else {
            // 5" Handy und kleiner
            bigPicSize_in_mm = 53;
            //Log.i(TAG, "5 Zoll, diagonalInches = " + diagonalInches);
        }

        // mm in Pixel umrechnen
        float bigPicSize_in_px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, bigPicSize_in_mm, getResources().getDisplayMetrics());

        bigImageDisplaySize = Math.round(bigPicSize_in_px);

    }

    private void checkIfArticlesInDb() {

        List<WikiArticle> wikiArticles = new ArrayList<>();

        // alles aus der DB holen
        Cursor c = db.getWritableDatabase().rawQuery("SELECT " +
                DbHelper.LAT            + ", " +
                DbHelper.LNG            + ", " +
                DbHelper.TITLE          + ", " +
                DbHelper.SUMMARY        + ", " +
                DbHelper.WIKIPEDIA_URL  + ", " +
                DbHelper.ELEVATION      + ", " +
                DbHelper.LANG           + ", " +
                DbHelper.THUMBNAIL_IMAGE_NAME +
                " FROM " + DbHelper.TABLE_ARTICLES_NAME, null);

        while (c.moveToNext()) {

            double lat = c.getDouble(0);
            double lng = c.getDouble(1);

            LatLng articlePosition = new LatLng(lat, lng);

            mItems.add(articlePosition);
            wikiArticles.add(new WikiArticle(lat, lng, c.getString(2), c.getString(3), c.getString(4), c.getInt(5), c.getString(6), c.getString(7)));

        }
        c.close();

        int count = mItems.size();

        if(count > 0) {
            String message = count + " " + getString(R.string.articles_recovered) + "...";

            showWikiArticlesOnMap(wikiArticles, message);
        } else {
            TextView tv = (TextView) getActivity().findViewById(R.id.num_markers);
            tv.setText(String.valueOf(numArticles));
        }
    }

    BroadcastReceiver onDownloadNotificationClick = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
        }
    };

    private class watchDogTask extends AsyncTask<String, Void, Void> {
        // android.os.AsyncTask<Params, Progress, Result>

        @Override
        protected Void doInBackground(String... arg) {

            if(IS_RELEASE) {
                try {

                    String modellUndVersion = android.os.Build.MODEL + ", Android " + String.valueOf(android.os.Build.VERSION.RELEASE);
                    RequestBody formBody = new FormEncodingBuilder()
                            .add("neue_Installation_auf_Modell", modellUndVersion)
                            .add("WikiMap Version", arg[0])
                            .build();
                    Request request = new Request.Builder()
                            .url("http://medeozz.com/wikimap_crashes.php")
                            .post(formBody)
                            .build();

                    okHttpClient.newCall(request).execute();

                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            return null;
        }
    }
    //</editor-fold>

    //<editor-fold desc="GPS und GoogleMap">
    public void displayLocation() {

        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        //Log.e(TAG, "displayLocation(): lastLocation: " + lastLocation + ", googleApiClient: " + googleApiClient);

        if (lastLocation != null) {
            if (googleMap != null) {
                if (!afterAppStart) {

                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude())));
                } else {

                    afterAppStart = false;

                    float sz = 12f;
                    if (shPref.contains("pref_startzoom")) {
                        sz = Float.parseFloat(shPref.getString("pref_startzoom", "12"));
                    }
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()), sz));
                }
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.no_location), Toast.LENGTH_LONG).show();
        }
    }

    private void buildGoogleApiClient() {

        googleApiClient = new GoogleApiClient
                .Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {

        int updateInterval = 1000;
        if (shPref.contains("pref_updateinterval")) {
            updateInterval = Integer.valueOf(shPref.getString("pref_updateinterval", "1000"));
        }
        locationRequest = new LocationRequest();
        locationRequest.setInterval(updateInterval);
        locationRequest.setFastestInterval(FASTEST_INTERVAL_IN_MS);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setSmallestDisplacement(DISPLACEMENT_IN_M);
    }

    private boolean playServicesAvailable() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());

        if (resultCode != ConnectionResult.SUCCESS) {

            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {

                GooglePlayServicesUtil.getErrorDialog(resultCode, getActivity(), PLAY_SERVICES_RESOLUTION_REQUEST).show();

            } else {

                Toast.makeText(getActivity().getApplicationContext(), "This device is not supported.", Toast.LENGTH_LONG).show();
                getActivity().finish();
            }
            return false;
        }
        return true;
    }

    protected void startLocationUpdates() {

        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle bundle) {

        if(firstMapMove || stateManager.getMapScrolling()) {
            displayLocation();
            //Log.e(TAG, "onConnected() ruft displayLocation() auf");
        }

        startLocationUpdates();

    }

    @Override
    public void onConnectionSuspended(int arg) {

        if(googleApiClient != null) {
            googleApiClient.connect();
        } else {
            Log.e(TAG, "onConnectionSuspended: googleApiClient = null");
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        lastLocation = location;

        //Toast.makeText(getApplicationContext(), "Location changed!", Toast.LENGTH_SHORT).show();
        if(firstMapMove || stateManager.getMapScrolling() ) {

            displayLocation();
            //Log.e(TAG, "onLocationChanged() ruft displayLocation() auf");

            if (firstMapMove) {
                firstMapMove = false;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        // globale Variable initialisieren
        this.googleMap = googleMap;

        // Kartentyp laut Einstellungen
        String mapType = "normal";
        if (shPref.contains("pref_maptype")) {
            mapType = shPref.getString("pref_maptype", "normal");
        }
        switch (mapType) {
            case "normal":    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);    break;
            case "hybrid":    googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);    break;
            case "terrain":   googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);   break;
        }

        // meine Position als blauen Punkt in der Karte anzeigen
        googleMap.setMyLocationEnabled(true);

        // MyLocation-Button oben rechts ggf. einfügen
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);

        // ClusterManager aktivieren
        clusterManager = new ClusterManager<>(getActivity(), googleMap);

        // Aussehen der Marker an WikiArticleRenderer abgeben
        clusterManager.setRenderer(new WikiArticleRenderer());

        // Cluster ändern beim Zoomen
        googleMap.setOnCameraChangeListener(clusterManager);

        // MarkerClicks von GoogleMap an ClusterManager abgeben
        googleMap.setOnMarkerClickListener(clusterManager);

        // ClusterManager verwaltet ClusterClicks
        clusterManager.setOnClusterClickListener(this);

        // ClusterManager verwaltet MarkerClicks
        clusterManager.setOnClusterItemClickListener(this);

        // Die Karte sollte zuerst zu sehen sein
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkIfArticlesInDb();
            }
        }, 3000);

    }
    //</editor-fold>

    //<editor-fold desc="Wikipedia">
    public void buildURL() {

        // aktuelles Zentrum der Karte holen
        LatLng center = googleMap.getCameraPosition().target;
        double centerLat = center.latitude;
        double centerLng = center.longitude;

        String wikiLang = stateManager.getSystemLanguage();
        //Log.e(TAG, stateManager.getSystemLanguage());
        if (shPref.contains("pref_wikilang")) {
            wikiLang = shPref.getString("pref_wikilang", "en");
        }

        String maxRows = "20";
        if (shPref.contains("pref_maxrows")) {
            maxRows = shPref.getString("pref_maxrows", "20");
        }
        // URL bauen
        String myURL = "http://api.geonames.org/findNearbyWikipediaJSON" +
                "?lang="        + wikiLang +
                "&lat="         + centerLat +
                "&lng="         + centerLng +
                "&radius=20"    +
                "&maxRows="     + maxRows +
                "&username="    + TAG_USERNAME;

        //Log.e("WIKIMAP_buildURL", myURL);
        new fetchWikiArticlesTask().execute(myURL);

        // war wohl offline bei der Installation, daher jetzt!
        boolean hasToStartwatchDogTask = true;
        if (shPref.contains("hasToStartwatchDogTask")) {
            hasToStartwatchDogTask = shPref.getBoolean("hasToStartwatchDogTask", true);
        }
        if(hasToStartwatchDogTask) {

            try {
                // aktuelle Version holen
                PackageInfo packageInfo;
                packageInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                final String currentVersionName = packageInfo.versionName;

                new watchDogTask().execute(currentVersionName);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            shPref.edit().putBoolean("hasToStartwatchDogTask", false).apply();
        }
    }

    private class fetchWikiArticlesTask extends AsyncTask<String, Void, Void> {
        // android.os.AsyncTask<Params, Progress, Result>

        public List<WikiArticle> wikiArticles = new ArrayList<>();
        private final Handler handler = new Handler();

        @Override
        protected void onPreExecute() {

            boolean downloadInBackground = shPref.contains("pref_downl_dlg") && shPref.getBoolean("pref_downl_dlg", false);
            if (!downloadInBackground) downloadProgressAlert();
        }

        @Override
        protected Void doInBackground(String... arg) {

            try {
                URL mURL = new URL(arg[0]); // URL-Objekt aus String arg[0] bauen und übergeben

                Request mRequest = new Request.Builder().url(mURL).build();
                Response mResponse = okHttpClient.newCall(mRequest).execute();
                String jsonStr = mResponse.body().string();
                JSONObject rootObj = new JSONObject(jsonStr);
                JSONArray geonamesArr = rootObj.getJSONArray("geonames");

                //<editor-fold desc="JSON Datei auslesen und wikiArticles füllen; Bild-URL holen und Bilder runterladen">
                for (int i = 0; i < geonamesArr.length(); i++) {

                    JSONObject geonamesObj = geonamesArr.getJSONObject(i);

                    ContentValues values = new ContentValues();

                    double lat = 0;
                    if (geonamesObj.has("lat")) lat = geonamesObj.getDouble("lat");
                    values.put(DbHelper.LAT, lat);

                    double lng = 0;
                    if (geonamesObj.has("lng")) lng = geonamesObj.getDouble("lng");
                    values.put(DbHelper.LNG, lng);

                    // eindeutige Marker-ID backen
                    String markerId = String.valueOf(lat) + "-" + String.valueOf(lng);
                    values.put(DbHelper.MARKER_ID, markerId);

                    LatLng articlePosition = new LatLng(lat, lng);

                    if(mItems.indexOf(articlePosition) >= 0) {
                        // wenn Artikel bereits in der Liste der AKTUELLEN Sitzung,
                        // dann ist er auch schon in der Datenbank
                        continue; // Schleife oben weitermachen
                    }

                    String title = "";
                    String titleForDb = "";
                    if (geonamesObj.has("title")) {
                        title = geonamesObj.getString("title");
                        titleForDb = title.replace("'", "''");
                    }
                    values.put(DbHelper.TITLE, titleForDb);

                    String summary = "";
                    String summaryForDb = "";
                    if (geonamesObj.has("summary")) {
                        summary = geonamesObj.getString("summary");
                        summaryForDb = summary.replace("'", "''");
                    }
                    values.put(DbHelper.SUMMARY, summaryForDb);

                    String wikipediaUrl = "";
                    if (geonamesObj.has("wikipediaUrl")) wikipediaUrl = geonamesObj.getString("wikipediaUrl");
                    values.put(DbHelper.WIKIPEDIA_URL, wikipediaUrl);

                    int elevation = 0;
                    if (geonamesObj.has("elevation")) elevation = geonamesObj.getInt("elevation");
                    values.put(DbHelper.ELEVATION, elevation);

                    String lang = "";
                    if (geonamesObj.has("lang")) lang = geonamesObj.getString("lang");
                    values.put(DbHelper.LANG, lang);

                    String imageName = "";

                    boolean noThumbnails = shPref.contains("pref_nothumbnails") && shPref.getBoolean("pref_nothumbnails", false);
                    if(!noThumbnails) {  // wenn Markerbildchen angefragt werden...

                        // Bild-URL aus Artikel-url backen
                        String path = URI.create(wikipediaUrl).getPath();
                        String titleUrlStr = path.substring(path.lastIndexOf('/') + 1);

                        // Titel des Artikels aus dem Schluss der Artikel-Url fischen und diesen in die API-Url setzen
                        String url2 = "http://" + lang +
                                ".wikipedia.org/w/api.php" +
                                "?action=query" +
                                "&prop=pageimages" +
                                "&pithumbsize=" + String.valueOf(THUMBNAIL_IMAGE_DOWNLOAD_SIZE) +
                                "&titles=" + titleUrlStr +
                                "&format=json";

                        URL mURL_2 = new URL(url2);  // um Verwechslungen zu vermeiden, alles mit _2
                        Request mRequest_2 = new Request.Builder().url(mURL_2).build();
                        Response mResponse_2 = okHttpClient.newCall(mRequest_2).execute();
                        String jsonContent_2 = mResponse_2.body().string();
                        JSONObject rootObj_2 = new JSONObject(jsonContent_2);
                        JSONObject queryObj = rootObj_2.getJSONObject("query");
                        JSONObject pagesObj = queryObj.getJSONObject("pages");
                        Iterator<?> jsonKeys = pagesObj.keys();
                        String articleId = (String) jsonKeys.next();
                        JSONObject articleIdObj = pagesObj.getJSONObject(articleId);

                        String thumbUrl = "";
                        if (articleIdObj.has("thumbnail")) {
                            JSONObject thumbnailObj = articleIdObj.getJSONObject("thumbnail");
                            thumbUrl = thumbnailObj.getString("source");
                        }

                        if (!thumbUrl.isEmpty() && thumbUrl.trim().length() != 0) { // wenn eine Markerbildchen-URL im JSON ist...

                            // Fotoname aus der Adresse holen
                            String imagePath = URI.create(thumbUrl).getPath();
                            imageName = imagePath.substring(imagePath.lastIndexOf('/') + 1);
                            imagePath = thumbnailStoragePath + "/" + imageName;

                            // REPLACE in db
                            values.put(DbHelper.THUMBNAIL_IMAGE_NAME, imageName);
                            long rowId = db.getWritableDatabase().replace(DbHelper.TABLE_ARTICLES_NAME, null, values);
                            if(rowId == -1) { // wenn die Zeile schon in der db ist
                                continue; // Schleife oben weitermachen
                            }

                            downloadManager.enqueue(new DownloadManager.Request(Uri.parse(thumbUrl))
                                    .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                                    .setAllowedOverRoaming(true)
                                    .setTitle(title)
                                    .setDescription("WikiMap fetches coolest places...")
                                    .setDestinationUri(Uri.parse("file://" + imagePath)));

                        } else { // wenn KEINE Markerbildchen-URL im JSON ist...

                            // REPLACE in db
                            values.put(DbHelper.THUMBNAIL_IMAGE_NAME, "");
                            long rowId = db.getWritableDatabase().replace(DbHelper.TABLE_ARTICLES_NAME, null, values);
                            if(rowId == -1) { // wenn die Zeile schon in der db ist
                                continue; // Schleife oben weitermachen
                            }
                        }

                    } else { // keine Markerbilchen werden angefragt

                        // REPLACE in db
                        values.put(DbHelper.THUMBNAIL_IMAGE_NAME, "");
                        long rowId = db.getWritableDatabase().replace(DbHelper.TABLE_ARTICLES_NAME, null, values);
                        if(rowId == -1) { // wenn die Zeile schon in der db ist
                            continue; // Schleife oben weitermachen
                        }
                    }

                    mItems.add(articlePosition);
                    wikiArticles.add(new WikiArticle(lat, lng, title, summary, wikipediaUrl, elevation, lang, imageName));
                }
                //</editor-fold>

            } catch (IOException | JSONException e) {
                Log.e(TAG + "_fetchWikiArt", e.getMessage());
                //showMyAwesomeSnackbar("Download is broken. Please try again!", true);
            }

            //Log.e(TAG + "_doInBackg", "wikiArticles.size() = " + wikiArticles.size());
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    boolean downloadInBackground = shPref.contains("pref_downl_dlg") && shPref.getBoolean("pref_downl_dlg", false);
                    if (!downloadInBackground) downloadProgress.dismiss();

                    showWikiArticlesOnMap(wikiArticles, "");
                }
            }, 5000);
        }
    }

    private class fetchWikiBigImageUrlTask extends AsyncTask<WikiArticle, Void, Void> {
        // android.os.AsyncTask<Params, Progress, Result>

        private WikiArticle wikiArticle;
        private String bigImageUrl;

        @Override
        protected void onPreExecute() {

            waitForInfoWindowProgressAlert();
        }

        @Override
        protected Void doInBackground(WikiArticle... arg) {

            wikiArticle = arg[0];

            // Bild-URL aus Artikel-URL backen
            String path = URI.create(wikiArticle.wikipediaURL).getPath();

            // Titel des Artikels aus dem Schluss der Artikel-URL fischen und diesen in die API-URL setzen
            String url = "http://"
                    + wikiArticle.lang +
                    ".wikipedia.org/w/api.php" +
                    "?action=query" +
                    "&prop=pageimages" +
                    "&pithumbsize=" + String.valueOf(BIG_IMAGE_DOWNLOAD_SIZE) +
                    "&titles=" + path.substring(path.lastIndexOf('/') + 1) +
                    "&format=json";

            //Log.e(TAG + "_etchWikiBigIm", url);

            try {

                URL mURL = new URL(url);  // um Verwechslungen zu vermeiden, alles mit _2
                Request mRequest = new Request.Builder().url(mURL).build();
                Response mResponse = okHttpClient.newCall(mRequest).execute();
                String jsonContent = mResponse.body().string();
                JSONObject rootObj = new JSONObject(jsonContent);
                JSONObject queryObj = rootObj.getJSONObject("query");
                JSONObject pagesObj = queryObj.getJSONObject("pages");
                Iterator<?> jsonKeys = pagesObj.keys();
                String articleId = (String) jsonKeys.next();
                JSONObject articleIdObj = pagesObj.getJSONObject(articleId);

                if (articleIdObj.has("thumbnail")) {
                    JSONObject bigImageUrlObj = articleIdObj.getJSONObject("thumbnail");
                    bigImageUrl = bigImageUrlObj.getString("source");
                } else {
                    bigImageUrl = "";
                }

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            waitForInfoWindowProgress.dismiss();

            showMarkerInfoWindow(wikiArticle, bigImageUrl);
        }
    }

    private class fetchWikiListImageUrlTask extends AsyncTask<Void, Void, Void> {
        // android.os.AsyncTask<Params, Progress, Result>

        @Override
        protected Void doInBackground(Void... arg) {

            // alle URLs aus der DB holen, wo keine Adresse des Bildes ist
            Cursor c = db.getWritableDatabase().rawQuery("SELECT " +
                    DbHelper.WIKIPEDIA_URL + ", " +
                    DbHelper.LANG +
                    " FROM " + DbHelper.TABLE_ARTICLES_NAME + " WHERE " + DbHelper.LIST_IMAGE_URL + " ISNULL", null);

            while (c.moveToNext()) {

                String currUrl = c.getString(0);
                String currLang = c.getString(1);
                if(currUrl.isEmpty() || currUrl.trim().length() == 0) continue;
                if(currLang.isEmpty() || currLang.trim().length() == 0) currLang = stateManager.getSystemLanguage();

                // Bild-URL aus Artikel-URL backen
                String path = URI.create(currUrl).getPath();

                // Titel des Artikels aus dem Schluss der Artikel-URL fischen und diesen in die API-URL setzen
                String url = "http://"
                        + currLang +
                        ".wikipedia.org/w/api.php" +
                        "?action=query" +
                        "&prop=pageimages" +
                        "&pithumbsize=" + String.valueOf(LIST_IMAGE_DOWNLOAD_SIZE) +
                        "&titles=" + path.substring(path.lastIndexOf('/') + 1) +
                        "&format=json";

                try {

                    URL mURL = new URL(url);
                    Request mRequest = new Request.Builder().url(mURL).build();
                    Response mResponse = okHttpClient.newCall(mRequest).execute();
                    String jsonContent = mResponse.body().string();
                    JSONObject rootObj = new JSONObject(jsonContent);
                    JSONObject queryObj = rootObj.getJSONObject("query");
                    JSONObject pagesObj = queryObj.getJSONObject("pages");
                    Iterator<?> jsonKeys = pagesObj.keys();
                    String articleId = (String) jsonKeys.next();
                    JSONObject articleIdObj = pagesObj.getJSONObject(articleId);

                    String imageUrl;
                    if (articleIdObj.has("thumbnail")) {
                        JSONObject imageUrlObj = articleIdObj.getJSONObject("thumbnail");
                        imageUrl = imageUrlObj.getString("source");
                    } else {
                        imageUrl = "";
                    }

                    ContentValues values = new ContentValues();
                    values.put(DbHelper.LIST_IMAGE_URL, imageUrl);
                    String whereClause = DbHelper.WIKIPEDIA_URL + " = '" + currUrl + "'";
                    db.getWritableDatabase().update(DbHelper.TABLE_ARTICLES_NAME, values, whereClause, null);

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }

            }
            c.close();

            return null;
        }
    }

    private void showWikiArticlesOnMap(List<WikiArticle> wikiArticles, String inputMessage) {

        int oldNumArticles = numArticles;

        for (WikiArticle wikiArticleItem : wikiArticles) {

            clusterManager.addItem(wikiArticleItem);
            // ... und mitzählen
            numArticles++;
        }

        //Log.e(TAG + "_owWikiArticlesO", String.valueOf(numArticles));

        int diff = Math.abs(oldNumArticles - numArticles);

        String message;
        if(inputMessage.isEmpty()) {
            message = String.valueOf(diff) + " " + getString(R.string.articles_downloaded) + "\n" + numArticles + " " + getString(R.string.articles_total);
        } else {
            message = inputMessage;
        }

        if(downloadedSnackbar) {
            showMyAwesomeSnackbar(message, false);
        } else {
            downloadedSnackbar = true;
        }

        TextView tv = (TextView) getActivity().findViewById(R.id.num_markers);
        tv.setText(String.valueOf(numArticles));

        if(numArticles > 300) haveToCleanupMap = true;

        //Log.e(TAG, "showWikiArticlesOnMap(): haveToCleanupMap = " + haveToCleanupMap);

        clusterManager.cluster();
        isWorking = false;

        if(isOnline()) new fetchWikiListImageUrlTask().execute();
    }

    private void showMarkerInfoWindow(final WikiArticle wikiArticle, String bigImageUrl) {

        //<editor-fold desc="Layout holen und Variablen damit verlinken">
        View v = getActivity().getLayoutInflater().inflate(R.layout.marker_info, null);

        TextView tvTit = (TextView)  v.findViewById(R.id.title);
        TextView  tvSum = (TextView)  v.findViewById(R.id.summary);
        TextView  tvEle = (TextView)  v.findViewById(R.id.elevation);
        TextView  tvDis = (TextView)  v.findViewById(R.id.distance);
        ImageView ivBigImage = (ImageView) v.findViewById(R.id.big_image);
        ivBigImage.setTag(wikiArticle.wikipediaURL);
        ivBigImage.setOnClickListener(this);

        //</editor-fold>

        //<editor-fold desc="Layout aus WikiArticle füllen und Entfernung berechnen">
        String tit = wikiArticle.title;
        tvTit.setText(tit);

        String sum = wikiArticle.summary;
        tvSum.setText(sum);

        if (BuildConfig.DEBUG) {
            //Picasso.with(getApplicationContext()).setIndicatorsEnabled(true);
            Picasso.with(getActivity().getApplicationContext()).setLoggingEnabled(true);
        }

        if(!bigImageUrl.isEmpty() && bigImageUrl.trim().length() != 0) {
            Picasso.with(getActivity().getApplicationContext())
                    .load(bigImageUrl)
                    .resize(bigImageDisplaySize, bigImageDisplaySize)
                    .placeholder(R.drawable.ic_wikipedia)
                    .centerCrop()
                    .into(ivBigImage);
        }

        int ele = wikiArticle.elevation;
        tvEle.setText(getString(R.string.marker_infowindow_elevation) + ": " + String.valueOf(ele) + " m " + getString(R.string.above_sea_level));

        // Entfernung zum Telefon ermitteln
        lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (lastLocation != null) {

            Location phoneLocation = new Location("phone");
            phoneLocation.setLatitude(lastLocation.getLatitude());
            phoneLocation.setLongitude(lastLocation.getLongitude());

            Location articleLocation = new Location("article");
            articleLocation.setLatitude(wikiArticle.getPosition().latitude);
            articleLocation.setLongitude(wikiArticle.getPosition().longitude);

            double temp = (double) articleLocation.distanceTo(phoneLocation);
            DecimalFormat df = new DecimalFormat("0.0");
            String dis = df.format(temp / 1000);

            tvDis.setText(getString(R.string.marker_infowindow_distance) + ": " + dis + " km");
        }
        //</editor-fold>

        //<editor-fold desc="Popup anzeigen">
        new MaterialDialog.Builder(getActivity())
                .customView(v, false)
                .cancelable(true)
                .positiveText(R.string.full_Article) // rechts
                .negativeText(R.string.navigate) // Mitte
                .neutralText(R.string.close) // links
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {

                        openWikiApp(wikiArticle.wikipediaURL);
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {

                        double rlat = wikiArticle.getPosition().latitude;
                        double rlng = wikiArticle.getPosition().longitude;

                        Intent openRouteIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://maps.google.com/maps?daddr=" + rlat + "," + rlng));
                        openRouteIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                        startActivity(openRouteIntent);

                    }
                })
                .show();
        //</editor-fold>
    }

    @Override
    public void onClick(View v) {

        openWikiApp(String.valueOf(v.getTag()));
    }

    private void openWikiApp(String url) {

        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            url = "http://" + url;
        }
        Intent openUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(openUrlIntent);
    }

    public void cleanupMap(boolean withSnackbar) {

        if(isWorking) return;

        if(withSnackbar) showMyAwesomeSnackbar(numArticles + " " + getString(R.string.markers_deleted), false);
        numArticles = 0;
        clusterManager.clearItems();
        mItems.clear();
        googleMap.clear();
        boolean success = true;
        for(File file: mFolder.listFiles()) success = file.delete();
        if(!success) {
            Log.e(TAG + "_cleanupMap", " delete() ging nicht!");
        }
        db.getWritableDatabase().execSQL("DELETE FROM " + DbHelper.TABLE_ARTICLES_NAME);
        TextView tv = (TextView) getActivity().findViewById(R.id.num_markers);
        tv.setText(String.valueOf(numArticles));
    }

    private void downloadProgressAlert() {

        String maxRows = "20";
        if (shPref.contains("pref_maxrows")) {
            maxRows = shPref.getString("pref_maxrows", "20");
        }

        View v = getActivity().getLayoutInflater().inflate(R.layout.download_popup, null);
        TextView  tvMaxNumber = (TextView)  v.findViewById(R.id.articles_downloading_maxnumber);
        tvMaxNumber.setText(getText(R.string.articles_downloading_maxnumber) + ": " + maxRows);

        downloadProgress = new MaterialDialog.Builder(getActivity())
                .customView(v, false)
                .positiveText(getString(R.string.download_in_background))
                .cancelable(false)
                .show();

    }

    private void waitForInfoWindowProgressAlert() {

        waitForInfoWindowProgress = new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.fetching_article))
                .content(getString(R.string.please_wait) + "...")
                .cancelable(true)
                .progress(true, 0)
                .show();
    }
    //</editor-fold>

    //<editor-fold desc="Clusterzeugs">
    @Override
    public boolean onClusterClick(Cluster<WikiArticle> wikiArticleCluster)  {

        String title = wikiArticleCluster.getItems().iterator().next().title;
        showMyAwesomeSnackbar(wikiArticleCluster.getSize() + " " + getString(R.string.articles) + ", " + getString(R.string.including) +
                " " + title + "\n" + getString(R.string.zoom_for_details), false);
        return true;
    }

    @Override
    public boolean onClusterItemClick(WikiArticle wikiArticle)  {

        if(isOnline()) {

            boolean noBigImages = shPref.contains("pref_nobigimages") && shPref.getBoolean("pref_nobigimages", false);

            if (!noBigImages) {
                new fetchWikiBigImageUrlTask().execute(wikiArticle);
            } else {
                showMarkerInfoWindow(wikiArticle, "");
            }

        } else {
            showNoInternetAlert();
        }
        return true;
    }

    public class WikiArticleRenderer extends DefaultClusterRenderer<WikiArticle> {

        private final IconGenerator iconGenerator = new IconGenerator(getActivity().getApplicationContext());
        private float fontSizeFloat = MARKER_TEXT_SIZE;
        private int textColor;

        public WikiArticleRenderer() {

            super(getActivity().getApplicationContext(), googleMap, clusterManager);

            int prefColorCode = -720809;
            if (shPref.contains("pref_marker_color")) {
                prefColorCode = shPref.getInt("pref_marker_color", -720809);
            }
            //Log.e(TAG, String.valueOf(prefColorCode));

            String hexCode = "#F50057";
            textColor = R.color.white;
            switch (prefColorCode) {
                case -720809:   hexCode = "#F50057"; textColor = R.color.white; break;
                case -65360:    hexCode = "#ff00b0"; textColor = R.color.white; break;
                case -16749825: hexCode = "#006aff"; textColor = R.color.white; break;
                case -13500672: hexCode = "#31ff00"; textColor = R.color.black; break;
                case -2304:     hexCode = "#fff700"; textColor = R.color.black; break;

                case -1242880:  hexCode = "#ed0900"; textColor = R.color.white; break;
                case -6815489:  hexCode = "#9800ff"; textColor = R.color.white; break;
                case -16773987: hexCode = "#000c9d"; textColor = R.color.white; break;
                case -16077312: hexCode = "#0aae00"; textColor = R.color.black; break;
                case -20961:    hexCode = "#ffae1f"; textColor = R.color.black; break;

                case -9371648:  hexCode = "#710000"; textColor = R.color.white; break;
                case -10944352: hexCode = "#5900a0"; textColor = R.color.white; break;
                case -16774579: hexCode = "#000a4d"; textColor = R.color.white; break;
                case -16752639: hexCode = "#006001"; textColor = R.color.white; break;
                case -5289216:  hexCode = "#af4b00"; textColor = R.color.white; break;
            }

            iconGenerator.setColor( Color.parseColor( hexCode ) );

            String fontSize = "normal";
            if (shPref.contains("pref_fontsize")) {
                fontSize = shPref.getString("pref_fontsize", "normal");
            }
            switch (fontSize) {
                case "huge":  fontSizeFloat = fontSizeFloat * 1.4f; break;
                case "big":  fontSizeFloat = fontSizeFloat * 1.15f; break;
                case "normal": break;
                case "small": fontSizeFloat = fontSizeFloat * 0.85f; break;
                case "tiny": fontSizeFloat = fontSizeFloat * 0.7f; break;
            }
            //Log.i(TAG, String.valueOf(fontSizeFloat));
        }

        @Override
        protected void onBeforeClusterItemRendered(WikiArticle wikiArticle, MarkerOptions markerOptions) {

            if(wikiArticle.imageName.isEmpty() || wikiArticle.imageName.trim().length() == 0) { // kein Bildchen anzeigen

                // Layout
                View v = getActivity().getLayoutInflater().inflate(R.layout.marker_textappearance, null);
                TextView tv = (TextView) v.findViewById(R.id.text);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeFloat);
                tv.setTextColor(getResources().getColor(textColor));

                // Icon Generator mit Layout füllen
                iconGenerator.setContentView(v);

                // Icon bauen
                Bitmap iconGeneratorBitmap = iconGenerator.makeIcon( wrapText(wikiArticle.title, getWraplength(wikiArticle.title), null, false) );
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconGeneratorBitmap));
                if (iconGeneratorBitmap != null && !iconGeneratorBitmap.isRecycled()) iconGeneratorBitmap.recycle();

            } else { // Bildchen anzeigen

                // Layout
                View v = getActivity().getLayoutInflater().inflate(R.layout.marker_imageappearance, null);
                TextView tv = (TextView) v.findViewById(R.id.marker_text);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeFloat);
                tv.setTextColor(getResources().getColor(textColor));
                ImageView iv = (ImageView) v.findViewById(R.id.marker_image);
                iv.getLayoutParams().height = thumbnailImageDisplaySize;
                iv.getLayoutParams().width = thumbnailImageDisplaySize;

                // Bildpfad
                String imagePath = thumbnailStoragePath + "/" + wikiArticle.imageName;

                // BitmapFactory.Options
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;

                // Bild aus dem Verzeichnis holen
                Bitmap imageBitmap = BitmapFactory.decodeFile(imagePath, options);

                // Textview und Imageview mit Inhalt füllen
                tv.setText( wrapText(wikiArticle.title, getWraplength(wikiArticle.title), null, false) );
                iv.setImageBitmap(imageBitmap);

                // Icon Generator mit Layout füllen
                iconGenerator.setContentView(v);

                // Icon bauen
                Bitmap iconGeneratorBitmap = iconGenerator.makeIcon();
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(iconGeneratorBitmap));
                if (iconGeneratorBitmap != null && !iconGeneratorBitmap.isRecycled()) iconGeneratorBitmap.recycle();
            }
        }

        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // ab n Artikel an einem Fleck -> cluster rendern
            return cluster.getSize() > 2; // n auf einem Fleck werden nicht einzeln angezeigt
        }

        private int getWraplength(String t) {

            if (t.length() <= 12) {
                return 10;
            } else if(t.length() >= 13 && t.length() <= 16) {
                return 12;
            } else if (t.length() >= 17 && t.length() <= 22) {
                return 16;
            } else {
                return 19;
            }
        }

        private String wrapText(String str, int wrapLength, String newLineStr, boolean wrapLongWords) {
            if (str == null) {
                return null;
            }
            if (newLineStr == null) {
                newLineStr = System.getProperty("line.separator");
            }
            if (wrapLength < 1) {
                wrapLength = 1;
            }
            int inputLineLength = str.length();
            int offset = 0;
            StringBuilder wrappedLine = new StringBuilder(inputLineLength + 32);

            while ((inputLineLength - offset) > wrapLength) {
                if (str.charAt(offset) == ' ') {
                    offset++;
                    continue;
                }
                int spaceToWrapAt = str.lastIndexOf(' ', wrapLength + offset);

                if (spaceToWrapAt >= offset) {
                    // normal case
                    wrappedLine.append(str.substring(offset, spaceToWrapAt));
                    wrappedLine.append(newLineStr);
                    offset = spaceToWrapAt + 1;

                } else {
                    // really long word or URL
                    if (wrapLongWords) {
                        // wrap really long word one line at a time
                        wrappedLine.append(str.substring(offset, wrapLength + offset));
                        wrappedLine.append(newLineStr);
                        offset += wrapLength;
                    } else {
                        // do not wrap really long word, just extend beyond limit
                        spaceToWrapAt = str.indexOf(' ', wrapLength + offset);
                        if (spaceToWrapAt >= 0) {
                            wrappedLine.append(str.substring(offset, spaceToWrapAt));
                            wrappedLine.append(newLineStr);
                            offset = spaceToWrapAt + 1;
                        } else {
                            wrappedLine.append(str.substring(offset));
                            offset = inputLineLength;
                        }
                    }
                }
            }

            // Whatever is left in line is short enough to just pass through
            wrappedLine.append(str.substring(offset));

            return wrappedLine.toString();
        }
    }
    //</editor-fold>
}
