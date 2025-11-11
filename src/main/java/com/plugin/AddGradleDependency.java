package com.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
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
    private static PsiFile getGradleFile(Project project) {
        String path = project.getBasePath();
        if (path == null) return null;
        String fullPath = Paths.get(path, "TeamCode/build.gradle").toString();

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
        PsiFile gradleFile = getGradleFile(project);
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
            block = (GrClosableBlock) statement.getChildren()[2];
        } else {
            statement = null;
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
                block3 = (GrClosableBlock) statement3.getChildren()[2];
            } else {
                statement3 = null;
                block3 = ((GrMethodCall) ref3.getElement()).getClosureArguments()[0];
            }
            statement2 = fac.createStatementFromText("maven {\n}");
            block2 = (GrClosableBlock) statement2.getChildren()[2];
        } else {
            statement3 = null;
            block3 = null;
            statement2 = null;
            block2 = ((GrMethodCall) ref2.getElement()).getClosureArguments()[0];
        }

        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (dialog.roadrunner.isSelected()) {
                block2.addStatementBefore(fac.createStatementFromText("url = 'https://maven.brott.dev/'"), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.roadrunner:ftc:0.1.25\""), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.roadrunner:core:1.0.1\""), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.roadrunner:actions:1.0.1\""), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.dashboard:dashboard:0.5.1\""), null);
            }
            if (dialog.pedro.isSelected()) {
                block2.addStatementBefore(fac.createStatementFromText("url = 'https://maven.pedropathing.com/'"), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.pedropathing:pedro:1.0.9\""), null);
            }
            if (statement != null) file.addStatementBefore(statement, null);
            if (statement2 != null) block3.addStatementBefore(statement2, null);
            if (statement3 != null) file.addStatementBefore(statement3, null);
        });
    }
}