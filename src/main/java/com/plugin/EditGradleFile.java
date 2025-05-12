package com.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiManager;

import javax.swing.*;
import java.util.Enumeration;
import java.util.Objects;

public class EditGradleFile extends AnAction {
    private static PsiFile getGradleFile(Project project) {
        VirtualFile baseDir = project.getWorkspaceFile();
        if (baseDir == null) {
            Messages.showErrorDialog(project, "Project workspace file not found.", "Error");
            return null;
        }

        VirtualFile file = baseDir.findFileByRelativePath("TeamCode/build.gradle");
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

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            Messages.showErrorDialog("Project is not available.", "Error");
            return;
        }

        Messages.showInfoMessage(project.getBasePath(),"path");
        Messages.showInfoMessage();

        PsiFile gradleFile = getGradleFile(project);
        if (gradleFile == null) {
            return;
        }

        String gradleContents = gradleFile.getText();
        if (gradleContents.isEmpty()) {
            Messages.showErrorDialog(project, "Gradle file is empty.", "Error");
            return;
        }

        // Debug: Log contents to the console
        System.out.println(gradleContents);

        Messages.showInfoMessage(gradleContents, "Gradle Contents:");
    }
}
