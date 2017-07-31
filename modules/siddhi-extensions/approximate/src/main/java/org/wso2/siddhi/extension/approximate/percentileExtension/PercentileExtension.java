/*
* Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.siddhi.extension.approximate.percentileExtension;

import org.wso2.siddhi.core.config.ExecutionPlanContext;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.event.stream.populater.ComplexEventPopulater;
import org.wso2.siddhi.core.exception.ExecutionPlanCreationException;
import org.wso2.siddhi.core.executor.ConstantExpressionExecutor;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.query.processor.stream.StreamProcessor;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.util.*;

/**
 * The following code conducts finding percentiles of an event stream.
 * This implements the tdigest algorithm for approximate answers which was originally described in
 * https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=web&cd=1&ved=0ahUKEwi7n5bFy_bUAhWHqI8KHcKqAsgQFgglMAA
 * &url=https%3A%2F%2Fraw.githubusercontent.com%2Ftdunning%2Ft-digest%2Fmaster%2Fdocs%2Ft-digest-paper%2Fhisto.pdf
 * &usg=AFQjCNFLJPx3pzBKx1ipxlLgzKtKA9ynNA
 */

/**
 * from inputStream#approximate:percentile(number,percentile,accuracy) ....
 */
public class PercentileExtension extends StreamProcessor{

    private double percentileNumber = 0;
    private double accuracy = 0.01;
    private PercentileCalculater percentileCalculater;

    /**
     * This will be called only once and this can be used to acquire
     * required resources for the processing element.
     * This will be called after initializing the system and before
     * starting to process the events.
     */
    @Override
    public void start() {
        //Do nothing
    }

    /**
     * This will be called only once and this can be used to release
     * the acquired resources for processing.
     * This will be called before shutting down the system.
     */
    @Override
    public void stop() {
        //Do nothing
    }

    /**
     * Used to collect the serializable state of the processing element, that need to be
     * persisted for the reconstructing the element to the same state on a different point of time
     *
     * @return stateful objects of the processing element as an array
     */
    @Override
    public Object[] currentState() {
        return new Object[0];
    }

    /**
     * Used to restore serialized state of the processing element, for reconstructing
     * the element to the same state as if was on a previous point of time.
     *
     * @param state the stateful objects of the element as an array on
     *              the same order provided by currentState().
     */
    @Override
    public void restoreState(Object[] state) {
        //Do nothing
    }

    /**
     * The main processing method that will be called upon event arrival
     *
     * @param streamEventChunk      the event chunk that need to be processed
     * @param nextProcessor         the next processor to which the success events need to be passed
     * @param streamEventCloner     helps to clone the incoming event for local storage or modification
     * @param complexEventPopulater helps to populate the events with the resultant attributes
     */
    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor,
                           StreamEventCloner streamEventCloner, ComplexEventPopulater complexEventPopulater) {

        while (streamEventChunk.hasNext()) {
            ComplexEvent complexEvent = streamEventChunk.next();
            double newData = (Double) attributeExpressionExecutors[0].execute(complexEvent);
            percentileCalculater.add(newData);

            Object[] outputData = {percentileCalculater.getPercentile(percentileNumber)};

            if (outputData == null) {
                streamEventChunk.remove();
            } else {
//                alter the stream
                complexEventPopulater.populateComplexEvent(complexEvent,outputData);
            }
        }

        nextProcessor.process(streamEventChunk);

    }

    /**
     * The init method of the LinearRegressionStreamProcessor, this method will be called before other methods
     *
     * @param inputDefinition              the incoming stream definition
     * @param attributeExpressionExecutors the executors of each function parameters
     * @param executionPlanContext         the context of the execution plan
     * @return the additional output attributes introduced by the function
     */
    @Override
    protected List<Attribute> init(AbstractDefinition inputDefinition, ExpressionExecutor[] attributeExpressionExecutors,
                                   ExecutionPlanContext executionPlanContext) {
//      Capture constant inputs
        if (attributeExpressionExecutors[1] instanceof ConstantExpressionExecutor){
            try {
                percentileNumber = ((Double)attributeExpressionExecutors[1].execute(null));
            } catch(ClassCastException c) {
                throw new ExecutionPlanCreationException("Percentile number should be of type double");
            }
        }

        if (attributeExpressionLength>2 && attributeExpressionExecutors[2] instanceof ConstantExpressionExecutor){
            try {
                accuracy = ((Double)attributeExpressionExecutors[2].execute(null));
            } catch(ClassCastException c) {
                throw new ExecutionPlanCreationException("Accuracy should be of type double and between 0 and 1");
            }
        }

//      Create the initial PercentileCalculator based on the accuracy
//        if(accuracy < 1) {
//            percentileCalculater = new PercentileNormalApproximator();
//        }
//        else{
//            percentileCalculater = new PercentileNormalApproximator();
//        }
        percentileCalculater = new PercentileApproximator();

        percentileCalculater.initialize(percentileNumber, accuracy);


//      Additional attribute declaration
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new Attribute("percentile",Attribute.Type.DOUBLE));

        return attributes;
    }

}
