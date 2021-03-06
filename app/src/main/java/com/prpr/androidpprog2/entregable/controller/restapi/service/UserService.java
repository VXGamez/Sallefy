package com.prpr.androidpprog2.entregable.controller.restapi.service;

import com.prpr.androidpprog2.entregable.model.Follow;

import com.prpr.androidpprog2.entregable.model.passwordChangeDto;
import com.prpr.androidpprog2.entregable.model.User;
import com.prpr.androidpprog2.entregable.model.UserRegister;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserService {

    @GET("users/{login}")
    Call<User> getUserById(@Path("login") String login);

    @GET("users")
    Call<List<User>> getAllUsers();

    @GET("users?notFollowing=true&popular=true&size=10")
    Call<List<User>> getTopUsers();

    @GET("me/followings")
    Call<List<User>> getFollowedUsers();



    @POST("account")
    Call<ResponseBody> saveAccount(@Body User user);

    @PUT("users")
    Call<User> updateUser(@Body User userDTO);
    @GET("playbacks")


    @PUT("users/{login}/follow")
    Call<Follow> startStopFollowing(@Path("login") String login);

    @GET("users/{login}/follow")
    Call<Follow> checkFollow(@Path("login") String login);

    @GET("users/{login}/followers")
    Call<List<User>> getFollowers(@Path("login") String login);

    @POST("account/change-password")
    Call<ResponseBody> updatePassword(@Body passwordChangeDto pd);
    //?popular=true
    //?sort=id
    @GET("users?popular=true")
    Call<List<User>> getSallefyUsers(@Query(value="page", encoded=true) int page);


}

