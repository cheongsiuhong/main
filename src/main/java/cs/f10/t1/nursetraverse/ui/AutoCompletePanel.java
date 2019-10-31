package cs.f10.t1.nursetraverse.ui;

import java.util.LinkedList;

import cs.f10.t1.nursetraverse.autocomplete.AutoCompleteListHandler;
import cs.f10.t1.nursetraverse.autocomplete.AutoCompleteWord;
import cs.f10.t1.nursetraverse.autocomplete.AutoCompleteWordStorage;
import cs.f10.t1.nursetraverse.autocomplete.CommandWord;
import cs.f10.t1.nursetraverse.autocomplete.ObjectWord;
import cs.f10.t1.nursetraverse.autocomplete.UserinputParserUtil;
import cs.f10.t1.nursetraverse.model.appointment.Appointment;
import cs.f10.t1.nursetraverse.model.patient.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;

/**
 * Panel containing the list of suggested words cards.
 */
public class AutoCompletePanel extends UiPart<Region> {
    private static final String FXML = "AutoCompletePanel.fxml";

    private int selectedIndex = 0;

    private LinkedList<AutoCompleteWord> matchedAutoCompleteWords = new LinkedList<>();

    private AutoCompleteListHandler autoCompleteListHandler;
    private AutoCompleteWordStorage autoCompleteWordStorage;

    @FXML
    private ListView<AutoCompleteWord> autoCompleteWordListView;

    public AutoCompletePanel(FilteredList<Patient> patList, FilteredList<Appointment> apptList) {
        super(FXML);

        autoCompleteWordStorage = new AutoCompleteWordStorage(patList, apptList);
        autoCompleteListHandler = new AutoCompleteListHandler();
        autoCompleteWordListView.setItems(autoCompleteWordStorage.getOListAllObjectWord());
        autoCompleteWordListView.setCellFactory(listView -> new AutoCompleteListViewCell());
    }

    /**
     * Custom {@code ListCell} that displays the graphics of a {@code AutoCompleteWord} using a {@code AutoCompleteCard}
     */
    class AutoCompleteListViewCell extends ListCell<AutoCompleteWord> {
        @Override
        protected void updateItem(AutoCompleteWord autoCompleteWord, boolean empty) {
            super.updateItem(autoCompleteWord, empty);
            if (empty || autoCompleteWord == null) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new AutoCompleteCard(autoCompleteWord.getSuggestedWord()).getRoot());
            }
        }
    }

    public AutoCompleteWord getSelected() {
        return autoCompleteWordListView.getSelectionModel().getSelectedItem();
    }

    public void setSelected(int index) {
        if (index > getTotalItems() - 1) {
            index = getTotalItems() - 1;
        } else if (index < 0) {
            index = 0;
        }
        this.selectedIndex = index;
        autoCompleteWordListView.getSelectionModel().select(selectedIndex);
        autoCompleteWordListView.scrollTo(selectedIndex);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public int getTotalItems() {
        return autoCompleteWordListView.getItems().size();
    }

    /**
     * Change current list if needed and update suggested words in current list
     *
     * @param currentPhraseInCommandBox current String in command
     */
    public void updateListView(String currentPhraseInCommandBox) {
        String[] segments = UserinputParserUtil.splitIntoSegments(currentPhraseInCommandBox);
        LinkedList<String> firstSegmentParts = UserinputParserUtil.parseFirstSegment(segments[0]);

        // Check and update matched words
        updateMatchedWords(segments, firstSegmentParts);
        // Choose initial list
        ObservableList<AutoCompleteWord> chosenList = chooseList();
        // Filter list based on previous matched words
        ObservableList<AutoCompleteWord> filteredList = autoCompleteListHandler
                .filterList(matchedAutoCompleteWords, chosenList);
        // Update list based on userinput
        ObservableList<AutoCompleteWord> updatedList = autoCompleteListHandler
                .updateList(filteredList, matchedAutoCompleteWords.size(), segments, firstSegmentParts);

        autoCompleteWordListView.setItems(updatedList);
        autoCompleteWordListView.setCellFactory(listView -> new AutoCompleteListViewCell());
    }

    /**
     * Choose initial list to be suggested
     */
    public ObservableList<AutoCompleteWord> chooseList() {
        ObservableList<AutoCompleteWord> currentList;
        if (matchedAutoCompleteWords.size() == 0) {
            // Set to object list
            currentList = autoCompleteWordStorage.getOListAllObjectWord();
        } else if (matchedAutoCompleteWords.size() == 1) {
            // Set to command list
            currentList = autoCompleteWordStorage.getOListAllCommandWord();
        } else if (matchedAutoCompleteWords.size() == 2) {
            // Set to prefix/index/empty list
            if (((CommandWord) matchedAutoCompleteWords.get(1)).hasIndex()) {
                currentList = autoCompleteWordStorage
                        .generateOListAllIndexWord((ObjectWord) matchedAutoCompleteWords.get(0));
            } else if (((CommandWord) matchedAutoCompleteWords.get(1)).hasPrefix()) {
                currentList = autoCompleteWordStorage.getOListAllPrefixWord();
            } else {
                currentList = FXCollections.observableArrayList();
            }
        } else {
            // Set to prefix/empty list
            if (((CommandWord) matchedAutoCompleteWords.get(1)).hasPrefix()) {
                currentList = autoCompleteWordStorage.getOListAllPrefixWord();
            } else {
                currentList = FXCollections.observableArrayList();
            }
        }
        return currentList;
    }

    /**
     * Update matchedAutoCompleteWords list given userinput
     *
     * @param segments segments of the userinputs
     * @param firstSegmentParts parts of the first segment of userinput
     */
    public void updateMatchedWords(String[] segments, LinkedList<String> firstSegmentParts) {
        // segments[0] -> object-command format
        // segments[1] -> either prefix or index
        // segments[2] onwards -> prefix
        matchedAutoCompleteWords.clear();

        boolean isCorrectFirstSegment = matchFirstSegment(firstSegmentParts);

        if (segments.length >= 2) {
            boolean isCorrectSecondSegment = false;
            if (isCorrectFirstSegment) {
                isCorrectSecondSegment = matchSecondSegment(segments[1]);
            }
            if (segments.length >= 3 && isCorrectFirstSegment && isCorrectSecondSegment) {
                matchRestOfSegment(segments);
            }
        }
    }

    /**
     * Check if first segment of userinput matches with current matched words
     *
     * @param firstSegmentParts parts of the first segment of userinput
     * @return true if first segment parts matches with current matched words
     */
    public boolean matchFirstSegment(LinkedList<String> firstSegmentParts) {
        boolean isCorrectObjectWord = false;
        boolean isCorrectCommandWord = false;

        if (firstSegmentParts.size() >= 1) {
            // First object word
            for (AutoCompleteWord autoCompleteWord : autoCompleteWordStorage.getOListAllObjectWord()) {
                if (firstSegmentParts.get(0).matches(autoCompleteWord.getSuggestedWord())) {
                    matchedAutoCompleteWords.add(autoCompleteWord);
                    isCorrectObjectWord = true;
                    break;
                }
            }
            // Second command word
            if (firstSegmentParts.size() == 2 && isCorrectObjectWord) {
                for (AutoCompleteWord autoCompleteWord : autoCompleteWordStorage.getOListAllCommandWord()) {
                    if (firstSegmentParts.get(1).matches(autoCompleteWord.getSuggestedWord())) {
                        matchedAutoCompleteWords.add(autoCompleteWord);
                        isCorrectCommandWord = true;
                        break;
                    }
                }
            }
        }
        return isCorrectObjectWord && isCorrectCommandWord;
    }

    /**
     * Check if second segment of userinput matches with current matched words
     *
     * @param secondSegment second segment of userinput
     * @return true if second segment matches with current matched words
     */
    public boolean matchSecondSegment(String secondSegment) {
        boolean isCorrect = false;

        if (((CommandWord) matchedAutoCompleteWords.get(1)).hasIndex()) {
            for (AutoCompleteWord autoCompleteWord : autoCompleteWordStorage
                    .generateOListAllIndexWord((ObjectWord) matchedAutoCompleteWords.get(0))) {
                if (secondSegment.equals(autoCompleteWord.getSuggestedWord())) {
                    matchedAutoCompleteWords.add(autoCompleteWord);
                    isCorrect = true;
                    break;
                }
            }
        } else if (((CommandWord) matchedAutoCompleteWords.get(1)).hasPrefix()) {
            for (AutoCompleteWord autoCompleteWord : autoCompleteListHandler
                    .filterList(matchedAutoCompleteWords, autoCompleteWordStorage.getOListAllPrefixWord())) {
                if (secondSegment.equals(autoCompleteWord.getSuggestedWord())) {
                    matchedAutoCompleteWords.add(autoCompleteWord);
                    isCorrect = true;
                    break;
                }
            }
        }
        return isCorrect;
    }

    /**
     * Check if all other segment of userinput matches with current matched words
     *
     * @param segments Array of segments to be checked
     */
    public void matchRestOfSegment(String[] segments) {
        for (int i = 3; i <= segments.length; i++) {
            if (matchedAutoCompleteWords.size() == i) {
                for (AutoCompleteWord autoCompleteWord : autoCompleteListHandler
                        .filterList(matchedAutoCompleteWords, autoCompleteWordStorage.getOListAllPrefixWord())) {
                    if (segments[i - 1].equals(autoCompleteWord.getSuggestedWord())) {
                        matchedAutoCompleteWords.add(autoCompleteWord);
                    }
                }
            }
        }
    }

    /**
     * @return string representation of all the matched words
     */
    public String getCombinedMatchedWords() {
        StringBuilder combinedMatchedWords = new StringBuilder();
        for (AutoCompleteWord autoCompleteWord : matchedAutoCompleteWords) {
            combinedMatchedWords
                    .append(autoCompleteWord.getSuggestedWord())
                    .append(autoCompleteWord.getConnectorChar());
        }
        combinedMatchedWords.append(getSelected().getSuggestedWord());
        return combinedMatchedWords.toString();
    }


}
