package com.medeozz.wikimap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.medeozz.wikimap.model.ListWikiArticle;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CardViewAdapter extends RecyclerView.Adapter<CardViewAdapter.WikiArticleViewHolder> {

    private List<ListWikiArticle> wikiList;
    private Context context;

    public CardViewAdapter(Context context, List<ListWikiArticle> wikiList) {
        this.context = context;
        this.wikiList = wikiList;
    }

    @Override
    public int getItemCount() {
        return wikiList.size();
    }

    @Override
    public void onBindViewHolder(WikiArticleViewHolder wikiArticleViewHolder, int i) {

        String thumbnailStoragePath = Environment.getExternalStorageDirectory() + "/WikiMap/thumbs";
        ListWikiArticle listWikiArticle = wikiList.get(i);

        final double rlat = listWikiArticle.lat;
        final double rlng = listWikiArticle.lng;
        final String url = listWikiArticle.wikipediaUrl;

        wikiArticleViewHolder.vTitle.setText(listWikiArticle.title);
        wikiArticleViewHolder.vSummary.setText(listWikiArticle.summary);

        if(!listWikiArticle.distance.isEmpty()) wikiArticleViewHolder.vDistance.setText(listWikiArticle.distance);
        else wikiArticleViewHolder.vDistance.setVisibility(View.GONE);

        wikiArticleViewHolder.vElevation.setText(listWikiArticle.elevation);
        wikiArticleViewHolder.vButtonNavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent openRouteIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://maps.google.com/maps?daddr=" + rlat + "," + rlng));
                openRouteIntent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                v.getContext().startActivity(openRouteIntent);
            }
        });
        wikiArticleViewHolder.vButtonMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWikiApp(url, v);
            }
        });
        wikiArticleViewHolder.vImageUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWikiApp(url, v);
            }
        });

        SharedPreferences shPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean noDisplayImages = shPref.contains("pref_noimages_cardview") && shPref.getBoolean("pref_noimages_cardview", false);

        if(!noDisplayImages && listWikiArticle.imageUrl != null) {
            String _imageUrl = listWikiArticle.imageUrl;
            if (!_imageUrl.isEmpty() && _imageUrl.trim().length() != 0) { // wenn es ein grosses Vorschaubild gibt...

                Picasso.with(context)
                        .load(_imageUrl)
                        .placeholder(Drawable.createFromPath(thumbnailStoragePath + "/" + listWikiArticle.thumbImageName))
                        .into(wikiArticleViewHolder.vImageUrl);
            } else {
                //wikiArticleViewHolder.vImageUrl.getLayoutParams().height = 200;
                wikiArticleViewHolder.vImageUrl.setImageResource(R.drawable.ic_wikipedia);
            }
        } else {
            wikiArticleViewHolder.vImageUrl.setVisibility(View.GONE);
        }
    }

    private void openWikiApp(String url, View v) {

        if (!url.startsWith("https://") && !url.startsWith("http://")) {
            url = "http://" + url;
        }
        Intent openUrlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        v.getContext().startActivity(openUrlIntent);
    }

    @Override
    public WikiArticleViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater
                    .from(viewGroup.getContext())
                    .inflate(R.layout.cardview_row, viewGroup, false);

        return new WikiArticleViewHolder(itemView);
    }

    public static class WikiArticleViewHolder extends RecyclerView.ViewHolder {

        // hier werden die Views verbunden
        protected TextView vTitle;
        protected TextView vSummary;
        protected TextView vDistance;
        protected TextView vElevation;
        protected ImageView vImageUrl;
        protected ClickableFrameLayout vButtonNavi;
        protected ClickableFrameLayout vButtonMore;

        public WikiArticleViewHolder(View v) {
            super(v);

            vTitle =  (TextView) v.findViewById(R.id.cardview_title);
            vSummary = (TextView) v.findViewById(R.id.cardview_summary);
            vDistance = (TextView) v.findViewById(R.id.cardview_distance);
            vElevation = (TextView) v.findViewById(R.id.cardview_elevation);
            vImageUrl = (ImageView) v.findViewById(R.id.cardview_image);
            vButtonNavi = (ClickableFrameLayout) v.findViewById(R.id.cardview_button_navi);
            vButtonMore = (ClickableFrameLayout) v.findViewById(R.id.cardview_button_more);
        }
    }
}
