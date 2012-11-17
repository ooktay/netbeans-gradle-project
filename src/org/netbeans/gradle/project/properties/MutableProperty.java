package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;

public interface MutableProperty<ValueType> {
    // Setting and getting the value of a property is only allowed from the EDT.
    public void setValueFromSource(PropertySource<? extends ValueType> source);
    public void setValue(ValueType value);
    public ValueType getValue();

    public boolean isDefault();

    public void addChangeListener(ChangeListener listener);
    public void removeChangeListener(ChangeListener listener);
}
