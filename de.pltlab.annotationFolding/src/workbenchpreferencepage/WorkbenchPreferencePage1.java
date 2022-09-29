package workbenchpreferencepage;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

@SuppressWarnings("unused")
public class WorkbenchPreferencePage1 extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public WorkbenchPreferencePage1() {
        super(GRID);
    }

    public void createFieldEditors() {
        addField(new BooleanFieldEditor("COMPLEX_ENABLED", "&Enable Complex Annotation Folding", getFieldEditorParent()));

        addField(new BooleanFieldEditor("FOLD_ARGUMENTS", "&Fold Arguments", getFieldEditorParent()));
        addField(new BooleanFieldEditor("HIDE_SELECTED", "Hide &Selected Annotations", getFieldEditorParent()));
       // addField(new RadioGroupFieldEditor("CHOICE", "A &multiple-choice preference", 1,
       //         new String[][] { { "&Fold Arguments", "choice1" }, { "&Hide Selected Annotations", "choice2" } }, getFieldEditorParent()));
        addField(new StringFieldEditor("TO_HIDE", "Annotations &to hide:", getFieldEditorParent()));
       
    }

    @Override
    public void init(IWorkbench workbench) {
        // second parameter is typically the plug-in id
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, "de.pltlab.annotationFolding"));
        setDescription("Configure Annotation Folding Behavior");
    }

}
