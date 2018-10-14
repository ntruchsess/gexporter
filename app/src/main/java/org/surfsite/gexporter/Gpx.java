package org.surfsite.gexporter;

import android.util.Xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Gpx {
    private static final Logger Log = LoggerFactory.getLogger(Gpx.class);

    private static final String HTTP_WWW_TOPOGRAFIX_COM_GPX_1_0 = "http://www.topografix.com/GPX/1/0";
    private static final String HTTP_WWW_TOPOGRAFIX_COM_GPX_1_1 = "http://www.topografix.com/GPX/1/1";
    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String GPXX = "http://www.garmin.com/xmlschemas/GpxExtensions/v3";
    private static final String WPTX = "http://www.garmin.com/xmlschemas/WaypointExtension/v1";
    private static final String TPTX = "http://www.garmin.com/xmlschemas/TrackPointExtension/v1";

    private final String courseName;
    private Metadata metadata = null;
    private final List<Track> tracks = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<WptPoint> wayPoints = new ArrayList<>();
    private String ns = HTTP_WWW_TOPOGRAFIX_COM_GPX_1_1;
    private Options options;

    public static class Options {

        boolean useExtensions;
        boolean transformRte2Wpts;
        double minProximity;
        double maxProximity;
        double proximityRatio;
        boolean indent;

        public boolean isUseExtensions() {
            return useExtensions;
        }

        public void setUseExtensions(boolean useExtensions) {
            this.useExtensions = useExtensions;
        }

        public boolean isTransformRte2Wpts() {
            return transformRte2Wpts;
        }

        public void setTransformRte2Wpts(boolean transformRte2Wpts) {
            this.transformRte2Wpts = transformRte2Wpts;
        }

        public double getMinProximity() {
            return minProximity;
        }

        public void setMinProximity(double minProximity) {
            this.minProximity = minProximity;
        }

        public double getMaxProximity() {
            return maxProximity;
        }

        public void setMaxProximity(double maxProximity) {
            this.maxProximity = maxProximity;
        }

        public double getProximityRatio() {
            return proximityRatio;
        }

        public void setProximityRatio(double proximityRatio) {
            this.proximityRatio = proximityRatio;
        }

        public boolean isIndent() {
            return indent;
        }

        public void setIndent(boolean indent) {
            this.indent = indent;
        }
    }

    public static class Metadata {

        private String name;
        private Author author;
        private final List<Link> links = new ArrayList<>();
        private Date time;

        public static class Author {
            private String name;
            private final List<Link> links = new ArrayList<>();

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public List<Link> getLinks() {
                return links;
            }

            public void addLink(Link link) {
                if (link != null && link.isValid()) {
                    links.add(link);
                }
            }

            public boolean isValid() {
                return (name != null && !name.isEmpty())
                        || (!links.isEmpty());
            }
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public Author getAuthor() {
            return author;
        }

        public void setAuthor(Author author) {
            this.author = author;
        }

        public List<Link> getLinks() {
            return links;
        }

        public void addLink(Link link) {
            if(link != null && link.isValid()) {
                links.add(link);
            }
        }

        public Date getTime() {
            return time;
        }

        public void setTime(Date time) {
            this.time = time;
        }

        public boolean isValid() {
            for (Link link : links) {
                if (link.isValid()) {
                    return true;
                }
            }
            return (name != null && !name.isEmpty())
                    || (author != null && author.isValid())
                    || time != null;
        }
    }

    public static class Link {
        private String href;
        private String text;
        private String type;

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isValid() {
            return (href != null && !href.isEmpty())
                    || (text != null && !text.isEmpty())
                    || (type != null && !type.isEmpty());
        }
    }

    public static class ExtWayPoint extends WayPoint {

        private String name = null;
        private String desc = null;
        private String sym = null;
        private String type = null;

        public ExtWayPoint() {
            super(Double.NaN, Double.NaN, Double.NaN, null );
        }

        public String getName() {
            return name;
        }

        public String getDesc() {
            return desc;
        }

        public String getSym() {
            return sym;
        }

        public String getType() {
            return type;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public void setSym(String sym) {
            this.sym = sym;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isValid() {
            return (!Double.isNaN(getLat()) && !Double.isNaN(getLon()));
        }
    }

    public static class RtePoint extends ExtWayPoint {

        private OsmAndExtensions extensions = null;

        public OsmAndExtensions getExtensions() {
            return extensions;
        }

        public void setExtensions(OsmAndExtensions extensions) {
            this.extensions = extensions;
        }
    }

    public static class WptPoint extends ExtWayPoint {

        private GarminExtensions extensions = null;

        public GarminExtensions getExtensions() {
            return extensions;
        }

        public void setExtensions(GarminExtensions extensions) {
            this.extensions = extensions;
        }
    }

    public static class TrkSeg {
        private List<WayPoint> trkPoints = new ArrayList<>();

        public List<WayPoint> getTrkPoints() {
            return trkPoints;
        }

        public void addTrkPoint(WayPoint trkPoint) {
            if (trkPoint != null && !Double.isNaN(trkPoint.getLat()) && !Double.isNaN(trkPoint.getLon())) {
                trkPoints.add(trkPoint);
            }
        }
        public boolean isValid() {
            return !trkPoints.isEmpty();
        }
    }

    public static class Track {

        private String name;
        private String desc;
        private final List<Link> links = new ArrayList<>();
        private final List<TrkSeg> segments = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public List<Link> getLinks() {
            return links;
        }

        public void addLink(Link link) {
            if (link != null && link.isValid()) {
                links.add(link);
            }
        }

        public List<TrkSeg> getSegments() {
            return segments;
        }

        public void addTrkSeg(TrkSeg trkSeg) {
            if (trkSeg != null && trkSeg.isValid()) {
                segments.add(trkSeg);
            }
        }

        public boolean isValid() {
            return !segments.isEmpty();
        }
    }

    public static class Route {

        private String name;
        private String desc;
        private final List<Link> links = new ArrayList<>();
        private final List<RtePoint> rtePoints = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public List<Link> getLinks() {
            return links;
        }

        public void addLink(Link link) {
            if (link != null && link.isValid()) {
                links.add(link);
            }
        }

        public List<RtePoint> getRtePoints() {
            return rtePoints;
        }

        public void addRtePoint(RtePoint rtePoint) {
            if (rtePoint != null && rtePoint.isValid()) {
                rtePoints.add(rtePoint);
            }
        }

        public boolean isValid() {
            return !rtePoints.isEmpty();
        }
    }

    public static class OsmAndExtensions {

        private Integer time = Integer.MIN_VALUE;
        private Integer offset = Integer.MIN_VALUE;
        private String turn = null;
        private Double turnAngle = Double.NaN;

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public String getTurn() {
            return turn;
        }

        public void setTurn(String turn) {
            this.turn = turn;
        }

        public double getTurnAngle() {
            return turnAngle;
        }

        public void setTurnAngle(double turnAngle) {
            this.turnAngle = turnAngle;
        }

        public boolean isValid() {
            return time != Integer.MIN_VALUE
                    || offset != Integer.MIN_VALUE
                    || (turn != null && !turn.isEmpty())
                    || !turnAngle.isNaN();
        }
    }

    public static class GarminExtensions {

        private Double proximity = Double.NaN;

        public Double getProximity() {
            return proximity;
        }

        public void setProximity(Double proximity) {
            this.proximity = proximity;
        }

        public boolean isValid() {
            return !proximity.isNaN();
        }
    }

    public Gpx(String name, InputStream in) throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        //parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        courseName = name;
        BufferedInputStream inputStream = new BufferedInputStream(in);
        inputStream.mark(64000);

        try {
            parser.setInput(inputStream, null);
            parser.nextTag();
            readGPX(parser);
            inputStream.close();
            return;
        } catch (Exception e) {
            ns = HTTP_WWW_TOPOGRAFIX_COM_GPX_1_0;
            Log.debug("Ex {}", e);
        }

        inputStream.reset();

        try {
            parser.setInput(inputStream, null);
            parser.nextTag();
            readGPX(parser);
        } finally {
            inputStream.close();
        }
    }

    public Options getOptions () {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public List<WptPoint> getWaypoints() {
        return wayPoints;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private void readGPX(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, "gpx");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            switch (name) {
                case "metadata":
                    metadata = readMeta(parser);
                    break;
                case "trk":
                    Track trk = readTrk(parser);
                    if (trk != null && trk.isValid()) {
                        tracks.add(trk);
                    }
                    break;
                case "rte":
                    Route rte = readRte(parser);
                    if (rte != null && rte.isValid()) {
                        routes.add(rte);
                    }
                    break;
                case "wpt":
                    WptPoint wpt = readWpt(parser);
                    if (wpt != null && wpt.isValid()) {
                        wayPoints.add(wpt);
                    }
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
    }

    private Metadata readMeta(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, "metadata");
        Metadata meta = new Metadata();
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            switch (name) {
                case "name":
                    meta.setName(readName(parser));
                    break;
                case "author":
                    meta.setAuthor(readAuthor(parser));
                    break;
                case "link":
                    meta.addLink(readLink(parser));
                    break;
                case "time":
                    meta.setTime(readTime(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return meta;
    }

    private Track readTrk(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        Track track = new Track();
        parser.require(XmlPullParser.START_TAG, ns, "trk");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            switch (name) {
                case "name":
                    track.setName(readName(parser));
                    break;
                case "desc":
                    track.setDesc(readDesc(parser));
                    break;
                case "link":
                    track.addLink(readLink(parser));
                    break;
                case "trkseg":
                    track.addTrkSeg(readTrkSeg(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return track;
    }

    private TrkSeg readTrkSeg(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        TrkSeg trkseg = new TrkSeg();
        parser.require(XmlPullParser.START_TAG, ns, "trkseg");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("trkpt")) {
                trkseg.addTrkPoint(readTrkPt(parser));
            } else {
                skip(parser);
            }
        }
        return trkseg;
    }

    private Route readRte(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        Route rte = new Route();
        parser.require(XmlPullParser.START_TAG, ns, "rte");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            switch (name) {
                case "name":
                    rte.setName(readName(parser));
                    break;
                case "desc":
                    rte.setDesc(readDesc(parser));
                    break;
                case "link":
                    rte.addLink(readLink(parser));
                    break;
                case "rtept":
                    rte.addRtePoint(readRtePt(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return rte;
    }

    private WptPoint readWpt(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns,"wpt");
        WptPoint wpt = new WptPoint();
        String lat = parser.getAttributeValue(null,"lat");
        if (lat != null) {
            try {
                wpt.setLat(Double.parseDouble(lat));
            } catch (NumberFormatException nfe) {
            }
        }
        String lon = parser.getAttributeValue(null,"lon");
        if (lon != null) {
            try {
                wpt.setLon(Double.parseDouble(lon));
            } catch (NumberFormatException nfe) {
            }
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "name":
                    wpt.setName(readName(parser));
                    break;
                case "desc":
                    wpt.setDesc(readDesc(parser));
                    break;
                case "ele":
                    wpt.setEle(readEle(parser));
                    break;
                case "time":
                    wpt.setTime(readTime(parser));
                    break;
                case "sym":
                    wpt.setSym(readSym(parser));
                    break;
                case "type":
                    wpt.setType(readType(parser));
                    break;
                case "extensions":
                    wpt.setExtensions(readGarminWptExtensions(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return wpt;
    }

    private WayPoint readTrkPt(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "trkpt");
        WayPoint trkPt = new WayPoint(Double.NaN, Double.NaN, Double.NaN,null);
        String lat = parser.getAttributeValue(null,"lat");
        if (lat != null) {
            try {
                trkPt.setLat(Double.parseDouble(lat));
            } catch (NumberFormatException nfe) {
            }
        }
        String lon = parser.getAttributeValue(null,"lon");
        if (lon != null) {
            try {
                trkPt.setLon(Double.parseDouble(lon));
            } catch (NumberFormatException nfe) {
            }
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "ele":
                    trkPt.setEle(readEle(parser));
                    break;
                case "time":
                    trkPt.setTime(readTime(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return trkPt;
    }

    private RtePoint readRtePt(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "rtept");
        RtePoint rtePt = new RtePoint();
        String lat = parser.getAttributeValue(null, "lat");
        if (lat != null) {
            try {
                rtePt.setLat(Double.parseDouble(lat));
            } catch (NumberFormatException nfe) {
            }
        }
        String lon = parser.getAttributeValue(null, "lon");
        if (lon != null) {
            try {
                rtePt.setLon(Double.parseDouble(lon));
            } catch (NumberFormatException nfe) {
            }
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "name":
                    rtePt.setName(readName(parser));
                    break;
                case "desc":
                    rtePt.setDesc(readDesc(parser));
                    break;
                case "ele":
                    rtePt.setEle(readEle(parser));
                    break;
                case "time":
                    rtePt.setTime(readTime(parser));
                    break;
                case "sym":
                    rtePt.setSym(readSym(parser));
                    break;
                case "type":
                    rtePt.setType(readType(parser));
                    break;
                case "extensions":
                    rtePt.setExtensions(readOsmAndRteExtensions(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return rtePt;
    }

    private Metadata.Author readAuthor(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, "author");

        Metadata.Author author = new Metadata.Author();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "name":
                    author.setName(readName(parser));
                    break;
                case "link":
                    author.addLink(readLink(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return author.isValid() ? author : null;
    }

    private Link readLink(XmlPullParser parser) throws XmlPullParserException, IOException, ParseException {
        parser.require(XmlPullParser.START_TAG, ns, "link");

        Link link = new Link();
        link.setHref(parser.getAttributeValue(null, "href"));

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "text":
                    link.setText(readTextElement(parser));
                    break;
                case "type":
                    link.setType(readType(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return link.isValid() ? link : null;
    }

    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "name");
        String txt = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "name");
        return txt;
    }

    private double readEle(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "ele");
        String txt = readText(parser);
        double ele = Double.parseDouble(txt);
        parser.require(XmlPullParser.END_TAG, ns, "ele");
        return ele;
    }

    private Date readTime(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "time");
        String txt = readText(parser);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Date time;
        try {
            time = dateFormat.parse(txt);
        } catch (ParseException e) {
            time = null;
        } finally {
            parser.require(XmlPullParser.END_TAG, ns, "time");
        }
        return time;
    }

    private String readSym(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "sym");
        String txt = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "sym");
        return txt;
    }

    private String readType(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "type");
        String txt = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "type");
        return txt;
    }

    private String readDesc(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "desc");
        String txt = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "desc");
        return txt;
    }

    private String readTextElement(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "text");
        String txt = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "text");
        return txt;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private OsmAndExtensions readOsmAndRteExtensions(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, "extensions");

        OsmAndExtensions extensions = new OsmAndExtensions();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "time":
                    extensions.setTime(readOsmAndTime(parser));
                    break;
                case "turn":
                    extensions.setTurn(readOsmAndTurn(parser));
                    break;
                case "turn-angle":
                    extensions.setTurnAngle(readOsmAndTurnAngle(parser));
                    break;
                case "offset":
                    extensions.setOffset(readOsmAndOffset(parser));
                    break;
                default:
                    skip(parser);
                    break;
            }
        }
        return extensions.isValid() ? extensions : null;
    }

    private GarminExtensions readGarminWptExtensions(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, GPXX, "extensions");

        GarminExtensions extensions = new GarminExtensions();

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            switch (name) {
                case "Proximity":
                    extensions.setProximity(readGarminProximity(parser));
                default:
                    skip(parser);
                    break;
            }
        }
        if (extensions.isValid()) {
            options.useExtensions = true;
            return extensions;
        }
        return null;
    }

    private int readOsmAndTime(XmlPullParser parser) throws IOException, XmlPullParserException {
        //OsmAnd does not declare it's own namespace for the rte-extensions :-(
        parser.require(XmlPullParser.START_TAG, ns, "time");
        String txt = readText(parser);
        int time = Integer.parseInt(txt);
        parser.require(XmlPullParser.END_TAG, ns, "time");
        return time;
    }

    private String readOsmAndTurn(XmlPullParser parser) throws IOException, XmlPullParserException {
        //OsmAnd does not declare it's own namespace for the rte-extensions :-(
        parser.require(XmlPullParser.START_TAG, ns, "turn");
        String txt = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, "turn");
        return txt;
    }

    private double readOsmAndTurnAngle(XmlPullParser parser) throws IOException, XmlPullParserException {
        //OsmAnd does not declare it's own namespace for the rte-extensions :-(
        parser.require(XmlPullParser.START_TAG, ns, "turn-angle");
        String txt = readText(parser);
        double angle = Double.parseDouble(txt);
        parser.require(XmlPullParser.END_TAG, ns, "turn-angle");
        return angle;
    }

    private int readOsmAndOffset(XmlPullParser parser) throws IOException, XmlPullParserException {
        //OsmAnd does not declare it's own namespace for the rte-extensions :-(
        parser.require(XmlPullParser.START_TAG, ns, "offset");
        String txt = readText(parser);
        int time = Integer.parseInt(txt);
        parser.require(XmlPullParser.END_TAG, ns, "offset");
        return time;
    }

    private double readGarminProximity(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, GPXX, "Proximity");
        String txt = readText(parser);
        double proximity = Double.NaN;
        try {
            proximity = Double.parseDouble(txt);
        } catch (NumberFormatException nfe) {
        }
        parser.require(XmlPullParser.END_TAG,GPXX, "Proximity");
        return proximity;
    }

    public String getName() {
        String metaName = metadata.getName();
        if (metaName != null && !metaName.isEmpty()) {
            return metaName;
        }
        for (Track trk : tracks) {
            String trkName = trk.getName();
            if (trkName != null && !trkName.isEmpty()) {
                return trkName;
            }
        }
        for (Route rte : routes) {
            String rteName = rte.getName();
            if (rteName != null && !rteName.isEmpty()) {
                return rteName;
            }
        }
        return courseName;
    }

    public void rte2wpts() {
        for (Route rte : routes) {
            for (RtePoint rtept : rte.getRtePoints()) {
                WptPoint newWpt = new WptPoint();
                newWpt.setLat(rtept.getLat());
                newWpt.setLon(rtept.getLon());
                newWpt.setEle(rtept.getEle());
                newWpt.setTime(rtept.getTime());
                newWpt.setName(rtept.getName());
                newWpt.setDesc(rtept.getDesc());
                newWpt.setType(rtept.getType());
                OsmAndExtensions osmAndExtensions = rtept.getExtensions();
                if (osmAndExtensions == null) {
                    newWpt.setSym(rtept.getSym());
                } else {
                    String turn = osmAndExtensions.getTurn();
                    if (turn != null) {
                        switch (turn) {
                            case "TU":   // u-turn
                                newWpt.setSym("U-Turn");
                                break;
                            case "TSHL": // turn sharp left
                                newWpt.setSym("SharpLeft");
                                break;
                            case "TL":   // turn left
                                newWpt.setSym("Left");
                                break;
                            case "TSLL": // turn slight left
                                newWpt.setSym("SlightLeft");
                                break;
                            case "KL":   // keep left
                                newWpt.setSym("KeepLeft");
                                break;
                            case "C":    // straight
                                newWpt.setSym("Straight");
                                break;
                            case "KR":   // keep right
                                newWpt.setSym("KeepRight");
                                break;
                            case "TSLR": // turn slight right
                                newWpt.setSym("SlightRight");
                                break;
                            case "TR":   // turn right
                                newWpt.setSym("Right");
                                break;
                            case "TSHR": // turn sharp right
                                newWpt.setSym("SharpRight");
                                break;
                            case "TRU":  // u-turn
                                newWpt.setSym("U-TurnRight");
                                break;
                            default: {
                                if (turn.startsWith("RNDB")) { // take roundabout exit nr))
                                    int num = Integer.parseInt(turn.substring(4));
                                    switch (num) {
                                        case 1:
                                            newWpt.setSym("RoundAb1");
                                            break;
                                        case 2:
                                            newWpt.setSym("RoundAb2");
                                            break;
                                        case 3:
                                            newWpt.setSym("RoundAb3");
                                            break;
                                        default:
                                            newWpt.setSym("RoundAb4");
                                            break;
                                    }
                                } else if (turn.startsWith("RNLB")) { // take roundabout exit nr. (to the left)
                                    int num = Integer.parseInt(turn.substring(4));
                                    switch (num) {
                                        case 1:
                                            newWpt.setSym("RoundAbL1");
                                            break;
                                        case 2:
                                            newWpt.setSym("RoundAbL2");
                                            break;
                                        case 3:
                                            newWpt.setSym("RoundAbL3");
                                            break;
                                        default:
                                            newWpt.setSym("RoundAbL4");
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
                GarminExtensions garminExtensions = new GarminExtensions();
                garminExtensions.setProximity(50.0);
                newWpt.setExtensions(garminExtensions);
                if (newWpt.isValid()) {
                    wayPoints.add(newWpt);
                }
                options.useExtensions = true;
            }
        }
    }

    public void writeGpx(OutputStream out) throws XmlPullParserException, java.io.IOException {
        XmlSerializer serializer= Xml.newSerializer();
        serializer.setOutput(out,"utf-8");
        serializer.startDocument("utf-8",false);
        serializer.setPrefix("xsi", XSI);
        if (options.useExtensions) {
            serializer.setPrefix("gpxx", GPXX);
            serializer.setPrefix("wptx1", WPTX);
//            serializer.setPrefix("gpxtpx", TPTX);
        }
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", options.indent);
        StringBuilder schemaLocation = new StringBuilder(HTTP_WWW_TOPOGRAFIX_COM_GPX_1_1+" http://www.topografix.com/GPX/1/1/gpx.xsd");
        if (options.useExtensions) {
            schemaLocation.append(" ").append(GPXX).append(" http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd");
            schemaLocation.append(" ").append(WPTX).append(" http://www.garmin.com/xmlschemas/WaypointExtensionv1.xsd");
//            schemaLocation.append(" ").append(TPTX).append(" http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd");
        }
        serializer.startTag(null,"gpx" )
                .attribute(null,"xmlns", HTTP_WWW_TOPOGRAFIX_COM_GPX_1_1)
                .attribute(null, "version", "1.1")
                .attribute(null, "creator", "gexporter" )
                .attribute( XSI, "schemaLocation", schemaLocation.toString() );

        writeMeta(serializer,metadata);
        for (WptPoint wayPoint : wayPoints) {
            writeWpt(serializer,wayPoint);
        }
        for (Track trk : tracks) {
            writeTrk(serializer, trk);
        }
        for (Route rte : routes) {
            writeRte(serializer, rte);
        }
        serializer.endDocument();
    }

    private void writeMeta(XmlSerializer serializer, Metadata meta) throws IOException {
        if (meta != null && meta.isValid()) {
            serializer.startTag(null,"meta");
            writeName(serializer, meta.getName());
            writeAuthor(serializer, meta.getAuthor());
            writeLinks(serializer, meta.getLinks());
            writeTime(serializer, meta.getTime());
            serializer.endTag(null,"meta");
        }
    }

    private void writeAuthor(XmlSerializer serializer, Metadata.Author author) throws IOException {
        if (author != null && author.isValid()) {
            serializer.startTag(null,"author");
            writeName(serializer, author.getName());
            writeLinks(serializer, author.getLinks());
            serializer.endTag(null,"author");
        }
    }

    private void writeLinks(XmlSerializer serializer, List<Link> links) throws IOException {
        for (Link link : links) {
            if (link != null && link.isValid()) {
                serializer.startTag(null,"link")
                        .attribute(null,"href",link.getHref());
                writeTextElement(serializer,link.getText());
                writeType(serializer,link.getType());
                serializer.endTag(null,"link");
            }
        }
    }

    private void writeTextElement(XmlSerializer serializer, String text) throws IOException {
        if (text != null && !text.isEmpty()) {
            serializer.startTag(null,"text")
                    .text(text)
                    .endTag(null,"text");
        }
    }

    private void writeWptExtensions(XmlSerializer serializer, GarminExtensions extensions) throws IOException {
        serializer.startTag(null,"extensions")
                .startTag(WPTX,"WaypointExtension")
                .startTag(WPTX,"Proximity")
                .text(String.format((Locale) null, "%1$.6f", extensions.getProximity()))
                .endTag(WPTX, "Proximity")
                .endTag(WPTX,"WaypointExtension")
                .endTag(null,"extensions");
    }

    private void writeTrk(XmlSerializer serializer, Track trk) throws IOException {
        serializer.startTag(null, "trk");
        String trkName = trk.getName();
        writeName(serializer, trkName == null || trkName.isEmpty() ? courseName : trkName);
        writeDesc(serializer, trk.getDesc());
        writeLinks(serializer,trk.getLinks());
        for (TrkSeg trkSeg : trk.getSegments()) {
            serializer.startTag(null, "trkseg");
            for (WayPoint trkPoint : trkSeg.getTrkPoints()) {
                serializer.startTag(null, "trkpt")
                        .attribute(null, "lat", String.format((Locale) null, "%1$.6f", trkPoint.getLat()))
                        .attribute(null, "lon", String.format((Locale) null, "%1$.6f", trkPoint.getLon()));
                writeTime(serializer, trkPoint.getTime());
                writeEle(serializer, trkPoint.getEle());
                serializer.endTag(null, "trkpt");
            }
            serializer.endTag(null, "trkseg");
        }
        serializer.endTag(null, "trk");
    }

    private void writeRte(XmlSerializer serializer, Route rte) throws IOException {
        serializer.startTag(null, "rte");
        String rteName = rte.getName();
        writeName(serializer, rteName == null || rteName.isEmpty() ? courseName : rteName);
        writeDesc(serializer, rte.getDesc());
        writeLinks(serializer,rte.getLinks());
        for (ExtWayPoint rtePt : rte.getRtePoints()) {
            serializer.startTag(null, "rtept")
                    .attribute(null, "lat", String.format((Locale) null, "%1$.6f", rtePt.getLat()))
                    .attribute(null, "lon", String.format((Locale) null, "%1$.6f", rtePt.getLon()));
            writeExtWayPointElements(serializer, rtePt);
            serializer.endTag(null, "rtept");
        }
        serializer.endTag(null, "rte");
    }

    private void writeWpt(XmlSerializer serializer, WptPoint wayPoint) throws IOException {
        serializer.startTag(null, "wpt")
                .attribute(null, "lat", String.format((Locale) null, "%1$.6f", wayPoint.getLat()))
                .attribute(null, "lon", String.format((Locale) null, "%1$.6f", wayPoint.getLon()));
        writeExtWayPointElements(serializer, wayPoint);
        GarminExtensions extensions = wayPoint.getExtensions();
        if (extensions != null && extensions.isValid()) {
            writeWptExtensions(serializer, extensions);
        }
        serializer.endTag(null, "wpt");
    }

    private void writeExtWayPointElements(XmlSerializer serializer, ExtWayPoint wayPoint) throws IOException {
        String name = wayPoint.getName();
        String desc = wayPoint.getDesc();
        writeName(serializer, name == null || name.isEmpty() ? desc : name );
        writeDesc(serializer, desc);
        writeTime(serializer, wayPoint.getTime());
        writeEle(serializer, wayPoint.getEle());
        writeSym(serializer, wayPoint.getSym());
        writeType(serializer, wayPoint.getType());
    }

    private void writeName(XmlSerializer serializer, String name) throws IOException {
        if (name != null && !name.isEmpty()) {
            serializer.startTag(null,"name")
                    .text(name)
                    .endTag(null,"name");
        }
    }

    private void writeDesc(XmlSerializer serializer, String desc) throws IOException {
        if (desc != null) {
            serializer.startTag(null,"desc")
                    .text(desc)
                    .endTag(null,"desc");
        }
    }

    private void writeSym(XmlSerializer serializer, String sym) throws IOException {
        if (sym != null) {
            serializer.startTag(null,"sym")
                    .text(sym)
                    .endTag(null,"sym");
        }
    }

    private void writeType(XmlSerializer serializer, String type) throws IOException {
        if (type != null) {
            serializer.startTag(null,"type")
                    .text(type)
                    .endTag(null,"type");
        }
    }

    private void writeTime(XmlSerializer serializer, Date time) throws IOException {
        if (time != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            serializer.startTag(null,"time")
                    .text(dateFormat.format(time))
                    .endTag(null,"time");
        }
    }

    private void writeEle(XmlSerializer serializer, Double ele) throws IOException {
        if (ele != null && !ele.isNaN()) {
            serializer.startTag(null,"ele")
                    .text(String.format((Locale) null, "%1$.2f", ele))
                    .endTag(null,"ele");
        }
    }
}
