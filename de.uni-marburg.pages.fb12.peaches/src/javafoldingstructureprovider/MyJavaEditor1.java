package javafoldingstructureprovider;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.MyJavaProjectionViewer2;
import org.eclipse.jface.text.source.projection.MyJavaSourceViewer;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings({ "restriction", "unused" })
public class MyJavaEditor1 extends JavaEditor{


	@Override
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean isOverviewRulerVisible, int styles, IPreferenceStore store) {
		return new MyJavaSourceViewer(parent, verticalRuler, getOverviewRuler(), isOverviewRulerVisible(), styles, store);
	}

	@Override
	protected IJavaElement getElementAt(int offset) {
		return super.getElementAt(offset, fIsUpdatingMarkerViews);
		
	}

	@Override
	protected IJavaElement getCorrespondingElement(IJavaElement element) {
		return element; 
	}
		
	

}
