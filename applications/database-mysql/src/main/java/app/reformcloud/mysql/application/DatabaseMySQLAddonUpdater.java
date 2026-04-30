package app.reformcloud.mysql.application;

import app.reformcloud.application.updater.ApplicationRemoteUpdate;
import app.reformcloud.application.updater.BasicApplicationRemoteUpdate;
import app.reformcloud.application.updater.DefaultApplicationUpdateRepository;
import app.reformcloud.mysql.MySQLApplication;
import app.reformcloud.shared.io.DownloadHelper;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseMySQLAddonUpdater extends DefaultApplicationUpdateRepository {
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
        return !MySQLApplication.getInstance().getApplication().getApplicationConfig().getVersion().equals(this.newVersion);
    }

    @Nullable
    @Override
    public ApplicationRemoteUpdate getUpdate() {
        if (!this.isNewVersionAvailable()) {
            return null;
        }

        String identifier = this.newVersion.endsWith("-dev") ? "snapshots" : "releases";
        String url = "http://repo.astralbe.net/" + identifier + "/app/reformcloud/database-mysql/"
                + this.newVersion
                + "/database-mysql-"
                + this.newVersion
                + ".jar";

        return new BasicApplicationRemoteUpdate(
                this.newVersion, url);
    }
}
