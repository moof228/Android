package com.example.myapplication5;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "User.db";

    public DatabaseHandler(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_USER_TABLE = "CREATE TABLE " + DBContract.UserEntry.TBALE_NAME + "(" +
                DBContract.UserEntry.COLOMN_NAME_KEY_ID + " INTEGER PRIMARY KEY," +
                DBContract.UserEntry.COLOMN_NAME_LOGIN + " TEXT, " +
                DBContract.UserEntry.COLOMN_NAME_PASS + " TEXT " + ")";

        db.execSQL(CREATE_USER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DBContract.UserEntry.TBALE_NAME);
        onCreate(db);
    }

    public String addUser(User user)
    {
        String message = "null";
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues value = new ContentValues();
        value.put(DBContract.UserEntry.COLOMN_NAME_LOGIN, user.getLogin());
        value.put(DBContract.UserEntry.COLOMN_NAME_PASS, user.getPass());

        db.insert(DBContract.UserEntry.TBALE_NAME, null, value);
        String newUser = getUser(user.getLogin());
        if(newUser != null){
            message = "Пользователь добавлен";
        }
        db.close();
        return message;
    }

    public List<User> getAllUsers()
    {
        List<User> usersList = new ArrayList<User>();
        String selectQuery = "SELECT  * FROM " + DBContract.UserEntry.TBALE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()) {
            do {
                User user = new User();
                user.setID(Integer.parseInt(cursor.getString(0)));
                user.setLogin(cursor.getString(1));
                user.setPass(cursor.getString(2));
                usersList.add(user);
            }while(cursor.moveToNext());
        }
        return usersList;
    }

    public boolean findUser(User userFinding)
    {
        String selectQuery = "SELECT  * FROM " + DBContract.UserEntry.TBALE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()) {
            do {
                if(cursor.getString(1).equals(userFinding.getLogin())
                && cursor.getString(2).equals(userFinding.getPass()))
                    return true;

            }while(cursor.moveToNext());
        }
        return false;
    }
    private String getUser(String login){

        String selectQuery = "SELECT * FROM " + DBContract.UserEntry.TBALE_NAME + " WHERE " + DBContract.UserEntry.COLOMN_NAME_LOGIN + " =?";
        String userLog = null;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{login});

        if(cursor.getCount() != 0){
            if (cursor.moveToFirst()) {
                do {
                    User user = new User();
                    user.setLogin(cursor.getString(1));
                    user.setPass(cursor.getString(2));
                    userLog = user.getLogin() + " " + user.getPass();
                } while (cursor.moveToNext());
            }
        }
        else{
            userLog = "null";
        }
        cursor.close();
        return userLog;
    }

    public String deleteByName(String name)
    {
        String message = "null";
        String nameUser;
        String selectQuery = "SELECT  * FROM " + DBContract.UserEntry.TBALE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        int i = 1;
        if(cursor.moveToFirst()) {
            do {
                if(cursor.getString(1).equals(name))
                {
                    db.delete(DBContract.UserEntry.TBALE_NAME,DBContract.UserEntry.COLOMN_NAME_KEY_ID +"="+ i, null);
                }

                ++i;
            }while(cursor.moveToNext());
        }
        nameUser = getUser(name);
        if(nameUser.equals("null")) {
            message = "Пользователь удален";
        }else{
            message = "Ошибка!";
        }

        return message;
    }



    public String resetPassword(User user, String newPassword)
    {
        String message = "null";
        String nameUser;

        String selectQuery = "SELECT  * FROM " + DBContract.UserEntry.TBALE_NAME;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        int i = 1;
        if(cursor.moveToFirst()) {
            do {
                if(cursor.getString(1).equals(user.getLogin())
                        && cursor.getString(2).equals(user.getPass()))
                {
                    ContentValues value = new ContentValues();
                    value.put(DBContract.UserEntry.COLOMN_NAME_LOGIN, user.getLogin());
                    value.put(DBContract.UserEntry.COLOMN_NAME_PASS, newPassword);

                    db.update(DBContract.UserEntry.TBALE_NAME, value, DBContract.UserEntry.COLOMN_NAME_KEY_ID +"="+ i, null);
                }

            ++i;
            }while(cursor.moveToNext());
        }
        nameUser = getUser(user.getLogin());
        if(!nameUser.equals("null")){
            String[] cmpData = nameUser.split(" ");
            if (cmpData[1].equals(newPassword)){
                message = "Пароль изменен";
            }else{
                message = "Пароль не изменен";
            }
        }

        return message;
    }

}
