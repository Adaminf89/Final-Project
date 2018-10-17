package com.example.adaminfiesto.droppit.Utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.R;

import java.util.List;

public class RecyclerImagerAdapter extends RecyclerView.Adapter<RecyclerImagerAdapter.ViewHolder> {

    private List<Photo> data;
    private Context mContext;

    public RecyclerImagerAdapter(Context context, List<Photo> data)
    {
        this.data = data;
        this.mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.layout_grid_imageview, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {

        String Data = data.get(position).getCaption();
        Glide.with(holder.image.getContext())
                .load(data.get(position).getImage_path())
                .into(holder.image);
    }

    @Override
    public int getItemCount()
    {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder
    {

        private ImageView image;

        public ViewHolder(View itemView)
        {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.gridImageView);
        }
    }
}
