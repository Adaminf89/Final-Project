package com.example.adaminfiesto.droppit.Utils;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.adaminfiesto.droppit.DataModels.Photo;
import com.example.adaminfiesto.droppit.R;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;
import java.util.List;

public class RecyclerImagerAdapter extends RecyclerView.Adapter<RecyclerImagerAdapter.ViewHolder>
{
    private ArrayList<String> data;
    private Context mContext;

    public RecyclerImagerAdapter(Context context, ArrayList<String> data)
    {
        this.data = data;
        this.mContext = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder
    {

        private ImageView image;
        private ProgressBar progressBar;

        public ViewHolder(View itemView)
        {
            super(itemView);
            image = (ImageView) itemView.findViewById(R.id.gridImageView);
            progressBar = itemView.findViewById(R.id.gridImageProgressbar);
        }

        public ProgressBar getProgressBar() {
            return progressBar;
        }

        public ImageView getImage(){ return this.image;}
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.layout_grid_imageview, parent, false);
        v.setLayoutParams(new RecyclerView.LayoutParams(1080,800));
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {

        RequestOptions requestOptions = new RequestOptions();
        requestOptions.diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(this.mContext)
                .load(data.get(position))
                .apply(requestOptions)
                .into(holder.getImage());

        holder.getProgressBar().setVisibility(View.GONE);
    }

    @Override
    public int getItemCount()
    {
        return data.size();
    }


}
