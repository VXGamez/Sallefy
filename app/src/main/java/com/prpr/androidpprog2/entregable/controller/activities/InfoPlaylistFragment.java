package com.prpr.androidpprog2.entregable.controller.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.icu.text.IDNA;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.downloader.Error;
import com.downloader.OnCancelListener;
import com.downloader.OnDownloadListener;
import com.downloader.OnPauseListener;
import com.downloader.OnProgressListener;
import com.downloader.OnStartOrResumeListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.callbacks.DownloadCallback;
import com.prpr.androidpprog2.entregable.controller.dialogs.ErrorDialog;
import com.prpr.androidpprog2.entregable.controller.dialogs.LoadingDialog;
import com.prpr.androidpprog2.entregable.model.DB.ObjectBox;
import com.prpr.androidpprog2.entregable.model.DB.SavedPlaylist;
import com.prpr.androidpprog2.entregable.model.DB.SavedTrack;
import com.prpr.androidpprog2.entregable.model.DB.UtilFunctions;
import com.prpr.androidpprog2.entregable.model.Playlist;
import com.prpr.androidpprog2.entregable.utils.Constants;
import com.prpr.androidpprog2.entregable.utils.Session;
import com.squareup.picasso.Picasso;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class InfoPlaylistFragment extends BottomSheetDialogFragment implements DownloadCallback {

    private Playlist playlist;
    private ImageView playlistCover;
    private TextView playlistName;
    private TextView playlistArtist;
    private LinearLayout layoutArtist;
    private LinearLayout layoutedit;
    private LinearLayout layoutdelete;
    private Switch download;
    private int i = 0;
    private Boolean switchState;
    private int[] downloadId;
    private LoadingDialog loading;
    private SavedPlaylist p;


    public InfoPlaylistFragment(Playlist p) {
        playlist = p;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        View view =  inflater.inflate(R.layout.fragment_playlist_info, container, false);

        loading = new LoadingDialog(getContext());

        playlistCover = view.findViewById(R.id.playlist_img);
        if (playlist.getThumbnail() != null) {
            Picasso.get().load(playlist.getThumbnail()).into(playlistCover);
        }else{
            Picasso.get().load("https://community.spotify.com/t5/image/serverpage/image-id/25294i2836BD1C1A31BDF2/image-size/original?v=mpbl-1&px=-1").into(playlistCover);
        }
        playlistName = view.findViewById(R.id.playlistName);
        playlistName.setText(playlist.getName());
        playlistArtist = view.findViewById(R.id.ArtistName);
        playlistArtist.setText(playlist.getUserLogin());

        layoutdelete = view.findViewById(R.id.layoutEliminar);
        layoutdelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((PlaylistActivity)getActivity()).onDelete();
            }
        });

        layoutedit = view.findViewById(R.id.layoutedit);
        layoutedit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Session.getInstance(((PlaylistActivity)getActivity()).getApplicationContext()).getUser().getLogin().equals(playlist.getUserLogin())) {
                    dismiss();
                    ((PlaylistActivity)getActivity()).showUIEdit();
                }else{
                    ErrorDialog.getInstance(((PlaylistActivity)getActivity())).showErrorDialog("This playlist is not yours to edit");
                }
            }
        });

        layoutArtist = view.findViewById(R.id.layoutUser);
        layoutArtist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(Session.getInstance(((PlaylistActivity)getActivity()).getApplicationContext()).getUser().getLogin().equals(playlist.getUserLogin())) {
                    ErrorDialog.getInstance(((PlaylistActivity)getActivity())).showErrorDialog("You cannot check yourself out!");
                }else{
                    Intent intent = new Intent(((PlaylistActivity)getActivity()), InfoArtistaActivity.class);
                    intent.putExtra("User", playlist.getOwner());
                    startActivity(intent);
                }
            }
        });
        downloadId = new int [playlist.getTracks().size()*2];
        download = (Switch) view.findViewById(R.id.simpleSwitch);
        if(UtilFunctions.playlistExistsInDatabase(playlist)){
            download.setChecked(true);
        }
        switchState = download.isChecked();
        download.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    loading.showLoadingDialog("Downloading "+playlist.getName());
                    File path= getActivity().getFilesDir();
                    p = new SavedPlaylist();
                    p.setId(playlist.getId());
                    try {
                        p.setPlaylist(p.savePlaylist(playlist));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    p.setCoverPath(path.toString() + "/Sallefy/covers/playlists/"+ playlist.getName() + "--" + playlist.getUserLogin()+".jpeg");

                    i=0;
                    if(playlist.getThumbnail()!=null){
                        int id = PRDownloader.download(playlist.getThumbnail(), path.toString() + "/Sallefy/covers/playlists/", playlist.getName() + "--" + playlist.getUserLogin()+".jpeg")
                                .build()
                                .start(new OnDownloadListener() {
                                    @Override
                                    public void onDownloadComplete() {
                                        System.out.println("Finished: " + playlist.getTracks().get(i).getName());
                                    }
                                    @Override
                                    public void onError(Error error) {
                                        System.out.println("Error en descarrega");
                                        System.out.println(error.getServerErrorMessage());
                                    }
                                });
                    }
                    try {
                        doNext();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }else{
                    PRDownloader.cancelAll();
                    UtilFunctions.deleteFiles(ObjectBox.get().boxFor(SavedPlaylist.class).get(playlist.getId()).coverPath);
                    ObjectBox.get().boxFor(SavedPlaylist.class).remove(playlist.getId());

                    for(int i=0; i<playlist.getTracks().size() ;i++){
                        if(UtilFunctions.trackInPlaylistTotal(playlist.getTracks().get(i))==0){
                            if(ObjectBox.get().boxFor(SavedTrack.class).get(playlist.getTracks().get(i).getId()).coverPath!=null){
                                UtilFunctions.deleteFiles(ObjectBox.get().boxFor(SavedTrack.class).get(playlist.getTracks().get(i).getId()).coverPath);
                            }
                            UtilFunctions.deleteFiles(ObjectBox.get().boxFor(SavedTrack.class).get(playlist.getTracks().get(i).getId()).trackPath);
                            ObjectBox.get().boxFor(SavedTrack.class).remove(playlist.getTracks().get(i).getId());
                        }
                    }
                    //Delete files created
                    //Delete from database
                }

            }
        });


        return view;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void progressChanged(Progress progress) {
        /*Intent intent = new Intent();
        final PendingIntent pendingIntent = PendingIntent.getActivity(
                getContext(), 0, intent, 0);

        NotificationManager notificationManager =
                (NotificationManager) getActivity().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(getContext())
                        .setSmallIcon(R.drawable.noti_icon)
                        .setContentTitle(playlist.getName());

        mBuilder.setContentIntent(pendingIntent);
        notificationManager = (NotificationManager) getActivity().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int p = (int) ((double) progress.currentBytes * 100 / progress.totalBytes);
        mBuilder.setProgress((int)progress.totalBytes, p, false);
        notificationManager.notify(101, mBuilder.build());*/
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void doNext() throws IOException {
        File path= getActivity().getFilesDir();
        String a = path.getAbsolutePath();


        SavedTrack t = new SavedTrack();
        t.setId(playlist.getTracks().get(i).getId());
        t.setTrackPath(path.toString() + "/Sallefy/tracks/"+playlist.getTracks().get(i).getName() + "--" + playlist.getTracks().get(i).getUserLogin());
        t.setTrack(t.saveTrack(playlist.getTracks().get(i)));
        t.setCoverPath(path.toString() + "/Sallefy/covers/tracks/"+playlist.getTracks().get(i).getName()+".jpeg");

        ObjectBox.get().boxFor(SavedTrack.class).attach(t);
        t.playlist.add(p);
        ObjectBox.get().boxFor(SavedPlaylist.class).attach(p);
        p.tracks.add(t);

        downloadId[i] = PRDownloader.download(playlist.getTracks().get(i).getUrl(), path.toString() + "/Sallefy/tracks/", playlist.getTracks().get(i).getName() + "--" + playlist.getTracks().get(i).getUserLogin())
                .build()
                .setOnStartOrResumeListener(new OnStartOrResumeListener() {
                    @Override
                    public void onStartOrResume() {
                        System.out.println("Started: " + playlist.getTracks().get(i).getName());
                    }
                })
                .setOnProgressListener(new OnProgressListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onProgress(Progress progress) {
                        InfoPlaylistFragment.this.progressChanged(progress);
                    }
                })
                .start(new OnDownloadListener() {
                    @Override
                    public void onDownloadComplete() {
                        System.out.println("Finished: " + playlist.getTracks().get(i).getName());
                        if(i<playlist.getTracks().size()-1){
                            i++;
                            try {
                                InfoPlaylistFragment.this.doNext();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{
                            loading.cancelLoadingDialog();
                            ObjectBox.get().boxFor(SavedPlaylist.class).put(p);
                        }
                        ObjectBox.get().boxFor(SavedTrack.class).put(t);
                    }

                    @Override
                    public void onError(Error error) {
                        System.out.println("Error en descarrega");
                        System.out.println(error.getServerErrorMessage());
                    }
                });
        if(playlist.getTracks().get(i).getThumbnail()!=null){
            downloadId[(playlist.getTracks().size()-1)+i] = PRDownloader.download(playlist.getTracks().get(i).getThumbnail(), path.toString() + "/Sallefy/covers/tracks/", playlist.getTracks().get(i).getName() + "--" + playlist.getTracks().get(i).getUserLogin()+".jpeg")
                    .build()
                    .start(new OnDownloadListener() {
                        @Override
                        public void onDownloadComplete() {
                            System.out.println("Finished: " + playlist.getTracks().get(i).getName());
                        }
                        @Override
                        public void onError(Error error) {
                            System.out.println("Error en descarrega");
                            System.out.println(error.getServerErrorMessage());
                        }
                    });
        }

    }


}
