package com.prpr.androidpprog2.entregable.controller.adapters;


import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.prpr.androidpprog2.entregable.R;
import com.prpr.androidpprog2.entregable.controller.activities.PlaylistActivity;
import com.prpr.androidpprog2.entregable.controller.dialogs.ErrorDialog;
import com.prpr.androidpprog2.entregable.controller.dialogs.StateDialog;
import com.prpr.androidpprog2.entregable.controller.restapi.callback.PlaylistCallback;
import com.prpr.androidpprog2.entregable.controller.restapi.manager.PlaylistManager;
import com.prpr.androidpprog2.entregable.model.Follow;
import com.prpr.androidpprog2.entregable.model.Playlist;
import com.prpr.androidpprog2.entregable.model.Track;
import com.prpr.androidpprog2.entregable.utils.Constants;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;


public class Add2PlaylistListAdapter extends RecyclerView.Adapter<Add2PlaylistListAdapter.ViewHolder>  implements PlaylistCallback {

    private static final String TAG = "PlaylistListAdapter";
    private ArrayList<Playlist> playlist;
    private Context mContext;
    private Track trck;
    private PlaylistManager pManager;
    private Playlist actual;


    public Add2PlaylistListAdapter(Context context, ArrayList<Playlist> playlists, Track t, Playlist actual) {
        this.playlist = playlists;
        this.mContext = context;
        this.trck = t;
        this.actual = actual;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_playlist_item, parent, false);
        pManager = new PlaylistManager(mContext);
        return new Add2PlaylistListAdapter.ViewHolder(itemView);
    }

    private boolean existsInPlaylist(List<Track> t, Track track){
        boolean exists = false;
        for(Track tk : t){
            if(tk.getName().equals(track.getName())){
                exists=true;
            }
        }
        return exists;
    }

    private void onPlaylistAdd(Playlist ply, Track trck){
        if(existsInPlaylist(ply.getTracks(), trck)){
            ErrorDialog.getInstance(mContext).showErrorDialog("This song is already in this playlist!");
        }else{
            ply.getTracks().add(trck);
            pManager.updatePlaylist(ply, this);
        }
    }

    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        holder.afegirAqui.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlaylistAdd(playlist.get(position), trck);
            }
        });
        holder.nomPlaylist.setText(playlist.get(position).getName());
        int size = playlist.get(position).getTracks() != null ? playlist.get(position).getTracks().size() : 0 ;
        holder.totalCancons.setText( size + " songs");
        if (playlist.get(position).getThumbnail() != null) {
            Picasso.get().load(playlist.get(position).getThumbnail()).into(holder.ivPicture);
        }else{
            Picasso.get().load(R.drawable.default_cover).into(holder.ivPicture);
        }
    }


    @Override
    public int getItemCount() {
        return playlist != null ? playlist.size():0;
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
    public void onPlaylistToUpdated(Playlist body) {
        Intent intent = new Intent(mContext.getApplicationContext(), PlaylistActivity.class);
        intent.putExtra("Playlst", body);
        mContext.startActivity(intent);
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

    public class ViewHolder extends RecyclerView.ViewHolder {

        LinearLayout mLayout;
        TextView nomPlaylist;
        TextView totalCancons;
        Button afegirAqui;
        ImageView ivPicture;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mLayout = itemView.findViewById(R.id.additmm);
            afegirAqui = itemView.findViewById(R.id.addButton);
            nomPlaylist = (TextView) itemView.findViewById(R.id.addplaylist_title);
            totalCancons = (TextView) itemView.findViewById(R.id.addplaylist_songs);
            ivPicture = (ImageView) itemView.findViewById(R.id.playlist_img);
        }
    }
}
