package com.example.myapplication5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;


public class DataActivity extends AppCompatActivity {


    static ArrayList<String> listString = new ArrayList<String>();
    static DatabaseHandler db;


       @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_activity);

        Button buttonAdd = findViewById(R.id.buttonAdd);
        Button buttonRemove = findViewById(R.id.buttonRemove);
        Button buttonBack = findViewById(R.id.buttonBack);

        EditText textToDelete = findViewById(R.id.editTextNameToDelete);

        Intent openIntent = new Intent(this, MainActivity.class);


        ListView listView = findViewById(R.id.list_item);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listString);

        listView.setAdapter(adapter);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    List<User> users = db.getAllUsers();
                    adapter.clear();
                    for (User usr : users) {
                        String str = "Id:" + usr.getID() + " ,Login: " + usr.getLogin() +
                                " Password: " + usr.getPass();
                        listString.add(str);
                        listView.setAdapter(adapter);
                    }
            }
        });

        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    db.deleteByName(textToDelete.getText().toString());
                    List<User> users = db.getAllUsers();

                    adapter.clear();
                    for (User usr : users) {
                        String str = "Id:" + usr.getID() + " ,Login: " + usr.getLogin() +
                                " Password: " + usr.getPass();
                        listString.add(str);
                        listView.setAdapter(adapter);
                    }
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
