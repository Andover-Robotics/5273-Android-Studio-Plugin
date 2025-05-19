package com.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameterList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.resolve.api.Argument;
import org.jetbrains.plugins.groovy.lang.resolve.api.GroovyMethodCallReference;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

class Dialog extends DialogWrapper {
    final ButtonGroup libraryChoice;
    Dialog() {
        super(true);
        libraryChoice = new ButtonGroup();
        setTitle("Choose Library");
        init();
    }

    private JPanel addRadioButtons(ButtonGroup group, JRadioButton... buttons) {
        JPanel panel = new JPanel();
        for (JRadioButton button : buttons) {
            group.add(button);
            panel.add(button);
        }
        return panel;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        JRadioButton button1 = new JRadioButton("Roadrunner");
        JRadioButton button2 = new JRadioButton("Pedro Pathing");
        JPanel panel2 = addRadioButtons(libraryChoice, button1, button2);
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Library choice:"));
        panel.add(panel2);
        return panel;
    }
}

public class AddGradleDependency extends AnAction {
    private static PsiFile getGradleFile(Project project) {

        String fullPath = Paths.get(project.getBasePath(), "TeamCode/build.gradle").toString();

        String fileUrl = "file://" + fullPath.replace("\\", "/");

        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(fileUrl);

        if (file == null) {
            Messages.showErrorDialog(project, "build.gradle not found at TeamCode/build.gradle", "Error");
            return null;
        }

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            Messages.showErrorDialog(project, "Failed to load the Gradle file.", "Error");
            return null;
        }
        return psiFile;
    }

    private static void replacePsiFile(Project project, PsiFile file, String text) {
        Document document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
        if (document != null) {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                document.setText(text);
            });
        }
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
        GrStatement statement;
        if (ref == null) {
            // Need to figure out how to extract a GroovyMethodCallReference from this one
            statement = fac.createStatementFromText("dependencies {\n\n}");
            ref = PsiTreeUtil.findChildrenOfType(statement, GrMethodCallExpression.class).iterator().next().getCallReference();
        } else {
            statement = null;
        }

        GroovyMethodCallReference ref2 = getReference(refs, "maven");
        GrStatement statement2 = null;

        // Same problem here
        /*if (ref2 == null) {
            GroovyMethodCallReference ref3 = getReference(refs, "repositories");
            if (ref3 == null) {
                // uhhh
            }
            statement2 = fac.createStatementFromText("maven {\n\n}");
            ref2 = PsiTreeUtil.findChildrenOfType(statement, GrMethodCallExpression.class).iterator().next().getCallReference();
        }*/

        boolean selected = dialog.libraryChoice.getElements().nextElement().isSelected();
        GrClosableBlock block = ((GrMethodCall) ref.getElement()).getClosureArguments()[0];
        GrClosableBlock block2 = ((GrMethodCall) ref2.getElement()).getClosureArguments()[0];

        WriteCommandAction.runWriteCommandAction(project, () -> {
            if (selected) {
                block2.addStatementBefore(fac.createStatementFromText("url = 'https://maven.brott.dev/'"), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.roadrunner:ftc:0.1.22\""), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.roadrunner:core:1.0.1\""), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.roadrunner:actions:1.0.1\""), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.dashboard:dashboard:0.4.16\""), null);
            } else {
                block2.addStatementBefore(fac.createStatementFromText("url = 'https://maven.brott.dev/'"), null);
                block2.addStatementBefore(fac.createStatementFromText("url = 'https://maven.pedropathing.com/'"), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.pedropathing:pedro:1.0.9\""), null);
                block.addStatementBefore(fac.createStatementFromText("implementation \"com.acmerobotics.dashboard:dashboard:0.4.16\""), null);
            }
        });
    }
}