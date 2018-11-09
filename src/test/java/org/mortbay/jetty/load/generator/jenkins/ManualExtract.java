package org.mortbay.jetty.load.generator.jenkins;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.mortbay.jetty.load.generator.jenkins.result.ElasticHost;
import org.mortbay.jetty.load.generator.jenkins.result.LoadResultProjectAction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ManualExtract
{

    public static void main( String[] args )
        throws Exception
    {
        ElasticHost elasticHost = //
            new ElasticHost( "extract", "localhost", "http", null, null, 9200 );

        Map<String, String> versions = LoadResultProjectAction.getJettyVersions( elasticHost );

        System.out.println( "versions " + versions );

        long sinceWhen = new Date().getTime() - ( 1000 * 60 * 60 * 24 * 20 );
        //printVersionStats( "9.4.11.v20180605", elasticHost, sinceWhen );
        printVersionStats( "9.4.12.v20180830", elasticHost, sinceWhen ); // "9.4.11.v20180605" // 9.4.12.v20180830
        //printVersionStats( "9.4.12-NO-LOGGER-SNAPSHOT", elasticHost, sinceWhen );
        //printVersionStats( "9.4.12-LOGGER-DEBUG-DISABLED-SNAPSHOT", elasticHost, sinceWhen );
    }


    public static void printVersionStats( String version, ElasticHost elasticHost, long sinceTimestamp )
        throws Exception
    {
        List<RunInformations> runInformations = getResultsForVersion( version, elasticHost, sinceTimestamp, 1000 );

        Map<Integer, List<RunInformations>> perQps = new HashMap<>();
        runInformations.stream().forEach( informations -> {
            int qps = informations.getEstimatedQps();
            List<RunInformations> informationsList = perQps.get( qps );
            if ( informationsList == null )
            {
                informationsList = new ArrayList<>();
                perQps.put( qps, informationsList );
            }
            informationsList.add( informations );
        } );
        perQps.entrySet().stream().sorted( Comparator.comparingInt( value -> value.getKey() ) ) //
            .forEach( qpsRunsList -> {
                int qps = qpsRunsList.getKey();
                List<RunInformations> runInformationsList = qpsRunsList.getValue();
                // display only significant results
                //if ( runInformationsList.size() > 5 ) // && Arrays.asList(27900, 45000).contains( qps ) )
                //{
                    runInformations.sort( Comparator.comparing( RunInformations::getEndTimeStamp ).reversed() );
                    SummaryStatistics statsMean = new SummaryStatistics();
                    SummaryStatistics statsValue90 = new SummaryStatistics();
                    SummaryStatistics statsValue50 = new SummaryStatistics();
                    int maxResults = 50;
                    runInformationsList = runInformationsList.subList( 0, runInformationsList.size() >= maxResults
                        ? maxResults
                        : runInformationsList.size() );
                    runInformationsList.stream().forEach( runInformation -> {
                        statsMean.addValue( runInformation.getMean() );
                        statsValue90.addValue( runInformation.getValue90() );
                        statsValue50.addValue( runInformation.getValue50() );
                    } );

                    System.out.println( version + ", qps " + qps + " :" );
                    System.out.println( "mean: " + String.format( "%.3f", statsMean.getMean() ) //
                                            + ", value50 mean: " + String.format( "%.3f", statsValue50.getMean() ) //
                                            + ", value90 mean: " + String.format( "%.3f", statsValue90.getMean() ) //
                                            + ", runs: " + runInformationsList.size() );
//                }
//                else
//                {
//                    //System.out.println( "ignore qps:" + qps );
//                }
            } );

    }


    public static List<RunInformations> getResultsForVersion( String version, ElasticHost elasticHost,
                                                              long sinceTimestamp, int buildNumber )
        throws Exception
    {
        List<RunInformations> runInformations = LoadResultProjectAction.searchRunInformations(version, elasticHost, 300);

        List<RunInformations> sinceList = //
            runInformations.stream().filter( runInformation -> runInformation.getEndTimeStamp() > sinceTimestamp ) //
                .sorted( Comparator.comparing( RunInformations::getEndTimeStamp ).reversed() ).collect(
                Collectors.toList() );
        if ( buildNumber > sinceList.size() )
        {
            buildNumber = sinceList.size();
        }
        return sinceList.subList( 0, buildNumber > 0 ? buildNumber : sinceList.size() );
    }


}
