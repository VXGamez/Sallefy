package com.prpr.androidpprog2.entregable.controller.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import java.util.*;

import android.os.IBinder;
import android.os.Parcelable;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.adapters.TrackListAdapter;
import com.prpr.androidpprog2.entregable.controller.callbacks.OptionDialogCallback;
import com.prpr.androidpprog2.entregable.controller.callbacks.TrackListCallback;
import com.prpr.androidpprog2.entregable.controller.dialogs.ErrorDialog;
import com.prpr.androidpprog2.entregable.controller.dialogs.OptionDialog;
import com.prpr.androidpprog2.entregable.controller.fragments.InfoPlaylistFragment;
import com.prpr.androidpprog2.entregable.controller.fragments.InfoTrackFragment;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.PlaylistCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.TrackCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.PlaylistManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.TrackManager;
import com.prpr.androidpprog2.entregable.controller.music.ReproductorService;
import com.prpr.androidpprog2.entregable.model.DB.ObjectBox;
import com.prpr.androidpprog2.entregable.model.DB.SavedPlaylist;
import com.prpr.androidpprog2.entregable.model.DB.UtilFunctions;
import com.prpr.androidpprog2.entregable.model.Follow;
import com.prpr.androidpprog2.entregable.model.Playlist;
import com.prpr.androidpprog2.entregable.model.Track;
import com.prpr.androidpprog2.entregable.model.User;
import com.prpr.androidpprog2.entregable.utils.ConnectivityService;
import com.prpr.androidpprog2.entregable.utils.Constants;
import com.prpr.androidpprog2.entregable.utils.KeyboardUtils;
import com.prpr.androidpprog2.entregable.utils.PreferenceUtils;
import com.prpr.androidpprog2.entregable.utils.Session;
import com.squareup.picasso.Picasso;


import java.util.ArrayList;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class PlaylistActivity extends AppCompatActivity implements TrackCallback, TrackListCallback, PlaylistCallback, OptionDialogCallback {
    private Playlist playlst;
    private TextView plyName;
    private TextView plyAuthor;
    private TextView followers;
    private ImageView plyImg;
    private ImageButton imgEdit;

    private TextView tvTitle;
    private TextView tvAuthor;
    private LinearLayout playing;
    private LinearLayout actionButtons;
    private LinearLayout reproductor;

    private Button back2Main;
    private Button infoPlaylist;
    private Button shuffle;
    private Button acceptEdit;
    private Button follow;
    private Button accessible;
    private Follow followingInfo;
    private boolean isFollowing = false;
    private Button addBunch;
    private Button play;
    private Button pause;

    private EditText newName;
    private TextView nametext;
    private EditText descripcioCanvi;
    private TextView descripcioText;
    private LinearLayout canvi;


    private RecyclerView mRecyclerView;
    private boolean bunch;
    private ArrayList<Track> mTracks;
    private int currentTrack = 0;
    private PlaylistManager pManager;
    private TrackManager trackManager;

    //Sort
    private FloatingActionButton mSorts;
    private FloatingActionButton mSortAlpha;
    private FloatingActionButton mSortTime;
    private FloatingActionButton mSortArtist;
    private int mSorted = -1;
    private boolean isOpen;
    private boolean asc_dsc;
    private BottomNavigationView navigation;

    private TrackListAdapter adapter;

    private ImageButton info;
    private ImageView user;
    private TextView name, description;
    private LinearLayout info_layout;
    private ImageButton back;

    private ScrollView scroll;


    private Animation fabOpen, fabClose;
    private OptionDialog dialogEdit;
    private final int SORT_AZ = 0;
    private final int SORT_TIME = 1;
    private final int SORT_ARTIST = 2;


    private ArrayList<User> seguidoreh;

    //----------------------------------------------------------------PART DE SERVICE--------------------------------------------------------------------------------
    private SeekBar mseek;
    private ReproductorService player;
    private ImageView im;
    private boolean serviceBound = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.prpr.androidpprog2.entregable.PlayNewAudio";

    private void playAudio(int audioIndex) {
        if(mSorted == -1){
            PreferenceUtils.saveAllTracks(getApplicationContext(), mTracks);
        }
        PreferenceUtils.saveTrack(getApplicationContext(), mTracks.get(audioIndex));
        PreferenceUtils.savePlayID(getApplicationContext(), playlst.getId());

        if (!serviceBound) {
            saveIdForFuture();
            Intent playerIntent = new Intent(this, ReproductorService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        } else {
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ReproductorService.LocalBinder binder = (ReproductorService.LocalBinder) service;
            player = binder.getService();
            player.setmSeekBar(mseek);
            player.setMainActivity(PlaylistActivity.this);
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
        unregisterReceiver(connectionLost);
        unregisterReceiver(connectionRegained);
    }


    @Override
    public void onStart() {
        super.onStart();
        if(!serviceBound){
            saveIdForFuture();
            Intent intent = new Intent(this, ReproductorService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }else{
            player.setUIControls(mseek, tvTitle, tvAuthor, play, pause, im);
            player.updateUI();
            player.setMainActivity(PlaylistActivity.this);

        }
        pManager.checkFollowing(playlst.getId(), this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!serviceBound){
            saveIdForFuture();
            Intent intent = new Intent(this, ReproductorService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }else{
            player.setUIControls(mseek, tvTitle, tvAuthor, play, pause, im);
            player.updateUI();
        }
        pManager.checkFollowing(playlst.getId(), this);
        pManager.getPlaylist(playlst.getId(), this);
        orderByPreferenceUtils();
    }

    private BroadcastReceiver connectionRegained = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter = new TrackListAdapter(PlaylistActivity.this, PlaylistActivity.this, mTracks, playlst);
            mRecyclerView.setAdapter(adapter);
        }
    };

    private BroadcastReceiver connectionLost = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adapter = new TrackListAdapter(PlaylistActivity.this, PlaylistActivity.this, mTracks, playlst);
            mRecyclerView.setAdapter(adapter);
        }
    };

    private void registerConnectionRegained() {
        IntentFilter filter = new IntentFilter(ConnectivityService.Broadcast_CONNECTION_REGAINED);
        registerReceiver(connectionRegained, filter);
    }

    private void registerConnectionLost() {
        IntentFilter filter = new IntentFilter(ConnectivityService.Broadcast_CONNECTION_LOST);
        registerReceiver(connectionLost, filter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_playlist_layout);
        if(getIntent().getSerializableExtra("Playlst")!=null){
            playlst = (Playlist) getIntent().getSerializableExtra("Playlst");
        }
        if(getIntent().getSerializableExtra("bunch")!=null){
            bunch = (boolean) getIntent().getSerializableExtra("bunch");
        }
        pManager = new PlaylistManager(this);
        trackManager = new TrackManager(this);
        if(playlst.getId()!=-5){
            pManager.checkFollowing(playlst.getId(), this);
        }
        if(!playlst.getUserLogin().equals(Session.getInstance().getUser().getLogin())){
            if(UtilFunctions.playlistExistsInDatabase(playlst)){
                UtilFunctions.checkForPlaylistUpdate(playlst);
            }
        }

        registerConnectionLost();
        registerConnectionRegained();
        initViews();
        getData();
    }

    //----------------------------------------------------------------FIN DE LA PART DE SERVICE--------------------------------------------------------------------------------



    private void initViews() {
        navigation = (BottomNavigationView) findViewById(R.id.menu);
        navigation.setSelectedItemId(R.id.none);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        saveIdForFuture();
                        Intent intent0 = new Intent(getApplicationContext(), MainActivity.class);
                        intent0.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(intent0, Constants.NETWORK.LOGIN_OK);
                        return true;
                    case R.id.buscar:
                        saveIdForFuture();
                        Intent intent1 = new Intent(getApplicationContext(), SearchActivity.class);
                        intent1.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(intent1, Constants.NETWORK.LOGIN_OK);
                        return true;
                    case R.id.perfil:
                        saveIdForFuture();
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

        followers = findViewById(R.id.followers);
        followers.setText(playlst.getFollowers() +" Followers");


        playing = findViewById(R.id.reproductor);
        playing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(player!=null && !tvTitle.getText().toString().equals("")){
                    saveIdForFuture();
                    Intent intent = new Intent(getApplicationContext(), ReproductorActivity.class);
                    startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
                    overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
                }else{
                    ErrorDialog.getInstance(PlaylistActivity.this).showErrorDialog("You haven't selected a song yet!");
                }

            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.dynamic_recyclerView);
        LinearLayoutManager manager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        adapter = new TrackListAdapter(this, this, null, playlst);
        ItemTouchHelper helper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder dragged, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                if (direction == ItemTouchHelper.LEFT) {
                    player.addToQueue(playlst.getTracks().get(viewHolder.getAdapterPosition()));
                    Toast toast = Toast.makeText(PlaylistActivity.this, "Track added to queue!", Toast.LENGTH_SHORT);
                    View view = toast.getView();
                    view.getBackground().setColorFilter(Color.parseColor("#21D760"), PorterDuff.Mode.SRC_IN);
                    TextView text = view.findViewById(android.R.id.message);
                    text.setTextColor(Color.WHITE);
                    text.setTypeface(text.getTypeface(), Typeface.BOLD);
                    toast.show();
                }
            }

            @Override
            public void onChildDraw (Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,float dX, float dY,int actionState, boolean isCurrentlyActive){
                new RecyclerViewSwipeDecorator.Builder(PlaylistActivity.this, c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addActionIcon(R.drawable.ic_queue_add).addSwipeLeftBackgroundColor(ContextCompat.getColor(PlaylistActivity.this, R.color.fonsColor))
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX / 6, dY, actionState, isCurrentlyActive);
            }
        });

        helper.attachToRecyclerView(mRecyclerView);

        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(adapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0 && mSorts.getVisibility() == View.VISIBLE) {
                    mSorts.hide();
                } else if (dy < 0 && mSorts.getVisibility() != View.VISIBLE) {
                    mSorts.show();
                }
            }
        });


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

        acceptEdit = findViewById(R.id.acceptEdit);
        acceptEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideUIEdit();
                if(!newName.getText().toString().matches("")){
                    playlst.setName(newName.getText().toString());
                }

                if(!descripcioCanvi.getText().toString().matches("")){
                    playlst.setDescription(descripcioCanvi.getText().toString());
                }

                pManager.updatePlaylist(playlst, PlaylistActivity.this);
            }
        });
        acceptEdit.setVisibility(View.GONE);

        reproductor= findViewById(R.id.reproductor);

        accessible= findViewById(R.id.privadaPublica);
        accessible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlst.setPublicAccessible(!playlst.isPublicAccessible());
                pManager.updatePlaylist(playlst, PlaylistActivity.this);
                if(playlst.isPublicAccessible()){
                    accessible.setText("Public");
                    accessible.setBackgroundResource(R.drawable.rectangle_small_gborder_green);;
                }else{
                    accessible.setText("Private");
                    accessible.setBackgroundResource(R.drawable.rectangle_small_gborder_black);;
                }
            }
        });
        if(playlst.isPublicAccessible()){
            accessible.setText("Public");
            accessible.setBackgroundResource(R.drawable.rectangle_small_gborder_green);;
        }else{
            accessible.setText("Private");
            accessible.setBackgroundResource(R.drawable.rectangle_small_gborder_black);;
        }



        plyName = findViewById(R.id.playlistName);
        plyName.setVisibility(View.VISIBLE);
        plyAuthor = findViewById(R.id.playlistAuthor);
        plyImg = findViewById(R.id.playlistCover);

        newName = findViewById(R.id.nomCanvi);
        newName.setVisibility(View.GONE);

        plyName.setText(playlst.getName());
        if(playlst.getOwner()!=null){
            plyAuthor.setText("Created by " + playlst.getUserLogin());
        }else{
            plyAuthor.setText("Created by admin");
        }

        if(UtilFunctions.noInternet(getApplicationContext())){
            if(UtilFunctions.playlistExistsInDatabase(playlst)){
                SavedPlaylist p = ObjectBox.get().boxFor(SavedPlaylist.class).get(playlst.getId());
                Bitmap myBitmap = BitmapFactory.decodeFile(p.coverPath);
                plyImg.setImageBitmap(myBitmap);
            }else{
                Picasso.get().load(R.drawable.default_cover).into(plyImg);
            }
        }else{
            if (playlst.getThumbnail() != null) {
                Picasso.get().load(playlst.getThumbnail()).into(plyImg);
            }else {
                Picasso.get().load(R.drawable.default_cover).into(plyImg);
            }
        }

        play = findViewById(R.id.playButton);
        play.setEnabled(true);
        play.bringToFront();
        play.setOnClickListener(new View.OnClickListener() {

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

            @Override
            public void onClick(View v) {
                player.pauseMedia();
                play.setVisibility(View.VISIBLE);
                pause.setVisibility(View.INVISIBLE);
            }
        });

        actionButtons = findViewById(R.id.actionbuttons);

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
                if(mTracks.size()>0){
                    playAudio(new Random().nextInt(mTracks.size()));
                    player.setShuffle(true);
                }else{
                    ErrorDialog.getInstance(PlaylistActivity.this).showErrorDialog("There is no tracks to play!");
                }

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


        infoPlaylist = findViewById(R.id.infoPlaylist);
        infoPlaylist.setEnabled(true);
        infoPlaylist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoPlaylistFragment bottomSheetDialog = new InfoPlaylistFragment(playlst);
                bottomSheetDialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
                bottomSheetDialog.show(getSupportFragmentManager(), "playlist_info");
            }
        });
        infoPlaylist.setVisibility(View.VISIBLE);


        if(Session.getInstance(getApplicationContext()).getUser().getLogin().equals(playlst.getOwner().getLogin())){
            addBunch.setVisibility(View.VISIBLE);
            follow.setVisibility(View.GONE);
            accessible.setVisibility(View.VISIBLE);
        }else{
            addBunch.setVisibility(View.INVISIBLE);
            follow.setVisibility(View.VISIBLE);
            accessible.setVisibility(View.GONE);
        }
        if(playlst.getId()==-5){
            follow.setVisibility(View.GONE);
        }
        if(UtilFunctions.noInternet(getApplicationContext())){
            accessible.setVisibility(View.GONE);
            follow.setVisibility(View.GONE);
            addBunch.setVisibility(View.GONE);
        }

        dialogEdit = new OptionDialog(PlaylistActivity.this, PlaylistActivity.this);

        if(playlst.getUserLogin().equals("Sallefy") && playlst.getId()== -5){
            followers.setVisibility(View.INVISIBLE);
            infoPlaylist.setVisibility(View.INVISIBLE);
        }


        back2Main = findViewById(R.id.back2Main);
        back2Main.setEnabled(true);
        back2Main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(bunch){
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);

                }else{
                    finish();
                    overridePendingTransition(R.anim.nothing,R.anim.nothing);
                }

            }
        });

        tvAuthor = findViewById(R.id.dynamic_artist);
        tvAuthor.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tvAuthor.setSelected(true);
        tvAuthor.setSingleLine(true);
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
                player.novaLlista();
                sortAZ();
            }
        });

        mSortTime = findViewById(R.id.playlistSortTime);
        mSortTime.setEnabled(false);
        mSortTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
                player.novaLlista();
                sortTime();
            }
        });

        mSortArtist = findViewById(R.id.playlistSortArtist);
        mSortArtist.setEnabled(false);
        mSortArtist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateFab();
                player.novaLlista();
                sortArtist();

            }
        });

        info = findViewById(R.id.info);
        user = findViewById(R.id.userImage);
        if (playlst.getOwner().getImageUrl() != null && !playlst.getOwner().getImageUrl().isEmpty()) {
            Picasso.get().load(playlst.getOwner().getImageUrl()).into(user);
        }else{
            Picasso.get().load(R.drawable.default_user_cover).into(user);
        }
        name = findViewById(R.id.userName);
        name.setText(playlst.getOwner().getFirstName());
        description = findViewById(R.id.description);
        description.setText(playlst.getDescription());

        info_layout = findViewById(R.id.InfoLayout);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                info.setVisibility(View.GONE);
                back.setVisibility(View.VISIBLE);
                info_layout.setVisibility(View.VISIBLE);
                plyImg.setVisibility(View.GONE);
            }
        });

        back = findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                info.setVisibility(View.VISIBLE);
                back.setVisibility(View.GONE);
                plyImg.setVisibility(View.VISIBLE);
                info_layout.setVisibility(View.GONE);
            }
        });

        descripcioText = findViewById(R.id.text_description);
        descripcioCanvi = findViewById(R.id.descripcioCanvi);

        nametext = findViewById(R.id.text_newName);

        canvi = findViewById(R.id.canvi);

        scroll = findViewById(R.id.scroll);

        user.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), InfoArtistaActivity.class);
                intent.putExtra("User", playlst.getOwner());
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
            }
        });
    }

    private void sortAZ(){
        if (mTracks != null) {
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
            adapter = new TrackListAdapter(this, this, mTracks, playlst);
            mRecyclerView.setAdapter(adapter);
            mSorted = SORT_AZ;

            PreferenceUtils.saveAllTracks(getApplicationContext(), mTracks);
            PreferenceUtils.savePlaylistOrder(this, SORT_AZ);
            if(player!=null){
                player.novaLlista();
            }
        }
    }

    private void sortTime(){
        if (mTracks != null) {
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
            adapter = new TrackListAdapter(this, this, mTracks, playlst);
            mRecyclerView.setAdapter(adapter);
            mSorted = SORT_TIME;

            PreferenceUtils.saveAllTracks(getApplicationContext(), mTracks);
            PreferenceUtils.savePlaylistOrder(this, SORT_TIME);
            if(player!=null){
                player.novaLlista();
            }
        }
    }

    private void sortArtist(){
        if (mTracks != null) {
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
            adapter = new TrackListAdapter(this, this, mTracks, playlst);
            mRecyclerView.setAdapter(adapter);
            mSorted = SORT_ARTIST;

            PreferenceUtils.saveAllTracks(getApplicationContext(), mTracks);
            if(player!=null){
                player.novaLlista();
            }
            PreferenceUtils.savePlaylistOrder(this, SORT_ARTIST);
        }
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
        orderByPreferenceUtils();
        adapter = new TrackListAdapter(this, this, mTracks, playlst);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onTracksReceived(List<Track> tracks) {
        mTracks = (ArrayList<Track>) tracks;
        orderByPreferenceUtils();
        adapter = new TrackListAdapter(this, this, mTracks, playlst);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onNoTracks(Throwable throwable) {

    }

    @Override
    public void onPersonalTracksReceived(List<Track> tracks) {

    }

    @Override
    public void onPersonalLikedTracksReceived(List<Track> tracks) {

    }

    @Override
    public void onUserTracksReceived(List<Track> tracks) {

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

    private int trackById(int id){
        int valor = 0;
        for(int i=0; i<mTracks.size() ;i++){
            if(mTracks.get(i).getId()==id){
                valor = i;
            }
        }
        return valor;
    }

    @Override
    public void onTrackLiked(int id) {
        if(mTracks.get(trackById(id)).isLiked()){
            mTracks.get(trackById(id)).setLiked(false);
        }else{
            mTracks.get(trackById(id)).setLiked(true);
        }
    }

    @Override
    public void onTrackNotFound(Throwable throwable) {

    }

    @Override
    public void onTrackUpdated(Track body) {

    }

    @Override
    public void onTrackUpdateFailure(Throwable throwable) {

    }

    @Override
    public void onTrackDeleted(int id) {

    }

    @Override
    public void onTrackReceived(Track track) {

    }

    @Override
    public void onMyTracksFailure(Throwable throwable) {

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
        saveIdForFuture();
        Playlist pl;
        if(p!=null){
            pl =  p;
        }else{
            pl = new Playlist("UserPlaylist", Session.getUser());
        }
        InfoTrackFragment bottomSheetDialog = new InfoTrackFragment(tracks.get(position), pl, Session.getUser());
        bottomSheetDialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
        bottomSheetDialog.show(getSupportFragmentManager(), "track_info");
    }

    @Override
    public void onTrackSelectedLiked(int position) {
        trackManager.likeTrack(mTracks.get(position).getId(), PlaylistActivity.this);
    }


    @Override
    public void onPlaylistCreated(Playlist playlist) {

    }

    @Override
    public void onPlaylistFailure(Throwable throwable) {

    }

    @Override
    public void onPlaylistRecieved(List<Playlist> playlists) {
        mTracks = (ArrayList<Track>) playlists.get(0).getTracks();
        orderByPreferenceUtils();
        adapter = new TrackListAdapter(this, this, mTracks, playlst);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onNoPlaylists(Throwable throwable) {

    }

    @Override
    public void onPlaylistSelected(Playlist playlist) {

    }

    @Override
    public void onPlaylistToUpdated(Playlist body) {
        description.setText(body.getDescription());

        newName.setText(body.getName());
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

    @Override
    public void onPlaylistRecived(Playlist playlist) {
        playlst = playlist;
        mTracks = (ArrayList<Track>) playlist.getTracks();
        adapter = new TrackListAdapter(this, this, mTracks, playlst);
        descripcioCanvi.setText(playlist.getDescription());
        mRecyclerView.setAdapter(adapter);
    }



    private void orderByPreferenceUtils(){
        if (PreferenceUtils.getLastPlaylistID(this) == playlst.getId()) {
            mSorted = PreferenceUtils.getPlaylistOrder(this);
            switch (mSorted) {
                case SORT_AZ:
                    sortAZ();
                    break;
                case SORT_TIME:
                    sortTime();
                    break;
                case SORT_ARTIST:
                    sortArtist();
                    break;
                default:
                    System.out.println("mSorted = " + mSorted);
            }
        }
    }

    private void saveIdForFuture(){
        PreferenceUtils.saveLastPlaylistID(this, playlst.getId());
    }

    @Override
    public void onPlaylistDeleted(Playlist body) {
        finish();
        overridePendingTransition(R.anim.nothing,R.anim.nothing);
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
    public void onDelete() {
        dialogEdit.cancelDialog();
        dialogEdit.showConfirmationDialog("Are you sure you want to delete this playlist?");
    }

    private void hideUIEdit(){
        newName.setVisibility(View.GONE);
        plyName.setVisibility(View.VISIBLE);
        plyName.setText(playlst.getName());
        mRecyclerView.setVisibility(View.VISIBLE);
        plyImg.setVisibility(View.VISIBLE);
        back2Main.setVisibility(View.VISIBLE);
        infoPlaylist.setVisibility(View.VISIBLE);
        shuffle.setVisibility(View.VISIBLE);
        addBunch.setVisibility(View.VISIBLE);
        acceptEdit.setVisibility(View.GONE);
        navigation.setVisibility(View.VISIBLE);
        actionButtons.setVisibility(View.VISIBLE);
        reproductor.setVisibility(View.VISIBLE);
        mseek.setVisibility(View.VISIBLE);
        canvi.setVisibility(View.GONE);
        plyAuthor.setVisibility(View.VISIBLE);
        followers.setVisibility(View.VISIBLE);
        scroll.setVisibility(View.VISIBLE);

        if(player!=null){
            player.updateUI();
        }else{
            play.setVisibility(View.VISIBLE);
            pause.setVisibility(View.INVISIBLE);
        }
        accessible.setVisibility(View.VISIBLE);

        if(!newName.getText().toString().matches("")){
            playlst.setName(newName.getText().toString());
        }

        if(!descripcioCanvi.getText().toString().matches("")){
            playlst.setName(descripcioCanvi.getText().toString());
        }

        KeyboardUtils.hideKeyboard(this);
    }

    public void showUIEdit() {
        newName.setVisibility(View.VISIBLE);
        plyName.setVisibility(View.GONE);
        newName.setText(playlst.getName());
        mRecyclerView.setVisibility(View.INVISIBLE);
        plyImg.setVisibility(View.GONE);
        back2Main.setVisibility(View.INVISIBLE);
        infoPlaylist.setVisibility(View.INVISIBLE);
        shuffle.setVisibility(View.GONE);
        addBunch.setVisibility(View.GONE);
        acceptEdit.setVisibility(View.VISIBLE);
        descripcioText.setVisibility(View.VISIBLE);
        descripcioCanvi.setVisibility(View.VISIBLE);
        navigation.setVisibility(View.GONE);
        actionButtons.setVisibility(View.GONE);
        reproductor.setVisibility(View.GONE);
        mseek.setVisibility(View.GONE);
        play.setVisibility(View.GONE);
        pause.setVisibility(View.GONE);
        accessible.setVisibility(View.INVISIBLE);
        plyAuthor.setVisibility(View.GONE);
        followers.setVisibility(View.GONE);
        canvi.setVisibility(View.VISIBLE);
        scroll.setVisibility(View.GONE);
    }


    @Override
    public void onEdit() {
        showUIEdit();
        dialogEdit.cancelDialog();
    }

    @Override
    public void onAccept() {
        dialogEdit.cancelDialog();
        pManager.deletePlaylist(playlst.getId(), this);
    }

    @Override
    public void onCancel() {
        dialogEdit.cancelDialog();
    }

}
