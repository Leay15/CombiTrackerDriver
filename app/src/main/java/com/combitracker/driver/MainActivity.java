package com.combitracker.driver;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Spinner spinnerRutas;
    EditText txUsuario,txContraseña;
    Button btnIngresar;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseDatabase=FirebaseDatabase.getInstance();
        spinnerRutas=findViewById(R.id.spRutas);
        txUsuario=findViewById(R.id.txUsuario);
        txContraseña=findViewById(R.id.txContraseña);
        btnIngresar=findViewById(R.id.btnIngresar);

        llenarRutas();
        btnIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logear();
            }
        });

        spinnerRutas.setSelection(1);
    }

    private ArrayList<String> listaRutas = new ArrayList<>();
    private void llenarRutas() {
        databaseReference=firebaseDatabase.getReference("Rutas");

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    listaRutas.add(ds.getKey());
                }
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_spinner_item,listaRutas);
                spinnerRutas.setAdapter(adapter);
                databaseReference.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    boolean found=false;

    private void logear() {
        final String usuario = txUsuario.getText().toString();
        final String contraseña = txContraseña.getText().toString();
        final String ruta = spinnerRutas.getSelectedItem().toString();

        if(!usuario.isEmpty() || !contraseña.isEmpty()){
            databaseReference=databaseReference.child(ruta).child("Combis");


            databaseReference.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String user = dataSnapshot.child("Usuario").getValue().toString();
                    String pass = dataSnapshot.child("Contraseña").getValue().toString();

                        if(user.equals(usuario)){
                            if(pass.equals(contraseña)){
                                found=true;
                                Intent i = new Intent(MainActivity.this,ActivityMapa.class);
                                i.putExtra("Usuario",user);
                                i.putExtra("Contraseña",pass);
                                i.putExtra("RutaPerteneciente",ruta);
                                databaseReference.removeEventListener(this);
                                startActivity(i);
                            }else{
                                Toast.makeText(MainActivity.this,"Credenciales Incorrectas",Toast.LENGTH_LONG).show();
                                txContraseña.setText("");
                                txUsuario.setText("");
                                spinnerRutas.setSelection(0);
                                databaseReference.removeEventListener(this);
                            }
                        }

                    if(!found){
                        Toast.makeText(MainActivity.this,"No Existe el Usuario Especificado",Toast.LENGTH_LONG).show();
                    }
                    txUsuario.setText("");
                    txContraseña.setText("");
                    spinnerRutas.setSelection(0);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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
        }else{
            if(usuario.isEmpty()){
                txUsuario.setError("Ingresa Usuario");
            }
            if(contraseña.isEmpty()){
                txContraseña.setError("Ingresa Contraseña");
            }
        }
    }
}
