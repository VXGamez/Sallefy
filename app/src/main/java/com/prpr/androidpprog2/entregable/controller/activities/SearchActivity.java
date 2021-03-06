package com.prpr.androidpprog2.entregable.controller.activities;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.adapters.GenereAdapter;
import com.prpr.androidpprog2.entregable.controller.adapters.PlaylistAdapter;
import com.prpr.androidpprog2.entregable.controller.adapters.TrackListAdapter;
import com.prpr.androidpprog2.entregable.controller.adapters.UserAdapter;
import com.prpr.androidpprog2.entregable.controller.callbacks.TrackListCallback;
import com.prpr.androidpprog2.entregable.controller.dialogs.ErrorDialog;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.GenreCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.PlaylistCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.SearchCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.UserCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.GenreManager;
import com.prpr.androidpprog2.entregable.controller.music.ReproductorService;
import com.prpr.androidpprog2.entregable.model.DB.ObjectBox;
import com.prpr.androidpprog2.entregable.model.DB.SavedCache;
import com.prpr.androidpprog2.entregable.model.DB.UtilFunctions;
import com.prpr.androidpprog2.entregable.model.Follow;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.SearchManager;
import com.prpr.androidpprog2.entregable.model.Genre;
import com.prpr.androidpprog2.entregable.model.Playlist;
import com.prpr.androidpprog2.entregable.model.Track;
import com.prpr.androidpprog2.entregable.model.User;
import com.prpr.androidpprog2.entregable.model.UserToken;
import com.prpr.androidpprog2.entregable.model.passwordChangeDto;
import com.prpr.androidpprog2.entregable.utils.Constants;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements  TrackListCallback, PlaylistCallback, GenreCallback, SearchCallback, UserCallback{

    //Llista de songs i playists
    private RecyclerView mRecyclerViewTracks;
    private RecyclerView mRecyclerViewPlaylist;
    private RecyclerView mRecyclerViewUser;

    //Generes
    private RecyclerView getmRecyclerViewGeneres;
    private ArrayList<Genre> mGeneres;
    private Playlist mPlaylistDeGenere;
    private ArrayList<Track> searchedTracks;

    //Cerca
    private EditText mSearchText;

    //Usuari
    private User user;

    //Possibles layouts en la cerca
    private LinearLayout mPlaylistLayout;
    private LinearLayout mTracksLayout;
    private LinearLayout mUsersLayout;
    private LinearLayout mGeneresLayout;
    private ScrollView mBothLayout;

    //----------------------------------------------------------------PART DE SERVICE--------------------------------------------------------------------------------
    private TextView trackTitle;
    private TextView followingTxt;
    private TextView trackAuthor;
    private SeekBar mSeekBar;
    private Button play;
    private Button pause;
    private ImageView im;
    private LinearLayout playing;
    private ReproductorService serv;
    private boolean servidorVinculat=false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ReproductorService.LocalBinder binder = (ReproductorService.LocalBinder) service;
            serv = binder.getService();
            servidorVinculat = true;
            serv.setUIControls(mSeekBar, trackTitle, trackAuthor, play, pause, im);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            servidorVinculat = false;
        }
    };

    void doUnbindService() {
        if (servidorVinculat) {
            unbindService(serviceConnection);
            servidorVinculat = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(!servidorVinculat){
            Intent intent = new Intent(this, ReproductorService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }else{
            serv.setUIControls(mSeekBar, trackTitle, trackAuthor, play, pause, im);
            serv.updateUI();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(servidorVinculat){

        }
    }




    //----------------------------------------------------------------FIN DE LA PART DE SERVICE--------------------------------------------------------------------------------
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        initViews();
    }

    @SuppressLint("ClickableViewAccessibility")
    void initViews(){

        play = findViewById(R.id.playButton);
        play.setEnabled(true);
        play.bringToFront();
        play.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                serv.resumeMedia();
            }
        });

        pause = findViewById(R.id.playPause);
        pause.setEnabled(true);
        pause.bringToFront();
        pause.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                serv.pauseMedia();
            }
        });

        trackAuthor = findViewById(R.id.dynamic_artist);
        trackAuthor.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        trackAuthor.setSelected(true);
        trackAuthor.setSingleLine(true);
        trackTitle = findViewById(R.id.dynamic_title);
        trackTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        trackTitle.setSelected(true);
        trackTitle.setSingleLine(true);
        mSeekBar = (SeekBar) findViewById(R.id.dynamic_seekBar);

        playing = findViewById(R.id.reproductor);
        playing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(serv!=null && !trackTitle.getText().toString().equals("")){
                    Intent intent = new Intent(getApplicationContext(), ReproductorActivity.class);
                    startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
                    overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
                }else{
                    ErrorDialog.getInstance(SearchActivity.this).showErrorDialog("You haven't selected a song yet!");
                }
            }
        });

        //No mostrem res
        mGeneresLayout = (LinearLayout) findViewById(R.id.search_genere_layout);
        mGeneresLayout.setVisibility(View.GONE);

        mPlaylistLayout = (LinearLayout) findViewById(R.id.search_recyclerView_playlist);

        mTracksLayout = (LinearLayout) findViewById(R.id.search_recyclerView_song);

        mUsersLayout = (LinearLayout) findViewById(R.id.search_recyclerView_user);

        mBothLayout = (ScrollView) findViewById(R.id.search_scroll_all);
        mBothLayout.setVisibility(View.INVISIBLE);

        mGeneres = new ArrayList<>();
        GenreManager.getInstance(this).getAllGenres(this);

        mRecyclerViewPlaylist = (RecyclerView) findViewById(R.id.search_dynamic_recyclerView_playlist);
        LinearLayoutManager managerPlaylist = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        PlaylistAdapter adapterPlaylist = new PlaylistAdapter(this, null);
        adapterPlaylist.setPlaylistCallback(this);
        mRecyclerViewPlaylist.setLayoutManager(managerPlaylist);
        mRecyclerViewPlaylist.setAdapter(adapterPlaylist);

        mRecyclerViewTracks = (RecyclerView) findViewById(R.id.search_dynamic_recyclerView_songs);
        LinearLayoutManager managerTrack = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        TrackListAdapter adapterTrack = new TrackListAdapter(this, this, null, null);
        mRecyclerViewTracks.setLayoutManager(managerTrack);
        mRecyclerViewTracks.setAdapter(adapterTrack);

        mRecyclerViewUser = (RecyclerView) findViewById(R.id.search_dynamic_recyclerView_user);
        LinearLayoutManager managerUser = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        UserAdapter adapterUser = new UserAdapter(this, null);
        adapterUser.setUserCallback(this);
        mRecyclerViewUser.setLayoutManager(managerUser);
        mRecyclerViewUser.setAdapter(adapterUser);

        getmRecyclerViewGeneres = (RecyclerView) findViewById(R.id.search_dynamic_recyclerView_genere);
        GridLayoutManager managerGenere = new GridLayoutManager(this, 2, GridLayoutManager.VERTICAL, false);
        GenereAdapter adapterGenere = new GenereAdapter(this, this, null);
        getmRecyclerViewGeneres.setLayoutManager(managerGenere);
        getmRecyclerViewGeneres.setAdapter(adapterGenere);

        //Search bar
        mSearchText = (EditText) findViewById(R.id.search_bar);
        mSearchText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(UtilFunctions.noInternet(getApplicationContext())){
                    ErrorDialog.getInstance(SearchActivity.this).showErrorDialog("You have no internet connection!");
                }
                return false;
            }
        });
        if(!UtilFunctions.noInternet(getApplicationContext())){
            mSearchText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    searchCall();
                }
            });
        }


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.menu);
        navigation.setSelectedItemId(R.id.buscar);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
                        return true;
                    case R.id.buscar:
                        return true;
                    case R.id.perfil:
                        Intent intent2 = new Intent(getApplicationContext(), UserMainActivity.class);
                        intent2.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

                        startActivityForResult(intent2, Constants.NETWORK.LOGIN_OK);
                        return true;
                }
                return false;
            }
        });
    }

    private void searchCall (){
        if (!mSearchText.getText().toString().equals("") && mSearchText.getText().toString().length() > 1) {
            SearchManager.getInstance(this).getSearch(this, mSearchText.getText().toString());
        } else {
            mGeneresLayout.setVisibility(View.VISIBLE);
            mBothLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onFailure(Throwable throwable) {

    }

    @Override
    public void onAllGenreFailure(Throwable throwable) {
        if(UtilFunctions.noInternet(this)){
            onGenresReceive(ObjectBox.get().boxFor(SavedCache.class).get(1).retrieveAllGenres());
        }
    }

    @Override
    public void onGenresReceive(ArrayList<Genre> genres) {
        mGeneres = genres;
        GenereAdapter adapter = new GenereAdapter(this, this, mGeneres);
        getmRecyclerViewGeneres.setAdapter(adapter);
        mGeneresLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTracksByGenre(ArrayList<Track> tracks) {
        mPlaylistDeGenere.setTracks(tracks);

        Intent intent = new Intent(getApplicationContext(), PlaylistActivity.class);
        intent.putExtra("Playlst", mPlaylistDeGenere);
        startActivity(intent);
    }

    @Override
    public void onGenreSelected(Genre genere) {
        if(UtilFunctions.noInternet(this)){
            Toast toast = Toast.makeText(SearchActivity.this, "Feature not available!", Toast.LENGTH_LONG);
            View view = toast.getView();
            view.getBackground().setColorFilter(Color.parseColor("#21D760"), PorterDuff.Mode.SRC_IN);
            TextView text = view.findViewById(android.R.id.message);
            text.setTextColor(Color.WHITE);
            text.setTypeface(text.getTypeface(), Typeface.BOLD);
            toast.show();
        }else{
            mPlaylistDeGenere = new Playlist(genere.getName(), new User("Sallefy"));
            GenreManager.getInstance(this).getTracksByGenre(genere.getId(), this);
        }


    }

    @Override
    public void onGenreCreated(Genre data) {

    }

    @Override
    public void onGenreCreateFailure(Throwable throwable) {

    }


    @Override
    public void onPlaylistCreated(Playlist playlist) {

    }

    @Override
    public void onPlaylistFailure(Throwable throwable) {

    }

    @Override
    public void onPlaylistRecieved(List<Playlist> playlists) {

    }

    @Override
    public void onNoPlaylists(Throwable throwable) {

    }

    @Override
    public void onPlaylistSelected(Playlist playlist) {
        Intent intent = new Intent(getApplicationContext(), PlaylistActivity.class);
        intent.putExtra("Playlst", playlist);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
    }

    @Override
    public void onPlaylistToUpdated(Playlist body) {

    }



    @Override
    public void onTrackAddFailure(Throwable throwable) {

    }

    @Override
    public void onAllPlaylistRecieved(List<Playlist> body) {

    }

    @Override
    public void onAllNoPlaylists(Throwable throwable) {

    }

    @Override
    public void onAllPlaylistFailure(Throwable throwable) {

    }

    @Override
    public void onTopRecieved(List<Playlist> topPlaylists) {

    }

    @Override
    public void onNoTopPlaylists(Throwable throwable) {

    }

    @Override
    public void onTopPlaylistsFailure(Throwable throwable) {

    }

    @Override
    public void onFollowingRecieved(List<Playlist> body) {

    }

    @Override
    public void onFollowingChecked(Follow body) {

    }

    @Override
    public void onFollowSuccessfull(Follow body) {

    }

    @Override
    public void onPlaylistRecived(Playlist playlist) {

    }

    @Override
    public void onPlaylistDeleted(Playlist body) {

    }

    @Override
    public void onPlaylistDeleteFailure(Throwable throwable) {

    }

    @Override
    public void onAllMyPlaylistFailure(Throwable throwable) {

    }

    @Override
    public void onFollowingPlaylistsFailure(Throwable throwable) {

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
        Intent intent = new Intent(getApplicationContext(), InfoArtistaActivity.class);
        intent.putExtra("User", user);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
    }

    @Override
    public void onAllUsersSuccess(List<User> users) {

    }

    @Override
    public void onFollowedUsersSuccess(List<User> users) {

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
    public void onTrackSearchRecived(ArrayList<Track> tracks) {
        this.searchedTracks = tracks;
        mRecyclerViewTracks.setAdapter(new TrackListAdapter(this, this, tracks, null));

        mTracksLayout.setVisibility(View.VISIBLE);
        mBothLayout.setVisibility(View.VISIBLE);
        mGeneresLayout.setVisibility(View.GONE);
    }

    @Override
    public void onNoTrackSearchRecived() {
        mTracksLayout.setVisibility(View.GONE);
    }

    @Override
    public void onPlaylistSearchRecived(ArrayList<Playlist> playlists) {
        PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, playlists);
        playlistAdapter.setPlaylistCallback(this);
        mRecyclerViewPlaylist.setAdapter(playlistAdapter);

        mPlaylistLayout.setVisibility(View.VISIBLE);
        mBothLayout.setVisibility(View.VISIBLE);
        mGeneresLayout.setVisibility(View.GONE);
    }

    @Override
    public void onNoPlaylistSearchRecived() {
        mPlaylistLayout.setVisibility(View.GONE);
    }

    @Override
    public void onUserSearchRecived(ArrayList<User> users) {
        UserAdapter userAdapter = new UserAdapter(this, users);
        userAdapter.setUserCallback(this);
        mRecyclerViewUser.setAdapter(userAdapter);

        mUsersLayout.setVisibility(View.VISIBLE);
        mBothLayout.setVisibility(View.VISIBLE);
        mGeneresLayout.setVisibility(View.GONE);
    }

    @Override
    public void onNoUserSearchRecived() {
        mUsersLayout.setVisibility(View.GONE);
    }

    @Override
    public void onEmptySearch() {
        mBothLayout.setVisibility(View.GONE);
    }

    @Override
    public void onTrackSelected(int index) {
        serv.addToQueue(searchedTracks.get(index));
    }

    @Override
    public void onTrackAddSelected(int position, ArrayList<Track> tracks, Playlist playlist) {

    }

    @Override
    public void onTrackSelectedLiked(int position) {

    }

}

