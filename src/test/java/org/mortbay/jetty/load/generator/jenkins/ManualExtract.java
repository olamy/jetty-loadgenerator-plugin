package org.mortbay.jetty.load.generator.jenkins;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.mortbay.jetty.load.generator.jenkins.result.ElasticHost;
import org.mortbay.jetty.load.generator.jenkins.result.LoadResultProjectAction;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ManualExtract
{

    public static void main( String[] args )
        throws Exception
    {

        ElasticHost elasticHost = //
            new ElasticHost( "extract", "localhost", "http", null, null, 9200 );
        long aWeekAgo = new Date().getTime() - ( 1000 * 60 * 60 * 24 * 4 );
        printVersionStats( "9.4.10.v20180503", elasticHost, aWeekAgo );
        printVersionStats( "9.4.11-SNAPSHOT", elasticHost, aWeekAgo );
        printVersionStats( "9.4.11-NO-LOGGER-SNAPSHOT", elasticHost, aWeekAgo );
    }


    public static void printVersionStats( String version, ElasticHost elasticHost, long sinceTimestamp )
        throws Exception
    {
        List<RunInformations> runInformations = getResultsForVersion( version, elasticHost, sinceTimestamp );
        SummaryStatistics statsMean = new SummaryStatistics();
        SummaryStatistics statsValue90 = new SummaryStatistics();
        SummaryStatistics statsValue50 = new SummaryStatistics();
        runInformations.stream().forEach( runInformation -> {
            statsMean.addValue( runInformation.getMean() );
            statsValue90.addValue( runInformation.getValue90() );
            statsValue50.addValue( runInformation.getValue50() );
        } );

        System.out.println( version + ":" );
        System.out.println( "mean: " + statsMean.getMean() //
                                + ", value50 mean: " + statsValue50.getMean() //
                                + ", value90 mean: " + statsValue90.getMean() //
                                + ", runs: " + runInformations.size() );

    }


    public static List<RunInformations> getResultsForVersion( String version, ElasticHost elasticHost,
                                                              long sinceTimestamp )
        throws Exception
    {
        List<RunInformations> runInformations = LoadResultProjectAction.searchRunInformations( version, elasticHost );

        List<RunInformations> sinceList = //
            runInformations.stream().filter( runInformation -> runInformation.getEndTimeStamp() > sinceTimestamp ) //
                .collect( Collectors.toList() );

        return sinceList.subList( 0, 25 );
    }


}
