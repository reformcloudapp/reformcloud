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
package app.reformcloud.file;

import app.reformcloud.ExecutorAPI;
import app.reformcloud.application.Application;
import app.reformcloud.application.updater.ApplicationUpdateRepository;
import app.reformcloud.file.application.DatabaseFileAddonUpdater;
import app.reformcloud.provider.DatabaseProvider;
import app.reformcloud.shared.dependency.DependencyFileLoader;
import org.jetbrains.annotations.Nullable;

public class FileDatabase extends Application {

    private static final ApplicationUpdateRepository REPOSITORY = new DatabaseFileAddonUpdater();

    private static FileDatabase instance;
    private DatabaseProvider before;

    public static FileDatabase getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;

        ExecutorAPI.getInstance().getDependencyLoader().load(
                DependencyFileLoader.collectDependenciesFromFile(FileDatabase.class.getClassLoader().getResourceAsStream("dependencies.txt"))
        );

        this.before = ExecutorAPI.getInstance().getDatabaseProvider();
        ExecutorAPI.getInstance().getServiceRegistry().setProvider(DatabaseProvider.class, new FileDatabaseProvider(), false, true);
    }

    @Override
    public void onDisable() {
        ExecutorAPI.getInstance().getServiceRegistry().setProvider(DatabaseProvider.class, this.before, false, true);
    }

    @Nullable
    @Override
    public ApplicationUpdateRepository getUpdateRepository() {
        return REPOSITORY;
    }
}
