package com.darktile.openhubx.ui.widget;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;

import com.darktile.openhubx.R;
import com.darktile.openhubx.databinding.DialogSearchFilterBinding;
import com.darktile.openhubx.mvp.model.SearchFilter;
import com.darktile.openhubx.mvp.model.TrendingLanguage;
import com.darktile.openhubx.util.StringUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Bottom sheet for the search language + advanced qualifiers. */
public class SearchFilterSheet {

    public interface Listener {
        void onFilterApplied(@NonNull SearchFilter filter);
    }

    private static final SearchFilter.ForkMode[] FORK_MODES = {
            SearchFilter.ForkMode.DEFAULT, SearchFilter.ForkMode.INCLUDE, SearchFilter.ForkMode.ONLY
    };

    private final BottomSheetDialog dialog;
    private final DialogSearchFilterBinding binding;
    private final List<TrendingLanguage> languages;

    private SearchFilterSheet(@NonNull Context context, @NonNull SearchFilter current,
                              @NonNull List<TrendingLanguage> languages, @NonNull Listener listener) {
        this.languages = languages;
        dialog = new BottomSheetDialog(context);
        binding = DialogSearchFilterBinding.inflate(LayoutInflater.from(context));
        dialog.setContentView(binding.getRoot());

        List<String> languageNames = new ArrayList<>();
        languageNames.add(context.getString(R.string.any_language));
        for (TrendingLanguage language : languages) {
            languageNames.add(language.getName());
        }
        binding.languageSpinner.setAdapter(simpleAdapter(context, languageNames));

        List<String> forkNames = new ArrayList<>();
        forkNames.add(context.getString(R.string.fork_default));
        forkNames.add(context.getString(R.string.fork_include));
        forkNames.add(context.getString(R.string.fork_only));
        binding.forkSpinner.setAdapter(simpleAdapter(context, forkNames));

        binding.pushedInput.setOnClickListener(v -> pickDate(context));
        binding.pushedInput.setOnLongClickListener(v -> {
            binding.pushedInput.setText("");
            return true;
        });
        binding.resetButton.setOnClickListener(v -> bind(new SearchFilter()));
        binding.applyButton.setOnClickListener(v -> {
            listener.onFilterApplied(read());
            dialog.dismiss();
        });
        bind(current);
    }

    public static void show(@NonNull Context context, @NonNull SearchFilter current,
                            @NonNull List<TrendingLanguage> languages, @NonNull Listener listener) {
        new SearchFilterSheet(context, current, languages, listener).dialog.show();
    }

    private static ArrayAdapter<String> simpleAdapter(Context context, List<String> items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private void bind(@NonNull SearchFilter filter) {
        int languageIndex = 0;
        for (int i = 0; i < languages.size(); i++) {
            if (languages.get(i).getSlug().equalsIgnoreCase(String.valueOf(filter.getLanguage()))) {
                languageIndex = i + 1;
                break;
            }
        }
        binding.languageSpinner.setSelection(languageIndex);
        binding.ownerInput.setText(nullToEmpty(filter.getOwner()));
        binding.ownerTypeGroup.check(filter.isOwnerIsOrg() ? R.id.owner_org : R.id.owner_user);
        Integer stars = filter.getMinStars();
        binding.starsInput.setText(stars == null || stars <= 0 ? "" : String.valueOf(stars));
        binding.pushedInput.setText(nullToEmpty(filter.getPushedAfter()));
        int forkIndex = 0;
        for (int i = 0; i < FORK_MODES.length; i++) {
            if (FORK_MODES[i] == filter.getForkMode()) forkIndex = i;
        }
        binding.forkSpinner.setSelection(forkIndex);
        binding.topicInput.setText(nullToEmpty(filter.getTopic()));
        binding.licenseInput.setText(nullToEmpty(filter.getLicense()));
    }

    @NonNull
    private SearchFilter read() {
        SearchFilter filter = new SearchFilter();
        int languageIndex = binding.languageSpinner.getSelectedItemPosition();
        if (languageIndex > 0 && languageIndex <= languages.size()) {
            filter.setLanguage(languages.get(languageIndex - 1).getSlug());
        }
        filter.setOwner(emptyToNull(binding.ownerInput.getText().toString()),
                binding.ownerTypeGroup.getCheckedRadioButtonId() == R.id.owner_org);
        String stars = binding.starsInput.getText().toString().trim();
        if (!stars.isEmpty()) {
            try {
                filter.setMinStars(Integer.parseInt(stars));
            } catch (NumberFormatException ignored) {
                // maxLength + number input keep this unreachable in practice
            }
        }
        filter.setPushedAfter(emptyToNull(binding.pushedInput.getText().toString()));
        int forkIndex = binding.forkSpinner.getSelectedItemPosition();
        filter.setForkMode(forkIndex >= 0 && forkIndex < FORK_MODES.length
                ? FORK_MODES[forkIndex] : SearchFilter.ForkMode.DEFAULT);
        filter.setTopic(emptyToNull(binding.topicInput.getText().toString()));
        filter.setLicense(emptyToNull(binding.licenseInput.getText().toString()));
        return filter;
    }

    private void pickDate(Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -6);
        new DatePickerDialog(context, (view, year, month, day) ->
                binding.pushedInput.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyToNull(String s) {
        return StringUtils.isBlank(s) ? null : s.trim();
    }
}
