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
package app.reformcloud.runner;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.runner.commands.*;
import app.reformcloud.runner.reformscript.InterpretedReformScript;
import app.reformcloud.runner.reformscript.ReformScriptInterpreter;
import app.reformcloud.runner.reformscript.basic.RunnerReformScriptInterpreter;
import app.reformcloud.runner.updater.Updater;
import app.reformcloud.runner.updater.basic.ApplicationsUpdater;
import app.reformcloud.runner.updater.basic.CloudVersionUpdater;
import app.reformcloud.runner.util.RunnerUtils;
import app.reformcloud.runner.variables.EnvNotAPIVariable;
import app.reformcloud.runner.variables.EnvSetVariable;
import app.reformcloud.runner.variables.GitCommitVariable;
import app.reformcloud.runner.variables.SetupRequiredVariable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Runner {

    private final ReformScriptInterpreter interpreter = new RunnerReformScriptInterpreter();
    private final Updater applicationsUpdater;
    private final Updater cloudVersionUpdater;
    private final String[] args;

    Runner(@NotNull String[] args) {
        this.interpreter
                .registerInterpreterCommand(new CheckForUpdatesCommand(this))
                .registerInterpreterCommand(new CheckIfDevModeCommand())
                .registerInterpreterCommand(new CheckIfSnapshotApplyCommand())
                .registerInterpreterCommand(new ExecuteCommand())
                .registerInterpreterCommand(new IfCommand())
                .registerInterpreterCommand(new PrintlnCommand())
                .registerInterpreterCommand(new SetSystemPropertiesCommand())
                .registerInterpreterCommand(new StartApplicationCommand(this))
                .registerInterpreterCommand(new UnpackApplicationCommand())
                .registerInterpreterCommand(new VariableCommand())
                .registerInterpreterCommand(new WriteEnvCommand())

                .registerInterpreterVariable(new EnvNotAPIVariable())
                .registerInterpreterVariable(new EnvSetVariable())
                .registerInterpreterVariable(new GitCommitVariable())
                .registerInterpreterVariable(new SetupRequiredVariable());

        this.applicationsUpdater = new ApplicationsUpdater(RunnerUtils.APP_UPDATE_FOLDER);
        this.cloudVersionUpdater = new CloudVersionUpdater(RunnerUtils.GLOBAL_REFORM_SCRIPT_FILE);
        this.args = args;
    }

    public void bootstrap() {
        if (Files.notExists(RunnerUtils.GLOBAL_REFORM_SCRIPT_FILE)) {
            RunnerUtils.copyCompiledFile("global.reformscript", RunnerUtils.GLOBAL_REFORM_SCRIPT_FILE);
        }

        int javaVersion = getJavaVersion();
        if (javaVersion < 23) {
            throw new RuntimeException("Using unsupported Java version! Minimum supported version is Java 23, found Java " + javaVersion);
        }

        InterpretedReformScript global = this.interpreter.interpret(RunnerUtils.GLOBAL_REFORM_SCRIPT_FILE);
        if (global == null) {
            throw new RuntimeException("Unable to interpret global reform script! Please recheck the syntax");
        }

        global.execute();
    }

    public void startApplication() {
        Path applicationFile = System.getProperties().containsKey("reformcloud.process.path")
                ? Paths.get(System.getProperty("reformcloud.process.path"))
                : RunnerUtils.EXECUTOR_PATH;
        if (Files.notExists(applicationFile) || Files.isDirectory(applicationFile)) {
            throw new UnsupportedOperationException("Unable to start non-executable file: " + applicationFile);
        }

        this.startApplication0(applicationFile);
    }

    private void startApplication0(@NotNull Path applicationFilePath) {
        try (JarFile file = new JarFile(applicationFilePath.toFile())) {
            URLClassLoader classLoader = new RunnerClassLoader(new URL[]{applicationFilePath.toUri().toURL()});
            Thread.currentThread().setContextClassLoader(classLoader);

            String mainClass = file.getManifest().getMainAttributes().getValue("Main-Class");
            Method main = classLoader.loadClass(mainClass).getMethod("main", String[].class);

            main.invoke(null, (Object) this.args);
        } catch (final IOException | ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static int getJavaVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }

        Matcher versionMatcher = Pattern.compile("\\d+").matcher(version);
        if (versionMatcher.find()) {
            version = versionMatcher.group(0);
        }

        int index = version.indexOf(".");
        if (index != -1) {
            version = version.substring(0, index);
        }
        return Integer.parseInt(version);
    }

    public Updater getApplicationsUpdater() {
        return this.applicationsUpdater;
    }

    public Updater getCloudVersionUpdater() {
        return this.cloudVersionUpdater;
    }
}
