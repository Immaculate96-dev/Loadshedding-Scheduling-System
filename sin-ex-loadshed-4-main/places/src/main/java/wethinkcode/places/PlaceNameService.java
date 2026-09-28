package wethinkcode.places;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import com.google.common.annotations.VisibleForTesting;
import io.javalin.Javalin;
import io.javalin.http.Context;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import wethinkcode.places.model.Places;
import wethinkcode.places.model.Town;

/**
 * I provide a Place-names Service for places in South Africa.
 * <p>
 * I read place-name data from a CSV file that I read and
 * parse into the objects (domain model) that I use,
 * discarding unwanted data in the file (things like mountain/river names). With my "database"
 * built, I then serve-up place-name data as JSON to clients.
 * <p>
 * Clients can request:
 * <ul>
 * <li>a list of available Provinces
 * <li>a list of all Towns/PlaceNameService in a given Province
 * <li>a list of all neighbourhoods in a given Town
 * </ul>
 * I understand the following command-line arguments:
 * <dl>
 * <dt>-c | --config &lt;configfile&gt;
 * <dd>a file pathname referring to an (existing!) configuration file in standard Java
 *      properties-file format
 * <dt>-d | --datadir &lt;datadirectory&gt;
 * <dd>the name of a directory where CSV datafiles may be found. This option <em>overrides</em>
 *      and data-directory setting in a configuration file.
 * <dt>-p | --places &lt;csvdatafile&gt;
 * <dd>a file pathname referring to a CSV file of place-name data. This option
 *      <em>overrides</em> any value in a configuration file and will bypass any
 *      data-directory set via command-line or configuration.
 */
@Command( name = "PlaceNameService", mixinStandardHelpOptions = true, version = "1.0" )
public class PlaceNameService implements Runnable {

    public static final int DEFAULT_PORT = 7000;

    // Configuration keys
    public static final String CFG_CONFIG_FILE = "config.file";
    public static final String CFG_DATA_DIR = "data.dir";
    public static final String CFG_DATA_FILE = "data.file";
    public static final String CFG_SERVICE_PORT = "server.port";


    @Option(names = { "-f", "--file" }, description = "CSV file containing places data")
    private String csvFile;

    @Option(names = { "-p", "--port" }, description = "Port to run the server on", defaultValue = "7000")
    private int port = DEFAULT_PORT;

    @Option(names = { "-c", "--config" }, description = "Configuration file")
    private String configFile;

    @Option(names = { "-d", "--datadir" }, description = "Data directory")
    private String dataDir;




    public static void main(String[] args) {
        final PlaceNameService svc = new PlaceNameService();
        final CommandLine cmd = new CommandLine(svc);
        cmd.execute(args);
    }

    // Instance state

    //private final Properties config;

    private Javalin server;

    private Places places;

    // FIXME: Command-line options. I don't like that these are in the PlaceNameService
    // where they might easily get (mis)used instead of the access methods
    // (configFile(), dataFile() and dataDir()) down below. BUT: can the `picocli`
    // library deal with them properly if they're in an inner/nested class (or something)?
    // I haven't the time to discover this right now.

    public PlaceNameService(){
        //config = initConfig();
    }
    public PlaceNameService(int port) {
        this.port = port;
    }

    public void start() {
        if (server != null) {
            server.start(port);
        }
    }

    public void start(int port) {
        this.port = port;
        start();
    }


    public void stop(){
        if (server != null) {
            server.stop();
        }
    }

    /**
     * Why not put all of this into the constructor? Well, this way makes
     * it easier (possible!) to test an instance of PlaceNameService without
     * starting up all the big machinery (i.e. without calling initialise()).
     */
    @VisibleForTesting
    PlaceNameService initialise(){
        places = initPlacesDb();
        server = initHttpServer();
        return this;
    }

    /**
     * Sometimes we want to initialise with test data...
     */
    @VisibleForTesting
    PlaceNameService initialise( Places aPlaceDb ){
        places = aPlaceDb;
        server = initHttpServer();
        return this;
    }

    @Override
    public void run(){
        initialise();
        start();
        System.out.println("PlaceNameService started on port " + port);
    }

    /**
     * Initialise the service configuration from either a config file specified on
     * the command-line or a default configuration-file
     * ({@code $WORKING_DIRECTORY/places.properties}),
     * then override those if specific config values have been given on the command-line.
     *
     * @return a non-null Properties instance
     */
//    private Properties initConfig(){
//        try( FileReader in = new FileReader( configFile() )){
//            final Properties p = new Properties( defaultConfig() );
//            p.load( in );
//            return p;
//        }catch( IOException ex ){
//
//            // We can recover from this, but (maybe later) we really
//            // ought to notify somebody that there's a problem.
//
//            return defaultConfig();
//        }
//    }

    private Places initPlacesDb(){
        File dataFile = findDataFile();
        if (dataFile != null && dataFile.exists()) {

            try {
                PlacesCsvParser parser = new PlacesCsvParser();
                Places result = parser.parseCsvSource(dataFile);
                System.out.println("Loaded " + result.size() + " places from " + dataFile.getPath());
                return result;

            } catch (IOException e) {
                System.err.println("Failed to load places data from " + dataFile.getPath() + ": " + e.getMessage());
                File fallbackFile = findFallbackDataFile(dataFile);
                if (fallbackFile != null) {

                    try {
                        PlacesCsvParser parser = new PlacesCsvParser();
                        Places result = parser.parseCsvSource(fallbackFile);
                        System.out.println(
                                "Loaded " + result.size() + " places from fallback file " + fallbackFile.getPath());
                        return result;

                    } catch (IOException fallbackException) {
                        System.err.println("Fallback file also failed: " + fallbackException.getMessage());
                    }
                }

                throw new RuntimeException("Failed to load places data from " + dataFile.getPath(), e);
            }
        } else {
            System.err.println("No data file found. Server will start with empty database.");
            return new wethinkcode.places.db.memory.PlacesDb(java.util.Set.of());
        }
    }

    private File findDataFile() {
        if (csvFile != null) {
            return new File(csvFile);
        }

        String dirPath = dataDir != null ? dataDir : "resources";
        File dir = new File(dirPath);

        if (dir.exists() && dir.isDirectory()) {
            File dataFile = new File(dir, "PlaceNamesZA2008.csv");

            if (dataFile.exists())
                return dataFile;
            dataFile = new File(dir, "PlaceNamesZAexsagns2008.csv");

            if (dataFile.exists())
                return dataFile;
        }

        String[] defaultPaths = {
                "resources/PlaceNamesZA2008.csv",
                "resources/PlaceNamesZAexsagns2008.csv",
                "places/resources/PlaceNamesZA2008.csv",
                "places/resources/PlaceNamesZAexsagns2008.csv"
        };

        for (String path : defaultPaths) {
            File file = new File(path);
            if (file.exists())
                return file;
        }

        return null;
    }

    private File findFallbackDataFile(File originalFile) {
        File parentDir = originalFile.getParentFile();
        if (parentDir == null || !parentDir.exists())
            return null;

        File[] csvFiles = parentDir
                .listFiles((dir, name) -> name.toLowerCase().endsWith(".csv") && !name.equals(originalFile.getName()));

        if (csvFiles != null && csvFiles.length > 0) {
            File newest = csvFiles[0];

            for (File file : csvFiles) {

                if (file.lastModified() > newest.lastModified()) {
                    newest = file;
                }
            }
            return newest;
        }

        return null;
    }

    private Places previousPlaces;

    private Javalin initHttpServer() {
        Javalin app = Javalin.create();

        app.get("/provinces", this::getProvinces);
        app.get("/towns/{province}", this::getTownsInProvince);

        app.post("/admin/reload", this::reloadData);
        app.post("/admin/rollback", this::rollbackData);

        return app;
    }

    private void getProvinces(Context ctx) {
        Collection<String> provinces = places.provinces();
        ctx.json(provinces);
    }

    private void getTownsInProvince(Context ctx) {
        String province = ctx.pathParam("province");
        Collection<Town> towns = places.townsIn(province);
        ctx.json(towns);
    }

    private void reloadData(Context ctx) {
        try {
            previousPlaces = places;
            places = initPlacesDb();

            ctx.json(java.util.Map.of(
                    "status", "success",
                    "message", "Data reloaded successfully",
                    "places_count", places.size()));

        } catch (Exception e) {

            if (previousPlaces != null) {
                places = previousPlaces;
            }

            ctx.status(500).json(java.util.Map.of(
                    "status", "error",
                    "message", "Failed to reload data: " + e.getMessage()));
        }
    }

    private void rollbackData(Context ctx) {
        if (previousPlaces != null) {
            places = previousPlaces;
            previousPlaces = null;

            ctx.json(java.util.Map.of(
                    "status", "success",
                    "message", "Data rolled back successfully",
                    "places_count", places.size()));
        } else {
            ctx.status(400).json(java.util.Map.of(
                    "status", "error",
                    "message", "No previous data available for rollback"));
        }
    }

    @VisibleForTesting
    public String getConfig(String key) {
        return switch (key) {
            case CFG_CONFIG_FILE -> getConfigFilePath();
            case CFG_DATA_DIR -> dataDir != null ? dataDir : System.getProperty("user.dir");
            case CFG_DATA_FILE -> csvFile != null ? csvFile : System.getProperty("user.dir") + "/places.csv";
            case CFG_SERVICE_PORT -> String.valueOf(port);
            default -> null;
        };
    }

    private String getConfigFilePath() {
        if (configFile != null) {
            File file = new File(configFile);
            if (file.exists()) {
                return configFile;
            }
        }
        return getDefaultConfigPath();
    }

    private String getDefaultConfigPath() {
        return System.getProperty("user.dir") + "/places.properties";
    }

    @VisibleForTesting
    public File configFile() {
        return configFile != null ? new File(configFile) : null;
    }

    @VisibleForTesting
    public File dataFile() {
        return csvFile != null ? new File(csvFile) : new File(System.getProperty("user.dir") + "/places.csv");
    }

    @VisibleForTesting
    public File dataDir() {
        return dataDir != null ? new File(dataDir) : new File(System.getProperty("user.dir"));
    }

    @VisibleForTesting
    public Places getDb() {
        return places;
    }

}