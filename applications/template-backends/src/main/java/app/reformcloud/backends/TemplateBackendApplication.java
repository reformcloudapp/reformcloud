/*
 * This file is part of reformcloud, licensed under the MIT License (MIT).
 *
 * Copyright (c) ReformCloud <https://github.com/reformcloudapp>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package app.reformcloud.backends;

import app.reformcloud.ExecutorAPI;
import app.reformcloud.application.Application;
import app.reformcloud.application.updater.ApplicationUpdateRepository;
import app.reformcloud.backends.application.TemplateBackendsAddonUpdater;
import app.reformcloud.backends.ftp.FTPTemplateBackend;
import app.reformcloud.backends.sftp.SFTPTemplateBackend;
import app.reformcloud.backends.url.URLTemplateBackend;
import app.reformcloud.shared.dependency.DependencyFileLoader;
import org.jetbrains.annotations.Nullable;

public class TemplateBackendApplication extends Application {

    private static final ApplicationUpdateRepository REPOSITORY = new TemplateBackendsAddonUpdater();

    private static TemplateBackendApplication instance;

    public static TemplateBackendApplication getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;

        ExecutorAPI.getInstance().getDependencyLoader().load(
                DependencyFileLoader.collectDependenciesFromFile(TemplateBackendApplication.class.getClassLoader().getResourceAsStream("dependencies.txt"))
        );

        FTPTemplateBackend.load(this.getDataDirectory().resolve("ftp.json"));
        SFTPTemplateBackend.load(this.getDataDirectory().resolve("sftp.json"));
        URLTemplateBackend.load(this.getDataDirectory().resolve("url.json"));
    }

    @Override
    public void onDisable() {
        URLTemplateBackend.unload();
        FTPTemplateBackend.unload();
        SFTPTemplateBackend.unload();
    }

    @Nullable
    @Override
    public ApplicationUpdateRepository getUpdateRepository() {
        return REPOSITORY;
    }
}
