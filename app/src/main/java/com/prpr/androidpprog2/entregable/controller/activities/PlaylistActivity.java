package com.prpr.androidpprog2.entregable.controller.activities;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import java.util.*;

import android.os.IBinder;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gauravk.audiovisualizer.visualizer.CircleLineVisualizer;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.adapters.TrackListAdapter;
import com.prpr.androidpprog2.entregable.controller.callbacks.ServiceCallback;
import com.prpr.androidpprog2.entregable.controller.callbacks.TrackListCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.PlaylistCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.TrackCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.PlaylistManager;
import com.prpr.androidpprog2.entregable.controller.restapi.service.ReproductorService;
import com.prpr.androidpprog2.entregable.model.Follow;
import com.prpr.androidpprog2.entregable.model.Playlist;
import com.prpr.androidpprog2.entregable.model.Track;
import com.prpr.androidpprog2.entregable.utils.Constants;
import com.prpr.androidpprog2.entregable.utils.PreferenceUtils;
import com.prpr.androidpprog2.entregable.utils.Session;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PlaylistActivity extends AppCompatActivity implements TrackCallback, TrackListCallback, PlaylistCallback, ServiceCallback {

    private Playlist playlst;
    private TextView plyName;
    private TextView plyAuthor;
    private ImageView plyImg;

    private TextView tvTitle;
    private TextView tvAuthor;
    private LinearLayout playing;

    private Button back2Main;
    private Button infoPlaylist;
    private Button shuffle;
    private Button follow;
    private Follow followingInfo;
    private boolean isFollowing = false;
    private Button addBunch;
    private Button play;
    private Button pause;

    private RecyclerView mRecyclerView;
    private boolean bunch;
    private ArrayList<Track> mTracks;
    private int currentTrack = 0;
    private PlaylistManager pManager;

    //Sort
    private FloatingActionButton mSorts;
    private FloatingActionButton mSortAlpha;
    private FloatingActionButton mSortTime;
    private FloatingActionButton mSortArtist;
    private int mSorted = -1;
    private boolean isOpen;
    private boolean asc_dsc;

    Animation fabOpen, fabClose;
    private final int SORT_AZ = 0;
    private final int SORT_TIME = 1;
    private final int SORT_ARTIST = 2;


    //----------------------------------------------------------------PART DE SERVICE--------------------------------------------------------------------------------
    private SeekBar mseek;
    private ReproductorService player;
    private ImageView im;
    private boolean serviceBound = false;
    private boolean isShuffle = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.prpr.androidpprog2.entregable.PlayNewAudio";


    private void playAudio(int audioIndex) {
        PreferenceUtils.saveAllTracks(getApplicationContext(), mTracks);
        PreferenceUtils.saveTrackIndex(getApplicationContext(), audioIndex);

        if (!serviceBound) {
            Intent playerIntent = new Intent(this, ReproductorService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        } else {
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
        tvTitle.setText(mTracks.get(audioIndex).getName());
        tvAuthor.setText(mTracks.get(audioIndex).getUserLogin());
    }

    private void getNewIndex(int ogIndex){
        int finalIndex=0;
        Track t = mTracks.get(ogIndex);
        Collections.shuffle(mTracks);
        for(int i=0; i<mTracks.size() ;i++){
            if(mTracks.get(i).getName().equals(t.getName()) && mTracks.get(i).getUserLogin().equals(t.getUserLogin())){
                finalIndex=i;
            }
        }
        Collections.swap(mTracks, 0, finalIndex);
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ReproductorService.LocalBinder binder = (ReproductorService.LocalBinder) service;
            player = binder.getService();
            player.setmSeekBar(mseek);
            player.setSeekCallback(PlaylistActivity.this);
            serviceBound = true;
            player.setUIControls(mseek, tvTitle, tvAuthor, play, pause, im);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            player.stopSelf();
        }
    }

    @Override
    public void onSeekBarUpdate(int progress, int duration, boolean isPlaying, String duracio) {
        if(isPlaying){
            mseek.postDelayed(player.getmProgressRunner(), 1000);
        }
        mseek.setProgress(progress);
    }


    @Override
    public void onStart() {
        super.onStart();
        if(!serviceBound){
            Intent intent = new Intent(this, ReproductorService.class);
            startService(intent);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }else{
            player.setUIControls(mseek, tvTitle, tvAuthor, play, pause, im);
            player.updateUI();
            player.setSeekCallback(this);
        }
        pManager.checkFollowing(playlst.getId(), this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(serviceBound){
            player.setSeekCallback(this);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_layout);
        if(getIntent().getSerializableExtra("Playlst")!=null){
            playlst = (Playlist) getIntent().getSerializableExtra("Playlst");
        }
        pManager = new PlaylistManager(this);
        if(playlst.getId()!=-5){
            pManager.checkFollowing(playlst.getId(), this);
        }
        initViews();
        getData();
    }

    //----------------------------------------------------------------FIN DE LA PART DE SERVICE--------------------------------------------------------------------------------



    private void initViews() {

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.menu);
        navigation.setSelectedItemId(R.id.none);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        Intent intent0 = new Intent(getApplicationContext(), MainActivity.class);
                        intent0.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(intent0, Constants.NETWORK.LOGIN_OK);
                        return true;
                    case R.id.buscar:
                        Intent intent1 = new Intent(getApplicationContext(), SearchActivity.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(intent1, Constants.NETWORK.LOGIN_OK);
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

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        playing = findViewById(R.id.reproductor);
        playing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ReproductorActivity.class);
                startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.dynamic_recyclerView);
        LinearLayoutManager manager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        TrackListAdapter adapter = new TrackListAdapter(this, this, null, playlst);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(adapter);



        follow = findViewById(R.id.playlistSeguirBoto);
        follow.setEnabled(true);
        follow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pManager.followPlaylist(playlst.getId(), PlaylistActivity.this);
            }
        });

        if(playlst.getId()==-5){
            follow.setVisibility(View.GONE);
        }

        plyName = findViewById(R.id.playlistName);
        plyAuthor = findViewById(R.id.playlistAuthor);
        plyImg = findViewById(R.id.playlistCover);

        plyName.setText(playlst.getName());
        if(playlst.getOwner()!=null){
            plyAuthor.setText("Created by " + playlst.getUserLogin());
        }else{
            plyAuthor.setText("Created by admin");
        }

        if (playlst.getThumbnail() != null) {
            Picasso.get().load(playlst.getThumbnail()).into(plyImg);
        }else{
            Picasso.get().load("https://community.spotify.com/t5/image/serverpage/image-id/25294i2836BD1C1A31BDF2/image-size/original?v=mpbl-1&px=-1").into(plyImg);
        }

        play = findViewById(R.id.playButton);
        play.setEnabled(true);
        play.bringToFront();
        play.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                int index=0;
                player.resumeMedia();
                pause.setVisibility(View.VISIBLE);
                play.setVisibility(View.INVISIBLE);
            }
        });
        pause = findViewById(R.id.playPause);
        pause.setEnabled(true);
        pause.bringToFront();
        pause.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                player.pauseMedia();
                play.setVisibility(View.VISIBLE);
                pause.setVisibility(View.INVISIBLE);
            }
        });

        mseek = findViewById(R.id.dynamic_seekBar);
        mseek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    player.seekToPosition(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        shuffle = findViewById(R.id.playlistRandom);
        shuffle.setEnabled(true);
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShuffle=true;
                playAudio(new Random().nextInt(mTracks.size()));
            }
        });

        addBunch = findViewById(R.id.PlaylistAddSongs);
        addBunch.setEnabled(true);
        addBunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AddSongsBunchActivity.class);
                intent.putExtra("Playlst", playlst);
                startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
            }
        });

        if(Session.getInstance(getApplicationContext()).getUser().getLogin().equals(playlst.getOwner().getLogin())){
            addBunch.setVisibility(View.VISIBLE);
            follow.setVisibility(View.GONE);
        }else{
            addBunch.setVisibility(View.INVISIBLE);
            follow.setVisibility(View.VISIBLE);
        }
        if(playlst.getId()==-5){
            follow.setVisibility(View.GONE);
        }

        infoPlaylist = findViewById(R.id.infoPlaylist);
        infoPlaylist.setEnabled(true);
        infoPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), InfoPlaylistActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
            }
        });


        back2Main = findViewById(R.id.back2Main);
        back2Main.setEnabled(true);
        back2Main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //player.savePosition();
                if(bunch){
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
                }else{
                    /*Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);*/
                    finish();
                    overridePendingTransition(R.anim.nothing,R.anim.nothing);
                }

            }
        });

        tvAuthor = findViewById(R.id.dynamic_artist);
        tvTitle = findViewById(R.id.dynamic_title);
        tvTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvTitle.setSelected(true);
        tvTitle.setSingleLine(true);

        fabOpen = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(this, R.anim.fab_close);

        mSorts = findViewById(R.id.playlistSorts);
        mSorts.setEnabled(true);
        mSorts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
            }
        });

        mSortAlpha = findViewById(R.id.playlistSortAlpha);
        mSortAlpha.setEnabled(false);
        mSortAlpha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
                sortAZ();
            }
        });

        mSortTime = findViewById(R.id.playlistSortTime);
        mSortTime.setEnabled(false);
        mSortTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
                sortTime();
            }
        });

        mSortArtist = findViewById(R.id.playlistSortArtist);
        mSortArtist.setEnabled(false);
        mSortArtist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
                sortArtist();

            }
        });
    }

    private void sortAZ(){
        if (mSorted != SORT_AZ) {
            Collections.sort(mTracks, Track.TrackNameAscendentComparator);
            asc_dsc = true;
        } else {
            if (asc_dsc) {
                Collections.sort(mTracks, Track.TrackNameDescendentComparator);
                asc_dsc = false;
            } else {
                Collections.sort(mTracks, Track.TrackNameAscendentComparator);
                asc_dsc = true;
            }
        }
        TrackListAdapter adapter = new TrackListAdapter(this, this, mTracks, playlst);
        mRecyclerView.setAdapter(adapter);
        mSorted = SORT_AZ;
    }

    private void sortTime(){
        if (mSorted != SORT_TIME) {
            Collections.sort(mTracks, Track.TrackAscendentDurationComparator);
            asc_dsc = true;
        } else {
            if (asc_dsc) {
                Collections.sort(mTracks, Track.TrackDescendentDurationComparator);
                asc_dsc = false;
            } else {
                Collections.sort(mTracks, Track.TrackAscendentDurationComparator);
                asc_dsc = true;
            }
        }
        TrackListAdapter adapter = new TrackListAdapter(this, this, mTracks, playlst);
        mRecyclerView.setAdapter(adapter);
        mSorted = SORT_TIME;
    }

    private void sortArtist(){
        if (mSorted != SORT_ARTIST) {
            Collections.sort(mTracks, Track.TrackArtistNameAscendentComparator);
            asc_dsc = true;
        } else {
            if (asc_dsc) {
                Collections.sort(mTracks, Track.TrackArtistNameDescendentComparator);
                asc_dsc = false;
            } else {
                Collections.sort(mTracks, Track.TrackArtistNameAscendentComparator);
                asc_dsc = true;
            }
        }
        TrackListAdapter adapter = new TrackListAdapter(this, this, mTracks, playlst);
        mRecyclerView.setAdapter(adapter);
        mSorted = SORT_ARTIST;
    }

    private void animateFab(){
        if(isOpen){
            mSortArtist.startAnimation(fabClose);
            mSortArtist.setClickable(false);
            mSortArtist.setEnabled(false);
            mSortTime.startAnimation(fabClose);
            mSortTime.setClickable(false);
            mSortTime.setEnabled(false);
            mSortAlpha.startAnimation(fabClose);
            mSortAlpha.setClickable(false);
            mSortAlpha.setEnabled(false);
            isOpen=false;
        }else{
            mSortArtist.startAnimation(fabOpen);
            mSortArtist.setClickable(true);
            mSortArtist.setEnabled(true);
            mSortTime.startAnimation(fabOpen);
            mSortTime.setClickable(true);
            mSortTime.setEnabled(true);
            mSortAlpha.startAnimation(fabOpen);
            mSortAlpha.setClickable(true);
            mSortAlpha.setEnabled(true);
            isOpen=true;
        }
    }

    private void getData() {
        mTracks = (ArrayList<Track>) playlst.getTracks();
        TrackListAdapter adapter = new TrackListAdapter(this, this, mTracks, playlst);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onTracksReceived(List<Track> tracks) {
        mTracks = (ArrayList<Track>) tracks;
        PreferenceUtils.saveAllTracks(getApplicationContext(), mTracks);
        TrackListAdapter adapter = new TrackListAdapter(this, this, mTracks, playlst);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onNoTracks(Throwable throwable) {

    }

    @Override
    public void onPersonalTracksReceived(List<Track> tracks) {

    }

    @Override
    public void onUserTracksReceived(List<Track> tracks) {
        playAudio(0);
    }

    @Override
    public void onCreateTrack(Track t) {

    }

    @Override
    public void onTopTracksRecieved(List<Track> tracks) {

    }

    @Override
    public void onNoTopTracks(Throwable throwable) {

    }

    @Override
    public void onTrackLiked() {

    }

    @Override
    public void onTrackNotFound(Throwable throwable) {

    }


    @Override
    public void onFailure(Throwable throwable) {

    }

    @Override
    public void onTrackSelected(int index) {
        pause.setVisibility(View.VISIBLE);
        play.setVisibility(View.INVISIBLE);
        currentTrack = index;
        playAudio(index);
    }

    @Override
    public void onTrackAddSelected(int position, ArrayList<Track> tracks, Playlist p) {
        Intent intent = new Intent(getApplicationContext(), InfoTrackActivity.class);
        intent.putExtra("Trck", tracks.get(position));
        intent.putExtra("Playlst", p);
        startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
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
        followingInfo = body;
        if(followingInfo.isFollowing()){
            follow.setText("Following");
            follow.setBackgroundResource(R.drawable.rectangle_small_gborder_green);;
            isFollowing=false;
        }else{
            follow.setText("Follow");
            follow.setBackgroundResource(R.drawable.rectangle_small_gborder_black);;
            isFollowing=true;
        }
    }

    @Override
    public void onFollowSuccessfull(Follow body) {
        followingInfo = body;
        if(followingInfo.isFollowing()){
            follow.setText("Following");
            follow.setBackgroundResource(R.drawable.rectangle_small_gborder_green);;
            isFollowing=false;
        }else{
            follow.setText("Follow");
            follow.setBackgroundResource(R.drawable.rectangle_small_gborder_black);;
            isFollowing=true;
        }
    }


}
