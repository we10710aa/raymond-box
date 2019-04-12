package com.kkbox.raymondbox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.api.kktix.Entry;

import java.util.List;

public class PersonalActivityAdapter extends ArrayAdapter<Entry> {
    List<Entry> entryList;
    Context context;

    int resource;

    public PersonalActivityAdapter(Context context, int resource,List<Entry> objects) {
        super(context, resource, objects);
        this.entryList = objects;
        this.resource  =resource;
        this.context = context;
    }

    @NonNull
    @Override
    public View getView(final int position, @NonNull View convertView, @NonNull ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(resource,null,false);
        TextView rowTitle = view.findViewById(R.id.row_title);
        TextView rowTime = view.findViewById(R.id.row_time);
        TextView rowHost = view.findViewById(R.id.row_host);
        TextView rowSummary = view.findViewById(R.id.row_summary);

        final Entry entry = entryList.get(position);
        rowTime.setText(entry.getPublished());
        rowTitle.setText(entry.getTitle());
        rowTitle.setSingleLine();
        rowTitle.setEllipsize(TextUtils.TruncateAt.END);
        rowHost.setText(entry.getAuthorName());
        rowSummary.setText(entry.getSummary());

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(entry.getUrl()));
                context.startActivity(browserIntent);
            }
        });
        return view;
    }
}
