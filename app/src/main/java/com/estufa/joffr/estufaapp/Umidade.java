package com.estufa.joffr.estufaapp;

/**
 * Created by joffr on 23/04/2018.
 */

public class Umidade {

    private long id;
    private float valor;
    private String tempocriado;

    public Umidade() {
    }

    public Umidade(long id, float valor, String tempocriado) {
        this.id = 0;
        this.valor = valor;
        this.tempocriado = tempocriado;
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

    public String getTempocriado() {
        return tempocriado;
    }

    public void setTempocriado(String tempocriado) {
        this.tempocriado = tempocriado;
    }

    @Override
    public String toString() {
        return "Umidade{" +
                "id=" + id +
                ", valor=" + valor +
                ", tempocriado='" + tempocriado + '\'' +
                '}';
    }
}
