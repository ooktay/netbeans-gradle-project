package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ProxyListenerRegistry;
import org.jtrim.event.SimpleListenerRegistry;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.event.NbListenerManagers;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.query.GradleFilesClassPathProvider;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.RefreshableChildren;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.spi.project.ui.PathFinder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class GradleHomeNode extends AbstractNode {
    private static final String INIT_GRADLE_NAME = "init.gradle";
    private static final String INIT_D_NAME = "init.d";

    private final GradleHomeNodeChildFactory childFactory;

    public GradleHomeNode() {
        this(new GradleHomeNodeChildFactory());
    }

    private GradleHomeNode(GradleHomeNodeChildFactory childFactory) {
        this(childFactory, Children.create(childFactory, true));
    }

    private GradleHomeNode(GradleHomeNodeChildFactory childFactory, Children children) {
        super(children, createLookup(childFactory, children));

        this.childFactory = childFactory;

        setName(getClass().getSimpleName());
    }

    private static Lookup createLookup(GradleHomeNodeChildFactory childFactory, Children children) {
        return Lookups.fixed(
                GradleHomePathFinder.INSTANCE,
                NodeUtils.defaultNodeRefresher(children, childFactory));
    }

    public static SingleNodeFactory getFactory() {
        return FactoryImpl.INSTANCE;
    }

    private Action openGradleHomeFile(String name) {
        File userHome = getGradleUserHome();
        File file = userHome != null ? new File(userHome, name) : new File(name);

        Action result = new OpenAlwaysFileAction(file.toPath());
        if (userHome == null) {
            result.setEnabled(false);
        }

        return result;
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> result = new ArrayList<>();

        result.add(openGradleHomeFile(INIT_GRADLE_NAME));
        result.add(openGradleHomeFile(SettingsFiles.GRADLE_PROPERTIES_NAME));
        if (!childFactory.hasInitDDirDisplayed) {
            result.add(new CreateInitDAction());
        }
        result.add(null);
        result.add(NodeUtils.getRefreshNodeAction(this));

        return result.toArray(new Action[result.size()]);
    }

    @Override
    public Image getIcon(int type) {
        return NbIcons.getFolderIcon();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return NbIcons.getOpenFolderIcon();
    }

    @Override
    public String getDisplayName() {
        return NbStrings.getGradleHomeNodeCaption();
    }

    private static File getGradleUserHome() {
        return GradleFileUtils.GRADLE_USER_HOME.getValue();
    }

    private enum GradleHomePathFinder implements PathFinder {
        INSTANCE;

        @Override
        public Node findPath(Node root, Object target) {
            FileObject targetFile = NodeUtils.tryGetFileSearchTarget(target);
            if (targetFile == null) {
                return null;
            }

            boolean canBeFound =
                SettingsFiles.GRADLE_PROPERTIES_NAME.equalsIgnoreCase(targetFile.getNameExt())
                || SettingsFiles.DEFAULT_GRADLE_EXTENSION_WITHOUT_DOT.equalsIgnoreCase(targetFile.getExt());
            // We have only gradle files and the gradle.properties.
            if (!canBeFound) {
                return null;
            }

            File userHome = getGradleUserHome();
            if (userHome == null) {
                // Most likely we could not create the nodes, so
                // don't bother looking at subnodes.
                return null;
            }

            FileObject userHomeObj = FileUtil.toFileObject(userHome);
            if (userHomeObj == null) {
                // The directory does not exist, so there should be
                // no valid node.
                return null;
            }

            if (!FileUtil.isParentOf(userHomeObj, targetFile)) {
                return null;
            }

            Node result = NodeUtils.findFileChildNode(root.getChildren(), targetFile);
            if (result != null) {
                return result;
            }

            return NodeUtils.askChildrenForTarget(root.getChildren(), target);
        }
    }

    private static class GradleHomeNodeChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory>
    implements
            RefreshableChildren {
        private final ListenerRegistrations listenerRefs;
        private final ProxyListenerRegistry<Runnable> userHomeChangeListeners;
        private volatile boolean hasInitDDirDisplayed;
        private volatile boolean createdOnce;

        public GradleHomeNodeChildFactory() {
            this.listenerRefs = new ListenerRegistrations();
            this.userHomeChangeListeners = new ProxyListenerRegistry<>(NbListenerManagers.neverNotifingRegistry());
            this.hasInitDDirDisplayed = false;
            this.createdOnce = false;
        }

        @Override
        public void refreshChildren() {
            if (createdOnce) {
                refresh(false);
            }
        }

        private FileObject tryGetUserHomeObj() {
            File userHome = GradleFileUtils.GRADLE_USER_HOME.getValue();
            if (userHome == null) {
                return null;
            }

            return FileUtil.toFileObject(userHome);
        }

        private void updateUserHome() {
            final FileObject userHome = tryGetUserHomeObj();
            if (userHome == null) {
                userHomeChangeListeners.replaceRegistry(NbListenerManagers.neverNotifingRegistry());
            }
            else {
                userHomeChangeListeners.replaceRegistry(new SimpleListenerRegistry<Runnable>() {
                    @Override
                    public ListenerRef registerListener(Runnable listener) {
                        return NbFileUtils.addDirectoryContentListener(userHome, true, listener);
                    }
                });
            }
            userHomeChangeListeners.onEvent(EventListeners.runnableDispatcher(), null);
        }

        @Override
        protected void addNotify() {
            listenerRefs.add(GradleFileUtils.GRADLE_USER_HOME.addChangeListener(new Runnable() {
                @Override
                public void run() {
                    updateUserHome();
                }
            }));
            updateUserHome();
            listenerRefs.add(userHomeChangeListeners.registerListener(new Runnable() {
                @Override
                public void run() {
                    refresh(false);
                }
            }));
        }

        @Override
        protected void removeNotify() {
            listenerRefs.unregisterAll();
        }

        private static FileObject tryGetFile(File dir, String name) {
            return FileUtil.toFileObject(new File(dir, name));
        }

        private void addGradleProperties(File userHome, List<SingleNodeFactory> toPopulate) {
            FileObject gradleProperties = tryGetFile(userHome, SettingsFiles.GRADLE_PROPERTIES_NAME);
            if (gradleProperties != null) {
                SingleNodeFactory node = NodeUtils.tryGetFileNode(gradleProperties);
                if (node != null) {
                    toPopulate.add(node);
                }
            }
        }

        private void addInitGradle(File userHome, List<SingleNodeFactory> toPopulate) {
            FileObject initGradle = tryGetFile(userHome, INIT_GRADLE_NAME);
            if (initGradle != null) {
                SingleNodeFactory node = NodeUtils.tryGetFileNode(
                        initGradle,
                        initGradle.getNameExt(),
                        NbIcons.getGradleIcon());
                if (node != null) {
                    toPopulate.add(node);
                }
            }
        }

        private void addInitDDir(File userHome, List<SingleNodeFactory> toPopulate) {
            final FileObject initD = tryGetFile(userHome, INIT_D_NAME);

            if (initD != null && initD.isFolder()) {
                hasInitDDirDisplayed = true;
                toPopulate.add(GradleFolderNode.getFactory(
                        NbStrings.getGlobalInitScriptsNodeCaption(),
                        initD));
            }
        }

        private void addOtherGradleFiles(File userHome, List<SingleNodeFactory> toPopulate) {
            FileObject userHomeObj = FileUtil.toFileObject(userHome);
            if (userHomeObj == null) {
                return;
            }

            FileObject initD = userHomeObj.getFileObject(INIT_GRADLE_NAME);

            List<FileObject> gradleFiles = new LinkedList<>();
            for (FileObject file: userHomeObj.getChildren()) {
                if (file.equals(initD)) {
                    continue;
                }

                if (GradleFilesClassPathProvider.isGradleFile(file)) {
                    gradleFiles.add(file);
                }
            }

            Collections.sort(gradleFiles, new Comparator<FileObject>() {
                @Override
                public int compare(FileObject o1, FileObject o2) {
                    return StringUtils.STR_CMP.compare(o1.getNameExt(), o2.getNameExt());
                }
            });

            for (FileObject file: gradleFiles) {
                SingleNodeFactory node = NodeUtils.tryGetFileNode(
                        file,
                        file.getNameExt(),
                        NbIcons.getGradleIcon());
                if (node != null) {
                    toPopulate.add(node);
                }
            }
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            hasInitDDirDisplayed = false;

            File userHome = getGradleUserHome();
            if (userHome == null) {
                return;
            }

            addGradleProperties(userHome, toPopulate);
            addInitGradle(userHome, toPopulate);
            addInitDDir(userHome, toPopulate);

            addOtherGradleFiles(userHome, toPopulate);
        }

        @Override
        protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
            createdOnce = true;
            readKeys(toPopulate);
            return true;
        }

        @Override
        protected Node createNodeForKey(SingleNodeFactory key) {
            return key.createNode();
        }
    }

    private enum FactoryImpl implements SingleNodeFactory {
        INSTANCE;

        @Override
        public Node createNode() {
            return new GradleHomeNode();
        }
    }

    @SuppressWarnings("serial")
    private static class CreateInitDAction extends AbstractAction {
        public CreateInitDAction() {
            super(NbStrings.getCreateInitDDir());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NbTaskExecutors.DEFAULT_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    File userHome = getGradleUserHome();
                    if (userHome != null) {
                        Path initDPath = userHome.toPath().resolve(INIT_D_NAME);
                        Files.createDirectories(initDPath);
                    }
                }
            }, null);
        }
    }
}
