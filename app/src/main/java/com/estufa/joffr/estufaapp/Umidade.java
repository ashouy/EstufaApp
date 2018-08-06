package com.estufa.joffr.estufaapp;

import com.orm.SugarRecord;

/**
 * Created by joffr on 23/04/2018.
 */

public class Umidade extends SugarRecord{

    private float valor;
    private String data, hora;

    public Umidade() {
    }

    public Umidade(float valor, String data, String hora) {
        this.valor = valor;
        this.data = data;
        this.hora = hora;
    }


    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHora() {
        return hora;
    }

    public void setHora(String hora) {
        this.hora = hora;
    }

    @Override
    public String toString() {
        return "Umidade{" +
                "id= "+ getId()+
                ", valor=" + valor +
                ", data='" + data + '\'' +
                ", hora='" + hora + '\'' +
                '}';
    }


}
