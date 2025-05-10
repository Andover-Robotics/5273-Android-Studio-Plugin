package com.plugin
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileFactory
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField

class OpModeDialog() : DialogWrapper(true) {
    val className: JTextField = JTextField()
    val opmodeName: JTextField = JTextField()
    val groupName: JTextField = JTextField()
    val opmodeType: ButtonGroup = ButtonGroup()
    val languageType: ButtonGroup = ButtonGroup()
    val classType: ButtonGroup = ButtonGroup()

    init {
        className.preferredSize = Dimension(100, 30)
        opmodeName.preferredSize = Dimension(100, 30)
        groupName.preferredSize = Dimension(100, 30)

        super.setTitle("Add FTC OpMode")
        init()
    }

    private fun addRadioButtons(group: ButtonGroup, vararg radio: JRadioButton): JPanel {
        val panel = JPanel()
        radio.forEach {
            group.add(it)
            panel.add(it)
        }
        return panel
    }

    override fun createCenterPanel(): JComponent {
        val opt1 = JRadioButton("Teleop", true)
        val opt2 = JRadioButton("Auto")
        val groupPanel = addRadioButtons(opmodeType, opt1, opt2)

        val optl1 = JRadioButton("Java", true)
        val optl2 = JRadioButton("Kotlin")
        val groupPanel2 = addRadioButtons(languageType, optl1, optl2)

        val optc1 = JRadioButton("LinearOpMode", true)
        val optc2 = JRadioButton("OpMode")
        val groupPanel3 = addRadioButtons(classType, optc1, optc2)

        val comp = JPanel()
        comp.layout = GridLayout(6, 2)
        comp.add(JLabel("Class name:"))
        comp.add(className)
        comp.add(JLabel("Opmode name (optional):"))
        comp.add(opmodeName)
        comp.add(JLabel("Opmode group name (optional):"))
        comp.add(groupName)
        comp.add(JLabel("Opmode type:"))
        comp.add(groupPanel)
        comp.add(JLabel("Language:"))
        comp.add(groupPanel2)
        comp.add(JLabel("Base class:"))
        comp.add(groupPanel3)
        return comp
    }
}

class MakeOpMode: AnAction("Add FTC OpMode") {
    private fun addFile(
        name: String,
        dir: PsiDirectory,
        project: Project,
        opmode: String,
        group: String,
        teleop: Boolean,
        isJava: Boolean,
        isLinear: Boolean
    ) {
        val ext = FileTypeManager.getInstance().getFileTypeByExtension("java")
        val factory = PsiFileFactory.getInstance(project)
        val text = createOpMode(isJava, name, opmode, group, teleop, isLinear)
        val suffix = if (isJava) "java" else "kt"
        dir.add(factory.createFileFromText("$name.$suffix", ext, text))
    }

    override fun actionPerformed(e: AnActionEvent) {
        val dialog = OpModeDialog()
        val status = dialog.showAndGet()
        if (!status) return

        val name = dialog.className.text
        if (name.isEmpty()) return

        val proj = e.project ?: return

        val dir = e.getData(CommonDataKeys.PSI_ELEMENT) ?: return
        if (dir !is PsiDirectory) return

        // Teleop is the first button
        val teleopSelected = dialog.opmodeType.elements.nextElement().isSelected
        val isJava = dialog.languageType.elements.nextElement().isSelected
        val isLinear = dialog.classType.elements.nextElement().isSelected
        WriteCommandAction.runWriteCommandAction(proj, {
            addFile(
                name,
                dir,
                proj,
                dialog.opmodeName.text,
                dialog.groupName.text,
                teleopSelected,
                isJava,
                isLinear
            )
        })
    }
}