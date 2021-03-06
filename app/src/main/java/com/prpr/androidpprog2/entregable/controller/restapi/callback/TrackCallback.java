package com.prpr.androidpprog2.entregable.controller.restapi.callback;

import com.prpr.androidpprog2.entregable.model.Track;

import java.util.List;


public interface TrackCallback extends FailureCallback {
    void onTracksReceived(List<Track> tracks);
    void onNoTracks(Throwable throwable);
    void onPersonalTracksReceived(List<Track> tracks);
    void onPersonalLikedTracksReceived(List<Track> tracks);
    void onUserTracksReceived(List<Track> tracks);
    void onCreateTrack(Track t);
    void onTopTracksRecieved(List<Track> tracks);
    void onNoTopTracks(Throwable throwable);
    void onTrackLiked(int id);
    void onTrackNotFound(Throwable throwable);
    void onTrackUpdated(Track body);
    void onTrackUpdateFailure(Throwable throwable);
    void onTrackDeleted(int id);
    void onTrackReceived(Track track);
    void onMyTracksFailure(Throwable throwable);
}
