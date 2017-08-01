
package org.wso2.siddhi.extension.approximate;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;

public class CardinalityExtensionTestCase {
    static final Logger LOG = Logger.getLogger(CardinalityExtensionTestCase.class);
    private volatile int count;
    private volatile boolean eventArrived;

    @Before
    public void init() {
        count = 0;
        eventArrived = false;
    }



    @Test
    public void testCardinalityTestCase() throws InterruptedException {
        final int noOfEvents = 1000;

        LOG.info("tdigest TestCase ..............");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream inputStream (number int);";
        String query = ("@info(name = 'query1') " +
                "from inputStream#approximate:cardinality(number,0.001)" +
                "select * " +
                "insert into outputStream;");

        ExecutionPlanRuntime executionPlanRuntime =
                siddhiManager.createExecutionPlanRuntime(inStreamDefinition + query);

        executionPlanRuntime.addCallback("outputStream", new StreamCallback() {

            long i = 1;
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event event : events) {
                    Assert.assertEquals(i, event.getData(1));
                    i++;
                }
                eventArrived = true;
            }
        });



        InputHandler inputHandler = executionPlanRuntime.getInputHandler("inputStream");
        executionPlanRuntime.start();


        for (double j = 0; j < noOfEvents; j++) {
            inputHandler.send(new Object[]{j});
        }


        Thread.sleep(100);
        Assert.assertEquals(1000, noOfEvents);
        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();
    }
  
}
