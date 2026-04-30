package app.reformcloud.backends.application;

import app.reformcloud.application.updater.ApplicationRemoteUpdate;
import app.reformcloud.application.updater.BasicApplicationRemoteUpdate;
import app.reformcloud.application.updater.DefaultApplicationUpdateRepository;
import app.reformcloud.backends.TemplateBackendApplication;
import app.reformcloud.shared.io.DownloadHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TemplateBackendsAddonUpdater extends DefaultApplicationUpdateRepository {
    private String newVersion;

    @Override
    public void fetchOrigin() {
        DownloadHelper.connect(
                "https://raw.githubusercontent.com/reformcloudapp/reformcloud/master/version.properties",
                (connection, throwable) -> {
                    try (InputStream stream = connection.getInputStream()) {
                        Properties properties = new Properties();
                        properties.load(stream);

                        this.newVersion = properties.getProperty("version");
                    } catch (final IOException ex) {
                        ex.printStackTrace();
                    }
                });
    }

    @Override
    public boolean isNewVersionAvailable() {
        return !TemplateBackendApplication.getInstance().getApplication().getApplicationConfig().getVersion().equals(this.newVersion);
    }

    @Nullable
    @Override
    public ApplicationRemoteUpdate getUpdate() {
        if (!this.isNewVersionAvailable()) {
            return null;
        }

        String identifier = this.newVersion.endsWith("-dev") ? "snapshots" : "releases";
        String url = "http://repo.astralbe.net/" + identifier + "/app/reformcloud/template-backends/"
                + this.newVersion
                + "/template-backends-"
                + this.newVersion
                + ".jar";

        return new BasicApplicationRemoteUpdate(
                this.newVersion, url);
    }
}
