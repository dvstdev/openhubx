

package com.darktile.openhubx.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.darktile.dataautoaccess.annotation.AutoAccess;
import com.darktile.openhubx.R;
import com.darktile.openhubx.common.AppEventBus;
import com.darktile.openhubx.common.Event;
import com.darktile.openhubx.inject.component.AppComponent;
import com.darktile.openhubx.inject.component.DaggerActivityComponent;
import com.darktile.openhubx.inject.module.ActivityModule;
import com.darktile.openhubx.mvp.contract.ISearchContract;
import com.darktile.openhubx.mvp.model.SearchFilter;
import com.darktile.openhubx.mvp.model.SearchModel;
import com.darktile.openhubx.mvp.presenter.SearchPresenter;
import com.darktile.openhubx.ui.activity.base.PagerActivity;
import com.darktile.openhubx.ui.adapter.base.FragmentPagerModel;
import com.darktile.openhubx.ui.fragment.CodeSearchFragment;
import com.darktile.openhubx.ui.fragment.IssuesFragment;
import com.darktile.openhubx.ui.fragment.RepositoriesFragment;
import com.darktile.openhubx.ui.fragment.UserListFragment;
import com.darktile.openhubx.ui.widget.SearchFilterSheet;
import com.darktile.openhubx.util.StringUtils;
import com.darktile.openhubx.util.SuggestionDebouncer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * Search screen. Input is a bottom panel: the type box and its trigger sit at the
 * bottom, search history is listed above the box (newest nearest it), and the window
 * resizes so the keyboard never covers the box. Submitting a query hides the panel and
 * shows the results pager; the toolbar search icon brings the panel back.
 */
public class SearchActivity extends PagerActivity<SearchPresenter>
        implements ISearchContract.View {

    public static void show(@NonNull Context context) {
        Intent intent = new Intent(context, SearchActivity.class);
        context.startActivity(intent);
    }

    private static final int PAGE_COUNT = 4;

    private final Map<Integer, List<Integer>> MENU_ID_MAP = new HashMap<>();

    @AutoAccess boolean isInputMode = true;
    @AutoAccess String[] sortInfos;

    private final PublishSubject<String> queryTextChanges = PublishSubject.create();
    private Disposable suggestionDisposable;

    private View searchPanel;
    private EditText searchInput;
    private RecyclerView historyRecycler;
    private HistoryAdapter historyAdapter;

    @Override
    protected void initActivity() {
        super.initActivity();
        MENU_ID_MAP.put(0, SearchModel.REPO_SORT_ID_LIST);
        MENU_ID_MAP.put(1, SearchModel.USER_SORT_ID_LIST);
    }

    @Override
    protected void setupActivityComponent(AppComponent appComponent) {
        DaggerActivityComponent.builder()
                .appComponent(appComponent)
                .activityModule(new ActivityModule(getActivity()))
                .build()
                .inject(this);
    }

    @Nullable
    @Override
    protected int getContentView() {
        return R.layout.activity_search_pager;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        super.initView(savedInstanceState);
        setToolbarScrollAble(true);
        setToolbarBackEnable();
        setToolbarTitle(getString(R.string.search));
        if (sortInfos == null || sortInfos.length != PAGE_COUNT) {
            sortInfos = new String[]{
                    getString(R.string.most_stars), getString(R.string.best_match),
                    getString(R.string.best_match), getString(R.string.best_match)
            };
        }
        suggestionDisposable = SuggestionDebouncer.debounce(queryTextChanges, Schedulers.computation())
                .observeOn(Schedulers.io())
                .map(text -> mPresenter.getSearchRecordList(getCurrentSearchType(), text))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showSuggestions, error -> { });

        setupSearchPanel();
    }

    private void setupSearchPanel() {
        searchPanel = findViewById(R.id.search_panel);
        searchInput = findViewById(R.id.search_input);
        historyRecycler = findViewById(R.id.history_recycler);

        // Reverse-stacked so history grows from the bottom (newest row sits next to the box).
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setReverseLayout(true);
        lm.setStackFromEnd(true);
        historyRecycler.setLayoutManager(lm);
        historyAdapter = new HistoryAdapter(this::submitQuery);
        historyRecycler.setAdapter(historyAdapter);

        findViewById(R.id.search_go).setOnClickListener(v -> submitFromInput());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submitFromInput();
                return true;
            }
            return false;
        });
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                queryTextChanges.onNext(s == null ? "" : s.toString());
            }
        });

        if (isInputMode) {
            showInputPanel();
        } else {
            searchPanel.setVisibility(View.GONE);
        }
        refreshSuggestions();
    }

    @Override
    protected void onDestroy() {
        if (suggestionDisposable != null) suggestionDisposable.dispose();
        super.onDestroy();
    }

    private void submitFromInput() {
        submitQuery(searchInput == null || searchInput.getText() == null
                ? "" : searchInput.getText().toString());
    }

    /** Runs a query: records it, hides the input panel and shows the results pager. */
    private void submitQuery(String query) {
        String text = query == null ? "" : query.trim();
        if (StringUtils.isBlank(text) && mPresenter.getFilter().isEmpty()) {
            showWarningToast(getString(R.string.invalid_query));
            return;
        }
        hideInputPanel();
        search(text);
        setSubTitle(viewPager.getCurrentItem());
        if (!StringUtils.isBlank(text)) {
            mPresenter.addSearchRecord(getCurrentSearchType(), text);
        }
    }

    private void showInputPanel() {
        isInputMode = true;
        searchPanel.setVisibility(View.VISIBLE);
        refreshSuggestions();
        searchInput.post(() -> {
            searchInput.requestFocus();
            showKeyboard(searchInput);
        });
        invalidateOptionsMenu();
    }

    private void hideInputPanel() {
        isInputMode = false;
        if (searchInput != null) hideKeyboard(searchInput);
        if (searchPanel != null) searchPanel.setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    private void showKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        // While typing over existing results, back returns to the results instead of exiting.
        if (isInputMode && searchPanel != null && searchPanel.getVisibility() == View.VISIBLE
                && pagerAdapter.getCount() != 0) {
            hideInputPanel();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            showInputPanel();
            return true;
        } else if (item.getItemId() == R.id.action_sort) {
            filterSortMenu(item);
        } else if (item.getItemId() == R.id.action_filter) {
            showFilterSheet();
            return true;
        } else if (item.getItemId() == R.id.action_clear_history) {
            confirmClearHistory();
            return true;
        } else if (SearchModel.SORT_ID_LIST.contains(item.getItemId())) {
            int page = viewPager.getCurrentItem();
            postSearchEvent(mPresenter.getSortModel(page, item.getItemId()));
            sortInfos[page] = item.getTitle().toString();
            setSubTitle(page);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_info).setVisible(false);
        menu.findItem(R.id.action_sort).setVisible(!isInputMode
                && pagerAdapter.getCount() != 0
                && MENU_ID_MAP.containsKey(viewPager.getCurrentItem()));
        return super.onPrepareOptionsMenu(menu);
    }

    private void showSuggestions(List<String> records) {
        if (historyAdapter != null) historyAdapter.setItems(records);
    }

    private void refreshSuggestions() {
        String text = searchInput == null || searchInput.getText() == null
                ? "" : searchInput.getText().toString();
        showSuggestions(mPresenter.getSearchRecordList(getCurrentSearchType(), text));
    }

    @NonNull
    private SearchModel.SearchType getCurrentSearchType() {
        ArrayList<SearchModel> models = mPresenter.getSearchModels();
        int index = viewPager == null ? 0 : viewPager.getCurrentItem();
        if (models == null || index < 0 || index >= models.size()) {
            return SearchModel.SearchType.Repository;
        }
        return models.get(index).getType();
    }

    private void showFilterSheet() {
        SearchFilterSheet.show(this, mPresenter.getFilter(), mPresenter.getFilterLanguages(),
                filter -> {
                    mPresenter.setFilter(filter);
                    if (pagerAdapter.getCount() != 0) {
                        search(mPresenter.getSearchModels().get(0).getQuery());
                        setSubTitle(viewPager.getCurrentItem());
                    }
                });
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(getActivity())
                .setCancelable(true)
                .setTitle(R.string.warning_dialog_tile)
                .setMessage(R.string.clear_search_history_warning)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    mPresenter.clearSearchRecords();
                    refreshSuggestions();
                    showSuccessToast(getString(R.string.search_history_cleared));
                })
                .show();
    }

    private void filterSortMenu(MenuItem item) {
        int index = viewPager.getCurrentItem();
        List<Integer> idList = MENU_ID_MAP.get(index);
        for (Integer id : SearchModel.SORT_ID_LIST) {
            item.getSubMenu().findItem(id).setVisible(idList != null && idList.contains(id));
        }
    }

    private void search(String query) {
        if (pagerAdapter.getCount() == 0) {
            pagerAdapter.setPagerList(FragmentPagerModel.
                    createSearchPagerList(getActivity(), mPresenter.getQueryModels(query), getFragments()));
            tabLayout.setVisibility(View.VISIBLE);
            tabLayout.setupWithViewPager(viewPager);
            viewPager.setAdapter(pagerAdapter);
            showFirstPager();
        } else {
            for (SearchModel searchModel : mPresenter.getQueryModels(query)) {
                postSearchEvent(searchModel);
            }
        }
    }

    private void postSearchEvent(SearchModel searchModel) {
        AppEventBus.INSTANCE.getEventBus().post(new Event.SearchEvent(searchModel));
    }

    private void setSubTitle(int page) {
        String query = mPresenter.getSearchModels().get(0).getQuery();
        StringBuilder subTitle = new StringBuilder(query == null ? "" : query);
        if (page >= 0 && page < sortInfos.length && MENU_ID_MAP.containsKey(page)) {
            subTitle.append("/").append(sortInfos[page]);
        }
        SearchFilter filter = mPresenter.getFilter();
        if (!filter.isEmpty()) {
            subTitle.append(" · ").append(getString(R.string.filters_count, filter.getActiveCount()));
        }
        setToolbarSubTitle(subTitle.toString());
    }

    @Override
    public void onPageSelected(int position) {
        super.onPageSelected(position);
        setSubTitle(position);
        invalidateOptionsMenu();
    }

    @Override
    public int getPagerSize() {
        return PAGE_COUNT;
    }

    @Override
    public void showSearches(ArrayList<SearchModel> searchModels) {
        search(searchModels.get(0).getQuery());
        setSubTitle(0);
    }

    @Override
    protected int getFragmentPosition(Fragment fragment) {
        if(fragment instanceof RepositoriesFragment){
            return 0;
        }else if(fragment instanceof UserListFragment){
            return 1;
        }else if(fragment instanceof CodeSearchFragment){
            return 2;
        }else if(fragment instanceof IssuesFragment){
            return 3;
        }else
            return -1;
    }

    /** RecyclerView adapter for the history rows above the input box. */
    private interface OnHistoryClick {
        void onClick(String query);
    }

    private static final class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.VH> {

        private final List<String> items = new ArrayList<>();
        private final OnHistoryClick listener;

        HistoryAdapter(OnHistoryClick listener) {
            this.listener = listener;
        }

        void setItems(List<String> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            TextView tv = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.layout_item_simple_list, parent, false);
            return new VH(tv);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            final String query = items.get(position);
            holder.text.setText(query);
            holder.itemView.setOnClickListener(v -> listener.onClick(query));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class VH extends RecyclerView.ViewHolder {
            final TextView text;
            VH(TextView itemView) {
                super(itemView);
                this.text = itemView;
            }
        }
    }

}
