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
import com.intellij.ui.JBColor;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

class Library {
    public String library;
    public String version;

    public Library() {
        this.library = "";
        this.version = "";
    }
    public Library(String library, String version) {
        this.library = library;
        this.version = version;
    }
}

class Dialog extends DialogWrapper {
    public ArrayList<Library> libraries = new ArrayList<>();
    private final JPanel dropdown = new JPanel();
    private final JPanel panel = new JPanel();
    Dialog() {
        super(true);
        setTitle("Add Dependencies");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JPanel panel1 = new JPanel();
        JButton btn = new JButton("Roadrunner");
        btn.addActionListener(e -> {
            libraries.clear();
            libraries.add(new Library("com.acmerobotics.roadrunner:ftc", "0.1.22"));
            libraries.add(new Library("com.acmerobotics.roadrunner:core", "1.0.1"));
            update();
        });
        JButton btn2 = new JButton("Pedro Pathing");
        btn2.addActionListener(e -> {
            libraries.clear();
            libraries.add(new Library("com.pedropathing:pedro", "1.0.9"));
            update();
        });
        panel1.add(new JLabel("Quickstart:"));
        panel1.add(btn);
        panel1.add(btn2);
        panel.add(panel1);
        JLabel lbl = new JLabel("The following libraries will be added:");
        // This is needed to center things
        // todo: why????
        JPanel fake = new JPanel();
        fake.add(lbl);
        panel.add(fake);
        panel.add(dropdown);
        JButton btna = new JButton("Add another library");
        btna.addActionListener(e -> {
            libraries.add(new Library());
            update();
        });
        JPanel fake2 = new JPanel();
        fake2.add(btna);
        panel.add(fake2);
        return panel;
    }
    private void update() {
        dropdown.removeAll();
        dropdown.setLayout(new BoxLayout(dropdown, BoxLayout.Y_AXIS));
        for (Library library: libraries) {
            JTextField lib = new JTextField();
            JTextField version = new JTextField();
            JButton btn = new JButton("-");
            btn.setForeground(JBColor.RED);
            btn.addActionListener(e -> {
                libraries.remove(library);
                update();
            });
            btn.setPreferredSize(new Dimension(20, 30));
            lib.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    library.library = lib.getText();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    // empty
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    library.library = lib.getText();
                }
            });
            version.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    library.version = version.getText();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    // empty
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    library.version = version.getText();
                }
            });
            lib.setText(library.library);
            version.setText(library.version);
            lib.setPreferredSize(new Dimension(250, 30));
            version.setPreferredSize(new Dimension(60,30));
            JPanel foo = new JPanel();
            foo.add(lib);
            foo.add(new JLabel("v"));
            foo.add(version);
            foo.add(btn);
            dropdown.add(foo);
        }
        dropdown.revalidate();
        dropdown.repaint();
        Window dialog = SwingUtilities.getWindowAncestor(dropdown);
        if (dialog != null) dialog.pack();
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

        GrClosableBlock block = ((GrMethodCall) ref.getElement()).getClosureArguments()[0];
        GrClosableBlock block2 = ((GrMethodCall) ref2.getElement()).getClosureArguments()[0];

        WriteCommandAction.runWriteCommandAction(project, () -> {
            // TODO refine
            block2.addStatementBefore(fac.createStatementFromText("url = 'https://maven.brott.dev/'"), null);
            block2.addStatementBefore(fac.createStatementFromText("url = 'https://maven.pedropathing.com/'"), null);
            for (Library library: dialog.libraries) {
                block.addStatementBefore(fac.createStatementFromText("implementation \"" + library.library + ":" + library.version + "\""), null);
            }
        });
    }
}