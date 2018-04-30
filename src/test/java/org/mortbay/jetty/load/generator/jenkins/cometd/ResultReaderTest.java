package org.mortbay.jetty.load.generator.jenkins.cometd;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.mortbay.jetty.load.generator.jenkins.cometd.beans.LoadResults;

import java.nio.file.Paths;

/**
 *
 */
public class ResultReaderTest
{

    @Test
    public void testReadJsonResult()
        throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();

        LoadResults loadResults =
            objectMapper.readValue( Paths.get( "src/test/resources/cometd/result.json" ).toFile(), LoadResults.class );

        Assert.assertNotNull( loadResults.getResults() );

        Assert.assertNotNull( loadResults.getResults().getCpu() );

        Assert.assertEquals( Float.valueOf( (float)34.145973 ), Float.valueOf( loadResults.getResults().getCpu().getValue() ) );
        Assert.assertEquals( "%", loadResults.getResults().getCpu().getUnit() );

    }

}
