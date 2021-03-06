/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.pmml.regression.tests;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.evaluator.core.PMMLContextImpl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RunWith(Parameterized.class)
@Ignore("DROOLS-5209")
public class MissingDataRegressionTest extends AbstractPMMLRegressionTest {

    private static final String MODEL_NAME = "missingValues_Model";
    private static final String PMML_SOURCE = "missingDataRegression.pmml";
    private static final String TARGET_FIELD = "result";

    private Double x;
    private String y;
    private double expectedResult;

    public MissingDataRegressionTest(Double x, String y, double expectedResult) {
        this.x = x;
        this.y = y;
        this.expectedResult = expectedResult;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Double.valueOf(0), "classA", 22}, {Double.valueOf(25), "classB", 92},
                {Double.valueOf(25), null, 92}, {null, "classC", 72},
                {null, null, 52}
        });
    }

    @Test
    public void testMissingValuesRegression() {
        final KiePMMLModel pmmlModel = loadPMMLModel(PMML_SOURCE);

        final Map<String, Object> inputData = new HashMap<>();
        if (x != null) {
            inputData.put("x", x.doubleValue());
        }
        if (y != null) {
            inputData.put("y", y);
        }

        final PMMLRequestData pmmlRequestData = getPMMLRequestData(MODEL_NAME, inputData);
        PMML4Result pmml4Result = EXECUTOR.evaluate(pmmlModel, new PMMLContextImpl(pmmlRequestData), RELEASE_ID);

        Assertions.assertThat(pmml4Result).isNotNull();
        Assertions.assertThat(pmml4Result.getResultVariables()).containsKey(TARGET_FIELD);
        Assertions.assertThat((Double) pmml4Result.getResultVariables().get(TARGET_FIELD))
                .isEqualTo(expectedResult);
    }
}
