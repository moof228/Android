package com.example.myapplication5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;



public class DataActivity extends AppCompatActivity {

    static ArrayList<String> listString = new ArrayList<String>();

       @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.data_activity);

        Button buttonAdd = findViewById(R.id.buttonAdd);
        Button buttonRemove = findViewById(R.id.buttonRemove);
        Button buttonBack = findViewById(R.id.buttonBack);

        Intent openIntent = new Intent(this, MainActivity.class);


        ListView listView = findViewById(R.id.list_item);
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, listString);

        listView.setAdapter(adapter);

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                listString.add("WOW!!!");
                listView.setAdapter(adapter);
            }
        });

        buttonRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listString.remove(listString.size() - 1);
                listView.setAdapter(adapter);
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
