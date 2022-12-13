package com.example.myapplication5;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;


public class DataActivity extends AppCompatActivity {


    static ArrayList<String> listString = new ArrayList<String>();
    static DatabaseHandler db;

    final Looper looper = Looper.getMainLooper();
    final Handler handler = new Handler(looper) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.sendingUid == 1) {
                List<User> users = (List<User>) msg.obj;
                adapter.clear();
                for (User usr : users) {
                    String str = "Id:" + usr.getID() + " ,Login: " + usr.getLogin() +
                            " Password: " + usr.getPass();
                    listString.add(str);
                    listView.setAdapter(adapter);
                }
            }
            if(msg.sendingUid == 5) {

                String message = (String) msg.obj;
                Toast.makeText(DataActivity.this, message, Toast.LENGTH_SHORT).show();
                new ThreadHandler(handler, DataActivity.this).getAllUser();
            }
        }
    };

    Button buttonAdd;
    Button buttonRemove;
    Button buttonBack;
    EditText textToDelete;
    ListView listView;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_activity);

        buttonAdd = findViewById(R.id.buttonAdd);
        buttonRemove = findViewById(R.id.buttonRemove);
        buttonBack = findViewById(R.id.buttonBack);

        textToDelete = findViewById(R.id.editTextNameToDelete);

        Intent openIntent = new Intent(this, MainActivity.class);


        listView = findViewById(R.id.list_item);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listString);

        listView.setAdapter(adapter);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadHandler(handler, DataActivity.this).getAllUser();
            }
        });

        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ThreadHandler(handler, DataActivity.this).delByName(textToDelete.getText().toString());
            }
        });

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(openIntent);
            }
        });

    }
}
