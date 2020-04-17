package com.example.project_katanemimena_2;

public class MusicFile {
    private String trackName;
    private String artistName;
    private String albumInfo;
    private String genre;
    private byte[] musicFileExtract;

    public MusicFile(String trackName,String artistName,String albumInfo,byte[] songData){
        this.trackName = trackName;
        this.artistName = artistName;
        this.albumInfo = albumInfo;
        musicFileExtract = songData;
    }

    public void setTrackName(String trackName){
        this.trackName=trackName;
    }

    public void setArtistName(String artistName){
        this.artistName=artistName;
    }

    public void setAlbumInfo(String albumInfo){
        this.albumInfo=albumInfo;
    }
    public void setGenre(String genre){
        this.genre=genre;
    }
    public void setMusicFileExtract(byte [] musicFileExtract){
        this.musicFileExtract=musicFileExtract;
    }
    public String  getTrackName(){
        return trackName;
    }

    public String getArtistName(){
        return artistName;
    }

    public String getAlbumInfo(){
        return albumInfo;
    }

    public String getGenre(){
        return genre;
    }

    public byte[] getMusicFileExtract(){
        return musicFileExtract;
    }

    public int getNumOfBytes() {
        return musicFileExtract.length;
    }
}