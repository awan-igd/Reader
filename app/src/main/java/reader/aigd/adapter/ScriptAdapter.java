package reader.aigd.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import reader.aigd.R;
import reader.aigd.model.Script;

public class ScriptAdapter extends RecyclerView.Adapter<ScriptAdapter.Holder> {

    private final OnScriptActionListener listener;
    private List<Script> list;

    public ScriptAdapter(List<Script> list, OnScriptActionListener listener) {
        this.list = list;
        this.listener = listener;
    }

    public void update(List<Script> newList) {
        this.list = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_script, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Script s = list.get(position);
        h.preview.setText(s.getPreview(120));
        h.meta.setText(s.getLastEdited());

        h.card.setOnClickListener(v -> listener.onScriptClick(s));

        // Clean entrance
        h.itemView.setAlpha(0f);
        h.itemView.setTranslationY(20f);
        h.itemView.animate().alpha(1f).translationY(0f).setDuration(400).setStartDelay(position * 30L).start();
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnScriptActionListener {
        void onScriptClick(Script script);
    }

    static class Holder extends RecyclerView.ViewHolder {
        View card;
        TextView meta, preview;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.scriptCard);
            meta = itemView.findViewById(R.id.scriptMetadata);
            preview = itemView.findViewById(R.id.scriptPreview);
        }
    }
}