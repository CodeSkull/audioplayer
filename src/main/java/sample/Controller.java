package sample;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.*;

import static java.lang.Math.floor;
import static java.lang.String.format;


public class Controller implements Initializable {

    private List<File> songFiles = new ArrayList<>();

    private static final int BANDS = 32;
    private File currentSongFile;
    private Media currentMediaLoaded;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private Duration duration = new Duration(0.0);
    private XYChart.Data[] series1Data;

    @FXML
    private Label songCurrentTime = new Label();
    @FXML
    private Button playPauseButton;
    @FXML
    private Button nextSongButton;
    @FXML
    private Button prevSongButton;
    @FXML
    private TextField songFolderPath;
    @FXML
    private ProgressBar songProgBar;
    @FXML
    private ComboBox<String> songListComboBox;
    @FXML
    private AreaChart<String, Number> spectrumChart;
    @FXML
    private Button browsePathButton;
    private Parent directoryPane;
    private Stage directory;
    @FXML
    private Slider songLengthSlider = new Slider();
    @FXML
    private Slider volumeSlider = new Slider();
    @FXML
    private Label volumeLabel;
    private double volume;

    //Initialising visualizer axes
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        series1Data = new XYChart.Data[BANDS + 2];
        for (int i = 0; i < series1Data.length; i++) {
            series1Data[i] = new XYChart.Data<>(Integer.toString(i + 1), 0);
            series1.getData().add(series1Data[i]);
        }
        spectrumChart.getData().add(series1);

    }

    //Prompts user to select path to songs folder
    public void chooseSongDirectory(ActionEvent event) {
        DirectoryChooser dc = new DirectoryChooser();
        File selectedDirectory = dc.showDialog(directory);

        if (selectedDirectory == null) {
            songFolderPath.setText("No Directory Selected");
        } else {
            songFolderPath.setText(selectedDirectory.getAbsolutePath());
            this.initialiseSongList(songFolderPath.getText());
        }
    }

    //Spectrum listener class for spectrum chart
    private class spectrumListener implements AudioSpectrumListener {

        @Override
        public void spectrumDataUpdate(double timestamp, double duration, float[] magnitudes, float[] phases) {
            for (int i = 0; i < magnitudes.length; i++) {
                series1Data[i].setYValue(magnitudes[i] - mediaPlayer.getAudioSpectrumThreshold());
            }
        }
    }

    //Creates buffer which makes visualizer look smoother
    private float[] createFilledBuffer(int size, float fillValue) {
        float[] floats = new float[size];
        Arrays.fill(floats, fillValue);
        return floats;
    }

    //Initialises song list combo box
    private void initialiseSongList(String songFolderPath) {
        ObservableList<String> songList = FXCollections.observableArrayList(loadSongs(songFolderPath));
        songListComboBox.setItems(songList);
    }

    //Returns a list of song names to be loaded into combo box
    private List<String> loadSongs(String songFolderPath) {
        File dir = new File(songFolderPath);
        File[] songsDirectory = dir.listFiles();
        List<String> songNameList = new ArrayList<>();
        if (songsDirectory != null) {
            for (File song : songsDirectory) {
                songFiles.add(song);
                songNameList.add(song.getName());
            }
        }
        return songNameList;
    }

    //Listens to combo box selection
    public void selectionChanged(ActionEvent event) {
        String songSelected = songListComboBox.getValue();
        for (File song : songFiles) {
            if (song.getName().equals(songSelected)) {
                this.currentSongFile = song;
                //If song has .mp3 extension, load file into media variable
                try {
                    String mediaPath = this.currentSongFile.toURI().toASCIIString();
                    //Stops current song playing when changing songs
                    if (isPlaying) {
                        this.mediaPlayer.stop();
                        this.isPlaying = false;
                    }
                    this.currentMediaLoaded = new Media(mediaPath);
                    this.mediaPlayer = new MediaPlayer(this.currentMediaLoaded);
                    this.componentsInit();
                    this.timeInit();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Functionality for play/pause button
    public void playPauseSong() {
        String currentSongName = currentSongFile.getName();
        if (this.currentMediaLoaded != null && currentSongName.substring(currentSongName.length() - 4).equals(".mp3")) {
            if (!isPlaying) {
                this.mediaPlayer.play();
                isPlaying = true;
            } else {
                this.mediaPlayer.pause();
                isPlaying = false;
            }
        }
        else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error Dialog");
            alert.setHeaderText("Invalid file type selected");
            alert.setContentText("Please select an mp3 file.");
            alert.showAndWait();
        }
    }


    //Functionality for rewind button (uses list iterator to get previous element in songFiles array list)
    public void rewindSong() {
        ListIterator<File> songListIterator = songFiles.listIterator(songFiles.indexOf(this.currentSongFile));
        if (songListIterator.hasPrevious()) {
            try {
                this.mediaPlayer.stop();
                this.isPlaying = false;

                this.currentSongFile = songListIterator.previous();

                String mediaPath = this.currentSongFile.toURI().toASCIIString();
                this.currentMediaLoaded = new Media(mediaPath);
                this.mediaPlayer = new MediaPlayer(this.currentMediaLoaded);

                songListComboBox.getSelectionModel().select(this.currentSongFile.getName());
                this.componentsInit();
                this.timeInit();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Functionality for fast forward button (iterates through songList array list)
    public void fastForwardSong() {
        ListIterator<File> songListIterator = songFiles.listIterator(songFiles.indexOf(this.currentSongFile) + 1);
        if (songListIterator.hasNext()) {
            try {
                this.mediaPlayer.stop();
                this.isPlaying = false;

                this.currentSongFile = songListIterator.next();

                String mediaPath = this.currentSongFile.toURI().toASCIIString();
                this.currentMediaLoaded = new Media(mediaPath);
                this.mediaPlayer = new MediaPlayer(this.currentMediaLoaded);

                songListComboBox.getSelectionModel().select(this.currentSongFile.getName());
                this.componentsInit();
                this.timeInit();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //Setup song time slider and volume slider
    void componentsInit() {

        volume = 1.0;
        volumeLabel.setText(String.format("%02d %%", (int) (volume * 100)));
        volumeSlider.setValue(1.0);

        volumeSlider.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (volumeSlider.isPressed()) {
                    volume = volumeSlider.getValue();
                    Platform.runLater(() -> volumeLabel.setText(String.format("%01d %%", (int) (volume * 100))));
                    if (mediaPlayer != null) {
                        mediaPlayer.setVolume(volume);
                    }
                }
            }
        });

        songLengthSlider.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (songLengthSlider.isValueChanging()) {
                    if (duration != null) {
                        mediaPlayer.seek(duration.multiply(songLengthSlider.getValue() / 100.0));
                    }
                }
                updateValues();
            }
        }
        );

        this.mediaPlayer.currentTimeProperty().addListener(new ChangeListener<Duration>() {
            @Override
            public void changed(ObservableValue<? extends Duration> observableValue, Duration duration, Duration t1) {
                updateValues();
            }
        });
    }

    //Setup time properties from media player
    void timeInit() {
        this.mediaPlayer.currentTimeProperty().addListener((Observable ov) -> {
            updateValues();
        });

        this.mediaPlayer.setOnReady(() -> {
            duration = mediaPlayer.getMedia().getDuration();
            mediaPlayer.setAudioSpectrumListener(new spectrumListener());
            mediaPlayer.setAudioSpectrumNumBands(BANDS);
            updateValues();
        });
    }

    //Method which updates values shown on the application which change
    protected void updateValues() {
        if (songCurrentTime != null && duration != null && songLengthSlider != null) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    Duration currentTime = mediaPlayer.getCurrentTime();
                    songCurrentTime.setText(formatTime(currentTime, duration));
                    songLengthSlider.setDisable(duration.isUnknown());
                    if (!songLengthSlider.isDisabled() && duration.greaterThan(Duration.ZERO) && !songLengthSlider.isValueChanging()) {
                        songLengthSlider.setValue((currentTime.toMillis() / duration.toMillis()) * 100.0);
                    }
                }
            });
        }
    }

    //Method which returns a formatted time string depending on the duration elapsed and duration of the media
    private static String formatTime(Duration elapsed, Duration duration) {
        int intElapsed = (int) floor(elapsed.toSeconds());
        int elapsedHours = intElapsed / (60 * 60);
        if (elapsedHours > 0) {
            intElapsed -= elapsedHours * 60 * 60;
        }
        int elapsedMinutes = intElapsed / 60;
        int elapsedSeconds = intElapsed - elapsedHours * 60 * 60
                - elapsedMinutes * 60;

        if (duration.greaterThan(Duration.ZERO)) {
            int intDuration = (int) floor(duration.toSeconds());
            int durationHours = intDuration / (60 * 60);
            if (durationHours > 0) {
                intDuration -= durationHours * 60 * 60;
            }
            int durationMinutes = intDuration / 60;
            int durationSeconds = intDuration - durationHours * 60 * 60
                    - durationMinutes * 60;
            if (durationHours > 0) {
                return format("%d:%02d:%02d/%d:%02d:%02d",
                        elapsedHours, elapsedMinutes, elapsedSeconds,
                        durationHours, durationMinutes, durationSeconds);
            } else {
                return format("%02d:%02d/%02d:%02d",
                        elapsedMinutes, elapsedSeconds, durationMinutes,
                        durationSeconds);
            }
        } else {
            if (elapsedHours > 0) {
                return format("%d:%02d:%02d", elapsedHours,
                        elapsedMinutes, elapsedSeconds);
            } else {
                return format("%02d:%02d", elapsedMinutes,
                        elapsedSeconds);
            }
        }
    }



}
