package com.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import java.nio.file.Paths;

public class AddGradleDependency extends AnAction {
    private static PsiFile getGradleFile(Project project) {

        String fullPath = Paths.get(project.getBasePath(),"TeamCode/build.gradle").toString();

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

    // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
    private final String LOOKUP = "dependencies {";

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

        String text = gradleFile.getText();
        int index = text.indexOf(LOOKUP);
        if (index == -1) return;

        String dep = Messages.showInputDialog("Dependency name:", "Add Gradle Dependency", null);
        int realIndex = index + LOOKUP.length();
        text = text.substring(0, realIndex) + "\n\timplementation \"" + dep + "\"" + text.substring(realIndex);
        replacePsiFile(e.getProject(), gradleFile, text);
    }
}
