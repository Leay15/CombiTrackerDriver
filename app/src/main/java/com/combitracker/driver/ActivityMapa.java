package com.combitracker.driver;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ActivityMapa extends MainActivity
        implements NavigationView.OnNavigationItemSelectedListener,OnMapReadyCallback{

    String Usuario,Contraseña,RutaAsignada,RutaPerteneciente,Key;

    private double latAct=0.0,lonAct=0.0,latPas=0.0,lonPas=0.0;


    private Marker markCombi;
    private Bitmap bmpN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Usuario=getIntent().getExtras().getString("Usuario");
        Contraseña=getIntent().getExtras().getString("Contraseña");
        RutaPerteneciente=getIntent().getExtras().getString("RutaPerteneciente");
        Key=getIntent().getExtras().getString("Key");

        databaseReference=firebaseDatabase.getReference("Rutas").child(RutaPerteneciente);
        bmpN= BitmapFactory.decodeResource(getResources(),R.drawable.bus);
        bmpN=Bitmap.createScaledBitmap(bmpN, bmpN.getWidth()/20,bmpN.getHeight()/20, false);

        iniciarHilo();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);
    }

    private void iniciarHilo() {
        Thread tiempo = new Thread(){
            public void run(){
                try{
                    while(true){
                        //Ejecutar el handler
                        handler.post(proceso);

                        Thread.sleep(3000);



                    }
                }catch(Exception e){

                }

            }
        };
        tiempo.start();
    }

    Handler handler= new Handler();
    Runnable proceso=new Runnable() {
        @Override
        public void run() {
            try{

                if(latAct!=latPas&&lonAct!=lonPas){

                    databaseReference.child("Combi").child(Key).child("Lat").setValue(latAct+"");
                    databaseReference.child("Combi").child(Key).child("Lon").setValue(lonAct+"");

                    Toast.makeText(ActivityMapa.this, "CAMBIO", Toast.LENGTH_SHORT).show();
                }

                latPas=latAct;
                lonPas=lonAct;

            }catch (Exception f){
                Toast.makeText(ActivityMapa.this, "Error en el handler/ "+f.getMessage(), Toast.LENGTH_SHORT).show();

            }

        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_mapa, menu);
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
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private GoogleMap googleMap;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap=googleMap;

        //Crear escuchador de combis
        seguirCombis();

        //Mostrar Ubicacion actual al abrir la app
        miUbicacion();
    }

    private void seguirCombis() {
        /*databaseReference.child("Combis").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snap, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot snap, String s) {


            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        */
    }

    private void miUbicacion(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        LocationManager locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        actualizarUbicacion(location);
        locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,200,10,location_listener);




    }

    private LocationListener location_listener= new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            actualizarUbicacion(location);

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private void actualizarUbicacion(Location location) {
        if (location != null) {

            latAct = location.getLatitude();
            lonAct = location.getLongitude();
            agregarMarcador(latAct, lonAct,"YO", BitmapDescriptorFactory.fromBitmap(bmpN));

        }

    }

    private void agregarMarcador(double lat, double lon,String title,BitmapDescriptor icono) {
        LatLng coordenadas = new LatLng(lat, lon);
        CameraUpdate mPosition = CameraUpdateFactory.newLatLngZoom(coordenadas, 40);
        if (markCombi!= null) markCombi.remove();
        markCombi = googleMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title(title)
                .icon(icono));
        googleMap.animateCamera(mPosition);


    }

}
