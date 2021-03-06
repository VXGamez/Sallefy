package com.prpr.androidpprog2.entregable.controller.music;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.StrictMode;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.utils.Utils;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaLoadRequestData;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaQueueData;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.gms.cast.MediaTrack;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.images.WebImage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.activities.PlaylistActivity;
import com.prpr.androidpprog2.entregable.controller.activities.SearchActivity;
import com.prpr.androidpprog2.entregable.controller.dialogs.StateDialog;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.TrackManager;
import com.prpr.androidpprog2.entregable.model.DB.ObjectBox;
import com.prpr.androidpprog2.entregable.model.DB.SavedTrack;
import com.prpr.androidpprog2.entregable.model.DB.UtilFunctions;
import com.prpr.androidpprog2.entregable.model.Position;
import com.prpr.androidpprog2.entregable.model.Track;
import com.prpr.androidpprog2.entregable.utils.ConnectivityService;
import com.prpr.androidpprog2.entregable.utils.PreferenceUtils;
import com.prpr.androidpprog2.entregable.utils.Session;
import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;



public class ReproductorService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener, SurfaceHolder.Callback {


    private MediaPlayer mediaPlayer;
    private int resumePosition;
    private AudioManager audioManager;
    private TextView title;
    private TextView artist;
    private ImageView imahen;
    private Button playB;

    private boolean offline = false;
    private boolean reseted = false;

    private Button pauseB;
    private ArrayList<Track> audioList;
    private ArrayList<Track> shuffledAudioList;

    private int currentPlaylistID;
    private TextView textActual;
    private ImageButton shuffle;
    private int audioIndex = -1;
    private Track activeAudio;
    private NotificationCompat.Builder notification;
    private SeekBar mSeekBar;
    private boolean novaLlista;

    private boolean destroyed = false;

    private Context activityContext;

    private FusedLocationProviderClient fusedLocationClient;


    public static final String ACTION_PLAY = "com.prpr.androidpprog2.entregable.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.prpr.androidpprog2.entregable.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.prpr.androidpprog2.entregable.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.prpr.androidpprog2.entregable.ACTION_NEXT";
    public static final String ACTION_STOP = "com.prpr.androidpprog2.entregable.ACTION_STOP";

    public static final String Broadcast_SONG_CHANGED = "com.prpr.androidpprog2.entregable.SONG_CHANGED";

    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaSession mSession;
    private MediaControllerCompat.TransportControls transportControls;

    private final IBinder iBinder = new LocalBinder();

    private static final int NOTIFICATION_ID = 101;

    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    private boolean isShuffle;

    private boolean wasPlaying = false;

    private boolean stopOnStart = false;

    @Override
    public void onCreate() {
        super.onCreate();
        callStateListener();
        registerBecomingNoisyReceiver();
        register_playNewAudio();
        registerConnectionRegained();
        registerConnectionLost();
    }

    private Runnable mProgressRunner = new Runnable() {
        @Override
        public void run() {
            if (mSeekBar != null && mediaPlayer!=null) {
                mSeekBar.setProgress(mediaPlayer.getCurrentPosition());

                if (mediaPlayer.isPlaying()) {
                    mSeekBar.postDelayed(mProgressRunner, 1000);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Error seekbar", Toast.LENGTH_SHORT).show();
            }
        }
    };


    public void setShuffleButtonUI() {
        if (!isShuffle) {
            shuffle.setBackgroundResource(R.drawable.no_shuffle);
            ;
        } else {
            shuffle.setBackgroundResource(R.drawable.si_shuffle);
            ;
        }
    }

    public void setShuffle(boolean valor) {
        isShuffle = valor;
        PreferenceUtils.saveShuffle(getApplicationContext(), isShuffle);
    }

    public void toggleShuffle() {
        if (isShuffle) {
            isShuffle = false;
            shuffle.setBackgroundResource(R.drawable.no_shuffle);
            ;
        } else {
            isShuffle = true;
            shuffle.setBackgroundResource(R.drawable.si_shuffle);
            ;
        }
        PreferenceUtils.saveShuffle(getApplicationContext(), isShuffle);
    }


    public void setmSeekBar(SeekBar s) {
        mSeekBar = s;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.reset();

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        String url="";

        if(UtilFunctions.noInternet(getApplicationContext())){
            offline = true;
            if(UtilFunctions.trackExistsInDatabase(activeAudio)){
                url = ObjectBox.get().boxFor(SavedTrack.class).get(activeAudio.getId()).trackPath;
            }else{
                skipToNext();
            }
        }else{
            offline = false;
            url = activeAudio.getUrl();
        }

        playTrackOnAPI();

        try {
            mediaPlayer.setDataSource(url);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();


    }

    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ){
            ActivityCompat.requestPermissions((Activity) activityContext, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION}, 123);
        }
    }

    private String duractioActual() {
        int duration = mediaPlayer.getCurrentPosition();
        @SuppressLint("DefaultLocale") String time = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
        return time;
    }

    public void setDuracioTotal(TextView txt, TextView txtActual) {
        int duration = mediaPlayer.getDuration();
        @SuppressLint("DefaultLocale") String time = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(duration),
                TimeUnit.MILLISECONDS.toSeconds(duration) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );
        txt.setText(time);
        this.textActual = txtActual;
    }

    public void stopOnStart() {
        stopOnStart = true;
    }

    public void updateAudioList(ArrayList<Track> list){
        if(isShuffle){
            shuffledAudioList = list;
        }else{
            audioList = list;
        }
    }

    public void playTrackOnAPI(){
        if(activityContext!=null){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                checkPermission();
            }
            if(fusedLocationClient==null){
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(activityContext);
            }
            fusedLocationClient.getLastLocation().addOnSuccessListener((Activity)activityContext, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Position p;
                    if (location != null) {
                        p = new Position(location.getLongitude(),location.getLatitude());
                    }else{
                        p = new Position(0,0);
                    }
                    TrackManager.getInstance(getApplicationContext()).playTrack(activeAudio.getId(), p);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    TrackManager.getInstance(getApplicationContext()).playTrack(activeAudio.getId(), new Position(0,0));
                }
            });
        }
    }

    public ArrayList<Track> getAudioList(){
        if(isShuffle){
            return shuffledAudioList;
        }else{
            return audioList;
        }
    }

    public void loadMedia(int position,CastSession castSession,boolean play) {
        if (castSession == null) {
            return;
        }

        final RemoteMediaClient remoteMediaClient = castSession.getRemoteMediaClient();
        if (remoteMediaClient == null) {
            return;
        }

        downVolume();

            remoteMediaClient.load(new MediaLoadRequestData.Builder()
                    .setMediaInfo(buildMediaInfo())
                    .setAutoplay(play)
                    .setCurrentTime(position).build());

    }

    private MediaInfo buildMediaInfo() {

        MediaMetadata musicMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);
        musicMetadata.putString(MediaMetadata.KEY_TITLE, getCurrentTrack().getName());
        musicMetadata.addImage(new WebImage(Uri.parse(getCurrentTrack().getThumbnail())));


        return new MediaInfo.Builder(getCurrentTrack().getUrl())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("audio/mp3")
                .setMetadata(musicMetadata)
                .setStreamDuration(getCurrentTrack().getDuration() * 1000)
                .build();

    }

    /*private MediaInfo buildMediaInfo() {

        MediaMetadata movieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK);

        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, mSelectedMedia.getStudio());
        movieMetadata.putString(MediaMetadata.KEY_TITLE, mSelectedMedia.getTitle());
        movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(0))));
        movieMetadata.addImage(new WebImage(Uri.parse(mSelectedMedia.getImage(1))))

        return new MediaInfo.Builder(mSelectedMedia.getUrl())
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("videos/mp4")
                .setMetadata(movieMetadata)
                .setStreamDuration(mSelectedMedia.getDuration() * 1000)
                .build();
    }*/


    public void setUIControls(SeekBar seekBar, TextView titol, TextView autor, Button play, Button pause, ImageView trackImg) {
        mSeekBar = seekBar;
        title = titol;
        artist = autor;
        playB = play;
        pauseB = pause;
        imahen = trackImg;
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) {
                    if (mediaPlayer.isPlaying()) {
                        mSeekBar.postDelayed(mProgressRunner, 1000);
                    }
                    mSeekBar.setProgress(progress);
                    if (textActual != null) {
                        textActual.setText(duractioActual());
                    }
                    if (stopOnStart) {
                        pauseMedia();
                        stopOnStart = false;
                    }

                } else {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        if (mediaPlayer != null) {
            updateUI();
        }
    }

    public Track getActiveAudio() {
        return activeAudio;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void removeTrack() {
        int oldI = indexTrack(activeAudio);

        for (int i = 0; i < audioList.size(); i++) {
            if (activeAudio.getName().equals(audioList.get(i).getName()) && activeAudio.getUserLogin().equals(audioList.get(i).getUserLogin())) {
                audioList.remove(i);
            }
        }
        for (int i = 0; i < shuffledAudioList.size(); i++) {
            if (activeAudio.getName().equals(shuffledAudioList.get(i).getName()) && activeAudio.getUserLogin().equals(shuffledAudioList.get(i).getUserLogin())) {
                shuffledAudioList.remove(i);
            }
        }

        if (isShuffle) {
            if (oldI == audioList.size() - 1) {
                activeAudio = shuffledAudioList.get(0);
            } else {
                activeAudio = shuffledAudioList.get(oldI);
            }
        } else {
            if (oldI == audioList.size() || oldI == audioList.size() - 1) {
                activeAudio = audioList.get(0);
            } else {
                activeAudio = audioList.get(oldI);
            }
        }

        audioIndex = indexTrack(activeAudio);

        PreferenceUtils.saveAllTracks(getApplicationContext(), audioList);
        PreferenceUtils.saveTrack(getApplicationContext(), activeAudio);

        stopMedia();
        mediaPlayer.reset();
        initMediaPlayer();
        updateUI();
        updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);

    }

    public void setRandomButton(ImageButton shuffle) {
        this.shuffle = shuffle;
    }

    private void clearUI() {
        if (mediaPlayer != null && title != null && artist != null) {
            title.setVisibility(View.INVISIBLE);
            artist.setVisibility(View.INVISIBLE);
            mSeekBar.setVisibility(View.INVISIBLE);
            mSeekBar.setVisibility(View.INVISIBLE);
            pauseB.setVisibility(View.INVISIBLE);
            playB.setVisibility(View.INVISIBLE);
        }

    }

    public void updateUI() {
        if (mediaPlayer != null && title != null && artist != null && !reseted) {
            title.setVisibility(View.VISIBLE);
            artist.setVisibility(View.VISIBLE);
            mSeekBar.setVisibility(View.VISIBLE);
            mSeekBar.setVisibility(View.VISIBLE);
            title.setText(activeAudio.getName());
            artist.setText(activeAudio.getUserLogin());
            mProgressRunner.run();
            mSeekBar.setMax(mediaPlayer.getDuration());
            mSeekBar.setProgress(mediaPlayer.getCurrentPosition());
            if (mediaPlayer.isPlaying()) {
                pauseB.setVisibility(View.VISIBLE);
                playB.setVisibility(View.INVISIBLE);
            } else {
                pauseB.setVisibility(View.INVISIBLE);
                playB.setVisibility(View.VISIBLE);
            }
            if (imahen != null) {
                if(UtilFunctions.noInternet(getApplicationContext())){
                    if(UtilFunctions.trackExistsInDatabase(activeAudio)){
                        SavedTrack p = ObjectBox.get().boxFor(SavedTrack.class).get(activeAudio.getId());
                        Bitmap myBitmap = BitmapFactory.decodeFile(p.coverPath);
                        imahen.setImageBitmap(myBitmap);
                    }else{
                        Picasso.get().load(R.drawable.default_track_cover).into(imahen);
                    }
                }else{
                    if (activeAudio.getThumbnail() != null && !activeAudio.getThumbnail().equals("")) {
                        Picasso.get().load(activeAudio.getThumbnail()).into(imahen);
                    } else {
                        Picasso.get().load(R.drawable.default_track_cover).into(imahen);
                    }
                }
            }
        }else if(reseted){
            clearUI();
        }
    }


    public void killNotification() {
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(ns);
        nMgr.cancel(NOTIFICATION_ID);
    }

    public void seekToPosition(int position) {
        mediaPlayer.seekTo(position);
    }

    private void playMedia() {
        if (!mediaPlayer.isPlaying() && mediaPlayer != null && mSeekBar != null) {
            mediaPlayer.start();
            int duration = mediaPlayer.getDuration();
            mSeekBar.setMax(duration);
            mSeekBar.postDelayed(mProgressRunner, 1000);

        }
        updateUI();

    }


    public void stopMedia() {
        if (mediaPlayer == null){
            return;
        }else{
            mediaPlayer.stop();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void pauseMedia() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                buildNotification(PlaybackStatus.PAUSED);
                resumePosition = mediaPlayer.getCurrentPosition();
            }
            updateUI();
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(resumePosition);
            mProgressRunner.run();
            buildNotification(PlaybackStatus.PLAYING);
            mediaPlayer.start();
        }
        updateUI();
    }

    private void makeShuffled() {
        shuffledAudioList = new ArrayList<>();
        shuffledAudioList.addAll(audioList);
        Collections.shuffle(shuffledAudioList);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void playAudio(){
        int index = PreferenceUtils.getPlayID(getApplicationContext());
        isShuffle = PreferenceUtils.getShuffle(getApplicationContext());
        if (currentPlaylistID != index || shuffledAudioList == null) {
            audioList = PreferenceUtils.getAllTracks(getApplicationContext());
            makeShuffled();
        }
        audioIndex = indexTrack(PreferenceUtils.getTrack(getApplicationContext()));
        if (audioIndex != -1 && audioIndex < audioList.size()) {
            if (isShuffle) {
                activeAudio = shuffledAudioList.get(audioIndex);
            } else {
                activeAudio = audioList.get(audioIndex);
            }
        } else {
            stopSelf();
        }
        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        stopMedia();
        mediaPlayer.reset();
        initMediaPlayer();
        updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);
    }

    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            reseted = false;
           playAudio();
        }
    };


    private void register_playNewAudio() {
        IntentFilter filter = new IntentFilter(PlaylistActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }

    private void registerConnectionRegained() {
        IntentFilter filter = new IntentFilter(ConnectivityService.Broadcast_CONNECTION_REGAINED);
        registerReceiver(connectionRegained, filter);
    }

    private void registerConnectionLost() {
        IntentFilter filter = new IntentFilter(ConnectivityService.Broadcast_CONNECTION_LOST);
        registerReceiver(connectionLost, filter);
    }

    private BroadcastReceiver connectionRegained = new BroadcastReceiver() {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
            if(offline){
                offline = false;
                if(mediaPlayer!=null){
                    stopMedia();
                }
                clearUI();
                reseted = true;
                Toast toast = Toast.makeText(Session.quinaActivityEsta(), "Connection Regained", Toast.LENGTH_LONG);
                View view = toast.getView();
                view.getBackground().setColorFilter(Color.parseColor("#21D760"), PorterDuff.Mode.SRC_IN);
                TextView text = view.findViewById(android.R.id.message);
                text.setTextColor(Color.WHITE);
                text.setTypeface(text.getTypeface(), Typeface.BOLD);
                toast.show();
            }
        }
    };



    private BroadcastReceiver connectionLost = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            if(!offline){
                offline = true;
                reseted = true;
                if(mediaPlayer!=null){
                    stopMedia();
                }
                clearUI();
                Toast toast = Toast.makeText(Session.quinaActivityEsta(), "Connection Lost", Toast.LENGTH_LONG);
                View view = toast.getView();
                view.getBackground().setColorFilter(Color.parseColor("#21D760"), PorterDuff.Mode.SRC_IN);
                TextView text = view.findViewById(android.R.id.message);
                text.setTextColor(Color.WHITE);
                text.setTypeface(text.getTypeface(), Typeface.BOLD);
                toast.show();
            }

        }
    };

    private void initMediaSession() throws RemoteException {
        if (mediaSessionManager != null) return;
        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        mSession = new MediaSession(getApplicationContext(), "Sallefy");
        mediaSession = new MediaSessionCompat(getApplicationContext(), "Sallefy");
        transportControls = mediaSession.getController().getTransportControls();
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        updateMetaData();
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onPlay() {
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onPause() {
                super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    public Track getCurrentTrack() {
        if(activeAudio==null){
            if(isShuffle){
                return shuffledAudioList.get(audioIndex);
            }else{
                return audioList.get(audioIndex);
            }
        }else{
          return activeAudio;
        }
    }

    private void updateMetaData() {
        Bitmap albumArt;
        String urlString;
        if(UtilFunctions.noInternet(getApplicationContext())){
            if(UtilFunctions.trackExistsInDatabase(activeAudio)){
                SavedTrack p = ObjectBox.get().boxFor(SavedTrack.class).get(activeAudio.getId());
                albumArt = BitmapFactory.decodeFile(p.coverPath);
            }else{
                albumArt = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.default_track_cover);
            }
        }else{
            if (activeAudio != null && activeAudio.getThumbnail() != null) {
                urlString = activeAudio.getThumbnail();
                try {
                    URL url = new URL(urlString);
                    albumArt = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                    albumArt = null;
                }
            } else {
                albumArt = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.default_track_cover);
            }

        }

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getUserLogin())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getName())
                .build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void buildNotification(PlaybackStatus playbackStatus) {
        boolean ongoing = true;
        int notificationAction = R.drawable.ic_pause_white;
        PendingIntent play_pauseAction = null;
        if (playbackStatus == PlaybackStatus.PLAYING) {
            ongoing = true;
            notificationAction = R.drawable.ic_pause_white;
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            ongoing = false;
            notificationAction = R.drawable.ic_play_white;
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon;
        String urlString;
        if(UtilFunctions.noInternet(getApplicationContext())){
            if(UtilFunctions.trackExistsInDatabase(activeAudio)){
                SavedTrack p = ObjectBox.get().boxFor(SavedTrack.class).get(activeAudio.getId());
                largeIcon = BitmapFactory.decodeFile(p.coverPath);
            }else{
                largeIcon = BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.default_track_cover);
            }
        }else{
            if (activeAudio.getThumbnail() != null) {
                urlString = activeAudio.getThumbnail();
            } else {
                urlString = " https://community.spotify.com/t5/image/serverpage/image-id/25294i2836BD1C1A31BDF2/image-size/original?v=mpbl-1&px=-1";
            }
            try {
                URL url = new URL(urlString);
                largeIcon = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
                largeIcon = null;
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel notificationChannel = new NotificationChannel("SALLEFY", "Sallefy", NotificationManager.IMPORTANCE_LOW);
        notificationManager.createNotificationChannel(notificationChannel);


        MediaSessionCompat.Token token = mediaSession.getSessionToken();

        notification = new NotificationCompat.Builder(this, "SALLEFY")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.noti_icon)
                .addAction(R.drawable.ic_skip_previous, "previous", playbackAction(3))
                .addAction(notificationAction, "pause", play_pauseAction)
                .addAction(R.drawable.ic_skip_next, "next", playbackAction(2))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1)
                        .setMediaSession(token))
                .setContentTitle(activeAudio.getName())
                .setContentText(activeAudio.getUserLogin())
                .setLargeIcon(largeIcon)
                .setOngoing(ongoing);


        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notification.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, ReproductorService.class);
        switch (actionNumber) {
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        updateUI();
        return null;
    }


    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
        updateUI();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void skipToNext() {
        if (isShuffle) {
            if (indexTrack(activeAudio) == shuffledAudioList.size() - 1) {
                activeAudio = shuffledAudioList.get(0);
            } else {
                int index = indexTrack(activeAudio);
                activeAudio = shuffledAudioList.get(++index);
            }
        } else {
            if (indexTrack(activeAudio) == audioList.size() - 1) {
                activeAudio = audioList.get(0);
            } else {
                int index = indexTrack(activeAudio) + 1;
                activeAudio = audioList.get(index);
            }
        }
        audioIndex = indexTrack(activeAudio);

        PreferenceUtils.saveTrack(getApplicationContext(), activeAudio);

        stopMedia();
        mediaPlayer.reset();
        initMediaPlayer();
        updateUI();
        updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);

        Intent broadcastIntent = new Intent(Broadcast_SONG_CHANGED);
        sendBroadcast(broadcastIntent);
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public void skipToPrevious() {
        audioIndex = indexTrack(activeAudio);
        if (isShuffle) {
            if (audioIndex == 0) {
                audioIndex = shuffledAudioList.size() - 1;
                activeAudio = shuffledAudioList.get(audioIndex);
            } else {
                activeAudio = shuffledAudioList.get(--audioIndex);
            }
        } else {
            if (audioIndex == 0) {
                audioIndex = audioList.size() - 1;
                activeAudio = audioList.get(audioIndex);
            } else {
                activeAudio = audioList.get(--audioIndex);
            }
        }

        audioIndex = indexTrack(activeAudio);
        PreferenceUtils.saveTrack(getApplicationContext(), activeAudio);

        stopMedia();
        mediaPlayer.reset();
        initMediaPlayer();
        updateUI();
        updateMetaData();
        buildNotification(PlaybackStatus.PLAYING);
    }


    private int indexTrack(Track t) {
        int index = -1;
        if (isShuffle) {
            for (int i = 0; i < shuffledAudioList.size(); i++) {
                if (t.equals(shuffledAudioList.get(i))) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < audioList.size(); i++) {
                if (t.equals(audioList.get(i))) {
                    return i;
                }
            }
        }

        return index;
    }


    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            audioList = PreferenceUtils.getAllTracks(getApplicationContext());
            Track t = PreferenceUtils.getTrack(getApplicationContext());
            audioIndex = indexTrack(t);
            isShuffle = PreferenceUtils.getShuffle(getApplicationContext());
            currentPlaylistID = PreferenceUtils.getPlayID(getApplicationContext());

            if(activityContext!=null){
                fusedLocationClient = LocationServices.getFusedLocationProviderClient(activityContext);

            }


            if (audioIndex != -1 && audioIndex < audioList.size()) {
                if (t != null) {
                    activeAudio = t;
                    makeShuffled();
                } else {
                    activeAudio = audioList.get(audioIndex);
                }
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }
        novaLlista = false;
        if (requestAudioFocus() == false) {
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }

        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(!destroyed){
            if (mediaPlayer != null) {
                stopMedia();
                mediaPlayer.release();
                mediaPlayer = null;
            }
            removeAudioFocus();
            if (phoneStateListener != null) {
                telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            }
            removeNotification();
            killNotification();
            //try {
            unregisterReceiver(becomingNoisyReceiver);
            unregisterReceiver(connectionLost);
            unregisterReceiver(connectionRegained);
            unregisterReceiver(playNewAudio);
            destroyed = true;
        }

        //} catch(IllegalArgumentException e) {
        //}

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        onDestroy();
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mp.getCurrentPosition() != 0) {
            skipToNext();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    public void novaLlista() {
        novaLlista = true;
        newOrder();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onAudioFocusChange(int focusState) {
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying() && wasPlaying) {
                    mediaPlayer.start();
                    buildNotification(PlaybackStatus.PLAYING);
                }
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    wasPlaying = true;
                    buildNotification(PlaybackStatus.PAUSED);
                } else {
                    wasPlaying = false;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    public void downVolume(){
        mediaPlayer.setVolume(0.0f,0.0f);
    }

    public void upVolume(){
        mediaPlayer.setVolume(1.0f,1.0f);
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return true;
        }
        return false;
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

    public void setMainActivity(Context context) {
        this.activityContext = context;
    }

    public void addToQueue(Track track) {
        int index = 0;
        if(audioIndex>0){
            index = audioIndex-1;
        }
        if(isShuffle){
            shuffledAudioList.add(index, track);
        }else{
            audioList.add(index, track);
        }
    }


    public class LocalBinder extends Binder {
        public ReproductorService getService() {
            return ReproductorService.this;
        }
    }

    private void callStateListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }


    private void newOrder() {
        if (novaLlista && !isShuffle) {
            audioList = PreferenceUtils.getAllTracks(getApplicationContext());
            audioIndex = indexTrack(activeAudio);
            novaLlista = false;
        }
        if (novaLlista && isShuffle) {
            novaLlista = false;
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer.setDisplay(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    public boolean isOffline() {
        return offline;
    }
}
