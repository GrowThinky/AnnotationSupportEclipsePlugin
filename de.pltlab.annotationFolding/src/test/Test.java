package test;


import static org.junit.jupiter.api.Assertions.*;
import java.io.InputStream;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaSourceViewer;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.projection.ProjectionDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.junit.jupiter.api.Assertions;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditor;

@SuppressWarnings("restriction")
class Test {

	@org.junit.jupiter.api.Test
	void test() throws ExecutionException, CoreException, BadLocationException {

		JavaEditor editor = (JavaEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		document.set(test1);
		editor.doSave(null);
		editor.getDocumentProvider().resetDocument(document);

		if (editor != null) {
			ProjectionViewer viewer = (ProjectionViewer) editor.getViewer();
			
			viewer.collapseAll();
			viewer.reinitializeProjection();

			ProjectionDocument projectionDocument = viewer.getProjectionTest();

			if (projectionDocument != null) {
				
				String foldedText = projectionDocument.get();
				System.out.println("one" + foldedText);

				Assertions.assertEquals(folded1,foldedText);
				
				document.set(test2);
				editor.doSave(null);
				
				viewer.collapseAll();
				viewer.reinitializeProjection();
				
				projectionDocument = viewer.getProjectionTest();
				foldedText = projectionDocument.get();
				
				Assertions.assertEquals(folded2,foldedText);			
				return;
			}

		}
		fail();

	}   
	
	String test1 = "package fruits;\n"
			+ "\n"
			+ "public class fruitTest {\n"
			+ "	\n"
			+ "	\n"
			+ "	@SuppressWarnings({ \"restriction\",\"unused\",\"restriction\", \"unused\"  })\n"
			+ "	@Finished(value = \"Class scope\", priority = Unfinished.Priority.LOW)\n"
			+ "	public void juice() {\n"
			+ "		System.out.println(\"Fruit Juice!\");\n"
			+ "	}\n"
			+ "}";
	
	String folded1 = "package fruits;\n"
			+ "\n"
			+ "public class fruitTest {\n"
			+ "	\n"
			+ "	\n"
			+ "	\n"
			+ "	public void juice() {\n"
			+ "}";
	
	String test2 = "package fruits;\n"
			+ "\n"
			+ "public class fruitTest {\n"
			+ "	\n"
			+ "	\n"
			+ "	@SuppressWarnings(\"unused\")\n"
			+ "	public void peel() {\n"
			+ "		System.out.println(\"Fruit Peel!\");\n"
			+ "	}\n"
			+ "}";
	
	String folded2 = "package fruits;\n"
			+ "\n"
			+ "public class fruitTest {\n"
			+ "	\n"
			+ "	\n"
			+ "	public void peel() {\n"
			+ "}";
			
}
