package com.example.vitorlasversenyapp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;



import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllUserLocationsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private DatabaseReference usersRef;
    private ValueEventListener usersLocationListener;
    private HashMap<String, Marker> userMarkers = new HashMap<>();
    private HashMap<String, List<Marker>> userMarkers2 = new HashMap<>();
    private Marker currentUserMarker;

    private Button btnAddMarker;
    private Button btnStartLine;
    private LatLng startPoint;
    private LatLng endPoint;
    private Marker startMarker;
    private Marker endMarker;
    private boolean addStartLinePressed;




    private void removeUserLocation() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference currentUserRef = usersRef.child(userId);
        currentUserRef.child("lat").setValue("na");
        currentUserRef.child("lng").setValue("na");
    }

    private void connectMarkersWithLine(LatLng startPoint, LatLng endPoint) {
        if (startPoint != null && endPoint != null) {
            mMap.addPolyline(new PolylineOptions()
                    .add(startPoint)
                    .add(endPoint)
                    .width(5)
                    .color(Color.RED));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_user_locations);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        usersRef = FirebaseDatabase.getInstance("https://vitorlasversenyapp-default-rtdb.europe-west1.firebasedatabase.app/").getReference("Users");
        Log.d("FirebasePath", "Users path: " + usersRef.toString());

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        //refresh every 5s
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        Button btnExit = findViewById(R.id.buttonexit);
        Button btnAddBoja = findViewById(R.id.addboja);
        Button btnStart = findViewById(R.id.start_finish);

        btnExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeUserLocation();


                Intent intent = new Intent(AllUserLocationsActivity.this, UserLocationMainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        btnAddBoja.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AllUserLocationsActivity.this);
                builder.setTitle("Adja meg a bója nevét");

                // Set up the input field
                final EditText input = new EditText(AllUserLocationsActivity.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);


                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String markerName = input.getText().toString();
                        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                            @Override
                            public void onMapClick(LatLng latLng) {
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

                builder.show();
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Reset the start and end points
                startPoint = null;
                endPoint = null;

                addStartLinePressed = true;
                Toast.makeText(AllUserLocationsActivity.this, "Válassza ki a kezdő és végpontját a start vonalnak!", Toast.LENGTH_SHORT).show();
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
            }
        });


        String userRole = getIntent().getStringExtra("userRole");

        if ("Szervező".equals(userRole)) {
            btnStart.setEnabled(true);
            btnAddBoja.setEnabled(true);
        } else {
            btnStart.setEnabled(false);
            btnAddBoja.setEnabled(false);
        }

    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setupLocationUpdates();
            getUsersLocations();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getUsersLocations() {
        usersLocationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("DataChange", "Data change called");
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    CreateUser user = userSnapshot.getValue(CreateUser.class);
                    if (user != null && user.lat != null && user.lng != null && !user.lat.equals("na") && !user.lng.equals("na")) {
                        LatLng userLatLng = new LatLng(Double.parseDouble(user.lat), Double.parseDouble(user.lng));
                        MarkerOptions markerOptions = new MarkerOptions().position(userLatLng).title(user.name);

                        Log.d("asd2", "User: " + user.name + ", LatLng: " + userLatLng.latitude + ", " + userLatLng.longitude);

                        List<Marker> userMarkerList = userMarkers2.get(user.userid);
                        if (userMarkerList == null) {
                            userMarkerList = new ArrayList<>();
                            userMarkers2.put(user.userid, userMarkerList);
                        }
                        Marker newUserMarker = mMap.addMarker(markerOptions);
                        userMarkerList.add(newUserMarker);

                        if (userMarkerList.size() > 10) {
                            Marker oldestMarker = userMarkerList.remove(0);
                            if (oldestMarker != null) {
                                oldestMarker.remove();
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AllUserLocationsActivity.this, "Error fetching user locations: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        usersRef.addValueEventListener(usersLocationListener);
    }

    private void setupLocationUpdates() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                        Log.d("asd", "LatLng: " + latLng.latitude + ", " + latLng.longitude);


                        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        DatabaseReference currentUserRef = usersRef.child(userId);
                        currentUserRef.child("lat").setValue(String.valueOf(location.getLatitude()));
                        currentUserRef.child("lng").setValue(String.valueOf(location.getLongitude()));

                        if (currentUserMarker == null) {
                            currentUserMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("You"));
                        } else {
                            currentUserMarker.setPosition(latLng);
                        }
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupLocationUpdates();
                getUsersLocations();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (usersLocationListener != null) {
            usersRef.removeEventListener(usersLocationListener);
        }
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}