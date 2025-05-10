package com.plugin

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiTreeUtil
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JTextField

class HardwareOptions: DialogWrapper(true) {
    val className: ButtonGroup = ButtonGroup()
    val fieldName: JTextField = JTextField()
    val deviceName: JTextField = JTextField()
    init {
        fieldName.preferredSize = Dimension(100, 30)
        deviceName.preferredSize = Dimension(100, 30)

        super.setTitle("Add FTC Hardware")
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

    override fun createCenterPanel(): JPanel {
        val opt1 = JRadioButton("Servo")
        val opt2 = JRadioButton("DcMotor", true)
        val panel = addRadioButtons(className, opt1, opt2)

        val comp = JPanel()
        comp.layout = GridLayout(3, 2)
        comp.add(JLabel("Hardware type:"))
        comp.add(panel)
        comp.add(JLabel("Field name:"))
        comp.add(fieldName)
        comp.add(JLabel("Device name:"))
        comp.add(deviceName)
        return comp
    }
}

class Hardware: AnAction("Add FTC Hardware") {
    private fun isRealSourceClass(psiClass: PsiClass): Boolean {
        // good luck checking for a class declaration instead of a class reference
        return !psiClass.isInterface
    }
    private fun getClassBase(el: PsiElement): PsiClass? {
        var base = if (el is PsiClass) el else PsiTreeUtil.getParentOfType(el, PsiClass::class.java)
        while (base != null && !isRealSourceClass(base)) {
            base = PsiTreeUtil.getParentOfType(base, PsiClass::class.java)
        }
        return base
    }

    private fun ensureImport(project: Project, file: PsiFile, name: String) {
        val importList = (file as PsiJavaFile).importList
        if (importList == null || !importList.importStatements.any {
            it.qualifiedName == name
        }) {
            // Who invented the Java PSI API???
            val dummyFile = PsiFileFactory.getInstance(project).createFileFromText(
                "Dummy.java",
                JavaLanguage.INSTANCE,
                "import $name;class Dummy{}"
            ) as PsiJavaFile
            // if this is null I think worse things are happening
            val cls = dummyFile.importList!!.importStatements[0]
            if (importList != null) importList.add(cls)
            // No importList means it should be top level
            else file.addAfter(cls, null)
        }
    }
    private fun addHardware(proj: Project, el: PsiClass, type: Boolean, fieldName: String, deviceName: String) {
        val fac = JavaPsiFacade.getElementFactory(proj)
        val file = el.containingFile
        val typeName = if (type) "Servo" else "DcMotor"
        el.add(fac.createFieldFromText("private final $typeName $fieldName;", el))
        var isConstructed = false

        val ctor = el.methods.find { it.isConstructor } ?: run {
            isConstructed = true
            fac.createConstructor("""
            public ${el.name}() {
            }
            """.trimIndent())
        }

        val hwi = "com.qualcomm.robotcore.hardware.HardwareMap"
        val argName = ctor.parameterList.parameters.find {
            it.type.canonicalText == hwi
        } ?: run {
            val param = fac.createParameterFromText("HardwareMap hardwareMap", el)
            ensureImport(proj, file, hwi)
            ctor.parameterList.add(param)
            param
        }

        val statement = fac.createStatementFromText(
            "$fieldName = ${argName.name}.get($typeName.class, \"$deviceName\");",
            el
        )
        ctor.body?.add(statement)
        if (isConstructed) el.add(ctor)
        ensureImport(proj, file, "com.qualcomm.robotcore.hardware.$typeName")
    }
    override fun actionPerformed(e: AnActionEvent) {
        val proj = e.project ?: return
        val res = e.getData(CommonDataKeys.PSI_ELEMENT) ?: return
        val cls = getClassBase(res)
        if (cls == null) {
            Messages.showErrorDialog("No surrounding class is available", "Failed To Create FTC Hardware")
            return
        }

        val dialog = HardwareOptions()
        val result = dialog.showAndGet()
        if (!result) return

        val isServo = dialog.className.elements.nextElement().isSelected
        WriteCommandAction.runWriteCommandAction(proj) {
            addHardware(proj, cls, isServo, dialog.fieldName.text, dialog.deviceName.text)
        }
    }
}