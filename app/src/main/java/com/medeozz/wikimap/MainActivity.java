package com.medeozz.wikimap;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

public class MainActivity extends MaterialNavigationDrawer {

    private static final String TAG = "WIKIMAP";
    private OnMyClickListener onMyClickListener; // eigenes Interface

    public interface OnMyClickListener {
        void onMyBackPressed();
        void onMaptypeNormalClicked();
        void onMaptypeHybridClicked();
        void onMaptypeTerrainClicked();
        void onDeleteMarkersClicked();
    }

    @Override
    public void init(Bundle savedInstanceState) {

        //MyStateManager stateManager = ((WikiMapApplication) getApplicationContext()).getStateManager();

        MyMapFragment mapFragment = new MyMapFragment(); // hier wird keine GoogleMap instanziiert!
        onMyClickListener = mapFragment;

        try {
            // aktuelle Version holen
            PackageInfo packageInfo;
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String currentVersionName = packageInfo.versionName;
            setUserEmail(currentVersionName);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        getToolbar().setVisibility(View.GONE); // original Toolbar nicht anzeigen
        setDrawerHeaderImage(R.drawable.map2);
        setUsername(getString(R.string.app_name));
        setFirstAccountPhoto(getResources().getDrawable(R.drawable.ic_launcher));

        //MaterialSection mapSection = new MaterialSection(this, MaterialSection.ICON_24DP, true, MaterialSection.TARGET_FRAGMENT);
        this.addSection(newSection(getString(R.string.app_name), R.drawable.ic_pin_drop_black_24dp, mapFragment));

        this.addSection(newSection(getString(R.string.articles_list), R.drawable.ic_view_list_black_24dp, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {

                startActivity(new Intent(getApplicationContext(), CardviewActivity.class));
                materialSection.unSelect();
            }
        }));

        this.addSubheader(getString(R.string.section_map));

        this.addSection(newSection(getString(R.string.map_type_street), R.drawable.ic_map_black_24dp, new MaterialSectionListener() {
                    @Override
                    public void onClick(MaterialSection materialSection) {
                        if (onMyClickListener != null) {
                            onMyClickListener.onMaptypeNormalClicked();
                        }
                    }
                }));

        this.addSection(newSection(getString(R.string.map_type_hybrid), R.drawable.ic_satellite_black_24dp, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                if (onMyClickListener != null) {
                    onMyClickListener.onMaptypeHybridClicked();
                }
            }
        }));

        this.addSection(newSection(getString(R.string.map_type_terrain), R.drawable.ic_landscape_black_24dp, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                if (onMyClickListener != null) {
                    onMyClickListener.onMaptypeTerrainClicked();
                }
            }
        }));

        this.addDivisor();

        this.addSection(newSection(getString(R.string.menu_delete_markers), R.drawable.ic_delete_black_24dp, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                if (onMyClickListener != null) {
                    onMyClickListener.onDeleteMarkersClicked();
                }
                materialSection.unSelect();
            }
        }));

        this.addBottomSection(newSection(getString(R.string.prefs_activity_title), R.drawable.ic_settings_black_24dp, new Intent(this, SetPreferenceActivity.class)));

        this.addBottomSection(newSection(getString(R.string.title_activity_tips), R.drawable.ic_help_black_24dp, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {

                startActivity(new Intent(getApplicationContext(), TipsActivity.class));
                materialSection.unSelect();
            }
        }));

        this.addBottomSection(newSection(getString(R.string.title_activity_about), R.drawable.ic_info_black_24dp, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {

                startActivity(new Intent(getApplicationContext(), AboutActivity.class));
                materialSection.unSelect();
            }
        }));
        // this.setBackPattern(MaterialNavigationDrawer.BACKPATTERN_CUSTOM);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        int id = android.os.Process.myPid();
        android.os.Process.killProcess(id);
    }

    @Override
    public void onBackPressed(){
        if(onMyClickListener != null) {
            onMyClickListener.onMyBackPressed();
        }
    }

}