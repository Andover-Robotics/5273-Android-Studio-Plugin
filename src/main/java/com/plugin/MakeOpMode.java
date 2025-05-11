package com.plugin;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;

import javax.swing.*;
import java.util.Enumeration;

public class MakeOpMode extends AnAction {
    private void addFile(
            String name,
            PsiDirectory dir,
            Project project,
            String opmode,
            String group,
            boolean teleop,
            boolean isJava,
            boolean isLinear
    ) {
        String suffix = isJava ? "java" : "kt";
        FileType fileType = FileTypeManager.getInstance().getFileTypeByExtension(suffix);
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        String text = OpModeGenerator.createOpMode(isJava, name, opmode, group, teleop, isLinear);
        dir.add(factory.createFileFromText(name + "." + suffix, fileType, text));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        OpModeDialog dialog = new OpModeDialog();
        boolean status = dialog.showAndGet();
        if (!status) return;

        String name = dialog.className.getText();
        if (name.isEmpty()) return;

        Project project = e.getProject();
        if (project == null) return;

        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (!(element instanceof PsiDirectory)) return;
        PsiDirectory dir = (PsiDirectory) element;

        boolean teleopSelected = getFirstSelected(dialog.opmodeType);
        boolean isJava = getFirstSelected(dialog.languageType);
        boolean isLinear = getFirstSelected(dialog.classType);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            addFile(
                    name,
                    dir,
                    project,
                    dialog.opmodeName.getText(),
                    dialog.groupName.getText(),
                    teleopSelected,
                    isJava,
                    isLinear
            );
        });
    }

    private boolean getFirstSelected(ButtonGroup group) {
        Enumeration<AbstractButton> buttons = group.getElements();
        while (buttons.hasMoreElements()) {
            AbstractButton button = buttons.nextElement();
            return button.isSelected();
        }
        return false;
    }
}
