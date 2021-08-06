package com.estufa.joffr.estufaapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DadosSalvos extends AppCompatActivity {

    private ProgressBar progressoenvio;
    private List<Umidade> dadosbanco;
    private CoordinatorLayout tela;
    private boolean connected = false;
    private Button botaonuvem;
    private TextView tv;
    private boolean flag = false;
    //private ProgressDialog pd;
    private static String url = "http://192.168.50.1:8080/Pomodoro/umidade";

    private List<Umidade> dados;
    private List<Umidade> dadosFiltro;

    private GraphView grafi;
    private View filtros;
    private Spinner spinDatas, spinAnos;

    private LineGraphSeries<DataPoint> series;
    private LineGraphSeries<DataPoint> meses;

    private ArrayAdapter adpterAnos;

    private List<String> anos = new ArrayList<>();
    private int mes;
    private String ano = null;
    private String[] calendario = new String[] {"Tudo", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

    //FIREBASE
        private DatabaseReference mDatabaseEnvio;
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

        spinDatas = findViewById(R.id.spinDatas);
        spinAnos = findViewById(R.id.spinAnos);
        filtros = findViewById(R.id.caixafiltro);

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
        mDatabaseEnvio = FirebaseDatabase.getInstance().getReference();

        progressoenvio = findViewById(R.id.progressBarnuvem);
        botaonuvem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (connected){
                //alert dialog de advertencia para o que o usuario esta fazendo
                AlertDialog alerta = new AlertDialog.Builder(DadosSalvos.this)
                        .setTitle(R.string.send_cloud)
                        .setMessage(R.string.want)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                enviarDados();
                            }
                        })
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Snackbar.make(tela, R.string.up_canc, Snackbar.LENGTH_SHORT).show();
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

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, calendario);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinDatas.setAdapter(adapter);
        spinDatas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mes = i;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        Button bot = findViewById(R.id.botfiltro);
        bot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ajustaGrafico();
            }
        });

        dados = new ArrayList<>();
        dadosFiltro = new ArrayList<>();
        SetGraficos();


        mDatabase = FirebaseDatabase.getInstance().getReference("dados");
        mDatabase.orderByChild("data").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot post : dataSnapshot.getChildren()){
                    Umidade u = post.getValue(Umidade.class);
                    dados.add(u);
                  //      Log.i(TAG, "onDataChange: "+ Date.valueOf(u.getData()));
                        Log.i("DadosColetados", "Dado Coletado: " + u.getData());


                }

                preencherGrafico();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i("dadosColetados", "erro ao coletar do firebase");
            }
        });

    }

    private void atualizarMsg(){
        dadosbanco = Umidade.listAll(Umidade.class);

        String text = String.valueOf(dadosbanco.size());
        //tratamento para o botao de enviar dados para nuvem, se nao tiver dados a ser emviados, ele não aparece ao usuario
        if (dadosbanco.size()>0) {
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
        Toast.makeText(DadosSalvos.this, R.string.sending, Toast.LENGTH_SHORT).show();
        if (dadosbanco.size()>0){
            for (Umidade umi : dadosbanco){
                mDatabaseEnvio.child("dados").push().setValue(umi).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //evento de falha caso aconteça algum imprevisto
                        Toast.makeText(DadosSalvos.this, getString(R.string.erro_to_send), Toast.LENGTH_SHORT).show();
                    }
                });
            }
                //evento de conclusao de envio para o feedback que foi tudo mandado
                Snackbar.make(tela, R.string.up_ok, Snackbar.LENGTH_SHORT).show();
                progressoenvio.setVisibility(View.GONE);
                //tudo enviado trata de apagar o que ta salvo no celular
                AlertDialog avisodelimpeza = new AlertDialog.Builder(DadosSalvos.this)
                        .setTitle("Aviso!").setMessage(R.string.del_advice)
                        .setPositiveButton("OK", null).create();
                avisodelimpeza.show();
                Umidade.deleteAll(Umidade.class);
                atualizarMsg();
/*
            //no banco do firebase no "galho" dados a lista sera salva
            mDatabase.child("dados").push().setValue(dados).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    //evento de conclusao de envio para o feedback que foi tudo mandado
                    Snackbar.make(tela, R.string.up_ok, Snackbar.LENGTH_SHORT).show();
                    progressoenvio.setVisibility(View.GONE);
                    //tudo enviado trata de apagar o que ta salvo no celular
                    AlertDialog avisodelimpeza = new AlertDialog.Builder(DadosSalvos.this)
                            .setTitle("Aviso!").setMessage(R.string.del_advice)
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
                    Toast.makeText(DadosSalvos.this, getString(R.string.erro_to_send), Toast.LENGTH_SHORT).show();
                }
            });
*/
        }else{
            Toast.makeText(DadosSalvos.this, "erro", Toast.LENGTH_SHORT).show();
        }


    }

    private void preencherGrafico(){
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd"),
                out = new SimpleDateFormat("yyyy");
        Date date;
        ano = null;
        anos.clear();
        anos.add("Todos");
        int x=0;
        for (Umidade u:dados){
            series.appendData(new DataPoint(x, dados.get(x).getValor()), false, dados.size());
            x++;
            try {
                date = in.parse(u.getData());
                if (!out.format(date).equals(ano)){
                    ano = out.format(date);
                    anos.add(ano);
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        adpterAnos = new ArrayAdapter(this, android.R.layout.simple_spinner_item, anos);
        adpterAnos.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinAnos.setAdapter(adpterAnos);
        spinAnos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //Log.i("teste", "onItemSelected: "+anos.get(i));
                ano = anos.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        grafi.addSeries(series);
        filtros.setVisibility(View.VISIBLE);
    }

    public void SetGraficos() {
        //GRAFICO 1
        grafi = (GraphView) findViewById(R.id.graf);

        grafi.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        //series = new LineGraphSeries<>(pontos);
        series = new LineGraphSeries<>();
        series.setDrawBackground(true);
        series.setDrawDataPoints(true);
        grafi.getViewport().setScalable(true);
        // set manual X bounds
        grafi.getViewport().setXAxisBoundsManual(true);
        grafi.getViewport().setMinX(1);
        grafi.getViewport().setMaxX(100);
        // set manual Y bounds
        grafi.getViewport().setYAxisBoundsManual(true);
        grafi.getViewport().setMinY(0);
        grafi.getViewport().setMaxY(80);
        grafi.getViewport().setScrollableY(true);
        //listener do ponto
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(DadosSalvos.this,
                        getString(R.string.day)+": "+dados.get((int)dataPoint.getX()).getData()+" - "+
                                dados.get((int)dataPoint.getX()).getHora()+"h\n"+getString(R.string.humidity)+": "+
                                dados.get((int)dataPoint.getX()).getValor()+"%",
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    public void ajustaGrafico(){
        //Me deu um pouco de medo isso que eu fiz, espero um dia precisar ajeitar isso, por enquanto... it works kkkkk =]
        grafi.removeAllSeries();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sout;
        Date date;
        int x=0;

        if (mes == 0 && ano.equals("Todos")){
            //tudo
            grafi.addSeries(series);
        }else if(mes != 0 && ano.equals("Todos")){
            //mes especifco de todos os anos... nao realizavel
            Toast.makeText(DadosSalvos.this, "Filtro nao aceito", Toast.LENGTH_SHORT).show();
        }
        else if (mes == 0 && !ano.equals("Todos")){
            //todos os meses de um ano especifico
            meses = new LineGraphSeries<>();
            meses.setDrawDataPoints(true);
            meses.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    Toast.makeText(DadosSalvos.this,
                            getString(R.string.day)+": "+dadosFiltro.get((int)dataPoint.getX()).getData()+" - "+
                                    dadosFiltro.get((int)dataPoint.getX()).getHora()+"h\n"+getString(R.string.humidity)+": "+
                                    dadosFiltro.get((int)dataPoint.getX()).getValor()+"%",
                            Toast.LENGTH_LONG).show();
                }
            });
            sout = new SimpleDateFormat("yyyy");
            for (Umidade u: dados){
                try {
                    date = sdf.parse(u.getData());
                    if (sout.format(date).equals(ano)){
                        meses.appendData(new DataPoint(x, dados.get(x).getValor()), false, dados.size());
                        dadosFiltro.add(u);
                        x++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            grafi.addSeries(meses);
        }else {
            //mes especifico de ano especifico
            meses = new LineGraphSeries<>();
            meses.setDrawDataPoints(true);
            meses.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    Toast.makeText(DadosSalvos.this,
                            getString(R.string.day)+": "+dadosFiltro.get((int)dataPoint.getX()).getData()+" - "+
                                    dadosFiltro.get((int)dataPoint.getX()).getHora()+"h\n"+getString(R.string.humidity)+": "+
                                    dadosFiltro.get((int)dataPoint.getX()).getValor()+"%",
                            Toast.LENGTH_LONG).show();
                }
            });
            sout = new SimpleDateFormat("MM/yyyy");
            dadosFiltro.clear();
            String mesano;
            if (mes < 10){
                mesano = "0"+mes+"/"+ano;
            }else {
                mesano = mes + "/" + ano;
            }
            for (Umidade u : dados) {
                try {
                    date = sdf.parse(u.getData());
                    if (sout.format(date).equals(mesano)) {
                        meses.appendData(new DataPoint(x, dados.get(x).getValor()), false, dados.size());
                        dadosFiltro.add(u);
                        x++;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
            grafi.addSeries(meses);
        }
    }


//=======================CONSUMO JSON=========================================

    private class JsonTask extends AsyncTask<String, String, String> {

        int result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            /*pd = new ProgressDialog(DadosSalvos.this);
            pd.setMessage(getString(R.string.geting_data));
            pd.setCancelable(false);
            pd.show();*/
            Snackbar.make(tela, getString(R.string.geting_data),Snackbar.LENGTH_SHORT).show();
            progressoenvio.setVisibility(View.VISIBLE);
        }

        @Override
        protected String doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try{
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                result = connection.getResponseCode();
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

            /*if (pd.isShowing()){
                pd.dismiss();
            }*/
            if(result == 200){
                if (!(arrayUmidade == null)) {
                    for (Umidade u : arrayUmidade) {
                        u.save();
//                    Log.i("objeto", u.toString());
                    }
                    atualizarMsg();

                    AlertDialog avisodelimpeza = new AlertDialog.Builder(DadosSalvos.this)
                            .setTitle("Aviso!").setMessage(R.string.del_database_aviso)
                            .setPositiveButton("OK", null).create();
                    avisodelimpeza.show();

                    new DeletaBancoRasp().execute(url);
                }else{
                    Snackbar.make(tela, getString(R.string.erro_coleta), Snackbar.LENGTH_LONG).show();
                }
            }else {
                Snackbar.make(tela, getString(R.string.erro_to_get), Snackbar.LENGTH_LONG).show();
            }

            progressoenvio.setVisibility(View.GONE);
        }
    }

//=======================DELETE REQUEST========================================
    private class DeletaBancoRasp extends AsyncTask<String, String, String> {

    int result;

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        /*pd = new ProgressDialog(DadosSalvos.this);
        pd.setMessage(getString(R.string.geting_data));
        pd.setCancelable(false);
        pd.show();*/
        progressoenvio.setVisibility(View.VISIBLE);
    }

    @Override
    protected String doInBackground(String... params) {

        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try{
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            //connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestMethod("DELETE");
            connection.connect();
            result = connection.getResponseCode();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null){
                connection.disconnect();
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        /*if (pd.isShowing()){
            pd.dismiss();
        }*/
        if(result == 200){
            Toast.makeText(DadosSalvos.this, "200 OK", Toast.LENGTH_SHORT).show();
        }else{
            Toast.makeText(DadosSalvos.this, "Houve um erro em limpar os dados do banco", Toast.LENGTH_SHORT).show();
        }
        progressoenvio.setVisibility(View.GONE);
    }
}
}
