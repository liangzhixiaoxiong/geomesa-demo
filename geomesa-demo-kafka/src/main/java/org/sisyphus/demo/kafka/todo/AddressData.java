package org.sisyphus.demo.kafka.todo;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.geomesa.utils.interop.SimpleFeatureTypes;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AddressData {

    private SimpleFeatureType sft = null;
    private List<SimpleFeature> features = null;
    private List<Query> queries = null;
    private Filter subsetFilter = null;

    public String getTypeName() {
        return "address";
    }


    public SimpleFeatureType getSimpleFeatureType() {
        if (sft == null) {
            // list the attributes that constitute the feature type
            // this is a reduced set of the attributes from GDELT 2.0
            StringBuilder attributes = new StringBuilder();
            attributes.append("addressId:String,");
            attributes.append("name:String,");
            attributes.append("*geom:Point:srid=4326"); // the "*" denotes the default geometry (used for indexing)

            // create the simple-feature type - use the GeoMesa 'SimpleFeatureTypes' class for best compatibility
            // may also use geotools DataUtilities or SimpleFeatureTypeBuilder, but some features may not work
            sft = SimpleFeatureTypes.createType(getTypeName(), attributes.toString());

            // use the user-data (hints) to specify which date field to use for primary indexing
            // if not specified, the first date attribute (if any) will be used
            // could also use ':default=true' in the attribute specification string

//            sft.getUserData().put(SimpleFeatureTypes.DEFAULT_DATE_KEY, "dtg");

        }
        return sft;
    }


    public List<SimpleFeature> getChinaPoiData() throws IOException {
        if (features == null) {
            List<SimpleFeature> features = new ArrayList<>();

            // read the bundled chinapoi CSV
            URL input = getClass().getClassLoader().getResource("chinapoi.txt");

//            List<String> input = ReadFile.readLine("D:\\data\\chinapoi.txt")

            if (input == null) {
                throw new RuntimeException("Couldn't load resource chinapoi.txt");
            }

            // date parser corresponding to the CSV format
            DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);

            // use a geotools SimpleFeatureBuilder to create our features
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(getSimpleFeatureType());

            try (CSVParser parser = CSVParser.parse(input, StandardCharsets.UTF_8, CSVFormat.DEFAULT)) {
                for (CSVRecord record : parser) {
                    // use apache commons-csv to parse the t-drive file
                    // pull out the fields corresponding to our simple feature attributes
                    builder.set("addressId", record.get(0));
                    builder.set("name", record.get(1));
                    // some dates are converted implicitly, so we can set them as strings
                    // however, the date format here isn't one that is converted, so we parse it into a java.util.Date
//                    builder.set("dtg", Date.from(LocalDateTime.parse(record.get(1), dateFormat).toInstant(ZoneOffset.UTC)));

                    // we can use WKT (well-known-text) to represent geometries
                    // note that we use longitude first ordering
                    double longitude = Double.parseDouble(record.get(2));
                    double latitude = Double.parseDouble(record.get(3));
                    builder.set("geom", "POINT (" + longitude + " " + latitude + ")");

                    // be sure to tell GeoTools explicitly that we want to use the ID we provided
                    builder.featureUserData(Hints.USE_PROVIDED_FID, Boolean.TRUE);

                    // build the feature - this also resets the feature builder for the next entry
                    // use the taxi ID as the feature ID
                    features.add(builder.buildFeature(record.get(0)));
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading chinapoi data:", e);
            }
            this.features = Collections.unmodifiableList(features);
        }
        return features;
    }

    public List<Query> getTestQueries() {
        if (queries == null) {
            List<Query> queries = new ArrayList<>();
            // this data set is meant to show streaming updates over time, so just return all features
            queries.add(new Query(getTypeName(), Filter.INCLUDE));
            this.queries = Collections.unmodifiableList(queries);
        }
        return queries;
    }

    public Filter getSubsetFilter() {
        return Filter.INCLUDE;
    }
}