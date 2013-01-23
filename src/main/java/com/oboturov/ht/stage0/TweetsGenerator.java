package com.oboturov.ht.stage0;

import com.oboturov.ht.ConfigUtils;
import com.oboturov.ht.Tweet;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.ChainMapper;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.mapred.lib.NLineInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

/**
 * The Script generates Tweets from raw Stanford-tweets data and saves each Tweets as a JSON string to the output file.
 * Script accepts two parameters:
 * <ol>
 *   <li>comma-separated list of input files</li>
 *   <li>output file name</li>
 * </ol>
 * The Script generates
 * @author aoboturov
 */
public class TweetsGenerator extends Configured implements Tool {

    @Override
    public int run(final String[] args) throws Exception {
        final JobConf conf = new JobConf(getConf(), getClass());
        conf.setJobName("tweets-generator");

        conf.setNumReduceTasks(0);
        conf.setNumMapTasks(10);

        conf.setInt("mapreduce.input.lineinputformat.linespermap", 4 * 1000000);
        ConfigUtils.makeMapOutputCompressedWithBZip2(conf);

        conf.setInputFormat(NLineInputFormat.class);
        conf.setOutputFormat(TextOutputFormat.class);

        FileInputFormat.setInputPaths(conf, args[0]);
        FileOutputFormat.setOutputPath(conf, new Path(args[1]));

        // Extract tweets
        ChainMapper.addMapper(
                conf,
                TweetsReader.Map.class,
                LongWritable.class,
                Text.class,
                NullWritable.class,
                Tweet.class,
                false,
                new JobConf(false)
        );

        JobClient.runJob(conf);

        return 0;
    }

    public static void main(String[] args) throws Exception {
        // Let ToolRunner handle generic command-line options
        int res = ToolRunner.run(new TweetsGenerator(), args);

        System.exit(res);
    }
}
