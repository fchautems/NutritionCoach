package ch.nutritioncoach;

import android.util.AtomicFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/** Downloads a complete offline document from the dedicated NutritionCoach release branch. */
final class UiChannel {
    static final int BUNDLED_VERSION = 4;
    private static final int MAX_HTML_BYTES = 512 * 1024;
    private static final String CHANNEL =
            "https://raw.githubusercontent.com/fchautems/NutritionCoach/ui-channel/";
    private static final Object CACHE_LOCK = new Object();
    private final AtomicFile active;
    private final AtomicFile staged;

    UiChannel(File appFiles) {
        active = new AtomicFile(new File(appFiles, "ui-active.json"));
        staged = new AtomicFile(new File(appFiles, "ui-staged.json"));
    }

    Package active() {
        synchronized (CACHE_LOCK) { return load(active); }
    }

    Package staged() {
        synchronized (CACHE_LOCK) { return load(staged); }
    }

    void discard(boolean pending) {
        synchronized (CACHE_LOCK) { (pending ? staged : active).delete(); }
    }

    void promote(Package update) throws IOException, JSONException {
        synchronized (CACHE_LOCK) {
            save(active, update);
            Package pending = load(staged);
            if (pending != null && pending.version <= update.version) staged.delete();
        }
    }

    boolean check(int currentVersion) throws IOException, JSONException {
        JSONObject manifest = new JSONObject(new String(download("channel.json", 4096),
                StandardCharsets.UTF_8));
        if (manifest.getInt("protocol") != 1) throw new IOException("Incompatible interface");
        int version = manifest.getInt("version");
        if (version <= currentVersion) return false;

        Package pending = staged();
        if (pending != null && pending.version >= version) return true;

        byte[] bytes = download("ui-" + version + ".html", MAX_HTML_BYTES);
        String checksum = manifest.getString("sha256");
        if (!matches(bytes, checksum)) throw new IOException("Invalid interface checksum");
        String html = new String(bytes, StandardCharsets.UTF_8);
        if (!html.startsWith("<!doctype html>") || !html.contains("<!-- nutrition-boot -->")) {
            throw new IOException("Invalid interface document");
        }
        synchronized (CACHE_LOCK) {
            Package installed = load(active);
            if (installed != null && installed.version >= version) return false;
            Package cached = load(staged);
            if (cached == null || cached.version < version) {
                save(staged, new Package(version, checksum, html));
            }
        }
        return true;
    }

    static boolean matches(byte[] data, String expected) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format(Locale.ROOT, "%02x", value & 255));
            return hex.toString().equals(expected);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private static byte[] download(String file, int limit) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(CHANNEL + file).openConnection();
        try {
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setUseCaches(false);
            if (connection.getResponseCode() != 200) throw new IOException("Unavailable interface");
            try (InputStream stream = connection.getInputStream()) {
                return readLimited(stream, limit);
            }
        } finally {
            connection.disconnect();
        }
    }

    static byte[] readLimited(InputStream stream, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] block = new byte[4096];
        int count;
        while ((count = stream.read(block)) != -1) {
            if (output.size() + count > limit) throw new IOException("Interface too large");
            output.write(block, 0, count);
        }
        return output.toByteArray();
    }

    private static Package load(AtomicFile file) {
        try (InputStream stream = file.openRead()) {
            JSONObject json = new JSONObject(new String(readLimited(stream, MAX_HTML_BYTES * 2),
                    StandardCharsets.UTF_8));
            int version = json.getInt("version");
            String checksum = json.getString("sha256");
            String html = json.getString("html");
            if (version <= BUNDLED_VERSION || !matches(html.getBytes(StandardCharsets.UTF_8), checksum)) {
                return null;
            }
            return new Package(version, checksum, html);
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    private static void save(AtomicFile file, Package update) throws IOException, JSONException {
        byte[] data = new JSONObject().put("version", update.version).put("sha256", update.sha256)
                .put("html", update.html).toString().getBytes(StandardCharsets.UTF_8);
        FileOutputStream stream = file.startWrite();
        try {
            stream.write(data);
            file.finishWrite(stream);
        } catch (IOException e) {
            file.failWrite(stream);
            throw e;
        }
    }

    static final class Package {
        final int version;
        final String sha256;
        final String html;

        Package(int version, String sha256, String html) {
            this.version = version;
            this.sha256 = sha256;
            this.html = html;
        }
    }
}
