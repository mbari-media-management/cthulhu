package org.mbari.cthulu.ui.player;

import org.mbari.cthulu.ui.components.mediaplayer.PlayPauseButton;
import org.tbee.javafx.scene.layout.MigPane;

/**
 * A component implementing a control panel containing various media player controls.
 */
final class MediaPlayerControls extends MigPane {

    /**
     * Associated media player component.
     */
    private final PlayerComponent playerComponent;

    private final MediaPlayerVolumeControls volumeControls;

    private final PlayPauseButton playPauseButton;

    private final TimelineComponent timelineComponent;

    /**
     * Create media player controls.
     *
     * @param playerComponent
     */
    MediaPlayerControls(PlayerComponent playerComponent) {
        super("fill, ins 6", "[sg, left][center][sg, right]", "[]0[]");

        this.playerComponent = playerComponent;

        volumeControls = new MediaPlayerVolumeControls(playerComponent);
        playPauseButton = new PlayPauseButton(playerComponent.mediaPlayer());
        timelineComponent = new TimelineComponent(playerComponent);

        add(volumeControls);
        add(playPauseButton, "wrap");
        add(timelineComponent, "span 3, grow");

        getStyleClass().add("media-player-controls");
    }
}
