package com.prpr.androidpprog2.entregable.controller.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.provisioning.Account;
import com.cloudinary.utils.ObjectUtils;
//import com.google.android.gms.tasks.OnSuccessListener;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//import com.google.firebase.storage.UploadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.AccountCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.UserCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.AccountManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.CloudinaryManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.UserManager;
import com.prpr.androidpprog2.entregable.model.Follow;
import com.prpr.androidpprog2.entregable.model.User;
import com.prpr.androidpprog2.entregable.model.UserRegister;
import com.prpr.androidpprog2.entregable.model.UserToken;
import com.prpr.androidpprog2.entregable.model.passwordChangeDto;
import com.prpr.androidpprog2.entregable.utils.CloudinaryConfigs;
import com.prpr.androidpprog2.entregable.utils.Session;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class RegisterActivity extends AppCompatActivity implements AccountCallback {

    private EditText etEmail;
    private EditText etLogin;
    private EditText etPassword;
    private Button btnRegister;
    private Button btnBack;
    private StorageReference mStorage;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initViews();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_up,R.anim.slide_down);
    }

    private void initViews () {
        etEmail = (EditText) findViewById(R.id.register_email);
        etLogin = (EditText) findViewById(R.id.register_login);
        etPassword = (EditText) findViewById(R.id.register_password);

        btnRegister = (Button) findViewById(R.id.register_btn_action);
        btnRegister.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String login = etLogin.getText().toString();
                String password = etPassword.getText().toString();
                String email = etEmail.getText().toString();
                Session.getInstance(getApplicationContext()).setUserRegister(new UserRegister(email, login, password));
                AccountManager.getInstance(getApplicationContext()).registerAttempt(email, login, password, RegisterActivity.this);
            }
        });


        btnBack = (Button) findViewById(R.id.back2login);
        btnBack.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    finishAfterTransition();
                } else finish();
            }
        });
    }

    private void doLogin(String username, String userpassword) {
        AccountManager.getInstance(getApplicationContext())
                .loginAttempt(username, userpassword, RegisterActivity.this);
    }

    @Override
    public void onLoginSuccess(UserToken userToken) {
        Session.getInstance(getApplicationContext())
                .setUserToken(userToken);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public void onLoginFailure(Throwable throwable) {
        Session.getInstance(getApplicationContext())
                .setUserRegister(null);
    }

    @Override
    public void onRegisterSuccess() {

        mStorage = FirebaseStorage.getInstance().getReference();
        StorageReference filePath = mStorage.child(Session.changeLogin(etLogin.getText().toString()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAfterTransition();
        }else finish();
    }

    @Override
    public void onRegisterFailure(Throwable throwable) {
        Session.getInstance(getApplicationContext())
                .setUserRegister(null);
        Toast.makeText(getApplicationContext(), "Register failed " + throwable.getMessage(), Toast.LENGTH_LONG).show();
    }


    @Override
    public void onUserInfoReceived(User userData) {

    }

    @Override
    public void onAccountSaved(User body) {

    }

    @Override
    public void onAccountSavedFailure(Throwable throwable) {

    }


    @Override
    public void onFailure(Throwable throwable) {

    }
}
