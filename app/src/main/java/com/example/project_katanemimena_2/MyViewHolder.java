package com.example.project_katanemimena_2;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

// Provide a reference to the views for each data item
// Complex data items may need more than one view per item, and
// you provide access to all the views for a data item in a view holder
public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    // each data item is just a string in this case
    public TextView textView;
    private MyItemClickListener mListener;

    public MyViewHolder(TextView v,MyItemClickListener mItemClickListener) {
        super(v);
        textView = v;
        this.mListener = mItemClickListener;
        textView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(mListener != null){
            mListener.onItemClick(v,getPosition());
        }
    }
}