// MainActivity.java
// Author: Adeline Harcourt based on skeleton code from Professor Justin Wolford in CS 496.
// Description: The main activity file for a simple app that utilizes SQLite and Google location
//          services to allow the user to create a database entry.

package com.example.adeline.sqliteandlocationapp;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;
import android.util.Log;
import android.content.Context;
import android.content.ContentValues;
import android.location.Location;
import android.provider.BaseColumns;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.widget.EditText;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    LocationTableHelper mLocationTable;                         // SQLite table instance
    Button mSQLSubmitButton;                                    // Submit button for user text
    Cursor mSQLCursor;                                          // Cursor to navigate query results
    SimpleCursorAdapter mSQLCursorAdapter;                      // Cursor Adapter to view query results
    SQLiteDatabase mSQLDB;                                      // SQLite database
    private GoogleApiClient mGoogleApiClient;                   // Google API client
    private LocationRequest mLocationRequest;                   // Location request variable
    private String mLatitude = null;                            // String to hold current latitude
    private String mLongitude = null;                           // String to hold current longitude
    private Location mLastLocation;                             // Last known location
    private LocationListener mLocationListener;                 // Location listener variable
    private static final String TAG = "SQLActivity";            // TAG for debugging and logging
    private static final int FINE_LOCATION_PERM_RESULT = 5;     // Integer to identify permission result type

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create Google API Client and add Location Services API
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Create and complete the Location Request
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(6000);
        mLocationRequest.setFastestInterval(6000);
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // If location is known, update lat and long text fields
                if (location != null) {
                    mLatitude = String.valueOf(location.getLatitude());
                    mLongitude = String.valueOf(location.getLongitude());
                // If location is not known, set lat and long fields to null
                } else {
                    mLatitude = null;
                    mLongitude = null;
                }
            }
        };

        // Create database table to hold lat and longs
        mLocationTable = new LocationTableHelper(this);
        mSQLDB = mLocationTable.getWritableDatabase();

        // Set on click listener for submit button to get user input and add it to database, along
        // with most recent lat and long values.
        mSQLSubmitButton = (Button) findViewById(R.id.main_button1);
        mSQLSubmitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSQLDB != null) {
                    ContentValues vals = new ContentValues();
                    vals.put(DBContract.LocationTable.COLUMN_NAME_USER_STRING, ((EditText) findViewById(R.id.main_editText1)).getText().toString());
                    vals.put(DBContract.LocationTable.COLUMN_NAME_USER_LAT, mLatitude);
                    vals.put(DBContract.LocationTable.COLUMN_NAME_USER_LONG, mLongitude);
                    mSQLDB.insert(DBContract.LocationTable.TABLE_NAME, null, vals);
                    populateTable();
                } else {
                    Log.d(TAG, "Unable to write to database.");
                }
            }
        });

        // Populate ListView
        populateTable();
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    // Check if permission is granted to access fine location. If not, request permission.
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERM_RESULT);
        }
        updateLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Dialog errDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, connectionResult.getErrorCode(), 0);
    }

    // If permission is granted to access fine location, update the location.
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == FINE_LOCATION_PERM_RESULT) {
            if (grantResults.length > 0) {
                updateLocation();
            }
        }
    }


    // Update the location of the device. If permission is not granted, set the lat and long to that of Oregon State University's
    // main campus.
    private void updateLocation() {
        // Set to Oregon State's lat and long
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mLatitude = "44.5";
            mLongitude = "-123.2";
            return;
        }

        // If location permission is granted, get the last location and update the lat and long text fields
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,mLocationRequest,mLocationListener);

        if (mLastLocation != null) {
            mLatitude = String.valueOf(mLastLocation.getLatitude());
            mLongitude = String.valueOf(mLastLocation.getLongitude());
        } else {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, mLocationListener);
        }
    }

    // Populate the listView used to display the SQLite database entries.
    private void populateTable() {
        if (mSQLDB != null) {
            try {
                if (mSQLCursorAdapter != null && mSQLCursorAdapter.getCursor() != null) {
                    if (!mSQLCursorAdapter.getCursor().isClosed()) {
                        mSQLCursorAdapter.getCursor().close();
                    }
                }
                // Submit query to get all database records and assign the results to a cursor
                mSQLCursor = mSQLDB.query(DBContract.LocationTable.TABLE_NAME,
                        new String[]{DBContract.LocationTable._ID, DBContract.LocationTable.COLUMN_NAME_USER_STRING,
                                DBContract.LocationTable.COLUMN_NAME_USER_LAT, DBContract.LocationTable.COLUMN_NAME_USER_LONG}, null, null, null, null, null);
                // Find listView from xml layout
                ListView LocationTableListView = (ListView) findViewById(R.id.main_listview1);
                // Create adapter to populate listView
                mSQLCursorAdapter = new SimpleCursorAdapter(this,
                        R.layout.listview_row,
                        mSQLCursor,
                        new String[]{DBContract.LocationTable.COLUMN_NAME_USER_STRING, DBContract.LocationTable.COLUMN_NAME_USER_LAT, DBContract.LocationTable.COLUMN_NAME_USER_LONG},
                        new int[]{R.id.listview1_SQLUserString, R.id.listview1_SQLUserLat, R.id.listview1_SQLUserLong},
                        0);
                // Set the adapter
                LocationTableListView.setAdapter(mSQLCursorAdapter);
            } catch (Exception e) {
                Log.d(TAG, "Error loading data from database");
            }
        }
    }
}

// Class to specify SQL command-related Strings to avoid misspellings.
final class DBContract {
    private DBContract(){};

    public final class LocationTable implements BaseColumns {
        public static final String DB_NAME = "SQLite_db";
        public static final String TABLE_NAME = "locations";
        public static final String COLUMN_NAME_USER_STRING = "user_string";
        public static final String COLUMN_NAME_USER_LAT = "user_lat";
        public static final String COLUMN_NAME_USER_LONG = "user_long";
        public static final int DB_VERSION = 7;


        public static final String SQL_CREATE_LOCATIONS_TABLE = "CREATE TABLE " +
                LocationTable.TABLE_NAME + "(" + LocationTable._ID + " INTEGER PRIMARY KEY NOT NULL," +
                LocationTable.COLUMN_NAME_USER_STRING + " VARCHAR(255)," +
                LocationTable.COLUMN_NAME_USER_LAT + " REAL," +
                LocationTable.COLUMN_NAME_USER_LONG + " REAL);";

        public  static final String SQL_DROP_LOCATIONS_TABLE = "DROP TABLE IF EXISTS " + LocationTable.TABLE_NAME;
    }
}

// Class to extend the SQLiteOpenHelper class. Handles creating and updating the table.
class LocationTableHelper extends SQLiteOpenHelper {

    public LocationTableHelper(Context context) {
        super(context, DBContract.LocationTable.DB_NAME, null, DBContract.LocationTable.DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBContract.LocationTable.SQL_CREATE_LOCATIONS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DBContract.LocationTable.SQL_DROP_LOCATIONS_TABLE);
        onCreate(db);
    }
}
