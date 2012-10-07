package com.oboturov.ht.etl;

import com.oboturov.ht.Tweet;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

/**
 * @author aoboturov
 */
public class TweetsReader {

    private final static Logger logger = Logger.getLogger(TweetsReader.class);

    public static class Map extends MapReduceBase implements Mapper<LongWritable, Text, LongWritable, Tweet> {

        private static final String HTTP_TWITTER_COM = "http://twitter.com/";
        private static final String HTTP_WWW_TWITTER_COM = "http://www.twitter.com/";
        private static final String EMPTY_POST_INDICATION = "No Post Title";

        private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        private Long time;
        private String user;
        private String post;

        private boolean skipTweet = false;

        public void map(final LongWritable key, final Text value, final OutputCollector<LongWritable, Tweet> output, final Reporter reporter) throws IOException {
            final String line = value.toString();
            if (line == null || line.isEmpty() || line.length() < 3) {
                skipTweet = false;
                return;
            }
            final String text = line.substring(2).trim(); // Skip {T,U,V}\tab sequence and consider rest as content.
            final char lineType = line.charAt(0); // One of {T,U,V} characters.
            switch (lineType) {
                case 'T':
                    try {
                        this.time = dateFormat.parse(text).getTime();
                    } catch (final ParseException e) {
                        skipTweet = true;
                        logger.error("Wrong date format for date: '"+text+"'");
                    }
                    return;
                case 'U':
                    if (text.startsWith(HTTP_TWITTER_COM)) {
                        this.user = "@"+text.substring(HTTP_TWITTER_COM.length());
                    } else if (text.startsWith(HTTP_WWW_TWITTER_COM)) {
                        this.user = "@"+text.substring(HTTP_WWW_TWITTER_COM.length());
                    } else {
                        this.user = text;
                        logger.error("Not normalized user name: '"+text+"'");
                    }
                    return;
                case 'W':
                    this.post = text;
                    if (EMPTY_POST_INDICATION.equals(text)) {
                        skipTweet = true;
                    }
                    break;
                default:
                    return;
            }
            if (this.time == null || this.user == null || this.post == null) {
                skipTweet = true;
            }
            try {
                if (skipTweet) {
                    return;
                }
                output.collect(new LongWritable(this.time), new Tweet(this.user, this.time, this.post));
            } finally {
                this.time = null;
                this.user = null;
                this.post = null;
            }
        }
    }

    public static class Reduce extends MapReduceBase implements Reducer<LongWritable, Tweet, LongWritable, Tweet> {
        /**
         * ID-map to produce file parsing output.
         */
        public void reduce(final LongWritable key, final Iterator<Tweet> values, final OutputCollector<LongWritable, Tweet> output, final Reporter reporter) throws IOException {
            while(values.hasNext()) {
                output.collect(key, values.next());
            }
        }
    }
}
