package com.kkbox.raymondbox;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StartFunctionAdapter extends BaseAdapter {
    List<String> functionList = new ArrayList<String>(
            Arrays.asList("主題展示","KKBRAIN","KKLINX")
    );
    List<Class<?>> activityList = new ArrayList<>();
    Context context;
    public StartFunctionAdapter(Context context){
        this.context = context;
        activityList.add(MainActivity.class);
        activityList.add(KKAssistantActivity.class);
        activityList.add(KKlinxActivity.class);
    }

    @Override
    public int getCount() {
        return functionList.size();
    }

    @Override
    public Object getItem(int position) {
        return functionList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Button button = new Button(context);
        button.setText(functionList.get(position));
        button.setLayoutParams(new ViewGroup.LayoutParams(300,300));
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context,activityList.get(position));

                context.startActivity(intent);
            }
        });
        return button;
    }
}
