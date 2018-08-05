package com.estufa.joffr.estufaapp;

/**
 * Created by joffr on 23/04/2018.
 */

public class Umidade {

    private long id;
    private float valor;
    private String dataRegistro, horaRegistro;

    public Umidade() {
    }

    public Umidade(long id, float valor, String dataRegistro, String horaRegistro) {
        this.id = 0;
        this.valor = valor;
        this.dataRegistro = dataRegistro;
        this.horaRegistro = horaRegistro;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public float getValor() {
        return valor;
    }

    public void setValor(float valor) {
        this.valor = valor;
    }

    public String getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(String dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public String getHoraRegistro() {
        return horaRegistro;
    }

    public void setHoraRegistro(String horaRegistro) {
        this.horaRegistro = horaRegistro;
    }

    @Override
    public String toString() {
        return "Umidade{" +
                "id=" + id +
                ", valor=" + valor +
                ", dataRegistro='" + dataRegistro + '\'' +
                ", horaRegistro='" + horaRegistro + '\'' +
                '}';
    }
}
