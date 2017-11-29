package com.example.ashutosh.analyticsdemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import java.util.*;

import com.google.firebase.analytics.FirebaseAnalytics;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String KEY_FAVORITE_FOOD = "favorite_food";

    private static final ImageInfo[] IMAGE_INFOS = {
            new ImageInfo(R.drawable.favorite, R.string.pattern1_title, R.string.pattern1_id),
            new ImageInfo(R.drawable.flash, R.string.pattern2_title, R.string.pattern2_id),
            new ImageInfo(R.drawable.face, R.string.pattern3_title, R.string.pattern3_id),
            new ImageInfo(R.drawable.whitebalance, R.string.pattern4_title, R.string.pattern4_id),
    };

    private ImagePagerAdapter mImagePagerAdapter;

    private ViewPager mViewPager;


    private FirebaseAnalytics mFirebaseAnalytics;


    private String mFavoriteFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        String userFavoriteFood = getUserFavoriteFood();
        if (userFavoriteFood == null) {
            askFavoriteFood();
        } else {
            setUserFavoriteFood(userFavoriteFood);
        }

        mImagePagerAdapter = new ImagePagerAdapter(getSupportFragmentManager(), IMAGE_INFOS);

        // Set up the ViewPager with the pattern adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mImagePagerAdapter);

        // Workaround for AppCompat issue not showing ViewPager titles
        ViewPager.LayoutParams params = (ViewPager.LayoutParams)
                findViewById(R.id.pager_tab_strip).getLayoutParams();
        params.isDecor = true;

        // When the visible image changes, send a screen view hit.
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                recordImageView();
                recordScreenView();
            }
        });

        recordImageView();
    }

    @Override
    public void onResume() {
        super.onResume();
        recordScreenView();
    }


    private void askFavoriteFood() {
        final String[] choices = getResources().getStringArray(R.array.food_items);
        AlertDialog ad = new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.food_dialog_title)
                .setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String food = choices[which];
                        setUserFavoriteFood(food);
                    }
                }).create();

        ad.show();
    }

    private String getUserFavoriteFood() {
        return PreferenceManager.getDefaultSharedPreferences(this)
                .getString(KEY_FAVORITE_FOOD, null);
    }

    private void setUserFavoriteFood(String food) {
        Log.d(TAG, "setFavoriteFood: " + food);
        mFavoriteFood = food;

        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putString(KEY_FAVORITE_FOOD, food)
                .apply();

        mFirebaseAnalytics.setUserProperty("favorite_food", mFavoriteFood);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.menu_share) {
            String name = getCurrentImageTitle();
            String text = "I'd love you to hear about " + name;

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            sendIntent.setType("text/plain");
            startActivity(sendIntent);

            Bundle params = new Bundle();
            params.putString("image_name", name);
            params.putString("full_text", text);
            mFirebaseAnalytics.logEvent("share_image", params);
        }
        return false;
    }

    private String getCurrentImageTitle() {
        int position = mViewPager.getCurrentItem();
        ImageInfo info = IMAGE_INFOS[position];
        return getString(info.title);
    }

    private String getCurrentImageId() {
        int position = mViewPager.getCurrentItem();
        ImageInfo info = IMAGE_INFOS[position];
        return getString(info.id);
    }

    private void recordImageView() {
        String id =  getCurrentImageId();
        String name = getCurrentImageTitle();

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void recordScreenView() {
        // This string must be <= 36 characters long in order for setCurrentScreen to succeed.
        String screenName = getCurrentImageId() + "-" + getCurrentImageTitle();

        // [START set_current_screen]
        mFirebaseAnalytics.setCurrentScreen(this, screenName, null /* class override */);
        // [END set_current_screen]
    }

    public class ImagePagerAdapter extends FragmentPagerAdapter {

        private final ImageInfo[] infos;

        public ImagePagerAdapter(FragmentManager fm, ImageInfo[] infos) {
            super(fm);
            this.infos = infos;
        }

        @Override
        public Fragment getItem(int position) {
            ImageInfo info = infos[position];
            return ImageFragment.newInstance(info.image);
        }

        @Override
        public int getCount() {
            return infos.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position < 0 || position >= infos.length) {
                return null;
            }
            Locale l = Locale.getDefault();
            ImageInfo info = infos[position];
            return getString(info.title).toUpperCase(l);
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        mFirebaseAnalytics.setAnalyticsCollectionEnabled(false);
    }

}
