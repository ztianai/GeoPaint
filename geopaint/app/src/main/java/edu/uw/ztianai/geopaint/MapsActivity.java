package edu.uw.ztianai.geopaint;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private boolean pen = false;
    private List<Polyline> lineOnTheMap = new ArrayList<Polyline>();
    private static final int Request_Code = 0;
    private Polyline currentPolyline;
    private int penColor = -1; //color starts with white

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Creates the Google API Client that allows the location to be tracked
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapFragment.setRetainInstance(true); //retain data even when rotate the screen
        mapFragment.setHasOptionsMenu(true); //show option menu at the action bar

    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.connect();
        super.onStop();
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest request = new LocationRequest();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(10000); //location updates desired interval
        request.setFastestInterval(5000); //fastest interval for location updates

        //Runtime permission check
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Request_Code);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    //Location listener, used when user moves
    @Override
    public void onLocationChanged(Location location) {
        if(pen){ //if the pen is down
            LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude()); //get the current lat/lng

            //initiate polyline with the defined color
            if(currentPolyline == null){
                PolylineOptions lines = new PolylineOptions().color(penColor);
                currentPolyline = mMap.addPolyline(lines);
                lineOnTheMap.add(currentPolyline); //store the drawn lines
            }

            //add points to the current line
            List<LatLng> lineList = currentPolyline.getPoints();
            lineList.add(newPoint);
            currentPolyline.setPoints(lineList);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPoint, 17));
        }
    }

    //Allow user to save data into a geojson file on the phone
    public void saveData(){
        if(isExternalStorageWritable()){
            try{
                if(lineOnTheMap != null && lineOnTheMap.size() > 0){
                    File file = new File(this.getExternalFilesDir(null), "drawing.geojson");
                    FileOutputStream outputStream = new FileOutputStream(file);
                    String pathsave = GeoJsonConverter.convertToGeoJson(lineOnTheMap); //cover polylines into a string geojson format to share
                    outputStream.write(pathsave.getBytes()); //write the string to the file
                    outputStream.close();
                    Toast.makeText(this, "Drawing Saved", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "No drawing to save", Toast.LENGTH_SHORT).show();
                }
            }catch (IOException e){

            }
        }
    }

    //Check whether there is an external storage place to store data
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case Request_Code:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    onConnected(null);
                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);

        //Allow user to share file with others
        MenuItem shareItem = menu.findItem(R.id.action_share);
        ShareActionProvider myShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        Uri fileUri;

        //File location
        File dir = getExternalFilesDir(null);
        File file = new File(dir, "drawing.geojson");
        fileUri = Uri.fromFile(file);

        //Intent to send the file to others through different app
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
        myShareActionProvider.setShareIntent(intent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_pen:
                penSettings();
                return true;
            case R.id.menu_color:
                colorSettings();
                return true;
            case R.id.menu_save:
                saveData();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Change the state of the pen, allow user to either draw or not draw on the map
    public void penSettings(){

        if(pen){ // if the user clicks on the pen icon, then pen is going to lift up -- not draw
            Toast.makeText(this, "Pen is not drawing!", Toast.LENGTH_SHORT).show();
            currentPolyline = null; //reinitialize the current polyline
        }else{ //Put down the pen and start drawing
            Toast.makeText(this, "Pen is drawing!", Toast.LENGTH_SHORT).show();
        }
        pen = !pen; //change the state of the pen
    }

    //Change the color of the pen
    public void colorSettings(){
        currentPolyline = null; //reinitialize the current polyline

        if(penColor == -1){ //this is the default color of the pen, which is color white
            penColor = ContextCompat.getColor(this, R.color.black_de);
        }

        //Use code from https://github.com/kristiyanP/colorpicker.git to handle different colors
        final ColorPicker colorPicker = new ColorPicker(this);
        colorPicker.setFastChooser(new ColorPicker.OnFastChooseColorListener() {
            @Override
            public void setOnFastChooseColorListener(int position, int color) {
                penColor = color;
                colorPicker.dismissDialog();
            }

        }).setColumns(5).show();
    }
}
