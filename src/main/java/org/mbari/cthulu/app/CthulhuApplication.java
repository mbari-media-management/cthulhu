package org.mbari.cthulu.app;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import javafx.application.Platform;
import org.mbari.cthulu.app.config.BuildInfo;
import org.mbari.cthulu.app.config.KeyMap;
import org.mbari.cthulu.app.config.MediaPlayerConfig;
import org.mbari.cthulu.settings.Settings;
import org.mbari.cthulu.ui.player.PlayerComponent;
import org.mbari.vcr4j.sharktopoda.client.udp.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.log.NativeLog;

import java.util.UUID;

import static org.mbari.cthulu.app.config.BuildInfo.readBuildInfo;
import static org.mbari.cthulu.app.config.KeyMap.readKeyMap;
import static org.mbari.cthulu.app.config.MediaPlayerConfig.readMediaPlayerConfig;
import static org.mbari.cthulu.settings.SettingsManager.settingsManager;

/**
 * Application global state.
 */
final public class CthulhuApplication {

    private static final Logger log = LoggerFactory.getLogger(CthulhuApplication.class);

    private static final String APPLICATION_NAME = "MBARI Cthulu";

    private static final class Holder {
        private static final CthulhuApplication INSTANCE = new CthulhuApplication();
    }

    private final BuildInfo buildInfo;

    private final MediaPlayerConfig mediaPlayerConfig;

    private final KeyMap keyMap;

    private Settings settings;

    private final MediaPlayerFactory mediaPlayerFactory;

    private final PlayerComponents playerComponents;

    private final NativeLog nativeLog;

    private final IO controlIo;

    private final PublishSubject<Settings> settingsChanged = PublishSubject.create();

    private CthulhuApplication() {
        this.buildInfo = readBuildInfo();
        log.info("build version: {}", buildInfo.version());
        log.info("build timestamp: {}", buildInfo.timestamp());

        this.mediaPlayerConfig = readMediaPlayerConfig();
        log.debug("mediaPlayerConfig={}", mediaPlayerConfig);

        this.keyMap = readKeyMap();
        log.debug("keyMap={}", keyMap);

        this.settings = settingsManager().read();
        log.debug("settings={}", settings);

        this.mediaPlayerFactory = new MediaPlayerFactory(mediaPlayerConfig.libVlcArgs());

        this.playerComponents = new PlayerComponents();

        this.nativeLog = mediaPlayerFactory.application().newLog();
        this.nativeLog.addLogListener(new NativeLogHandler());

        this.controlIo = initControlPort();
    }

    /**
     * Initialise the network control port.
     * <p>
     * If the configured port number is already in use it is still possible to continue but with a disabled control
     * port.
     *
     * @return network control agent
     */
    private IO initControlPort() {
        log.debug("initControlPort()");
        try {
            int portNumber = settings.network().controlPort();
            log.debug("portNumber={}", portNumber);
            // FIXME with the current implementation of the external UDP agent library, any initialisation failure
            //       exception is swallowed by that library and it returns as normal, we do not see any exception here
            IO io = new IO(new CthuluClientController(), portNumber);
            log.info("application initialised, listening on UDP port {}", settings.network().controlPort());
            return io;
        } catch (Exception e) {
            log.error("Failed to initialise network control port", e.getMessage());
            // FIXME the idea is to add to a list of errors that can be fetched and reported by the first open stage
            return null;
        }
    }

    /**
     * Get the singleton reference to the global application state.
     *
     * @return application state
     */
    public static CthulhuApplication application() {
        return Holder.INSTANCE;
    }

    /**
     * Get the application name.
     *
     * @return name
     */
    public String applicationName() {
        return APPLICATION_NAME;
    }

    /**
     * Get the application build/version information.
     *
     * @return build information
     */
    public BuildInfo buildInfo() {
        return buildInfo;
    }

    /**
     * Get the application key-bindings.
     *
     * @return key-bindings
     */
    public KeyMap keyMap() {
        return keyMap;
    }

    /**
     * Get the current application settings.
     *
     * @return settings
     */
    public Settings settings() {
        return settings;
    }

    /**
     * Get the factory used to create native media players.
     *
     * @return media player factory
     */
    public MediaPlayerFactory mediaPlayerFactory() {
        return mediaPlayerFactory;
    }

    /**
     * Get the player component manager.
     *
     * @return player component manager
     */
    public PlayerComponents playerComponents() {
        return playerComponents;
    }

    /**
     * Get an observable that tracks when the application settings have been changed.
     *
     * @return settings changed observable
     */
    public Observable<Settings> settingsChanged() {
        return settingsChanged;
    }

    /**
     * Apply new application settings.
     *
     * @param newSettings settings to apply
     */
    public void applySettings(Settings newSettings) {
        log.debug("applySettings(newSettings={})", newSettings);

        this.settings = newSettings;
        saveSettings();

        this.settingsChanged.onNext(newSettings);
    }

    /**
     * Open a new player component.
     *
     * @return player component
     */
    public PlayerComponent open() {
        log.debug("open()");
        return playerComponents.open();
    }

    /**
     * Close a player component.
     *
     * @param uuid unique identifier of the player component to close
     */
    public void close(UUID uuid) {
        log.debug("close(uuid={})", uuid);
        playerComponents.close(uuid);
        if (playerComponents.empty()) {
            log.debug("no more player components");
            close();
        }
    }

    /**
     * Save the current settings to the configuration file.
     */
    public void saveSettings() {
        log.debug("saveSettings()");
        settingsManager().write(settings);
    }

    /**
     * Close down the application.
     */
    private void close() {
        log.debug("close()");

        if (controlIo != null) {
            controlIo.close();
        }

        nativeLog.release();
        mediaPlayerFactory.release();

        Platform.exit();
    }
}
