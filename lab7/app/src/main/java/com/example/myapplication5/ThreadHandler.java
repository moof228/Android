package com.example.myapplication5;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;

public class ThreadHandler {

    Handler t_handler;
    final Message message = Message.obtain();
    Context context;

    ThreadHandler(Handler handler, Context context){
        this.t_handler = handler;
        this.context = context;
    }

    DatabaseHandler db;

    public void getAllUser(){
        new Thread(() -> {
            Log.i("Thread", Thread.currentThread().getName());
            db = new DatabaseHandler(context);
            List<User> userList = db.getAllUsers();
            message.sendingUid = 1;
            message.obj = userList;
            t_handler.sendMessage(message);
        }).start();
    }
    public void userAdd(String log, String pass){
        new Thread(() -> {
            Log.i("Thread", Thread.currentThread().getName());
            db = new DatabaseHandler(context);
            String messageAdd = db.addUser(new User(log, pass));
            message.sendingUid = 2;
            message.obj = messageAdd;
            t_handler.sendMessage(message);
        }).start();
    }
    public void userFind(User user){
        new Thread(() -> {
            Log.i("Thread", Thread.currentThread().getName());
            db = new DatabaseHandler(context);

            boolean status = db.findUser(user);
            message.sendingUid = 3;
            message.obj = status;
            t_handler.sendMessage(message);
        }).start();
    }
    public void resetPass(User user, String pass){
        new Thread(() -> {
            Log.i("Thread", Thread.currentThread().getName());
            db = new DatabaseHandler(context);
            String messageUpd = db.resetPassword(user, pass);
            message.sendingUid = 4;
            message.obj = messageUpd;
            t_handler.sendMessage(message);
        }).start();
    }
    public void delByName(String log){
        new Thread(() -> {
            Log.i("Thread", Thread.currentThread().getName());
            db = new DatabaseHandler(context);
            String messageUpd = db.deleteByName(log);
            message.sendingUid = 5;
            message.obj = messageUpd;
            t_handler.sendMessage(message);
        }).start();
    }
}
