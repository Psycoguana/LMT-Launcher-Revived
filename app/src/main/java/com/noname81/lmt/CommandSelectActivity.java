package com.noname81.lmt;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks which action a slot fires.
 *
 * <p>Presents {@link Action}'s catalogue grouped into sections by the {@code Offset*} markers, and
 * for action types that need one, collects the payload — an app, an activity, a URL, a script, a
 * key code, or (Tasker) a task chosen from Tasker's own picker.
 */
public class CommandSelectActivity extends ThemedActivity {
    private static final int REQUEST_TASKER_TASK_SELECT = 1;
    private static final int REQUEST_SCRIPT_FILE = 2;

    /** Material's opacity for disabled content. */
    static final float DISABLED_ALPHA = 0.38f;

    Adapter mAdapter;
    private int mPos = 0;
    boolean mRootDenied;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

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
        getSupportActionBar().setTitle(getString(R.string.app_name));
        root.addView(
                appBarLayout,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

        RecyclerView recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.mAdapter = new Adapter();
        recyclerView.setAdapter(this.mAdapter);
        RootContext rootContext = RootContext.getInstance(getApplicationContext());
        this.mRootDenied = rootContext.isRootDenied();
        rootContext.whenRootKnown(
                () -> {
                    if (!isDestroyed() && this.mRootDenied != rootContext.isRootDenied()) {
                        this.mRootDenied = rootContext.isRootDenied();
                        this.mAdapter.notifyDataSetChanged();
                    }
                });
        root.addView(
                recyclerView,
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void handleRowClick(int actionType) {
        this.mPos = actionType;
        if (actionType == Action.Folder) {
            // Keep the folder's name when a folder slot is picked again, then edit its items.
            SettingsValues settings = SettingsValues.getInstance(getApplicationContext());
            int slot = settings.currentPieSlot();
            Action current = settings.getCurrentAction();
            boolean wasFolder = current.getType() == Action.Folder;
            Action folder = new Action(Action.Folder, wasFolder ? current.getString() : "Folder");
            folder.setSymbol(wasFolder ? current.getSymbol() : null);
            settings.setCurrentAction(this, folder);
            startActivity(FolderActivity.intent(this, slot));
            finish();
            return;
        }
        if (actionType == 2) {
            MultiSelectActivity.AppSelectMode = 0;
            startActivityForResult(new Intent(this, MultiSelectActivity.class), 0);
            finish();
            return;
        }
        if (actionType == 27) {
            MultiSelectActivity.AppSelectMode = 1;
            startActivityForResult(new Intent(this, MultiSelectActivity.class), 0);
            finish();
            return;
        }
        if (actionType == 33) {
            MultiSelectActivity.AppSelectMode = 5;
            startActivityForResult(new Intent(this, MultiSelectActivity.class), 0);
            finish();
            return;
        }
        if (actionType == Action.TaskerTask) {
            Intent picker = TaskerIntents.selectTaskIntent();
            if (getPackageManager().resolveActivity(picker, 0) != null) {
                startActivityForResult(picker, REQUEST_TASKER_TASK_SELECT);
                return;
            }
            // No Tasker installed: fall through to typing the name, as before.
        }
        if ((actionType > 25 && actionType < 30) || actionType == 32) {
            Action currentAction =
                    SettingsValues.getInstance(getApplicationContext()).getCurrentAction();
            String string = currentAction.getType() == this.mPos ? currentAction.getString() : "";
            InputDialog dialog =
                    new InputDialog(
                            this,
                            getString(R.string.dialog_input),
                            getString(R.string.dialog_define_the)
                                    + " "
                                    + Action.names[this.mPos]
                                    + " "
                                    + getString(R.string.dialog_to_be_executed),
                            string,
                            false) {
                        @Override
                        public boolean onOkClicked(String value) {
                            SettingsValues.getInstance(
                                            CommandSelectActivity.this.getApplicationContext())
                                    .setCurrentAction(
                                            CommandSelectActivity.this.getParent(),
                                            new Action(CommandSelectActivity.this.mPos, value));
                            CommandSelectActivity.this.onBackPressed();
                            return true;
                        }
                    };
            if (actionType == Action.Script) {
                // ROADMAP 1.13: pick the script with the file explorer instead of typing its path.
                dialog.setNeutralButton(
                        "Browse…",
                        (d, which) ->
                                startActivityForResult(
                                        new Intent(Intent.ACTION_OPEN_DOCUMENT)
                                                .addCategory(Intent.CATEGORY_OPENABLE)
                                                .setType("*/*"),
                                        REQUEST_SCRIPT_FILE));
            }
            dialog.show();
            return;
        }
        SettingsValues.getInstance(getApplicationContext())
                .setCurrentAction(this, new Action(this.mPos));
        onBackPressed();
    }

    /** Test-only entry point: reproduces a real tap on the row at this flat position. */
    void clickRowForTest(int flatPosition) {
        handleRowClick(this.mAdapter.mRows.rowAt(flatPosition));
    }

    SectionedRows<Integer> rowsForTest() {
        return this.mAdapter.mRows;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCRIPT_FILE && resultCode == RESULT_OK && data != null) {
            String path = filePathOf(data.getData());
            if (path == null) {
                Toast.makeText(
                                this,
                                "Pick the script from the device storage, not a cloud folder",
                                Toast.LENGTH_LONG)
                        .show();
                return;
            }
            SettingsValues.getInstance(getApplicationContext())
                    .setCurrentAction(getParent(), new Action(Action.Script, path));
            onBackPressed();
            return;
        }
        if (requestCode != REQUEST_TASKER_TASK_SELECT || resultCode != RESULT_OK) {
            return;
        }
        String taskName = TaskerIntents.taskNameFromSelectResult(data);
        if (taskName == null) {
            return;
        }
        SettingsValues.getInstance(getApplicationContext())
                .setCurrentAction(getParent(), new Action(Action.TaskerTask, taskName));
        onBackPressed();
    }

    /**
     * The file path behind a picked document, for the root shell that runs scripts: storage volumes
     * ("primary:Download/x.sh") and the Downloads provider's "raw:" ids map to paths; anything else
     * (cloud storage) has none.
     */
    static String filePathOf(Uri document) {
        if (document == null) {
            return null;
        }
        String authority = document.getAuthority();
        String id;
        try {
            id = DocumentsContract.getDocumentId(document);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (id.startsWith("raw:")) {
            return id.substring(4);
        }
        if (!"com.android.externalstorage.documents".equals(authority)) {
            return null;
        }
        int colon = id.indexOf(':');
        if (colon < 0) {
            return null;
        }
        String volume = id.substring(0, colon);
        String root = volume.equals("primary") ? "/storage/emulated/0" : "/storage/" + volume;
        return root + "/" + id.substring(colon + 1);
    }

    final class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        final SectionedRows<Integer> mRows = new SectionedRows<>();

        Adapter() {
            this.mRows.addSection(getString(R.string.commands_normal_commands), range(1, 18));
            // Actions added later have ids past the last section; each joins its section here.
            List<Integer> toggles = range(20, 24);
            toggles.add(Action.Flashlight);
            toggles.add(Action.KeepScreenOn);
            this.mRows.addSection(getString(R.string.commands_toogle_commands), toggles);
            List<Integer> advanced = range(26, 38);
            advanced.add(Action.PopUpWindow);
            this.mRows.addSection(getString(R.string.commands_advanced_commands), advanced);
            this.mRows.addSection(getString(R.string.commands_app_drawer_commands), range(40, 42));
            List<Integer> pieActions = range(44, 45);
            pieActions.add(Action.BrightnessDial);
            // Folders hold other actions, so they go on pie slots only, not in a folder.
            if (SettingsValues.getInstance(getApplicationContext()).currentPieSlot() >= 0) {
                pieActions.add(Action.Folder);
            }
            this.mRows.addSection(getString(R.string.commands_pie_action_commands), pieActions);
        }

        private List<Integer> range(int from, int to) {
            List<Integer> types = new ArrayList<>();
            for (int type = from; type <= to; type++) {
                types.add(type);
            }
            return types;
        }

        @Override
        public int getItemCount() {
            return this.mRows.getItemCount();
        }

        @Override
        public int getItemViewType(int position) {
            return this.mRows.getItemViewType(position);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == SectionedRows.VIEW_TYPE_HEADER) {
                return new HeaderViewHolder(inflater.inflate(R.layout.listheader, parent, false));
            }
            return new RowViewHolder(
                    inflater.inflate(R.layout.listitem_icondescriptionicon, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).text.setText(this.mRows.headerAt(position));
                return;
            }
            RowViewHolder rowHolder = (RowViewHolder) holder;
            int actionType = this.mRows.rowAt(position);
            rowHolder.title.setText(Action.names[actionType]);
            rowHolder.caption.setText(Action.captions[actionType]);
            IconUtils.setMaxSizeForImageView(getApplicationContext(), rowHolder.icon);
            IconLoader.load(
                    rowHolder.icon,
                    R.drawable.none,
                    actionType,
                    () ->
                            IconUtils.getIconForAction(
                                    getApplicationContext(), new Action(actionType), null));
            if (actionType
                    == SettingsValues.getInstance(getApplicationContext())
                            .getCurrentAction()
                            .getType()) {
                rowHolder.icon2.setImageDrawable(IconUtils.getIconForOK(getApplicationContext()));
                // ok.png is white; tint it so the selection mark shows on light themes too.
                rowHolder.icon2.setImageTintList(
                        ColorStateList.valueOf(
                                MaterialColors.getColor(
                                        rowHolder.icon2,
                                        com.google.android.material.R.attr.colorPrimary)));
            } else {
                rowHolder.icon2.setImageDrawable(null);
            }
            // Root-only actions can't work while su is missing or denied (ROADMAP 1.13).
            boolean blocked =
                    CommandSelectActivity.this.mRootDenied && Action.needsRoot(actionType);
            rowHolder.itemView.setAlpha(blocked ? DISABLED_ALPHA : 1f);
            rowHolder.itemView.setOnClickListener(
                    v -> {
                        if (blocked) {
                            Toast.makeText(
                                            CommandSelectActivity.this,
                                            "This action needs root, which isn't granted",
                                            Toast.LENGTH_SHORT)
                                    .show();
                        } else {
                            handleRowClick(actionType);
                        }
                    });
        }
    }

    private static final class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            this.text = (TextView) itemView;
        }
    }

    private static final class RowViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView caption;
        final ImageView icon;
        final ImageView icon2;

        RowViewHolder(@NonNull View itemView) {
            super(itemView);
            this.title = itemView.findViewById(R.id.listitem_icondescriptionicon_text);
            this.caption = itemView.findViewById(R.id.listitem_icondescriptionicon_caption);
            this.icon = itemView.findViewById(R.id.listitem_icondescriptionicon_icon);
            this.icon2 = itemView.findViewById(R.id.listitem_icondescriptionicon_icon2);
        }
    }
}
