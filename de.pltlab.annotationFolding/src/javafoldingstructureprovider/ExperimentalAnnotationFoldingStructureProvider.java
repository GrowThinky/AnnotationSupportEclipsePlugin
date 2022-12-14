package javafoldingstructureprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.IProjectionListener;
import org.eclipse.jface.text.source.projection.IProjectionPosition;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.validator.AnnotationsValidator;

import javafoldingstructureprovider.BasicAnnotationFoldingStructureProvider.RegionType;

import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IAnnotatable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IImportContainer;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.SourceRange;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.IScanner;
import org.eclipse.jdt.core.compiler.ITerminalSymbols;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.CompilationUnit;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProvider;
import org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProviderExtension;
import org.eclipse.jdt.internal.core.util.PublicScanner;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.actions.SelectionConverter;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.internal.ui.text.DocumentCharacterIterator;

/**
 * Updates the projection model of a class file or compilation unit.
 * <p>
 * Clients may instantiate or subclass. Subclasses must make sure to always call
 * the superclass' code when overriding methods that are marked with "subclasses
 * may extend".
 * </p>
 *
 * @since 3.0 (internal)
 * @since 3.2 (API)
 */
@SuppressWarnings({ "restriction", "unused" })
public class ExperimentalAnnotationFoldingStructureProvider
		implements IJavaFoldingStructureProvider, IJavaFoldingStructureProviderExtension {
	/**
	 * A context that contains the information needed to compute the folding
	 * structure of an {@link ICompilationUnit} or an {@link IClassFile}. Computed
	 * folding regions are collected via
	 * {@linkplain #addProjectionRange(DefaultJavaFoldingStructureProvider.JavaProjectionAnnotation, Position)
	 * addProjectionRange}.
	 */
	protected final class FoldingStructureComputationContext {
		private final ProjectionAnnotationModel fModel;
		private final IDocument fDocument;

		private final boolean fAllowCollapsing;

		private IType fFirstType;
		private boolean fHasHeaderComment;
		private LinkedHashMap<JavaProjectionAnnotation, Position> fMap = new LinkedHashMap<>();
		private IScanner fScanner;

		private FoldingStructureComputationContext(IDocument document, ProjectionAnnotationModel model,
				boolean allowCollapsing, IScanner scanner) {
			Assert.isNotNull(document);
			Assert.isNotNull(model);
			fDocument = document;
			fModel = model;
			fAllowCollapsing = allowCollapsing;
			fScanner = scanner;
		}

		private void setFirstType(IType type) {
			if (hasFirstType())
				throw new IllegalStateException();
			fFirstType = type;
		}

		boolean hasFirstType() {
			return fFirstType != null;
		}

		private IType getFirstType() {
			return fFirstType;
		}

		private boolean hasHeaderComment() {
			return fHasHeaderComment;
		}

		private void setHasHeaderComment() {
			fHasHeaderComment = true;
		}

		/**
		 * Returns <code>true</code> if newly created folding regions may be collapsed,
		 * <code>false</code> if not. This is usually <code>false</code> when updating
		 * the folding structure while typing; it may be <code>true</code> when
		 * computing or restoring the initial folding structure.
		 *
		 * @return <code>true</code> if newly created folding regions may be collapsed,
		 *         <code>false</code> if not
		 */
		public boolean allowCollapsing() {
			return fAllowCollapsing;
		}

		/**
		 * Returns the document which contains the code being folded.
		 *
		 * @return the document which contains the code being folded
		 */
		private IDocument getDocument() {
			return fDocument;
		}

		private ProjectionAnnotationModel getModel() {
			return fModel;
		}

		private IScanner getScanner() {
			if (fScanner == null)
				fScanner = ToolFactory.createScanner(true, false, false, true);
			return fScanner;
		}

		/**
		 * Adds a projection (folding) region to this context. The created annotation /
		 * position pair will be added to the {@link ProjectionAnnotationModel} of the
		 * {@link ProjectionViewer} of the editor.
		 *
		 * @param annotation the annotation to add
		 * @param position   the corresponding position
		 */
		public void addProjectionRange(JavaProjectionAnnotation annotation, Position position) {
			fMap.put(annotation, position);
		}

		/**
		 * Returns <code>true</code> if header comments should be collapsed.
		 *
		 * @return <code>true</code> if header comments should be collapsed
		 */
		public boolean collapseHeaderComments() {
			return fAllowCollapsing && fCollapseHeaderComments;
		}

		/**
		 * Returns <code>true</code> if import containers should be collapsed.
		 *
		 * @return <code>true</code> if import containers should be collapsed
		 */
		public boolean collapseImportContainer() {
			return fAllowCollapsing && fCollapseImportContainer;
		}

		/**
		 * Returns <code>true</code> if inner types should be collapsed.
		 *
		 * @return <code>true</code> if inner types should be collapsed
		 */
		public boolean collapseInnerTypes() {
			return fAllowCollapsing && fCollapseInnerTypes;
		}

		/**
		 * Returns <code>true</code> if javadoc comments should be collapsed.
		 *
		 * @return <code>true</code> if javadoc comments should be collapsed
		 */
		public boolean collapseJavadoc() {
			return fAllowCollapsing && fCollapseJavadoc;
		}

		/**
		 * Returns <code>true</code> if methods should be collapsed.
		 *
		 * @return <code>true</code> if methods should be collapsed
		 */
		public boolean collapseMembers() {
			return fAllowCollapsing && fCollapseMembers;
		}

		/**
		 * Returns <code>true</code> if annotations should be collapsed.
		 *
		 * @return <code>true</code> if annotations should be collapsed
		 */
		public boolean collapseAnnotations() {
			return fAllowCollapsing && fCollapseAnnotations;
		}

	}

	public enum RegionType {
	    COMMENT,
	    ANNOTATION_INLINE, 
	    ANNOTATION_BLOCK,
	    MEMBER; 
	}
	
	/**
	 * A {@link ProjectionAnnotation} for java code.
	 */
	protected static final class JavaProjectionAnnotation extends ProjectionAnnotation {

		private IJavaElement fJavaElement;
		private boolean fIsComment;
		private RegionType fRegionType;

		/**
		 * Creates a new projection annotation.
		 *
		 * @param isCollapsed <code>true</code> to set the initial state to collapsed,
		 *                    <code>false</code> to set it to expanded
		 * @param element     the java element this annotation refers to
		 * @param isComment   <code>true</code> for a foldable comment,
		 *                    <code>false</code> for a foldable code element
		 */
		public JavaProjectionAnnotation(boolean isCollapsed, IJavaElement element, boolean isComment, RegionType regionType) {
			super(isCollapsed);
			fJavaElement = element;
			fIsComment = isComment;
			fRegionType= regionType;
		}

		IJavaElement getElement() {
			return fJavaElement;
		}

		void setElement(IJavaElement element) {
			fJavaElement = element;
		}

		boolean isComment() {
			return fIsComment;
		}

		public String getRegionType() {
			return this.fRegionType.toString();
		}
		
		void setIsComment(boolean isComment) {
			fIsComment = isComment;
		}

		/*
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "JavaProjectionAnnotation:\n" + //$NON-NLS-1$
					"\telement: \t" + fJavaElement.toString() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcollapsed: \t" + isCollapsed() + "\n" + //$NON-NLS-1$ //$NON-NLS-2$
					"\tcomment: \t" + isComment() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private static final class Tuple {
		JavaProjectionAnnotation annotation;
		Position position;

		Tuple(JavaProjectionAnnotation annotation, Position position) {
			this.annotation = annotation;
			this.position = position;
		}
	}

	/**
	 * Filter for annotations.
	 */
	private interface Filter {
		boolean match(JavaProjectionAnnotation annotation);
	}

	/**
	 * Matches comments.
	 */
	private static final class CommentFilter implements Filter {
		@Override
		public boolean match(JavaProjectionAnnotation annotation) {
			if (annotation.isComment() && !annotation.isMarkedDeleted()) {
				return true;
			}
			return false;
		}
	}

	/**
	 * Matches members.
	 */
	private static final class MemberFilter implements Filter {
		@Override
		public boolean match(JavaProjectionAnnotation annotation) {
			if (!annotation.isComment() && !annotation.isMarkedDeleted()) {
				IJavaElement element = annotation.getElement();
				if (element instanceof IMember) {
					if (element.getElementType() != IJavaElement.TYPE
							|| ((IMember) element).getDeclaringType() != null) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * Matches java elements contained in a certain set.
	 */
	private static final class JavaElementSetFilter implements Filter {
		private final Set<? extends IJavaElement> fSet;
		private final boolean fMatchCollapsed;

		private JavaElementSetFilter(Set<? extends IJavaElement> set, boolean matchCollapsed) {
			fSet = set;
			fMatchCollapsed = matchCollapsed;
		}

		@Override
		public boolean match(JavaProjectionAnnotation annotation) {
			boolean stateMatch = fMatchCollapsed == annotation.isCollapsed();
			if (stateMatch && !annotation.isComment() && !annotation.isMarkedDeleted()) {
				IJavaElement element = annotation.getElement();
				if (fSet.contains(element)) {
					return true;
				}
			}
			return false;
		}
	}

	private class ElementChangedListener implements IElementChangedListener {

		/*
		 * @see
		 * org.eclipse.jdt.core.IElementChangedListener#elementChanged(org.eclipse.jdt.
		 * core.ElementChangedEvent)
		 */
		@Override
		public void elementChanged(ElementChangedEvent e) {
			IJavaElementDelta delta = findElement(fInput, e.getDelta());
			if (delta != null
					&& (delta.getFlags() & (IJavaElementDelta.F_CONTENT | IJavaElementDelta.F_CHILDREN)) != 0) {

				if (shouldIgnoreDelta(e.getDelta().getCompilationUnitAST(), delta))
					return;

				fUpdatingCount++;
				try {
					update(createContext(false));
				} finally {
					fUpdatingCount--;
				}
			}
		}

		/**
		 * Ignore the delta if there are errors on the caret line.
		 * <p>
		 * We don't ignore the delta if an import is added and the caret isn't inside
		 * the import container.
		 * </p>
		 *
		 * @param ast   the compilation unit AST
		 * @param delta the Java element delta for the given AST element
		 * @return <code>true</code> if the delta should be ignored
		 * @since 3.3
		 */
		private boolean shouldIgnoreDelta(CompilationUnit ast, IJavaElementDelta delta) {
			if (ast == null)
				return false; // can't compute

			IDocument document = getDocument();
			if (document == null)
				return false; // can't compute

			JavaEditor editor = fEditor;
			if (editor == null || editor.getCachedSelectedRange() == null)
				return false; // can't compute

			try {
				if (delta.getAffectedChildren().length == 1
						&& delta.getAffectedChildren()[0].getElement() instanceof IImportContainer) {
					IJavaElement elem = SelectionConverter.getElementAtOffset(ast.getTypeRoot(),
							new TextSelection(editor.getCachedSelectedRange().x, editor.getCachedSelectedRange().y));
					if (!(elem instanceof IImportDeclaration))
						return false;

				}
			} catch (JavaModelException e) {
				return false; // can't compute
			}

			int caretLine = 0;
			try {
				caretLine = document.getLineOfOffset(editor.getCachedSelectedRange().x) + 1;
			} catch (BadLocationException x) {
				return false; // can't compute
			}

			if (caretLine > 0) {
				for (IProblem problem : ast.getProblems()) {
					if (problem.isError() && caretLine == problem.getSourceLineNumber()) {
						return true;
					}
				}
			}

			return false;
		}

		private IJavaElementDelta findElement(IJavaElement target, IJavaElementDelta delta) {

			if (delta == null || target == null)
				return null;

			IJavaElement element = delta.getElement();

			if (element.getElementType() > IJavaElement.CLASS_FILE)
				return null;

			if (target.equals(element))
				return delta;

			for (IJavaElementDelta child : delta.getAffectedChildren()) {
				IJavaElementDelta d = findElement(target, child);
				if (d != null)
					return d;
			}

			return null;
		}
	}

	
	/**
	 * Projection position that will return two foldable regions: one folding away
	 * the region from after the '/**' to the beginning of the content, the other
	 * from after the first content line until after the comment.
	 */
	private static final class CommentPosition extends Position implements IProjectionPosition {
		private boolean isAnnotation;

		CommentPosition(int offset, int length, boolean isAnnotation) {
			super(offset, length);
			this.isAnnotation = isAnnotation;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
		 * computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {

			DocumentCharacterIterator sequence = new DocumentCharacterIterator(document, offset, offset + length);
			int prefixEnd = 0;
			int contentStart = findFirstContent(sequence, prefixEnd);

			int firstLine = document.getLineOfOffset(offset + prefixEnd);
			int captionLine = document.getLineOfOffset(offset + contentStart);
			int lastLine = document.getLineOfOffset(offset + length);

			Assert.isTrue(firstLine <= captionLine, "first folded line is greater than the caption line"); //$NON-NLS-1$
			Assert.isTrue(captionLine <= lastLine, "caption line is greater than the last folded line"); //$NON-NLS-1$

			IRegion preRegion;
			if (firstLine < captionLine) {
//				preRegion= new Region(offset + prefixEnd, contentStart - prefixEnd);
				int preOffset = document.getLineOffset(firstLine);
				IRegion preEndLineInfo = document.getLineInformation(captionLine);
				int preEnd = preEndLineInfo.getOffset();
				preRegion = new Region(preOffset, preEnd - preOffset);
			} else {
				preRegion = null;
			}

			if (captionLine < lastLine) {
				int postOffset = document.getLineOffset(captionLine + 1);
				int postLength = offset + length - postOffset;
				if (postLength > 0) {
					IRegion postRegion = new Region(postOffset, postLength);
					if (preRegion == null)
						return new IRegion[] { postRegion };
					return new IRegion[] { preRegion, postRegion };
				}
			}

			if (preRegion != null)
				return new IRegion[] { preRegion };

			return null;

		}

		/**
		 * Finds the offset of the first identifier part within <code>content</code>.
		 * Returns 0 if none is found.
		 *
		 * @param content   the content to search
		 * @param prefixEnd the end of the prefix
		 * @return the first index of a unicode identifier part, or zero if none can be
		 *         found
		 */
		private int findFirstContent(final CharSequence content, int prefixEnd) {
			int lenght = content.length();
			for (int i = prefixEnd; i < lenght; i++) {
				if (Character.isUnicodeIdentifierPart(content.charAt(i)))
					return i;
			}
			return 0;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
		 * computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public int computeCaptionOffset(IDocument document) throws BadLocationException {
			DocumentCharacterIterator sequence = new DocumentCharacterIterator(document, offset, offset + length);
			return findFirstContent(sequence, 0);
		}
	}

	/**
	 * Projection position that will return two foldable regions: one folding away
	 * the lines before the one containing the simple name of the java element, one
	 * folding away any lines after the caption.
	 */
	private static final class JavaElementPosition extends Position implements IProjectionPosition {

		private IMember fMember;

		public JavaElementPosition(int offset, int length, IMember member) {
			super(offset, length);
			Assert.isNotNull(member);
			fMember = member;
		}

		public void setMember(IMember member) {
			Assert.isNotNull(member);
			fMember = member;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
		 * computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
			int nameStart = offset;
			try {
				/*
				 * The member's name range may not be correct. However, reconciling would
				 * trigger another element delta which would lead to reentrant situations.
				 * Therefore, we optimistically assume that the name range is correct, but
				 * double check the received lines below.
				 */
				ISourceRange nameRange = fMember.getNameRange();
				if (nameRange != null)
					nameStart = nameRange.getOffset();

			} catch (JavaModelException e) {
				// ignore and use default
			}

			int firstLine = document.getLineOfOffset(offset);
			int captionLine = document.getLineOfOffset(nameStart);
			int lastLine = document.getLineOfOffset(offset + length);

			/*
			 * see comment above - adjust the caption line to be inside the entire folded
			 * region, and rely on later element deltas to correct the name range.
			 */
			if (captionLine < firstLine)
				captionLine = firstLine;
			if (captionLine > lastLine)
				captionLine = lastLine;

			IRegion preRegion;
			if (firstLine < captionLine) {
				int preOffset = document.getLineOffset(firstLine);
				IRegion preEndLineInfo = document.getLineInformation(captionLine);
				int preEnd = preEndLineInfo.getOffset();
				preRegion = new Region(preOffset, preEnd - preOffset);
			} else {
				preRegion = null;
			}

			if (captionLine < lastLine) {
				int postOffset = document.getLineOffset(captionLine + 1);
				int postLength = offset + length - postOffset;
				if (postLength > 0) {
					IRegion postRegion = new Region(postOffset, postLength);
					if (preRegion == null)
						return new IRegion[] { postRegion };
					return new IRegion[] { preRegion, postRegion };
				}
			}

			if (preRegion != null)
				return new IRegion[] { preRegion };

			return null;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
		 * computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public int computeCaptionOffset(IDocument document) throws BadLocationException {
			int nameStart = offset;
			try {
				// need a reconcile here?
				ISourceRange nameRange = fMember.getNameRange();
				if (nameRange != null)
					nameStart = nameRange.getOffset();
			} catch (JavaModelException e) {
				// ignore and use default
			}

			return nameStart - offset;
		}

	}

	private static final class AnnotationBlockPosition extends Position implements IProjectionPosition {

		private IMember fMember;
		boolean foldAll;
		FoldingStructureComputationContext ctx;
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"de.pltlab.annotationFolding");
		String annotationsToHide = scopedPreferenceStore.getString("TO_HIDE");
		
		
		public AnnotationBlockPosition(int offset, int length, IMember member, boolean foldAll, FoldingStructureComputationContext ctx) {
			super(offset, length);
			Assert.isNotNull(member);
			fMember = member;
			this.foldAll = foldAll;
			this.ctx = ctx;
			

		}

		public void setMember(IMember member) {
			Assert.isNotNull(member);
			fMember = member;
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
		 * computeFoldingRegions(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {

			int captionLine = document.getLineOfOffset(offset);
			int lastLine = document.getLineOfOffset(offset + (length - 1));
			int lastLineEndOffset = document.getLineOffset(lastLine + 1) -1;
			
			IScanner scanner = ToolFactory.createScanner(true, false, false, true);
			scanner.setSource(document.get(offset, length).toCharArray());

			int shift;
			int argStart;

			try {
				// find "(" and ")" token offsets
				shift = offset;
				argStart = shift;
				
				int token = -1;
				while (token != ITerminalSymbols.TokenNameEOF) {

					token = scanner.getNextToken();

					if (token == ITerminalSymbols.TokenNameLPAREN) {
						argStart = shift + scanner.getCurrentTokenEndPosition() + 1;
						break;
					}
				}
				
					int annotationOffset = argStart;
					int annotationLength = lastLineEndOffset - annotationOffset;
					if (annotationLength > 0) {
						IRegion annotationRegion = new Region(annotationOffset, annotationLength);
						return new IRegion[] { annotationRegion };
					}
				

			} catch (InvalidInputException e) {

			}
			return null;
		}
		
		private void complexFolding(ArrayList<String> toHide, IAnnotation[] annotations, IRegion[] regions,
				int maxLength) throws JavaModelException, InvalidInputException {
			for (int i = 0; i < annotations.length; i++) {

				IAnnotation annotation = annotations[i];

				ISourceRange nameRange = annotation.getNameRange();
				ISourceRange sourceRange = annotation.getSourceRange();

				int sourceEnd = sourceRange.getOffset() + sourceRange.getLength();

				int peekLength = 0;
				if (sourceRange.getLength() > maxLength) {
					peekLength = maxLength;

					IScanner scanner = ToolFactory.createScanner(true, false, false, true);
					scanner.setSource(annotation.getSource().toCharArray());

					int start = sourceRange.getOffset();
					int cutOff = start;

					ArrayList<Integer> tokenEnds = new ArrayList<Integer>();

					// find token-end closest to peek cutoff
					int token = -1;
					while (token != ITerminalSymbols.TokenNameEOF) {
						token = scanner.getNextToken();
						tokenEnds.add(scanner.getCurrentTokenEndPosition());
					}

					for (int x : tokenEnds) {
						if (x > (peekLength)) {
							peekLength = x + 1;
							break;
						}

					}
				}

				// hide entire annotation if in toHide list
				if (toHide.contains(annotation.getElementName())) {
					regions[i] = new Region(sourceRange.getOffset() - 2, sourceRange.getLength() + 2);
				} else {
					// else hide portion after cutoff
					if (peekLength != 0) {
						regions[i] = new Region(sourceRange.getOffset() + peekLength,
								sourceRange.getLength() - peekLength);

					} else {
						// dummy region
						regions[i] = new Region(sourceRange.getOffset() + sourceRange.getLength(), 0);
					}
				}
			}
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
		 * computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public int computeCaptionOffset(IDocument document) throws BadLocationException {
			int nameStart = offset;
			try {
				// need a reconcile here?
				ISourceRange nameRange = fMember.getNameRange();
				if (nameRange != null)
					nameStart = nameRange.getOffset();
			} catch (JavaModelException e) {
				// ignore and use default
			}

			return nameStart - offset;
		}

	}
	

	private static final class AnnotationInlinePosition extends Position implements IProjectionPosition {

		private IMember fMember;
		FoldingStructureComputationContext ctx;
		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
				"de.pltlab.annotationFolding");
		String annotationsToHide = scopedPreferenceStore.getString("TO_HIDE");


		public AnnotationInlinePosition(int offset, int length, IMember member, boolean foldAll,
				int[] annotationGrouping, FoldingStructureComputationContext ctx) {
			super(offset, length);
			Assert.isNotNull(member);
			fMember = member;
			this.ctx = ctx;
			this.offset = offset;
		}

		/*
		 * Computes the actual text regions to be removed from the projection document 
		 * when a group of single line annotations is collapsed.
		 */
		@Override
		public IRegion[] computeProjectionRegions(IDocument document) throws BadLocationException {
			
			IRegion[] regions = null;	
			ArrayList<IRegion> regionsArrayList = new ArrayList<IRegion>();
			
			IScanner scanner = ToolFactory.createScanner(true, false, false, true);
			scanner.setSource(document.get(offset, length).toCharArray());
		
			int shift;
			int argStart;
			int argEnd;
			
			try {
				// find "(" and ")" token offsets
				shift = offset;
				argStart = shift;
				argEnd = shift;
				
				int token = -1;
				boolean startFound = false;
				while (token != ITerminalSymbols.TokenNameEOF) {
					token = scanner.getNextToken();
					if (!startFound && token == ITerminalSymbols.TokenNameLPAREN) {
						argStart = shift + scanner.getCurrentTokenEndPosition() + 1;
						startFound = true;
					}
					if (startFound && token == ITerminalSymbols.TokenNameRPAREN) {
						argEnd = shift + scanner.getCurrentTokenEndPosition();
						regionsArrayList.add(new Region(argStart, argEnd - argStart ));  
						startFound = false;
				//		regionsArrayList.add(new Region(argEnd+1, 1));					  //TODO: adding the region for the newlineRange causes problems with line numbers and document's char-index to line mapping										
					}
				}

			//	regionsArrayList.remove(regionsArrayList.size()-1); // removing region for last inlineAnnotation 

				regions = new IRegion[regionsArrayList.size()];
				for (int i = 0; i < regions.length; i++) {
					regions[i] = regionsArrayList.get(i);
				}

			} catch ( InvalidInputException e) {

			}

			return regions;

		}

		//old Complex folding strategy
		private void complexFolding(ArrayList<String> toHide, IAnnotation[] annotations, IRegion[] regions,
				int maxLength) throws JavaModelException, InvalidInputException {
			for (int i = 0; i < annotations.length; i++) {

				IAnnotation annotation = annotations[i];

				ISourceRange nameRange = annotation.getNameRange();
				ISourceRange sourceRange = annotation.getSourceRange();

				int sourceEnd = sourceRange.getOffset() + sourceRange.getLength();

				int peekLength = 0;
				if (sourceRange.getLength() > maxLength) {
					peekLength = maxLength;

					IScanner scanner = ToolFactory.createScanner(true, false, false, true);
					scanner.setSource(annotation.getSource().toCharArray());

					int start = sourceRange.getOffset();
					int cutOff = start;

					ArrayList<Integer> tokenEnds = new ArrayList<Integer>();

					// find token-end closest to peek cutoff
					int token = -1;
					while (token != ITerminalSymbols.TokenNameEOF) {
						token = scanner.getNextToken();
						tokenEnds.add(scanner.getCurrentTokenEndPosition());
					}

					for (int x : tokenEnds) {
						if (x > (peekLength)) {
							peekLength = x + 1;
							break;
						}

					}
				}

				// hide entire annotation if in toHide list
				if (toHide.contains(annotation.getElementName())) {
					regions[i] = new Region(sourceRange.getOffset() - 2, sourceRange.getLength() + 2);
				} else {
					// else hide portion after cutoff
					if (peekLength != 0) {
						regions[i] = new Region(sourceRange.getOffset() + peekLength,
								sourceRange.getLength() - peekLength);

					} else {
						// dummy region
						regions[i] = new Region(sourceRange.getOffset() + sourceRange.getLength(), 0);
					}
				}
			}
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionPosition#
		 * computeCaptionOffset(org.eclipse.jface.text.IDocument)
		 */
		@Override
		public int computeCaptionOffset(IDocument document) throws BadLocationException {
			int nameStart = offset;
			try {
				// need a reconcile here?
				ISourceRange nameRange = fMember.getNameRange();
				if (nameRange != null)
					nameStart = nameRange.getOffset();
			} catch (JavaModelException e) {
				// ignore and use default
			}

			return nameStart - offset;
		}

	}

	/**
	 * Internal projection listener.
	 */
	private final class MyProjectionListener implements IProjectionListener {
		private ProjectionViewer fViewer;

		/**
		 * Registers the listener with the viewer.
		 *
		 * @param viewer the viewer to register a listener with
		 */
		public MyProjectionListener(ProjectionViewer viewer) {
			Assert.isLegal(viewer != null);
			fViewer = viewer;
			fViewer.addProjectionListener(this);
		}

		/**
		 * Disposes of this listener and removes the projection listener from the
		 * viewer.
		 */
		public void dispose() {
			if (fViewer != null) {
				fViewer.removeProjectionListener(this);
				fViewer = null;
			}
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionListener#
		 * projectionEnabled()
		 */
		@Override
		public void projectionEnabled() {
			handleProjectionEnabled();
		}

		/*
		 * @see org.eclipse.jface.text.source.projection.IProjectionListener#
		 * projectionDisabled()
		 */
		@Override
		public void projectionDisabled() {
			handleProjectionDisabled();
		}
	}

	/* context and listeners */
	private JavaEditor fEditor;
	private MyProjectionListener fProjectionListener;
	private IJavaElement fInput;
	private IElementChangedListener fElementListener;

	/* annotation folding variables */
	private Boolean isAnnotated;
	IAnnotation[] annotations = new IAnnotation[0];
	private int[] lineLengthOfAnnotation;
	private int[] lengthOfAnnotation;
	private ISourceRange[] rangeOfAnnotation;
	private ISourceRange rangeOfMember;
	private int methodOffset;
	private int methodLength;

	/* preferences */
	private boolean fCollapseJavadoc = false;
	private boolean fCollapseImportContainer = true;
	private boolean fCollapseInnerTypes = true;
	private boolean fCollapseMembers = false;
	private boolean fCollapseHeaderComments = true;
	private boolean fCollapseAnnotations = false;
	private boolean complexAnnotationFolding = true;

	private int numberOfAnnotationRanges = 0;
	private int annotationFoldingMinLineNumbers = 1;
	private IJavaElement javaElement;
	private IMember currentMember;

	/* filters */
	/** Member filter, matches nested members (but not top-level types). */
	private final Filter fMemberFilter = new MemberFilter();
	/** Comment filter, matches comments. */
	private final Filter fCommentFilter = new CommentFilter();

	/**
	 * Reusable scanner.
	 * 
	 * @since 3.3
	 */
	private IScanner fSharedScanner = ToolFactory.createScanner(true, false, false, false);

	private volatile int fUpdatingCount = 0;

	/**
	 * Creates a new folding provider. It must be
	 * {@link #install(ITextEditor, ProjectionViewer) installed} on an editor/viewer
	 * pair before it can be used, and {@link #uninstall() uninstalled} when not
	 * used any longer.
	 * <p>
	 * The projection state may be reset by calling {@link #initialize()}.
	 * </p>
	 */
	public ExperimentalAnnotationFoldingStructureProvider() {
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 *
	 * @param editor {@inheritDoc}
	 * @param viewer {@inheritDoc}
	 */

	public void install(ITextEditor editor, ProjectionViewer viewer) {
		Assert.isLegal(editor != null);
		Assert.isLegal(viewer != null);

		internalUninstall();

		if (editor instanceof JavaEditor) {
			fProjectionListener = new MyProjectionListener(viewer);
			fEditor = (JavaEditor) editor;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	@Override
	public void uninstall() {
		internalUninstall();
	}

	/**
	 * Internal implementation of {@link #uninstall()}.
	 */
	private void internalUninstall() {
		if (isInstalled()) {
			handleProjectionDisabled();
			fProjectionListener.dispose();
			fProjectionListener = null;
			fEditor = null;
		}
	}

	/**
	 * Returns <code>true</code> if the provider is installed, <code>false</code>
	 * otherwise.
	 *
	 * @return <code>true</code> if the provider is installed, <code>false</code>
	 *         otherwise
	 */
	protected final boolean isInstalled() {
		return fEditor != null;
	}

	/**
	 * Called whenever projection is enabled, for example when the viewer issues a
	 * {@link IProjectionListener#projectionEnabled() projectionEnabled} message.
	 * When the provider is already enabled when this method is called, it is first
	 * {@link #handleProjectionDisabled() disabled}.
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	protected void handleProjectionEnabled() {
		// http://home.ott.oti.com/teams/wswb/anon/out/vms/index.html
		// projectionEnabled messages are not always paired with projectionDisabled
		// i.e. multiple enabled messages may be sent out.
		// we have to make sure that we disable first when getting an enable
		// message.
		handleProjectionDisabled();

		if (isInstalled()) {
			initialize();
			fElementListener = new ElementChangedListener();
			JavaCore.addElementChangedListener(fElementListener);
		}
	}

	/**
	 * Called whenever projection is disabled, for example when the provider is
	 * {@link #uninstall() uninstalled}, when the viewer issues a
	 * {@link IProjectionListener#projectionDisabled() projectionDisabled} message
	 * and before {@link #handleProjectionEnabled() enabling} the provider.
	 * Implementations must be prepared to handle multiple calls to this method even
	 * if the provider is already disabled.
	 * <p>
	 * Subclasses may extend.
	 * </p>
	 */
	protected void handleProjectionDisabled() {
		if (fElementListener != null) {
			JavaCore.removeElementChangedListener(fElementListener);
			fElementListener = null;
		}
	}

	/*
	 * @see
	 * org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProvider#initialize()
	 */
	@Override
	public final void initialize() {
		fUpdatingCount++;
		try {
			update(createInitialContext());
		} finally {
			fUpdatingCount--;
		}
	}

	private FoldingStructureComputationContext createInitialContext() {
		initializePreferences();
		fInput = getInputElement();
		if (fInput == null)
			return null;

		return createContext(true);
	}

	private FoldingStructureComputationContext createContext(boolean allowCollapse) {
		if (!isInstalled())
			return null;
		ProjectionAnnotationModel model = getModel();
		if (model == null)
			return null;
		IDocument doc = getDocument();
		if (doc == null)
			return null;

		IScanner scanner = null;
		if (fUpdatingCount == 1)
			scanner = fSharedScanner; // reuse scanner

		return new FoldingStructureComputationContext(doc, model, allowCollapse, scanner);
	}

	private IJavaElement getInputElement() {
		if (fEditor == null)
			return null;
		return EditorUtility.getEditorInputJavaElement(fEditor, false);
	}

	private void initializePreferences() {

		IPreferenceStore store = JavaPlugin.getDefault().getPreferenceStore();
		fCollapseInnerTypes = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_INNERTYPES);
		fCollapseImportContainer = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_IMPORTS);
		fCollapseJavadoc = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_JAVADOC);
		fCollapseMembers = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_METHODS);
		fCollapseHeaderComments = store.getBoolean(PreferenceConstants.EDITOR_FOLDING_HEADERS);
		fCollapseAnnotations = store.getBoolean("INIT_LONG");
//		ScopedPreferenceStore scopedPreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE,
//				"de.pltlab.annotationFolding");
//		complexAnnotationFolding = scopedPreferenceStore.getBoolean("COMPLEX_ENABLED");
		//fCollapseAnnotations = scopedPreferenceStore.getBoolean("INITIAL_FOLD");
		

//		int userMinLines = 1;
//		try {
//			userMinLines = Integer.valueOf(scopedPreferenceStore.getString("MIN_LINES"));
//		} catch (NumberFormatException e) {
//
//		}
//		if (userMinLines == (int) userMinLines && userMinLines > 0 && userMinLines < 5) {
//			annotationFoldingMinLineNumbers = userMinLines;
//		}
	}
	

	private void update(FoldingStructureComputationContext ctx) {
		if (ctx == null)
			return;

		Map<JavaProjectionAnnotation, Position> additions = new HashMap<>();
		List<JavaProjectionAnnotation> deletions = new ArrayList<>();
		List<JavaProjectionAnnotation> updates = new ArrayList<>();

		computeFoldingStructure(ctx);
		Map<JavaProjectionAnnotation, Position> newStructure = ctx.fMap;
		Map<IJavaElement, List<Tuple>> oldStructure = computeCurrentStructure(ctx);

		Iterator<JavaProjectionAnnotation> e = newStructure.keySet().iterator();
		while (e.hasNext()) {
			JavaProjectionAnnotation newAnnotation = e.next();
			Position newPosition = newStructure.get(newAnnotation);

			IJavaElement element = newAnnotation.getElement();
			/*
			 * See https://bugs.eclipse.org/bugs/show_bug.cgi?id=130472 and
			 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=127445 In the presence of
			 * syntax errors, anonymous types may have a source range offset of 0. When such
			 * a situation is encountered, we ignore the proposed folding range: if no
			 * corresponding folding range exists, it is silently ignored; if there *is* a
			 * matching folding range, we ignore the position update and keep the old range,
			 * in order to keep the folding structure stable.
			 */
			boolean isMalformedAnonymousType = newPosition.getOffset() == 0
					&& element.getElementType() == IJavaElement.TYPE && isInnerType((IType) element);
			List<Tuple> annotations = oldStructure.get(element);
			if (annotations == null) {
				if (!isMalformedAnonymousType)
					additions.put(newAnnotation, newPosition);
			} else {
				Iterator<Tuple> x = annotations.iterator();
				boolean matched = false;
				while (x.hasNext()) {
					Tuple tuple = x.next();
					JavaProjectionAnnotation existingAnnotation = tuple.annotation;
					Position existingPosition = tuple.position;
					if (newAnnotation.getRegionType() == existingAnnotation.getRegionType()) {
						boolean updateCollapsedState = ctx.allowCollapsing()
								&& existingAnnotation.isCollapsed() != newAnnotation.isCollapsed();
						if (!isMalformedAnonymousType && existingPosition != null
								&& (!newPosition.equals(existingPosition) || updateCollapsedState)) {
							existingPosition.setOffset(newPosition.getOffset());
							existingPosition.setLength(newPosition.getLength());
							if (updateCollapsedState)
								if (newAnnotation.isCollapsed())
									existingAnnotation.markCollapsed();
								else
									existingAnnotation.markExpanded();
							updates.add(existingAnnotation);
						}
						matched = true;
						x.remove();
						break;
					}
				}
				if (!matched)
					additions.put(newAnnotation, newPosition);

				if (annotations.isEmpty())
					oldStructure.remove(element);
			}
		}

		Iterator<List<Tuple>> iter = oldStructure.values().iterator();
		while (iter.hasNext()) {
			List<Tuple> list = iter.next();
			int size = list.size();
			for (int i = 0; i < size; i++)
				deletions.add(list.get(i).annotation);
		}

		match(deletions, additions, updates, ctx);

		Annotation[] deletedArray = deletions.toArray(new Annotation[deletions.size()]);
		Annotation[] changedArray = updates.toArray(new Annotation[updates.size()]);
		ctx.getModel().modifyAnnotations(deletedArray, additions, changedArray);

		ctx.fScanner.setSource(null);
	}

	private void computeFoldingStructure(FoldingStructureComputationContext ctx) {
		IParent parent = (IParent) fInput;
		try {
			if (!(fInput instanceof ISourceReference))
				return;
			String source = ((ISourceReference) fInput).getSource();
			if (source == null)
				return;

			ctx.getScanner().setSource(source.toCharArray());
			computeFoldingStructure(parent.getChildren(), ctx);
		} catch (JavaModelException x) {
		}
	}

	// compute for children (recursively) JavaElements
	private void computeFoldingStructure(IJavaElement[] elements, FoldingStructureComputationContext ctx)
			throws JavaModelException {

		for (IJavaElement element : elements) {
			if (element instanceof IMethod) {
				IAnnotatable method = (IAnnotatable) element;
				IAnnotation[] annotations = method.getAnnotations();
			}

			computeFoldingStructure(element, ctx);

			if (element instanceof IParent) {
				IParent parent = (IParent) element;
				computeFoldingStructure(parent.getChildren(), ctx);
			}
		}
	}

	/**
	 * Computes the folding structure for a given {@link IJavaElement java element}.
	 * Computed projection annotations are
	 * {@link DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext#addProjectionRange(DefaultJavaFoldingStructureProvider.JavaProjectionAnnotation, Position)
	 * added} to the computation context.
	 * <p>
	 * Subclasses may extend or replace. The default implementation creates
	 * projection annotations for the following elements:
	 * </p>
	 * <ul>
	 * <li>true members (not for top-level types)</li>
	 * <li>the javadoc comments of any member</li>
	 * <li>header comments (javadoc or multi-line comments appearing before the
	 * first type's javadoc or before the package or import declarations).</li>
	 * </ul>
	 *
	 * @param element the java element to compute the folding structure for
	 * @param ctx     the computation context
	 * @throws JavaModelException
	 */
	@SuppressWarnings({ "restriction", "unused" })
	protected void computeFoldingStructure(IJavaElement element, FoldingStructureComputationContext ctx)
			throws JavaModelException {
		
		boolean collapse = false;
		boolean collapseCode = true;
		boolean annotationInitialCollapse = true;
			
		isAnnotated = false;
		javaElement = element;
		
		if(javaElement instanceof IMember) {
			currentMember = (IMember) javaElement;
		}
		
		switch (element.getElementType()) {

		case IJavaElement.IMPORT_CONTAINER:
			collapse = ctx.collapseImportContainer();
			break;
		case IJavaElement.TYPE:
			collapseCode = isInnerType((IType) element) && !isAnonymousEnum((IType) element);
			collapse = ctx.collapseInnerTypes() && collapseCode;
			break;
		case IJavaElement.METHOD:
			
			if (element instanceof IAnnotatable) {
				IAnnotatable method = (IAnnotatable) element;
				annotations = method.getAnnotations();
				if (annotations.length > 0) {
					isAnnotated = true;
					collectAnnotationInformation(annotations);
				}
			}
			
			collapse = ctx.collapseMembers();
			break;
		case IJavaElement.FIELD:
		case IJavaElement.INITIALIZER:
			collapse = ctx.collapseMembers();
			break;
		default:
			return;
		}

		IRegion[] regions = computeProjectionRanges((ISourceReference) element, ctx);

		if (regions.length > 0) {
			// comments
			for (int i = 0; i < regions.length - (numberOfAnnotationRanges + 1); i++) {
				IRegion normalized= alignRegion(regions[i], ctx);

				if (normalized != null) {
					Position position = createCommentPosition(normalized, false);
					if (position != null) {
						boolean commentCollapse;
						if (i == 0 && (regions.length > 2 || ctx.hasHeaderComment()) && element == ctx.getFirstType()) {
							commentCollapse = ctx.collapseHeaderComments();
						} else {
							commentCollapse = ctx.collapseJavadoc();
						}
						ctx.addProjectionRange(new JavaProjectionAnnotation(commentCollapse, element, true, RegionType.COMMENT), position);
					}
				}
			}
			// annotations 
			if (numberOfAnnotationRanges > 0 ) {
				for (int i = regions.length - (numberOfAnnotationRanges + 1); i < regions.length - 1; i++) {
					Position position = null;
					RegionType regionType;
					AnnotationRegion normalized =  (AnnotationRegion) alignRegion(regions[i], ctx);
					if (normalized != null) {
						if (normalized.inline) {
							annotationInitialCollapse = false;
							regionType = RegionType.ANNOTATION_INLINE;
							position = createAnnotationInlinePosition(normalized, (IMember) element, false, ctx);
						} else {
							annotationInitialCollapse = ctx.collapseAnnotations();
							regionType = RegionType.ANNOTATION_BLOCK;
							position = createAnnotationBlockPosition(normalized, (IMember) element, false, ctx);
						}
						if (position != null) {
							ctx.addProjectionRange(new JavaProjectionAnnotation(annotationInitialCollapse, element, true, regionType),
									position);
						}
					}
				}
				// addCombinedAnnotationRegion(element, ctx, regions);
			}

			// code
			if (collapseCode) {
				IRegion normalized = alignRegion(regions[regions.length - 1], ctx);
				if (normalized != null) {
					Position position = element instanceof IMember ? createMemberPosition(normalized, (IMember) element)
							: createCommentPosition(normalized, false);
					if (position != null)
						ctx.addProjectionRange(new JavaProjectionAnnotation(collapse, element, false, RegionType.MEMBER), position);
				}
			}
		}
	}

	private void collectAnnotationInformation(IAnnotation[] annotations) throws JavaModelException {
		
		lineLengthOfAnnotation = new int[annotations.length];
		lengthOfAnnotation = new int[annotations.length];
		rangeOfAnnotation = new ISourceRange[annotations.length];

		for (int i = 0; i < annotations.length; i++) {
			lineLengthOfAnnotation[i] = (int) annotations[i].getSource().lines().count();
			lengthOfAnnotation[i] = annotations[i].getSource().length();
			rangeOfAnnotation[i] = annotations[i].getSourceRange();
		}
	}

//	private void addCombinedAnnotationRegion(IJavaElement element, FoldingStructureComputationContext ctx,
//			IRegion[] regions) {
//		boolean foldAll = true; // TODO: clarify logic
//
//		int numberOfNonAnnotationRanges = numberOfAnnotationRanges + 1; // add member's range
//
//		for (int i = regions.length - numberOfNonAnnotationRanges; i < regions.length - 1; i++) {
//			IRegion normalized = alignRegion(regions[i], ctx);
//			if (normalized != null) {
//				Position position = createBlockAnnotationPosition(normalized, (IMember) element, foldAll, ctx);
//				if (position != null) {
//					boolean annotationCollapse;
//					annotationCollapse = ctx.collapseAnnotations();
//					ctx.addProjectionRange(new JavaProjectionAnnotation(annotationCollapse, element, true, null), position);
//				}
//			}
//			foldAll = false;
//		}
//	}

	/**
	 * Returns <code>true</code> if <code>type</code> is an anonymous enum
	 * declaration, <code>false</code> otherwise. See also
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=143276
	 *
	 * @param type the type to test
	 * @return <code>true</code> if <code>type</code> is an anonymous enum
	 *         declaration
	 * @since 3.3
	 */
	private boolean isAnonymousEnum(IType type) {
		try {
			return type.isEnum() && type.isAnonymous();
		} catch (JavaModelException x) {
			return false; // optimistically
		}
	}

	/**
	 * Returns <code>true</code> if <code>type</code> is not a top-level type,
	 * <code>false</code> if it is.
	 *
	 * @param type the type to test
	 * @return <code>true</code> if <code>type</code> is an inner type
	 */
	private boolean isInnerType(IType type) {
		return type.getDeclaringType() != null;
	}
	

	/**
	 * Computes the projection ranges for a given <code>ISourceReference</code>.
	 * More than one range or none at all may be returned. If there are no foldable
	 * regions, an empty array is returned.
	 * <p>
	 * The last region in the returned array (if not empty) describes the region for
	 * the java element that implements the source reference. Any preceding regions
	 * describe javadoc comments of that java element.
	 * </p>
	 *
	 * @param reference a java element that is a source reference
	 * @param ctx       the folding context
	 * @return the regions to be folded
	 */
	protected final IRegion[] computeProjectionRanges(ISourceReference reference,
			FoldingStructureComputationContext ctx) {
		
		numberOfAnnotationRanges = 0;
	
		try {
			ISourceRange range = reference.getSourceRange();
			if (!SourceRange.isAvailable(range)) {
				return new IRegion[0];
			}

			String contents = reference.getSource();
			if (contents == null) {
				return new IRegion[0];
			}


			List<IRegion> regions = new ArrayList<>();
			if (!ctx.hasFirstType() && reference instanceof IType) {
				ctx.setFirstType((IType) reference);
				IRegion headerComment = computeHeaderComment(ctx);
				if (headerComment != null) {
					regions.add(headerComment);
					ctx.setHasHeaderComment();
				}
			}

			final int shift = range.getOffset();
			IScanner scanner = ctx.getScanner();
			scanner.resetTo(shift, shift + range.getLength());

			int start = shift;
			while (true) {

				int token = scanner.getNextToken();
				start = scanner.getCurrentTokenStartPosition();

				switch (token) {

				case ITerminalSymbols.TokenNameCOMMENT_JAVADOC:
				case ITerminalSymbols.TokenNameCOMMENT_BLOCK: {

					int end = scanner.getCurrentTokenEndPosition() + 1;

					regions.add(new Region(start, end - start));
					continue;
				}
				case ITerminalSymbols.TokenNameCOMMENT_LINE:
					continue;

				}

				break;
			}
			// java annotation ranges
			if (isAnnotated && isLongAnnotation() && complexAnnotationFolding) {

				IDocument document = ctx.getDocument();
				Boolean[] isInline = new Boolean[annotations.length];
				
				int aStart = 0;
				int aEnd = 0;
				int lastInlineIndex = 0;
				
				for (int i = 0; i < lineLengthOfAnnotation.length; i++) {
					if (lineLengthOfAnnotation[i] > 1) {
						isInline[i] = false;
					} else {
						isInline[i] = true;
					}
				}					
					for (int i = 0; i < isInline.length; i++) {
						// block annotations:
						if (!isInline[i]) {
							aStart = rangeOfAnnotation[i].getOffset();
							aEnd = aStart + rangeOfAnnotation[i].getLength();		
							regions.add(new AnnotationRegion(aStart, aEnd - aStart, false));
							numberOfAnnotationRanges++;

							// in-line annotations
						} else {
							boolean inlineGroup = false;
							aStart = rangeOfAnnotation[i].getOffset();
							while (i + 1 < isInline.length && isInline[i + 1]) {
								inlineGroup = true;
								i++;
								lastInlineIndex = i;
							}
							aEnd = getEndOffset(rangeOfAnnotation[lastInlineIndex]);

							if (inlineGroup) {
								regions.add(new AnnotationRegion(aStart, aEnd - aStart, true));
								numberOfAnnotationRanges++;
							}
						}
					}

				int memberStart = document.getLineOffset(document.getLineOfOffset(currentMember.getNameRange().getOffset()));
				int memberEnd =  currentMember.getSourceRange().getOffset() + currentMember.getSourceRange().getLength();
				
				// add member region
				regions.add(new Region(memberStart, memberEnd - memberStart - 2));

			} else {
				// member without annotation folding range
				regions.add(new Region(start, shift + range.getLength() - start));

			}
			IRegion[] result = new IRegion[regions.size()];
			regions.toArray(result);
			return result;
		} catch (JavaModelException | InvalidInputException | BadLocationException e) {
		}

		return new IRegion[0];
	}
	
	private int getEndOffset(ISourceRange range) {
		return range.getOffset() + range.getLength();
	}



	private boolean isLongAnnotation() {
		return IntStream.of(lineLengthOfAnnotation).sum() > annotationFoldingMinLineNumbers;
	}
	

	private void addMultipleAnnotationRanges(List<IRegion> regions, int start) {
		// method with multiple line annotations
		for (int i = 0; i < lengthOfAnnotation.length; i++) {
			if (lineLengthOfAnnotation[i] > 1) {

				// calculate start of method below annotation
				int prevStart = start + 3;
				start = start + (lengthOfAnnotation[i] - 2);

				// add annotation regions TODO: single Region makes more sense
				regions.add(new Region(rangeOfAnnotation[i].getOffset(), rangeOfAnnotation[i].getLength() - 1));

			}
		}
	}

	private int scanTilMethod(List<IRegion> regions, IScanner scanner, ISourceRange range, int start) {

		int shift = start;
		try {
			outer: while (start <= range.getOffset() + range.getLength()) {
				int token = scanner.getNextToken();
				start = scanner.getCurrentTokenStartPosition();
				switch (token) {
				case ITerminalSymbols.TokenNamepublic:
					break outer;
				case ITerminalSymbols.TokenNameprivate:
					break outer;
				case ITerminalSymbols.TokenNameprotected:
					break outer;
				}
			}
		} catch (InvalidInputException e) {
		}

		return start;

	}

	private IRegion computeHeaderComment(FoldingStructureComputationContext ctx) throws JavaModelException {
		// search at most up to the first type
		ISourceRange range = ctx.getFirstType().getSourceRange();
		if (range == null)
			return null;
		int start = 0;
		int end = range.getOffset();

		/*
		 * code adapted from CommentFormattingStrategy: scan the header content up to
		 * the first type. Once a comment is found, accumulate any additional comments
		 * up to the stop condition. The stop condition is reaching a package
		 * declaration, import container, or the end of the input.
		 */
		IScanner scanner = ctx.getScanner();
		scanner.resetTo(start, end);

		int headerStart = -1;
		int headerEnd = -1;
		try {
			boolean foundComment = false;
			int terminal = scanner.getNextToken();
			while (terminal != ITerminalSymbols.TokenNameEOF && (terminal != ITerminalSymbols.TokenNameclass)
					&& (terminal != ITerminalSymbols.TokenNameinterface) && (terminal != ITerminalSymbols.TokenNameenum)
					&& (!foundComment || ((terminal != ITerminalSymbols.TokenNameimport)
							&& (terminal != ITerminalSymbols.TokenNamepackage)))) {

				if (terminal == ITerminalSymbols.TokenNameCOMMENT_JAVADOC
						|| terminal == ITerminalSymbols.TokenNameCOMMENT_BLOCK
						|| terminal == ITerminalSymbols.TokenNameCOMMENT_LINE) {
					if (!foundComment)
						headerStart = scanner.getCurrentTokenStartPosition();
					headerEnd = scanner.getCurrentTokenEndPosition();
					foundComment = true;
				}
				terminal = scanner.getNextToken();

			}

		} catch (InvalidInputException ex) {
			return null;
		}

		if (headerEnd != -1) {
			return new Region(headerStart, headerEnd - headerStart);
		}
		return null;
	}

	/**
	 * Creates a comment folding position from an
	 * {@link #alignRegion(IRegion, DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext)
	 * aligned} region.
	 *
	 * @param aligned an aligned region
	 * @return a folding position corresponding to <code>aligned</code>
	 */
	protected final Position createCommentPosition(IRegion aligned, Boolean isAnnotation) {
		return new CommentPosition(aligned.getOffset(), aligned.getLength(), isAnnotation);
	}

	/**
	 * Creates a folding position that remembers its member from an
	 * {@link #alignRegion(IRegion, DefaultJavaFoldingStructureProvider.FoldingStructureComputationContext)
	 * aligned} region.
	 *
	 * @param aligned an aligned region
	 * @param member  the member to remember
	 * @return a folding position corresponding to <code>aligned</code>
	 */
	protected final Position createMemberPosition(IRegion aligned, IMember member) {
		return new JavaElementPosition(aligned.getOffset(), aligned.getLength(), member);
	}


	protected final Position createAnnotationBlockPosition(AnnotationRegion aligned, IMember member, boolean foldAll,
			FoldingStructureComputationContext ctx) {
		return new AnnotationBlockPosition(aligned.getOffset(), aligned.getLength(), member, foldAll, ctx);
	}

	protected final Position createAnnotationInlinePosition(AnnotationRegion aligned, IMember member, boolean foldAll,
			FoldingStructureComputationContext ctx) {
		return new AnnotationInlinePosition(aligned.getOffset(), aligned.getLength(), member, foldAll, aligned.annotationGrouping, ctx);
	}

	/**
	 * 
	 * /** Aligns <code>region</code> to start and end at a line offset. The
	 * region's start is decreased to the next line offset, and the end offset
	 * increased to the next line start or the end of the document.
	 * <code>null</code> is returned if <code>region</code> is <code>null</code>
	 * itself or does not comprise at least one line delimiter, as a single line
	 * cannot be folded.
	 *
	 * @param region the region to align, may be <code>null</code>
	 * @param ctx    the folding context
	 * @return a region equal or greater than <code>region</code> that is aligned
	 *         with line offsets, <code>null</code> if the region is too small to be
	 *         foldable (e.g. covers only one line)
	 */
	protected final IRegion alignRegion(IRegion region, FoldingStructureComputationContext ctx) {
		if (region == null)
			return null;

		IDocument document = ctx.getDocument();

		try {

			int start = document.getLineOfOffset(region.getOffset());
			int end = document.getLineOfOffset(region.getOffset() + region.getLength());
			if (start >= end)
				return null;

			int offset = document.getLineOffset(start);
			int endOffset;
			if (document.getNumberOfLines() > end + 1)
				endOffset = document.getLineOffset(end + 1);
			else
				endOffset = document.getLineOffset(end) + document.getLineLength(end);
			
			if(region instanceof AnnotationRegion) {
				AnnotationRegion annotationRegion = (AnnotationRegion) region;
				return new AnnotationRegion(offset, endOffset - offset, annotationRegion.inline);
			}

			return new Region(offset, endOffset - offset);

		} catch (BadLocationException x) {
			// concurrent modification
			return null;
		}
	}

	private ProjectionAnnotationModel getModel() {
		return fEditor.getAdapter(ProjectionAnnotationModel.class);
	}

	private IDocument getDocument() {
		JavaEditor editor = fEditor;
		if (editor == null)
			return null;

		IDocumentProvider provider = editor.getDocumentProvider();
		if (provider == null)
			return null;

		return provider.getDocument(editor.getEditorInput());
	}

	/**
	 * Matches deleted annotations to changed or added ones. A deleted
	 * annotation/position tuple that has a matching addition / change is updated
	 * and marked as changed. The matching tuple is not added (for additions) or
	 * marked as deletion instead (for changes). The result is that more annotations
	 * are changed and fewer get deleted/re-added.
	 *
	 * @param deletions list with deleted annotations
	 * @param additions map with position to annotation mappings
	 * @param changes   list with changed annotations
	 * @param ctx       the context
	 */
	private void match(List<JavaProjectionAnnotation> deletions, Map<JavaProjectionAnnotation, Position> additions,
			List<JavaProjectionAnnotation> changes, FoldingStructureComputationContext ctx) {
		if (deletions.isEmpty() || (additions.isEmpty() && changes.isEmpty()))
			return;

		List<JavaProjectionAnnotation> newDeletions = new ArrayList<>();
		List<JavaProjectionAnnotation> newChanges = new ArrayList<>();

		Iterator<JavaProjectionAnnotation> deletionIterator = deletions.iterator();
		while (deletionIterator.hasNext()) {
			JavaProjectionAnnotation deleted = deletionIterator.next();
			Position deletedPosition = ctx.getModel().getPosition(deleted);
			if (deletedPosition == null)
				continue;

			Tuple deletedTuple = new Tuple(deleted, deletedPosition);

			Tuple match = findMatch(deletedTuple, changes, null, ctx);
			boolean addToDeletions = true;
			if (match == null) {
				match = findMatch(deletedTuple, additions.keySet(), additions, ctx);
				addToDeletions = false;
			}

			if (match != null) {
				IJavaElement element = match.annotation.getElement();
				deleted.setElement(element);
				deletedPosition.setLength(match.position.getLength());
				if (deletedPosition instanceof JavaElementPosition && element instanceof IMember) {
					JavaElementPosition jep = (JavaElementPosition) deletedPosition;
					jep.setMember((IMember) element);
				}

				deletionIterator.remove();
				newChanges.add(deleted);

				if (addToDeletions)
					newDeletions.add(match.annotation);
			}
		}

		deletions.addAll(newDeletions);
		changes.addAll(newChanges);
	}

	/**
	 * Finds a match for <code>tuple</code> in a collection of annotations. The
	 * positions for the <code>JavaProjectionAnnotation</code> instances in
	 * <code>annotations</code> can be found in the passed <code>positionMap</code>
	 * or <code>fCachedModel</code> if <code>positionMap</code> is
	 * <code>null</code>.
	 * <p>
	 * A tuple is said to match another if their annotations have the same comment
	 * flag and their position offsets are equal.
	 * </p>
	 * <p>
	 * If a match is found, the annotation gets removed from
	 * <code>annotations</code>.
	 * </p>
	 *
	 * @param tuple       the tuple for which we want to find a match
	 * @param annotations collection of <code>JavaProjectionAnnotation</code>
	 * @param positionMap a <code>Map&lt;Annotation, Position&gt;</code> or
	 *                    <code>null</code>
	 * @param ctx         the context
	 * @return a matching tuple or <code>null</code> for no match
	 */
	private Tuple findMatch(Tuple tuple, Collection<JavaProjectionAnnotation> annotations,
			Map<JavaProjectionAnnotation, Position> positionMap, FoldingStructureComputationContext ctx) {
		Iterator<JavaProjectionAnnotation> it = annotations.iterator();
		while (it.hasNext()) {
			JavaProjectionAnnotation annotation = it.next();
			if (tuple.annotation.isComment() == annotation.isComment()) {
				Position position = positionMap == null ? ctx.getModel().getPosition(annotation)
						: positionMap.get(annotation);
				if (position == null)
					continue;

				if (tuple.position.getOffset() == position.getOffset()) {
					it.remove();
					return new Tuple(annotation, position);
				}
			}
		}

		return null;
	}

	private Map<IJavaElement, List<Tuple>> computeCurrentStructure(FoldingStructureComputationContext ctx) {
		Map<IJavaElement, List<Tuple>> map = new HashMap<>();
		ProjectionAnnotationModel model = ctx.getModel();
		Iterator<Annotation> e = model.getAnnotationIterator();
		while (e.hasNext()) {
			Object annotation = e.next();
			if (annotation instanceof JavaProjectionAnnotation) {
				JavaProjectionAnnotation java = (JavaProjectionAnnotation) annotation;
				Position position = model.getPosition(java);
				Assert.isNotNull(position);
				List<Tuple> list = map.get(java.getElement());
				if (list == null) {
					list = new ArrayList<>(2);
					map.put(java.getElement(), list);
				}
				list.add(new Tuple(java, position));
			}
		}

		Comparator<Tuple> comparator = (o1, o2) -> o1.position.getOffset() - o2.position.getOffset();
		for (List<Tuple> list : map.values()) {
			Collections.sort(list, comparator);
		}
		return map;
	}

	/*
	 * @see IJavaFoldingStructureProviderExtension#collapseMembers()
	 * 
	 * @since 3.2
	 */
	@Override
	public final void collapseMembers() {
		modifyFiltered(fMemberFilter, false);
	}

	/*
	 * @see IJavaFoldingStructureProviderExtension#collapseComments()
	 * 
	 * @since 3.2
	 */
	@Override
	public final void collapseComments() {
		modifyFiltered(fCommentFilter, false);
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProviderExtension#
	 * collapseElements(org.eclipse.jdt.core.IJavaElement[])
	 */
	@Override
	public final void collapseElements(IJavaElement[] elements) {
		Set<IJavaElement> set = new HashSet<>(Arrays.asList(elements));
		modifyFiltered(new JavaElementSetFilter(set, false), false);
	}

	/*
	 * @see org.eclipse.jdt.ui.text.folding.IJavaFoldingStructureProviderExtension#
	 * expandElements(org.eclipse.jdt.core.IJavaElement[])
	 */
	@Override
	public final void expandElements(IJavaElement[] elements) {
		Set<IJavaElement> set = new HashSet<>(Arrays.asList(elements));
		modifyFiltered(new JavaElementSetFilter(set, true), true);
	}

	/**
	 * Collapses or expands all annotations matched by the passed filter.
	 *
	 * @param filter the filter to use to select which annotations to collapse
	 * @param expand <code>true</code> to expand the matched annotations,
	 *               <code>false</code> to collapse them
	 */
	private void modifyFiltered(Filter filter, boolean expand) {
		if (!isInstalled())
			return;

		ProjectionAnnotationModel model = getModel();
		if (model == null)
			return;

		List<JavaProjectionAnnotation> modified = new ArrayList<>();
		Iterator<Annotation> iter = model.getAnnotationIterator();
		while (iter.hasNext()) {
			Object annotation = iter.next();
			if (annotation instanceof JavaProjectionAnnotation) {
				JavaProjectionAnnotation java = (JavaProjectionAnnotation) annotation;

				if (expand == java.isCollapsed() && filter.match(java)) {
					if (expand)
						java.markExpanded();
					else
						java.markCollapsed();
					modified.add(java);
				}

			}
		}

		model.modifyAnnotations(null, null, modified.toArray(new Annotation[modified.size()]));
	}

}
