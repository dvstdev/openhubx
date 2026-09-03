package com.thirtydegreesray.openhub.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.thirtydegreesray.openhub.R;
import com.thirtydegreesray.openhub.inject.component.AppComponent;
import com.thirtydegreesray.openhub.inject.component.DaggerFragmentComponent;
import com.thirtydegreesray.openhub.inject.module.FragmentModule;
import com.thirtydegreesray.openhub.mvp.contract.IBookmarkContract;
import com.thirtydegreesray.openhub.mvp.model.BookmarkExt;
import com.thirtydegreesray.openhub.mvp.presenter.BookmarkPresenter;
import com.thirtydegreesray.openhub.ui.activity.ProfileActivity;
import com.thirtydegreesray.openhub.ui.activity.RepositoryActivity;
import com.thirtydegreesray.openhub.ui.adapter.BookmarksAdapter;
import com.thirtydegreesray.openhub.ui.adapter.base.ItemTouchHelperCallback;
import com.thirtydegreesray.openhub.ui.fragment.base.ListFragment;
import com.thirtydegreesray.openhub.util.BookmarkBackup;
import com.thirtydegreesray.openhub.util.PrefUtils;

import java.util.ArrayList;

/**
 * Created by ThirtyDegreesRay on 2017/11/22 16:29:20
 */

public class BookmarksFragment extends ListFragment<BookmarkPresenter, BookmarksAdapter>
        implements IBookmarkContract.View, ItemTouchHelperCallback.ItemGestureListener {

    public static BookmarksFragment create(){
        return new BookmarksFragment();
    }

    private ItemTouchHelper itemTouchHelper;

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_list;
    }

    @Override
    protected void setupFragmentComponent(AppComponent appComponent) {
        DaggerFragmentComponent.builder()
                .appComponent(appComponent)
                .fragmentModule(new FragmentModule(this))
                .build()
                .inject(this);
    }

    @Override
    protected void initFragment(Bundle savedInstanceState) {
        super.initFragment(savedInstanceState);
        setHasOptionsMenu(true);
        setLoadMoreEnable(true);
        ItemTouchHelperCallback callback = new ItemTouchHelperCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT, this);
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    @Override
    protected void onReLoadData() {
        mPresenter.loadBookmarks(1);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        // MainActivity switches tabs via FragmentManager show/hide (no onResume),
        // so reload on show to reflect bookmarks added from other screens.
        if (!hidden && mPresenter != null) {
            mPresenter.loadBookmarks(1);
        }
    }

    @Override
    protected String getEmptyTip() {
        return getString(R.string.no_bookmarks);
    }

    @Override
    public void onItemClick(int position, @NonNull View view) {
        super.onItemClick(position, view);
        BookmarkExt bookmark = adapter.getData().get(position);
        if("user".equals(bookmark.getType())){
            View userAvatar = view.findViewById(R.id.avatar);
            ProfileActivity.show(getActivity(), userAvatar, bookmark.getUser().getLogin(),
                    bookmark.getUser().getAvatarUrl());
        } else {
            RepositoryActivity.show(getActivity(), bookmark.getRepository().getOwner().getLogin(),
                    bookmark.getRepository().getName());
        }
    }

    @Override
    protected void onLoadMore(int page) {
        super.onLoadMore(page);
        mPresenter.loadBookmarks(page);
    }

    @Override
    public void showBookmarks(ArrayList<BookmarkExt> bookmarks) {
        adapter.setData(bookmarks);
        postNotifyDataSetChanged();

        if(bookmarks != null && bookmarks.size() > 0 && PrefUtils.isBookmarksTipAble()){
            showOperationTip(R.string.bookmarks_tip);
            PrefUtils.set(PrefUtils.BOOKMARKS_TIP_ABLE, false);
        }
    }

    @Override
    public void notifyItemAdded(int position) {
        if(adapter.getData().size() == 1){
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemInserted(position);
        }
    }

    @Override
    public boolean onItemMoved(int fromPosition, int toPosition) {
        return false;
    }

    @Override
    public void onItemSwiped(int position, int direction) {
        mPresenter.removeBookmark(position);
        if(adapter.getData().size() == 0){
            postNotifyDataSetChanged();
        } else {
            adapter.notifyItemRemoved(position);
        }
        Snackbar.make(recyclerView, R.string.bookmark_removed, Snackbar.LENGTH_LONG)
                .setAction(R.string.undo, v -> mPresenter.undoRemoveBookmark() )
                .show();
    }

    private static final int REQUEST_EXPORT = 3001;
    private static final int REQUEST_IMPORT = 3002;
    private String pendingExportJson;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_bookmarks, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export_bookmarks) {
            if (mPresenter.getDaoSession().getBookmarkDao().count() == 0) {
                Toast.makeText(getActivity(), R.string.bookmarks_export_empty, Toast.LENGTH_SHORT).show();
                return true;
            }
            pendingExportJson = BookmarkBackup.export(mPresenter.getDaoSession());
            startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_TITLE, "openhubx-bookmarks.json"), REQUEST_EXPORT);
            return true;
        } else if (item.getItemId() == R.id.action_import_bookmarks) {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*"), REQUEST_IMPORT);
            return true;
        } else if (item.getItemId() == R.id.action_share_bookmarks) {
            shareBookmarks();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareBookmarks() {
        if (mPresenter.getDaoSession().getBookmarkDao().count() == 0) {
            Toast.makeText(getActivity(), R.string.bookmarks_export_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            java.io.File dir = new java.io.File(getActivity().getCacheDir(), "shared");
            dir.mkdirs();
            java.io.File file = new java.io.File(dir, "openhubx-bookmarks.json");
            writeToFile(file, BookmarkBackup.export(mPresenter.getDaoSession()));
            Uri uri = FileProvider.getUriForFile(getActivity(),
                    getActivity().getPackageName() + ".fileProvider", file);
            Intent share = new Intent(Intent.ACTION_SEND)
                    .setType("application/json")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, getString(R.string.share_to)));
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.bookmarks_backup_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void writeToFile(java.io.File file, String content) throws java.io.IOException {
        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
        fos.write(content.getBytes(java.nio.charset.Charset.forName("UTF-8")));
        fos.flush();
        fos.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != android.app.Activity.RESULT_OK || data == null || data.getData() == null) {
            pendingExportJson = null;
            return;
        }
        Uri uri = data.getData();
        try {
            if (requestCode == REQUEST_EXPORT) {
                writeToUri(uri, pendingExportJson);
                pendingExportJson = null;
                Toast.makeText(getActivity(), R.string.bookmarks_exported, Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_IMPORT) {
                int count = BookmarkBackup.importJson(mPresenter.getDaoSession(), readFromUri(uri));
                Toast.makeText(getActivity(), getString(R.string.bookmarks_imported, count),
                        Toast.LENGTH_SHORT).show();
                mPresenter.loadBookmarks(1);
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), R.string.bookmarks_backup_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void writeToUri(Uri uri, String content) throws java.io.IOException {
        java.io.OutputStream os = getActivity().getContentResolver().openOutputStream(uri, "w");
        if (os == null) throw new java.io.IOException("null output stream");
        os.write(content.getBytes(java.nio.charset.Charset.forName("UTF-8")));
        os.flush();
        os.close();
    }

    private String readFromUri(Uri uri) throws java.io.IOException {
        java.io.InputStream is = getActivity().getContentResolver().openInputStream(uri);
        if (is == null) throw new java.io.IOException("null input stream");
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) bos.write(buf, 0, n);
        is.close();
        return new String(bos.toByteArray(), java.nio.charset.Charset.forName("UTF-8"));
    }

}
