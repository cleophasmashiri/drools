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
package org.kie.pmml.evaluator.core.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.drools.core.definitions.InternalKnowledgePackage;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.pmml.PMML4Result;
import org.kie.api.pmml.PMMLRequestData;
import org.kie.api.pmml.ParameterInfo;
import org.kie.pmml.commons.model.KiePMMLModel;
import org.kie.pmml.commons.model.enums.PMML_MODEL;
import org.kie.pmml.evaluator.api.container.PMMLPackage;
import org.kie.pmml.evaluator.api.executor.PMMLContext;
import org.kie.pmml.evaluator.api.executor.PMMLRuntime;
import org.kie.pmml.evaluator.core.executor.PMMLModelExecutor;
import org.kie.pmml.evaluator.core.executor.PMMLModelExecutorFinderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PMMLRuntimeImpl implements PMMLRuntime {

    private static final Logger logger = LoggerFactory.getLogger(PMMLRuntimeImpl.class);

    private final KieBase knowledgeBase;
    private final PMMLModelExecutorFinderImpl pmmlModelExecutorFinder;

    public PMMLRuntimeImpl(KieBase knowledgeBase, PMMLModelExecutorFinderImpl pmmlModelExecutorFinder) {
        this.knowledgeBase = knowledgeBase;
        this.pmmlModelExecutorFinder = pmmlModelExecutorFinder;
    }

    @Override
    public List<KiePMMLModel> getModels() {
        logger.trace("getModels");
        List<KiePMMLModel> models = new ArrayList<>();
        knowledgeBase.getKiePackages().forEach(kpkg -> {
            PMMLPackage pmmlPackage = (PMMLPackage) ((InternalKnowledgePackage) kpkg).getResourceTypePackages().get(ResourceType.PMML);
            if (pmmlPackage != null) {
                models.addAll(pmmlPackage.getAllModels().values());
            }
        });
        return models;
    }

    @Override
    public Optional<KiePMMLModel> getModel(String modelName) {
        logger.trace("getModels {}", modelName);
        return getModels()
                .stream()
                .filter(model -> Objects.equals(modelName, model.getName()))
                .findFirst();
    }

    @Override
    public PMML4Result evaluate(KiePMMLModel model, PMMLContext context, String releaseId) {
        logger.debug("evaluate {} {}", model, context);
        addMissingValuesReplacements(model, context);
        Optional<PMMLModelExecutor> pmmlModelExecutor = getFromPMMLModelType(model.getPmmlMODEL());
        return pmmlModelExecutor.isPresent() ? pmmlModelExecutor.get().evaluate(model, context, releaseId) : new PMML4Result();
    }

    /**
     * Add missing input values if defined in original PMML as <b>missingValueReplacement</b>.
     * <p>
     * "missingValueReplacement: If this attribute is specified then a missing input value is automatically replaced by the given value.
     * That is, the model itself works as if the given value was found in the original input. "
     * @param model
     * @param context
     * @see <a href="http://dmg.org/pmml/v4-4/MiningSchema.html#xsdType_MISSING-VALUE-TREATMENT-METHOD">MISSING-VALUE-TREATMENT-METHOD</a>
     */
    protected void addMissingValuesReplacements(KiePMMLModel model, PMMLContext context) {
        logger.debug("addMissingValuesReplacements {} {}", model, context);
        final PMMLRequestData requestData = context.getRequestData();
        final Map<String, ParameterInfo> mappedRequestParams = requestData.getMappedRequestParams();
        final Map<String, Object> missingValueReplacementMap = model.getMissingValueReplacementMap();
        missingValueReplacementMap.forEach((fieldName, missingValueReplacement) -> {
            if (!mappedRequestParams.containsKey(fieldName)) {
                logger.debug("missingValueReplacement {} {}", fieldName, missingValueReplacement);
                requestData.addRequestParam(fieldName, missingValueReplacement);
                context.addMissingValueReplaced(fieldName, missingValueReplacement);
            }
        });
    }

    /**
     * Returns an <code>Optional&lt;PMMLModelExecutor&gt;</code> to allow
     * incremental development of different model-specific executors
     * @param pmmlMODEL
     * @return
     */
    private Optional<PMMLModelExecutor> getFromPMMLModelType(PMML_MODEL pmmlMODEL) {
        logger.trace("getFromPMMLModelType {}", pmmlMODEL);
        return pmmlModelExecutorFinder.getImplementations(false)
                .stream()
                .filter(implementation -> pmmlMODEL.equals(implementation.getPMMLModelType()))
                .findFirst();
    }

}
