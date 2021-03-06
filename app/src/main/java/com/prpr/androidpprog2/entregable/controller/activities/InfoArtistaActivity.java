package com.prpr.androidpprog2.entregable.controller.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.adapters.PlaylistAdapter;
import com.prpr.androidpprog2.entregable.controller.adapters.TrackListAdapter;
import com.prpr.androidpprog2.entregable.controller.callbacks.TrackListCallback;
import com.prpr.androidpprog2.entregable.controller.dialogs.ErrorDialog;
import com.prpr.androidpprog2.entregable.controller.fragments.InfoTrackFragment;
import com.prpr.androidpprog2.entregable.controller.fragments.ShareTrackFragment;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.PlaylistCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.TrackCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.UserCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.PlaylistManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.TrackManager;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.UserManager;
import com.prpr.androidpprog2.entregable.controller.music.ReproductorService;
import com.prpr.androidpprog2.entregable.model.Follow;
import com.prpr.androidpprog2.entregable.model.Playlist;
import com.prpr.androidpprog2.entregable.model.Track;
import com.prpr.androidpprog2.entregable.model.User;
import com.prpr.androidpprog2.entregable.model.UserToken;
import com.prpr.androidpprog2.entregable.model.passwordChangeDto;
import com.prpr.androidpprog2.entregable.utils.Constants;
import com.prpr.androidpprog2.entregable.utils.PreferenceUtils;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class InfoArtistaActivity extends AppCompatActivity implements TrackListCallback, TrackCallback, PlaylistCallback, UserCallback {



    private Button back;
    private RecyclerView topSongsRecycle;
    private RecyclerView playlistByArtistRecycle;
    private RecyclerView allSongsRecycle;

    private ArrayList<Track> artTracks;
    private ArrayList<Playlist> artPlaylist;
    private User artist;
    private TextView name;
    private TextView login;
    private TextView topSongs;
    private TextView plists;
    private TextView songs;
    private Button follow;
    private ImageButton buttonShare;
    private Follow followingInfo;
    private boolean isFollowing = false;
    private UserManager umanager;
    private ImageView profilePic;

    private ArrayList<User> followers;

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
    private boolean servidorVinculat = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.prpr.androidpprog2.entregable.PlayNewAudio";


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
        if (!servidorVinculat) {
            Intent intent = new Intent(this, ReproductorService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            serv.setUIControls(mSeekBar, trackTitle, trackAuthor, play, pause, im);
            serv.updateUI();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!servidorVinculat) {
            Intent intent = new Intent(this, ReproductorService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            serv.setUIControls(mSeekBar, trackTitle, trackAuthor, play, pause, im);
            serv.updateUI();
        }
    }


    //----------------------------------------------------------------FIN DE LA PART DE SERVICE--------------------------------------------------------------------------------


    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_user);
        artist = (User) getIntent().getSerializableExtra("User");
        umanager = new UserManager(this);
        umanager.checkFollow(artist.getLogin(), this);
        umanager.getFollowers(artist.getLogin(), this);
        initViews();

        TrackManager topmanager = new TrackManager(this);
        topmanager.getTopTracks(artist.getLogin(), this);

        PlaylistManager pmanager = new PlaylistManager(this);
        pmanager.showUserPlaylist(artist.getLogin(), this);

        TrackManager tmanager = new TrackManager(this);
        tmanager.getUserTracks(artist.getLogin(), this);
    }

    private void initViews() {


        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.menu);
        navigation.setSelectedItemId(R.id.none);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        Intent intent0 = new Intent(getApplicationContext(), MainActivity.class);
                        intent0.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(intent0, Constants.NETWORK.LOGIN_OK);
                        return true;
                    case R.id.buscar:
                        Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
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


        buttonShare = findViewById(R.id.button_share);
        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShareTrackFragment bottomSheetDialog = new ShareTrackFragment(null, artist, null);
                bottomSheetDialog.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogTheme);
                bottomSheetDialog.show(getSupportFragmentManager(), "track_info");
            }
        });

        play = findViewById(R.id.playButton);
        play.setEnabled(true);
        play.bringToFront();
        play.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                serv.resumeMedia();
            }
        });
        pause = findViewById(R.id.playPause);
        pause.setEnabled(true);
        pause.bringToFront();
        pause.setOnClickListener(new View.OnClickListener() {
            
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
                if (serv != null && !trackTitle.getText().toString().equals("")) {
                    Intent intent = new Intent(getApplicationContext(), ReproductorActivity.class);
                    startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
                    overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
                } else {
                    ErrorDialog.getInstance(InfoArtistaActivity.this).showErrorDialog("You haven't selected a song yet!");
                }
            }
        });


        back = findViewById(R.id.back2Main);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(R.anim.nothing, R.anim.nothing);
            }
        });

        name = findViewById(R.id.userName);
        String fName = "";
        String lName = "";
        if (artist.getFirstName() != null) {
            fName = artist.getFirstName();
        } else {
            fName = "--";
        }
        if (artist.getLastName() != null) {
            lName = artist.getLastName();
        } else {
            lName = "--";
        }
        String nom = fName + " " + lName;
        name.setText(nom);

        login = findViewById(R.id.userLogin);
        int followers = artist.getFollowers() != null ? artist.getFollowers() : 0 ;
        login.setText(artist.getLogin() + " - " + followers + " Followers");
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FollowersActivity.class);
                intent.putExtra("followers", InfoArtistaActivity.this.followers);
                startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
                overridePendingTransition(R.anim.slide_up, R.anim.slide_down);
            }
        });


        profilePic = (ImageView) findViewById(R.id.profilePic);
        if (artist.getImageUrl() != null && !artist.getImageUrl().isEmpty()) {
            Picasso.get().load(artist.getImageUrl()).into(profilePic, new Callback() {
                @Override
                public void onSuccess() {
                    Bitmap imageBitmap = ((BitmapDrawable) profilePic.getDrawable()).getBitmap();
                    RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(getResources(), imageBitmap);
                    imageDrawable.setCircular(true);
                    imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                    profilePic.setImageDrawable(imageDrawable);
                }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load("https://user-images.githubusercontent.com/48185184/77792597-e939a400-7068-11ea-8ade-cd8b4e4ab7c9.png").into(profilePic);
                }
            });
        } else {
            Picasso.get().load("https://user-images.githubusercontent.com/48185184/77792597-e939a400-7068-11ea-8ade-cd8b4e4ab7c9.png").into(profilePic, new Callback() {
                @Override
                public void onSuccess() {
                    Bitmap imageBitmap = ((BitmapDrawable) profilePic.getDrawable()).getBitmap();
                    RoundedBitmapDrawable imageDrawable = RoundedBitmapDrawableFactory.create(getResources(), imageBitmap);
                    imageDrawable.setCircular(true);
                    imageDrawable.setCornerRadius(Math.max(imageBitmap.getWidth(), imageBitmap.getHeight()) / 2.0f);
                    profilePic.setImageDrawable(imageDrawable);
                }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load("https://user-images.githubusercontent.com/48185184/77792597-e939a400-7068-11ea-8ade-cd8b4e4ab7c9.png").into(profilePic);
                }
            });
        }


        follow = (Button) findViewById(R.id.followUser);
        follow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                umanager.startStopFollowing(artist.getLogin(), InfoArtistaActivity.this);
            }
        });

        topSongsRecycle = (RecyclerView) findViewById(R.id.topSongsRecycle);
        LinearLayoutManager man = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        TrackListAdapter topadapter = new TrackListAdapter(this, this, artTracks, null);
        topSongsRecycle.setLayoutManager(man);
        topSongsRecycle.setAdapter(topadapter);

        playlistByArtistRecycle = (RecyclerView) findViewById(R.id.playlistByArtistRecycle);
        LinearLayoutManager manager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false);
        PlaylistAdapter pladapter = new PlaylistAdapter(this, artPlaylist);
        playlistByArtistRecycle.setLayoutManager(manager);
        playlistByArtistRecycle.setAdapter(pladapter);

        allSongsRecycle = (RecyclerView) findViewById(R.id.allSongsRecycle);
        LinearLayoutManager manager2 = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        TrackListAdapter adapter = new TrackListAdapter(this, this, artTracks, null);
        allSongsRecycle.setLayoutManager(manager2);
        allSongsRecycle.setAdapter(adapter);

    }


    private void playAudio(int audioIndex) {

        PreferenceUtils.saveAllTracks(getApplicationContext(), artTracks);
        PreferenceUtils.saveTrack(getApplicationContext(), artTracks.get(audioIndex));
        PreferenceUtils.savePlayID(getApplicationContext(), -6);

        if (!servidorVinculat) {
            Intent playerIntent = new Intent(this, ReproductorService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        } else {
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
        trackTitle.setText(artTracks.get(audioIndex).getName());
        trackAuthor.setText(artTracks.get(audioIndex).getUserLogin());
    }


    @Override
    public void onTrackSelected(int index) {
        pause.setVisibility(View.VISIBLE);
        play.setVisibility(View.INVISIBLE);
        playAudio(index);
    }

    @Override
    public void onTrackAddSelected(int position, ArrayList<Track> tracks, Playlist playlist) {
        Intent intent = new Intent(getApplicationContext(), InfoTrackFragment.class);
        intent.putExtra("Trck", tracks.get(position));
        intent.putExtra("Playlst", playlist);
        startActivityForResult(intent, Constants.NETWORK.LOGIN_OK);
    }

    @Override
    public void onTrackSelectedLiked(int position) {

    }

    @Override
    public void onTracksReceived(List<Track> tracks) {

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
        this.artTracks = (ArrayList) tracks;
        if (tracks.size() == 0) {
            songs = findViewById(R.id.noSongsAvailable);
            songs.setVisibility(View.VISIBLE);
        } else {
            TrackListAdapter trackListAdapter = new TrackListAdapter(this, this, this.artTracks, null);
            allSongsRecycle.setAdapter(trackListAdapter);
        }
    }

    @Override
    public void onCreateTrack(Track t) {

    }

    @Override
    public void onTopTracksRecieved(List<Track> tracks) {
        this.artTracks = (ArrayList) tracks;
        if (tracks.size() == 0) {
            topSongs = findViewById(R.id.noTopAvailable);
            topSongs.setVisibility(View.VISIBLE);
        } else {
            TrackListAdapter trackListAdapter = new TrackListAdapter(this, this, this.artTracks, null);
            topSongsRecycle.setAdapter(trackListAdapter);
        }
    }

    @Override
    public void onNoTopTracks(Throwable throwable) {

    }

    @Override
    public void onTrackLiked(int id) {

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
        this.artPlaylist = (ArrayList) body;
        if (body.size() == 0) {
            plists = findViewById(R.id.noPlistAvailable);
            plists.setVisibility(View.VISIBLE);
        } else {
            PlaylistAdapter padapt = new PlaylistAdapter(this, this.artPlaylist);
            padapt.setPlaylistCallback(this);
            playlistByArtistRecycle.setAdapter(padapt);
        }
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
        followingInfo = body;
        if (followingInfo.isFollowing()) {
            follow.setText("Following");
            follow.setBackgroundResource(R.drawable.rectangle_small_gborder_green);
            ;
            isFollowing = false;
        } else {
            follow.setText("Follow");
            follow.setBackgroundResource(R.drawable.rectangle_small_gborder_black);
            ;
            isFollowing = true;
        }
    }

    @Override
    public void onAccountSavedFailure(Throwable throwable) {

    }

    @Override
    public void onFollowFailure(Throwable throwable) {

    }

    @Override
    public void onCheckSuccess(Follow body) {
        followingInfo = body;
        if (followingInfo.isFollowing()) {
            follow.setText("Following");
            follow.setBackgroundResource(R.drawable.rectangle_small_gborder_green);
            ;
            isFollowing = false;
        } else {
            follow.setText("Follow");
            follow.setBackgroundResource(R.drawable.rectangle_small_gborder_black);
            ;
            isFollowing = true;
        }
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
        this.followers = body;
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
}
