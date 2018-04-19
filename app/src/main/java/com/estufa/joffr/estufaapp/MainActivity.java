package com.estufa.joffr.estufaapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    WifiManager wifi;

    static String MQTTHOST = "tcp://192.168.50.1:1883";
    static String USERNAME = "JoffrMQTT";
    static String SENHA = "mosquito";

    String topicoU = "Umidade"; //topicos usados nessa aplicação
    boolean conectado = false; //flag para conexao com o broker
    int xU = 0;

    MqttAndroidClient client;
    MqttConnectOptions options;

    GraphView grafi;
    TextView tvh;
    View tela;

    LineGraphSeries<DataPoint> series;

    private String TAG = MainActivity.class.getSimpleName();

    private ProgressDialog pDialog;
    private ListView lv;
    //private static String url = "http://192.168.0.11/api-rest-php/view/Conteudo/listar.php";
    private static String url = "http://192.168.50.1/api-rest-php/view/Conteudo/listar.php";

    ArrayList<HashMap<String, String>> dadosList;

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
                        Snackbar.make(tela, "O wifi está desligado", Snackbar.LENGTH_SHORT).show();
                    }
                } else {
                    Snackbar.make(tela, "Você já esta conectado", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        FloatingActionButton fabjson = findViewById(R.id.fabjson);
        fabjson.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dadosList = new ArrayList<>();
                //lv = findViewById(R.id.list);
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
                new GetContacts().execute();
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX//
            }
        });

        tvh = findViewById(R.id.tvH);
        tela = findViewById(R.id.tela);

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        SetGraficos();
        CriaClienteMQTT();

    }

//=======================CONSUMO JSON=========================================
    private class GetContacts extends AsyncTask<Void, Void, Void> {

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Showing progress dialog
        pDialog = new ProgressDialog(MainActivity.this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();

    }

    @Override
    protected Void doInBackground(Void... arg0) {
        HttpHandler sh = new HttpHandler();

        // Making a request to url and getting response
        String jsonStr = sh.makeServiceCall(url);

        Log.e(TAG, "Response from url: " + jsonStr);

        if (jsonStr != null) {
            try {
                JSONObject jsonObj = new JSONObject(jsonStr);

                // Getting JSON Array node
                JSONArray contacts = jsonObj.getJSONArray("dados");
                // looping through All Contacts
                for (int i = 0; i < contacts.length(); i++) {
                    JSONObject c = contacts.getJSONObject(i);

                    String id = c.getString("id");
                    String valor = c.getString("valorHum");
                    String data = c.getString("criado");


                    // tmp hash map for single contact
                    HashMap<String, String> dado = new HashMap<>();

                    // adding each child node to HashMap key => value
                    dado.put("id", id);
                    dado.put("valorHum", valor);
                    dado.put("criado", data);
                    // adding contact to contact list
                    dadosList.add(dado);
                }
            } catch (final JSONException e) {
                Log.e(TAG, "Json parsing error: " + e.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),
                                "Json parsing error: " + e.getMessage(),
                                Toast.LENGTH_LONG)
                                .show();
                    }
                });

            }
        } else {
            Log.e(TAG, "Couldn't get json from server.");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Couldn't get json from server. Check LogCat for possible errors!",
                            Toast.LENGTH_LONG)
                            .show();
                }
            });

        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        // Dismiss the progress dialog
        if (pDialog.isShowing())
            pDialog.dismiss();
        /**
         * Updating parsed JSON data into ListView
         * */
        /*
        ListAdapter adapter = new SimpleAdapter(
                MainActivity.this, contactList,
                R.layout.list_item, new String[]{"id", "valorHum",
                "criado"}, new int[]{R.id.id,
                R.id.valor, R.id.data});

        lv.setAdapter(adapter);
        */
    }

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
                String humPayload;
                if (topic.equals(topicoU)) {
                    humPayload = new String(message.getPayload());
                    float y = Float.valueOf(humPayload);
                    tvh.setText(humPayload);
                    xU++;
                    //aqui tem a mesma situação do de cima porem eh para um topico diferente
                    series.appendData(new DataPoint(xU, y), true, 40);
                    series.setColor(Color.rgb(0, 188, 212));
                    series.setBackgroundColor(Color.argb(50, 79, 195, 247));
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    private void ConectaMQTT() {
        try {
            Snackbar.make(tela, "Criando conexão", Snackbar.LENGTH_SHORT).show();
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //conectou
//                    Log.d("teste", "onSucess");
//                    Toast.makeText(MainActivity.this, "Conectou", Toast.LENGTH_SHORT).show();
                    Snackbar.make(tela, R.string.BrokerConect, Snackbar.LENGTH_SHORT).show();

//                    setSubcription();
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

    private void setSubcription() {
        try {
            client.subscribe(topicoU, 0);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

//===============================================================================

    public void SetGraficos() {
        //GRAFICO 1
        grafi = (GraphView) findViewById(R.id.graf);
        //series = new LineGraphSeries<>(pontos);
        series = new LineGraphSeries<>();
        series.setDrawBackground(true);
        series.setDrawDataPoints(true);
        grafi.addSeries(series);
        grafi.getViewport().setScrollable(true);
        // set manual X bounds
        grafi.getViewport().setXAxisBoundsManual(true);
        grafi.getViewport().setMinX(1);
        grafi.getViewport().setMaxX(20);
        // set manual Y bounds
        grafi.getViewport().setYAxisBoundsManual(true);
        grafi.getViewport().setMinY(24);
        grafi.getViewport().setMaxY(32);
        grafi.setTitle("Temperatura");
        //listener do ponto
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(MainActivity.this, "Você clicou no ponto: " + dataPoint, Toast.LENGTH_SHORT).show();
            }
        });
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
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
