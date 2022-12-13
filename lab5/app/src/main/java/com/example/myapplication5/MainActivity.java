package com.example.myapplication5;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String APP_PREFERENCES = "mysetting";
    public static final String APP_PREFERENCES_NAME = "name";
    public static final String APP_PREFERENCES_PASS = "pass";

    private SharedPreferences mSetting;

    DatabaseHandler db = new DatabaseHandler(this);

    private User user = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DataActivity.db = db;

        mSetting = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);



        Intent openIntent = new Intent(this, DataActivity.class);

        setContentView(R.layout.activity_main);

        Button buttonCreateProf = findViewById(R.id.buttonCreateProf);
        Button buttonDataBase = findViewById(R.id.buttonDataBase);
        Button buttonAvtorization = findViewById(R.id.buttonAvtorization);
        Button buttonResetPassword = findViewById(R.id.buttonResetPassword);

        EditText name = findViewById(R.id.editTextName);
        EditText pass = findViewById(R.id.editTextPassword);
        TextView statusAvtorization = findViewById(R.id.textViewStatusAvtorization);

        buttonAvtorization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean status = false;

                user = new User(name.getText().toString(), pass.getText().toString());

                status = db.findUser(user);

                if (status) {
                    statusAvtorization.setText("Вы авторизированны");
                    buttonResetPassword.setEnabled(true);
                    buttonCreateProf.setEnabled(false);
                    name.setEnabled(false);
                } else {
                    statusAvtorization.setText("Вы не авторизированны");
                    buttonResetPassword.setEnabled(false);
                    buttonCreateProf.setEnabled(true);
                    name.setEnabled(true);
                }
            }
        });


        buttonCreateProf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.addUser(new User(name.getText().toString(), pass.getText().toString()));
                //DataActivity.listString.add(name.getText() + "" + pass.getText());
            }
        });

        buttonResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                db.resetPassword(user, pass.getText().toString());

            }
        });

        buttonDataBase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(openIntent);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        EditText name = findViewById(R.id.editTextName);
        EditText pass = findViewById(R.id.editTextPassword);


        SharedPreferences.Editor edit = mSetting.edit();
        edit.clear();

        edit.putString(APP_PREFERENCES_NAME, name.getText().toString());
        edit.apply();

        edit.putString(APP_PREFERENCES_PASS, pass.getText().toString());
        edit.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();

        EditText name = findViewById(R.id.editTextName);
        EditText pass = findViewById(R.id.editTextPassword);

        if(mSetting.contains(APP_PREFERENCES_NAME))
            name.setText(mSetting.getString(APP_PREFERENCES_NAME, ""));

        if(mSetting.contains(APP_PREFERENCES_PASS))
            pass.setText(mSetting.getString(APP_PREFERENCES_PASS, ""));
    }
}

