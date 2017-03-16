package org.wso2.siddhi.core.query.selector.attribute.aggregator.incremental;

import org.wso2.siddhi.query.api.aggregation.TimePeriod;
import org.wso2.siddhi.query.api.expression.AttributeFunction;
import org.wso2.siddhi.query.api.expression.Expression;

import java.util.*;

public class IncrementalCalculator {
    // has the property called Duration it can be SECOND, MINUTE and etc
    // list of functions operators
    // Also need to have a link to the parent for instance a link from SECOND to MINUTE
    //

    private TimePeriod.Duration duration;
    private IncrementalCalculator child;
    private List<AttributeFunction> functionAttributes;
    private List<IncrementalAggregator> incrementalAggregators;

    private IncrementalCalculator(TimePeriod.Duration duration, IncrementalCalculator child,
                                  List<AttributeFunction> functionAttributes) {
        this.duration = duration;
        this.child = child;
        this.functionAttributes = functionAttributes;
        this.incrementalAggregators = createIncrementalAggregators(this.functionAttributes);
    }

    private List<IncrementalAggregator> createIncrementalAggregators(List<AttributeFunction> functionAttributes) {
        List<IncrementalAggregator> incrementalAggregators = new ArrayList<>();
        for (AttributeFunction function : functionAttributes) {
            if (function.getName().equals("avg")) {
                AvgIncrementalAttributeAggregator average = new AvgIncrementalAttributeAggregator(function);
                incrementalAggregators.add(average);
            } else {
                // TODO: 3/10/17 Exception....
            }
        }
        return incrementalAggregators;
    }

    public Set<Expression> getBaseAggregators() {
        Set<Expression> baseAggregators = new HashSet<>();
        for(IncrementalAggregator aggregator : this.incrementalAggregators){
            Expression[] bases = aggregator.getBaseAggregators();
            baseAggregators.addAll(Arrays.asList(bases));
        }
        return baseAggregators;
    }



    public static IncrementalCalculator second(List<AttributeFunction> functionAttributes, IncrementalCalculator child) {
        IncrementalCalculator second = new IncrementalCalculator(TimePeriod.Duration.SECONDS, child, functionAttributes);
        return second;
    }

    public static IncrementalCalculator minute(List<AttributeFunction> functionAttributes, IncrementalCalculator child) {
        IncrementalCalculator minute = new IncrementalCalculator(TimePeriod.Duration.MINUTES, child, functionAttributes);
        return minute;
    }
}