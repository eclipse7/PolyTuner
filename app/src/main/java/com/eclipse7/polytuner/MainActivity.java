package com.eclipse7.polytuner;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.eclipse7.polytuner.core.Audio;

public class MainActivity extends AppCompatActivity {
    private Display display;
    private Tuner tuner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }

        display = findViewById(R.id.display);
        display.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                boolean displayMode = preferences.getBoolean(SettingsActivity.DISPLAY_MODE_SWITCH, false);
                SharedPreferences.Editor editor = preferences.edit();
                boolean b;
                String toastText;
                if (displayMode) {
                    b = false;
                    toastText = "Display Classic";
                }
                else {
                    b = true;
                    toastText = "Display Strobe";
                }

                Toast toast = Toast.makeText(getApplicationContext(),
                        toastText, Toast.LENGTH_SHORT);
                toast.show();

                editor.putBoolean(SettingsActivity.DISPLAY_MODE_SWITCH, b);
                editor.apply();
                getPreferences();
            }
        });

        display.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view)
            {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                boolean polyMode = preferences.getBoolean(SettingsActivity.POLY_MODE_SWITCH, false);
                SharedPreferences.Editor editor = preferences.edit();
                boolean b;
                String toastText;
                if (polyMode) {
                    b = false;
                    toastText = "Poly Off";
                }
                else {
                    b = true;
                    toastText = "Poly On";
                }

                Toast toast = Toast.makeText(getApplicationContext(),
                        toastText, Toast.LENGTH_SHORT);
                toast.show();

                editor.putBoolean(SettingsActivity.POLY_MODE_SWITCH, b);
                editor.apply();
                getPreferences();
                return true;
            }
        });

        tuner = new Tuner(this, display);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferences();
        tuner.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        tuner.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tuner = null;
        display = null;
        System.runFinalization();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_info) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setCancelable(true);

            String version = "";
            try {
                PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
                version = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            View view = getLayoutInflater().inflate(R.layout.about_dialog, null);
            TextView version_view = view.findViewById(R.id.about);
            String s = "Tuner version " + version;
            version_view.setText(s);
            alertDialogBuilder.setView(view);

            alertDialogBuilder.setTitle(R.string.info_title);
            alertDialogBuilder.setPositiveButton("Ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                        }
                    });

            AlertDialog infoDialog = alertDialogBuilder.create();
            infoDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void getPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        Audio audio = tuner.getAudio();
        if (audio != null) {
            boolean displayMode = preferences.getBoolean(SettingsActivity.DISPLAY_MODE_SWITCH, false);
            if (displayMode) {
                tuner.setStrobe(true);
            }
            else {
                tuner.setStrobe(false);
            }

            boolean polyMode = preferences.getBoolean(SettingsActivity.POLY_MODE_SWITCH, false);
            audio.setPolyMode(polyMode);

            int tuneMode = Integer.parseInt(preferences.getString(SettingsActivity.TUNNING_LIST, "0"));
            Resources resources = getResources();
            String[] tuneModeArray = resources.getStringArray(R.array.tuning_entries);
            tuner.setTuneModeString(tuneModeArray[tuneMode]);

            for(int i = 0; i < 6; i++){
                audio.numberPolyNotes[i] = audio.numberPolyNotesArray[tuneMode][i];
            }

            tuner.setReference(440);

            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }
}
