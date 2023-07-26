package com.example.vitorlasversenyapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.se.omapi.SEService;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.navigation.NavigationView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import static java.lang.Math.toRadians;

import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.example.vitorlasversenyapp.databinding.ActivityUserLocationMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

public class UserLocationMainActivity extends AppCompatActivity implements
        OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    FirebaseAuth auth;
    GoogleApiClient client;
    LocationRequest request;
    LatLng latLng;
    FirebaseUser user;

    GoogleMap mMap;


    DatabaseReference databaseReference;
    String current_user_name;
    String current_user_email;
    String current_user_imageUrl;
    TextView t1_currentName,t2_currentEmail;
    ImageView i1;

    private boolean isMarkerPassedFromRight(LatLng previousLocation, LatLng currentLocation, LatLng markerLocation) {
        double prevAngle = calculateBearing(previousLocation, markerLocation);
        double currAngle = calculateBearing(currentLocation, markerLocation);
        return Math.abs(currAngle - prevAngle) > 90;
    }

    private double calculateBearing(LatLng point1, LatLng point2) {
        double lat1 = toRadians(point1.latitude);
        double lat2 = toRadians(point2.latitude);
        double deltaLong = toRadians(point2.longitude - point1.longitude);

        double y = Math.sin(deltaLong) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLong);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }


    private LatLng prevLocation;
    private final LatLng referenceDirection = new LatLng(0, 1); // North direction

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityUserLocationMainBinding binding;
    private Button btnAddMarker;
    private Polyline line;
    private Button btnAddStartLine;
    boolean addStartLinePressed = false;


    private Polyline polyline;
    private Marker startMarker, endMarker;
    private LatLng startPoint, endPoint;

    private LatLng previousLocation;
    private LatLng createdMarkerLatLng;

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;



    private int pointLineSide(LatLng lineStart, LatLng lineEnd, LatLng point) {
        double result = (point.longitude - lineStart.longitude) * (lineEnd.latitude - lineStart.latitude) - (point.latitude - lineStart.latitude) * (lineEnd.longitude - lineStart.longitude);
        return (int) Math.signum(result);
    }


    private void connectMarkersWithLine(LatLng start, LatLng end) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(start, end)
                .width(5)
                .color(Color.RED);
        mMap.addPolyline(polylineOptions);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        binding = ActivityUserLocationMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        auth = FirebaseAuth.getInstance();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int id = item.getItemId();


                switch (id) {
                    case R.id.nav_item1:
                        AlertDialog.Builder builder2 = new AlertDialog.Builder(UserLocationMainActivity.this);
                        builder2.setTitle("Válassza ki a szerepét");

                        String[] roles = {"Szervező", "Résztvevő"};
                        builder2.setItems(roles, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Intent intent = new Intent(UserLocationMainActivity.this, AllUserLocationsActivity.class);
                                intent.putExtra("userRole", roles[which]);
                                startActivity(intent);
                            }
                        });

                        builder2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });


                        builder2.show();
                        break;
                    case R.id.nav_item2:
                        AlertDialog.Builder builder = new AlertDialog.Builder(UserLocationMainActivity.this);
                        builder.setTitle("Adja meg a bója nevét");


                        final EditText input = new EditText(UserLocationMainActivity.this);
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        builder.setView(input);


                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                String markerName = input.getText().toString();
                                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                                    @Override
                                    public void onMapClick(LatLng latLng) {

                                        createdMarkerLatLng = latLng;
                                        MarkerOptions markerOptions = new MarkerOptions()
                                                .position(latLng)
                                                .title(markerName);
                                        mMap.addMarker(markerOptions);


                                        mMap.setOnMapClickListener(null);
                                    }
                                });
                            }
                        });



                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        // Show the prompt
                        builder.show();
                        break;
                    case R.id.nav_item3:

                        startPoint = null;
                        endPoint = null;

                        addStartLinePressed = true;
                        Toast.makeText(UserLocationMainActivity.this, "Válassza ki a kezdő és végpontját a start vonalnak!", Toast.LENGTH_SHORT).show();
                        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                            @Override
                            public void onMapClick(LatLng point) {
                                if (addStartLinePressed) {
                                    if (startPoint == null) {
                                        startPoint = point;
                                        startMarker = mMap.addMarker(new MarkerOptions().position(startPoint).title("Start"));
                                    } else if (endPoint == null) {
                                        endPoint = point;
                                        endMarker = mMap.addMarker(new MarkerOptions().position(endPoint).title("Vég"));
                                        connectMarkersWithLine(startPoint, endPoint);
                                        addStartLinePressed = false;
                                    }
                                }
                            }
                        });
                        break;
                    case R.id.nav_item4:
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(latLng)
                                .title("Jelenlegi lokációm");


                        mMap.addMarker(markerOptions);


                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, "A jelenlegi helyem : https://www.google.com/maps/place/"
                                + latLng.latitude + "," + latLng.longitude);
                        startActivity(Intent.createChooser(shareIntent, "Share using: "));
                        break;
                    case R.id.nav_item5:
                        auth.signOut();
                        Intent myIntent = new Intent(UserLocationMainActivity.this, MainActivity.class);
                        startActivity(myIntent);
                        finish();

                        break;
                    default:
                        break;
                }

                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }
        });



        user = auth.getCurrentUser();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map1);
        mapFragment.getMapAsync(this);

        View header = navigationView.getHeaderView(0);
        t1_currentName = header.findViewById(R.id.title_text);
        databaseReference = FirebaseDatabase.getInstance().getReference().child("Users");

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot)
            {
                current_user_name = snapshot.child(user.getUid()).child("name").getValue(String.class);
                current_user_email = snapshot.child(user.getUid()).child("email").getValue(String.class);
                current_user_imageUrl = snapshot.child(user.getUid()).child("imageUrl").getValue(String.class);


                t1_currentName.setText(current_user_name);
                t2_currentEmail.setText(current_user_email);

                Picasso.get().load(current_user_imageUrl).into(i1);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error)
            {

            }
        });

        // Get the header view at the 0th index
        View headerView = navigationView.getHeaderView(0);
        // Get the ImageView from the header view
        ImageView navHeaderImageView = headerView.findViewById(R.id.imageView_1); // replace with your ImageView's ID

        // Now load the image
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            StorageReference storageRef = FirebaseStorage.getInstance().getReference();
            StorageReference userImageRef = storageRef.child("User_Images/" + uid + ".jpg");

            userImageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    // Load the image using Picasso
                    Picasso.get()
                            .load(uri)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_background)
                            .into(navHeaderImageView);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle any errors
                }
            });

        }

    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        client.connect();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user_location_main, menu);
        return true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        request = new LocationRequest().create();
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        request.setInterval(1000);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(client, request, this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {

    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }

    @Override
    public void onLocationChanged(@NonNull Location location) {

        if (location == null) {
            Toast.makeText(getApplicationContext(), "Could not get location", Toast.LENGTH_SHORT).show();
        } else {
            latLng = new LatLng(location.getLatitude(), location.getLongitude());

            MarkerOptions options = new MarkerOptions();
            options.position(latLng);
            options.title("Current Location");

            mMap.addMarker(options);
            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());

            if (startPoint != null && endPoint != null) {

                if (previousLocation != null) {
                    int sidePrevious = pointLineSide(startPoint, endPoint, previousLocation);
                    int sideCurrent = pointLineSide(startPoint, endPoint, currentLocation);

                    if (sidePrevious != sideCurrent && sidePrevious != 0 && sideCurrent != 0) {
                        Toast.makeText(this, "User crossed the line!", Toast.LENGTH_SHORT).show();
                    }
                }

                previousLocation = currentLocation;
            }

            // Check if the user passed the created marker from the right side
            if (createdMarkerLatLng != null && previousLocation != null && isMarkerPassedFromRight(previousLocation, currentLocation, createdMarkerLatLng)) {
                Toast.makeText(this, "Passed the marker from the right side!", Toast.LENGTH_SHORT).show();
            }

            previousLocation = currentLocation;
        }





    }
}