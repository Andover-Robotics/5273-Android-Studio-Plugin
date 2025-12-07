package com.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.resolve.api.GroovyMethodCallReference;

import javax.swing.*;
import java.nio.file.Paths;
import java.util.*;

public class AddGradleDependency extends AnAction {
    // Index of the closure block child within a statement node (abstracted cause im sigma)
    private static final int CLOSURE_BLOCK_CHILD_INDEX = 2;

    /**
     * @param id                 unique id
     * @param displayName        shown in UI
     * @param mavenUrl           repository URL to add (nullable)
     * @param dependencyLines    lines to add to dependencies block
     * @param zipUrl             optional zip URL to import from (nullable)
     * @param zipSourcePath      path inside zip to pull files from (nullable)
     * @param zipDestinationPath destination relative to project base (nullable)
     * @param manualSteps        user-facing manual next steps for this library (nullable)
     */
    // Small DTO to hold all metadata for a library so adding new libraries is a single change.
    private record LibraryDescriptor(String id, String displayName, String mavenUrl, List<String> dependencyLines,
                                     String zipUrl, String zipSourcePath, String zipDestinationPath,
                                     String manualSteps) {
        private LibraryDescriptor(String id, String displayName,
                                  String mavenUrl, List<String> dependencyLines,
                                  String zipUrl, String zipSourcePath, String zipDestinationPath,
                                  String manualSteps) {
            this.id = id;
            this.displayName = displayName;
            this.mavenUrl = mavenUrl;
            this.dependencyLines = dependencyLines == null ? Collections.emptyList() : dependencyLines;
            this.zipUrl = zipUrl;
            this.zipSourcePath = zipSourcePath;
            this.zipDestinationPath = zipDestinationPath;
            this.manualSteps = manualSteps;
        }
    }

    // Registry of libraries, rn its just roadrunner and pedro
    private static final List<LibraryDescriptor> LIBRARY_REGISTRY = List.of(new LibraryDescriptor(
            "roadrunner",
            "Roadrunner",
            "https://maven.brott.dev/",
            Arrays.asList(
                    "implementation \"com.acmerobotics.roadrunner:ftc:0.1.25\"",
                    "implementation \"com.acmerobotics.roadrunner:core:1.0.1\"",
                    "implementation \"com.acmerobotics.roadrunner:actions:1.0.1\"",
                    "implementation \"com.acmerobotics.dashboard:dashboard:0.5.1\""
            ),
            // zip import info (optional) - keep existing quickstart sha URL
            "https://github.com/acmerobotics/road-runner-quickstart/archive/6e63a7792e9bb6958798bf46fc03d84765b50c51.zip",
            "TeamCode/src/main/java/org/firstinspires/ftc/teamcode",
            "TeamCode/src/main/java/org/firstinspires/ftc/teamcode",
            // manual steps shown to user after operation
            "1) Perform a Gradle sync (press 'Sync Now' in the blue banner).\n" +
                    "2) If the import task added any resources, verify they appear correctly in your project."
    ), new LibraryDescriptor(
            "pedro",
            "PedroPathing",
            "https://mymaven.bylazar.com/releases",
            Arrays.asList(
                    "implementation 'com.pedropathing:ftc:2.0.4'",
                    "implementation 'com.pedropathing:telemetry:1.0.0'",
                    "implementation 'com.bylazar:fullpanels:1.0.6'"
            ),
            // No zip import for Pedro in the official instructions, keep null
            null, null, null,
            """
                    1) Perform a Gradle sync (press 'Sync Now' in the blue banner).
                    2) Go to File > Project Structure > Modules and set Compile Sdk Version to 34 for FtcRobotController and TeamCode.
                    3) Press Apply and OK."""
    ));

    //builds checkboxes for each library
    private static final class RegistryDialog extends DialogWrapper {
        private final Map<String, JCheckBox> boxes = new LinkedHashMap<>();

        RegistryDialog() {
            super(true);
            setTitle("Add Libraries");
            init();
        }

        @Override
        public JPanel createCenterPanel() {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            JPanel row = new JPanel();
            row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
            for (LibraryDescriptor lib : LIBRARY_REGISTRY) {
                JCheckBox cb = new JCheckBox(lib.displayName);
                boxes.put(lib.id, cb);
                row.add(cb);
            }
            panel.add(row);
            return panel;
        }

        boolean isSelected(String id) {
            JCheckBox cb = boxes.get(id);
            return cb != null && cb.isSelected();
        }

        // expose the set of selected library ids
        Set<String> getSelectedIds() {
            Set<String> s = new LinkedHashSet<>();
            for (String id : boxes.keySet()) {
                if (isSelected(id)) s.add(id);
            }
            return s;
        }
    }

    private static PsiFile getGradleFile(Project project, String basePath) {
        String fullPath = Paths.get(basePath, "TeamCode/build.gradle").toString();

        String fileUrl = "file://" + fullPath.replace("\\", "/");

        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(fileUrl);

        if (file == null) {
            Messages.showErrorDialog(project, "File build.gradle not found at TeamCode/build.gradle", "Error");
            return null;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            Messages.showErrorDialog(project, "Failed to load the Gradle file.", "Error");
            return null;
        }
        return psiFile;
    }

    private @Nullable GroovyMethodCallReference getReference(ArrayList<GroovyMethodCallReference> refs, String name) {
        for (GroovyMethodCallReference ref : refs) {
            if (ref.getMethodName().equals(name)) return ref;
        }
        return null;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog("Project is not available.", "Error");
            return;
        }
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        PsiFile gradleFile = getGradleFile(project, basePath);
        if (gradleFile == null) {
            return;
        }

        RegistryDialog dialog = new RegistryDialog();
        boolean res = dialog.showAndGet();
        if (!res) return;

        GroovyFile file = (GroovyFile) gradleFile;
        Collection<GrMethodCallExpression> expr = PsiTreeUtil.findChildrenOfType(file, GrMethodCallExpression.class);
        ArrayList<GroovyMethodCallReference> refs = new ArrayList<>();
        for (GrMethodCallExpression state : expr) {
            GroovyMethodCallReference ref = state.getCallReference();
            if (ref != null) refs.add(ref);
        }

        GroovyPsiElementFactory fac = GroovyPsiElementFactory.getInstance(e.getProject());

        // find or create dependencies block
        GroovyMethodCallReference ref = getReference(refs, "dependencies");
        GrClosableBlock dependenciesBlock;
        GrStatement dependenciesStatement;
        if (ref == null) {
            dependenciesStatement = fac.createStatementFromText("dependencies {\n}");
            dependenciesBlock = (GrClosableBlock) dependenciesStatement.getChildren()[CLOSURE_BLOCK_CHILD_INDEX];
        } else {
            dependenciesStatement = null;
            if (((GrMethodCall) ref.getElement()).getClosureArguments().length == 0) {
                Messages.showErrorDialog(project, "The dependencies block is malformed.", "Error");
                return;
            }
            dependenciesBlock = ((GrMethodCall) ref.getElement()).getClosureArguments()[0];
        }

        // find or create repositories by maven blocks
        GroovyMethodCallReference ref2 = getReference(refs, "maven");
        GrClosableBlock mavenBlock; // we can add url statements to this maven closure
        GrStatement mavenStatement; // created if there was no maven block
        GrClosableBlock repositoriesBlock;
        GrStatement repositoriesStatement;

        if (ref2 == null) {
            GroovyMethodCallReference ref3 = getReference(refs, "repositories");
            if (ref3 == null) {
                repositoriesStatement = fac.createStatementFromText("repositories {\n}");
                repositoriesBlock = (GrClosableBlock) repositoriesStatement.getChildren()[CLOSURE_BLOCK_CHILD_INDEX];
            } else {
                repositoriesStatement = null;
                repositoriesBlock = ((GrMethodCall) ref3.getElement()).getClosureArguments()[0];
            }
            mavenStatement = fac.createStatementFromText("maven {\n}");
            mavenBlock = (GrClosableBlock) mavenStatement.getChildren()[CLOSURE_BLOCK_CHILD_INDEX];
        } else {
            repositoriesStatement = null;
            repositoriesBlock = null;
            mavenStatement = null;
            mavenBlock = ((GrMethodCall) ref2.getElement()).getClosureArguments()[0];
        }

        // schedule background imports for libraries that provide a zip import
        Set<String> selected = dialog.getSelectedIds();
        for (LibraryDescriptor lib : LIBRARY_REGISTRY) {
            if (!selected.contains(lib.id)) continue;
            if (lib.zipUrl != null && lib.zipSourcePath != null && lib.zipDestinationPath != null) {
                final String zip = lib.zipUrl;
                final String src = lib.zipSourcePath;
                final String dest = lib.zipDestinationPath;
                final String label = lib.displayName;
                new Task.Backgroundable(project, "Importing " + label) {
                    @Override
                    public void run(@NotNull ProgressIndicator progressIndicator) {
                        progressIndicator.setIndeterminate(true);
                        GithubImports.importFromZip(basePath, zip, src, dest, progressIndicator);
                    }
                }.setCancelText("Cancel Import").queue();
            }
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            // For each selected library add maven URL and dependencies
            for (LibraryDescriptor lib : LIBRARY_REGISTRY) {
                if (!selected.contains(lib.id)) continue;

                // add maven repo url if present
                if (lib.mavenUrl != null && !lib.mavenUrl.isEmpty()) {
                    mavenBlock.addStatementBefore(fac.createStatementFromText("url = '" + lib.mavenUrl + "'"), null);
                }

                // add each dependency line
                for (String dep : lib.dependencyLines) {
                    dependenciesBlock.addStatementBefore(fac.createStatementFromText(dep), null);
                }
            }

            if (dependenciesStatement != null) file.addStatementBefore(dependenciesStatement, null);
            if (mavenStatement != null) repositoriesBlock.addStatementBefore(mavenStatement, null);
            if (repositoriesStatement != null) file.addStatementBefore(repositoriesStatement, null);
        });

        // Compose per-library next steps using descriptor.manualSteps if present
        StringBuilder nextSteps = new StringBuilder();
        for (LibraryDescriptor lib : LIBRARY_REGISTRY) {
            if (!selected.contains(lib.id)) continue;
            nextSteps.append(lib.displayName).append(" - Next steps (manual):\n");
            if (lib.manualSteps != null && !lib.manualSteps.isEmpty()) {
                nextSteps.append(lib.manualSteps).append("\n\n");
            } else {
                // default generic instruction
                nextSteps.append("1) Perform a Gradle sync (press 'Sync Now' in the blue banner).\n\n");
            }
        }
        if (nextSteps.isEmpty()) {
            nextSteps.append("No libraries were selected.");
        }

        Messages.showInfoMessage(project, nextSteps.toString().trim(), "Library Import - Manual Steps");
    }
}