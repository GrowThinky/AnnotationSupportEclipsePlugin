package workbenchpreferencepage;

import org.eclipse.jdt.internal.ui.text.folding.*;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

// org.eclipse.jdt.ui.text.folding.IJavaFoldingPreferenceBlock
public class AnnotationFoldingPreferenceBlock implements IJavaFoldingPreferenceBlock{

	@Override
	public Control createControl(Composite parent) {
		Composite inner= new Composite(composite, SWT.NONE);
		inner.setLayout(new GridLayout(3, false));

		Label label= new Label(inner, SWT.CENTER);
		GridData gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= 30;
		label.setLayoutData(gd);

		label= new Label(inner, SWT.CENTER);
		label.setText(FoldingMessages.EmptyJavaFoldingPreferenceBlock_emptyCaption);
		gd= new GridData(GridData.CENTER);
		label.setLayoutData(gd);

		label= new Label(inner, SWT.CENTER);
		gd= new GridData(GridData.FILL_BOTH);
		gd.widthHint= 30;
		label.setLayoutData(gd);

		return inner;
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
