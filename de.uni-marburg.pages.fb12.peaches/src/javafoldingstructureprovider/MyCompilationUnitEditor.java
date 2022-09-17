package javafoldingstructureprovider;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.ISlaveDocumentManager;
import org.eclipse.jface.text.source.IOverviewRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.MyJavaSourceViewer;
import org.eclipse.swt.widgets.Composite;

@SuppressWarnings({ "restriction", "unused" })
public class MyCompilationUnitEditor extends CompilationUnitEditor {
	
	@Override
	protected ISourceViewer createJavaSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler, boolean isOverviewRulerVisible, int styles, IPreferenceStore store) {
		return new MyJavaSourceViewer(parent, verticalRuler, getOverviewRuler(), isOverviewRulerVisible(), styles, store);
	}
	
	
	protected class MyAdaptedSourceViewer extends AdaptedSourceViewer  {

		public MyAdaptedSourceViewer(Composite parent, IVerticalRuler verticalRuler, IOverviewRuler overviewRuler,
				boolean showAnnotationsOverview, int styles, IPreferenceStore store) {
			super(parent, verticalRuler, overviewRuler, showAnnotationsOverview, styles, store);
		}
		
		@Override
		protected ISlaveDocumentManager createSlaveDocumentManager() {
			return new MyProjectionDocumentManager();
		}
		
	}

}
