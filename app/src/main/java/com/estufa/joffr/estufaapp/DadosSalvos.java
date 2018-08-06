package com.estufa.joffr.estufaapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

    private ProgressBar progressoenvio;
    private List<Umidade> dados;
    private CoordinatorLayout tela;
    private boolean connected = false;
    private Button botaonuvem;
    private TextView tv;
    private ProgressDialog pd;
    //private static String url = "http://192.168.0.11/api-rest-php/view/Conteudo/listar.php";
    private static String url = "http://192.168.50.1:8080/Pomodoro/umidade";

    //FIREBASE
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dados_salvos);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tela = findViewById(R.id.teladados);
        botaonuvem = findViewById(R.id.botaoSalvarNuvem);

        //teste se há conexão com o banco no momento
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                connected = dataSnapshot.getValue(Boolean.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DadosSalvos.this, "Listener de conexão perdida\n"+databaseError.getDetails(), Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton fabjson = findViewById(R.id.fabjson);
        fabjson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
                new JsonTask().execute(url);
        //XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
            }
        });
        //instanciando o banco da nuvem
        mDatabase = FirebaseDatabase.getInstance().getReference();

        progressoenvio = findViewById(R.id.progressBarnuvem);
        botaonuvem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (connected){
                //alert dialog de advertencia para o que o usuario esta fazendo
                AlertDialog alerta = new AlertDialog.Builder(DadosSalvos.this)
                        .setTitle("Enviar para nuvem")
                        .setMessage("Deseja enviar os dados agora?")
                        .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                enviarDados();
                            }
                        })
                        .setNegativeButton("Não", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Snackbar.make(tela, "Envio cancelado", Snackbar.LENGTH_SHORT).show();
                            }
                        }).create();
                alerta.show();
//                }else{
//                    Snackbar.make(tela, "Incapaz de enviar os dados agora, verifique sua conexão", Snackbar.LENGTH_SHORT).show();
//                }
            }
        });

        tv = findViewById(R.id.txt);
        atualizarMsg();

    }

    private void atualizarMsg(){
        dados = Umidade.listAll(Umidade.class);

        String text = String.valueOf(dados.size());
        //tratamento para o botao de enviar dados para nuvem, se nao tiver dados a ser emviados, ele não aparece ao usuario
        if (dados.size()>0) {
            botaonuvem.setVisibility(View.VISIBLE);
        }else{
            botaonuvem.setVisibility(View.GONE);
        }

        tv.setText(text);
//        for (Umidade u : dados){
//            text += u.toString()+"\n";
//        }

    }

    private void enviarDados(){

        progressoenvio.setVisibility(View.VISIBLE);
        Toast.makeText(DadosSalvos.this, "Enviando dados, não sair dessa tela", Toast.LENGTH_SHORT).show();
        if (dados.size()>0){

            //no banco do firebase no "galho" dados a lista sera salva
            mDatabase.child("dados").setValue(dados).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    //evento de conclusao de envio para o feedback que foi tudo mandado
                    Snackbar.make(tela, "Dados enviados", Snackbar.LENGTH_SHORT).show();
                    progressoenvio.setVisibility(View.GONE);
                    //tudo enviado trata de apagar o que ta salvo no celular
                    AlertDialog avisodelimpeza = new AlertDialog.Builder(DadosSalvos.this)
                            .setTitle("Aviso!").setMessage("Com os dados salvos em nuvem, os dados armazenados nesse dispositivos serão apagados")
                            .setPositiveButton("OK", null).create();
                    avisodelimpeza.show();
                    Umidade.deleteAll(Umidade.class);
                    atualizarMsg();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    //evento de falha caso aconteça algum imprevisto
                    Toast.makeText(DadosSalvos.this, "Algo deu errado ao enviar os dados", Toast.LENGTH_SHORT).show();
                }
            });

        }else{
            Toast.makeText(DadosSalvos.this, "erro", Toast.LENGTH_SHORT).show();
        }


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
            if (!(arrayUmidade == null)) {
                for (Umidade u : arrayUmidade) {
                    u.save();
                    Log.i("objeto", u.toString());
                }
                atualizarMsg();
            }else{
                Snackbar.make(tela, "Houve um erro em coletar os dados", Snackbar.LENGTH_LONG).show();
            }
            if (pd.isShowing()){
                pd.dismiss();
            }

        }
    }
}
