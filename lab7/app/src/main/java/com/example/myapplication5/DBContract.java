package com.example.myapplication5;
import android.provider.BaseColumns;

public final class DBContract {

    private DBContract() {}

    public static class UserEntry implements BaseColumns
    {
        public static final String TBALE_NAME = "user";

        public static final String COLOMN_NAME_KEY_ID = "id";
        public static final String COLOMN_NAME_LOGIN = "login";
        public static final String COLOMN_NAME_PASS = "pass";
    }

}
