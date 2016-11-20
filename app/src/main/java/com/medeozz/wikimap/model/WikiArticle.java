package com.medeozz.wikimap.model;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class WikiArticle implements ClusterItem {

    private final LatLng mPosition;

    public final String title;
    public final String summary;
    public final String wikipediaURL;
    public final int elevation;
    public final String lang;
    public final String imageName;

    // Konstruktor
    public WikiArticle(double lat, double lng, String title, String summary, String wikipediaUrl,
         int elevation, String lang, String imageName) {

        mPosition = new LatLng(lat, lng);

        this.title = title;
        this.summary = summary;
        this.wikipediaURL = wikipediaUrl;
        this.elevation = elevation;
        this.lang = lang;
        this.imageName = imageName;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }
}
