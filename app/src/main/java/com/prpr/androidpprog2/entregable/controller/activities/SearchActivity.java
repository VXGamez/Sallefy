package com.prpr.androidpprog2.entregable.controller.activities;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.adapters.GenereAdapter;
import com.prpr.androidpprog2.entregable.controller.adapters.PlaylistAdapter;
import com.prpr.androidpprog2.entregable.controller.adapters.TrackListAdapter;
import com.prpr.androidpprog2.entregable.controller.callbacks.PlaylistListCallback;
import com.prpr.androidpprog2.entregable.controller.callbacks.TrackListCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.GenreCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.PlaylistCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.TrackCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.GenreManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.PlaylistManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.TrackManager;
import com.prpr.androidpprog2.entregable.model.Genre;
import com.prpr.androidpprog2.entregable.model.Playlist;
import com.prpr.androidpprog2.entregable.model.Track;
import com.prpr.androidpprog2.entregable.model.User;
import com.prpr.androidpprog2.entregable.utils.Constants;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements TrackListCallback, PlaylistListCallback, GenreCallback, TrackCallback, PlaylistCallback {

    //Arraylist de totes les cançons i playlists
    private ArrayList<Track> mTracks;
    private ArrayList<Playlist> mPlaylist;

    //Arraylist de totes les cançons i playlists que s'han de mostrar
    private ArrayList<Track> mTracksOnView;
    private ArrayList<Playlist> mPlaylistOnView;

    //Llista de songs i playists
    private RecyclerView mRecyclerViewTracks;
    private RecyclerView mRecyclerViewPlaylist;

    //Generes
    private RecyclerView getmRecyclerViewGeneres;
    private ArrayList<Genre> mGeneres;
    private Playlist mPlaylistDeGenere;

    //Cerca
    private EditText mSearchText;

    //Possibles layouts en la cerca
    private LinearLayout mGeneresLayout;
    private LinearLayout mPlaylistLayout;
    private LinearLayout mTracksLayout;
    private LinearLayout mBothLayout;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initViews();
    }

    void initViews(){
        //No mostrem res
        mGeneresLayout = (LinearLayout) findViewById(R.id.search_genere_layout);
        //mGeneresLayout.setVisibility(View.GONE);

        mPlaylistLayout = (LinearLayout) findViewById(R.id.search_recyclerView_playlist);
        //mPlaylistLayout.setVisibility(View.GONE);

        mTracksLayout = (LinearLayout) findViewById(R.id.search_recyclerView_song);
        //mTracksLayout.setVisibility(View.GONE);

        mBothLayout = (LinearLayout) findViewById(R.id.search_recyclerView_both);
        //mBothLayout.setVisibility(View.GONE);

        //Obtenim GENERES LIST
        mGeneres = new ArrayList<>();
        GenreManager.getInstance(this).getAllGenres(this);

        //Obtenim totes les playlists
        mTracks = new ArrayList<>();
        mTracksOnView = new ArrayList<>();
        PlaylistManager.getInstance(this).getAllPlaylists(this);

        //Obtemin totes les tracks
        mPlaylist = new ArrayList<>();
        mPlaylistOnView = new ArrayList<>();
        TrackManager.getInstance(this).getAllTracks(this);

        //Recicle views
        mRecyclerViewTracks = (RecyclerView) findViewById(R.id.search_dynamic_recyclerView_songs);
        LinearLayoutManager managerTrack = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        TrackListAdapter adapterTrack = new TrackListAdapter(this, this, null, null);
        mRecyclerViewTracks.setLayoutManager(managerTrack);
        mRecyclerViewTracks.setAdapter(adapterTrack);

        mRecyclerViewPlaylist = (RecyclerView) findViewById(R.id.search_dynamic_recyclerView_playlist);
        LinearLayoutManager managerPlaylist = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        PlaylistAdapter adapterPlaylist = new PlaylistAdapter(this, null);
        mRecyclerViewPlaylist.setLayoutManager(managerPlaylist);
        mRecyclerViewPlaylist.setAdapter(adapterPlaylist);

        getmRecyclerViewGeneres = (RecyclerView) findViewById(R.id.search_dynamic_recyclerView_genere);
        LinearLayoutManager managerGenere = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        GenereAdapter adapterGenere = new GenereAdapter(this, null);
        mRecyclerViewPlaylist.setLayoutManager(managerGenere);
        mRecyclerViewPlaylist.setAdapter(adapterGenere);

        //Search bar
        mSearchText = (EditText) findViewById(R.id.search_bar);
        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                System.out.println("ele1");
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                System.out.println("ele2");
            }

            @Override
            public void afterTextChanged(Editable editable) {
                updateInfo();
                System.out.println("ele3");
            }
        });

        //XI
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
                        Intent intent2 = new Intent(getApplicationContext(), UserPlaylistActivity.class);
                        intent2.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(intent2, Constants.NETWORK.LOGIN_OK);
                        return true;
                }
                return false;
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateInfo() {
        String input = mSearchText.getText().toString();

        //TRACKS
        mTracksOnView.clear();
        for (Track track : mTracks) {
            if (track.getName().toLowerCase().contains(input.toLowerCase()))
                mTracksOnView.add(track);
        }

        if (mTracksOnView.size() > 0) {
            mRecyclerViewTracks.setAdapter(new TrackListAdapter(this, this, mTracksOnView, null));
            mRecyclerViewTracks.setVisibility(View.VISIBLE);
        } else {
            mRecyclerViewTracks.setVisibility(View.GONE);
        }

        //PLAYLISTS
        mPlaylistOnView.clear();
        for (Playlist playlist : mPlaylist) {
            if (playlist.getName().toLowerCase().contains(input.toLowerCase()))
                mPlaylistOnView.add(playlist);
        }

        if (mPlaylistOnView.size() > 0) {
            mRecyclerViewPlaylist.setAdapter(new PlaylistAdapter(this, mPlaylistOnView));
            mRecyclerViewPlaylist.setVisibility(View.VISIBLE);
        } else {
            mRecyclerViewPlaylist.setVisibility(View.GONE);
        }

        //BOTH
        if (mPlaylistOnView.size() < 1 && mTracksOnView.size() < 1) {
            mBothLayout.setVisibility(View.GONE);
        } else {
            mBothLayout.setVisibility(View.VISIBLE);
        }

        //SOUTS DE TRACKS
        System.out.println("TRACKS ON VIEW");
        for (Track track : mTracksOnView)
            track.print();

        //SOUTS DE PLAYLISTS
        System.out.println("PLAYLISTS ON VIEW");
        for (Playlist playlist: mPlaylistOnView)
            playlist.print();

    }

    @Override
    public void onTrackSelected(int index) {

    }

    @Override
    public void onTrackAddSelected(int position, ArrayList<Track> tracks, Playlist playlist) {

    }

    @Override
    public void onGenresReceive(ArrayList<Genre> genres) {
        mGeneres = genres;

        GenereAdapter adapter = new GenereAdapter(this, mGeneres);
        getmRecyclerViewGeneres.setAdapter(adapter);

       // mGeneresLayout.setVisibility(View.VISIBLE);
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
        mPlaylistDeGenere = new Playlist(genere.getName(), new User("Sallefy"));
        GenreManager.getInstance(this).getTracksByGenre(genere.getId(), this);
    }

    @Override
    public void onFailure(Throwable throwable) {

    }

    @Override
    public void onTracksReceived(List<Track> tracks) {
        mTracks = (ArrayList<Track>) tracks;
    }

    @Override
    public void onNoTracks(Throwable throwable) {

    }

    @Override
    public void onPersonalTracksReceived(List<Track> tracks) {

    }

    @Override
    public void onUserTracksReceived(List<Track> tracks) {

    }

    @Override
    public void onCreateTrack(Track t) {

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

    }

    @Override
    public void onTrackAdded(Playlist body) {

    }

    @Override
    public void onTrackAddFailure(Throwable throwable) {

    }

    @Override
    public void onAllPlaylistRecieved(List<Playlist> body) {
        mPlaylist = (ArrayList<Playlist>) body;
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
}

