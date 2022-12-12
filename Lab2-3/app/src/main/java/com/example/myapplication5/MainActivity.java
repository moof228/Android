package com.example.myapplication5;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent openIntent = new Intent(this, DataActivity.class);

        setContentView(R.layout.activity_main);

        Button buttonCreateProf = findViewById(R.id.buttonCreateProf);
        Button buttonDataBase = findViewById(R.id.buttonDataBase);

        EditText name = findViewById(R.id.editTextName);
        EditText pass = findViewById(R.id.editTextPassword);

        buttonCreateProf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DataActivity.listString.add(name.getText() + "" + pass.getText());
            }
        });

        buttonDataBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(openIntent);
            }
        });
    }
}

