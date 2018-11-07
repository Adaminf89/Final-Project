package com.example.adaminfiesto.droppit.DataModels;

public class Trending
{
    private String pic_id;
    private String user_id;

    public Trending(String user_id, String pic_id)
    {
        this.pic_id = pic_id;
        this.user_id = user_id;
    }

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getPic_id() {
        return pic_id;
    }

    public void setPic_id(String pic_id) {
        this.pic_id = pic_id;
    }

    @Override
    public String toString()
    {
        return "Trending{" +
                "pic_id='" + pic_id + '\'' +
                "user_id='" + user_id + '\'' +
                '}';
    }
}
