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
package app.reformcloud.embedded;

import org.jetbrains.annotations.NotNull;
import app.reformcloud.embedded.cache.DefaultProcessCache;
import app.reformcloud.embedded.cache.ProcessCache;
import app.reformcloud.embedded.config.EmbeddedConfig;
import app.reformcloud.embedded.database.DefaultEmbeddedDatabaseProvider;
import app.reformcloud.embedded.group.DefaultEmbeddedMainGroupProvider;
import app.reformcloud.embedded.group.DefaultEmbeddedProcessGroupProvider;
import app.reformcloud.embedded.messaging.DefaultEmbeddedChannelMessageProvider;
import app.reformcloud.embedded.network.EmbeddedChannelListener;
import app.reformcloud.embedded.node.DefaultEmbeddedNodeInformationProvider;
import app.reformcloud.embedded.player.DefaultEmbeddedPlayerProvider;
import app.reformcloud.embedded.process.DefaultEmbeddedProcessProvider;
import app.reformcloud.embedded.processors.*;
import app.reformcloud.ExecutorAPI;
import app.reformcloud.ExecutorType;
import app.reformcloud.dependency.DependencyLoader;
import app.reformcloud.event.EventManager;
import app.reformcloud.event.events.process.ProcessUpdateEvent;
import app.reformcloud.event.handler.Listener;
import app.reformcloud.group.messages.IngameMessages;
import app.reformcloud.group.process.player.PlayerAccessConfiguration;
import app.reformcloud.network.channel.NetworkChannel;
import app.reformcloud.network.channel.manager.ChannelManager;
import app.reformcloud.network.client.NetworkClient;
import app.reformcloud.network.packet.Packet;
import app.reformcloud.network.packet.PacketProvider;
import app.reformcloud.network.packet.query.QueryManager;
import app.reformcloud.process.ProcessInformation;
import app.reformcloud.protocol.node.ApiToNodeGetIngameMessages;
import app.reformcloud.protocol.node.ApiToNodeGetIngameMessagesResult;
import app.reformcloud.protocol.processor.PacketProcessorManager;
import app.reformcloud.protocol.shared.*;
import app.reformcloud.provider.*;
import app.reformcloud.registry.service.ServiceRegistry;
import app.reformcloud.shared.dependency.DefaultDependencyLoader;
import app.reformcloud.shared.event.DefaultEventManager;
import app.reformcloud.shared.json.GsonFactories;
import app.reformcloud.shared.network.channel.DefaultChannelManager;
import app.reformcloud.shared.network.client.DefaultNetworkClient;
import app.reformcloud.shared.network.packet.DefaultPacketProvider;
import app.reformcloud.shared.network.packet.DefaultQueryManager;
import app.reformcloud.shared.platform.Platform;
import app.reformcloud.shared.registry.service.DefaultServiceRegistry;
import app.reformcloud.task.Task;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This class can only get called if the environment is {@link ExecutorType#API}.
 * Check this by using {@link ExecutorAPI#getType()}. If the current instance is not an api instance
 * just use the default cloud api based on {@link ExecutorAPI#getInstance()}.
 */
public abstract class Embedded extends ExecutorAPI {

    protected final ServiceRegistry serviceRegistry = new DefaultServiceRegistry();
    protected final NetworkClient networkClient = new DefaultNetworkClient();
    protected final EmbeddedConfig config;

    private final ProcessProvider processProvider;
    private final DatabaseProvider databaseProvider = new DefaultEmbeddedDatabaseProvider();
    private final ChannelMessageProvider channelMessageProvider = new DefaultEmbeddedChannelMessageProvider();
    private final NodeInformationProvider nodeInformationProvider = new DefaultEmbeddedNodeInformationProvider();
    private final PlayerProvider playerProvider = new DefaultEmbeddedPlayerProvider();
    private final MainGroupProvider mainGroupProvider = new DefaultEmbeddedMainGroupProvider();
    private final ProcessGroupProvider processGroupProvider = new DefaultEmbeddedProcessGroupProvider();
    private final DependencyLoader dependencyLoader = new DefaultDependencyLoader();

    protected int maxPlayers;
    protected ProcessInformation processInformation;
    protected IngameMessages ingameMessages = new IngameMessages();

    protected Embedded() {
        GsonFactories.init();
        ExecutorAPI.setInstance(this);

        this.serviceRegistry.setProvider(EventManager.class, new DefaultEventManager(), false, true);
        this.serviceRegistry.setProvider(ChannelManager.class, new DefaultChannelManager(), true);
        this.serviceRegistry.setProvider(PacketProvider.class, new DefaultPacketProvider(), false, true);
        this.serviceRegistry.setProvider(QueryManager.class, new DefaultQueryManager(), false, true);
        this.serviceRegistry.setProvider(ProcessCache.class, new DefaultProcessCache(), false, true);

        this.processProvider = new DefaultEmbeddedProcessProvider(); // after service registry init
        this.serviceRegistry.getProviderUnchecked(EventManager.class).registerListener(new CurrentProcessUpdateEventListener());

        this.config = new EmbeddedConfig();
        this.processInformation = this.config.getProcessInformation();

        Lock lock = new ReentrantLock();
        try {
            lock.lock();
            Condition condition = lock.newCondition();

            this.networkClient.connect(
                    this.config.getConnectionHost(),
                    this.config.getConnectionPort(),
                    channel -> new EmbeddedChannelListener(channel, lock, condition)
            );

            try {
                if (!condition.await(30, TimeUnit.SECONDS)) {
                    System.exit(-1);
                }
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }

            if (this.serviceRegistry.getProviderUnchecked(ChannelManager.class).getFirstChannel().isEmpty()) {
                System.exit(-1);
            }
        } finally {
            lock.unlock();
        }

        this.sendSyncQuery(new ApiToNodeGetIngameMessages()).ifPresent(result -> {
            if (result instanceof ApiToNodeGetIngameMessagesResult) {
                this.ingameMessages = ((ApiToNodeGetIngameMessagesResult) result).getMessages();
            }
        });

        this.processInformation.setCurrentState(this.processInformation.getInitialState());

        PacketProcessorManager.getInstance()
                .registerProcessor(new ChannelMessageProcessor(), PacketChannelMessage.class)
                .registerProcessor(new PacketConnectPlayerToServerProcessor(), PacketConnectPlayerToServer.class)
                .registerProcessor(new PacketDisconnectPlayerProcessor(), PacketDisconnectPlayer.class)
                .registerProcessor(new PacketSendPlayerMessageProcessor(), PacketSendPlayerMessage.class)
                .registerProcessor(new PacketSendPlayerTitleProcessor(), PacketSendPlayerTitle.class);

        Runtime.getRuntime().addShutdownHook(new Thread(this.networkClient::closeSync));
        this.updateCurrentProcessInformation();
    }

    @NotNull
    public static Embedded getInstance() {
        return (Embedded) ExecutorAPI.getInstance();
    }

    @NotNull
    @Override
    public ChannelMessageProvider getChannelMessageProvider() {
        return this.channelMessageProvider;
    }

    @NotNull
    @Override
    public DatabaseProvider getDatabaseProvider() {
        return this.databaseProvider;
    }

    @NotNull
    @Override
    public MainGroupProvider getMainGroupProvider() {
        return this.mainGroupProvider;
    }

    @NotNull
    @Override
    public NodeInformationProvider getNodeInformationProvider() {
        return this.nodeInformationProvider;
    }

    @NotNull
    @Override
    public PlayerProvider getPlayerProvider() {
        return this.playerProvider;
    }

    @NotNull
    @Override
    public ProcessGroupProvider getProcessGroupProvider() {
        return this.processGroupProvider;
    }

    @NotNull
    @Override
    public ProcessProvider getProcessProvider() {
        return this.processProvider;
    }

    @NotNull
    @Override
    public ServiceRegistry getServiceRegistry() {
        return this.serviceRegistry;
    }

    @NotNull
    @Override
    public DependencyLoader getDependencyLoader() {
        return this.dependencyLoader;
    }

    @Override
    public boolean isReady() {
        return this.serviceRegistry.getProviderUnchecked(ChannelManager.class).getFirstChannel().isPresent();
    }

    @NotNull
    public ProcessInformation getCurrentProcessInformation() {
        return this.processInformation;
    }

    @NotNull
    public EmbeddedConfig getConfig() {
        return this.config;
    }

    @NotNull
    public IngameMessages getIngameMessages() {
        return this.ingameMessages;
    }

    public void sendPacket(@NotNull Packet packet) {
        this.serviceRegistry.getProviderUnchecked(ChannelManager.class).getFirstChannel().ifPresent(e -> e.sendPacket(packet));
    }

    @NotNull
    public Task<Packet> sendQuery(@NotNull Packet packet) {
        Optional<NetworkChannel> channel = this.serviceRegistry.getProviderUnchecked(ChannelManager.class).getFirstChannel();
        return channel
                .map(networkChannel -> this.serviceRegistry.getProviderUnchecked(QueryManager.class).sendPacketQuery(networkChannel, packet))
                .orElseGet(() -> Task.completedTask(null));
    }

    @NotNull
    public Optional<Packet> sendSyncQuery(@NotNull Packet packet) {
        Packet result = this.sendQuery(packet).getUninterruptedly(TimeUnit.SECONDS, 5);
        return Optional.ofNullable(result);
    }

    public void updateCurrentProcessInformation() {
        this.processInformation.setRuntimeInformation(Platform.createProcessRuntimeInformation());
        this.updatePlayersOfEnvironment(this.processInformation);
        this.processProvider.updateProcessInformation(this.processInformation);
    }

    protected void updateMaxPlayers() {
        final PlayerAccessConfiguration configuration = this.processInformation.getProcessGroup().getPlayerAccessConfiguration();
        if (configuration.isUsePlayerLimit() && configuration.getMaxPlayers() >= 0) {
            this.maxPlayers = configuration.getMaxPlayers();
        } else {
            this.maxPlayers = this.getMaxPlayersOfEnvironment();
        }
    }

    public int getMaxPlayers() {
        this.updateMaxPlayers();
        return this.maxPlayers;
    }

    @NotNull
    public ProcessCache getProcessCache() {
        return ServiceRegistry.getUnchecked(ProcessCache.class);
    }

    public abstract int getPlayerCount();

    protected abstract int getMaxPlayersOfEnvironment();

    protected abstract void updatePlayersOfEnvironment(@NotNull ProcessInformation information);

    public final class CurrentProcessUpdateEventListener {

        @Listener
        public void handle(@NotNull ProcessUpdateEvent event) {
            if (Embedded.this.processInformation.getId().getUniqueId().equals(event.getProcessInformation().getId().getUniqueId())) {
                Embedded.this.processInformation = event.getProcessInformation();
            }
        }
    }
}
