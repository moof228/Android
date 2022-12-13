package com.example.myapplication5;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String APP_PREFERENCES = "mysetting";
    public static final String APP_PREFERENCES_NAME = "name";
    public static final String APP_PREFERENCES_PASS = "pass";

    private SharedPreferences mSetting;

    private User user = null;

    Button buttonCreateProf;
    Button buttonDataBase;
    Button buttonAvtorization;
    Button buttonResetPassword;
    EditText name;
    EditText pass;
    TextView statusAvtorization;


    final Looper looper = Looper.getMainLooper();
    final Handler handler = new Handler(looper) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.sendingUid == 3) {
                boolean status = false;

                status = (boolean) msg.obj;

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
            if(msg.sendingUid == 2){
                String mess = (String) msg.obj;
                Toast.makeText(MainActivity.this, mess, Toast.LENGTH_SHORT).show();
            }
            if(msg.sendingUid == 4){
                String mess = (String) msg.obj;
                Toast.makeText(MainActivity.this, mess, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSetting = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);

        Intent openIntent = new Intent(this, DataActivity.class);

        setContentView(R.layout.activity_main);

        buttonCreateProf = findViewById(R.id.buttonCreateProf);
        buttonDataBase = findViewById(R.id.buttonDataBase);
        buttonAvtorization = findViewById(R.id.buttonAvtorization);
        buttonResetPassword = findViewById(R.id.buttonResetPassword);

        name = findViewById(R.id.editTextName);
        pass = findViewById(R.id.editTextPassword);
        statusAvtorization = findViewById(R.id.textViewStatusAvtorization);

        buttonAvtorization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                user = new User(name.getText().toString(), pass.getText().toString());
                new ThreadHandler(handler, MainActivity.this).userFind(user);
            }
        });

        buttonCreateProf.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadHandler(handler, MainActivity.this).userAdd(name.getText().toString(), pass.getText().toString());
            }
        });

        buttonResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadHandler(handler, MainActivity.this).resetPass(user, pass.getText().toString());
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

