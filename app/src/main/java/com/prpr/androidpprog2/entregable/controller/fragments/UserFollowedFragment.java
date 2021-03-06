package com.prpr.androidpprog2.entregable.controller.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.activities.InfoArtistaActivity;
import com.prpr.androidpprog2.entregable.controller.adapters.UserFollowedAdapter;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.UserCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.UserManager;
import com.prpr.androidpprog2.entregable.model.DB.ObjectBox;
import com.prpr.androidpprog2.entregable.model.DB.SavedCache;
import com.prpr.androidpprog2.entregable.model.DB.UtilFunctions;
import com.prpr.androidpprog2.entregable.model.Follow;
import com.prpr.androidpprog2.entregable.model.User;
import com.prpr.androidpprog2.entregable.model.UserToken;
import com.prpr.androidpprog2.entregable.model.passwordChangeDto;
import com.prpr.androidpprog2.entregable.utils.Constants;

import java.util.ArrayList;
import java.util.List;


public class UserFollowedFragment extends Fragment implements UserCallback {

    private ArrayList<User> followedUsers;

    private EditText etSearchFollowed;

    private UserManager userManager;
    private RecyclerView mRecyclerView;

    private FloatingActionButton btnSettings;


    private FloatingActionButton btnSettingsPlaylists;

    public UserFollowedFragment() {
        // Required empty public constructor
        this.followedUsers = new ArrayList<>();
    }
    @Override
    public void onResume() {
        super.onResume();
        userManager.getFollowedUsers(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_user_followed, container, false);

        /*btnSettingsPlaylists = (FloatingActionButton) view.findViewById(R.id.configUsersFollowedButton);
        btnSettingsPlaylists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });*/

        etSearchFollowed = (EditText) view.findViewById(R.id.search_user_users_followed);
        etSearchFollowed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                filter(editable.toString());
            }
        });

        btnSettings = (FloatingActionButton)getActivity().findViewById(R.id.configButton);


        mRecyclerView = (RecyclerView) view.findViewById(R.id.usersFollowedRecyclerview);
        LinearLayoutManager manager = new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
        UserFollowedAdapter adapter = new UserFollowedAdapter(getContext(), null);
        adapter.setUserCallback(this);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && btnSettings.getVisibility() == View.VISIBLE) {
                    btnSettings.hide();
                } else if (dy < 0 && btnSettings.getVisibility() != View.VISIBLE) {
                    btnSettings.show();
                }
            }
        });

        userManager = new UserManager(getContext());
        userManager.getFollowedUsers(this);


        return view;
    }

    private void filter(String text){
        ArrayList<User> filteredUsers = new ArrayList<>();

        for(User u : followedUsers){
            if(u.getLogin().toLowerCase().contains(text.toLowerCase())){
                filteredUsers.add(u);
            }
        }
        mRecyclerView.setAdapter(new UserFollowedAdapter(getContext(), filteredUsers));
    }



    @Override
    public void onUserInfoReceived(User userData) {

    }

    @Override
    public void onUserUpdated(User body) {

    }

    @Override
    public void onAccountSaved(User body) {

    }


    @Override
    public void onTopUsersRecieved(List<User> body) {

    }

    @Override
    public void onUserUpdateFailure(Throwable throwable) {

    }

    @Override
    public void onUserSelected(User user) {
        Intent intent = new Intent(getContext(), InfoArtistaActivity.class);
        intent.putExtra("User", user);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
    }

    @Override
    public void onAllUsersSuccess(List<User> users) {

    }

    @Override
    public void onFollowedUsersSuccess(List<User> users) {
        this.followedUsers = (ArrayList) users;
        UserFollowedAdapter userFollowedAdapter = new UserFollowedAdapter(getContext(), this.followedUsers);
        userFollowedAdapter.setUserCallback(this);
        mRecyclerView.setAdapter(userFollowedAdapter);
    }


    @Override
    public void onAllUsersFail(Throwable throwable) {

    }

    @Override
    public void onFollowedUsersFail(Throwable throwable) {

    }

    @Override
    public void onFollowSuccess(Follow body) {

    }

    @Override
    public void onAccountSavedFailure(Throwable throwable) {

    }

    @Override
    public void onFollowFailure(Throwable throwable) {

    }

    @Override
    public void onCheckSuccess(Follow body) {

    }

    @Override
    public void onCheckFailure(Throwable throwable) {

    }

    @Override
    public void onTopUsersFailure(Throwable throwable) {

    }

    @Override
    public void onFollowedUsersFailure(Throwable t) {
        if(UtilFunctions.noInternet(getActivity().getApplicationContext())){
            onFollowedUsersSuccess(ObjectBox.get().boxFor(SavedCache.class).get(1).retrieveFollowedUsers());
        }
    }

    @Override
    public void onFollowersRecieved(ArrayList<User> body) {

    }

    @Override
    public void onFollowersFailed(Throwable throwable) {

    }

    @Override
    public void onFollowersFailure(Throwable throwable) {

    }

    @Override
    public void onPasswordUpdated(passwordChangeDto pd) {


    }

    @Override
    public void onPasswordUpdatedFailure(Throwable throwable) {

    }

    @Override
    public void onSallefySectionRecieved(List<User> body, boolean recieved) {

    }

    @Override
    public void onSallefySectionFailure(Throwable throwable) {

    }

    @Override
    public void onFailure(Throwable throwable) {

    }

}
