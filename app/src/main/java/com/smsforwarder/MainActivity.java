package com.smsforwarder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.smsforwarder.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "sms_forwarder_prefs";
    private static final String KEY_DESTINATION = "destination_number";
    private static final String KEY_KEYWORD = "keyword";
    private static final String KEY_FORWARD_ALL = "forward_all";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LOGS = "logs";
    private static final int MAX_LOG_ENTRIES = 50;

    private ActivityMainBinding binding;
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NotificationHelper.ensureChannels(this);
        configurePermissionLauncher();
        setupUi();
        restoreState();
        requestMissingPermissions();
    }

    private void configurePermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> Toast.makeText(
                        this,
                        areCorePermissionsGranted(this)
                                ? R.string.permissions_granted
                                : R.string.permissions_missing,
                        Toast.LENGTH_LONG
                ).show()
        );
    }

    private void setupUi() {
        binding.switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> updateStatusBanner(isChecked));
        binding.checkboxForwardAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.inputKeyword.setEnabled(!isChecked);
            if (isChecked) {
                binding.inputKeyword.setText("");
            }
        });

        binding.buttonRequestPermissions.setOnClickListener(v -> requestMissingPermissions());
        binding.buttonSave.setOnClickListener(v -> saveConfiguration());
        binding.buttonClearLog.setOnClickListener(v -> {
            getPrefs(this).edit().remove(KEY_LOGS).apply();
            renderLogs(Collections.emptyList());
            Toast.makeText(this, R.string.logs_cleared, Toast.LENGTH_SHORT).show();
        });
    }

    private void restoreState() {
        SharedPreferences prefs = getPrefs(this);
        binding.inputDestination.setText(prefs.getString(KEY_DESTINATION, ""));
        boolean forwardAll = prefs.getBoolean(KEY_FORWARD_ALL, false);
        binding.checkboxForwardAll.setChecked(forwardAll);
        binding.inputKeyword.setEnabled(!forwardAll);
        binding.inputKeyword.setText(prefs.getString(KEY_KEYWORD, ""));
        boolean enabled = prefs.getBoolean(KEY_ENABLED, false);
        binding.switchEnable.setChecked(enabled);
        updateStatusBanner(enabled);
        renderLogs(getLogs(this));
    }

    private void saveConfiguration() {
        String destination = binding.inputDestination.getText().toString().trim();
        String keyword = binding.inputKeyword.getText().toString().trim();
        boolean forwardAll = binding.checkboxForwardAll.isChecked();
        boolean enabled = binding.switchEnable.isChecked();

        if (TextUtils.isEmpty(destination)) {
            binding.inputDestination.setError(getString(R.string.error_destination_required));
            return;
        }

        if (!forwardAll && TextUtils.isEmpty(keyword)) {
            binding.inputKeyword.setError(getString(R.string.error_keyword_required));
            return;
        }

        getPrefs(this).edit()
                .putString(KEY_DESTINATION, destination)
                .putString(KEY_KEYWORD, keyword)
                .putBoolean(KEY_FORWARD_ALL, forwardAll)
                .putBoolean(KEY_ENABLED, enabled)
                .apply();

        if (enabled) {
            startForwarderService();
        } else {
            stopService(new Intent(this, ForwarderService.class));
        }

        appendLog(this, getString(R.string.log_configuration_saved));
        renderLogs(getLogs(this));
        Toast.makeText(this, R.string.configuration_saved, Toast.LENGTH_SHORT).show();
    }

    private void startForwarderService() {
        Intent intent = new Intent(this, ForwarderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void requestMissingPermissions() {
        List<String> missingPermissions = new ArrayList<>();
        addIfMissing(missingPermissions, Manifest.permission.RECEIVE_SMS);
        addIfMissing(missingPermissions, Manifest.permission.READ_SMS);
        addIfMissing(missingPermissions, Manifest.permission.SEND_SMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addIfMissing(missingPermissions, Manifest.permission.POST_NOTIFICATIONS);
        }

        if (!missingPermissions.isEmpty()) {
            permissionLauncher.launch(missingPermissions.toArray(new String[0]));
        }
    }

    private void addIfMissing(List<String> permissions, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission);
        }
    }

    private void updateStatusBanner(boolean enabled) {
        binding.textStatus.setText(enabled ? R.string.status_enabled : R.string.status_disabled);
        int color = ContextCompat.getColor(this, enabled ? R.color.success : R.color.warning);
        binding.cardStatus.setCardBackgroundColor(color);
    }

    private void renderLogs(List<String> logs) {
        if (logs.isEmpty()) {
            binding.textLogs.setText(R.string.no_logs);
            return;
        }

        binding.textLogs.setText(TextUtils.join("\n\n", logs));
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean areCorePermissionsGranted(Context context) {
        List<String> permissions = new ArrayList<>(Arrays.asList(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS
        ));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean isForwardingEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_ENABLED, false);
    }

    public static boolean isForwardAllEnabled(Context context) {
        return getPrefs(context).getBoolean(KEY_FORWARD_ALL, false);
    }

    public static String getKeyword(Context context) {
        return getPrefs(context).getString(KEY_KEYWORD, "");
    }

    public static String getDestinationNumber(Context context) {
        return getPrefs(context).getString(KEY_DESTINATION, "");
    }

    public static void appendLog(Context context, @NonNull String entry) {
        SharedPreferences prefs = getPrefs(context);
        List<String> logs = new ArrayList<>(getLogs(context));
        logs.add(0, entry);
        if (logs.size() > MAX_LOG_ENTRIES) {
            logs = logs.subList(0, MAX_LOG_ENTRIES);
        }
        prefs.edit().putString(KEY_LOGS, TextUtils.join("|||", logs)).apply();
    }

    public static List<String> getLogs(Context context) {
        String rawLogs = getPrefs(context).getString(KEY_LOGS, "");
        if (TextUtils.isEmpty(rawLogs)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(rawLogs.split("\\|\\|\\|")));
    }
}
