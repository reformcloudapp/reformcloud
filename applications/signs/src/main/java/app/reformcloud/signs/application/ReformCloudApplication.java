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
package app.reformcloud.signs.application;

import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.application.Application;
import app.reformcloud.application.updater.ApplicationUpdateRepository;
import app.reformcloud.configuration.JsonConfiguration;
import app.reformcloud.event.EventManager;
import app.reformcloud.network.channel.NetworkChannel;
import app.reformcloud.network.channel.manager.ChannelManager;
import app.reformcloud.network.packet.PacketProvider;
import app.reformcloud.signs.application.listener.ProcessInclusionHandler;
import app.reformcloud.signs.application.packets.PacketCreateSign;
import app.reformcloud.signs.application.packets.PacketDeleteBulkSigns;
import app.reformcloud.signs.application.packets.PacketDeleteSign;
import app.reformcloud.signs.application.packets.PacketRequestSignLayouts;
import app.reformcloud.signs.application.updater.SignsUpdater;
import app.reformcloud.signs.packets.PacketReloadSignConfig;
import app.reformcloud.signs.packets.PacketUtil;
import app.reformcloud.signs.util.SignSystemAdapter;
import app.reformcloud.signs.util.sign.CloudSign;
import app.reformcloud.signs.util.sign.config.SignConfig;
import app.reformcloud.utility.MoreCollections;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class ReformCloudApplication extends Application {

    private static final ApplicationUpdateRepository REPOSITORY = new SignsUpdater();
    private static SignConfig signConfig;
    private static JsonConfiguration databaseEntry;
    private static ReformCloudApplication instance;

    public static ReformCloudApplication getInstance() {
        return instance;
    }

    public static SignConfig getSignConfig() {
        return signConfig;
    }

    public static void insert(CloudSign cloudSign) {
        Collection<CloudSign> signs = read();
        if (signs == null) {
            return;
        }

        signs.add(cloudSign);
        databaseEntry.add("signs", signs);

        insert();
    }

    public static void delete(CloudSign cloudSign) {
        Collection<CloudSign> signs = read();
        if (signs == null) {
            return;
        }

        MoreCollections.findFirst(signs, e -> e.getLocation().equals(cloudSign.getLocation())).ifPresent(signs::remove);
        databaseEntry.add("signs", signs);

        insert();
    }

    private static Collection<CloudSign> read() {
        return databaseEntry.get("signs", TypeToken.getParameterized(Collection.class, CloudSign.class).getType());
    }

    private static void insert() {
        ExecutorAPI.getInstance().getDatabaseProvider().getDatabase(SignSystemAdapter.table).update("signs", "", databaseEntry);
    }

    @Override
    public void onLoad() {
        instance = this;
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(EventManager.class).registerListener(new ProcessInclusionHandler());
    }

    @Override
    public void onEnable() {
        Path configFile = this.getDataDirectory().resolve("layout.json");
        if (Files.notExists(configFile)) {
            ConfigHelper.createDefault(configFile);
        }

        ExecutorAPI.getInstance().getDatabaseProvider().createTable(SignSystemAdapter.table);
        if (!ExecutorAPI.getInstance().getDatabaseProvider().getDatabase(SignSystemAdapter.table).has("signs")) {
            ExecutorAPI.getInstance().getDatabaseProvider().getDatabase(SignSystemAdapter.table).insert(
                    "signs",
                    "",
                    JsonConfiguration.newJsonConfiguration().add("signs", Collections.emptyList())
            );
        }

        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(PacketProvider.class).registerPackets(Arrays.asList(
                PacketCreateSign.class,
                PacketDeleteSign.class,
                PacketDeleteBulkSigns.class,
                PacketRequestSignLayouts.class
        ));

        databaseEntry = ExecutorAPI.getInstance().getDatabaseProvider().getDatabase(SignSystemAdapter.table).get("signs", "").orElse(JsonConfiguration.newJsonConfiguration());
        signConfig = ConfigHelper.read(configFile);

        for (NetworkChannel registeredChannel : ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(ChannelManager.class).getRegisteredChannels()) {
            if (registeredChannel.isKnown()) {
                registeredChannel.sendPacket(new PacketReloadSignConfig(signConfig));
            }
        }
    }

    @Override
    public void onDisable() {
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(PacketProvider.class).unregisterPacket(PacketUtil.SIGN_BUS + 1);
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(PacketProvider.class).unregisterPacket(PacketUtil.SIGN_BUS + 2);
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(PacketProvider.class).unregisterPacket(PacketUtil.SIGN_BUS + 3);
        ExecutorAPI.getInstance().getServiceRegistry().getProviderUnchecked(PacketProvider.class).unregisterPacket(PacketUtil.SIGN_BUS + 7);
    }

    @Nullable
    @Override
    public ApplicationUpdateRepository getUpdateRepository() {
        return REPOSITORY;
    }
}
