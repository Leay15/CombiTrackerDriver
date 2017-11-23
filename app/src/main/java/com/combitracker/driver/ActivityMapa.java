package com.combitracker.driver;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class ActivityMapa extends MainActivity
        implements OnMapReadyCallback{

    String Usuario,Contraseña,RutaAsignada,RutaPerteneciente,Key,ColorRecibido;

    private double latAct=0.0,lonAct=0.0,latPas=0.0,lonPas=0.0;
    ArrayList<LatLng> coordenadasRuta = new ArrayList<>();

    private Marker markCombi;
    private Marker markAux=null;
    private Bitmap bmpN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Usuario=getIntent().getExtras().getString("Usuario");
        Contraseña=getIntent().getExtras().getString("Contraseña");
        RutaPerteneciente=getIntent().getExtras().getString("RutaPerteneciente");
        Key=getIntent().getExtras().getString("Key");
        RutaAsignada=getIntent().getExtras().getString("RutaAsignada");
        ColorRecibido=getIntent().getExtras().getString("Color");

        databaseReference=firebaseDatabase.getReference("Rutas").child(RutaPerteneciente);

        DatabaseReference subrutas = databaseReference.child("Subrutas");
        subrutas.keepSynced(true);
        subrutas.removeEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                leerRuta();
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                leerRuta();
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
        bmpN= BitmapFactory.decodeResource(getResources(),R.drawable.bus);
        bmpN=Bitmap.createScaledBitmap(bmpN, bmpN.getWidth()/20,bmpN.getHeight()/20, false);

        seguirCombis();

        iniciarHilo();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapa);
        mapFragment.getMapAsync(this);
    }

    DatabaseReference referenciaAuxiliar;
    private void leerRuta() {

        referenciaAuxiliar=firebaseDatabase.getReference("Rutas").child(RutaPerteneciente).child("Subrutas");
        referenciaAuxiliar.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds:dataSnapshot.getChildren()){
                    if(ds.child("ruta").getValue().toString().equals(RutaAsignada)){
                        String cord = ds.child("camino").getValue().toString();
                        interpretarCordenadas(cord);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void interpretarCordenadas(String cord) {
        StringTokenizer st = new StringTokenizer(cord,",");

        while(st.hasMoreTokens()){
            Double lat = Double.parseDouble(st.nextToken());
            Double lon = Double.parseDouble(st.nextToken());
            LatLng cordAux = new LatLng(lat,lon);
            coordenadasRuta.add(cordAux);
        }

        cargarCordenadas();
    }

    private void cargarCordenadas() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        PolylineOptions polyLines = new PolylineOptions();
        coordenadasRuta.add(coordenadasRuta.get(0));
        polyLines.addAll(coordenadasRuta);
        polyLines.width(20);
        polyLines.color(Color.parseColor(ColorRecibido));
        googleMap.clear();
        googleMap.addPolyline(polyLines);
        locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,200,10,location_listener);

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

                    databaseReference.child("Combis").child(Key).child("lat").setValue(latAct+"");
                    databaseReference.child("Combis").child(Key).child("lon").setValue(lonAct+"");
                }

                latPas=latAct;
                lonPas=lonAct;

            }catch (Exception f){
                Toast.makeText(ActivityMapa.this, "Error en el handler/ "+f.getMessage(), Toast.LENGTH_SHORT).show();

            }

        }
    };


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


    private GoogleMap googleMap;
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap=googleMap;
        //Mostrar Ubicacion actual al abrir la app


        if(gpsStatus()){
            miUbicacion();

        }else{
            AlertDialog.Builder alerta=new AlertDialog.Builder(this);
            alerta.setTitle("GPS Desactivado");
            alerta.setCancelable(false);
            alerta.setPositiveButton("Activar GPS", new
                    DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new
                                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                            miUbicacion();
                        }
                    });
            alerta.setNegativeButton("Cancelar", new
                    DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            cerrarApp();
                        }
                    });
            alerta.create();
            alerta.show();
        }
    }

    private void cerrarApp() {
        this.finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.FROYO)
    private boolean gpsStatus() {
        ContentResolver content = getBaseContext().getContentResolver();
        boolean gps = Settings.Secure.isLocationProviderEnabled(content, LocationManager.NETWORK_PROVIDER);
        return gps;


    }

    private void seguirCombis() {

        //Obtener combis registradas en la ruta seleccionada

        final Query  refCombis=firebaseDatabase.getReference()
                .child("Rutas")
                .child(RutaPerteneciente)
                .child("Combis");


        refCombis.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot snap, String s) {
                String rutA = snap.child("rutaAsignada").getValue().toString();
                //aux= snap.getValue(Combi.class);
                if(snap!=null&&rutA.equalsIgnoreCase(RutaAsignada)){
                    String lat=snap.child("lat").getValue().toString();
                    String lon=snap.child("lon").getValue().toString();
                    String num=snap.child("numero").getValue().toString();

                    agregarMarcadorC(Double.parseDouble(lat),Double.parseDouble(lon),num+"-"+rutA,markAux);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot snap, String s) {

                String rutA = snap.child("rutaAsignada").getValue().toString();
                String lat=snap.child("lat").getValue().toString();
                String lon=snap.child("lon").getValue().toString();
                String num=snap.child("numero").getValue().toString();

                agregarMarcadorC(Double.parseDouble(lat),Double.parseDouble(lon),num+"-"+rutA,markAux);
                actualizarUbicacionC(rutA,lat,lon,num);
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
    }

    private ArrayList<Marker> lstMarkers= new ArrayList<>();
    private void actualizarUbicacionC(String rutA, String lat, String lon, String num) {
        for(int i=0;i<lstMarkers.size();i++){
            if(lstMarkers.get(i).getTitle().equalsIgnoreCase(num+"-"+rutA)){
                agregarMarcadorC(Double.parseDouble(lat),Double.parseDouble(lon),num+"-"+rutA,lstMarkers.get(i));
                lstMarkers.remove(i);

                break;

            }
        }
    }

    private void agregarMarcadorC(double lat, double lon,String title, Marker mark) {
        LatLng coordenadas = new LatLng(lat, lon);

        if (mark!= null) mark.remove();
        mark = googleMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title(title)
                .icon(BitmapDescriptorFactory.fromBitmap(bmpN)));

        lstMarkers.add(mark);

    }


    LocationManager locManager;

    private void miUbicacion(){
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
        CameraUpdate mPosition = CameraUpdateFactory.newLatLngZoom(coordenadas, 20);
        if (markCombi!= null) markCombi.remove();
        markCombi = googleMap.addMarker(new MarkerOptions()
                .position(coordenadas)
                .title(title)
                .icon(icono));
        googleMap.animateCamera(mPosition);


    }

}
