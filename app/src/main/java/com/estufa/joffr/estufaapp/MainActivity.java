package com.estufa.joffr.estufaapp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
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
    private String fase, dianum, fasePomodoro;
    private AlertDialog alert, alert2;
    private AlertDialog.Builder builder, builder2;

    private static String MQTTHOST = "tcp://192.168.50.1:1883",
                        USERNAME = "JoffrMQTT", SENHA = "mosquito",
                        ATIVO = "1", DESATIVO = "0";

    private int valorIdeal, valorMinimo;
    private String topicoB = "Bomba", topicoM = "Manual",
                    topicoI = "Ideal", topicoMin = "Minimo",
                    topicoTemporada = "Temporada", topicoD = "Dia"; //topicos usados nessa aplicação
    private boolean conectado = false; //flag para conexao com o broker

    private MqttAndroidClient client;
    private MqttConnectOptions options;

    private View tela;

    private FloatingActionButton fab;
    private Switch SwManual, SwBomba;
    private TextView estadobomba;
    private ProgressBar carregatopic;
    private SeekBar umidademaxima, umidademinima;
    private TextView valormax, valormin, faseatual, diaatual;
    private Button butSetFase, butSetDia, butsetTolerancia;
    private Spinner temporada, dia;
    private CardView infos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
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
        infos = findViewById(R.id.infos);
        faseatual = findViewById(R.id.valorfaseatual);
        diaatual = findViewById(R.id.valordiaatual);
        estadobomba = findViewById(R.id.estBomba);
        SwManual = findViewById(R.id.swmanual);
        SwBomba = findViewById(R.id.swBomba);

        tela = findViewById(R.id.tela);

        butsetTolerancia = findViewById(R.id.settolerancia);
        umidademaxima = findViewById(R.id.valortoleranciamaxima);
        umidademinima = findViewById(R.id.valortoleranciaminima);
        valormax = findViewById(R.id.valormax);
        valormin = findViewById(R.id.valorminimo);

        butsetTolerancia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder = new AlertDialog.Builder(MainActivity.this)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MqttMessage m = new MqttMessage();
                        m.setRetained(true);
                        m.setPayload(String.valueOf(valorIdeal).getBytes());
                        try {
                            client.publish(topicoI, m);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        EnviaMinimo();
                    }
                }).setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Snackbar.make(tela, "Cancelado", Snackbar.LENGTH_SHORT).show();
                        EnviaMinimo();
                    }
                }).setTitle("Confirmar atualização").setMessage("Atualizar o valor ideal de campo: "+valorIdeal+"%");
                alert = builder.create();
                alert.show();
            }
        });

        butSetFase = findViewById(R.id.setfase);
        butSetDia = findViewById(R.id.setDia);
        temporada = findViewById(R.id.fasenum);
        dia = findViewById(R.id.spinDias);

        temporada.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                fase = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        butSetFase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder = new AlertDialog.Builder(MainActivity.this).setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (conectado){
                            try{
                                MqttMessage m = new MqttMessage();
                                m.setRetained(true);
                                m.setPayload(fase.getBytes());
                                client.publish(topicoTemporada, m);
                            }catch (MqttException e){
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "Erro ao enviar fase", Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(MainActivity.this, "Não esta conectado ao broker", Toast.LENGTH_SHORT).show();
                        }

                    }
                }).setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "Cancelado", Toast.LENGTH_SHORT).show();
                    }
                }).setTitle("Aviso").setMessage("Atualizar fase do sistema?");
                alert = builder.create();
                alert.show();
            }
        });

        dia.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                dianum = adapterView.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        butSetDia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                builder = new AlertDialog.Builder(MainActivity.this).setTitle("Aviso").setMessage("Atualizar dia da fase?")
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (conectado){
                            try{
                                MqttMessage m = new MqttMessage();
                                m.setRetained(true);
                                m.setPayload(dianum.getBytes());
                                client.publish("Dia", m);
                            }catch (MqttException e){
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "Erro ao enviar dia", Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            Toast.makeText(MainActivity.this, "Não esta conectado ao broker", Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("Não", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(MainActivity.this, "Cancelado", Toast.LENGTH_SHORT).show();
                            }
                        });
                alert = builder.create();
                alert.show();
            }
        });

        umidademaxima.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                valormax.setText(String.valueOf(i)+"%");
                valorIdeal = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        umidademinima.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                valormin.setText(String.valueOf(i)+"%");
                valorMinimo = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        carregatopic = findViewById(R.id.carregatopic);
        SwManual.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    MqttMessage m = new MqttMessage();
                    m.setRetained(true);
                    if (b){
                        Toast.makeText(MainActivity.this, "Atividade Manual ativada",Toast.LENGTH_SHORT).show();
                        m.setPayload(ATIVO.getBytes());

                    }else{
                        Toast.makeText(MainActivity.this, "Atividade manual desativada", Toast.LENGTH_SHORT).show();
                        m.setPayload(DESATIVO.getBytes());
                    }
                    client.publish(topicoM, m);
                } catch (MqttException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });

        SwBomba.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    MqttMessage m = new MqttMessage();
                    m.setRetained(true);
                    if (b){
                        m.setPayload(ATIVO.getBytes());
                    }else{
                        m.setPayload(DESATIVO.getBytes());
                    }
                    client.publish(topicoB, m);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        CriaClienteMQTT();

//        botaofase.setVisibility(View.VISIBLE);
//        Configurabotao();
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
        SwManual.setClickable(false);
        SwBomba.setClickable(false);
        butsetTolerancia.setClickable(false);
        butSetDia.setClickable(false);
        butSetFase.setClickable(false);
        try {
            carregatopic.setVisibility(View.VISIBLE);
            Snackbar.make(tela, R.string.crt_con, Snackbar.LENGTH_SHORT).show();
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //conectou ao broker
//                    Toast.makeText(MainActivity.this, "Conectou", Toast.LENGTH_SHORT).show();
                    Snackbar.make(tela, R.string.BrokerConect, Snackbar.LENGTH_SHORT).show();
                    infos.setVisibility(View.VISIBLE);
                    fab.setVisibility(View.GONE);
                    SwManual.setClickable(true);
                    SwBomba.setClickable(true);
                    butsetTolerancia.setClickable(true);
                    butSetDia.setClickable(true);
                    butSetFase.setClickable(true);
                    carregatopic.setVisibility(View.GONE);
                    setSubcription();
                    conectado = true;
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //algo deu errado
//                    Log.d("onFailure", "onFailure: "+exception.toString());
                    Snackbar.make(tela, R.string.BrokerErr, Snackbar.LENGTH_SHORT).show();
                    fab.setVisibility(View.VISIBLE);
                    SwManual.setClickable(false);
                    SwBomba.setClickable(false);
                    butsetTolerancia.setClickable(false);
                    butSetDia.setClickable(false);
                    butSetFase.setClickable(false);
                    infos.setVisibility(View.GONE);
                    carregatopic.setVisibility(View.GONE);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, ""+e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    //metodo de inscrição de topico mqtt
    private void setSubcription() {
        try {
            client.subscribe(topicoM, 0);
            client.subscribe(topicoB, 0);
            client.subscribe(topicoTemporada, 0);
            client.subscribe(topicoD, 0);

        } catch (MqttException e) {
            e.printStackTrace();
        }
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                Snackbar.make(tela,"Connection lost", BaseTransientBottomBar.LENGTH_SHORT).show();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                String topico = topic, msg = message.toString();
                switch (topico){
                    case "Manual":
                        if (msg.equals("0")){
                            //logica invertida
                            SwManual.setChecked(false);
                        }else{
                            SwManual.setChecked(true);
                        }
                        break;
                    case "Bomba":
                        if (msg.equals("0")){
                            SwBomba.setChecked(false);
                            estadobomba.setText("Bomba Desligada");
                            estadobomba.setTextColor(Color.rgb(255,0,0));
                        }else{
                            SwBomba.setChecked(true);
                            estadobomba.setText("Bomba Ligada");
                            estadobomba.setTextColor(Color.rgb(0,255,0));
                        }
                        break;
                    case "Temporada":
                            fasePomodoro = msg;
                            faseatual.setText(fasePomodoro);
                        break;
                    case "Dia":
                        diaatual.setText(msg);
                        Toast.makeText(MainActivity.this, "Fooooo", Toast.LENGTH_SHORT).show();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Toast.makeText(MainActivity.this, "fui chamado nesse momento", Toast.LENGTH_SHORT).show();
            }
        });
    }

//===============================================================================

    public void EnviaMinimo(){
        builder2 = new AlertDialog.Builder(MainActivity.this)
                .setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MqttMessage m = new MqttMessage();
                        m.setRetained(true);
                        m.setPayload(String.valueOf(valorMinimo).getBytes());
                        try {
                            client.publish(topicoMin, m);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                }).setNegativeButton("Não", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Snackbar.make(tela, "Cancelado", Snackbar.LENGTH_SHORT).show();
                    }
                }).setTitle("Confirmar atualização").setMessage("Atualizar o valor minimo de umidade: "+valorMinimo+"%");
        alert2 = builder2.create();
        alert2.show();
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
