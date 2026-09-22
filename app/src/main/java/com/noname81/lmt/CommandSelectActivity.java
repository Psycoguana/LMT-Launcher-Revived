package com.noname81.lmt;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/* JADX INFO: loaded from: classes.dex */
public class CommandSelectActivity extends Activity implements AdapterView.OnItemClickListener {
    private int mPos = 0;

    @Override // android.app.Activity
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        LinkedList linkedList = new LinkedList();
        linkedList.add(SeparatedListAdapter.createItem(Action.names[1], Action.captions[1]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[2], Action.captions[2]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[3], Action.captions[3]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[4], Action.captions[4]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[5], Action.captions[5]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[6], Action.captions[6]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[7], Action.captions[7]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[8], Action.captions[8]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[9], Action.captions[9]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[10], Action.captions[10]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[11], Action.captions[11]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[12], Action.captions[12]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[13], Action.captions[13]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[14], Action.captions[14]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[15], Action.captions[15]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[16], Action.captions[16]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[17], Action.captions[17]));
        linkedList.add(SeparatedListAdapter.createItem(Action.names[18], Action.captions[18]));
        LinkedList linkedList2 = new LinkedList();
        linkedList2.add(SeparatedListAdapter.createItem(Action.names[20], Action.captions[20]));
        linkedList2.add(SeparatedListAdapter.createItem(Action.names[21], Action.captions[21]));
        linkedList2.add(SeparatedListAdapter.createItem(Action.names[22], Action.captions[22]));
        linkedList2.add(SeparatedListAdapter.createItem(Action.names[23], Action.captions[23]));
        linkedList2.add(SeparatedListAdapter.createItem(Action.names[24], Action.captions[24]));
        LinkedList linkedList3 = new LinkedList();
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[26], Action.captions[26]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[27], Action.captions[27]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[28], Action.captions[28]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[29], Action.captions[29]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[30], Action.captions[30]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[31], Action.captions[31]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[32], Action.captions[32]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[33], Action.captions[33]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[34], Action.captions[34]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[35], Action.captions[35]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[36], Action.captions[36]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[37], Action.captions[37]));
        linkedList3.add(SeparatedListAdapter.createItem(Action.names[38], Action.captions[38]));
        LinkedList linkedList4 = new LinkedList();
        linkedList4.add(SeparatedListAdapter.createItem(Action.names[40], Action.captions[40]));
        linkedList4.add(SeparatedListAdapter.createItem(Action.names[41], Action.captions[41]));
        linkedList4.add(SeparatedListAdapter.createItem(Action.names[42], Action.captions[42]));
        LinkedList linkedList5 = new LinkedList();
        linkedList5.add(SeparatedListAdapter.createItem(Action.names[44], Action.captions[44]));
        linkedList5.add(SeparatedListAdapter.createItem(Action.names[45], Action.captions[45]));
        SeparatedListAdapter separatedListAdapter = new SeparatedListAdapter(this);
        separatedListAdapter.addSection(
                getString(R.string.commands_normal_commands),
                new CommandSelectSimpleAdapter(this, linkedList, 0));
        separatedListAdapter.addSection(
                getString(R.string.commands_toogle_commands),
                new CommandSelectSimpleAdapter(this, linkedList2, 19));
        separatedListAdapter.addSection(
                getString(R.string.commands_advanced_commands),
                new CommandSelectSimpleAdapter(this, linkedList3, 25));
        separatedListAdapter.addSection(
                getString(R.string.commands_app_drawer_commands),
                new CommandSelectSimpleAdapter(this, linkedList4, 39));
        separatedListAdapter.addSection(
                getString(R.string.commands_pie_action_commands),
                new CommandSelectSimpleAdapter(this, linkedList5, 43));
        ListView listView = new ListView(this);
        listView.setAdapter((ListAdapter) separatedListAdapter);
        listView.setOnItemClickListener(this);
        listView.setDividerHeight(0);
        setContentView(listView);
    }

    @Override // android.widget.AdapterView.OnItemClickListener
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        this.mPos = i;
        if (i == 2) {
            MultiSelectActivity.AppSelectMode = 0;
            startActivityForResult(
                    new Intent(view.getContext(), (Class<?>) MultiSelectActivity.class), 0);
            finish();
            return;
        }
        if (i == 27) {
            MultiSelectActivity.AppSelectMode = 1;
            startActivityForResult(
                    new Intent(view.getContext(), (Class<?>) MultiSelectActivity.class), 0);
            finish();
            return;
        }
        if (i == 33) {
            MultiSelectActivity.AppSelectMode = 5;
            startActivityForResult(
                    new Intent(view.getContext(), (Class<?>) MultiSelectActivity.class), 0);
            finish();
            return;
        }
        if ((i > 25 && i < 30) || i == 32) {
            Action currentAction =
                    SettingsValues.getInstance(getApplicationContext()).getCurrentAction();
            String string = currentAction.getType() == this.mPos ? currentAction.getString() : "";
            new InputDialog(
                    this,
                    getString(R.string.dialog_input),
                    getString(R.string.dialog_define_the)
                            + " "
                            + Action.names[this.mPos]
                            + " "
                            + getString(R.string.dialog_to_be_executed),
                    string,
                    false) { // from class: com.noname81.lmt.CommandSelectActivity.1
                @Override // com.noname81.lmt.InputDialog
                public boolean onOkClicked(String str) {
                    SettingsValues.getInstance(CommandSelectActivity.this.getApplicationContext())
                            .setCurrentAction(
                                    CommandSelectActivity.this.getParent(),
                                    new Action(CommandSelectActivity.this.mPos, str));
                    CommandSelectActivity.this.onBackPressed();
                    return true;
                }
            }.show();
            return;
        }
        SettingsValues.getInstance(getApplicationContext())
                .setCurrentAction(this, new Action(this.mPos));
        onBackPressed();
    }

    class CommandSelectSimpleAdapter extends SimpleAdapter {
        private final int mOffset;

        CommandSelectSimpleAdapter(Context context, List<? extends Map<String, ?>> list, int i) {
            super(
                    context,
                    list,
                    R.layout.listitem_icondescriptionicon,
                    new String[] {"title", "caption"},
                    new int[] {
                        R.id.listitem_icondescriptionicon_text,
                        R.id.listitem_icondescriptionicon_caption
                    });
            this.mOffset = i;
        }

        @Override // android.widget.SimpleAdapter, android.widget.Adapter
        public View getView(final int position, View view, ViewGroup viewGroup) {
            View view2 = super.getView(position, view, viewGroup);
            ImageView imageView =
                    (ImageView) view2.findViewById(R.id.listitem_icondescriptionicon_icon);
            ImageView imageView2 =
                    (ImageView) view2.findViewById(R.id.listitem_icondescriptionicon_icon2);
            IconUtils.setMaxSizeForImageView(
                    CommandSelectActivity.this.getApplicationContext(), imageView);
            new AsyncDrawableTask(
                    imageView,
                    R.drawable
                            .none) { // from class:
                                     // com.noname81.lmt.CommandSelectActivity.CommandSelectSimpleAdapter.1
                /* JADX WARN: Can't rename method to resolve collision */
                @Override // com.noname81.lmt.AsyncDrawableTask, android.os.AsyncTask
                protected Drawable doInBackground(Void... voidArr) {
                    return IconUtils.getIconForAction(
                            CommandSelectActivity.this.getApplicationContext(),
                            new Action(CommandSelectSimpleAdapter.this.mOffset + position + 1),
                            null);
                }
            }.execute(new Void[0]);
            if (this.mOffset + position + 1
                    == SettingsValues.getInstance(
                                    CommandSelectActivity.this.getApplicationContext())
                            .getCurrentAction()
                            .getType()) {
                imageView2.setImageDrawable(
                        IconUtils.getIconForOK(CommandSelectActivity.this.getApplicationContext()));
            } else {
                imageView2.setImageDrawable(null);
            }
            return view2;
        }
    }
}
