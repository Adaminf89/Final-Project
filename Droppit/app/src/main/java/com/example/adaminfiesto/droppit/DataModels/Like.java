package com.example.adaminfiesto.droppit.DataModels;

public class Like {

    private String user_id;
    private Integer rating;

    public Like(String user_id, Integer rating) {
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
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
