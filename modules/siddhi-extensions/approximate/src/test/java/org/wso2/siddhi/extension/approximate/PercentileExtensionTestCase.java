
package org.wso2.siddhi.extension.approximate;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.stream.input.InputHandler;
import org.wso2.siddhi.core.stream.output.StreamCallback;
import org.wso2.siddhi.core.util.EventPrinter;

public class PercentileExtensionTestCase {
    static final Logger log = Logger.getLogger(PercentileExtensionTestCase.class);
    private volatile int count;
    private volatile boolean eventArrived;

    @Before
    public void init() {
        count = 0;
        eventArrived = false;
    }

    @Test
    public void testPercentileExtensionTestCase() throws InterruptedException {
        log.info("CardinalityExtension TestCase ..............");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream inputStream (number double);";
        String query = ("@info(name = 'query1') " +
                "from inputStream#approximate:percentile(number,0.5,0.1)" +
                "select * " +
                "insert into outputStream;");

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(inStreamDefinition + query);

        executionPlanRuntime.addCallback("outputStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event inEvent : events) {
//                    count++;
//                    if (count == 1) {
//                        Assert.assertEquals(2.3, inEvent.getData(1));
//                    }
//                    if (count == 2) {
//                        Assert.assertEquals(3.35, inEvent.getData(1));
//                    }
//                    if (count == 3) {
//                        Assert.assertEquals(2.3, inEvent.getData(1));
//                    }
//                    if (count == 4) {
//                        Assert.assertEquals(3.35, inEvent.getData(1));
//                    }
//                    eventArrived = true;
                }
            }
        });



        InputHandler inputHandler = executionPlanRuntime.getInputHandler("inputStream");
        executionPlanRuntime.start();
//        for(double i=120;i>0;i-=0.5) {
//            inputHandler.send(new Object[]{i});
//
//        }

        inputHandler.send(new Object[]{2.3});
        inputHandler.send(new Object[]{4.4});
        inputHandler.send(new Object[]{1.8});
        inputHandler.send(new Object[]{34.0});
        Thread.sleep(100);
//        Assert.assertEquals(4, count);
//        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();
    }

    @Test
    public void testTdigestTestCase() throws InterruptedException {
        final int noOfEvents = 1000;

        log.info("tdigest TestCase ..............");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream inputStream (number double);";
        String query = ("@info(name = 'query1') " +
                "from inputStream#approximate:percentile(number,0.5,0.001)" +
                "select * " +
                "insert into outputStream;");

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(inStreamDefinition + query);

        executionPlanRuntime.addCallback("outputStream", new StreamCallback() {

            int i = 0;
            @Override
            public void receive(Event[] events) {
//                EventPrinter.print(events);
                for(Event event : events){
                    Assert.assertEquals(i/2.0, event.getData(1));
                    i++;
                }
                eventArrived = true;
            }
        });



        InputHandler inputHandler = executionPlanRuntime.getInputHandler("inputStream");
        executionPlanRuntime.start();


        for(double j = 0;j < noOfEvents; j++){
            inputHandler.send(new Object[]{j});
        }


        Thread.sleep(100);
//        Assert.assertEquals(1000, noOfEvents);
        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();
    }
  
}
