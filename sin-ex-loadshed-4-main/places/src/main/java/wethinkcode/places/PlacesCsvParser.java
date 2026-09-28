package wethinkcode.places;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashSet;
import java.util.Set;

import com.google.common.annotations.VisibleForTesting;
import wethinkcode.places.db.memory.PlacesDb;
import wethinkcode.places.model.Places;
import wethinkcode.places.model.Town;

/**
 * PlacesCsvParser : I parse a CSV file with each line containing the fields (in
 * order):
 * <code>Name, Feature_Description, pklid, Latitude, Longitude, Date, MapInfo, Province,
 * fklFeatureSubTypeID, Previous_Name, fklMagisterialDistrictID, ProvinceID, fklLanguageID,
 * fklDisteral, Local Municipality, Sound, District Municipality, fklLocalMunic, Comments, Meaning</code>.
 * <p>
 * For the PlaceNameService we're only really interested in the
 * <code>Name</code>,
 * <code>Feature_Description</code> and <code>Province</code> fields.
 * <code>Feature_Description</code> allows us to distinguish towns and urban
 * areas from
 * (e.g.) rivers, mountains, etc. since our PlaceNameService is only concerned
 * with occupied places.
 */
public class PlacesCsvParser {
    public static final Set<String> WANTED_FEATURES = Set.of(
            "Power station",
            "Town",
            "Urban Area",
            "Industrial",
            "Hospital",
            "Airport",
            "College",
            "School",
            "Railway Station",
            "Hotel",
            "Museum",
            "Observatory",
            "Police_Station",
            "Post Office",
            "Prison",
            "Research Centre",
            "Research Institute",
            "Residential Town",
            "Residential Township",
            "Township",
            "Village");

    public static final int MIN_COLUMNS = 19;
    public static final int FEATURE_COLUMN = 1;
    public static final int NAME_COLUMN = 0;
    public static final int PROVINCE_COLUMN = 7;

    public Places parseCsvSource(File csvFile) throws FileNotFoundException, IOException {
        try (LineNumberReader reader = new LineNumberReader(new FileReader(csvFile))) {
            return parseDataLines(reader);
        }
    }

    @VisibleForTesting
    public Places parseCsvSource(LineNumberReader reader) throws IOException {
        return parseDataLines(reader);
    }

    @VisibleForTesting
    Places parseDataLines(final LineNumberReader in) {
        Set<Town> towns = new HashSet<>();

        try {
            String line = in.readLine();

            while ((line = in.readLine()) != null) {
                String[] fields = splitLineIntoValues(line);

                if (fields.length >= MIN_COLUMNS && isLineAWantedFeature(fields)) {
                    towns.add(createTown(fields));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error parsing CSV data", e);
        }

        return new PlacesDb(towns);
    }

    @VisibleForTesting
    public Town createTown(String[] fields) {
        return new Town(fields[NAME_COLUMN].trim(), fields[PROVINCE_COLUMN].trim());
    }

    @VisibleForTesting
    public String[] splitLineIntoValues(String line) {
        return splitCsvLine(line);
    }

    @VisibleForTesting
    public String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    @VisibleForTesting
    public boolean isWantedFeature(String featureDescription) {
        return WANTED_FEATURES.contains(featureDescription);
    }

    @VisibleForTesting
    public boolean isLineAWantedFeature(String[] fields) {
        if (fields.length > FEATURE_COLUMN) {
            return isWantedFeature(fields[FEATURE_COLUMN].trim());
        }
        return false;
    }

}
