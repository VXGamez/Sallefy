package com.prpr.androidpprog2.entregable.controller.activities;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//import com.google.firebase.storage.UploadTask;
import com.downloader.PRDownloader;
import com.downloader.PRDownloaderConfig;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.dialogs.ErrorDialog;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.AccountCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.UserCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.AccountManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.CloudinaryManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.UserManager;
import com.prpr.androidpprog2.entregable.model.DB.ObjectBox;
import com.prpr.androidpprog2.entregable.model.DB.SavedCache;
import com.prpr.androidpprog2.entregable.model.DB.UtilFunctions;
import com.prpr.androidpprog2.entregable.model.Follow;
import com.prpr.androidpprog2.entregable.model.User;
import com.prpr.androidpprog2.entregable.model.UserToken;
import com.prpr.androidpprog2.entregable.model.passwordChangeDto;
import com.prpr.androidpprog2.entregable.utils.Session;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.objectbox.android.AndroidObjectBrowser;

public class LoginActivity extends AppCompatActivity implements AccountCallback {

    private EditText etLogin;
    private EditText etPassword;
    private Button btnLogin;
    private CheckBox btnRemember;
    private TextView tvToRegister;
    private UserToken usTkn;
    private boolean d1=true;
    private String username="";
    private LinearLayout loginLayout;
    private LinearLayout loadingLayout;
    private StorageReference mStorage;
    private Uri data;

    @Override
    public void onCreate(Bundle savedInstanceSate) {

        super.onCreate(savedInstanceSate);
        setContentView(R.layout.activity_login);

        //----------------------------------------- NO TOCAR -- BASE DE DADES TESTING -----------------------------------------
        PRDownloaderConfig config = PRDownloaderConfig.newBuilder().build();
        PRDownloader.initialize(getApplicationContext(), config);
        ObjectBox.init(this);
        //if (BuildConfig.DEBUG) {
        //boolean started = new AndroidObjectBrowser(ObjectBox.get()).start(this);
        //Log.i("ObjectBrowser", "Started: " + started);
        //}
        //---------------------------------------------------------------------------------------------------------------------
        Intent intent = getIntent();
        data = intent.getData();

        initViews();

    }

    private void initViews () {
        loginLayout = findViewById(R.id.LoginView);
        loadingLayout = findViewById(R.id.loadingScreen);

        loadingLayout.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.GONE);

        etLogin = (EditText) findViewById(R.id.login_user);
        etPassword = (EditText) findViewById(R.id.login_password);
        tvToRegister = (TextView) findViewById(R.id.register_btn_action);
        tvToRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
            }
        });

        btnRemember = (CheckBox) findViewById(R.id.checkBox);

        final SharedPreferences prefs = getSharedPreferences("RememberMe", Context.MODE_PRIVATE);

        String nickname = prefs.getString("nickname", "");
        String pass = prefs.getString("password", "");
        boolean stateSwitch = prefs.getBoolean("stateSwitch", false);
        btnRemember.setChecked(stateSwitch);
        etLogin.setText(nickname);
        etPassword.setText(pass);



        if(btnRemember.isChecked()){
           doLogin(etLogin.getText().toString(),etPassword.getText().toString());
        }else{
            loadingLayout.setVisibility(View.GONE);
            loginLayout.setVisibility(View.VISIBLE);
        }

        btnLogin = (Button) findViewById(R.id.login_btn_action);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d1 = false;
                if(btnRemember.isChecked()){

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("nickname", etLogin.getText().toString());
                    editor.putString("password", etPassword.getText().toString());
                    editor.putBoolean("stateSwitch", btnRemember.isChecked());
                    editor.commit();


                } else {

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("nickname","");
                    editor.putString("password", "");
                    editor.putBoolean("stateSwitch", btnRemember.isChecked());
                    editor.commit();

                }

                doLogin(etLogin.getText().toString(), etPassword.getText().toString());
            }
        });
        btnLogin.setEnabled(true);
    }

    private void doLogin(String username, String userpassword) {
        SavedCache c;
        if(!UtilFunctions.hasCache()){
            c = new SavedCache();
            c.setId(1);
        }else{
            c = ObjectBox.get().boxFor(SavedCache.class).get(1);
            c.setPassword(userpassword);
            c.setUsername(username);
        }
        ObjectBox.get().boxFor(SavedCache.class).put(c);

        this.username = username;
        if(UtilFunctions.noInternet(getApplicationContext())){
            onLoginSuccess(new UserToken(ObjectBox.get().boxFor(SavedCache.class).get(1).oldToken));
        }else{
            AccountManager.getInstance(getApplicationContext()).loginAttempt(username, userpassword, LoginActivity.this);
        }
    }

    @Override
    public void onLoginSuccess(UserToken userToken) {

        usTkn = userToken;
        Session.getInstance(getApplicationContext()).setUserToken(usTkn);

        if(UtilFunctions.noInternet(getApplicationContext())){
            onUserInfoReceived(ObjectBox.get().boxFor(SavedCache.class).get(1).retrieveUser());
        }else{
            SavedCache c =  ObjectBox.get().boxFor(SavedCache.class).get(1);
            c.setOldToken(userToken.getIdToken());

            ObjectBox.get().boxFor(SavedCache.class).put(c);
            mStorage = FirebaseStorage.getInstance().getReference();
            StorageReference filePath = mStorage.child(Session.changeLogin(etLogin.getText().toString()));
            UserManager.getInstance(getApplicationContext()).getUserData(username, LoginActivity.this, userToken);
        }
    }

    @Override
    public void onLoginFailure(Throwable throwable) {
        String message = "";
        if(throwable.getMessage().contains("401")){
            message = "Wrong Credentials";
        }else if(throwable.getMessage().contains("403")){
            message = "Wrong Credentials";
        }else if(throwable.getMessage().contains("404")){
            message = "User doesn't Exist";
        }else{
            message = "Unkown Error";
        }
        ErrorDialog.getInstance(this).showErrorDialog(message);
    }

    @Override
    public void onRegisterSuccess() {

    }

    @Override
    public void onRegisterFailure(Throwable throwable) {

    }

    @Override
    public void onUserInfoReceived(User userData) {
        if(!UtilFunctions.noInternet(getApplicationContext())){
           SavedCache c=  ObjectBox.get().boxFor(SavedCache.class).get(1);
           c.saveUser(userData);

           ObjectBox.get().boxFor(SavedCache.class).put(c);
        }
        Session.getInstance(getApplicationContext()).setUser(userData);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("sameUser", d1);
        if(data!=null){
            intent.putExtra("url", data.toString());
        }
        intent.putExtra("UserInfo", userData);
        startActivity(intent);
    }

    @Override
    public void onAccountSaved(User body) {

    }

    @Override
    public void onAccountSavedFailure(Throwable throwable) {

    }

    @Override
    public void onFailure(Throwable t) {

    }


}

