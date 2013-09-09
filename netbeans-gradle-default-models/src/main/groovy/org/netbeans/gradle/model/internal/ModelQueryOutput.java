package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.Map;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class ModelQueryOutput implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GenericProjectProperties genericProperties;
    private final Map<Object, Object> projectInfoResults;

    public ModelQueryOutput(GenericProjectProperties genericProperties, Map<Object, Object> projectInfoResults) {
        if (genericProperties == null) throw new NullPointerException("genericProperties");

        this.genericProperties = genericProperties;
        this.projectInfoResults = CollectionUtils.copyNullSafeHashMap(projectInfoResults);
    }

    public GenericProjectProperties getGenericProperties() {
        return genericProperties;
    }

    public Map<Object, Object> getProjectInfoResults() {
        return projectInfoResults;
    }
}