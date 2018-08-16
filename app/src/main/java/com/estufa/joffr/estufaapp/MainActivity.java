package com.estufa.joffr.estufaapp;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {



    private WifiManager wifi;

    private static String MQTTHOST = "tcp://192.168.50.1:1883";
    private static String USERNAME = "JoffrMQTT";
    private static String SENHA = "mosquito";

//    private String topicoU = "Umidade"; //topicos usados nessa aplicação
    private boolean conectado = false; //flag para conexao com o broker
    private List<Umidade> dados;
    private List<Umidade> dadosFiltro;

    private MqttAndroidClient client;
    private MqttConnectOptions options;

    private GraphView grafi;
    private View tela, filtros;
    private Spinner spinDatas;

    private LineGraphSeries<DataPoint> series;
    private LineGraphSeries<DataPoint> meses;

    private int mes;
    private String TAG = MainActivity.class.getSimpleName();
    private String[] calendario = new String[] {"Tudo", "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};

    //firebase
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //AQUI TENTA RECONECTAR, CASO ESTEJA DESCONECTADO
                if (!conectado) {
                    if (wifi.isWifiEnabled()) {
                        ConectaMQTT();
                    } else {
                        Snackbar.make(tela, R.string.wifidesc, Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    Snackbar.make(tela, R.string.alrd_con, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        tela = findViewById(R.id.tela);
        filtros = findViewById(R.id.caixafiltro);

        spinDatas = findViewById(R.id.spinDatas);
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


        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

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
//                        Log.i(TAG, "onDataChange: "+Date.valueOf(u.getData()));

                    }

                    preencherGrafico();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        CriaClienteMQTT();

    }

//====================== METODOS QUE TRABALHAM O MQTT ==============================

    private void CriaClienteMQTT() {
        //TRABALHANDO O MQTT CLIENT

        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST, clientId);

        options = new MqttConnectOptions();
        options.setUserName(USERNAME);
        options.setPassword(SENHA.toCharArray());

        //TESTA SE O DISPOSITIVO ESTA CONECTADO AO WIFI, SE NAO ESTIVER ELE NAO TENTARA CONECTAR AO BROKER
        if (wifi.isWifiEnabled()) {
            ConectaMQTT();
        } else {
            Snackbar.make(tela, R.string.wifidesc, Snackbar.LENGTH_SHORT).show();
        }

        client.setCallback(new MqttCallback() {
            //aqui trata eventos do mqtt (perda de conexao, chegada de mensagens e envio de mensagens
            @Override
            public void connectionLost(Throwable cause) {
                //caso a conexao caia
                Snackbar.make(tela, "Conexão com o Broker perdida", Snackbar.LENGTH_SHORT).show();
                conectado = false;
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //executa sempre que uma nova mensagem chega do broker que esta conectado
//                String humPayload;
//                if (topic.equals(topicoU)) {
//                    humPayload = new String(message.getPayload());
//                    float y = Float.valueOf(humPayload);
//
//                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
    //metodo de conexão ao mqtt broker
    private void ConectaMQTT() {
        try {
            Snackbar.make(tela, R.string.crt_con, Snackbar.LENGTH_SHORT).show();
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //conectou
//                    Log.d("teste", "onSucess");
//                    Toast.makeText(MainActivity.this, "Conectou", Toast.LENGTH_SHORT).show();
                    Snackbar.make(tela, R.string.BrokerConect, Snackbar.LENGTH_SHORT).show();

                    setSubcription();
                    conectado = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //algo deu errado
//                    Log.d("onFailure", "onFailure: "+exception.toString());
                    Snackbar.make(tela, R.string.BrokerErr, Snackbar.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, ""+e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    //metodo de inscrição de topico mqtt
    private void setSubcription() {
//        try {
//            client.subscribe(topicoU, 0);
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
    }

//===============================================================================

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
                Toast.makeText(MainActivity.this,
                        R.string.day+": "+dados.get((int)dataPoint.getX()).getData()+" - "+
                                dados.get((int)dataPoint.getX()).getHora()+"h\n"+R.string.humidity+": "+
                                dados.get((int)dataPoint.getX()).getValor()+"%",
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private void preencherGrafico(){
        int x=0;
        for (Umidade u:dados){
            series.appendData(new DataPoint(x, dados.get(x).getValor()), false, dados.size());
            x++;
        }
        grafi.addSeries(series);
        filtros.setVisibility(View.VISIBLE);
    }

    public void ajustaGrafico(){

        grafi.removeAllSeries();
        if (mes == 0){
            grafi.addSeries(series);
        }else {
            meses = new LineGraphSeries<>();
            meses.setDrawDataPoints(true);
            meses.setOnDataPointTapListener(new OnDataPointTapListener() {
                @Override
                public void onTap(Series series, DataPointInterface dataPoint) {
                    Toast.makeText(MainActivity.this,
                            R.string.day+": "+dadosFiltro.get((int)dataPoint.getX()).getData()+" - "+
                                    dadosFiltro.get((int)dataPoint.getX()).getHora()+"h\n"+R.string.humidity+": "+
                                    dadosFiltro.get((int)dataPoint.getX()).getValor()+"%",
                            Toast.LENGTH_LONG).show();
                }
            });
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat sout = new SimpleDateFormat("MM");
            Date date;
            int x=0;
            dadosFiltro.clear();
            for (Umidade u : dados) {
                try {
                    date = sdf.parse(u.getData());
                    if (Integer.parseInt(sout.format(date)) == mes) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_dados) {
            startActivity(new Intent(this, DadosSalvos.class));
        }
        return super.onOptionsItemSelected(item);
    }
}
