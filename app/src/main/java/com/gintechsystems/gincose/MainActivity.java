package com.gintechsystems.gincose;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.gintechsystems.gincose.bluetooth.BluetoothManager;

/**
 * Created by joeginley on 3/13/16.
 */
public class MainActivity extends AppCompatActivity {
    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GINcoseWrapper.getSharedInstance().currentAct = this;

        mDrawerLayout = (DrawerLayout)findViewById(R.id.activity_main);
        mDrawerList = (ListView)findViewById(R.id.navList);

        final RelativeLayout drawerContainerLayout = (RelativeLayout)findViewById(R.id.drawer_container_layout);
        final EditText transmitterIdBox = (EditText)findViewById(R.id.transmitter_id_box);

        // Removes annoying may produce NullPointerException.
        assert getSupportActionBar() != null;
        assert drawerContainerLayout != null;
        assert transmitterIdBox != null;

        setupDrawer();
        addDrawerItems();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // Enable all caps and a max length of a transmitter id, 6.
        transmitterIdBox.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(6)});
        transmitterIdBox.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    // Write transmitter id to preferences and begin scanning for the bt devices.
                    GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId = transmitterIdBox.getText().toString();
                    GINcoseWrapper.getSharedInstance().saveTransmitterId(MainActivity.this, transmitterIdBox.getText().toString());

                    Log.i("Transmitter", String.format("The TransmitterId has been updated to %s", GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId));

                    startBTManager();
                }
                return false;
            }
        });

        // Check if a transmitterId already exists.
        String storedTransmitterId = GINcoseWrapper.getSharedInstance().getStoredTransmitterId(MainActivity.this);
        if (storedTransmitterId != null) {
            transmitterIdBox.setText(storedTransmitterId);
            GINcoseWrapper.getSharedInstance().defaultTransmitter = new Transmitter(storedTransmitterId);
        }
        else {
            GINcoseWrapper.getSharedInstance().defaultTransmitter = new Transmitter();
        }

        startBTManager();

        drawerContainerLayout.setFocusableInTouchMode(true);
        drawerContainerLayout.requestFocus();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_menu, menu);

        MenuItem miSessionStart = menu.findItem(R.id.action_start_sensor);
        MenuItem miSessionStop = menu.findItem(R.id.action_stop_sensor);

        boolean sessionExists = GINcoseWrapper.getSharedInstance().getSensorSession(MainActivity.this);
        if (sessionExists) {
            miSessionStart.setVisible(false);
            miSessionStop.setVisible(true);
        }
        else {
            miSessionStop.setVisible(false);
            miSessionStart.setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_calibrate) {

        }
        else if (id == R.id.action_start_sensor) {
            GINcoseWrapper.getSharedInstance().defaultTransmitter.startSession();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    invalidateOptionsMenu();
                }
            }, 500);
        }
        else if (id == R.id.action_stop_sensor) {
            GINcoseWrapper.getSharedInstance().defaultTransmitter.stopSession();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    invalidateOptionsMenu();
                }
            }, 500);
        }

        return mDrawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // BT Request
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                GINcoseWrapper.getSharedInstance().btManager.setupBluetooth();
            }
            else {
                GINcoseWrapper.getSharedInstance().showPermissionToast(this, "Please allow bluetooth services in order to access bluetooth devices.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                       startBTManager();
                    }
                    else {
                        GINcoseWrapper.getSharedInstance().showPermissionToast(this, "Please allow location services in order to access bluetooth devices.");
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Display the unbond message when our user returns to our app if forgetting the device.
        if (GINcoseWrapper.getSharedInstance().showUnbondedMessage) {
            GINcoseWrapper.getSharedInstance().showUnbondedMessage = false;

            GINcoseWrapper.getSharedInstance().showPermissionToast(GINcoseWrapper.getSharedInstance().currentAct, GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterName + " has been unpaired.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        GINcoseWrapper.getSharedInstance().doStopBTManagerOnExit();
    }

    private void startBTManager() {
        if (GINcoseWrapper.getSharedInstance().defaultTransmitter.transmitterId != null) {
            if (GINcoseWrapper.getSharedInstance().btManager == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!GINcoseWrapper.getSharedInstance().locationPermission()) {
                        requestPermissions(new String[]{ Manifest.permission.ACCESS_FINE_LOCATION }, 0);
                    }
                    else {
                        GINcoseWrapper.getSharedInstance().btManager = new BluetoothManager();
                    }
                }
                else {
                    GINcoseWrapper.getSharedInstance().btManager = new BluetoothManager();
                }
            }
        }
    }

    private void setupDrawer() {
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                invalidateOptionsMenu();
            }

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                invalidateOptionsMenu();
            }
        };

        mDrawerToggle.setDrawerIndicatorEnabled(true);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    private void addDrawerItems() {
        String[] navArray = { "Alerts", "Settings", "Share", "Help" };
        mAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, navArray);
        mDrawerList.setAdapter(mAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });
    }
}
