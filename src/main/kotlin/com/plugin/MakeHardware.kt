package com.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class Hardware: AnAction("Add FTC Hardware") {
    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = true
    }
    override fun actionPerformed(e: AnActionEvent) {
    }
}