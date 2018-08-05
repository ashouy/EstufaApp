package com.estufa.joffr.estufaapp;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class DadosSalvos extends AppCompatActivity {

    private TextView tv;
    private ProgressDialog pd;
    //private static String url = "http://192.168.0.11/api-rest-php/view/Conteudo/listar.php";
    private static String url = "http://192.168.50.1:8080/Pomodoro/umidade";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dados_salvos);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        FloatingActionButton fabjson = findViewById(R.id.fabjson);
        fabjson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
                new JsonTask().execute(url);
                // new GetContacts().execute();
        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
            }
        });

        tv = findViewById(R.id.txt);
        atualizarMsg();

    }

    private void atualizarMsg(){
        List<Umidade> dados = Umidade.listAll(Umidade.class);

        String text = String.valueOf(dados.size());

//        for (Umidade u : dados){
//            text += u.toString()+"\n";
//        }

        tv.setText(text);
    }

//=======================CONSUMO JSON=========================================

    private class JsonTask extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(DadosSalvos.this);
            pd.setMessage("Coletando dados");
            pd.setCancelable(false);
            pd.show();
        }

        @Override
        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try{
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                InputStream stream = connection.getInputStream();

                reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while((line = reader.readLine()) != null){
                    buffer.append(line+"\n");
//                    Log.d("Response: ", "> "+line);
                }

                return buffer.toString();

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null){
                    connection.disconnect();
                }
                try{
                    if (reader != null){
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Gson gson = new Gson();
            Umidade[] arrayUmidade = gson.fromJson(s, Umidade[].class);
//            Log.i("tamanho: ", ""+arrayUmidade[1].toString());
            for(Umidade u: arrayUmidade){
                u.save();
                Log.i("objeto", u.toString());
            }
            if (pd.isShowing()){
                pd.dismiss();
            }
            atualizarMsg();
        }
    }
}
