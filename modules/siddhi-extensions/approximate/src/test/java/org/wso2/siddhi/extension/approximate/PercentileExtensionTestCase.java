/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.extension.approximate;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.wso2.siddhi.core.ExecutionPlanRuntime;
import org.wso2.siddhi.core.SiddhiManager;
import org.wso2.siddhi.core.event.Event;
import org.wso2.siddhi.core.query.output.callback.QueryCallback;
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
        log.info("PercentileExtension TestCase ..............");
        SiddhiManager siddhiManager = new SiddhiManager();

        String inStreamDefinition = "define stream inputStream (number double);";
        String query = ("@info(name = 'query1') " +
                "from inputStream#approximate:percentile(number,0.5,0.01)" +
                "select * " +
                "insert into outputStream;");

        ExecutionPlanRuntime executionPlanRuntime = siddhiManager.createExecutionPlanRuntime(inStreamDefinition + query);

        executionPlanRuntime.addCallback("outputStream", new StreamCallback() {
            @Override
            public void receive(Event[] events) {
                EventPrinter.print(events);
                for (Event inEvent : events) {
                    count++;
                    if (count == 1) {
                        Assert.assertEquals(2.3, inEvent.getData(1));
                    }
                    if (count == 2) {
                        Assert.assertEquals(3.35, inEvent.getData(1));
                    }
                    if (count == 3) {
                        Assert.assertEquals(2.3, inEvent.getData(1));
                    }
                    if (count == 4) {
                        Assert.assertEquals(3.35, inEvent.getData(1));
                    }
                    eventArrived = true;
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
        Assert.assertEquals(4, count);
        Assert.assertTrue(eventArrived);
        executionPlanRuntime.shutdown();
    }

  
}
