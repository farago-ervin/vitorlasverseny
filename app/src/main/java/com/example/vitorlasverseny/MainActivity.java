package com.example.vitorlasverseny;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getName();
//globalis valtozok
    EditText userNameET;
    EditText passwordET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userNameET = findViewById(R.id.editTextUserName);
        passwordET = findViewById(R.id.editTextPassword);
    }

    public void login(View view) {
        String userName = userNameET.getText().toString();
        String password = passwordET.getText().toString();

        //logoljuk hogy ki jelentkezett be, és milyen jelszóval és mikor
        Log.i(LOG_TAG,"Bejelentkezett: " + userName + ", Jelszo: " + password);

    }

    public void register(View view) {
        Intent Registerintent = new Intent(this, RegisterActivity.class);

        // TODO
        startActivity(Registerintent);
    }

    public void maps(View view) {
        Intent Mapsintent = new Intent(this,MapsActivity.class);
        startActivity(Mapsintent);
    }
}