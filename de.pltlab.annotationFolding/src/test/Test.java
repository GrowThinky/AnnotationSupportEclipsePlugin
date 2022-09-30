package test;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditor;

class Test {

	@org.junit.jupiter.api.Test
	void test() throws ExecutionException, CoreException {

//		ExecutionEvent event = null;
//		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
//		IEditorPart editorPart = HandlerUtil.getActiveEditor(event);
//
//		MessageDialog.openInformation(window.getShell(), "GenerateBuilderProject",
//				editorPart.getEditorInput().getName());
//
//		IEditorInput input = editorPart.getEditorInput();
//		if (input instanceof FileEditorInput) {
//			IFile file = ((FileEditorInput) input).getFile();
//			InputStream is = file.getContents();
//			// TODO get contents from InputStream
//		}
//		
		TextEditor editor = (TextEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow()
			    .getActivePage().getActiveEditor();
		
		IDocumentProvider provider = editor.getDocumentProvider();
		
			// isInstanceOf
		ProjectionAnnotationModel model = (ProjectionAnnotationModel) provider.getAnnotationModel(provider);
		
		
		
		IDocument document = provider.getDocument(model);
		
		
		
		// IDE.openEditor(	, input, null)

	}   

}
