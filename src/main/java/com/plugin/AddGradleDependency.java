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
import java.util.ArrayList;
import java.util.Collection;

class Dialog extends DialogWrapper {
    final JCheckBox roadrunner = new JCheckBox("Roadrunner");
    final JCheckBox pedro = new JCheckBox("Pedro");
    Dialog() {
        super(true);
        setTitle("Add Libraries");
        init();
    }
    @Override
    public JPanel createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JPanel panel1 = new JPanel();
        panel1.add(roadrunner);
        panel1.add(pedro);
        panel.add(panel1);
        return panel;
    }
}

public class AddGradleDependency extends AnAction {
    // Index of the closure block child within a statement node (abstracted to a constant for clarity)
    private static final int CLOSURE_BLOCK_CHILD_INDEX = 2;

    // Pedro constants (existing)
    private static final String PEDRO_MAVEN_URL = "https://mymaven.bylazar.com/releases";
    private static final String PEDRO_FTC_DEP = "implementation 'com.pedropathing:ftc:2.0.4'";
    private static final String PEDRO_TELEMETRY_DEP = "implementation 'com.pedropathing:telemetry:1.0.0'";
    private static final String FULLPANELS_DEP = "implementation 'com.bylazar:fullpanels:1.0.6'";

    // Roadrunner constants (new) - keep dependency strings exactly as they should appear in the Gradle file
    private static final String ROADRUNNER_MAVEN_URL = "https://maven.brott.dev/";
    private static final String ROADRUNNER_FTC_DEP = "implementation \"com.acmerobotics.roadrunner:ftc:0.1.25\"";
    private static final String ROADRUNNER_CORE_DEP = "implementation \"com.acmerobotics.roadrunner:core:1.0.1\"";
    private static final String ROADRUNNER_ACTIONS_DEP = "implementation \"com.acmerobotics.roadrunner:actions:1.0.1\"";
    private static final String DASHBOARD_DEP = "implementation \"com.acmerobotics.dashboard:dashboard:0.5.1\"";

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

        Dialog dialog = new Dialog();
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

        GroovyMethodCallReference ref = getReference(refs, "dependencies");
        GrClosableBlock block;
        GrStatement statement;
        if (ref == null) {
            statement = fac.createStatementFromText("dependencies {\n}");
            block = (GrClosableBlock) statement.getChildren()[CLOSURE_BLOCK_CHILD_INDEX];
        } else {
            statement = null;
            if(((GrMethodCall)ref.getElement()).getClosureArguments().length == 0) {
                Messages.showErrorDialog(project, "The dependencies block is malformed.", "Error");
                return;
            }
            block = ((GrMethodCall) ref.getElement()).getClosureArguments()[0];
        }

        GroovyMethodCallReference ref2 = getReference(refs, "maven");
        GrClosableBlock block2; // maven block
        GrStatement statement2; // maven statement
        GrClosableBlock block3; // repositories block
        GrStatement statement3; // repositories statement

        if (ref2 == null) {
            GroovyMethodCallReference ref3 = getReference(refs, "repositories");
            if (ref3 == null) {
                statement3 = fac.createStatementFromText("repositories {\n}");
                block3 = (GrClosableBlock) statement3.getChildren()[CLOSURE_BLOCK_CHILD_INDEX];
            } else {
                statement3 = null;
                block3 = ((GrMethodCall) ref3.getElement()).getClosureArguments()[0];
            }
            statement2 = fac.createStatementFromText("maven {\n}");
            block2 = (GrClosableBlock) statement2.getChildren()[CLOSURE_BLOCK_CHILD_INDEX];
        } else {
            statement3 = null;
            block3 = null;
            statement2 = null;
            block2 = ((GrMethodCall) ref2.getElement()).getClosureArguments()[0];
        }

        if (dialog.roadrunner.isSelected()) {
            // TODO use Kotlin so that way coroutines can be used instead of "Obsolete API"
            new Task.Backgroundable(project, "Importing roadrunner") {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
                    progressIndicator.setIndeterminate(true);
                    GithubImports.importRoadrunner(basePath, progressIndicator);
                }
            }.setCancelText("Cancel Import").queue();
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (dialog.roadrunner.isSelected()) {
                // Set the Roadrunner maven url and add its dependencies using well-named constants
                block2.addStatementBefore(fac.createStatementFromText("url = '" + ROADRUNNER_MAVEN_URL + "'"), null);
                block.addStatementBefore(fac.createStatementFromText(ROADRUNNER_FTC_DEP), null);
                block.addStatementBefore(fac.createStatementFromText(ROADRUNNER_CORE_DEP), null);
                block.addStatementBefore(fac.createStatementFromText(ROADRUNNER_ACTIONS_DEP), null);
                block.addStatementBefore(fac.createStatementFromText(DASHBOARD_DEP), null);
            }
            if (dialog.pedro.isSelected()) {
                // Use the official Pedro/Bylazar maven URL and add the recommended dependencies.
                block2.addStatementBefore(fac.createStatementFromText("url = '" + PEDRO_MAVEN_URL + "'"), null);

                // Add the three dependencies recommended by Pedro's official instructions.
                block.addStatementBefore(fac.createStatementFromText(PEDRO_FTC_DEP), null);
                block.addStatementBefore(fac.createStatementFromText(PEDRO_TELEMETRY_DEP), null);
                block.addStatementBefore(fac.createStatementFromText(FULLPANELS_DEP), null);
            }
            if (statement != null) file.addStatementBefore(statement, null);
            if (statement2 != null) block3.addStatementBefore(statement2, null);
            if (statement3 != null) file.addStatementBefore(statement3, null);
        });

        // Build separate next-steps for Roadrunner and Pedro so each has clear, distinct instructions.
        StringBuilder nextSteps = new StringBuilder();
        if (dialog.roadrunner.isSelected()) {
            nextSteps.append("Roadrunner - Next steps (manual):\n")
                    .append("1) Perform a Gradle sync (press 'Sync Now' in the blue banner).\n")
                    .append("2) If the import task added any resources, verify they appear correctly in your project.\n\n");
        }
        if (dialog.pedro.isSelected()) {
            nextSteps.append("Pedro - Next steps (manual):\n")
                    .append("1) Perform a Gradle sync (press 'Sync Now' in the blue banner).\n")
                    .append("2) Go to File > Project Structure > Modules and set Compile Sdk Version to 34 for FtcRobotController and TeamCode.\n")
                    .append("3) Press Apply and OK.\n");
        }
        if (nextSteps.length() == 0) {
            nextSteps.append("No libraries were selected.");
        }

        Messages.showInfoMessage(project, nextSteps.toString(), "Library Import - Manual Steps");
    }
}