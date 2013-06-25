package org.netbeans.gradle.project.tasks;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;
import org.netbeans.gradle.project.NbStrings;

@SuppressWarnings("serial")
public final class TaskVariableQueryDialog extends JDialog {
    private static final Map<String, UserVariableFactory> FACTORY_MAP = variableFactoryMap();
    private static final Map<String, Integer> TYPE_ORDER = typeOrderMap();

    private final List<UserVariable> variablesToQuery;

    public TaskVariableQueryDialog(Collection<DisplayedTaskVariable> variablesToQuery) {
        super((Frame)null, true);

        this.variablesToQuery = toUserVariables(variablesToQuery);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    private static Map<String, UserVariableFactory> variableFactoryMap() {
        Map<String, UserVariableFactory> result = new HashMap<String, UserVariableFactory>();

        result.put(VariableTypeDescription.TYPE_NAME_BOOL, BoolVariable.FACTORY);
        result.put(VariableTypeDescription.TYPE_NAME_STRING, StringVariable.FACTORY);

        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Integer> typeOrderMap() {
        Map<String, Integer> result = new HashMap<String, Integer>();

        result.put(VariableTypeDescription.TYPE_NAME_STRING, 0);
        result.put(VariableTypeDescription.TYPE_NAME_ENUM, 1);
        result.put(VariableTypeDescription.TYPE_NAME_BOOL, 2);

        return Collections.unmodifiableMap(result);
    }

    private static int getTypeOrder(DisplayedTaskVariable var) {
        Integer order = TYPE_ORDER.get(var.getTypeDescription().getTypeName());
        return order != null ? order.intValue() : 0;
    }

    private static List<UserVariable> toUserVariables(Collection<DisplayedTaskVariable> variables) {
        List<UserVariable> result = new ArrayList<UserVariable>(variables.size());
        for (DisplayedTaskVariable variable: variables) {
            String tpyeName = variable.getTypeDescription().getTypeName();

            UserVariableFactory variableFactory = FACTORY_MAP.get(tpyeName);
            if (variableFactory == null) {
                variableFactory = StringVariable.FACTORY;
            }

            result.add(variableFactory.createVariable(variable));
        }

        Collections.sort(result, new Comparator<UserVariable>() {
            @Override
            public int compare(UserVariable o1, UserVariable o2) {
                DisplayedTaskVariable var1 = o1.getDisplayedVariable();
                DisplayedTaskVariable var2 = o2.getDisplayedVariable();

                int typeOrder1 = getTypeOrder(var1);
                int typeOrder2 = getTypeOrder(var2);
                if (typeOrder1 != typeOrder2) {
                    return typeOrder1 < typeOrder2 ? -1 : 1;
                }

                return var1.getDisplayName().compareToIgnoreCase(var2.getDisplayName());
            }
        });

        return result;
    }

    private ParallelGroup createParallelGroup(GroupLayout layout) {
        ParallelGroup group = layout.createParallelGroup(GroupLayout.Alignment.LEADING);
        for (UserVariable variable: variablesToQuery) {
            variable.addToParallel(group);
        }
        return group;
    }

    private SequentialGroup createSequentialGroup(GroupLayout layout) {
        SequentialGroup group = layout.createSequentialGroup();
        group.addContainerGap();

        boolean first = true;
        for (UserVariable variable: variablesToQuery) {
            if (!first) {
                group.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED);
            }
            first = false;

            variable.addToSequential(group);
        }
        group.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
        return group;
    }

    private JPanel createQueryPanel() {
        JPanel panel = new JPanel();

        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(createParallelGroup(layout))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(createSequentialGroup(layout))
        );
        return panel;
    }

    public Map<DisplayedTaskVariable, String> queryVariables() {
        if (variablesToQuery.isEmpty()) {
            return Collections.emptyMap();
        }

        getContentPane().removeAll();

        JPanel queryPanel = createQueryPanel();
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = new JButton(NbStrings.getOkOption());
        buttonPanel.add(okButton);
        getRootPane().setDefaultButton(okButton);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(queryPanel, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(buttonPanel, GroupLayout.Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(queryPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(buttonPanel, GroupLayout.PREFERRED_SIZE, 33, GroupLayout.PREFERRED_SIZE))
        );

        setTitle(NbStrings.getTaskVariableQueryCaption());
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        Map<DisplayedTaskVariable, String> result = new HashMap<DisplayedTaskVariable, String>();
        for (UserVariable variable: variablesToQuery) {
            result.put(variable.getDisplayedVariable(), variable.getValue());
        }
        return result;
    }

    private static final class BoolVariable implements UserVariable {
        public static final UserVariableFactory FACTORY = new UserVariableFactory() {
            @Override
            public UserVariable createVariable(DisplayedTaskVariable variable) {
                return new BoolVariable(variable);
            }
        };

        private final DisplayedTaskVariable variable;
        private final JCheckBox checkBox;
        private final String[] possibleValues;

        public BoolVariable(DisplayedTaskVariable variable) {
            if (variable == null) throw new NullPointerException("variable");

            this.variable = variable;
            this.checkBox = new JCheckBox(variable.getDisplayName());
            this.possibleValues = parsePossibleValues(variable.getTypeDescription().getTypeArguments());
        }

        private static String[] parsePossibleValues(String valuesStr) {
            String[] values = valuesStr.split(",");
            if (values.length != 2) {
                return new String[]{Boolean.FALSE.toString(), Boolean.TRUE.toString()};
            }

            values[0] = values[0].trim();
            values[1] = values[1].trim();
            return values;
        }

        @Override
        public DisplayedTaskVariable getDisplayedVariable() {
            return variable;
        }

        @Override
        public void addToSequential(SequentialGroup group) {
            group.addComponent(checkBox);
        }

        @Override
        public void addToParallel(ParallelGroup group) {
            group.addComponent(checkBox);
        }

        @Override
        public String getValue() {
            return checkBox.isSelected()
                    ? possibleValues[1]
                    : possibleValues[0];
        }
    }

    private static final class StringVariable implements UserVariable {
        public static final UserVariableFactory FACTORY = new UserVariableFactory() {
            @Override
            public UserVariable createVariable(DisplayedTaskVariable variable) {
                return new StringVariable(variable);
            }
        };

        private final DisplayedTaskVariable variable;
        private final JLabel label;
        private final JTextField value;

        public StringVariable(DisplayedTaskVariable variable) {
            if (variable == null) throw new NullPointerException("variable");

            this.variable = variable;
            this.label = new JLabel(variable.getDisplayName());
            this.value = new JTextField(variable.getTypeDescription().getTypeArguments());
        }

        @Override
        public DisplayedTaskVariable getDisplayedVariable() {
            return variable;
        }

        @Override
        public void addToSequential(SequentialGroup group) {
            group.addComponent(label);
            group.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED);
            group.addComponent(value, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
        }

        @Override
        public void addToParallel(ParallelGroup group) {
            group.addComponent(label);
            group.addComponent(value);
        }

        @Override
        public String getValue() {
            return value.getText().trim();
        }
    }

    private interface UserVariable {
        public DisplayedTaskVariable getDisplayedVariable();
        public void addToSequential(SequentialGroup group);
        public void addToParallel(ParallelGroup group);
        public String getValue();
    }

    private interface UserVariableFactory {
        public UserVariable createVariable(DisplayedTaskVariable variable);
    }
}
