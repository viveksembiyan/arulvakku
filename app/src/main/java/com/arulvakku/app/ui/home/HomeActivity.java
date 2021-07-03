package com.arulvakku.app.ui.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.arulvakku.R;
import com.arulvakku.app.MyApplication;
import com.arulvakku.app.network.WebServiceHandler;
import com.arulvakku.app.ui.NotificationActivity;
import com.arulvakku.app.ui.prayer_request.PrayerRequestActivity;
import com.arulvakku.app.ui.rosary.RosaryActivity;
import com.arulvakku.app.utils.Constants;
import com.arulvakku.app.utils.ShareLink;
import com.arulvakku.app.utils.Utils;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class HomeActivity extends AppCompatActivity implements View.OnClickListener {

    private CardView rosaryCardView;
    private TextView txtTitle;
    private AudioManager am;
    private BroadcastReceiver receiver;
    /**
     * This variable used to avoid multiple call on receive
     * silent -0
     * vibrate - 1
     * normal - 2
     */
    View view;
    private int mediaMode = 0;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mSharedPreferences = getSharedPreferences(getResources().getString(R.string.app_name), 0);
        inflateXML();
        getCurrentDay();
        // change music stream volume while activity is running
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        silentStatus();
    }

    private void inflateXML() {
        view = findViewById(R.id.parent);
        txtTitle = findViewById(R.id.textView);
        rosaryCardView = findViewById(R.id.cardView2);
        rosaryCardView.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        boolean isExistingFcm = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false);

        if (!isExistingFcm) {
            new PushFCMTokenAsync().execute();
        }

        Intent intent = this.getIntent();
        if (intent != null && intent.hasExtra("type")) {
            Intent intent1 = null;
            if (intent.getStringExtra("type").equalsIgnoreCase("Notifications")) {
                intent1 = new Intent(this, NotificationActivity.class);
            } else if (intent.getStringExtra("type").equalsIgnoreCase("Prayer Request")) {
                intent1 = new Intent(this, PrayerRequestActivity.class);
            }
            if (intent1 != null) {
                startActivity(intent1);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_share) {
            ShareLink shareObject = new ShareLink(this, "https://play.google.com/store/apps/details?id=com.arulvakku");
            shareObject.shareLink();
            return true;
        } else if (id == R.id.action_notification) {
            Intent intent = new Intent(this, NotificationActivity.class);
            startActivity(intent);

        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.sBus.register(this);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int a = am.getRingerMode();
                if (mediaMode != a)
                    silentStatus();
            }
        };
        IntentFilter filter = new IntentFilter(
                AudioManager.RINGER_MODE_CHANGED_ACTION);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.sBus.unregister(this);
        // Unregister receiver
        unregisterReceiver(receiver);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cardView2:
                Intent intent = new Intent(this, RosaryActivity.class);
                if (txtTitle.getText().toString().equals("மகிழ்ச்சி நிறை மறை உண்மைகள்")) {
                    intent.putExtra("day", 0);
                } else if (txtTitle.getText().toString().equals("துயர் மறை உண்மைகள்")) {
                    intent.putExtra("day", 1);
                } else if (txtTitle.getText().toString().equals("மகிமை நிறை மறை உண்மைகள்")) {
                    intent.putExtra("day", 2);
                } else if (txtTitle.getText().toString().equals("ஒளி நிறை மறை உண்மைகள்")) {
                    intent.putExtra("day", 3);
                }
                startActivity(intent);
                break;
            default:
                break;
        }
    }


    public void getCurrentDay() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.SUNDAY: //மகிமை நிறை மறை உண்மைகள்:
                txtTitle.setText("மகிமை நிறை மறை உண்மைகள்");
                break;
            case Calendar.MONDAY: //மகிழ்ச்சி நிறை மறை உண்மைகள்:
                txtTitle.setText("மகிழ்ச்சி நிறை மறை உண்மைகள்");
                break;
            case Calendar.TUESDAY: //துயர் மறை உண்மைகள்:
                txtTitle.setText("துயர் மறை உண்மைகள்");
                break;
            case Calendar.WEDNESDAY: //மகிமை நிறை மறை உண்மைகள்:
                txtTitle.setText("மகிமை நிறை மறை உண்மைகள்");
                break;
            case Calendar.THURSDAY: //ஒளி நிறை மறை உண்மைகள்
                txtTitle.setText("ஒளி நிறை மறை உண்மைகள்");
                break;
            case Calendar.FRIDAY: //துயர் மறை உண்மைகள்:
                txtTitle.setText("துயர் மறை உண்மைகள்");
                break;
            case Calendar.SATURDAY: //மகிழ்ச்சி நிறை மறை உண்மைகள்:
                txtTitle.setText("மகிழ்ச்சி நிறை மறை உண்மைகள்");
                break;
        }
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        startActivity(intent);
    }


    // Get mobile ringer mode status
    private void silentStatus() {
        mediaMode = am.getRingerMode();
        switch (am.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                Log.i("MyApp", "Silent mode");
//                textView.setText("Silent mode");
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                Log.i("MyApp", "Vibrate mode");
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                Log.i("MyApp", "Normal mode");
                notifySilentMode();
                break;
        }
    }

    // Show notification to notify user to put mobile on silent mode
    private void notifySilentMode() {
        Snackbar snackbar = Snackbar
                .make(view, "ஆலயத்தினுள்ளே கைபேசியை அமைதிப்படுத்துக!", Snackbar.LENGTH_LONG)
                .setAction("SETTINGS", view -> openSettings());
        snackbar.show();
    }


    private JSONObject initRequest() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("FirebaseKey", mSharedPreferences.getString(Constants.FCM_TOKEN, ""));
            jsonObject.put("DeviceId", Utils.getAndroidDeviceID(this));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public class PushFCMTokenAsync extends AsyncTask<Void, Void, String> {

        public PushFCMTokenAsync() {

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            WebServiceHandler webService = WebServiceHandler.getInstance(HomeActivity.this);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), initRequest().toString());
            String jsonString = webService.postWebServiceCall(Constants.UPDATE_FIREBASE_KEY, requestBody);
            return jsonString;
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            Log.d(HomeActivity.class.getSimpleName(), response.toString());
            if (response != null && response.length() > 0 && Utils.isValidJSON(response)) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (jsonObject != null && jsonObject.optBoolean("IsTransactionDone")) {
                        SharedPreferences.Editor editor = mSharedPreferences.edit();
                        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true);
                        editor.commit();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}