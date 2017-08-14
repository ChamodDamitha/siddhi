package org.wso2.siddhi.extension.approximate;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;

/**
 * Created by chamod on 8/11/17.
 */
public class TestJoin {
    static final Logger LOG = Logger.getLogger(TestJoin.class);

    @Test
    public void test() {
        LOG.info("SimilarityExtension TestCase ..............");
        SiddhiManager siddhiManager = new SiddhiManager();


        String inStreamDefinition1 = "define stream inputStream1 (x int);";
        String inStreamDefinition2 = "define stream inputStream2 (y int);";

        String query = ("@info(name = 'query1') " +
                "from inputStream1, inputStream2" +
                "select * " +
                "insert into outputStream;");


        ExecutionPlanRuntime executionPlanRuntime =
                siddhiManager.createExecutionPlanRuntime(inStreamDefinition1 + inStreamDefinition2 + query);


    }

}
