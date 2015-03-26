package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.JavaPlatformManager;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties.standard.SourceEncodingProperty;
import org.netbeans.gradle.project.util.NbGuiUtils;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.modules.SpecificationVersion;
import org.openide.util.Lookup;

@SuppressWarnings("serial")
public class CommonProjectPropertiesPanel extends JPanel {
    private PropertyValues currentValues;
    private GradleLocation selectedGradleLocation;

    private CommonProjectPropertiesPanel() {
        currentValues = null;
        selectedGradleLocation = null;

        initComponents();

        @SuppressWarnings("unchecked")
        ComboBoxModel<Charset> sourceEncodingModel = (ComboBoxModel<Charset>)ProjectCustomizer.encodingModel(
                SourceEncodingProperty.DEFAULT_SOURCE_ENCODING.name());
        jSourceEncoding.setModel(sourceEncodingModel);

        setupEnableDisable();
    }

    public static ProfileBasedPanel createProfileBasedPanel(final NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        final CommonProjectPropertiesPanel customPanel = new CommonProjectPropertiesPanel();
        return ProfileBasedPanel.createPanel(project, customPanel, new ProfileValuesEditorFactory() {
            @Override
            public ProfileValuesEditor startEditingProfile(String displayName, ActiveSettingsQuery profileQuery) {
                PropertyValues currentValues = customPanel.new PropertyValues(project, profileQuery);
                customPanel.currentValues = currentValues;

                return currentValues;
            }
        });
    }

    private static <Value> Value setInheritAndGetValue(
            Value value,
            PropertyReference<? extends Value> valueWitFallbacks,
            JCheckBox inheritCheck) {
        inheritCheck.setSelected(value == null);
        return value != null ? value : valueWitFallbacks.getActiveValue();
    }

    private static void setupInheritCheck(final JCheckBox inheritCheck, JComponent... components) {
        NbGuiUtils.enableBasedOnCheck(inheritCheck, false, components);
    }

    private void setupEnableDisable() {
        setupInheritCheck(jScriptPlatformInherit, jScriptPlatformCombo);
        setupInheritCheck(jGradleHomeInherit, jGradleHomeEdit, jGradleHomeChangeButton);
        setupInheritCheck(jPlatformComboInherit, jPlatformCombo);
        setupInheritCheck(jSourceEncodingInherit, jSourceEncoding);
        setupInheritCheck(jSourceLevelComboInherit, jSourceLevelCombo);
    }

    private void fillProjectPlatformCombo() {
        List<ProjectPlatformComboItem> comboItems = new LinkedList<>();
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            for (ProjectPlatform platform: query.getAvailablePlatforms()) {
                comboItems.add(new ProjectPlatformComboItem(platform));
            }
        }

        jPlatformCombo.setModel(new DefaultComboBoxModel<>(comboItems.toArray(new ProjectPlatformComboItem[comboItems.size()])));
    }

    private void fillScriptPlatformCombo() {
        JavaPlatform[] platforms = JavaPlatformManager.getDefault().getInstalledPlatforms();
        List<JavaPlatformComboItem> comboItems = new LinkedList<>();

        for (JavaPlatform platform: GlobalGradleSettings.filterIndistinguishable(platforms)) {
            Specification specification = platform.getSpecification();
            if (specification != null && specification.getVersion() != null) {
                comboItems.add(new JavaPlatformComboItem(platform));
            }
        }

        jScriptPlatformCombo.setModel(new DefaultComboBoxModel<>(comboItems.toArray(new JavaPlatformComboItem[comboItems.size()])));
    }

    private final class PropertyValues implements ProfileValuesEditor {
        public NbGradleCommonProperties commonProperties;

        public GradleLocation gradleLocation;
        public JavaPlatform scriptPlatform;
        public Charset sourceEncoding;
        public ProjectPlatform targetPlatform;
        public String sourceLevel;

        public PropertyValues(NbGradleProject ownerProject, ActiveSettingsQuery settings) {
            this(new NbGradleCommonProperties(ownerProject, settings));
        }

        public PropertyValues(NbGradleCommonProperties commonProperties) {
            this.commonProperties = commonProperties;

            this.gradleLocation = commonProperties.gradleLocation().tryGetValueWithoutFallback();
            this.scriptPlatform = commonProperties.scriptPlatform().tryGetValueWithoutFallback();
            this.sourceEncoding = commonProperties.sourceEncoding().tryGetValueWithoutFallback();
            this.targetPlatform = commonProperties.targetPlatform().tryGetValueWithoutFallback();
            this.sourceLevel = commonProperties.sourceLevel().tryGetValueWithoutFallback();
        }

        public void refreshPlatformCombos() {
            displayScriptPlatform();
            displayTargetPlatform();
        }

        @Override
        public void displayValues() {
            displayGradleLocation();
            displayScriptPlatform();
            displayTargetPlatform();
            displaySourceEncoding();
            displaySourceLevel();
        }

        private Charset getSelectedSourceEncoding(Charset defaultEncoding) {
            Object selected = jSourceEncoding.getSelectedItem();
            if (selected instanceof Charset) {
                return (Charset)selected;
            }
            return defaultEncoding;
        }

        @Override
        public void readFromGui() {
            GradleLocation gradleHome = selectedGradleLocation;
            gradleLocation = jGradleHomeInherit.isSelected() ? null : gradleHome;

            JavaPlatformComboItem selectedScriptPlatform = (JavaPlatformComboItem)jScriptPlatformCombo.getSelectedItem();
            if (selectedScriptPlatform != null) {
                scriptPlatform = jScriptPlatformInherit.isSelected() ? null : selectedScriptPlatform.getPlatform();
            }

            ProjectPlatformComboItem selected = (ProjectPlatformComboItem)jPlatformCombo.getSelectedItem();
            if (selected != null) {
                targetPlatform = jPlatformComboInherit.isSelected() ? null : selected.getPlatform();
            }

            sourceEncoding = getSelectedSourceEncoding(sourceEncoding);

            sourceLevel = jSourceLevelComboInherit.isSelected() ? null : (String)jSourceLevelCombo.getSelectedItem();
        }

        @Override
        public void applyValues() {
            commonProperties.scriptPlatform().trySetValue(scriptPlatform);
            commonProperties.gradleLocation().trySetValue(gradleLocation);
            commonProperties.targetPlatform().trySetValue(targetPlatform);
            commonProperties.sourceEncoding().trySetValue(sourceEncoding);
            commonProperties.sourceLevel().trySetValue(sourceLevel);
        }

        private void displayGradleLocation() {
            GradleLocation value = setInheritAndGetValue(
                    gradleLocation,
                    commonProperties.gradleLocation(),
                    jGradleHomeInherit);

            if (value != null) {
                selectGradleLocation(value);
            }
        }

        private void displayScriptPlatform() {
            JavaPlatform value = setInheritAndGetValue(
                    scriptPlatform,
                    commonProperties.scriptPlatform(),
                    jScriptPlatformInherit);

            fillScriptPlatformCombo();
            if (value != null) {
                jScriptPlatformCombo.setSelectedItem(new JavaPlatformComboItem(value));
            }
        }

        private void displayTargetPlatform() {
            ProjectPlatform value = setInheritAndGetValue(
                    targetPlatform,
                    commonProperties.targetPlatform(),
                    jPlatformComboInherit);
            fillProjectPlatformCombo();
            if (value != null) {
                jPlatformCombo.setSelectedItem(new ProjectPlatformComboItem(value));
            }
        }

        private void displaySourceEncoding() {
            Charset value = setInheritAndGetValue(
                    sourceEncoding,
                    commonProperties.sourceEncoding(),
                    jSourceEncodingInherit);
            if (value != null) {
                jSourceEncoding.setSelectedItem(value);
            }
        }

        private void displaySourceLevel() {
            String value = setInheritAndGetValue(
                    sourceLevel,
                    commonProperties.sourceLevel(),
                    jSourceLevelComboInherit);
            if (value != null) {
                jSourceLevelCombo.setSelectedItem(value);
            }
        }
    }

    private void selectGradleLocation(GradleLocation newLocation) {
        selectedGradleLocation = newLocation;
        jGradleHomeEdit.setText(newLocation != null ? newLocation.toLocalizedString() : "");
    }

    private static class JavaPlatformComboItem {
        private final JavaPlatform platform;

        public JavaPlatformComboItem(JavaPlatform platform) {
            ExceptionHelper.checkNotNullArgument(platform, "platform");
            this.platform = platform;
        }

        public JavaPlatform getPlatform() {
            return platform;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + (this.platform.getSpecification().getVersion().hashCode());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final JavaPlatformComboItem other = (JavaPlatformComboItem)obj;
            SpecificationVersion thisVersion = this.platform.getSpecification().getVersion();
            SpecificationVersion otherVersion = other.platform.getSpecification().getVersion();
            return thisVersion.equals(otherVersion);
        }

        @Override
        public String toString() {
            return platform.getDisplayName();
        }
    }

    private static class ProjectPlatformComboItem {
        private final ProjectPlatform platform;

        public ProjectPlatformComboItem(ProjectPlatform platform) {
            ExceptionHelper.checkNotNullArgument(platform, "platform");
            this.platform = platform;
        }

        public ProjectPlatform getPlatform() {
            return platform;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 41 * hash + Objects.hashCode(platform.getName());
            hash = 41 * hash + Objects.hashCode(platform.getVersion());
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final ProjectPlatformComboItem other = (ProjectPlatformComboItem)obj;
            return Objects.equals(this.platform.getName(), other.platform.getName())
                    && Objects.equals(this.platform.getVersion(), other.platform.getVersion());
        }

        @Override
        public String toString() {
            return platform.getDisplayName();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings({"unchecked", "Convert2Diamond"})
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSourceLevelCombo = new javax.swing.JComboBox<String>();
        jGradleHomeEdit = new javax.swing.JTextField();
        jGradleHomeInherit = new javax.swing.JCheckBox();
        jSourceEncodingInherit = new javax.swing.JCheckBox();
        jPlatformComboInherit = new javax.swing.JCheckBox();
        jScriptPlatformCombo = new javax.swing.JComboBox<JavaPlatformComboItem>();
        jSourceLevelComboInherit = new javax.swing.JCheckBox();
        jScriptPlatformInherit = new javax.swing.JCheckBox();
        jPlatformPreferenceButton = new javax.swing.JButton();
        jPlatformCombo = new javax.swing.JComboBox<ProjectPlatformComboItem>();
        jGradleHomeCaption = new javax.swing.JLabel();
        jSourceEncodingCaption = new javax.swing.JLabel();
        jTargetPlatformCaption = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jSourceLevelCaption = new javax.swing.JLabel();
        jGradleHomeChangeButton = new javax.swing.JButton();
        jSourceEncoding = new javax.swing.JComboBox<Charset>();

        jSourceLevelCombo.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1.3", "1.4", "1.5", "1.6", "1.7", "1.8" }));

        jGradleHomeEdit.setEditable(false);
        jGradleHomeEdit.setText(org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jGradleHomeEdit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jGradleHomeInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jGradleHomeInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceEncodingInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jSourceEncodingInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jPlatformComboInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jPlatformComboInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceLevelComboInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jSourceLevelComboInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jScriptPlatformInherit, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jScriptPlatformInherit.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jPlatformPreferenceButton, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jPlatformPreferenceButton.text")); // NOI18N
        jPlatformPreferenceButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPlatformPreferenceButtonActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(jGradleHomeCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jGradleHomeCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceEncodingCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jSourceEncodingCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jTargetPlatformCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jTargetPlatformCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jLabel1.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jSourceLevelCaption, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jSourceLevelCaption.text")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jGradleHomeChangeButton, org.openide.util.NbBundle.getMessage(CommonProjectPropertiesPanel.class, "CommonProjectPropertiesPanel.jGradleHomeChangeButton.text")); // NOI18N
        jGradleHomeChangeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jGradleHomeChangeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jScriptPlatformCombo, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSourceLevelCombo, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPlatformCombo, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jGradleHomeChangeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jGradleHomeEdit))
                            .addComponent(jSourceEncoding, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(6, 6, 6)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jSourceEncodingInherit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jGradleHomeInherit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScriptPlatformInherit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPlatformComboInherit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSourceLevelComboInherit, javax.swing.GroupLayout.Alignment.TRAILING)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jPlatformPreferenceButton))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSourceEncodingCaption)
                            .addComponent(jGradleHomeCaption)
                            .addComponent(jTargetPlatformCaption)
                            .addComponent(jLabel1)
                            .addComponent(jSourceLevelCaption))
                        .addGap(0, 363, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSourceEncodingCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourceEncodingInherit)
                    .addComponent(jSourceEncoding, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jGradleHomeCaption, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jGradleHomeEdit, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jGradleHomeInherit)
                    .addComponent(jGradleHomeChangeButton))
                .addGap(12, 12, 12)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jScriptPlatformCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScriptPlatformInherit))
                .addGap(12, 12, 12)
                .addComponent(jTargetPlatformCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jPlatformCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPlatformComboInherit))
                .addGap(12, 12, 12)
                .addComponent(jSourceLevelCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSourceLevelCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSourceLevelComboInherit))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPlatformPreferenceButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jPlatformPreferenceButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPlatformPreferenceButtonActionPerformed
        if (PlatformPriorityPanel.showDialog(this)) {
            if (currentValues != null) {
                currentValues.refreshPlatformCombos();
            }
        }
    }//GEN-LAST:event_jPlatformPreferenceButtonActionPerformed

    private void jGradleHomeChangeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jGradleHomeChangeButtonActionPerformed
        if (currentValues == null) {
            return;
        }

        GradleLocation currentLocation = selectedGradleLocation != null
                ?  selectedGradleLocation
                : currentValues.commonProperties.gradleLocation().getActiveValue();
        GradleLocation newLocation = GradleLocationPanel.tryChooseLocation(this, currentLocation);
        if (newLocation != null) {
            selectGradleLocation(newLocation);
        }
    }//GEN-LAST:event_jGradleHomeChangeButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jGradleHomeCaption;
    private javax.swing.JButton jGradleHomeChangeButton;
    private javax.swing.JTextField jGradleHomeEdit;
    private javax.swing.JCheckBox jGradleHomeInherit;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JComboBox<ProjectPlatformComboItem> jPlatformCombo;
    private javax.swing.JCheckBox jPlatformComboInherit;
    private javax.swing.JButton jPlatformPreferenceButton;
    private javax.swing.JComboBox<JavaPlatformComboItem> jScriptPlatformCombo;
    private javax.swing.JCheckBox jScriptPlatformInherit;
    private javax.swing.JComboBox<Charset> jSourceEncoding;
    private javax.swing.JLabel jSourceEncodingCaption;
    private javax.swing.JCheckBox jSourceEncodingInherit;
    private javax.swing.JLabel jSourceLevelCaption;
    private javax.swing.JComboBox<String> jSourceLevelCombo;
    private javax.swing.JCheckBox jSourceLevelComboInherit;
    private javax.swing.JLabel jTargetPlatformCaption;
    // End of variables declaration//GEN-END:variables
}
