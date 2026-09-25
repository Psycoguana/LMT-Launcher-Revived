package com.noname81.lmt;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Edits a pie folder (ROADMAP 1.17): its name and up to {@link SettingsValuesBase#FOLDER_ITEMS}
 * actions, each chosen with the usual action picker. Empty items are left out of the pie, which
 * shows the rest in a ring around the folder slice.
 */
public class FolderActivity extends ThemedActivity {
    static final String EXTRA_SLOT = "slot";

    private int mSlot;
    private Adapter mAdapter;
    private TextInputEditText mName;
    private TextInputLayout mNameLayout;

    static Intent intent(Context context, int slot) {
        return new Intent(context, FolderActivity.class).putExtra(EXTRA_SLOT, slot);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mSlot = getIntent().getIntExtra(EXTRA_SLOT, 0);
        SettingsValues settings = SettingsValues.getInstance(getApplicationContext());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        AppBarLayout appBarLayout = new AppBarLayout(this);
        MaterialToolbar toolbar = new MaterialToolbar(this);
        appBarLayout.addView(
                toolbar,
                new AppBarLayout.LayoutParams(
                        AppBarLayout.LayoutParams.MATCH_PARENT,
                        AppBarLayout.LayoutParams.WRAP_CONTENT));
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Pie folder");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());
        root.addView(
                appBarLayout,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        TextInputLayout nameLayout =
                new TextInputLayout(
                        this, null, com.google.android.material.R.attr.textInputOutlinedStyle);
        nameLayout.setHint("Folder name");
        // The folder's icon sits left of its name; tapping it picks another (ROADMAP 1.17).
        this.mNameLayout = nameLayout;
        showIcon(settings.getPieAction(this.mSlot).getSymbol());
        nameLayout.setStartIconContentDescription("Folder icon");
        nameLayout.setStartIconOnClickListener(v -> pickIcon());
        TextInputEditText name = new TextInputEditText(nameLayout.getContext());
        name.setId(R.id.folder_name);
        name.setSingleLine(true);
        name.setText(settings.getPieAction(this.mSlot).getString());
        this.mName = name;
        nameLayout.addView(name);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams nameParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        nameParams.setMargins(pad, pad, pad, pad / 2);
        root.addView(nameLayout, nameParams);

        TextView hint = new TextView(this);
        hint.setText(
                "The pie shows these around the folder slice. Tap an item to choose its action;"
                        + " pick \"Trigger no command\" to empty it.");
        hint.setTextAppearance(
                com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        hint.setPadding(pad, 0, pad, pad / 2);
        root.addView(hint);

        RecyclerView list = new RecyclerView(this);
        list.setLayoutManager(new LinearLayoutManager(this));
        this.mAdapter = new Adapter();
        list.setAdapter(this.mAdapter);
        root.addView(
                list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    /** Saves the name on leaving: each save restarts the pie service. */
    @Override
    protected void onPause() {
        super.onPause();
        SettingsValues settings = SettingsValues.getInstance(getApplicationContext());
        String name = this.mName.getText() == null ? "" : this.mName.getText().toString().trim();
        if (!name.equals(settings.getPieAction(this.mSlot).getString())) {
            settings.saveFolderName(this.mSlot, name);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.mAdapter != null) {
            this.mAdapter.notifyDataSetChanged();
        }
    }

    private void showIcon(String symbol) {
        this.mNameLayout.setStartIconDrawable(
                MaterialIcons.uiDrawable(this, symbol != null ? symbol : "folder", 24));
    }

    private void pickIcon() {
        SettingsValues settings = SettingsValues.getInstance(getApplicationContext());
        IconPickerDialog.show(
                this,
                settings.getPieAction(this.mSlot).getSymbol(),
                name -> {
                    settings.saveFolderSymbol(this.mSlot, name);
                    showIcon(name);
                });
    }

    void pickIconForTest() {
        pickIcon();
    }

    void clickItemForTest(int item) {
        pick(item);
    }

    private void pick(int item) {
        SettingsValues.getInstance(getApplicationContext()).setCurrentFolderItem(this.mSlot, item);
        startActivity(new Intent(this, CommandSelectActivity.class));
    }

    /** The Pie tab's caption for a folder slot: its name and how many actions it holds. */
    static String summary(Context context, int slot) {
        SettingsValues settings = SettingsValues.getInstance(context.getApplicationContext());
        int count = 0;
        for (int item = 0; item < SettingsValuesBase.FOLDER_ITEMS; item++) {
            if (settings.getFolderAction(slot, item).getType() > Action.None) {
                count++;
            }
        }
        String name = settings.getPieAction(slot).getString();
        String title =
                name == null || name.isEmpty() ? "Folder" : "Folder \u201c" + name + "\u201d";
        return title + ", " + count + (count == 1 ? " action" : " actions");
    }

    /** A row's caption: the app's name for apps, the action's description otherwise. */
    static String describe(Context context, Action action) {
        if (action.getType() <= Action.None) {
            return "Empty";
        }
        if (action.getType() == Action.App && action.getString() != null) {
            PackageManager pm = context.getPackageManager();
            try {
                return pm.getApplicationLabel(pm.getApplicationInfo(action.getString(), 0))
                        .toString();
            } catch (PackageManager.NameNotFoundException e) {
                return action.getString();
            }
        }
        String caption = Action.captions[action.getType()];
        int tag = caption.lastIndexOf(" [");
        return tag > 0 ? caption.substring(0, tag) : caption;
    }

    private final class Adapter extends RecyclerView.Adapter<Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.listitem_icondescriptionicon, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            FolderActivity activity = FolderActivity.this;
            Action action =
                    SettingsValues.getInstance(getApplicationContext())
                            .getFolderAction(activity.mSlot, position);
            holder.title.setText("Item " + (position + 1));
            holder.caption.setText(describe(activity, action));
            IconUtils.setMaxSizeForImageView(activity, holder.icon);
            holder.icon.setImageDrawable(
                    action.getType() > Action.None
                            ? IconUtils.getIconForAction(activity, action, null)
                            : null);
            holder.icon2.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> pick(position));
        }

        @Override
        public int getItemCount() {
            return SettingsValuesBase.FOLDER_ITEMS;
        }
    }

    private static final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView caption;
        final ImageView icon;
        final ImageView icon2;

        Holder(@NonNull View itemView) {
            super(itemView);
            this.title = itemView.findViewById(R.id.listitem_icondescriptionicon_text);
            this.caption = itemView.findViewById(R.id.listitem_icondescriptionicon_caption);
            this.icon = itemView.findViewById(R.id.listitem_icondescriptionicon_icon);
            this.icon2 = itemView.findViewById(R.id.listitem_icondescriptionicon_icon2);
        }
    }
}
