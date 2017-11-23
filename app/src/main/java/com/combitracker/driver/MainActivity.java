package com.combitracker.driver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    EditText txUsuario,txContraseña;
    ImageView btnIngresar;

    public FirebaseDatabase firebaseDatabase;
    public DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDatabase=FirebaseDatabase.getInstance();

        try{
            firebaseDatabase.setPersistenceEnabled(true);
        }catch (Exception e){

        }
        DatabaseReference ref = firebaseDatabase.getReference("Rutas");
        ref.keepSynced(true);

        txUsuario=findViewById(R.id.txUsuario);
        txContraseña=findViewById(R.id.txContraseña);
        btnIngresar=findViewById(R.id.btnIngresar);

        btnIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logear();
            }
        });

    }

    private ArrayList<String> listaRutas = new ArrayList<>();

    boolean found=false;

    String usuario;
    String contraseña;
    String color;

    private void logear() {
        usuario = txUsuario.getText().toString();
        contraseña = txContraseña.getText().toString();
        Toast.makeText(MainActivity.this,"Espere un Momento Por Favor.",Toast.LENGTH_LONG).show();
        if(!usuario.isEmpty() || !contraseña.isEmpty()){
            final DatabaseReference buscador = firebaseDatabase.getReference("Rutas");
            buscador.addValueEventListener(new ValueEventListener() {
                boolean found = false;
                String user,pass,key,rutaAsignada,ruta;
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for(DataSnapshot ds : dataSnapshot.getChildren()){
                        if(!found) {
                            DataSnapshot datos = ds.child("Combis");
                            for (DataSnapshot combis : datos.getChildren()) {
                                String us = combis.child("usuario").getValue().toString();
                                if (us.equals(usuario)) {
                                    String ps = combis.child("contraseña").getValue().toString();
                                    if (ps.equals(contraseña)) {
                                        found = true;

                                        ruta =ds.getKey();
                                        user = combis.child("usuario").getValue().toString();
                                        pass = combis.child("contraseña").getValue().toString();
                                        key = combis.getKey();
                                        rutaAsignada = combis.child("rutaAsignada").getValue().toString();
                                        color = ds.child("Color").getValue().toString();
                                        break;
                                    }
                                }
                            }
                        }else{
                            break;
                        }

                    }
                    txUsuario.setText("");
                    txContraseña.setText("");
                    txUsuario.requestFocus();
                    if(found){
                        abrirActividadMapa(user,pass,key,rutaAsignada,ruta,color);
                    }else{
                        txUsuario.setError("No Existe el Usuario Especificado");
                    }
                    buscador.removeEventListener(this);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }else{
            if(usuario.isEmpty()){
                txUsuario.setError("Ingresa Email");
            }
            if(contraseña.isEmpty()){
                txContraseña.setError("Ingresa Contraseña");
            }
        }
    }

    private void abrirActividadMapa(final String user, final String pass, final String key, final String rutaAsignada, String ruta, String color){

        Intent i = new Intent(MainActivity.this,ActivityMapa.class);
        i.putExtra("Usuario",user);
        i.putExtra("Contraseña",pass);
        i.putExtra("RutaPerteneciente", ruta);
        i.putExtra("Key",key);
        i.putExtra("RutaAsignada",rutaAsignada);
        i.putExtra("Color",color);
        startActivity(i);
    }
}
