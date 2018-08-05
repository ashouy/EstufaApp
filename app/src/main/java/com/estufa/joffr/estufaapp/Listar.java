package com.estufa.joffr.estufaapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class Listar extends AppCompatActivity {

    TextView tv;
    List<Umidade> dados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listar);

        tv = findViewById(R.id.txt);

        Banco bd = new Banco(this);
        dados = new ArrayList<>();
        dados = bd.findAll();

        String text = "";
        Log.i("tam", ""+dados.size());
        for (int i=0; i<dados.size(); i++){
            text += ""+dados.get(i).toString()+"/n";
            Log.i("for", ""+i);
        }
        Log.i("textosalvo", ""+text);
        Log.i("construiu", "onCreate: terminou");
        tv.setText(text);
    }
}
