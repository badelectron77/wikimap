package com.medeozz.wikimap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.AdapterView.OnItemSelectedListener;

import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.medeozz.wikimap.model.ListWikiArticle;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class CardviewActivity extends AppCompatActivity {

    private DbHelper db;
    private Spinner spinnerNav;
    private String orderBy = DbHelper.TITLE;
    private boolean afterActivityStart = true;
    private RecyclerView recyclerView;
    private int numArticles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.cardview_layout);
        Toolbar toolbar = (Toolbar) findViewById(R.id.cardview_toolbar);
        setSupportActionBar(toolbar);
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().getThemedContext();
        spinnerNav = (Spinner) findViewById(R.id.spinner_nav);

        ClickableFrameLayout clickableFrameLayout1 = (ClickableFrameLayout) findViewById(R.id.scrolltotop);
        clickableFrameLayout1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recyclerView.scrollToPosition(0);
            }
        });

        ClickableFrameLayout clickableFrameLayout2 = (ClickableFrameLayout) findViewById(R.id.scrolltobottom);
        clickableFrameLayout2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recyclerView.scrollToPosition(numArticles - 1);
            }
        });

        db = new DbHelper(this);

        addItemsToSpinner();
        initCardView();
    }

    private void initCardView() {

        recyclerView = (RecyclerView) findViewById(R.id.cardList);
        recyclerView.setHasFixedSize(true);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        CardViewAdapter cardViewAdapter = new CardViewAdapter(this, createList());
        recyclerView.setAdapter(cardViewAdapter);
    }

    public void addItemsToSpinner() {

        ArrayList<String> list = new ArrayList<>();
        list.add(getString(R.string.cardview_alphabetical));

        if(gpsEnabled()) list.add(getString(R.string.cardview_nearest));

        list.add(getString(R.string.cardview_highest));
        list.add(getString(R.string.cardview_recent));

        CustomSpinnerAdapter spinAdapter = new CustomSpinnerAdapter(getApplicationContext(), list);
        spinnerNav.setAdapter(spinAdapter);
        spinnerNav.setOnItemSelectedListener(new OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> adapter, View v, int position, long id) {

                if(gpsEnabled()) {
                    switch (position) {
                        case 0:
                            orderBy = DbHelper.TITLE;
                            initCardView();
                            break;
                        case 1:
                            orderBy = DbHelper.TEMP_DISTANCE;
                            initCardView();
                            break;
                        case 2:
                            orderBy = DbHelper.ELEVATION + " DESC";
                            initCardView();
                            break;
                        case 3:
                            orderBy = "ROWID DESC";
                            initCardView();
                            break;
                    }
                } else {
                    switch (position) {
                        case 0:
                            orderBy = DbHelper.TITLE;
                            initCardView();
                            break;
                        case 1:
                            orderBy = DbHelper.ELEVATION + " DESC";
                            initCardView();
                            break;
                        case 2:
                            orderBy = "ROWID DESC";
                            initCardView();
                            break;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private boolean gpsEnabled() {

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private double getDistance(double lat, double lng) {

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        /*LocationListener locationListener = new LocationListener() {

            public void onLocationChanged(Location location) {}

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };*/

        Location lastLocation = null;

        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {

            Criteria crit = new Criteria();
            crit.setAccuracy(Criteria.ACCURACY_FINE);
            String gpsProvider = locationManager.getBestProvider(crit, false);
            //locationManager.requestLocationUpdates(gpsProvider, 5000, 5, locationListener);
            lastLocation = locationManager.getLastKnownLocation(gpsProvider);
        }

        if (lastLocation != null) {

            Location phoneLocation = new Location("phone");
            phoneLocation.setLatitude(lastLocation.getLatitude());
            phoneLocation.setLongitude(lastLocation.getLongitude());

            Location articleLocation = new Location("article");
            articleLocation.setLatitude(lat);
            articleLocation.setLongitude(lng);

            double distance_in_m = (double) articleLocation.distanceTo(phoneLocation);

            return distance_in_m/1000;

        } else {
            return 0.0;
        }
    }

    private List<ListWikiArticle> createList() {

        List<ListWikiArticle> result = new ArrayList<>();

        // alles aus der DB holen
        Cursor c = db.getWritableDatabase().rawQuery("SELECT " +
                DbHelper.LAT            + ", " +
                DbHelper.LNG            + ", " +
                DbHelper.TITLE          + ", " +
                DbHelper.SUMMARY        + ", " +
                DbHelper.WIKIPEDIA_URL  + ", " +
                DbHelper.ELEVATION      + ", " +
                DbHelper.LANG           + ", " +
                DbHelper.THUMBNAIL_IMAGE_NAME + ", " +
                DbHelper.LIST_IMAGE_URL + ", " +
                DbHelper.TEMP_DISTANCE +
                " FROM " + DbHelper.TABLE_ARTICLES_NAME + " ORDER BY " + orderBy, null);

        while (c.moveToNext()) {

            double lat = c.getDouble(0);
            double lng = c.getDouble(1);

            DecimalFormat df = new DecimalFormat("0.0");

            String distanceTextValue = "";

            if(afterActivityStart && gpsEnabled()) {
                // aktuelle Entfernung in DB eintragen

                double distance = getDistance(lat, lng);

                ContentValues values = new ContentValues();
                values.put(DbHelper.TEMP_DISTANCE, distance);
                String whereClause = DbHelper.WIKIPEDIA_URL + " = '" + c.getString(4) + "'";
                db.getWritableDatabase().update(DbHelper.TABLE_ARTICLES_NAME, values, whereClause, null);

                distanceTextValue = df.format(distance);
            } else {
                if(!Double.isNaN(c.getDouble(9))) distanceTextValue = df.format(c.getDouble(9));
            }

            String distanceText;
            if(gpsEnabled()) {
                distanceText = getString(R.string.marker_infowindow_distance) + ": " + distanceTextValue + " km";
            } else {
                distanceText = "";
            }

            ListWikiArticle listWikiArticle = new ListWikiArticle();

            listWikiArticle.lat = lat;
            listWikiArticle.lng = lng;
            listWikiArticle.title = c.getString(2);
            listWikiArticle.summary = c.getString(3);
            listWikiArticle.wikipediaUrl = c.getString(4);
            listWikiArticle.distance = distanceText;
            listWikiArticle.elevation = getString(R.string.marker_infowindow_elevation) + ": " + c.getString(5) + " m " + getString(R.string.above_sea_level);
            listWikiArticle.thumbImageName = c.getString(7);
            listWikiArticle.imageUrl = c.getString(8);

            result.add(listWikiArticle);
        }

        numArticles = c.getCount();

        TextView tv = (TextView) findViewById(R.id.cardview_num_markers);
        tv.setText(String.valueOf(numArticles) + " ");

        c.close();
        if(afterActivityStart) afterActivityStart = false;

        return result;
    }

    // nötig, damit bei "Zurück" die HauptActivity nicht wieder neu gestartet wird
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}