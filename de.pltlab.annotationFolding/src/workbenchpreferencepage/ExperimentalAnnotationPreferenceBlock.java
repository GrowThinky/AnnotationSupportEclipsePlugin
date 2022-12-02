package workbenchpreferencepage;

import org.eclipse.jdt.internal.ui.text.folding.*;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore;
import org.eclipse.jdt.internal.ui.preferences.OverlayPreferenceStore.OverlayKey;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.internal.ui.*;

// org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock
public class ExperimentalAnnotationPreferenceBlock implements IJavaFoldingPreferenceBlock{
	
	private IPreferenceStore fStore;
	private Map<Button, String> fCheckBoxes= new HashMap<>();
	private SelectionListener fCheckBoxListener= new SelectionListener() {
		@Override
		public void widgetDefaultSelected(SelectionEvent e) {
		}
		@Override
		public void widgetSelected(SelectionEvent e) {
			Button button= (Button) e.widget;
			fStore.setValue(fCheckBoxes.get(button), button.getSelection());
		}
	};


	@SuppressWarnings("restriction")
	public ExperimentalAnnotationPreferenceBlock() {
		fStore= JavaPlugin.getDefault().getPreferenceStore();
	}


	@Override
	public Control createControl(Composite composite) {

		Composite inner= new Composite(composite, SWT.NONE);
		GridLayout layout= new GridLayout(1, true);
		layout.verticalSpacing= 3;
		layout.marginWidth= 0;
		inner.setLayout(layout);

		Label label= new Label(inner, SWT.LEFT);
		label.setText("Initially fold these elements:");

		addCheckBox(inner, "Comments", PreferenceConstants.EDITOR_FOLDING_JAVADOC, 0);
		addCheckBox(inner, "Header Comments", PreferenceConstants.EDITOR_FOLDING_HEADERS, 0);
		addCheckBox(inner, "Inner types", PreferenceConstants.EDITOR_FOLDING_INNERTYPES, 0);
		addCheckBox(inner, "Members", PreferenceConstants.EDITOR_FOLDING_METHODS, 0);
		addCheckBox(inner, "Imports", PreferenceConstants.EDITOR_FOLDING_IMPORTS, 0);
		
		
		Label label2= new Label(inner, SWT.LEFT);
		label2.setText("AnnotationFolding:");
		
		
		addCheckBox(inner, "Initially fold multi-line annotation ranges ", "INIT_LONG", 0);
		
		
		Label label3= new Label(inner, SWT.LEFT);
		label3.setText(
				System.lineSeparator() +
						"This structure provider will only work if all toLineStart() calls in ProjectionViewer's "
						+ System.lineSeparator()+ "add/removeMasterDocumentRange() methods are commented out.");
		
		Label label4= new Label(inner, SWT.LEFT);
		
		label4.setText(System.lineSeparator() +
				"Java files need to be re-opened for changes to take effect.");
		
		initializeFields();

		return inner;
	}

	private Button addCheckBox(Composite parent, String label, String key, int indentation) {
		Button checkBox= new Button(parent, SWT.CHECK);
		checkBox.setText(label);

		GridData gd= new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		gd.horizontalIndent= indentation;
		gd.horizontalSpan= 1;
		gd.grabExcessVerticalSpace= false;
		checkBox.setLayoutData(gd);
		checkBox.addSelectionListener(fCheckBoxListener);

		fCheckBoxes.put(checkBox, key);

		return checkBox;
	}

	private void initializeFields() {
		Iterator<Button> it= fCheckBoxes.keySet().iterator();
		while (it.hasNext()) {
			Button b= it.next();
			String key= fCheckBoxes.get(b);
			b.setSelection(fStore.getBoolean(key));
		}
		
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void performOk() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void performDefaults() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

}
