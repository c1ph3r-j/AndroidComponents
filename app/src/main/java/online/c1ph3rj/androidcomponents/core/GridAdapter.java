package online.c1ph3rj.androidcomponents.core;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import online.c1ph3rj.androidcomponents.R;
import online.c1ph3rj.androidcomponents.model.GridItem;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.GridViewHolder> {
    private final List<GridItem> items;
    private final Context context;

    public GridAdapter(Context context, List<GridItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grid_layout, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        GridItem item = items.get(position);
        holder.textView.setText(item.getName());
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, item.getActivityClass());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class GridViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        GridViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView);
        }
    }
}
