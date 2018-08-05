package com.estufa.joffr.estufaapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joffr on 23/04/2018.
 */

public class Banco extends SQLiteOpenHelper{

    private static final String FLOAT_TYPE = " FLOAT";
    //private static final String TIMESTAMP = " TIMESTAMP";
    private static final String DATE = " DATE";
    private static final String TIME = " TIME";
    private static final String VIRGULA = ",";

    private static final String DATABASE = "estufa.sqlite";
    private static final int VERSION = 1;

    private static final String SQL_CREATE_TABLE =
            ("CREATE TABLE " + Entry.TABLE_NAME + "(" +
                    Entry._ID + " INTEGER" + " PRIMARY KEY ,"+
                    Entry.VALOR + FLOAT_TYPE + "," +
                    Entry.DATA_REGISTRO + DATE + "," +
                    Entry.HORA_REGISTRO + TIME + ");");

    private static final String SQL_DROP_TABLE = ("DROP TABLE " + Entry.TABLE_NAME + ";");

    public Banco(Context context) {
        super(context, DATABASE, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int VERSAOANTIGA, int NOVAVERSAO) {}

    public long save(Umidade u){
        long id = u.getId();
        SQLiteDatabase db = getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(Entry.VALOR, u.getValor());
            values.put(Entry.DATA_REGISTRO, u.getDataRegistro());
            values.put(Entry.HORA_REGISTRO, u.getHoraRegistro());

            if (id != 0){
                String selection = Entry._ID + "= ?";
                String[] whereArgs = new String[]{String.valueOf(id)};

                int count = db.update(Entry.TABLE_NAME, values, selection, whereArgs);

                return count;
            } else {
                id = db.insert(Entry.TABLE_NAME, null, values);
                return id;
            }
        }finally {
            db.close();
        }
    }

    public List<Umidade> findAll(){
        SQLiteDatabase db = getReadableDatabase();
        try {
            Cursor c = db.query(Entry.TABLE_NAME, null,null,null,null,null,null);
            return toList(c);
        }finally {

        }
    }

    private List<Umidade> toList(Cursor c){
        List<Umidade> dados = new ArrayList<>();
        if (c.moveToFirst()){
            do{
                Umidade umidade = new Umidade();
                dados.add(umidade);

                umidade.setId(c.getInt(c.getColumnIndex(Entry._ID)));
                umidade.setValor(c.getFloat(c.getColumnIndex(Entry.VALOR)));
                umidade.setDataRegistro(c.getString(c.getColumnIndex(Entry.DATA_REGISTRO)));
                umidade.setHoraRegistro(c.getString(c.getColumnIndex(Entry.HORA_REGISTRO)));

            }while (c.moveToNext());
        }
        return dados;
    }

    public void execSQL(String sql){
        SQLiteDatabase db = getWritableDatabase();
        try{
            db.execSQL(sql);
        }finally {
            db.close();
        }
    }

    public void execSQL(String sql, Object[] args){
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.execSQL(sql, args);
        }finally {
            db.close();
        }
    }

    public static class Entry implements BaseColumns{
        public static final String TABLE_NAME = "umidades",
                                    VALOR = "valor",
                                    DATA_REGISTRO = "data_registro",
                                    HORA_REGISTRO = "hora_registro";
    }
}
