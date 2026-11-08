package ch.nutritioncoach;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int CREAM = Color.rgb(252, 248, 241);
    private static final String ORIGIN = "https://appassets.androidplatform.net";
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private static final int WITHINGS_PERMISSION = 42;
    private static final String REJECTED_VERSION = "rejected_interface_v1";

    private WebView web;
    private SharedPreferences prefs;
    private UiChannel channel;
    private JournalStore journal;
    private UiChannel.Package loading;
    private boolean loadingStaged;
    private boolean ready;
    private boolean checkedAtStartup;
    private volatile int version;
    private volatile int loadId;
    private volatile String status = "idle";
    private String bundledHtml;
    private volatile boolean withingsSupported;
    private volatile String withingsStatus = "idle";
    private volatile int withingsAdded;
    private boolean requestingWithings;

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(CREAM);
        getWindow().setNavigationBarColor(CREAM);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        prefs = getSharedPreferences("nutrition_coach", MODE_PRIVATE);
        journal = new JournalStore(prefs);
        channel = new UiChannel(getFilesDir());
        withingsSupported = Build.VERSION.SDK_INT >= 34 && WithingsReader.available(this);
        try (InputStream stream = getAssets().open("nutritioncoach.html")) {
            bundledHtml = new String(UiChannel.readLimited(stream, 512 * 1024), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("The offline interface is missing", e);
        }

        web = new WebView(this);
        web.setBackgroundColor(CREAM);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.getSettings().setBlockNetworkLoads(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true;
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // The whole document is loaded by the host. It needs no network subresources.
                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream(new byte[0]));
            }
        });
        web.addJavascriptInterface(new Bridge(), "NutritionHost");
        setContentView(web);

        UiChannel.Package staged = channel.staged();
        openInterface(staged != null ? staged : channel.active(), staged != null);
    }

    private void openInterface(UiChannel.Package candidate, boolean staged) {
        ready = false;
        loading = candidate;
        loadingStaged = staged;
        version = candidate == null ? UiChannel.BUNDLED_VERSION : candidate.version;
        int opening = ++loadId;
        String html = candidate == null ? bundledHtml : candidate.html;
        String document = html.replace("<!-- nutrition-boot -->",
                "<script>window.nutritionOpening = " + opening + ";</script>");
        web.loadDataWithBaseURL(ORIGIN + "/", document, "text/html", "UTF-8", null);
        if (candidate != null) {
            ui.postDelayed(() -> {
                if (opening != loadId || ready || isDestroyed()) return;
                prefs.edit().putInt(REJECTED_VERSION, candidate.version).apply();
                channel.discard(staged);
                status = "failed";
                openInterface(staged ? channel.active() : null, false);
            }, 5000);
        }
    }

    private void interfaceReady(int opening) {
        if (opening != loadId || ready || isDestroyed()) return;
        ready = true;
        web.clearHistory();
        if (loadingStaged) {
            try {
                channel.promote(loading);
                status = "applied";
            } catch (IOException | JSONException e) {
                status = "failed";
            }
            announceStatus();
        }
        if (!checkedAtStartup) {
            checkedAtStartup = true;
            checkUpdate();
        }
    }

    private void checkUpdate() {
        if (isDestroyed() || !ready || "checking".equals(status)) return;
        int currentVersion = Math.max(version, prefs.getInt(REJECTED_VERSION, 0));
        status = "checking";
        announceStatus();
        worker.execute(() -> {
            String result;
            try {
                result = channel.check(currentVersion) ? "available" : "current";
            } catch (IOException | JSONException e) {
                UiChannel.Package staged = channel.staged();
                result = staged != null && staged.version > currentVersion ? "available" : "unavailable";
            }
            String next = result;
            ui.post(() -> {
                if (isDestroyed()) return;
                status = next;
                announceStatus();
            });
        });
    }

    private void announceStatus() {
        web.evaluateJavascript("window.dispatchEvent(new Event('nutrition-update'))", null);
    }

    @Override protected void onResume() {
        super.onResume();
        if (withingsSupported && hasWeightPermission()) importWithings(false);
    }

    private boolean hasWeightPermission() {
        return checkSelfPermission(WithingsReader.WEIGHT) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean supportsHistoryPermission() {
        try {
            getPackageManager().getPermissionInfo(WithingsReader.HISTORY, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void importWithings(boolean interactive) {
        if (!withingsSupported || isDestroyed() || requestingWithings || "reading".equals(withingsStatus)) return;
        if (!hasWeightPermission()) {
            if (!interactive) return;
            requestingWithings = true;
            requestPermissions(supportsHistoryPermission()
                    ? new String[]{WithingsReader.WEIGHT, WithingsReader.HISTORY}
                    : new String[]{WithingsReader.WEIGHT}, WITHINGS_PERMISSION);
            return;
        }

        withingsAdded = 0;
        setWithingsStatus("reading");
        boolean history = checkSelfPermission(WithingsReader.HISTORY) == PackageManager.PERMISSION_GRANTED;
        WithingsReader.read(this, history, getMainExecutor(), records -> {
            if (isDestroyed()) return;
            worker.execute(() -> {
                int added;
                try {
                    added = journal.mergeWithings(records);
                } catch (JSONException e) {
                    added = -1;
                }
                int count = added;
                ui.post(() -> {
                    if (isDestroyed()) return;
                    withingsAdded = Math.max(0, count);
                    setWithingsStatus(count < 0 ? "error" : records.length() == 0 ? "empty" : "done");
                });
            });
        }, result -> {
            if (!isDestroyed()) setWithingsStatus(result);
        });
    }

    private void setWithingsStatus(String status) {
        withingsStatus = status;
        web.evaluateJavascript("window.dispatchEvent(new Event('nutrition-withings'))", null);
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request, permissions, grants);
        if (request != WITHINGS_PERMISSION) return;
        requestingWithings = false;
        if (hasWeightPermission()) importWithings(false);
        else setWithingsStatus("denied");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        web.evaluateJavascript("typeof window.nutritionBack === 'function' && window.nutritionBack()",
                result -> {
                    if (!isDestroyed() && !"true".equals(result)) super.onBackPressed();
                });
    }

    @Override
    protected void onDestroy() {
        ui.removeCallbacksAndMessages(null);
        worker.shutdownNow();
        web.removeJavascriptInterface("NutritionHost");
        web.destroy();
        super.onDestroy();
    }

    private final class Bridge {
        @JavascriptInterface public String state() {
            try {
                return journal.read();
            } catch (JSONException e) {
                return "{\"error\":true}";
            }
        }

        @JavascriptInterface public boolean saveProfile(String name, String sex, String cm, String kg) {
            try {
                return journal.saveProfile(name, sex, cm, kg);
            } catch (JSONException e) {
                return false;
            }
        }

        @JavascriptInterface public boolean saveProfileDetails(String payload) {
            try {
                return journal.saveProfileDetails(payload, LocalDate.now());
            } catch (JSONException e) {
                return false;
            }
        }

        @JavascriptInterface public boolean addEntry(String date, String meal, String name, String kcal) {
            try {
                return journal.addEntry(date, meal, name, kcal);
            } catch (JSONException e) {
                return false;
            }
        }

        @JavascriptInterface public boolean deleteEntry(String id) {
            try {
                return journal.deleteEntry(id);
            } catch (JSONException e) {
                return false;
            }
        }

        @JavascriptInterface public boolean saveGoal(String kcal) {
            return journal.saveGoal(kcal);
        }

        @JavascriptInterface public String withingsInfo() {
            return "{\"available\":" + withingsSupported + ",\"status\":" + JSONObject.quote(withingsStatus)
                    + ",\"added\":" + withingsAdded + "}";
        }

        @JavascriptInterface public void importWithings() {
            ui.post(() -> MainActivity.this.importWithings(true));
        }

        @JavascriptInterface public void ready(int opening) {
            ui.post(() -> interfaceReady(opening));
        }

        @JavascriptInterface public String updateInfo() {
            return "{\"version\":" + version + ",\"status\":" + JSONObject.quote(status) + "}";
        }

        @JavascriptInterface public void checkUpdate() {
            ui.post(MainActivity.this::checkUpdate);
        }

        @JavascriptInterface public void applyUpdate() {
            ui.post(() -> {
                if (isDestroyed() || !"available".equals(status)) return;
                UiChannel.Package staged = channel.staged();
                if (staged != null && staged.version > version) openInterface(staged, true);
            });
        }
    }
}
