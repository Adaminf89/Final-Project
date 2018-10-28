package com.example.adaminfiesto.droppit.DataModels;

public class Like {

    private String user_id;
    private Double rating;

    public Like(String user_id, Double rating) {
        this.user_id = user_id;
        this.rating = rating;
    }

    public Like() {

    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    //pretty much whats getting written to the jsonfile going to the database
    @Override
    public String toString() {
        return "Like{" +
                "user_id='" + user_id + '\'' +
                "rating='" + rating + '\'' +
                '}';
    }
}
